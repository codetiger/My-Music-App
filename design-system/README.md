# My Music App design system

Reference for building the app's screens in Jetpack Compose. It follows [highlevel-requirements.md](../highlevel-requirements.md), with the changes listed under "Changes to the requirements" below.

Live, browsable copy: https://claude.ai/artifact/9zj9p676R3DSBuhimU7X9k (private until shared).

## What is here

| Path | What |
| --- | --- |
| `README.md` | This brand book: principles, colour, writing, type, shape, layout, icons, Compose mapping |
| `tokens.json` | Source of truth for every colour, type style, spacing, radius and size token |
| `tokens.css` | The same tokens as CSS variables, generated from `tokens.json` by `build-tokens-css.py` (used by the previews) |
| `build-tokens-css.py` | Writes `tokens.css`. Run it after any change to `tokens.json` |
| `components/<Name>/README.md` | Rules for each component: what it shows, what the screen provides, do and don't |
| `components/<Name>/preview.html` | A static HTML preview of each component |
| `components/bundle.css` | Preview styles. They mirror the intended Compose look; they are not app code |
| `assets/logos/` | `logo-mark.svg` (standard logo) and `app-icon.svg` (launcher icon) |
| `index.html` | Every preview on one page. Open it in a browser |

## Changes to the requirements

These were decided while making the design system; `highlevel-requirements.md` has been updated to match:

- **Text size (section 2):** body 18sp, labels under icons 16sp, titles 24sp. The requirements say at least 20sp body and 28sp titles. Text Size Large and Extra Large still scale it up.
- **Theme:** one light cream theme only. The app ignores the phone's dark mode.
- **Playlist tiles (PL-3):** all tiles use one colour (`fill`) and are told apart by name, icon and the first song's picture. The requirements ask for a set of tile colours.
- **Photo drawing (section 7, step 6):** ink #2B1D14 on paper #F7F0E4 instead of #1A1A1A on #FAF7F0.
- **Colours:** four only (cream, tan, dark brown, brown), flat, with no outlines or shadows.
- **Home Play button (HOME-2):** no separate Play button; the Now Playing bar's Play / Pause is Home's main action.
- **Title bar (HOME-1, FL-6, SET-1):** tab screens have no title bar. Home opens with the drawing and the personalised title, which scroll away; Settings is a labelled button at the end of Home.

---

My Music App is a music player for one person aged 60 or more. The look is flat, warm and simple: a cream page, dark brown words, plain shapes in one soft tan, and one brown button that says what to do next. Four colours in all, no outlines, no shadows. Every screen does one job, uses big plain words, and forgives mistakes.

## Principles

- **Four colours, no more.** `surface`, `fill`, `ink`, `accent`. Any new need is solved with these four, size and weight, never a fifth colour.
- **Flat shapes, no lines.** A tappable thing is a filled shape. No borders, outlines, dividers or shadows; air between things does the separating.
- **One accent per screen.** The screen's main action is the only `accent` shape (plus the current choice in a group and the seek bar). On Home, which has no main button of its own, the Now Playing bar's Play / Pause is that action; on every other screen the bar's button is plain.
- **An icon and a word on every button.** No button is an icon alone, and none is a word alone: each word has a fixed icon (see Iconography). Labels say exactly what happens.
- **Comfortable by default, bigger on request.** Body text 18sp, labels under icons 16sp, nothing smaller; Text Size in Settings scales it up. Touch targets never below 64 × 64dp, 16dp apart.
- **Nothing is cut off.** Titles and names wrap; no ellipsis.

## Colour

| Token | Value | Job |
| --- | --- | --- |
| `surface` | #f7f0e4 | The page; labels on `accent`; buttons inside a `fill` area |
| `fill` | #dcc8a8 | Every flat shape that is not the main action |
| `ink` | #2b1d14 | All text and icons |
| `accent` | #6b4423 | The main action, the current choice, the seek bar |

