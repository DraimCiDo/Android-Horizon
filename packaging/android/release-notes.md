> 🤖 Published by DraimCiDo's AI agent (Codex), not by DraimCiDo personally.

## Android 0.4.4: Horizon Nightly 2026-11-05

The engine source is merged with the official Nightly's exact published revision:
[f319701e8c9ab657ff562250b3ba07b2d654e3bc](https://github.com/horizon-rust/horizon/commit/f319701e8c9ab657ff562250b3ba07b2d654e3bc),
permanent upstream tag `nightly-source/2026-11-05-f319701`.
The moving `nightly` tag is not used at runtime.

- Includes the upstream changes since our November 4 engine base: PvP/HUD settings,
  controller menu input updates, shader/renderer improvements, native mod API
  additions and gameplay fixes. See the upstream Nightly notes for the full list;
  desktop-specific additions are not promises of Android hardware support.
- Preserves the Android launcher, offline resource bundle, S25 Ultra profile,
  live touch settings, offhand button, inventory gestures and portable backups.
- Resolves upstream startup/HUD changes while retaining local skins and touch
  controls. Android surface-loss/resume handling is retained.
- README is completely rewritten for this Android fork: installation, exact engine
  version, controls, backup/restore, graphics, Microsoft status, mods and builds.
  The original current upstream README remains in README.upstream.md.

Download `horizon-0.4.4-s25-ultra-arm64.apk`; `SHA256SUMS` contains its checksum.
The complete game resources remain inside the APK. No separate ZIP is required.
Microsoft sign-in remains unconfigured, as requested; no client ID was invented.

Validation includes the merged game/tool type checks, Android Java compilation,
Python/JVM tests, touch geometry tests, APK signing and launcher entry point,
resource archive checksum and 16 KiB ELF alignment. The updated Nightly still
needs testing on a physical S25 Ultra; prior device testing covers older releases.

ARM64 Android 8.0+; experimental prerelease. **Back up before uninstalling**:
CI uses a fresh signing key and Android may require removing the previous APK.
Use Files → Create backup and save outside Horizon (0.4.2+), or manually copy
`saves` via the system Files app (0.4.1). Uninstalling deletes app data.

🤖 Generated with [Codex](https://openai.com/codex/)
