package team.misakanet.stylussync.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import team.misakanet.stylussync.R;
import team.misakanet.stylussync.midi.MidiClient;
import team.misakanet.stylussync.midi.MidiEvent;
import team.misakanet.stylussync.midi.MidiEvent.Type;
import team.misakanet.stylussync.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom view for handling stylus input and rendering canvas with stroke overlay.
 * Manages touch events, converts them to MIDI messages, and displays visual feedback.
 */
@SuppressLint("ViewConstructor")
public class CanvasView extends View implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "StylusSync.CanvasView";

	/**
	 * Enumeration for stylus range status.
	 */
	private enum InRangeStatus {
		OutOfRange,
		InRange,
		FakeInRange
	}

    final SharedPreferences settings;
    MidiClient midiClient;
	boolean acceptStylusOnly;
	int maxX, maxY;
	InRangeStatus inRangeStatus;
	boolean showStrokeOverlay;
	long strokeFadeDurationMs;
	float strokeBaseRadiusPx;
	int strokeColor;
	private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Map<Integer, StrokePath> activePaths = new HashMap<>();
	private final List<StrokePath> fadingPaths = new ArrayList<>();
	
	// Track stylus side button states
	private boolean stylusButton1Pressed = false;
	private boolean stylusButton2Pressed = false;

	private static final long MIN_STROKE_FADE_DURATION_MS = 100L;
	private static final long DEFAULT_STROKE_FADE_DURATION_MS = 1200L;
	private static final float STROKE_BASE_RADIUS_DP = 4f;
	private static final float STROKE_MIN_STEP_PX = 4f;

	/**
	 * Inner class representing a stroke path with coordinates and fade information.
	 */
	private static final class StrokePath {
		final List<Float> xs = new ArrayList<>();
		final List<Float> ys = new ArrayList<>();
		final List<Float> widths = new ArrayList<>();
		long fadeStartMs = Long.MAX_VALUE; // not fading until set
		long createdMs;
	}


    // setup

    /**
     * Constructs a CanvasView with the given context and attribute set.
     * @param context The context.
     * @param attributeSet The attribute set.
     */
    public CanvasView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        // view is disabled until a MIDI client is set
        setEnabled(false);

        settings = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        settings.registerOnSharedPreferenceChangeListener(this);
        setBackground();
        setInputMethods();
		initStrokeOverlay();
		inRangeStatus = InRangeStatus.OutOfRange;
    }

    /**
     * Sets the MIDI client for sending touch events.
     * @param midiClient The MIDI client to use.
     */
    public void setMidiClient(MidiClient midiClient) {
        this.midiClient = midiClient;
        setEnabled(true);
    }


    // settings

	/**
	 * Sets the background based on grid visibility settings.
	 */
	protected void setBackground() {
		boolean gridVisible = settings.getBoolean(Constants.PreferenceKeys.KEY_GRID_VISIBLE, true);
		if (gridVisible) {
			// Create a drawable with grid pattern and apply opacity
			int opacity = settings.getInt(Constants.PreferenceKeys.KEY_GRID_OPACITY, 30);
			setBackgroundResource(R.drawable.bg_grid_pattern);
			getBackground().setAlpha((int)(opacity * 2.55f)); // Convert 0-100 to 0-255
		} else {
			setBackgroundColor(android.graphics.Color.TRANSPARENT);
		}
	}

    protected void setInputMethods() {
        acceptStylusOnly = settings.getBoolean(Constants.PreferenceKeys.KEY_STYLUS_ONLY, false);
    }

	private void initStrokeOverlay() {
		strokeColor = resolveStrokeColor();
		strokePaint.setStyle(Paint.Style.STROKE);
		strokePaint.setStrokeCap(Paint.Cap.ROUND);
		strokePaint.setStrokeJoin(Paint.Join.ROUND);
		strokePaint.setColor(strokeColor);
		strokeBaseRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, STROKE_BASE_RADIUS_DP, getResources().getDisplayMetrics());
		setStrokeOverlayPreferences();
	}

	private void setStrokeOverlayPreferences() {
		showStrokeOverlay = settings.getBoolean(Constants.PreferenceKeys.KEY_STROKE_OVERLAY_VISIBLE, true);
		strokeFadeDurationMs = settings.getInt(Constants.PreferenceKeys.KEY_STROKE_FADE_DURATION, (int) DEFAULT_STROKE_FADE_DURATION_MS);
		strokeFadeDurationMs = Math.max(strokeFadeDurationMs, MIN_STROKE_FADE_DURATION_MS);
		if (!showStrokeOverlay) {
			activePaths.clear();
			fadingPaths.clear();
			postInvalidateOnAnimation();
		}
	}

	private int resolveStrokeColor() {
		TypedValue value = new TypedValue();
		if (getContext().getTheme().resolveAttribute(android.R.attr.colorAccent, value, true)) {
			return value.data;
		}
		return 0xFFFF4081; // fallback accent-like color
	}

    /**
     * Called when a shared preference is changed.
     * Updates the view settings accordingly.
     * @param sharedPreferences The shared preferences.
     * @param key The key of the changed preference.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.PreferenceKeys.KEY_STYLUS_ONLY:
                setInputMethods();
                break;
            case Constants.PreferenceKeys.KEY_GRID_VISIBLE:
            case Constants.PreferenceKeys.KEY_GRID_OPACITY:
                setBackground();
                break;
			case Constants.PreferenceKeys.KEY_STROKE_OVERLAY_VISIBLE:
			case Constants.PreferenceKeys.KEY_STROKE_FADE_DURATION:
				setStrokeOverlayPreferences();
				break;
        }
    }

	/**
	 * Detect and send stylus side button events
	 * Button 1: BUTTON_STYLUS (secondary stylus button)
	 * Button 2: BUTTON_STYLUS2 (tertiary stylus button)
	 */
	private void detectAndSendSidButtons(MotionEvent event, short x, short y, short pressure) {
		if (event.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
			return;
		}

		// Check BUTTON_STYLUS (side button 1)
		boolean button1Now = event.isButtonPressed(MotionEvent.BUTTON_STYLUS_PRIMARY);
		if (button1Now != stylusButton1Pressed) {
			stylusButton1Pressed = button1Now;
			Log.d(TAG, String.format("Stylus button 1 %s", button1Now ? "pressed" : "released"));
			midiClient.getQueue().add(new MidiEvent(Type.TYPE_BUTTON, x, y, pressure, 1, button1Now));
		}

		// Check BUTTON_STYLUS2 (side button 2)
		boolean button2Now = event.isButtonPressed(MotionEvent.BUTTON_STYLUS_SECONDARY);
		if (button2Now != stylusButton2Pressed) {
			stylusButton2Pressed = button2Now;
			Log.d(TAG, String.format("Stylus button 2 %s", button2Now ? "pressed" : "released"));
			midiClient.getQueue().add(new MidiEvent(Type.TYPE_BUTTON, x, y, pressure, 2, button2Now));
		}
	}


    // drawing

    @Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.i(TAG, "Canvas size changed: " + w + "x" + h + " (before: " + oldw + "x" + oldh + ")");
		maxX = w;
		maxY = h;
	}

	/**
	 * Handles generic motion events, such as hover movements.
	 * @param event The motion event.
	 * @return true if the event was handled, false otherwise.
	 */
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (isEnabled()) {
			for (int ptr = 0; ptr < event.getPointerCount(); ptr++)
				if (!acceptStylusOnly || (event.getToolType(ptr) == MotionEvent.TOOL_TYPE_STYLUS)) {
					short nx = normalizeX(event.getX(ptr)),
							ny = normalizeY(event.getY(ptr)),
							npressure = normalizePressure(event.getPressure(ptr));
					
					// Detect stylus side buttons
					detectAndSendSidButtons(event, nx, ny, npressure);
					
					switch (event.getActionMasked()) {
					case MotionEvent.ACTION_HOVER_MOVE:
						midiClient.getQueue().add(new MidiEvent(Type.TYPE_MOTION, nx, ny, npressure));
						break;
					case MotionEvent.ACTION_HOVER_ENTER:
						inRangeStatus = InRangeStatus.InRange;
						midiClient.getQueue().add(new MidiEvent(Type.TYPE_BUTTON, nx, ny, npressure, -1, true));
						break;
					case MotionEvent.ACTION_HOVER_EXIT:
						inRangeStatus = InRangeStatus.OutOfRange;
						midiClient.getQueue().add(new MidiEvent(Type.TYPE_BUTTON, nx, ny, npressure, -1, false));
						break;
					}
				}
			return true;
		}
		return false;
	}
	
	/**
	 * Handles touch events from the user.
	 * @param event The motion event.
	 * @return true if the event was handled, false otherwise.
	 */
	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		if (isEnabled()) {
			for (int ptr = 0; ptr < event.getPointerCount(); ptr++)
				if (!acceptStylusOnly || (event.getToolType(ptr) == MotionEvent.TOOL_TYPE_STYLUS)) {
					short nx = normalizeX(event.getX(ptr)),
						  ny = normalizeY(event.getY(ptr)),
						  npressure = normalizePressure(event.getPressure(ptr));
					
					// Detect stylus side buttons
					detectAndSendSidButtons(event, nx, ny, npressure);
					
					switch (event.getActionMasked()) {
					case MotionEvent.ACTION_MOVE:
						recordStrokePath(event.getX(ptr), event.getY(ptr), event.getPressure(ptr), event.getPointerId(ptr), false);
						midiClient.getQueue().add(new MidiEvent(Type.TYPE_MOTION, nx, ny, npressure));
						break;
					case MotionEvent.ACTION_DOWN:
						recordStrokePath(event.getX(ptr), event.getY(ptr), event.getPressure(ptr), event.getPointerId(ptr), true);
						if (inRangeStatus == inRangeStatus.OutOfRange) {
							inRangeStatus = inRangeStatus.FakeInRange;
							midiClient.getQueue().add(new MidiEvent(Type.TYPE_BUTTON, nx, ny, (short)0, -1, true));
						}
						midiClient.getQueue().add(new MidiEvent(Type.TYPE_BUTTON, nx, ny, npressure, 0, true));
						break;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						recordStrokePath(event.getX(ptr), event.getY(ptr), event.getPressure(ptr), event.getPointerId(ptr), false);
						midiClient.getQueue().add(new MidiEvent(Type.TYPE_BUTTON, nx, ny, npressure, 0, false));
						if (inRangeStatus == inRangeStatus.FakeInRange) {
							inRangeStatus = inRangeStatus.OutOfRange;
							midiClient.getQueue().add(new MidiEvent(Type.TYPE_BUTTON, nx, ny, (short)0, -1, false));
						}
						finishStrokePath(event.getPointerId(ptr));
						break;
					}
						
				}
			return true;
		}
		return false;
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		super.onDraw(canvas);
		if (!showStrokeOverlay) {
			return;
		}

		long now = SystemClock.uptimeMillis();
		strokePaint.setColor(strokeColor);

		boolean needsMoreFrames = false;

		// Draw active strokes (no fade yet)
		strokePaint.setAlpha(255);
		for (StrokePath path : activePaths.values()) {
			drawStrokePath(canvas, path, 1f);
			needsMoreFrames = true; // active stroke needs redraw for updates
		}

		// Draw fading strokes
		for (int i = fadingPaths.size() - 1; i >= 0; i--) {
			StrokePath path = fadingPaths.get(i);
			long age = now - path.fadeStartMs;
			if (age >= strokeFadeDurationMs) {
				fadingPaths.remove(i);
				continue;
			}
			float alpha = 1f - (float) age / (float) strokeFadeDurationMs;
			strokePaint.setAlpha((int) (alpha * 255));
			drawStrokePath(canvas, path, alpha);
			needsMoreFrames = true;
		}

		if (needsMoreFrames) {
			postInvalidateOnAnimation();
		}
	}

	private void drawStrokePath(Canvas canvas, StrokePath path, float alpha) {
		int size = path.xs.size();
		if (size < 2) {
			return;
		}
		for (int i = 0; i < size - 1; i++) {
			float x1 = path.xs.get(i);
			float y1 = path.ys.get(i);
			float x2 = path.xs.get(i + 1);
			float y2 = path.ys.get(i + 1);
			float width1 = path.widths.get(i);
			float width2 = path.widths.get(i + 1);
			float width = (width1 + width2) * 0.5f; // average width for smoother transition
			strokePaint.setStrokeWidth(width);
			canvas.drawLine(x1, y1, x2, y2, strokePaint);
		}
	}

	private void recordStrokePath(float rawX, float rawY, float pressure, int pointerId, boolean isDown) {
		if (!showStrokeOverlay) {
			return;
		}
		long now = SystemClock.uptimeMillis();
		float clampedPressure = Math.min(Math.max(pressure, 0f), 1f);
		float strokeWidth = strokeBaseRadiusPx * 2f * (0.6f + (0.8f * clampedPressure));

		StrokePath path = activePaths.get(pointerId);
		if (path == null || isDown) {
			path = new StrokePath();
			path.createdMs = now;
			path.xs.add(rawX);
			path.ys.add(rawY);
			path.widths.add(strokeWidth);
			activePaths.put(pointerId, path);
			postInvalidateOnAnimation();
			return;
		}

		// Get last point
		int lastIndex = path.xs.size() - 1;
		float lastX = path.xs.get(lastIndex);
		float lastY = path.ys.get(lastIndex);
		float lastWidth = path.widths.get(lastIndex);

		float dx = rawX - lastX;
		float dy = rawY - lastY;
		float dist = (float) Math.hypot(dx, dy);

		if (dist > STROKE_MIN_STEP_PX) {
			int steps = (int) Math.ceil(dist / STROKE_MIN_STEP_PX);
			for (int i = 1; i <= steps; i++) {
				float t = (float) i / steps;
				float ix = lastX + dx * t;
				float iy = lastY + dy * t;
				float iwidth = lastWidth + (strokeWidth - lastWidth) * t;
				path.xs.add(ix);
				path.ys.add(iy);
				path.widths.add(iwidth);
			}
		} else {
			path.xs.add(rawX);
			path.ys.add(rawY);
			path.widths.add(strokeWidth);
		}
		postInvalidateOnAnimation();
	}

	private void finishStrokePath(int pointerId) {
		StrokePath path = activePaths.remove(pointerId);
		if (path == null) {
			return;
		}
		path.fadeStartMs = SystemClock.uptimeMillis();
		fadingPaths.add(path);
		postInvalidateOnAnimation();
	}
	
	// these overflow and wrap around to negative short values, but thankfully Java will continue
	// on regardless, so we can just ignore Java's interpretation of them and send them anyway.
	short normalizeX(float x) {
		return (short)(Math.min(Math.max(0, x), maxX) * 2*Short.MAX_VALUE/maxX);
	}
	
	short normalizeY(float x) {
		return (short)(Math.min(Math.max(0, x), maxY) * 2*Short.MAX_VALUE/maxY);
	}
	
	short normalizePressure(float x) {
		return (short)(Math.min(Math.max(0, x), 2.0) * Short.MAX_VALUE);
	}

}
