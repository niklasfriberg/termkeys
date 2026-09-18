package com.github.niklasfriberg.termkeys

import com.intellij.ide.DataManager
import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.awt.AWTEvent
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

/**
 * Intercepts configured keystrokes while a terminal is focused and sends the mapped byte sequence
 * to that terminal's input, so e.g. Shift+Enter can insert a soft newline (0x0A) instead of submitting.
 * Keys with no mapping, or presses outside a terminal, pass through untouched.
 *
 * The terminal input has no stable public accessor, so it is reached reflectively via three
 * strategies covering the terminal engines seen across IDE versions:
 *  1. classic engine: a JBTerminalWidget ancestor of the focused component exposes getTtyConnector
 *  2. legacy widget list: TerminalToolWindowManager exposes a widget whose component holds focus
 *  3. reworked engine (2025.x): a TerminalInput is available from the focus data context
 */
class TermKeysDispatcher(private val project: Project) : IdeEventQueue.EventDispatcher {

    private val log = thisLogger()

    override fun dispatch(e: AWTEvent): Boolean {
        if (e !is KeyEvent || e.id != KeyEvent.KEY_PRESSED) return false
        if (e.keyCode == KeyEvent.VK_UNDEFINED) return false

        val mapping = TermKeysSettings.getInstance().match(KeyStroke.getKeyStrokeForEvent(e)) ?: return false
        val bytes = KeySequenceCodec.decode(mapping.sequence)
        if (bytes.isEmpty()) return false

        val focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner ?: return false

        return if (sendBytes(focus, bytes)) {
            e.consume()
            true
        } else {
            false
        }
    }

    private fun sendBytes(focus: Component, bytes: ByteArray): Boolean {
        try {
            connectorFromFocusAncestors(focus)?.let { writeToConnector(it, bytes); return true }
            connectorFromWidgets(focus)?.let { writeToConnector(it, bytes); return true }
            terminalInputFromDataContext(focus)?.let { return sendToTerminalInput(it, bytes) }
        } catch (t: Throwable) {
            log.warn("TermKeys: failed to send sequence", t)
        }
        return false
    }

    /** Classic engine: the JBTerminalWidget/ShellTerminalWidget ancestor exposes getTtyConnector. */
    private fun connectorFromFocusAncestors(focus: Component): Any? {
        var c: Component? = focus
        while (c != null) {
            ttyConnectorOf(c)?.let { return it }
            c = c.parent
        }
        return null
    }

    /** Legacy widget list: match the widget whose component contains the focused component. */
    private fun connectorFromWidgets(focus: Component): Any? {
        val widgets = try {
            TerminalToolWindowManager.getInstance(project).terminalWidgets
        } catch (t: Throwable) {
            return null
        }
        val widget = widgets.firstOrNull { w ->
            componentOf(w)?.let { SwingUtilities.isDescendingFrom(focus, it) } ?: false
        } ?: return null
        return ttyConnectorOf(widget)
    }

    /** Reworked engine (2025.x): TerminalInput is exposed via the focus data context. */
    private fun terminalInputFromDataContext(focus: Component): Any? =
        DataManager.getInstance().getDataContext(focus).getData("TerminalInput")

    private fun writeToConnector(connector: Any, bytes: ByteArray) {
        val method = connector.javaClass.methods.firstOrNull {
            it.name == "write" && it.parameterTypes.size == 1 && it.parameterTypes[0] == ByteArray::class.java
        } ?: error("TtyConnector.write(byte[]) not found on ${connector.javaClass.name}")
        method.invoke(connector, bytes)
    }

    private fun sendToTerminalInput(terminalInput: Any, bytes: ByteArray): Boolean {
        val methods = terminalInput.javaClass.methods
        methods.firstOrNull {
            it.name == "sendBytes" && it.parameterTypes.size == 1 && it.parameterTypes[0] == ByteArray::class.java
        }?.let { it.invoke(terminalInput, bytes); return true }

        methods.firstOrNull {
            (it.name == "sendString" || it.name == "send") && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java
        }?.let { it.invoke(terminalInput, String(bytes, Charsets.UTF_8)); return true }

        log.warn("TermKeys: no send method on ${terminalInput.javaClass.name}")
        return false
    }

    private fun componentOf(widget: Any): Component? =
        invokeNoArg(widget, "getComponent") as? Component

    private fun ttyConnectorOf(widget: Any): Any? =
        invokeNoArg(widget, "getTtyConnector")

    private fun invokeNoArg(target: Any, name: String): Any? =
        try {
            target.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
                ?.invoke(target)
        } catch (t: Throwable) {
            null
        }
}
