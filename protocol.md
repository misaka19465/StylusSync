# MIDI Protocol Design Document - StylusSync

## Overview

StylusSync uses the MIDI protocol, registering the Android device as a MIDI input device and communicating with the computer via USB.

## MIDI Protocol Mapping Scheme

### Data Transmission Requirements

The original network protocol needs to transmit the following data:

- **X Coordinate**: 0-65535 (16-bit)
- **Y Coordinate**: 0-65535 (16-bit)
- **Pressure**: 0-65535 (16-bit, actually using 0-32768)
- **Event Type**: Motion(0) or Button(1)
- **Button ID**: -1 to 2
- **Button State**: Down(1) or Up(0)

### MIDI Message Design

Uses MIDI Control Change (CC) messages for data transmission. Each CC message contains:

- **Status Byte**: 0xB0-0xBF (Control Change, channel 0-15)
- **Controller Number**: 0-127 (7-bit)
- **Data Value**: 0-127 (7-bit)

#### Channel Assignment

- **Channel 1**: Coordinate and pressure data
- **Channel 2**: Button and event control

#### Controller Mapping

##### Channel 1 - Coordinates and Pressure

```
CC 1:  X coordinate high 7 bits (MSB) - bits 15-9
CC 2:  X coordinate mid 7 bits        - bits 8-2
CC 3:  X coordinate low 2 bits (LSB)  - bits 1-0 (left-shifted 5 bits for padding)

CC 4:  Y coordinate high 7 bits (MSB) - bits 15-9
CC 5:  Y coordinate mid 7 bits        - bits 8-2
CC 6:  Y coordinate low 2 bits (LSB)  - bits 1-0 (left-shifted 5 bits for padding)

CC 7:  Pressure high 7 bits (MSB)     - bits 15-9
CC 8:  Pressure mid 7 bits            - bits 8-2
CC 9:  Pressure low 2 bits (LSB)      - bits 1-0 (left-shifted 5 bits for padding)

CC 10: Packet sequence number         - 0-127 cyclic, for packet loss detection
```

##### Channel 2 - Control

```
CC 20: Event type
       0   = Motion Event (hover)
       1   = Button Event (touch)
       127 = Heartbeat/keepalive message

CC 21: Button ID
       0   = Button -1 (stylus in range)
       1   = Button 0 (left click/stylus contact)
       2   = Button 1 (extra button 1)
       3   = Button 2 (extra button 2)

CC 22: Button state
       0   = Button released (Up)
       127 = Button pressed (Down)

CC 30: Data complete flag
       127 = Current data packet complete, receiver can process
```

### Data Transmission Flow

#### Motion Event Transmission Sequence

```
1. [CH2] CC 20 = 0          (Set event type to Motion)
2. [CH1] CC 1  = X_MSB      (X coordinate high bits)
3. [CH1] CC 2  = X_MID      (X coordinate mid bits)
4. [CH1] CC 3  = X_LSB      (X coordinate low bits)
5. [CH1] CC 4  = Y_MSB      (Y coordinate high bits)
6. [CH1] CC 5  = Y_MID      (Y coordinate mid bits)
7. [CH1] CC 6  = Y_LSB      (Y coordinate low bits)
8. [CH1] CC 7  = P_MSB      (Pressure high bits)
9. [CH1] CC 8  = P_MID      (Pressure mid bits)
10.[CH1] CC 9  = P_LSB      (Pressure low bits)
11.[CH1] CC 10 = SEQ        (Sequence number)
12.[CH2] CC 30 = 127        (Data complete flag)
```

#### Button Event Transmission Sequence

```
1. [CH2] CC 20 = 1          (Set event type to Button)
2. [CH1] CC 1  = X_MSB
3. [CH1] CC 2  = X_MID
4. [CH1] CC 3  = X_LSB
5. [CH1] CC 4  = Y_MSB
6. [CH1] CC 5  = Y_MID
7. [CH1] CC 6  = Y_LSB
8. [CH1] CC 7  = P_MSB
9. [CH1] CC 8  = P_MID
10.[CH1] CC 9  = P_LSB
11.[CH2] CC 21 = BUTTON_ID  (Button ID)
12.[CH2] CC 22 = BTN_STATE  (Button state)
13.[CH1] CC 10 = SEQ        (Sequence number)
14.[CH2] CC 30 = 127        (Data complete flag)
```

### Encoding 16-bit Values to Three 7-bit Values

```java
// Encode 16-bit value to 3 MIDI 7-bit values
short value = 42536; // Example: 0-65535

int msb = (value >> 9) & 0x7F;   // High 7 bits: bits 15-9
int mid = (value >> 2) & 0x7F;   // Mid 7 bits: bits 8-2
int lsb = (value << 5) & 0x7F;   // Low 2 bits: bits 1-0, left-shifted 5 bits

// Decode back to 16-bit value
int decoded = ((msb & 0x7F) << 9) | 
              ((mid & 0x7F) << 2) | 
              ((lsb & 0x7F) >> 5);
```

### Button ID Mapping

```
Original  ->  MIDI   Description
-1        ->  0      Stylus in range pseudo-button
0         ->  1      Left click / Stylus contact
1         ->  2      Extra button 1
2         ->  3      Extra button 2
```

## Android MIDI Implementation

### MIDI Device Registration

```java
MidiManager midiManager = (MidiManager) getSystemService(Context.MIDI_SERVICE);
MidiDeviceInfo deviceInfo = midiManager.getDevicesForTransport(
    MidiManager.TRANSPORT_MIDI_BYTE_STREAM)[0];
```

### Permission Requirements

```xml
<uses-feature android:name="android.software.midi" android:required="true"/>
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
```

## Computer-side Reception

On Linux, StylusSync can create a virtual input device via `python-evdev` to emit standard stylus events:

### Linux Input Mapping

- Event Type: Motion → `EV_ABS` updates for `ABS_X`, `ABS_Y`, `ABS_PRESSURE`
- Button `-1` (in-range) → `BTN_TOOL_PEN` (Down/Up)
- Button `0` (contact) → `BTN_TOUCH` (Down/Up)
- Button `1` (side button 1) → `BTN_STYLUS` (Down/Up)
- Button `2` (side button 2) → `BTN_STYLUS2` (Down/Up)

The virtual device exposes these absolute axes ranges:

- `ABS_X`: 0–65535
- `ABS_Y`: 0–65535
- `ABS_PRESSURE`: 0–32768

Permissions note: creating a uinput device may require root or appropriate udev rules.

## Implementation Notes

1. **Message Order**: Must be sent in sequence to ensure proper reassembly at receiver
2. **Sequence Number**: Used for packet loss detection and duplicate data
3. **Heartbeat Mechanism**: Periodically send heartbeats to keep connection active
4. **Buffering**: Sender uses queue buffering to avoid blocking UI thread
5. **Error Handling**: Graceful degradation when MIDI device disconnects

## Version History

- **Version 1.0**: Initial MIDI protocol design (2026-01-12)
