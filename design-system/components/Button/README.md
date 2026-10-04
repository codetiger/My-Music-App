# Button

Every action: a flat shape with an icon and a word, at least 64 × 64dp. No outlines, no shadows.

## Kinds
- **Hero**: the one 96dp main button on Home ("Continue: <song>", "Play Favourites") and Add Song ("Paste Link"). `accent` fill, `radius-lg`, a 72dp `on-accent` disc holding the icon, label in `button-hero`.
- **Primary**: `accent` fill, `on-accent` label. One per screen: the screen's main action (Play on the Song screen, Save Song, Open Settings, the confirm button in a dialog).
- **Secondary** (default): `fill` shape, `ink` label. Everything else, including Remove. On a `fill` area (cards, the Now Playing bar) it becomes `surface`.
- **Stacked**: icon above a `control-label` word, for tight spots (title bar Settings, row Play, bar Pause).
- **Toggle**: a secondary button whose label says its state ("Shuffle: Off", "Repeat: All"). When on, it takes the `accent` fill and the word says On.

## Rules
- Label in `button`, Title Case: "Add Song", "Put Back", "Change Order". Icon `size-icon`, `space-3` before the label.
- Neighbouring buttons sit `space-4` apart. Pairs share a row at equal width; a label that will not fit makes the pair stack.
- Pressed: `press-overlay` on the shape. Focus: 3dp `focus-ring` outline, 3dp away.
- Never disable a button to mean "not yet". Keep it active and answer with a Message.
