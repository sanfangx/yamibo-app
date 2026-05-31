# Yamibo App Updater Release Guide

This document describes the manifest used by the in-app updater and the Android APK release flow.

## Manifest

The app checks these manifests in order:

1. `https://raw.githubusercontent.com/LittleSurvival/yamibo-app/main/update/stable.json`
2. `https://gitee.com/LittleSurvival/yamibo-app/raw/main/update/stable.json`
3. `https://gitcode.com/LittleSurvival/yamibo-app/raw/main/update/stable.json`

Keep the same `update/stable.json` file in each mirror. The Gitee/GitCode URLs are intended for users who cannot reliably reach GitHub.

```json
{
  "channel": "stable",
  "versionName": "1.2.0",
  "versionCode": 120,
  "minVersionCode": 100,
  "releaseNotes": "Fixes and improvements.",
  "releaseUrl": "https://github.com/LittleSurvival/yamibo-app/releases/tag/v1.2.0",
  "assets": [
    {
      "platform": "android",
      "type": "universal-apk",
      "url": "https://github.com/LittleSurvival/yamibo-app/releases/download/v1.2.0/yamibo-1.2.0.apk",
      "sha256": "PUT_APK_SHA256_HERE",
      "size": 12345678
    }
  ]
}
```

Fields:

- `versionCode`: the numeric value compared with the installed Android `versionCode`. Increase this for every release.
- `versionName`: display version shown to users.
- `minVersionCode`: optional lower bound for future forced-update policy. The app records it now but does not force updates yet.
- `releaseNotes`: plain text shown in the update screen.
- `releaseUrl`: page opened when the user taps the release-page action.
- `assets`: installable files. Android currently accepts `type = "universal-apk"` or `"apk"`.
- `sha256`: required for production releases. The app verifies it before opening the installer.
- `size`: optional byte count used by the progress UI.

## Android APK Release Flow

1. Update `versionCode` and `versionName` in `composeApp/build.gradle.kts`.
2. Build a release APK:

```powershell
.\gradlew :composeApp:assembleRelease --console=plain
```

3. Sign the APK if the build is not already configured with a release signing config.
4. Calculate SHA-256:

```powershell
Get-FileHash .\composeApp\build\outputs\apk\release\composeApp-release.apk -Algorithm SHA256
```

5. Upload the APK to GitHub Releases.
6. Mirror the APK or at least the manifest to Gitee/GitCode. Mainland-friendly downloads should use URLs reachable without GitHub.
7. Update `update/stable.json` in GitHub, Gitee, and GitCode.
8. Open the app Settings -> App updates -> Check update.

## iOS Update Flow

iOS cannot install an IPA directly from inside a normal App Store/TestFlight app. The app updater therefore only checks the manifest and opens `releaseUrl`.

Recommended options:

- App Store: use an App Store product URL as `releaseUrl`.
- TestFlight: use the TestFlight invite/app URL.
- Enterprise/MDM/internal distribution: use the organization's HTTPS landing page. The actual install must be handled by Safari, MDM, or Apple's enterprise install flow.

## Mirror Maintenance

Recommended mirror URLs:

- GitHub: `https://github.com/LittleSurvival/yamibo-app`
- Gitee: `https://gitee.com/LittleSurvival/yamibo-app`
- GitCode: `https://gitcode.com/LittleSurvival/yamibo-app`

If the mirror owner/name changes, update `DefaultAppUpdateRepository.sources` and this document together.
