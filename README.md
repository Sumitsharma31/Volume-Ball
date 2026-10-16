# Volume Ball 🕹️🔊
**The Modern Floating Game-Joystick Solution for Android Volume Control**

[![Platform](https://img.shields.io/badge/Platform-Android_6.0+_(API_23+)-brightgreen.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Version](https://img.shields.io/badge/Version-1.0_Release-orange.svg)](https://github.com/Sumitsharma31/Volume-Ball/releases)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

---

## 💡 The Lifesaver for Damaged Hardware Buttons

Are your phone's physical volume buttons **broken, stuck, unresponsive, or worn out from years of use**? 

Physical volume rockers on smartphones are mechanical parts that frequently suffer from:
- **Wear and tear** from constant clicking every day.
- **Accidental drops** damaging internal button flex cables or switches.
- **Dust, dirt, or liquid ingress** causing sticky or erratic volume changes.
- **Costly repairs** that require opening up the phone display or motherboard.

**Volume Ball** provides the ultimate software replacement: an interactive, ultra-responsive **floating game joystick** on your screen that lets you effortlessly control media, phone call, and ringtone volume with smooth thumbstick gestures — **without ever touching your physical hardware buttons again!**

---

## ✨ Features at a Glance

### 🕹️ Interactive Game Joystick Control
- **Drag Up or Right**: Raise volume (`+`)
- **Drag Down or Left**: Lower volume (`−`)
- **Hold in Direction**: Smoothly glides volume continuously
- **Spring-Back Return**: Fluid physics animation returns the thumbstick knob to center when released

### 📞 Automatic Call Volume & Stream Routing
- **In-Call Audio Support**: When you are in a cellular phone call or VoIP call (**WhatsApp, Telegram, Google Meet, Zoom**), the ball automatically switches to **Call Volume** (`STREAM_VOICE_CALL`) and displays a **`📞`** icon!
- **Ringtone Mode**: When receiving an incoming call, it automatically switches to **Ringtone Volume** (`STREAM_RING`) with a **`🔔`** icon!
- **Media Mode**: When listening to music or watching videos, it controls **Media Volume** (`STREAM_MUSIC`).

### 🛡️ Accidental Touch Prevention
- Quick taps, brushes, or accidental clicks on the ball are completely ignored — no annoying unexpected volume bar popups while gaming, browsing, or watching full-screen videos.
- Sound adjustments only trigger when you deliberately push the joystick past the deadzone.

### 📍 Snappy Reposition Mode
- Need to move the ball? Just **press & hold for ~350ms** until you feel the haptic pulse and see the golden dashed ring.
- Drag the ball anywhere on your screen — on release, it smoothly snaps to the nearest edge and remembers its position across reboots!

### 🎨 4 High-Tech Ball Styles
- **🕹️ Joystick**: Classic tactile cyber gamepad thumbstick with directional indicators.
- **⚡ Neon Glow**: Cyberpunk luminous neon rings with reticle crosshairs.
- **🫧 Minimal Glass**: Translucent frosted glass effect for clean, unobtrusive viewing.
- **👾 Retro Arcade**: Nostalgic 90s arcade cabinet ball-top controller.

### ❖ 4 Inside Icon Sets
Customize the icon that sits inside the ball:
- **❖ Minimal Geometric**: `○` (Muted), `●` (Low), `❖` (Medium), `✦` (High)
- **🔊 Classic Volume**: `🔇`, `🔈`, `🔉`, `🔊`
- **🎮 Cyber Gamer**: `💤`, `⚡`, `🎮`, `👾`
- **🎧 Music Wave**: `•`, `♪`, `♫`, `🎧`

### 🌈 Vibrant Palette & Customization
- **6 Accent Colors**: Cyber Purple, Electric Blue, Emerald Green, Sunset Orange, Crimson Red, Golden Yellow.
- **Size Adjustment**: 48 dp to 88 dp slider.
- **Opacity Slider**: 25% to 100% transparency.
- **System Theme Support**: Adapts automatically to your Android system's Light or Dark mode.

### ☕ Buy Developer a Coffee
- Built-in one-tap UPI support (`ssumit20kr@ybl`) with preset chips (**₹49, ₹99, ₹199, ₹499**) allowing supporters to fund future updates directly via Google Pay, PhonePe, Paytm, or BHIM.

---

## 📲 Download & Installation

### Option 1: Direct APK Download
Grab the pre-built, signed release APK directly from this repository:
👉 **[Download VolumeBall-v1.0.apk](release/VolumeBall-v1.0.apk)**

### Option 2: Build from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/Sumitsharma31/Volume-Ball.git
   cd Volume-Ball
   ```
2. Build debug or release APK using Gradle:
   ```bash
   ./gradlew assembleRelease
   ```
3. Locate the output APK at `app/build/outputs/apk/release/app-release.apk`.

### Permission Required
- **Display over other apps (`SYSTEM_ALERT_WINDOW`)**: Required to display the floating joystick ball over your other apps and games. The app will guide you to enable this permission on first launch.

---

## 🛠️ Architecture & Tech Stack

- **Language**: 100% Kotlin
- **Min SDK**: Android 6.0 Marshmallow (API 23)
- **Target SDK**: Android 15 (API 35)
- **Overlay Rendering**: Native Android `WindowManager` with hardware-accelerated custom Canvas drawing (`JoystickBallView`)
- **Audio Routing**: Real-time `AudioManager` suggested stream routing matching physical hardware volume rockers
- **Observer Pattern**: `ContentObserver` + system `VOLUME_CHANGED_ACTION` BroadcastReceiver for instantaneous two-way synchronization

---

## 👨‍💻 Creator & Maintainer

Created with passion by **Sumit Sharma**:

- 🐙 **GitHub**: [@Sumitsharma31](https://github.com/Sumitsharma31)
- 💼 **LinkedIn**: [Sumit Sharma](https://www.linkedin.com/in/sumitsharma31)
- 🌐 **Portfolio**: [sumit.dpdns.org](https://sumit.dpdns.org/)
- 📸 **Instagram**: [@sumit.sharma31](https://www.instagram.com/sumit.sharma31/)

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) - feel free to use, modify, and distribute.
