# SortButtons

Choose one short option that changes what a list shows: **A–Z**, **Newest**, **Most Played** on the "Songs" section of Home.

## Rules
- Flat pills (`radius-full`), 64dp tall. Pills mean "choose one"; rounded rectangles mean "do something".
- Word in `button` (18sp): a pill has no icon above it, so `control-label` does not apply.
- Unselected: `fill`, `ink`. Selected: `accent` fill, `on-accent` word and a `size-icon-sm` check in front (the cue that works without colour).
- A visible "Sort" label sits above. Pills wrap rather than scroll sideways. The choice is remembered.
