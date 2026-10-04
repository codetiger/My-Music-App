# My Music App design system

Reference for building the app's screens in Jetpack Compose. It follows [highlevel-requirements.md](../highlevel-requirements.md), with the changes listed under "Changes to the requirements" below.

Live, browsable copy: https://claude.ai/artifact/9zj9p676R3DSBuhimU7X9k (private until shared).

## What is here

| Path | What |
| --- | --- |
| `README.md` | This brand book: principles, colour, writing, type, shape, layout, icons, Compose mapping |
| `tokens.json` | Source of truth for every colour, type style, spacing, radius and size token |
| `tokens.css` | The same tokens as CSS variables, generated from `tokens.json` (used by the previews) |
| `components/<Name>/README.md` | Rules for each component: what it shows, what the screen provides, do and don't |
| `components/<Name>/preview.html` | A static HTML preview of each component |
| `components/bundle.css` | Preview styles. They mirror the intended Compose look; they are not app code |
| `assets/logos/` | `logo-mark.svg` (standard logo) and `app-icon.svg` (launcher icon) |
| `index.html` | Every preview on one page. Open it in a browser |

## Changes to the requirements

These were decided while making the design system. Update `highlevel-requirements.md` to match:

- **Text size (section 2):** body 18sp, labels under icons 16sp, titles 24sp. The requirements say at least 20sp body and 28sp titles. Text Size Large and Extra Large still scale it up.
- **Theme:** one light cream theme only. The app ignores the phone's dark mode.
- **Playlist tiles (PL-3):** all tiles use one colour (`fill`) and are told apart by name, icon and the first song's picture. The requirements ask for a set of tile colours.
- **Photo drawing (section 7, step 6):** ink #2B1D14 on paper #F7F0E4 instead of #1A1A1A on #FAF7F0.
- **Colours:** four only (cream, tan, dark brown, brown), flat, with no outlines or shadows.

---

My Music App is a music player for one person aged 60 or more. The look is flat, warm and simple: a cream page, dark brown words, plain shapes in one soft tan, and one brown button that says what to do next. Four colours in all, no outlines, no shadows. Every screen does one job, uses big plain words, and forgives mistakes.

## Principles

- **Four colours, no more.** `surface`, `fill`, `ink`, `accent`. Any new need is solved with these four, size and weight, never a fifth colour.
- **Flat shapes, no lines.** A tappable thing is a filled shape. No borders, outlines, dividers or shadows; air between things does the separating.
- **One accent per screen.** The screen's main action is the only `accent` shape (plus the current choice in a group and the seek bar).
- **Words with every icon.** No button is an icon alone. Labels say exactly what happens.
- **Comfortable by default, bigger on request.** Body text 18sp, labels under icons 16sp, nothing smaller; Text Size in Settings scales it up. Touch targets never below 64 × 64dp, 16dp apart.
- **Nothing is cut off.** Titles and names wrap; no ellipsis.

## Colour

| Token | Value | Job |
| --- | --- | --- |
| `surface` | #f7f0e4 | The page; labels on `accent`; buttons inside a `fill` area |
| `fill` | #e9dcc6 | Every flat shape that is not the main action |
| `ink` | #2b1d14 | All text and icons |
| `accent` | #6b4423 | The main action, the current choice, the seek bar |

- `ink` on `surface` is 14:1, on `fill` 12:1. `on-accent` is `surface`, 7.5:1 (WCAG AAA).
- One bright theme only. The app always shows cream, even when the phone is set to dark mode (`android:forceDarkAllowed="false"`, no `values-night` resources).
- There is no grey text. Secondary lines (artist, length, status) are `ink` at regular weight under a bold title.
- `accent` as text is allowed only on `surface`. Never put `accent` text on `fill`.
- Inside a `fill` area (a card, the Now Playing bar, a message), shapes flip to `surface`, so a button on a card is still a visible shape.
- Status has no colours of its own: "On your phone", "Downloading 40%", "Will download later" and "Can't be saved" are told apart by icon and words, the last in bold.
- All playlists use the same `fill`; they are told apart by name, icon and picture.
- `press-overlay` and `scrim` are `ink` at low opacity, not extra colours. The photo drawing and logo use `ink` on `surface` (`drawing-ink`, `drawing-paper`).

## Writing

