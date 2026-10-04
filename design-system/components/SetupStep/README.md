# SetupStep

One Phone Setup step per screen: what to change, a picture of where to tap, **Open Settings**, **Skip**.

## Rules
- "Step 2 of 4" in `body-strong`, title in `title`, one sentence in `body`.
- The picture is a flat `fill` panel with a simple drawing of the one Android control (a `surface` row with an `accent` switch) and "Tap this on the next screen".
- **Open Settings** is primary; **Skip** (`skip_next`) is a `fill` button below.
- Done: a `size-icon-md` filled `check_circle` and "Done" in bold `ink`; the primary button becomes **Next** (`chevron_right`).
- The drawn switch is `accent` because it pictures the control to find, not an action in this app; it is the one allowed second accent.
- Android Auto (no direct link): a numbered list of written steps replaces the picture; the primary button is **I Have Done This** (`check`).
