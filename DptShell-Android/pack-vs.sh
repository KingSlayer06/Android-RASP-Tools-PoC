#!/usr/bin/env bash
#
# Pack the signature-verify (-vs) config so it actually ENGAGES, using your
# Android debug keystore. dpt's -vs needs a -c protect-config pointing at a real
# keystore: it then computes the certificate SHA-256 from that keystore AND signs
# the output with the same keystore, so the runtime signature check matches and
# passes. (Without -c, dpt's "default keystore" path just fails and disables -vs.)
#
# Run from: ~/Developer/Android Apps/Android RASP Tools PoC/DptShell-Android
#   bash pack-vs.sh
#
set -uo pipefail
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
HERE="$(cd "$(dirname "$0")" && pwd)"
PK="$HERE/packer"
DPT="$(find "$PK" -name dpt.jar | head -1)"
APK="${1:-$HERE/app/build/outputs/apk/debug/app-debug.apk}"
KS="$HOME/.android/debug.keystore"

[ -n "$DPT" ] && [ -f "$DPT" ] || { echo "ERROR: dpt.jar not found — run run-dpt-pack.sh first."; exit 1; }
[ -f "$APK" ] || { echo "ERROR: APK not found: $APK"; exit 1; }
[ -f "$KS" ]  || { echo "ERROR: debug keystore not found at $KS (build a debug APK once and it appears)."; exit 1; }

CFG="$HERE/protect-config.debug.json"
# shellPkgName MUST be set to a valid dotted package: if omitted, dpt uses its
# default "com/luoyesiqiu/shell" (slashes), which becomes an invalid Application
# class name "com/luoyesiqiu/shell.ProxyApplication" in the manifest and the APK
# won't install (INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION).
cat > "$CFG" <<JSON
{
  "shellPkgName": "com.kingslayer06.dpt_shell_android.shell",
  "signature": {
    "keystore": "$KS",
    "alias": "androiddebugkey",
    "storepass": "android",
    "keypass": "android"
  }
}
JSON
echo "Protect config: $CFG"
echo "Keystore:       $KS (alias androiddebugkey)"

OUT="$HERE/shielded/signature-verify"
rm -rf "$OUT"; mkdir -p "$OUT"
echo "== Packing signature-verify with -vs -c =="
java -jar "$DPT" -f "$APK" -o "$OUT" -vs -c "$CFG" 2>&1 | tee "$HERE/shielded/signature-verify.log"

echo ""
echo "-- output --"; ls -la "$OUT"
echo ""
echo "Engaged if the log shows:  'Computing SHA-256 from signing keystore: ...'  and  'Use custom key store',"
echo "and the 'Write config:' line now includes an \"app_sign_sha256\":\"...\" value."
echo ""
echo "Optional runtime re-check (signed with the same debug cert, so it should still run):"
echo "  ADB=~/Library/Android/sdk/platform-tools/adb"
echo "  \$ADB install -r \"$OUT/\"*.apk && \$ADB shell monkey -p com.kingslayer06.dpt_shell_android -c android.intent.category.LAUNCHER 1"
echo "  sleep 2; \$ADB shell pidof com.kingslayer06.dpt_shell_android"
