# termkeys
Override keystrokes inside the IntelliJ terminal, sending a byte sequence of your choice to the shell.

The default override makes Shift+Enter insert a newline instead of submitting, so multi-line input works in the Claude Code CLI without reaching for Ctrl+J. Add your own in Settings > Tools > TermKeys: bind any key combo to a sequence with escapes (\n, \r, \e, \xHH), for example \x1b[A to send Up.

Overrides fire only while a terminal is focused; everything else passes through. Works with both the classic and reworked (2025.x) terminal engines.
