# dpt-shell — Proof-of-Concept Report

**Tool:** dpt-shell (luoyesiqiu/dpt-shell) v2.22.1 · MIT · https://github.com/luoyesiqiu/dpt-shell
**Category:** Open-source Android packer with runtime shielding (post-build)
**Date:** 2026-11-21 · **Device:** Himanshus-MacBook-Pro (Apple Silicon, macOS 27), JDK 17
**Project:** `~/Developer/Android Apps/Android RASP Tools PoC/DptShell-Android`

## Verdict

- **Integration: works.** Post-build packer — no source changes. `java -jar dpt.jar -f app.apk -o out`.
- **Runtime shielding: works and verified on a device.** A shielded APK installed, launched, and ran on the emulator (MainActivity displayed, live process, no crash). dpt-shell hollows the app's DEX methods and reconstructs them at runtime via a native shell `.so`.
- **Configurable: YES.** Each runtime check is an independent pack-time CLI flag; the setting is written into the shell config as a `risk_check_flags` bitmask (proof below). No source changes needed.
- **Caveat:** v2.22.1 has two bugs that block this workflow out of the box (a DEX-merge crash on modern-AGP APKs, and a config-logging crash when `-c` is used). Both have small fixes (below); all results here use the patched build.

## What dpt-shell is

A DEX protection "shell": it removes (hollows) method bodies from the app's DEX, stores them encrypted inside the APK, and injects a native shell library that decrypts and restores them at runtime. Alongside this it adds runtime self-protection: anti-debug, Frida detection, and a libc `.text` CRC integrity check, plus optional app-signature verification. It is applied to a finished APK/AAB; the developer does not modify app source.

## Environment & integration

- Cloud build was not possible: the session's network policy blocks Google's servers (`dl.google.com`, `maven.google.com → dl.google.com`), so the Android SDK/NDK and Android Gradle Plugin can't be fetched there. All build/pack/run work was done on the Mac.
- The official **prebuilt** packer was used (the Quick-uses path). Note the current release asset is `dpt-shell-<version>.zip` (containing `executable/dpt.jar` + `executable/shell-files/`), **not** `executable.zip` as the README still says.
- The native shell for all four ABIs (armeabi-v7a, arm64-v8a, x86, x86_64) ships prebuilt inside the release, so no NDK/CMake was needed to pack.
- Demo app: a minimal Kotlin activity, `applicationId com.kingslayer06.dpt_shell_android`, built with the Mac's Android toolchain (`:app:assembleDebug`).

## Bugs found in v2.22.1 (and the fixes)

### Bug 1 — junk-code DEX merge crash (blocks every pack)

Every pack initially aborted with:

```
java.lang.AssertionError
  at com.android.dex.Dex$Section.writeStringData(Dex.java:692)
  at com.android.dx.merge.DexMerger...
  at com.luoye.dpt.util.DexUtils.mergeDex(DexUtils.java:503)
  at com.luoye.dpt.builder.AndroidPackage.addJunkCodeDex(...)
```

**Root cause.** dpt injects a generated "junk code" DEX and merges it into the app's `classes.dex` using the deprecated `com.android.dx` DexMerger. On DEX produced by modern AGP/D8 that merger mis-handles the input and ends up writing a bogus >64 KB MUTF-8 string, which `dx` reports as an `AssertionError`. dpt's `mergeDex()` is written to catch a failed merge and fall back to keeping the junk DEX as a separate file — but it only `catch (Exception …)`, and `AssertionError` is an `Error`, not an `Exception`, so it escaped and killed the whole process.

**Fix (one line).** Change the catch in `dpt/src/main/java/com/luoye/dpt/util/DexUtils.java` (`mergeDex`) from `catch (Exception ignored)` to `catch (Throwable ignored)`. dpt then falls back to adding the junk DEX separately (its own intended behaviour) and packing completes. Worth reporting upstream.

### Bug 2 — `StackOverflowError` whenever `-c` is used (blocks `-vs`)

Passing a `-c` protect-config (needed to make `-vs` work) crashed before packing: dpt logs the parsed config ("Use config: …"), which calls `ShellConfig.toString()` = `return JSON.toJSONString(this)`; with fastjson2 that recurses into itself endlessly → `java.lang.StackOverflowError` (ShellConfig.java:241).

**Fix.** The class already has a safe manual serializer, `toJson()`. Point the outer `ShellConfig.toString()` at it (`try { return toJson(); } catch (Throwable t) { return "ShellConfig"; }`). Note there are two `toString()` with that body — the inner `SignatureConfig` one (no `toJson()`, leave it) and the outer `ShellConfig` one; patch only the outer. Also worth reporting upstream.

**Rebuild.** Only the pure-Java `dpt.jar` was rebuilt (no NDK): the `:shell` module was dropped from `settings.gradle`, and `build/dpt-build-ids.properties` was seeded with the release's `soName` + `Dpt-Build-Key` so the rebuilt jar stays compatible with the release's prebuilt native shell. Scripts: `build-patched-dpt.sh` (applies both fixes), `run-dpt-pack.sh`, `pack-vs.sh`.

