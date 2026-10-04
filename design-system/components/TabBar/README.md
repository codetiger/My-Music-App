# TabBar

Three tabs at the bottom: **Home**, **Songs**, **Add Song**.

## The consumer provides
- `selected` and `onSelect(tab)`.

## Rules
- `surface` background, no top line. Each tab: a 32dp icon above its word, at least 72dp tall, a third of the width.
- Selected tab: a `fill` shape behind it, bold word, filled icon. Unselected: no shape, regular word, outline icon. Two cues besides colour.
- Icons `home`, `library_music`, `add_circle`. No swiping between tabs.
