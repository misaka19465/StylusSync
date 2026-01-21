package team.misakanet.stylussync.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import team.misakanet.stylussync.R
import team.misakanet.stylussync.midi.MidiClient
import team.misakanet.stylussync.midi.MidiEvent
import team.misakanet.stylussync.midi.MidiEvent.Type
import team.misakanet.stylussync.utils.Constants
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Custom view for handling stylus input and rendering canvas with stroke overlay.
 * Manages touch events, converts them to MIDI messages, and displays visual feedback.
 */
@SuppressLint("ViewConstructor")
class CanvasView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet),
    SharedPreferences.OnSharedPreferenceChangeListener {
    
    companion object {
        private const val TAG = "StylusSync.CanvasView"
        private const val MIN_STROKE_FADE_DURATION_MS = 100L
        private const val DEFAULT_STROKE_FADE_DURATION_MS = 1200L
        private const val STROKE_BASE_RADIUS_DP = 4f
        private const val STROKE_MIN_STEP_PX = 4f
    }
    
    /**
     * Enumeration for stylus range status.
     */
    private enum class InRangeStatus {
        OutOfRange,
        InRange,
        FakeInRange
    }
    
    /**
     * Inner class representing a stroke path with coordinates and fade information.
     */
    private class StrokePath {
        val xs = ArrayList<Float>()
        val ys = ArrayList<Float>()
        val widths = ArrayList<Float>()
        var fadeStartMs: Long = Long.MAX_VALUE // not fading until set
        var createdMs: Long = 0
    }
    
    val settings: SharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
    var midiClient: MidiClient? = null
        set(value) {
            field = value
            isEnabled = value != null
        }
    private var acceptStylusOnly: Boolean = false
    private var maxX: Int = 0
    private var maxY: Int = 0
    private var inRangeStatus: InRangeStatus = InRangeStatus.OutOfRange
    private var showStrokeOverlay: Boolean = false
    private var strokeFadeDurationMs: Long = 0
    private var strokeBaseRadiusPx: Float = 0f
    private var strokeColor: Int = 0
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val activePaths = mutableMapOf<Int, StrokePath>()
    private val fadingPaths = mutableListOf<StrokePath>()
    
    // Track stylus side button states
    private var stylusButton1Pressed = false
    private var stylusButton2Pressed = false
    
    init {
        // view is disabled until a MIDI client is set
        isEnabled = false
        
        settings.registerOnSharedPreferenceChangeListener(this)
        setBackground()
        setInputMethods()
        initStrokeOverlay()
    }
    
    // settings
    
    /**
     * Sets the background based on grid visibility settings.
     */
    protected fun setBackground() {
        val gridVisible = settings.getBoolean(Constants.PreferenceKeys.KEY_GRID_VISIBLE, true)
        if (gridVisible) {
            // Create a drawable with grid pattern and apply opacity
            val opacity = settings.getInt(Constants.PreferenceKeys.KEY_GRID_OPACITY, 30)
            setBackgroundResource(R.drawable.bg_grid_pattern)
            background.alpha = (opacity * 2.55f).toInt() // Convert 0-100 to 0-255
        } else {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }
    
    protected fun setInputMethods() {
        acceptStylusOnly = settings.getBoolean(Constants.PreferenceKeys.KEY_STYLUS_ONLY, false)
    }
    
    private fun initStrokeOverlay() {
        strokeColor = resolveStrokeColor()
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.strokeJoin = Paint.Join.ROUND
        strokePaint.color = strokeColor
        strokeBaseRadiusPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            STROKE_BASE_RADIUS_DP,
            resources.displayMetrics
        )
        setStrokeOverlayPreferences()
    }
    
    private fun setStrokeOverlayPreferences() {
        showStrokeOverlay = settings.getBoolean(Constants.PreferenceKeys.KEY_STROKE_OVERLAY_VISIBLE, true)
        strokeFadeDurationMs = settings.getInt(
            Constants.PreferenceKeys.KEY_STROKE_FADE_DURATION,
            DEFAULT_STROKE_FADE_DURATION_MS.toInt()
        ).toLong()
        strokeFadeDurationMs = strokeFadeDurationMs.coerceAtLeast(MIN_STROKE_FADE_DURATION_MS)
        if (!showStrokeOverlay) {
            activePaths.clear()
            fadingPaths.clear()
            postInvalidateOnAnimation()
        }
    }
    
    private fun resolveStrokeColor(): Int {
        val value = TypedValue()
        return if (context.theme.resolveAttribute(android.R.attr.colorAccent, value, true)) {
            value.data
        } else {
            0xFFFF4081.toInt() // fallback accent-like color
        }
    }
    
    /**
     * Called when a shared preference is changed.
     * Updates the view settings accordingly.
     * @param sharedPreferences The shared preferences.
     * @param key The key of the changed preference.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Constants.PreferenceKeys.KEY_STYLUS_ONLY -> {
                setInputMethods()
            }
            Constants.PreferenceKeys.KEY_GRID_VISIBLE,
            Constants.PreferenceKeys.KEY_GRID_OPACITY -> {
                setBackground()
            }
            Constants.PreferenceKeys.KEY_STROKE_OVERLAY_VISIBLE,
            Constants.PreferenceKeys.KEY_STROKE_FADE_DURATION -> {
                setStrokeOverlayPreferences()
            }
        }
    }
    
    /**
     * Detect and send stylus side button events
     * Button 1: BUTTON_STYLUS (secondary stylus button)
     * Button 2: BUTTON_STYLUS2 (tertiary stylus button)
     */
    private fun detectAndSendSidButtons(event: MotionEvent, x: Short, y: Short, pressure: Short) {
        if (event.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            return
        }
        
        // Check BUTTON_STYLUS (side button 1)
        val button1Now = event.isButtonPressed(MotionEvent.BUTTON_STYLUS_PRIMARY)
        if (button1Now != stylusButton1Pressed) {
            stylusButton1Pressed = button1Now
            Log.d(TAG, "Stylus button 1 ${if (button1Now) "pressed" else "released"}")
            midiClient?.queue?.add(MidiEvent(Type.TYPE_BUTTON, x, y, pressure, 1, button1Now))
        }
        
        // Check BUTTON_STYLUS2 (side button 2)
        val button2Now = event.isButtonPressed(MotionEvent.BUTTON_STYLUS_SECONDARY)
        if (button2Now != stylusButton2Pressed) {
            stylusButton2Pressed = button2Now
            Log.d(TAG, "Stylus button 2 ${if (button2Now) "pressed" else "released"}")
            midiClient?.queue?.add(MidiEvent(Type.TYPE_BUTTON, x, y, pressure, 2, button2Now))
        }
    }
    
    // drawing
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        Log.i(TAG, "Canvas size changed: ${w}x$h (before: ${oldw}x$oldh)")
        maxX = w
        maxY = h
    }
    
    /**
     * Handles generic motion events, such as hover movements.
     * @param event The motion event.
     * @return true if the event was handled, false otherwise.
     */
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (isEnabled) {
            for (ptr in 0 until event.pointerCount) {
                if (!acceptStylusOnly || (event.getToolType(ptr) == MotionEvent.TOOL_TYPE_STYLUS)) {
                    val nx = normalizeX(event.getX(ptr))
                    val ny = normalizeY(event.getY(ptr))
                    val npressure = normalizePressure(event.getPressure(ptr))
                    
                    // Detect stylus side buttons
                    detectAndSendSidButtons(event, nx, ny, npressure)
                    
                    when (event.actionMasked) {
                        MotionEvent.ACTION_HOVER_MOVE -> {
                            midiClient?.queue?.add(MidiEvent(Type.TYPE_MOTION, nx, ny, npressure))
                        }
                        MotionEvent.ACTION_HOVER_ENTER -> {
                            inRangeStatus = InRangeStatus.InRange
                            midiClient?.queue?.add(MidiEvent(Type.TYPE_BUTTON, nx, ny, npressure, -1, true))
                        }
                        MotionEvent.ACTION_HOVER_EXIT -> {
                            inRangeStatus = InRangeStatus.OutOfRange
                            midiClient?.queue?.add(MidiEvent(Type.TYPE_BUTTON, nx, ny, npressure, -1, false))
                        }
                    }
                }
            }
            return true
        }
        return false
    }
    
    /**
     * Handles touch events from the user.
     * @param event The motion event.
     * @return true if the event was handled, false otherwise.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEnabled) {
            for (ptr in 0 until event.pointerCount) {
                if (!acceptStylusOnly || (event.getToolType(ptr) == MotionEvent.TOOL_TYPE_STYLUS)) {
                    val nx = normalizeX(event.getX(ptr))
                    val ny = normalizeY(event.getY(ptr))
                    val npressure = normalizePressure(event.getPressure(ptr))
                    
                    // Detect stylus side buttons
                    detectAndSendSidButtons(event, nx, ny, npressure)
                    
                    when (event.actionMasked) {
                        MotionEvent.ACTION_MOVE -> {
                            recordStrokePath(event.getX(ptr), event.getY(ptr), event.getPressure(ptr), event.getPointerId(ptr), false)
                            midiClient?.queue?.add(MidiEvent(Type.TYPE_MOTION, nx, ny, npressure))
                        }
                        MotionEvent.ACTION_DOWN -> {
                            recordStrokePath(event.getX(ptr), event.getY(ptr), event.getPressure(ptr), event.getPointerId(ptr), true)
                            if (inRangeStatus == InRangeStatus.OutOfRange) {
                                inRangeStatus = InRangeStatus.FakeInRange
                                midiClient?.queue?.add(MidiEvent(Type.TYPE_BUTTON, nx, ny, 0.toShort(), -1, true))
                            }
                            midiClient?.queue?.add(MidiEvent(Type.TYPE_BUTTON, nx, ny, npressure, 0, true))
                        }
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            recordStrokePath(event.getX(ptr), event.getY(ptr), event.getPressure(ptr), event.getPointerId(ptr), false)
                            midiClient?.queue?.add(MidiEvent(Type.TYPE_BUTTON, nx, ny, npressure, 0, false))
                            if (inRangeStatus == InRangeStatus.FakeInRange) {
                                inRangeStatus = InRangeStatus.OutOfRange
                                midiClient?.queue?.add(MidiEvent(Type.TYPE_BUTTON, nx, ny, 0.toShort(), -1, false))
                            }
                            finishStrokePath(event.getPointerId(ptr))
                        }
                    }
                }
            }
            return true
        }
        return false
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!showStrokeOverlay) {
            return
        }
        
        val now = SystemClock.uptimeMillis()
        strokePaint.color = strokeColor
        
        var needsMoreFrames = false
        
        // Draw active strokes (no fade yet)
        strokePaint.alpha = 255
        for (path in activePaths.values) {
            drawStrokePath(canvas, path, 1f)
            needsMoreFrames = true // active stroke needs redraw for updates
        }
        
        // Draw fading strokes
        for (i in fadingPaths.indices.reversed()) {
            val path = fadingPaths[i]
            val age = now - path.fadeStartMs
            if (age >= strokeFadeDurationMs) {
                fadingPaths.removeAt(i)
                continue
            }
            val alpha = 1f - age.toFloat() / strokeFadeDurationMs.toFloat()
            strokePaint.alpha = (alpha * 255).toInt()
            drawStrokePath(canvas, path, alpha)
            needsMoreFrames = true
        }
        
        if (needsMoreFrames) {
            postInvalidateOnAnimation()
        }
    }
    
    private fun drawStrokePath(canvas: Canvas, path: StrokePath, alpha: Float) {
        val size = path.xs.size
        if (size < 2) {
            return
        }
        for (i in 0 until size - 1) {
            val x1 = path.xs[i]
            val y1 = path.ys[i]
            val x2 = path.xs[i + 1]
            val y2 = path.ys[i + 1]
            val width1 = path.widths[i]
            val width2 = path.widths[i + 1]
            val width = (width1 + width2) * 0.5f // average width for smoother transition
            strokePaint.strokeWidth = width
            canvas.drawLine(x1, y1, x2, y2, strokePaint)
        }
    }
    
    private fun recordStrokePath(rawX: Float, rawY: Float, pressure: Float, pointerId: Int, isDown: Boolean) {
        if (!showStrokeOverlay) {
            return
        }
        val now = SystemClock.uptimeMillis()
        val clampedPressure = pressure.coerceIn(0f, 1f)
        val strokeWidth = strokeBaseRadiusPx * 2f * (0.6f + (0.8f * clampedPressure))
        
        var path = activePaths[pointerId]
        if (path == null || isDown) {
            path = StrokePath()
            path.createdMs = now
            path.xs.add(rawX)
            path.ys.add(rawY)
            path.widths.add(strokeWidth)
            activePaths[pointerId] = path
            postInvalidateOnAnimation()
            return
        }
        
        // Get last point
        val lastIndex = path.xs.size - 1
        val lastX = path.xs[lastIndex]
        val lastY = path.ys[lastIndex]
        val lastWidth = path.widths[lastIndex]
        
        val dx = rawX - lastX
        val dy = rawY - lastY
        val dist = hypot(dx, dy)
        
        if (dist > STROKE_MIN_STEP_PX) {
            val steps = ceil(dist / STROKE_MIN_STEP_PX).toInt()
            for (i in 1..steps) {
                val t = i.toFloat() / steps
                val ix = lastX + dx * t
                val iy = lastY + dy * t
                val iwidth = lastWidth + (strokeWidth - lastWidth) * t
                path.xs.add(ix)
                path.ys.add(iy)
                path.widths.add(iwidth)
            }
        } else {
            path.xs.add(rawX)
            path.ys.add(rawY)
            path.widths.add(strokeWidth)
        }
        postInvalidateOnAnimation()
    }
    
    private fun finishStrokePath(pointerId: Int) {
        val path = activePaths.remove(pointerId) ?: return
        path.fadeStartMs = SystemClock.uptimeMillis()
        fadingPaths.add(path)
        postInvalidateOnAnimation()
    }
    
    // these overflow and wrap around to negative short values, but thankfully Java will continue
    // on regardless, so we can just ignore Java's interpretation of them and send them anyway.
    private fun normalizeX(x: Float): Short =
        (x.coerceIn(0f, maxX.toFloat()) * 2 * Short.MAX_VALUE / maxX).toInt().toShort()
    
    private fun normalizeY(x: Float): Short =
        (x.coerceIn(0f, maxY.toFloat()) * 2 * Short.MAX_VALUE / maxY).toInt().toShort()
    
    private fun normalizePressure(x: Float): Short =
        (x.coerceIn(0f, 2.0f) * Short.MAX_VALUE).toInt().toShort()
}
