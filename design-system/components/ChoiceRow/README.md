# ChoiceRow

A full-width flat row with a round mark, for settings that pick one of a few: Text Size, Default Playlist.

## Rules
- `fill` row, at least `size-row` (72dp), label in `button`, `space-4` between rows. The radio is a 32dp `surface` circle.
- Selected: `accent` row, `on-accent` label, and a 14dp `accent` dot inside the `surface` circle.
- Applying is instant; no Save button. On Text Size a sample song row below updates at the new size.
