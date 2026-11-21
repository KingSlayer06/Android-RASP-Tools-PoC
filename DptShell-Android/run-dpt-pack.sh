#!/usr/bin/env bash
#
# dpt-shell PoC packer runner
# -----------------------------------------------------------------------------
# Downloads the official prebuilt dpt-shell packer, then packs your demo APK
# under several runtime-check configurations so you can compare them.
#
# Usage:
#   cd "~/Developer/Android Apps/Android RASP Tools PoC/DptShell-Android"
#   bash run-dpt-pack.sh            # uses app/build/outputs/apk/debug/app-debug.apk
#   bash run-dpt-pack.sh /path/to/some.apk
#
set -uo pipefail

export JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null)"
echo "JAVA_HOME=$JAVA_HOME"

HERE="$(cd "$(dirname "$0")" && pwd)"
APK="${1:-$HERE/app/build/outputs/apk/debug/app-debug.apk}"
PK="$HERE/packer"
OUT="$HERE/shielded"
mkdir -p "$PK" "$OUT"

# 1) Fetch the official prebuilt packer if we don't have it yet.
DPT="$(find "$PK" -name dpt.jar 2>/dev/null | head -1)"
if [ -z "$DPT" ]; then
  echo "== Discovering latest dpt-shell release asset =="
  api="https://api.github.com/repos/luoyesiqiu/dpt-shell/releases/latest"
  meta="$(curl -fsSL -H 'Accept: application/vnd.github+json' "$api")"
  url="$(printf '%s' "$meta" | grep -oE 'https://[^"]+\.zip' | grep -i 'dpt-shell' | head -1)"
  [ -z "$url" ] && url="$(printf '%s' "$meta" | grep -oE 'https://[^"]+\.zip' | head -1)"
  if [ -z "$url" ]; then
    echo "ERROR: no .zip asset found in the latest release. Download URLs seen:"
    printf '%s' "$meta" | grep -oE 'https://[^"]+' | grep -i download
    exit 1
  fi
  echo "Asset: $url"
  curl -fL -o "$PK/dpt-release.zip" "$url" || { echo "Download failed"; exit 1; }
  unzip -o "$PK/dpt-release.zip" -d "$PK" >/dev/null
  DPT="$(find "$PK" -name dpt.jar | head -1)"
fi
echo "dpt.jar: $DPT"
java -jar "$DPT" -v || { echo "Could not run dpt.jar"; exit 1; }

echo "Input APK: $APK"
[ -f "$APK" ] || { echo "ERROR: APK not found. Build it first (./gradlew :app:assembleDebug)."; exit 1; }

# 2) Pack under each configuration.  -o sets the output dir; logs saved per run.
pack () {
  local name="$1"; shift
  echo ""
  echo "=================================================================="
  echo "== Config: $name    flags: $*"
  echo "=================================================================="
  rm -rf "$OUT/$name"; mkdir -p "$OUT/$name"
  java -jar "$DPT" -f "$APK" -o "$OUT/$name" "$@" 2>&1 | tee "$OUT/$name.log"
  echo "-- output of $name --"
  ls -la "$OUT/$name" 2>/dev/null
}

pack all-checks-on                                                      # default: every runtime check ON
pack anti-debug-off     --disable-anti-debug
pack frida-detect-off   --disable-frida-detect
pack crc-detect-off     --disable-crc-detect
pack all-checks-off     --disable-anti-debug --disable-frida-detect --disable-crc-detect
pack signature-verify   -vs                                            # add runtime signature verification

echo ""
echo "== DONE. Shielded APKs + logs under: $OUT =="
find "$OUT" -name '*.apk' -exec ls -la {} \;
echo ""
echo "Compare sizes of the input vs each shielded APK, and skim the per-config .log files."
echo "To try one on the emulator:  adb install -r \"$OUT/all-checks-on/\"*.apk"
