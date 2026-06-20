# 🚗 Rear Collision Alert System

A real-time Android application that helps drivers avoid rear-end collisions by detecting nearby obstacles using the phone's camera and providing visual + voice alerts.

## 📱 Overview

This app uses the rear camera of an Android smartphone to monitor the area behind a vehicle. It estimates the distance of obstacles in real-time and categorizes them into Safe, Warning, and Danger zones, alerting the driver through both an on-screen dashboard and voice notifications.

## ✨ Features

- **Real-time camera feed** using CameraX
- **Live distance estimation** based on image brightness analysis
- **Three-zone alert system:**
  - ✅ **Safe Zone** (distance > 5m) – Green indicator
  - ⚠️ **Warning Zone** (2m – 5m) – Yellow indicator, voice alert
  - 🔴 **Danger Zone** (< 2m) – Red indicator, voice alert
- **Voice alerts** using Android Text-to-Speech (TTS)
- **Modern GUI dashboard** built with Jetpack Compose
- **Frame counter** to monitor live analysis
- **Zone indicator bar** showing current status at a glance

## 🚀 How It Works

1. The app requests camera permission on first launch.
2. The rear camera captures live video frames.
3. Each frame's average brightness is analyzed.
4. Brightness values are mapped to an estimated distance (0–10 meters).
5. Based on the estimated distance, the app updates the zone status (Safe/Warning/Danger).
6. Visual dashboard updates instantly, and voice alerts trigger for Warning and Danger zones (with a 3-second cooldown to avoid repetition).

## ⚙️ Setup & Installation

1. Clone the repository:
```bash
   git clone https://github.com/Anosha-Aamer/RearCollisionAlert.git
```
2. Open the project in **Android Studio**.
3. Let Gradle sync and download dependencies.
4. Connect an Android device (USB Debugging enabled) or use an emulator.
5. Click **Run ▶️** to install and launch the app.

## 📋 Permissions Required

- `android.permission.CAMERA` – to access the rear camera for real-time detection.

## 👤 Author

Anosha Aamer

Email : aameranosha@gmail.com

LinkedIn : https://www.linkedin.com/in/anosha-aamer-a406ba321/

## 📄 License

This project is for educational purposes.
