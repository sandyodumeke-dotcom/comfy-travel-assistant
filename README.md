# Comfy Travel Assistant: cloud APK build

This folder is your existing Comfy Travel Assistant, packaged as a native Android project (Java).
GitHub builds the APK for you. You do not need Android Studio.

## What is in here
- `app/src/main/assets/index.html`: your app, unchanged (screens, features, voice reminders)
- `app/src/main/java/...`: the Android code that shows the app and keeps the repeating voice reminder and Stop button working when the app is closed
- `.github/workflows/build-apk.yml`: the cloud build
- Gradle files, manifest and icons

## Upload to GitHub (once)
1. Unzip this file on the laptop. Open the unzipped folder.
2. Go to github.com, sign in (free account) and click **New repository**. Name it `comfy-travel-assistant`, tick **Add a README file is NOT needed**, leave it empty and click **Create repository**.
3. On the empty repository page click **uploading an existing file**.
4. In the unzipped folder press **Ctrl+A** to select everything inside it, then **drag it all onto the GitHub page**. Upload the CONTENTS of the folder, not the folder itself.
5. Wait until every file is listed. Check that you can see `.github/workflows/build-apk.yml` in the list. Click **Commit changes**.

If the `.github` folder did not upload (some computers hide it): click **Add file, Create new file**, type `.github/workflows/build-apk.yml` as the name (the slashes make the folders), open `build-apk.workflow.yml` from this folder, copy everything into the big box and click **Commit changes**.

## Build the APK
The build starts by itself after you commit. To start it again: **Actions** tab, click **Build Comfy Travel Assistant APK**, then **Run workflow**. If GitHub shows a green button asking to enable workflows, click it.
It takes about 8 to 15 minutes. A green tick means it worked.

## Download the APK
1. Actions tab, click the latest run (top of the list).
2. Scroll to the bottom to **Artifacts** and click **Comfy-Travel-Assistant-APK**. A zip downloads.
3. Unzip it. Inside is `Comfy-Travel-Assistant.apk`.
4. Copy the APK to your phone (cable, Google Drive, WhatsApp) or download the zip on the phone and unzip it with the Files app. Tap the APK and allow "Install unknown apps" when asked.

## If the build shows a red cross
Click the run, click the red step, copy the error text and send it to me. A `build-reports` download is also attached to failed runs.

## Good to know
- The APK runs on Android 7.0 and newer, which includes Android 8.0 (API 26). The build checks this and fails if it is wrong.
- Reminders keep repeating until you press Stop (there is no automatic time limit).
- On Android 12 and newer, Android asks you to allow "Alarms & reminders" for Comfy so reminders ring at the exact minute. Comfy asks once when you tap Start; you can open the same setting later in Settings, then "Allow exact reminder times". Without it, reminders still fire, but Android may delay them by a few minutes.
- This APK is for installing on your phone. Google Play needs a signed release file (AAB), which is a separate step.
- The project does not contain `gradle-wrapper.jar` (a small binary file). The cloud build creates it with the official Gradle command, so nothing is needed from you.
