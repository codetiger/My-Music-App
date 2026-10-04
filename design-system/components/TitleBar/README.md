# TitleBar

The top of every screen: who the app belongs to, where you are, and the one way to Settings or Back.

**Tab screens** (Home, Songs, Add Song): the photo drawing (`size-avatar`) on the left, the personalised title in `title`, and a stacked **Settings** button on the right.
**Every other screen**: a **Back** button (arrow and word), then the screen title.

## The consumer provides
- `title`: "Murali's Music App", or "My Music App" when no name was given.
- `avatar`: the saved drawing, or `assets/Logos/logo-mark.svg`.
- `onSettings` or `onBack`, never both.

## Rules
- `surface` background, no divider line, no elevation, no colour change on scroll.
- The title wraps to two lines and is never cut off; at Extra Large text it may take three and the bar grows.
- Settings and Back are flat `fill` buttons with a label, never a bare icon or a menu.
