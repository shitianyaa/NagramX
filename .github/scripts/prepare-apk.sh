#!/usr/bin/env bash

set -euo pipefail

APK_DIR=${1:?Usage: prepare-apk.sh <apk-directory> [exact|commit-suffixed|debug]}
VERSION_MODE=${2:-exact}

version_name=$(sed -n 's/^APP_VERSION_NAME=//p' gradle.properties | head -n 1)
version_code=$(sed -n 's/^APP_VERSION_CODE=//p' gradle.properties | head -n 1)
expected_package=$(sed -n 's/^APP_PACKAGE=//p' gradle.properties | head -n 1)

if [[ -z "$version_name" || -z "$version_code" || -z "$expected_package" ]]; then
  echo "::error::Unable to read APP_VERSION_NAME, APP_VERSION_CODE, or APP_PACKAGE"
  exit 1
fi

mapfile -t apk_files < <(find "$APK_DIR" -maxdepth 1 -type f -name 'NagramX-*.apk' -print)
if [[ ${#apk_files[@]} -ne 1 ]]; then
  echo "::error::Expected exactly one APK in $APK_DIR, found ${#apk_files[@]}"
  exit 1
fi

apk_file=${apk_files[0]}
apk_name=$(basename "$apk_file")
case "$VERSION_MODE" in
  exact)
    expected_name="NagramX-${version_name}-${version_code}-arm64-v8a.apk"
    [[ "$apk_name" == "$expected_name" ]] || {
      echo "::error::Unexpected APK name: $apk_name (expected $expected_name)"
      exit 1
    }
    ;;
  commit-suffixed)
    [[ "$apk_name" == NagramX-"${version_name}"-*-"${version_code}"-arm64-v8a.apk ]] || {
      echo "::error::Unexpected APK name: $apk_name"
      exit 1
    }
    ;;
  debug)
    expected_name="NagramX-${version_name}-${version_code}-arm64-v8a-debug.apk"
    [[ "$apk_name" == "$expected_name" ]] || {
      echo "::error::Unexpected APK name: $apk_name (expected $expected_name)"
      exit 1
    }
    ;;
  *)
    echo "::error::Unsupported version validation mode: $VERSION_MODE"
    exit 1
    ;;
esac

aapt_path=$(find "$ANDROID_HOME/build-tools" -maxdepth 2 -type f -name aapt -print | sort -V | tail -n 1)
if [[ -z "$aapt_path" ]]; then
  echo "::error::Unable to locate aapt"
  exit 1
fi

package_info=$("$aapt_path" dump badging "$apk_file" | sed -n '1p')
actual_package=$(sed -n "s/^package: name='\([^']*\)'.*/\1/p" <<<"$package_info")
actual_version_code=$(sed -n "s/.* versionCode='\([^']*\)'.*/\1/p" <<<"$package_info")
actual_version_name=$(sed -n "s/.* versionName='\([^']*\)'.*/\1/p" <<<"$package_info")

if [[ "$actual_package" != "$expected_package" ]]; then
  echo "::error::APK package is $actual_package, expected $expected_package"
  exit 1
fi
if [[ "$actual_version_code" != "$version_code" ]]; then
  echo "::error::APK versionCode is $actual_version_code, expected $version_code"
  exit 1
fi
if [[ "$VERSION_MODE" == "exact" || "$VERSION_MODE" == "debug" ]]; then
  [[ "$actual_version_name" == "$version_name" ]] || {
    echo "::error::APK versionName is $actual_version_name, expected $version_name"
    exit 1
  }
else
  [[ "$actual_version_name" == "$version_name"-* ]] || {
    echo "::error::APK versionName is $actual_version_name, expected ${version_name}-<commit>"
    exit 1
  }
fi

mapfile -t packaged_abis < <(unzip -Z1 "$apk_file" | sed -n 's#^lib/\([^/]*\)/.*#\1#p' | sort -u)
if [[ ${#packaged_abis[@]} -ne 1 || "${packaged_abis[0]}" != "arm64-v8a" ]]; then
  echo "::error::APK contains unexpected native ABIs: ${packaged_abis[*]:-none}"
  exit 1
fi

checksum_file="${apk_file}.sha256"
(
  cd "$(dirname "$apk_file")"
  sha256sum "$apk_name" > "$(basename "$checksum_file")"
)

echo "Verified $apk_name ($actual_package, $actual_version_name, $actual_version_code, arm64-v8a)"
if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "apk_path=$apk_file"
    echo "checksum_path=$checksum_file"
    echo "apk_name=$apk_name"
  } >> "$GITHUB_OUTPUT"
fi
