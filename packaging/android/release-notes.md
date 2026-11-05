> 🤖 Published by DraimCiDo's AI agent (Codex), not by DraimCiDo personally.

## Android 0.4.2: launcher and world backups

- A portrait launcher with Play, Files and Build pages replaces immediate game startup.
- Play opens setup when needed, launches the game and provides profile/account access.
- Files opens the game folder in Android's document browser and exports portable ZIP
  backups of worlds, screenshots, resource/shader packs, skin and player settings.
- Restore preserves existing worlds and packs by giving imported duplicates a new
  name, and keeps existing settings. It validates the complete archive before adding
  player data. Generated resources, native mods and account credentials are excluded.
- Microsoft application ID configuration now follows upstream: the build owner's
  `HORIZON_MSA_CLIENT_ID` repository variable is validated and embedded in the APK.
  This build has no registered application ID, so Microsoft login remains unavailable.

Download `horizon-0.4.2-s25-ultra-arm64.apk`; `SHA256SUMS` contains its checksum.
The APK still includes the complete game resources and prepares them offline.
The working Galaxy S25 Ultra profile, Vulkan driver choices and landscape gameplay
are preserved. Engine base: [Horizon 63eccfc](https://github.com/horizon-rust/horizon/commit/63eccfc3559807d338e93ca2fec1e1fdc077a721).

The launcher/backup feature comparison used the public documentation of
[DreamingbullyYT/horizon-android-edition](https://github.com/DreamingbullyYT/horizon-android-edition/tree/87449a0796cd5355700acbd419a468d659e09c9d).
The new implementation is independent; no source, GPL touch-control components,
fonts or engine patches were copied from that project.

Validation: real JVM archive roundtrip, collision preservation, malformed archives,
path traversal, symlink isolation and credential exclusion; Android Java compilation;
Python tests; APK signing, embedded-resource checksum and 16 KiB ELF alignment checks.
The new launcher and backup picker still require testing on a physical phone.

ARM64 Android 8.0+; experimental prerelease. A fresh CI signing key means Android may
require uninstalling a previous build. **Back up your worlds before uninstalling.**
For 0.4.1, use Android's Files app → Horizon → `saves` and copy it outside the app
folder; 0.4.1 has no in-app ZIP backup. Uninstalling removes app data.

🤖 Generated with [Codex](https://openai.com/codex/)
