# Nagram X
[![Crowdin](https://badges.crowdin.net/NagramX/localized.svg)](https://crowdin.com/project/NagramX)  
A variant of [Nagram](https://github.com/NextAlone/Nagram) with additional features.

## Download

Latest versions are available through:
* [Telegram Channel](https://t.me/NagramX) (Latest Beta)
* [GitHub Actions](https://github.com/shitianyaa/NagramX/actions) (CI Artifacts)
* [GitHub Releases](https://github.com/shitianyaa/NagramX/releases) (Published Builds)

## Verify APK

Release APKs use the following application identity:

* Application ID: `com.shitianyaa.nagramx`
* Supported ABI: `arm64-v8a`

The release keystore is not stored in this repository. Verify a downloaded APK
against the checksum attached to the same GitHub Release, then inspect its
certificate before installing:

```bash
sha256sum -c NagramX-*.apk.sha256
apksigner verify --print-certs NagramX-*.apk
```

Do not rely on certificate fingerprints copied from older Nagram/NagramX
packages; the package name and signing certificate must both match the release
you intend to install.

## Compilation Guide

1. Clone the repository with its submodules:

    ```bash
    git clone --recursive --shallow-submodules https://github.com/shitianyaa/NagramX.git NagramX
    ```

    If you already cloned the repository without submodules, run:

    ```bash
    git submodule update --init --recursive --depth=1
    ```

2. Obtain API credentials (`TELEGRAM_APP_ID` and `TELEGRAM_APP_HASH`) from [Telegram Developer Portal](https://my.telegram.org/auth). Create `local.properties` in the project root with:

   ```properties
   TELEGRAM_APP_ID=<your_telegram_app_id>
   TELEGRAM_APP_HASH=<your_telegram_app_hash>
   ```

3. For APK signing: Place your keystore at `TMessagesProj/release.keystore`
   and add signing configuration to `local.properties`:

   ```properties
   KEYSTORE_PASS=<your_keystore_password>
   ALIAS_NAME=<your_alias_name>
   ALIAS_PASS=<your_alias_password>
   ```

4. For FCM support: Replace `TMessagesProj/google-services.json` with your own configuration file.

5. Replace project-specific metadata:

    - Set your Google Maps API key in the `com.google.android.maps.v2.API_KEY` meta-data entry in `TMessagesProj/src/main/AndroidManifest.xml`.
    - Set `BaseRemoteHelper.CHANNEL_METADATA_ID` in `TMessagesProj/src/main/java/tw/nekomimi/nekogram/helpers/remote/BaseRemoteHelper.java` to your metadata channel's numeric ID, without the `-100` prefix.

6. Open the project in Android Studio to start building.

## GitHub Actions Build

1. Prepare your signing keystore and configure `local.properties` with the following:

   ```properties
   KEYSTORE_PASS=<your_keystore_password>
   ALIAS_NAME=<your_alias_name>
   ALIAS_PASS=<your_alias_password>
   TELEGRAM_APP_ID=<your_telegram_app_id>
   TELEGRAM_APP_HASH=<your_telegram_app_hash>
   ```

   Base64 encode the contents of this file.

2. Configure GitHub Action secrets:
   - `LOCAL_PROPERTIES`: Base64-encoded content from step 1
   - `RELEASE_KEYSTORE_BASE64`: Base64-encoded content of `TMessagesProj/release.keystore`

   > `TMessagesProj/release.keystore` is not committed to the repository. The
   > workflow restores it from `RELEASE_KEYSTORE_BASE64` before signing.

3. Use `APP_VERSION_NAME`, `APP_VERSION_CODE`, and `APP_PACKAGE` in
   `gradle.properties` as the single source of truth for release metadata.

4. The release channels are:
   - `dev`: a version-field change builds a signed Staging APK and publishes an
     optional prerelease tag such as `v12.9.2-6992-staging`.
   - `canary`: every push builds a signed Canary artifact without publishing a
     GitHub Release.
   - `main`: a version-field change builds a signed Release APK and publishes a
     stable tag such as `v12.9.2-6992`.
   - pull requests: build a debug-signed APK for validation only.

   Staging and Canary `versionName` values include the short commit ID. Stable
   Release builds keep the exact `APP_VERSION_NAME`. APKs use the form
   `NagramX-<versionName>-<versionCode>-arm64-v8a.apk` and are published with a
   matching `.sha256` checksum.

5. Add an optional curated release note at
   `release-notes/v<versionName>-<versionCode>.md`. When it is absent, the
   workflow generates user-facing notes from Conventional Commit messages and
   excludes internal `ci`, `chore`, `docs`, `build`, and test-only commits.

## Acknowledgments

- [AyuGram](https://github.com/AyuGram/AyuGram4A)
- [Cherrygram](https://github.com/arsLan4k1390/Cherrygram)
- [Dr4iv3rNope](https://github.com/Dr4iv3rNope/NotSoAndroidAyuGram)
- [exteraGram](https://github.com/exteraSquad/exteraGram)
- [Nagram](https://github.com/NextAlone/Nagram)
- [Nekogram](https://github.com/Nekogram/Nekogram)
- [OctoGram](https://github.com/OctoGramApp/OctoGram)
