# Message

A short sentence at the bottom that confirms what happened or explains what did not.

## Rules
- Flat `fill` shape, `ink` text and icon, `radius-md`, padding `space-4`, inset `space-4` from the screen edges, `space-3` above the Now Playing bar.
- At most one action, a `surface` button with its icon like any other: **Put Back** `restore_from_trash`, **Play** `play_arrow`.
- One plain sentence, no codes: "Song saved on your phone", "No internet — this song will download later", "Song put back in your library", "No song link found in this message."
- Stays 10 seconds or until tapped, longer if Android's accessibility timeout asks. With an action it stays until used or the screen changes. No sound, no vibration.
