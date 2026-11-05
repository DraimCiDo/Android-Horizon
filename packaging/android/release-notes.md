> 🤖 Published by DraimCiDo's AI agent (Codex), not by DraimCiDo personally.

## Android 0.4.3: touch controls and interface

- New **Controls and interface** screen in the launcher and in-game profile:
  camera sensitivity/inversion, button size/opacity, edge margins, joystick
  deadzone, control scheme and interface scale. Save and return to the game;
  settings apply after all fingers are lifted, without restarting.
- Resolution-independent camera movement and a touch-specific joystick deadzone.
- Main/off-hand swap button; inventory/drop controls moved away from the hotbar.
- Fast button taps reach the simulation; a second finger cannot steal the joystick
  or release another finger's button. System-cancelled touches cannot become
  short attack/use taps.
- Tap inventory slots normally, long press for right click (half stack / one item),
  drag through the normal inventory input path, and swipe world/server lists.
- Responsive English/Russian layout editor; existing custom positions survive.
  Touch preferences are now included in user-data backups.

Download `horizon-0.4.3-s25-ultra-arm64.apk`; `SHA256SUMS` contains its checksum.
Complete game resources remain inside the APK. The S25 Ultra device profile and
engine base Horizon `63eccfc` are retained. Microsoft client ID configuration is
still absent, as requested; Microsoft sign-in remains unavailable.

Validation: game compilation/type checking, geometry/deadzone/camera math tests,
Android Java compilation, Python/JVM tests, APK signature, launcher entry point,
embedded-resource checksum and 16 KiB ELF alignment. New touch gestures and UI
still need testing on a physical phone.

ARM64 Android 8.0+; experimental prerelease. CI uses a fresh signing key, so Android
may require uninstalling the old APK. **Back up player data before uninstalling.**
In 0.4.2 use Files → Create backup and save the ZIP outside Horizon. In 0.4.1 copy
`saves` using Android's Files app → Horizon. Uninstalling deletes app data.

🤖 Generated with [Codex](https://openai.com/codex/)
