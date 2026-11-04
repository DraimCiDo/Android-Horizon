> 🤖 Published by DraimCiDo's AI agent (Codex, GPT-6), not by DraimCiDo personally.

Android 0.4.0 updates the Android port from its October 31 source snapshot to
[Horizon main at 63eccfc](https://github.com/horizon-rust/horizon/commit/63eccfc3559807d338e93ca2fec1e1fdc077a721)
(November 4, 2026). Minecraft target: **26.3**. The Android version number is
independent of desktop Horizon's release numbers.

- Galaxy S25 Ultra (`SM-S938*`) first-launch defaults: 60 FPS cap, 8/5 chunk
  render/simulation distance, 75% world resolution with FXAA, four chunk workers.
  Existing settings and touch layouts are preserved.
- Current upstream gameplay, rendering, native mod API and client library split.
  Upstream's renderer replaces the old port's experimental Fast Opaque Pass.
- Android touch, keyboard, controller, file picker, resource setup and lifecycle
  support retained; suspension is forwarded through the new event-loop wrapper.
- Microsoft token replacement, confirmation before importing mods, build error
  handling and 16 KiB native-library alignment.

Download `horizon-0.4.0-s25-ultra-arm64.apk`. `SHA256SUMS` contains its checksum.
Requires ARM64 Android 8.0+; this is an **experimental prerelease**, not a
device-tested performance guarantee. Physical S25 Ultra validation is pending.

The APK is debug-key signed. An APK signed by another key cannot be replaced
in place. Back up worlds before changing installations; uninstalling deletes
app data. CI runners generate a fresh debug key, so this also matters when
moving between separately built APKs.

Minecraft files and derived resources are **not included**. Prepare resources
from your own Minecraft installation using this revision of horizon-prepare;
an older derived pack may not include new upstream exports. See
[the Android guide](https://github.com/DraimCiDo/Android-Horizon/blob/android-port/ANDROID.md)
and [S25 Ultra setup](https://github.com/DraimCiDo/Android-Horizon/blob/android-port/docs/samsung-s25-ultra.md).

🤖 Generated with [Codex](https://openai.com/codex/)
