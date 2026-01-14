# StylusSync

Turn your Android device into a digitizer for your computer.

## Project Overview

- Client (Python): Handles receiving/forwarding input to a virtual device or MIDI. Main code is in `client/`.
- Android App: Located in `app-android/`, an Android Studio project.
- Windows VHF Driver: Not implemented yet.
- Protocol specification: See `protocol.md`.

## How to Use

### Android App

You can find prebuilt APKs in Actions artifacts (or build the Android app yourself).

### Client

1. It's recommended to use a virtual environment:

```bash
python3 -m venv .venv
source .venv/bin/activate
```

2. Install dependencies (Linux only for now):

```bash
pip install -r client/requirements-linux.txt
```

3. Run the client:

```bash
python -m client
# or
python client/main.py
```

## Build & Run (Android)

- Open `app-android/` in Android Studio and build/install as usual.
- The Android app communicates with the Python client using the protocol defined in the project documentation; see `protocol.md` for details.

## Development & Debugging

- Read `protocol.md` before developing to understand the message format.
- The Python code follows standard formatting and linting practices; use a local virtual environment for testing.

## TODO

- [ ] Implement Windows driver.
- [ ] Screen mirroring.
- [ ] ...

## Credits

Many thanks to [GfxTablet](https://github.com/rfc2822/GfxTablet) for inspirations and Android app framework.
