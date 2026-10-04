# HomeScreen

Home put together: the one screen for lists and songs. The PageHeader (drawing and "Murali's Music App"), "My Lists" tiles, then "Songs" with the search box, sort buttons and every song, a **Settings** button, the Now Playing bar and the tabs. There is no separate Play button: the Now Playing bar's `accent` Play / Pause is Home's one main action, and it plays the default list when nothing is loaded. The reference for how flat shapes and air do all the separating.

- Order: PageHeader, at most one NoticeCard, "My Lists", "Songs", **Settings**. The whole body scrolls as one page, header included; only the Now Playing bar and tabs stay put. There is no fixed top bar.
- **Settings** is a full-width secondary button (`settings` icon) `space-6` below the last song, or below "Songs you add will be listed here." on an empty library.
- Body padding `space-4`; `space-6` above each section heading.
- No All Songs tile: every song is already listed under "Songs".
- Search filters only the songs. The list tiles stay where they are.
