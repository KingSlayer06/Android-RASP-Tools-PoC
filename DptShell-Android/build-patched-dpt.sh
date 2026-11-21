#!/usr/bin/env bash
#
# Build a PATCHED dpt.jar that fixes the junk-code DEX-merge crash.
# -----------------------------------------------------------------------------
# Root cause: dpt-shell merges a generated junk-code DEX into your app's DEX
# using the deprecated `dx` DexMerger. On DEX produced by modern D8/AGP that
# merge throws java.lang.AssertionError. dpt's mergeDex() is meant to fall back
# to keeping the junk DEX separate when the merge fails, but it only
# `catch (Exception ...)` — and AssertionError is an Error, not an Exception,
# so it escapes and aborts the whole packer.
#
# Fix: change that catch to `catch (Throwable ...)` so the author's intended
# fallback runs. We then rebuild ONLY the pure-Java dpt.jar (no NDK / no native
# shell build) and reuse the official release's prebuilt shell-files, seeding
# the build key so the rebuilt jar stays compatible with those shell binaries.
#
# Run from:  ~/Developer/Android Apps/Android RASP Tools PoC/DptShell-Android
#   bash build-patched-dpt.sh
# Then re-run:  bash run-dpt-pack.sh
#
set -euo pipefail

export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
echo "JAVA_HOME=$JAVA_HOME"

HERE="$(cd "$(dirname "$0")" && pwd)"
PK="$HERE/packer"
REL_JAR="$(find "$PK" -name dpt.jar | head -1)"
[ -n "$REL_JAR" ] || { echo "ERROR: run run-dpt-pack.sh once first so packer/.../dpt.jar exists."; exit 1; }
EXECDIR="$(cd "$(dirname "$REL_JAR")" && pwd)"
echo "Release jar:   $REL_JAR"
echo "shell-files:   $EXECDIR/shell-files"

# 1) Pull the release's build key + soName so the rebuilt jar matches the prebuilt shell .so
KEY="$(unzip -p "$REL_JAR" META-INF/MANIFEST.MF | tr -d '\r' | awk -F': ' '/Dpt-Build-Key/{print $2}')"
SO_PATH="$(find "$EXECDIR/shell-files/libs" -name 'lib*.so' 2>/dev/null | head -1)"
SO="$(basename "$SO_PATH" | sed -E 's/^lib(.*)\.so$/\1/')"
[ -n "$KEY" ] || { echo "ERROR: could not read Dpt-Build-Key from release jar"; exit 1; }
[ -n "$SO" ]  || { echo "ERROR: could not find a shell .so to read soName from"; exit 1; }
echo "Release build key: $KEY"
echo "Release soName:    $SO"

# 2) Clone source (shallow, NO submodules — we only build the pure-Java module)
SRC="$HERE/dpt-src"
if [ ! -d "$SRC/.git" ]; then
  git clone --depth 1 https://github.com/luoyesiqiu/dpt-shell "$SRC"
fi

# 3) Patch mergeDex: catch Throwable instead of Exception
F="$SRC/dpt/src/main/java/com/luoye/dpt/util/DexUtils.java"
perl -0777 -pi -e 's/catch \(Exception ignored\) \{\s*\}\s*return false;/catch (Throwable ignored) {}\n        return false;/' "$F"
grep -q "catch (Throwable ignored)" "$F" || { echo "ERROR: patch did not apply"; exit 1; }
echo "Patched: $F"

# 3b) Patch ShellConfig.toString(): JSON.toJSONString(this) recurses to a
#     StackOverflowError when a -c protect-config is read and logged. Use the
#     class's own manual toJson() builder instead (anchored on getInstance() so
#     only the outer ShellConfig.toString() is touched, not the inner SignatureConfig).
G="$SRC/dpt/src/main/java/com/luoye/dpt/config/ShellConfig.java"
perl -0777 -pi -e 's/(public static ShellConfig getInstance\(\)\s*\{\s*return INSTANCE;\s*\}\s*\@Override\s*public String toString\(\)\s*\{\s*)return JSON\.toJSONString\(this\);/${1}try { return toJson(); } catch (Throwable t) { return "ShellConfig"; }/' "$G"
grep -q 'try { return toJson(); }' "$G" || { echo "ERROR: ShellConfig patch did not apply"; exit 1; }
echo "Patched: $G"

# 4) Remove the native :shell module from the build so no NDK/CMake is needed
sed -i '' "/include ':shell'/d" "$SRC/settings.gradle"

# 5) Seed build ids so the rebuilt jar's Dpt-Build-Key == the release's (shell compatibility)
mkdir -p "$SRC/build"
printf 'soName=%s\ndptBuildKey=%s\n' "$SO" "$KEY" > "$SRC/build/dpt-build-ids.properties"
echo "Seeded $SRC/build/dpt-build-ids.properties"

# 6) Build only the jar (invoke via sh so a missing +x bit on gradlew doesn't matter)
chmod +x "$SRC/gradlew" 2>/dev/null || true
( cd "$SRC" && sh ./gradlew --no-daemon :dpt:jar )

NEW_JAR="$SRC/executable/dpt.jar"
[ -f "$NEW_JAR" ] || { echo "ERROR: build did not produce $NEW_JAR"; exit 1; }

# 7) Install the patched jar in place of the release jar (keeps shell-files alongside)
cp "$REL_JAR" "$REL_JAR.orig.bak" 2>/dev/null || true
cp "$NEW_JAR" "$REL_JAR"
echo ""
echo "== Patched dpt.jar installed: $REL_JAR =="
echo "Build key in patched jar:"
unzip -p "$REL_JAR" META-INF/MANIFEST.MF | tr -d '\r' | grep Dpt-Build-Key || true
echo ""
echo "Now re-run:  bash run-dpt-pack.sh"
