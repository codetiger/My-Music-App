# Button

Every action: a flat shape with an icon and a word, at least 64 × 64dp. No outlines, no shadows.

## Kinds
- **Hero**: the 96dp buttons on Add Song. `radius-lg`, a 72dp `surface` disc holding a `size-icon-lg` icon, label in `button-hero` with a `body` line under it. **Primary hero** is `accent` with an `accent` icon in the disc: "Paste Link" before anything is pasted. **Secondary hero** is `fill` with an `ink` icon: "Pick a File", and "Paste Link" while a song waits to be saved, so Save Song is the only accent.
- **Primary**: `accent` fill, `on-accent` label. One per screen: the screen's main action (Play on the Song screen, Save Song, Open Settings, the confirm button in a dialog, and the Now Playing bar's Play / Pause on Home).
- **Secondary** (default): `fill` shape, `ink` label. Everything else, including Remove. On a `fill` area (cards, the Now Playing bar) it becomes `surface`.
- **Stacked**: icon above a `control-label` word, for tight spots (row Play, bar Pause, Move Up / Move Down). Secondary, except the bar's Play / Pause on Home, which is primary.
- **Toggle**: a secondary button whose label says its state ("Shuffle: Off", "Repeat: All"). When on, it takes the `accent` fill and the word says On.

## Rules
- Label in `button`, Title Case: "Add Song", "Put Back", "Change Order". Icon `size-icon`, `space-3` before the label.
- Every button has both an icon and a word, including Cancel, Skip, OK and No Thanks. The icon for each word is fixed in the README's Iconography list; the same word always gets the same icon.
- Neighbouring buttons sit `space-4` apart. Pairs share a row at equal width; a label that will not fit makes the pair stack.
- Pressed: `press-overlay` on the shape. Focus: `focus-width` `focus-ring` outline, `focus-width` away (`Modifier.focusRing`).
- Never disable a button to mean "not yet". Keep it active and answer with a Message.
