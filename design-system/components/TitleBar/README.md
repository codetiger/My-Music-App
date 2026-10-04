# TitleBar

The fixed bar at the top of every screen that is not a tab: **Back** and where you are. Tab screens (Home, Add Song) have no title bar; they start with a PageHeader instead.

## The consumer provides
- `title`: the screen's name ("Song", "Now Playing", "Favourites", "Settings").
- `onBack`.

## Rules
- A **Back** button (arrow and word, `fill`), then the title in `title`. Never a bare arrow, never a menu.
- `surface` background, at least `size-row-tall` (88dp), no divider line, no elevation, no colour change on scroll. It stays put while the page scrolls, so Back is always in reach.
- The title wraps to two lines and is never cut off; at Extra Large text it may take three and the bar grows.
- Phone Setup's Back goes to the previous step before it leaves.
