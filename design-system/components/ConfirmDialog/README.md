# ConfirmDialog

Asks once before anything is removed, deleted or very large, and says how to undo it.

## Rules
- `surface` panel on `scrim`, `radius-lg`, padding `space-5`. Question in `heading`, detail in `body`.
- Full-width stacked buttons: the confirm action on top as the primary (`accent`) button, **Cancel** below as `fill`. No red: the question and the undo note carry the weight.
- The confirm label repeats the verb: "Remove this song?" → Remove; "Delete this list? Songs stay in your library." → Delete List; "Delete these for good?" → Delete for Good; "This is a long recording (2 h 10 min, about 150 MB). Save it?" → Save.
- Tapping the scrim or Android Back is Cancel.