- `ink` on `surface` is 14:1, on `fill` 10:1. `on-accent` is `surface`, 7.5:1 (WCAG AAA).
- `fill` against `surface` is 1.44:1: the most a tan can be while `ink` on it stays AAA. Shapes are told apart by this step plus their icon and word, never by an outline.
- One bright theme only. The app always shows cream, even when the phone is set to dark mode (`android:forceDarkAllowed="false"`, no `values-night` resources).
- There is no grey text. Secondary lines (artist, length, status) are `ink` at regular weight under a bold title.
- `accent` as text is allowed only on `surface`. Never put `accent` text on `fill`.
- Inside a `fill` area (a card, the Now Playing bar, a message), shapes flip to `surface`, so a button on a card is still a visible shape. The one exception is the bar's Play / Pause on Home, which is `accent` because it is Home's main action.
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
- Scale (sp): `song-hero` 28, `title` 24, `input` 24 (regular, for typed names), `button-hero` 22, `heading` 20, `button` / `body` / `body-strong` / `time` 18, `control-label` 16. Nothing smaller.
- `control-label` (16sp) only ever sits under an icon in a stacked button. A word on its own, such as a sort pill, uses `button`.
- `time` uses tabular figures so counting numbers do not shift.
- At Text Size Large (×1.25) body becomes 22.5sp; at Extra Large (×1.5), 27sp.
- Hierarchy comes from size and weight only: bold (700) for titles, labels and song names, regular (400) for everything under them.
- Text follows the phone's font size × Text Size (`text-normal` 1, `text-large` 1.25, `text-extra-large` 1.5), capped at `text-cap` 2. Layouts hold at 36sp body: rows grow, pairs stack.

## Shape and space

- Shapes: `radius-md` (16dp) for buttons, tiles, cards, rows; `radius-lg` (24dp) for the main button, dialogs and the big picture; `radius-sm` (8dp) for song pictures; `radius-full` for round things and the "choose one" pills.
- Screen side margin `space-4`, for the body and for the Message, Now Playing bar and tabs at the bottom. Gap between touch targets `space-4`; between song rows `space-2`; between groups `space-5`; above a section heading `space-6`.
- Touch targets `size-target` (64dp); main button and Play disc `size-play` (96dp). Choice rows and tabs at least `size-row` (72dp); settings rows, the Back bar and page headers at least `size-row-tall` (88dp).
- A size used by only one component (the 36dp tick box, the 32dp radio mark, the 420dp dialog width) lives in that component's README, not in the tokens.
- Pills mean "choose one"; rounded rectangles mean "do something".

## Layout

- Portrait only. A fixed top bar only when it holds a control that must always be in reach: every screen that is not a tab has a fixed **Back** bar with its title.
- Tab screens have no top bar. The page starts with its PageHeader (Home: the drawing and the personalised title; Add Song: "Add a Song") and scrolls; the selected tab says where you are. Then the main action in the top half, then content.
- Settings is set up once, so it never takes a spot at the top: it is a labelled button at the end of Home, after the songs. Home's notice cards link straight to the setting they are about.
- Two tabs: **Home** (lists and every song) and **Add Song**. Tab screens end with the Now Playing bar above the tabs. Other screens show **Back** and no tabs.
- No swipe-only or long-press-only actions, no hidden menus, no floating button. **Move Up** / **Move Down** always exist beside any drag.

## States and motion

- Pressed: `press-overlay` on the shape. Focus (keyboard, Switch Access): a `focus-width` `focus-ring` outline, `focus-width` clear of the shape, on every tappable shape and text box. Selected: `accent` fill plus a second cue (check, dot, filled icon, the word On).
- Leave at least 8dp of air around a tappable shape inside anything that clips (a card, a dialog, a scrolling page), so its focus ring is not cut off.
- Never disable a button to mean "not yet"; answer with a Message.
- Motion: 150ms fades and the standard screen slide only. With "Remove animations" on, changes are instant.

