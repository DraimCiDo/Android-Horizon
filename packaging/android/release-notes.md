> 🤖 Published by DraimCiDo's AI agent (Codex, GPT-6), not by DraimCiDo personally.

## Android 0.4.1: one complete APK

- The first-run screen is portrait; the game remains landscape.
- The APK includes the complete prepared Minecraft 26.3 resources: gameplay facts,
  data, models, fonts, languages and sounds. First launch automatically unpacks them.
- No separate horizon-derived.zip, PC preparation or first-run game download is needed.
- Setup checks the actual space needed by the bundled archive and displays extraction
  progress. Failed/interrupted extraction can be retried without downloading again.
- The build verifies resource completeness before packaging, the embedded archive's
  SHA-256, APK signature and 16 KiB native-library alignment before publication.

Download `horizon-0.4.1-s25-ultra-arm64.apk`; `SHA256SUMS` contains its checksum.
The APK is larger because it now contains the game resources. The engine remains
[Horizon main 63eccfc](https://github.com/horizon-rust/horizon/commit/63eccfc3559807d338e93ca2fec1e1fdc077a721)
(November 4, 2026), with the Galaxy S25 Ultra profile and Android adaptations.

ARM64 Android 8.0+; experimental prerelease, physical S25 Ultra testing pending.
The APK uses a fresh CI debug signing key. If Android refuses an update over a
previous APK, back up saves before uninstalling; uninstalling removes app data.

Prepared resources are generated during the build and embedded only in the APK;
they are not committed to the source repository.

🤖 Generated with [Codex](https://openai.com/codex/)
