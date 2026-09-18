# TermKeys

Override keystrokes in the IntelliJ terminal to send a byte sequence to the shell. The default
override makes **Shift+Enter** insert a newline instead of submitting, so multi-line input works
in the Claude Code CLI without reaching for `Ctrl+J`. Add your own overrides in the settings.

## Configuring overrides

Settings > Tools > TermKeys. Each row is `keystroke -> sequence`:
- **Keystroke**: click the field and press the combo (any key + Shift/Ctrl/Alt/Meta).
- **Sequence**: the bytes to send, with escapes `\n \r \t \e \xHH \\` (e.g. `\x1b[A` for Up).

Ships with one row, `Shift+Enter -> \n`. Overrides only fire while a terminal is focused;
everything else passes through.

## Why

The IntelliJ terminal routes keystrokes through a PTY, which collapses `Enter` and `Shift+Enter`
to the same byte (`0x0D`, CR). The CLI can't tell them apart, so Shift+Enter submits.
`Ctrl+J` works because it sends `0x0A` (LF), which the CLI treats as a soft newline.

TermKeys intercepts Shift+Enter while the terminal is focused and writes `0x0A` to the PTY,
giving Shift+Enter the same effect as Ctrl+J. Plain Enter is untouched and still submits.

## Status

Working on IntelliJ IDEA 2024.3 (classic engine) and 2025.3 (`IU-253`, reworked engine). Two
things are version-sensitive:

1. **Interception** — an `IdeEventQueue` dispatcher catches Shift+Enter before the terminal.
2. **Input sink** — reached reflectively, since no engine exposes a stable public accessor. Three
   strategies cover the engines seen so far:
   - classic: `getTtyConnector` on a `JBTerminalWidget` ancestor of the focused component
   - legacy widget list: `TerminalToolWindowManager.terminalWidgets` (empty on 2025.x)
   - reworked (2025.x): `TerminalInput` from the focus data context, `sendBytes(0x0A)`

   This is the spot to adjust if it breaks on a future IDE; a failure logs `TermKeys:` warnings
   in `idea.log`.

## Build & try

```bash
./gradlew runIde        # launches a sandbox IDE with the plugin
./gradlew buildPlugin    # produces build/distributions/termkeys-*.zip to install
```

Install the zip via Settings -> Plugins -> gear -> Install Plugin from Disk.

Bump the target IDE in `build.gradle.kts` (`intellijIdeaCommunity("2024.3")`) to match the IDE you run.

## If it doesn't intercept

The reworked terminal may consume Shift+Enter before the dispatcher, or expose the connector
differently. Fallbacks to try, in order:
- Confirm the dispatcher fires (add logging in `TermKeysDispatcher.dispatch`).
- Inspect the focused terminal widget's class + methods to find the real connector accessor.
- As a last resort, target the classic terminal engine.