## Iconography

- Material Symbols Rounded, weight 500, always in `ink` (or `on-accent` on accent). Outline by default; filled for the selected tab, transport and the failed status.
- Sizes: `size-icon-sm` 24 inline with body text (status, settings value, pill check); `size-icon` 28 in buttons and rows; `size-icon-md` 32 standing alone (tabs, notice cards, Done, picture placeholders); `size-icon-lg` 40 in hero buttons, tiles and Previous / Next; `size-icon-xl` 56 in the Play / Pause disc.
- An icon always sits beside or above its word; TalkBack reads the word.
- Buttons, by word: Home `home`, Add Song `add_circle`, Settings `settings`, Back `arrow_back`, Play / Play All `play_arrow`, Pause `pause`, Previous `skip_previous`, Next (track) `skip_next`, Back 10 s `replay_10`, Ahead 10 s `forward_10`, Shuffle `shuffle`, Repeat `repeat` / `repeat_one`, Up Next `queue_music`, Song Details `info`, Favourite `favorite` (filled when on), Add to List `playlist_add`, Take Out / Take Out of a list `playlist_remove`, Edit Name / Rename / Change Name `edit`, Remove / Delete List / Delete for Good / Empty Now `delete`, Put Back `restore_from_trash`, Change Order `swap_vert`, Done (Change Order) `check`, Move Up `arrow_upward`, Move Down `arrow_downward`, Paste Link `content_paste`, Pick a File `audio_file`, Save Song / Save / Try Again (song) / Install `download`, Take Photo / Try Again (photo) `photo_camera`, Choose Photo `image`, Use This / OK / Save (a name) / I Have Done This `check`, Make List / New List / Add `add`, Search `search`, Clear / Clear Search / Cancel / No Thanks `close`, Skip `skip_next`, Continue / Next (setup step) `chevron_right`, Open Settings `open_in_new`, Finish Setup `settings`, See Names `list`.
- Status and notices: On your phone `offline_pin`, Downloading `downloading`, Will download later `schedule`, Can't be saved `error`, Phone Setup done `check_circle`, Already in your library `library_music`, Update ready `system_update`, Music may stop `battery_alert`, Home-screen picture `add_to_home_screen`, Phone nearly full `sd_card_alert`, Recently Played tile `history`, a list or song with no picture `music_note`, settings row `chevron_right`.
- Volume has no icon: the word "Volume" sits before the bar.
- Logos: `assets/Logos/logo-mark.svg` and `assets/Logos/app-icon.svg`.

## In Compose

- 1px in these tokens is 1dp for sizes and 1sp for text.
- `ColorScheme`: `surface` → background, surface, onPrimary; `fill` → surfaceContainer, secondaryContainer, primaryContainer; `ink` → onSurface, onSurfaceVariant, onSecondaryContainer; `accent` → primary. Set outline and outlineVariant to transparent, and tonal and shadow elevation to 0 everywhere.
- Use `Button` with `ButtonDefaults.buttonColors` (never `OutlinedButton`) and `Card` with `CardDefaults.cardColors(containerColor = fill)` and no border.
- Set `minimumInteractiveComponentSize` to 64dp and the font scale to phone scale × Text Size, capped at 2.
- `ui/theme/` (`Color.kt`, `Dimens.kt`, `Type.kt`, `TextSize.kt`) holds the same values as `tokens.json`. `TokensTest` fails if the two disagree, so change `tokens.json` first, then the Kotlin, then run `build-tokens-css.py`.
- Every tappable shape and text box carries `Modifier.focusRing(shape)` (`ui/components/FocusRing.kt`), placed before `clip` and `clickable`. New components must add it too.
- `MusicButton`, `WideButton` and `HeroButton` take an icon that cannot be null, and a Message action is a `MessageAction(label, icon)`, so a word-only button does not compile.
