# SongRow

One song in a list: picture, title, artist with its state, and a Play button.

## The consumer provides
- `picture` (or none), `title`, `artist`, `status`, `onOpen` (picture, title or artist opens the Song screen), `onPlay`.

## Rules
- No dividers and no row background: rows are separated by `space-2` of air.
- Picture `size-art-row`, `radius-sm`. No picture: a `fill` square with a music-note icon.
- Title in `body-strong`; the line below in `body`: "Artist · status". Both wrap, never "…".
- Play is a stacked `fill` button. It plays this song then the rest of the list as shown.
- In Change Order mode, Play is replaced by **Move Up** and **Move Down** buttons.
