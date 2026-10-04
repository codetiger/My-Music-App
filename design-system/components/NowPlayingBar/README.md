# NowPlayingBar

The flat `fill` strip above the tab bar that always says what is playing. Tapping it (outside its button) opens Now Playing.

## The consumer provides
- Current song (picture, title, artist), position as a fraction, `isPlaying`.

## Rules
- A `fill` shape with `radius-md`, inset `space-3` from the screen edges. A 4dp `accent` progress line runs along its bottom edge.
- Inside a `fill` area, the Pause / Play button and the picture placeholder switch to `surface`, so they still read as shapes.
- Title wraps; the bar grows rather than cutting the name.
- After a restart it shows the saved song, paused, with **Play**. It never starts sound by itself.
