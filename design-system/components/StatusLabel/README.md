# StatusLabel

Where a song stands, in words with an icon. All four use `ink`; the icon and the words carry the meaning.

| State | Words | Icon |
| --- | --- | --- |
| done | On your phone | `offline_pin` |
| downloading | Downloading 40% | `downloading` |
| waiting_retry | Will download later | `schedule` |
| failed | Can't be saved | `error` (filled), words in bold |

## Rules
- `body` size, icon `size-icon-sm`, `space-1` apart. No coloured status text.
- "Can't be saved" on the Song screen comes with **Try Again** and **Remove**, or **Remove** alone when it can never work.
