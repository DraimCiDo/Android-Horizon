# Samsung Galaxy S25 Ultra

This branch supplies conservative starting settings for Samsung models `SM-S938*`
(including regional and dual-SIM variants). It uses the existing Android port and
the system Vulkan driver, with OpenGL fallback under `graphicsApi:auto`. No
third-party GPU driver is bundled or selected automatically.

## Default profile

On game startup, before settings load, `android_main` reads Android's manufacturer
and model. On an S25 Ultra, it creates each settings file **only if that file does
not exist**. Other devices keep their current defaults. Existing files, worlds,
accounts and touch layouts are preserved.

| Setting | Initial value |
| --- | --- |
| Frame limit | 60 FPS with VSync |
| Render / simulation distance | 8 / 5 chunks |
| World resolution | 75% on each axis (56.25% of native pixels) |
| World anti-aliasing | FXAA |
| Chunk workers / updates per frame | 4 / 4 |
| Upload budget per frame | 4096 KiB |
| Clouds / particles / entity distance | Off / decreased / 75% |
| HDR | Off |

These are starting values, **not a measured FPS guarantee**. On a 3120×1440
surface, 75% renders the world at 2340×1080; the UI stays at surface resolution.
At a lower Android display resolution the render target scales down accordingly.
Adjust the values in the game's Video and Horizon Graphics menus. To reapply
defaults to an existing install, back up and rename `options.txt` and
`horizon-graphics.txt` in the game's Files root while the game is closed. The next
start creates new files; restoring the backups restores the previous settings.

## Build and install

In a GitHub fork, open **Actions → Android APK → Run workflow**, choosing this
branch. Download and unzip the `horizon-s25-ultra-arm64` artifact. The APK is
debug-key signed: it may not update over an APK signed by a different author.
Back up worlds before changing installations; uninstalling removes app data.

For a local Linux build, follow [ANDROID.md](../ANDROID.md), installing SDK
platform **35**, build tools **35.0.0**, NDK **27.2.12479018**, JDK **21**, Rust
and cargo-ndk, then run:

```sh
PROFILE=release bash packaging/android/build_apk.sh
adb install -r target/android/horizon.apk
```

The native library is linked with 16 KiB segment alignment for compatibility
with both 4 KiB and 16 KiB Android page sizes. Java compilation errors stop the
build. Minecraft resources and the derived pack are not bundled: use your own
prepared resources as described in [ANDROID.md](../ANDROID.md#game-files).

## Device validation still needed

This profile has not been benchmarked on an S25 Ultra. Verify cold start, loading
a world, touch and keyboard input, online-server sign-in after a token refresh,
screen lock/resume and a sustained 15-minute play session. Check frame times,
temperature and memory before increasing distance or choosing 90/120 FPS.
Logs are in the game's `logs/latest.log`; crash reports are in `crash-reports/`.

The branch also requires confirmation before installing an externally supplied
mod, preserves an existing mod if replacement fails, and allows a renewed
Microsoft session to replace the token held by the running game.
