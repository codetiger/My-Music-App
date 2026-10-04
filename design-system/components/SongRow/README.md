# SongRow

One song in a list: picture, title, artist with its state, and a Play button.

## The consumer provides
- `picture` (or none), `title`, `artist`, `status`, `onOpen` (picture, title or artist opens the Song screen), `onPlay`.

## Rules
- No dividers and no row background: rows are separated by `space-2` of air.
- Picture `size-art-row`, `radius-sm`. No picture: a `fill` square with a music-note icon.
- Title in `body-strong`, then the artist in `body`, then the StatusLabel on its own line. Each wraps, never "…". Three short lines hold up at 2× text better than one long "Artist · status" line.
- Play is a stacked `fill` button. It plays this song then the rest of the list as shown.
- In Change Order mode, Play is replaced by stacked **Move Up** and **Move Down** buttons (TalkBack: "Move <song> up"). At the top or bottom they stay active and do nothing.

## Variants
- **Up Next**: no status line; "Playing now" replaces the artist on the current song. Tapping the row plays that song. **Move Up**, **Move Down** and **Take Out** (`playlist_remove`, never "Remove": the song stays in the library) sit in a row under it, aligned to the end.
- **Recently Removed**: picture, title and artist (or "List" for a list), and a stacked **Put Back**. No Play.
