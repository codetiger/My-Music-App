# PlayerControls

The Now Playing control block: skip 10 seconds, Previous / Play-Pause / Next, Shuffle / Repeat.

## The consumer provides
- `isPlaying`, `shuffle` (on / off), `repeat` (Off / All / One) and the handlers.

## Rules
- Play / Pause is a `size-play` (96dp) `accent` disc with a 56dp filled icon and the word under it. Previous and Next are `size-transport` (72dp) `fill` discs. Disc and word together are the touch target.
- Labels always: "Previous", "Pause" / "Play", "Next", "Back 10 s", "Ahead 10 s". Repeat cycles "Repeat: Off" → "Repeat: All" → "Repeat: One" (icons `repeat`, `repeat`, `repeat_one`). Toggles that are on take the `accent` fill.
- Lock screen, notification, Bluetooth and Android Auto use Android's own media controls with the same state.
