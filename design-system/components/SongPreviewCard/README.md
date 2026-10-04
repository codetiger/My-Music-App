# SongPreviewCard

What a pasted or shared link (or WhatsApp file) will save: picture, title, source and length, then **Save Song**.

## The consumer provides
- `picture`, `title`, `source`, `length`, `onSave`. With several links: one card per link with a tick box (all ticked) and one primary button under them, "Save 3 Songs".

## Rules
- Flat `fill` card, `radius-md`, padding `space-4`, picture 96dp. Meta line "From YouTube · 4:05".
- Tick box: a 36dp `surface` square in a 64dp target; ticked it turns `accent` with an `on-accent` check.
- While the link is read the title reads "Reading the link…" and Save Song is hidden. Errors replace the card with a Message.
