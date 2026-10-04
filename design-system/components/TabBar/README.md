# TabBar

Two tabs at the bottom: **Home** and **Add Song**. Home holds both the lists and the songs.

## The consumer provides
- `selected` and `onSelect(tab)`.

## Rules
- `surface` background, no top line, side padding `space-4`. Each tab: a `size-icon-md` icon above its word, at least `size-row` (72dp), half the width.
- Selected tab: a `fill` shape behind it, bold word, filled icon. Unselected: no shape, regular word, outline icon. Two cues besides colour.
- Icons `home`, `add_circle`. No swiping between tabs.
