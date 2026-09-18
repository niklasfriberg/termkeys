package com.github.niklasfriberg.termkeys

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.XCollection
import javax.swing.KeyStroke

/** One terminal key override: when [keystroke] is pressed, send [sequence] (with escapes) to the PTY. */
data class KeyMapping(
    var keystroke: String = "",
    var sequence: String = "",
    var enabled: Boolean = true,
)

class TermKeysState {
    @get:XCollection(style = XCollection.Style.v2)
    var mappings: MutableList<KeyMapping> = defaultMappings()
}

/** Shift+Enter -> line feed, the same soft-newline byte as Ctrl+J. */
fun defaultMappings(): MutableList<KeyMapping> =
    mutableListOf(KeyMapping(keystroke = "shift pressed ENTER", sequence = "\\n"))

@Service(Service.Level.APP)
@State(name = "TermKeysSettings", storages = [Storage("termKeys.xml")])
class TermKeysSettings : PersistentStateComponent<TermKeysState> {

    private var state = TermKeysState()

    override fun getState(): TermKeysState = state

    override fun loadState(loaded: TermKeysState) {
        XmlSerializerUtil.copyBean(loaded, state)
    }

    val mappings: List<KeyMapping> get() = state.mappings

    fun replaceMappings(newMappings: List<KeyMapping>) {
        state.mappings = newMappings.map { it.copy() }.toMutableList()
    }

    /** First enabled mapping whose parsed keystroke equals [stroke], or null. */
    fun match(stroke: KeyStroke): KeyMapping? =
        state.mappings.firstOrNull { it.enabled && parse(it.keystroke) == stroke }

    private fun parse(s: String): KeyStroke? =
        try {
            KeyStroke.getKeyStroke(s)
        } catch (t: Throwable) {
            null
        }

    companion object {
        fun getInstance(): TermKeysSettings = service()
    }
}
