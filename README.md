# Glyph Studio — Nothing Phone (2a)

An independent Android app for manual control and animation of the Nothing Phone (2a) Glyph Interface.

## Features
- Individual C1–C24 controls, plus B and A (26 zones total)
- Apply selected zones, all-on and all-off
- Chase, bounce, pulse, pairs and fill animations
- Adjustable animation speed
- Save and load a custom 26-zone pattern locally

## Build and download
GitHub Actions builds a debug APK on each push to `main`. Open the repository's **Actions** tab, select the latest **Glyph Studio Android APK** run, and download the `GlyphStudio-debug-apk` artifact.

The workflow fetches the official Nothing Glyph SDK from the Nothing Developer Programme repository during the build; the vendor SDK binary is not committed here.

## Device setup
- Requires a Nothing Phone (2a) running Android 14 or newer.
- The official SDK only permits foreground app use.
- For development, enable Glyph debug mode from a computer with ADB:
  `adb shell settings put global nt_glyph_interface_debug_enable 1`
- Nothing says this debug mode automatically expires after 48 hours.
- Glyph LEDs are monochrome; this app controls zones and effects, not RGB colours.

## Zone mapping
C1–C24 map to SDK channels 0–23, B maps to 24, and A maps to 25.

Independent community project; not affiliated with or endorsed by Nothing Technology Limited.
