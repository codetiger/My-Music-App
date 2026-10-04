# PageHeader

The top of a tab screen's page. Tab screens have no fixed bar: the selected tab already says where you are, so the header scrolls away with the page and leaves the room to songs.

## Kinds
- **Home**: the photo drawing (`size-avatar-lg`, or the music-note logo) beside the personalised title, "Murali's Music App" or "My Music App" when no name was given. It greets the user each time the app opens.
- **Add Song**: "Add a Song" alone.

## Rules
- Title in `song-hero` (28sp), a TalkBack heading; it wraps and is never cut off.
- At least `size-row-tall` tall, `space-4` above it, then the page's first section.
- No buttons in the header. Settings lives at the end of Home; Back belongs to screens that are not tabs (TitleBar).
