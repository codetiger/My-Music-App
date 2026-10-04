# ListTile

A big flat tile that opens a playlist, on Home and in Add to List.

## The consumer provides
- `name`, `count`, and the first song's picture, if any.

## Rules
- Two per row, `space-4` gap, at least `size-tile` tall, `radius-md`, `fill` shape, `ink` text. Name in `button` weight, count in `body`.
- Every list uses the same `fill`. Lists are told apart by name, icon and the first song's picture, not by colour.
- With songs: the first song's picture sits in the top-left 64dp square. Empty: a music-note icon. Built-ins use `favorite` (Favourites) and `history` (Recently Played). Home has no All Songs tile; Add to List never shows one either.
- **New List** is the last tile: `surface` (no shape of its own) with a plus in a `fill` circle.
- Names wrap; a long name makes the tile taller.
