# SLA Task Notifier - Android App

Enterprise Android WebView application designed for high-priority SLA task monitoring with custom "Tingin" alerts, screen wakeup, multi-system URL configuration, and offline repeating alert tracking.

---

## 🛠️ How to Build the APK (Command Line)

### Local Gradle CLI Build:
Run the following command in the project root:

```bash
./gradlew assembleDebug
```

On Windows Command Prompt / PowerShell:
```cmd
gradlew.bat assembleDebug
```

The compiled APK will be output to:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 App Features & Usage

### 1. Dynamic URL Setup & Switching
- When the app is launched for the first time, it prompts for the target System URL (e.g. `https://crm.company.com`).
- To change the URL at any time, tap the **Settings (⚙️)** icon in the top right menu of the App Bar.

### 2. High-Priority SLA Alert & "Tingin" Sound
- High-priority notification channel (`sla_high_priority_channel`) configured with `IMPORTANCE_MAX`.
- Automatically wakes up the phone screen (`PowerManager.SCREEN_BRIGHT_WAKE_LOCK` + `setFullScreenIntent`) when an SLA task alert triggers—even if locked or screen is OFF.
- Built-in dynamic sound tone generator creates a high-clarity 2-tone "Ting-In!" chime audio file.

### 3. Offline Unacknowledged Task Monitoring (Repeating Alert)
- Unacknowledged tasks are stored in local offline storage (`TaskManager`).
- An `AlarmManager` repeating timer re-rings the "Tingin" alert every 60 seconds until the user acknowledges the task or opens the item in WebView.

---

## 🌐 Web App Integration (JavaScript Bridge)

If you own the remote web app loaded inside the WebView, you can trigger native SLA alerts or acknowledge tasks directly from JavaScript using `window.AndroidBridge`:

```javascript
// Trigger a new high-priority SLA alert with Tingin tone
if (window.AndroidBridge) {
    window.AndroidBridge.triggerAlert(
        "TASK_1029",                     // Task ID
        "Urgent Support Ticket #1029",    // Title
        "Customer SLA breach in 3 mins"   // Message
    );
}

// Acknowledge a task (stops repeating alert)
if (window.AndroidBridge) {
    window.AndroidBridge.acknowledgeTask("TASK_1029");
}

// Acknowledge all pending tasks
if (window.AndroidBridge) {
    window.AndroidBridge.acknowledgeAllTasks();
}
```

---

## 🚀 GitHub Actions Automated APK Build Setup

If you want GitHub to automatically build your `.apk` whenever you push code, create `.github/workflows/build-apk.yml`:

```yaml
name: Build Android APK

on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Build Debug APK
        run: ./gradlew assembleDebug

      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: SlaTaskNotifier-APK
          path: app/build/outputs/apk/debug/app-debug.apk
```
