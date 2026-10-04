# TextField

A box for typing a name: Welcome ("What is your name?"), New List, Rename List, Change Name, Edit Name. Search has its own SearchField.

## The consumer provides
- `label`, the current text, and what Done on the keyboard does.

## Rules
- The label sits above the box in `body-strong`, `space-2` away, and is what TalkBack reads. No placeholder inside the box.
- Flat `fill` box, at least `size-target` tall, `radius-md`, padding `space-4`, no border. Typed text in `input` (24sp regular), so each letter is easy to check.
- While typing: the `focus-width` `focus-ring` outline, `focus-width` clear of the box. No other colour change.
- Keyboard: words start with a capital; the Done key confirms, same as the dialog's main button. In a dialog the box takes focus at once.
- An empty name is never saved, and the button never stays silent. Welcome answers with the Message "Type your name, or tap Skip.". A dialog covers the Message, so a hint line appears under the box instead: an `info` icon (`size-icon`) and one `body` sentence, "Type a name first." (Edit Name: "Type the song's name first."), read out by TalkBack. It goes as soon as a letter is typed; the dialog stays open.
