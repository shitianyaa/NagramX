# Nagram X
[![Crowdin](https://badges.crowdin.net/NagramX/localized.svg)](https://crowdin.com/project/NagramX)  
A variant of [Nagram](https://github.com/NextAlone/Nagram) with additional features.

## Download

Latest versions are available through:
* [Telegram Channel](https://t.me/NagramX) (Latest Beta)
* [GitHub Actions](https://github.com/risin42/NagramX/actions/workflows/staging.yml) (CI Artifacts)
* [GitHub Releases](https://github.com/risin42/NagramX/releases) (Latest Stable)

## Verify APK

Official APKs use the following Android signing certificate:

* Package name: `nu.gpu.nagram` / `nu.gpu.nagramx` (base version)
* SHA-256: `0D:51:91:56:E8:0C:91:8C:28:C4:80:BF:D1:3F:31:6A:3B:3B:F7:22:DB:53:2F:AB:74:66:0E:C8:E5:C5:06:A1`

## Compilation Guide

1. Clone the repository with its submodules:

    ```bash
    git clone --recursive --shallow-submodules https://github.com/risin42/NagramX.git NagramX
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

3. Push to `dev` with a change to `APP_VERSION_CODE`/`APP_VERSION_NAME` in
   `gradle.properties` (or the version fields in `TMessagesProj/build.gradle`).
   The Staging workflow then:
   - builds an `arm64-v8a` APK,
   - uploads it as a run artifact,
   - publishes it as a prerelease `<versionName>.<versionCode>` (e.g.
     `12.9.2.6992`) on GitHub Releases, with a generated Chinese changelog
     of the commits since the previous release (run inputs can toggle
     `publish_release` for manual dispatches).

   The Canary/Release workflows follow the same signing setup but are only
   triggered on the `canary`/`main` branches.

## Acknowledgments

- [AyuGram](https://github.com/AyuGram/AyuGram4A)
- [Cherrygram](https://github.com/arsLan4k1390/Cherrygram)
- [Dr4iv3rNope](https://github.com/Dr4iv3rNope/NotSoAndroidAyuGram)
- [exteraGram](https://github.com/exteraSquad/exteraGram)
- [Nagram](https://github.com/NextAlone/Nagram)
- [Nekogram](https://github.com/Nekogram/Nekogram)
- [OctoGram](https://github.com/OctoGramApp/OctoGram)