## Shielding results — all 6 configurations

Input: `app/build/outputs/apk/debug/app-debug.apk`. Each shielded APK is signed and ~31.8 MB (the shell bundles the hollowed DEX, encrypted, plus native `.so` for 4 ABIs). Output: `shielded/<config>/app-debug_signed.apk`.

| Config | Flag(s) | `risk_check_flags` | Shielded APK |
| --- | --- | --- | --- |
| all checks on | (none) | 0 | app-debug_signed.apk (~31.79 MB) |
| frida detect off | `--disable-frida-detect` | 1 | ✓ |
| crc detect off | `--disable-crc-detect` | 2 | ✓ |
| anti-debug off | `--disable-anti-debug` | 4 | ✓ |
| all checks off | all three above | 7 | ✓ |
| signature verify | `-vs -c <keystore cfg>` | 0 (+ `app_sign_sha256`) | ✓ (sig check engaged) |

**Runtime verification** (emulator): both the **all-checks-on** and the **signature-verify** builds were installed and launched — `ActivityTaskManager: Displayed com.kingslayer06.dpt_shell_android/.MainActivity`, a live `pidof`, no `FATAL`/`AndroidRuntime` errors. The shell decrypted and ran the app with the checks active (the signature-verify build is signed with the same debug cert whose SHA-256 is baked in, so its runtime signature check passes). Installing the signature-verify build over the all-checks-on one first needs `adb uninstall`, since the two are signed with different keys.

## Configurability analysis

**Runtime checks are per-check, pack-time CLI flags** — the headline configurability finding. The value dpt writes into the shell config, `risk_check_flags`, is a bitmask that tracks the flags exactly:

- bit 0 (1) = Frida detection disabled
- bit 1 (2) = libc `.text` CRC detection disabled
- bit 2 (4) = anti-debug disabled
- 0 = all enabled · 7 = all three disabled

So anti-debug, Frida detection, and CRC integrity checking can each be toggled independently, with no source changes, confirmed by the written config.

**Other configurable options (from `--help`, not stressed in this PoC):**

- `-vs` / `--verify-sign` — runtime app-signature verification (needs a real signing keystore; see note).
- `-c <file>` — protect config (shell package name, signing keystore).
- `-r <file>` — rules file: class-name patterns to exclude from protection.
- `-K` — keep some classes unprotected (faster startup; not supported by every app).
- `-S` — smaller output, trading some runtime performance.
- `-e <abi,...>` — exclude ABIs (e.g. `x86,x86_64`).
- `--debug`, `--disable-acf`, `--dump-code`, `--noisy-log`, `-x/--no-sign`, `-o` output dir.

**`-vs` (runtime signature verification).** It needs a real signing keystore passed via a `-c` protect-config (the "default keystore" path is a no-op and silently disables the check). Using the debug keystore (`~/.android/debug.keystore`, alias `androiddebugkey`, store/key password `android`), dpt logged "Computing SHA-256 from signing keystore … Signature verification enabled, SHA-256: c3dfddcd…617ee8", wrote `app_sign_sha256` into the shell config, and signed the output with that same keystore ("Use custom key store") — so the baked hash matches the signing certificate and the runtime check passes. (`risk_check_flags` is a separate bitmask for the three disable-flags and stays 0 here.) Script: `pack-vs.sh`.

One more gotcha when using `-c`: the protect-config must also set a valid dotted `shellPkgName`. If omitted, dpt uses its built-in default `com/luoyesiqiu/shell` (slashes) and stamps it into the manifest's Application name as `com/luoyesiqiu/shell.ProxyApplication`, which Android rejects at install (`INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION`). The five non-`-c` configs avoid this because they get a valid dotted randomized name. `pack-vs.sh` sets `shellPkgName` accordingly.

## Summary for the catalogue

dpt-shell is a maintained, MIT-licensed, open-source packer that delivers real runtime shielding post-build and exposes its runtime checks as independent command-line flags — the most configurable open-source packer in the catalogue for this. The practical gotcha is two bugs in v2.22.1 — the DexMerger crash on modern-toolchain APKs, and a `StackOverflowError` whenever `-c` is used — both with small, clearly-correct fixes applied here. Expect to carry those patches (or wait for an upstream release) when using it on a modern app.

## Artifacts

- `~/Developer/Android Apps/Android RASP Tools PoC/DptShell-Android/app/` — demo app
- `~/Developer/Android Apps/Android RASP Tools PoC/DptShell-Android/packer/executable/dpt.jar` — patched packer (+ `shell-files/`)
- `~/Developer/Android Apps/Android RASP Tools PoC/DptShell-Android/shielded/<config>/app-debug_signed.apk` — six shielded APKs + per-config `.log`
- Helper scripts (also in the project library): `run-dpt-pack.sh`, `build-patched-dpt.sh`
