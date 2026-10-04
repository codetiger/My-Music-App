# NoticeCard

The single card Home may show at the top of its body (requirements HOME-4): update ready, unfinished setup, songs that did not move, first-song hint, home-screen picture.

## Rules
- One at a time. Flat `fill` card, `radius-md`, padding `space-4`. Icon `size-icon-md` in `ink`, headline in `heading`, detail in `body`.
- Its buttons are `surface` shapes on the `fill`, so they never compete with a screen's `accent` main action. The doing action comes first.
- Every button has its icon: Install `download`, Finish Setup `settings`, See Names `list`, OK `check`, Add Song `add_circle`, Add `add`, No Thanks `close`.
- Add Song uses the same card, without buttons, for "Your phone is nearly full".
- Copy: "A new version is ready" / Install; "Music may stop when the screen is off" / Finish Setup; "5 songs from WhatsApp or files couldn't be moved to this phone" / See Names, OK; "Tap Add Song to save your first song"; "Put your picture on the home screen?" / Add, No Thanks.
