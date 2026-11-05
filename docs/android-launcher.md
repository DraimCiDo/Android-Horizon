# Android launcher and portable user data

Android 0.4.2 opens a portrait launcher before starting the game. Play handles
first-run preparation, game launch and profile access. Files opens the existing
Android DocumentsProvider and creates/restores portable ZIP backups. Build shows
the installed version, device, engine revision, Microsoft configuration status
and a link to releases. Gameplay remains landscape.

The launcher wraps the working Android runtime and bundled-resource setup;
it does not change graphics API selection or overwrite the S25 Ultra defaults.
Those defaults are applied only when settings files do not already exist.

## Backups

Fully quit the game, reopen Horizon, then select **Files → Create backup**.
The Android document picker saves the ZIP in a user-selected location, including
Downloads or a cloud provider. Save it outside Horizon's own game folder.

The archive includes `saves/`, `screenshots/`, `resourcepacks/`, `shaderpacks/`,
`options.txt`, `horizon-graphics.txt`, `horizon-extras.txt`, `horizon-player.txt`,
`horizon-skin.txt`, `horizon-skin.png`, `servers.dat` and (0.4.3+)
`touch_layout.json`, when present.
It excludes generated game resources, logs, native mods and Microsoft credentials.
Mod-specific configuration is not part of this backup format.

**Restore from ZIP** accepts backups of this format, not arbitrary Minecraft
installations or launcher backups. It validates and stages the complete archive
before adding data to the game folder. Existing worlds and packs are preserved;
conflicting names gain ` (imported N)`. A world directory is moved as a complete
unit. Existing settings files are kept; missing settings are restored.

The format uses `horizon-backup.txt` (`horizon-user-data-v1\n`) plus a final
`horizon-backup-complete.txt` recording the file count and uncompressed byte count.
ZIP entry CRCs, path checks, an entry limit and an 8 GiB uncompressed limit are
enforced. Symlinks cannot redirect reads or restored files outside user data.
Temporary extraction data is removed on success or failure. A process killed
by Android can leave a `.horizon-restore-*` staging directory; it is not a world
and can be removed after closing the app.

The game activity currently ends its process when quit; reopen the launcher to
work with backups. Backup controls reject an active game in the same process.

## Comparison source

The feature comparison used the public README and build documentation of
[DreamingbullyYT/horizon-android-edition at 87449a0](https://github.com/DreamingbullyYT/horizon-android-edition/tree/87449a0796cd5355700acbd419a468d659e09c9d).
Its documentation describes a Compose launcher, file import/backup, touch HUD
editing, a compatibility pipeline and Microsoft device-code authentication.
Those are documented capabilities, not independently verified performance results.

That project documents MIT upstream Horizon code, GPL-3.0-derived touch controls
and separately licensed fonts. Our repository's `CONTRIBUTING.md` permits source
reuse only under its listed permissive licenses. No implementation source from
that repository was read, copied or adapted: this launcher and archive code are
independent implementations, using our existing Android bridge and UI helpers.
There is consequently no imported library or third-party code notice to add.
Touch HUD editing, its Compose UI and its engine patch were not transplanted.

Both ports still require the build owner's registered Microsoft application
and Minecraft API access. Changing launchers does not supply that configuration;
see [Android Microsoft sign-in](android-microsoft.md).
