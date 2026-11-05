# Android touch controls (0.4.3)

Open **Play → Controls and interface**, or open Profile from the game and choose
**Controls and interface**. Save and return to the game. Changes are applied on
its next idle touch frame, within about one second after all fingers are lifted.
No restart is required.

The screen edits `touch_layout.json`, preserving custom control positions. It
provides camera sensitivity (25–300%), vertical inversion, camera motion from a
button finger, joystick deadzone (0–40%), button scale (70–160%), global opacity,
edge margins, Standard/Bedrock/Custom schemes and the game's interface scale.
The interface scale affects menus, inventory slots and the HUD; the framebuffer
can limit the effective maximum. Camera sensitivity is proportional to swipe
fraction of the screen width rather than framebuffer pixel count.

The touch joystick applies its own radial deadzone, without the physical pad's
second deadzone. It can move the player even with physical controllers disabled.
Only one finger owns the joystick. Fast button taps are retained until the pad
poll sees them; lifting another finger does not release a button still held.
System-cancelled touches do not generate short attack/use taps.

A new two-arrow button swaps the main and off hands through the standard virtual
controller binding (`West`, normally `key.swapOffhand`). Jump, sneak, sprint,
attack, use, inventory, chat and drop remain available. Standard layouts move
inventory/drop away from hotbar slots and inset the top controls. Existing custom
positions remain; enlarged controls can be rearranged using the gear editor.
The editor wraps its buttons on smaller GUIs and has English/Russian labels.

## Inventory and lists

- Tap a container slot: normal left click.
- Hold a slot for at least 450 ms and lift without dragging: right click, following
  the game's normal half-stack / one-item behavior.
- Move past the gesture threshold: drag with the normal container mouse path.
- Swipe world/server lists: scroll. Lifting after scrolling does not select a row.
- Additional fingers do not create additional menu clicks. A deferred gesture is
  discarded when its menu category closes; it cannot become a world click.

Sliders and other menus retain immediate mouse dragging. Physical mouse/keyboard
input is unchanged. Touch layout preferences are included in the user-data ZIP
backup; restoring still keeps an existing configuration.

Validation covers real game type checking, standalone geometry/deadzone/camera
math tests, Java compilation against Android 35, and the backup roundtrip with a
touch layout. Multitouch feel, inventory gestures and safe margins need physical
phone testing; compilation is not a substitute for those checks.