- Talk to the user as "you": "Your Name and Photo", "Song saved on your phone". The app never says "I" or "we".
- Buttons and screen names in Title Case, two or three words: **Save Song**, **Change Order**, **Recently Removed**. Messages in sentence case, one short sentence, no exclamation marks.
- Plain words: "Up Next", not queue; "saved on your phone", not downloaded; "Will download later", not retrying; "Can't be saved", not error. No codes, no "Oops".
- Say what happened and what to do: "No internet — this song will download later".
- Before removing, ask and name the thing: "Remove this song?", "Delete this list? Songs stay in your library."
- Numbers as people say them: "4:05", "2 h 10 min", "about 150 MB", "Downloading 40%", "4 Oct". No emoji in the interface.

## Type

- One family: Atkinson Hyperlegible Next, made for readers with low vision. Bundle it from Google Fonts (OFL) in `res/font`.
- Scale (sp): `song-hero` 28, `title` 24, `button-hero` 22, `heading` 20, `button` / `body` / `body-strong` / `time` 18, `control-label` 16. Nothing smaller.
- At Text Size Large (×1.25) body becomes 22.5sp; at Extra Large (×1.5), 27sp.
- Hierarchy comes from size and weight only: bold (700) for titles, labels and song names, regular (400) for everything under them.
- Text follows the phone's font size × Text Size (`text-normal` 1, `text-large` 1.25, `text-extra-large` 1.5), capped at `text-cap` 2. Layouts hold at 36sp body: rows grow, pairs stack.

## Shape and space

- Shapes: `radius-md` (16dp) for buttons, tiles, cards, rows; `radius-lg` (24dp) for the main button, dialogs and the big picture; `radius-sm` (8dp) for song pictures; `radius-full` for round things and the "choose one" pills.
- Screen side margin `space-4`. Gap between touch targets `space-4`; between song rows `space-2`; between groups `space-5`; above a section heading `space-6`.
- Touch targets `size-target` (64dp); main button and Play disc `size-play` (96dp).
- Pills mean "choose one"; rounded rectangles mean "do something".

## Layout

- Portrait only. Title bar, screen title, main action in the top half, then content.
- Tab screens end with the Now Playing bar above the three tabs. Other screens show **Back** and no tabs.
- No swipe-only or long-press-only actions, no hidden menus, no floating button. **Move Up** / **Move Down** always exist beside any drag.

## States and motion

- Pressed: `press-overlay` on the shape. Focus: 3dp `focus-ring` outline, 3dp clear. Selected: `accent` fill plus a second cue (check, dot, filled icon, the word On).
- Never disable a button to mean "not yet"; answer with a Message.
- Motion: 150ms fades and the standard screen slide only. With "Remove animations" on, changes are instant.

## Iconography

- Material Symbols Rounded, weight 500, 28dp in buttons and rows, 40dp in the main button and tiles, always in `ink` (or `on-accent` on accent). Outline by default; filled for the selected tab, transport and the failed status.
- An icon always sits beside or above its word; TalkBack reads the word.
- Names: Home `home`, Songs `library_music`, Add Song `add_circle`, Settings `settings`, Back `arrow_back`, Play `play_arrow`, Pause `pause`, Previous `skip_previous`, Next `skip_next`, Back 10 s `replay_10`, Ahead 10 s `forward_10`, Shuffle `shuffle`, Repeat `repeat` / `repeat_one`, Up Next `queue_music`, Song Details `info`, Favourite `favorite`, Add to List `playlist_add`, Edit Name `edit`, Remove `delete`, Put Back `restore_from_trash`, Change Order `swap_vert`, Move Up `arrow_upward`, Move Down `arrow_downward`, Paste Link `content_paste`, Pick a File `audio_file`, Save Song `download`, Take Photo `photo_camera`, Choose Photo `image`, Search `search`, Clear `close`, Volume `volume_down` / `volume_up`, Open Settings `open_in_new`.
- Logos: `assets/Logos/logo-mark.svg` and `assets/Logos/app-icon.svg`.

## In Compose

- 1px in these tokens is 1dp for sizes and 1sp for text.
- `ColorScheme`: `surface` → background, surface, onPrimary; `fill` → surfaceContainer, secondaryContainer, primaryContainer; `ink` → onSurface, onSurfaceVariant, onSecondaryContainer; `accent` → primary. Set outline and outlineVariant to transparent, and tonal and shadow elevation to 0 everywhere.
- Use `Button` with `ButtonDefaults.buttonColors` (never `OutlinedButton`) and `Card` with `CardDefaults.cardColors(containerColor = fill)` and no border.
- Set `minimumInteractiveComponentSize` to 64dp and the font scale to phone scale × Text Size, capped at 2.
