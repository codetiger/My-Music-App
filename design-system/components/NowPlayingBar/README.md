# NowPlayingBar

The flat `fill` strip above the tab bar that always says what is playing. Tapping it (outside its button) opens Now Playing.

## The consumer provides
- Current song (picture, title, artist), position as a fraction, `isPlaying`; or, when nothing is loaded, the default list's name and picture.

## Rules
- A `fill` shape with `radius-md`, inset `space-4` from the screen edges like the body. A `size-progress` (4dp) `accent` progress line runs along its bottom edge.
- On Home the Play / Pause button is `accent`: Home has no main button of its own, so this is it (HOME-2). Everywhere else the bar appears (Add Song, Song, a list) the screen has its own main action, so Play / Pause is a `surface` shape like the picture placeholder.
- Title wraps; the bar grows rather than cutting the name. From font scale 1.25 up, the artist line is left out so the bar does not crowd the screen; the title alone says what plays.
- After a restart it shows the saved song, paused, with **Play**. It never starts sound by itself.
- When nothing is loaded it offers the default list instead: "Play Favourites" (or that list's name, or "Play All Songs"), "Nothing playing" under it, the list's first picture and **Play**; no progress line. Tapping anywhere on it plays the list. An empty library answers with the Message "Tap Add Song to save your first song".
