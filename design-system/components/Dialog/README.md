# Dialog

The panel every dialog uses, and the three kinds built on it. ConfirmDialog has its own README.

## Rules
- `surface` panel on `scrim`, `radius-lg`, padding `space-5`, at most 420dp wide, `space-4` from the screen sides and `space-6` from top and bottom. It scrolls when text is large.
- Title in `heading` (a TalkBack heading), then the content, then full-width stacked buttons `space-4` apart: the main action on top as primary, **Cancel** (`close`) below as `fill`.
- Tapping the scrim or Android Back is Cancel.

## Kinds
- **Confirm**: a question before removing or saving something big. See ConfirmDialog.
- **Name**: one TextField. Confirming with the box empty shows the TextField hint line, never a silent button. New List → **Make List** (`add`); Rename List → **Rename** (`edit`); Change Name → **Save** (`check`). Edit Name has two fields, "Song name" and "Singer or artist", and **Save** (`check`).
- **List**: plain `body` lines and a single primary **OK** (`check`), no Cancel. Used for See Names ("Songs that couldn't be moved").
