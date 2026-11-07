# Comfy Travel Assistant: Android Studio guide

This folder is a complete Android project. Nothing else needs installing except Android Studio.
It has no Node, no Capacitor and no extra tools.

## 1. Before you start (on the laptop)
- Android Studio (latest stable) from developer.android.com/studio
- About 10 GB of free disk space and an internet connection (the first open downloads Gradle and the Android SDK)
- Close the browser and other programs. Your friend's laptop has 4 GB of RAM, so give Studio all of it.

## 2. Make Android Studio lighter (4 GB laptop)
- This project already uses low-memory Gradle settings (see gradle.properties).
- Do **not** create an emulator. Build the APK and install it on your phone instead.
- In Studio: File, then Power Save Mode (turns off background checks).
- Help, then Change Memory Settings: set the IDE to 1280 MB.
- The first sync can take 10 to 25 minutes on a slow laptop. Be patient.

## 3. Open and build a test APK
1. Unzip comfy-android-studio.zip. Start Android Studio, choose **Open** and pick the **comfy-android-studio** folder. Tap Trust Project.
2. Wait for "Gradle sync" to finish (progress bar at the bottom). If Studio offers to install **Android SDK Platform 36** or to update something, accept.
3. Menu: **Build, Build Bundle(s) / APK(s), Build APK(s)**.
4. When it finishes, click **locate** in the popup. The file is `app/build/outputs/apk/debug/app-debug.apk`.
5. Send it to your phone (USB cable, Google Drive, WhatsApp or email). Tap it on the phone and allow "install unknown apps".

## 4. Set up the phone
Open Comfy, then Settings and tap **Allow notifications**, **Let Comfy run in the background** (choose Comfy, then Unrestricted) and **Test a real reminder now**. Then make a reminder 2 minutes ahead, lock the phone and wait.

## 5. Make the file for the Play Store (AAB)
1. Menu: **Build, Generate Signed App Bundle / APK**, choose **Android App Bundle**, Next.
2. Under Key store path choose **Create new**. Pick a safe place and a name such as comfy-upload.jks. Write down the password and the key alias. Fill in your name. OK.
3. **Back up the .jks file and passwords** (email them to yourself). If you lose them you cannot update the app easily.
4. Next, choose **release**, then Create. The file is `app/release/app-release.aab`.
5. Upload that .aab in Play Console (Testing, Internal testing, Create release).

## 6. Updating the app later
Open app/build.gradle and raise `versionCode` by 1 (and change `versionName`). Build the AAB again with the same keystore.
`targetSdk` is 36. If Play Console asks for a newer level, change targetSdk and compileSdk to the number it asks for.

## 7. If something goes wrong
- "Gradle sync failed" about the Android Gradle Plugin: click the **Upgrade** or **Update** link Studio shows, accept, then sync again.
- Wrong JDK error: File, Settings, Build Tools, Gradle, set **Gradle JDK** to "jbr-21" or "Embedded JDK".
- Gradle download stuck: check the internet, then File, Sync Project with Gradle Files.
- Build error: copy the red error text (View, Tool Windows, Build) and send it to me.

## 8. Play Store files
In the store folder: STORE_LISTING.md (text to paste, data-safety notes and the foreground-service declaration), PRIVACY_POLICY.md (replace [DATE] and [YOUR EMAIL ADDRESS]), icon-512.png and feature-graphic-1024x500.png. Add 2 to 8 phone screenshots from your own phone.

## What is inside
- app/src/main/assets/index.html: the whole app (screens, voice, tools)
- app/src/main/java/.../MainActivity.java: shows the app, handles dialogs and links
- ComfyBridge, AlarmStore, AlarmReceiver, SpeakService, BootReceiver: the alarm and voice code that lets her speak when the app is closed
