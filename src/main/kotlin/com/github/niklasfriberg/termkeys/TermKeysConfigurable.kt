package com.github.niklasfriberg.termkeys

import com.intellij.openapi.options.Configurable
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import javax.swing.JComponent

/** Settings > Tools > TermKeys: edit the terminal key overrides. */
class TermKeysConfigurable : Configurable {

    private val settings = TermKeysSettings.getInstance()
    private var working: MutableList<KeyMapping> = mutableListOf()
    private lateinit var model: ListTableModel<KeyMapping>
    private lateinit var table: JBTable

    override fun getDisplayName(): String = "TermKeys"

    override fun createComponent(): JComponent {
        working = settings.mappings.map { it.copy() }.toMutableList()
        model = ListTableModel(arrayOf(EnabledColumn, KeystrokeColumn, SequenceColumn), working)
        table = JBTable(model)
        table.setShowGrid(false)

        return ToolbarDecorator.createDecorator(table)
            .setAddAction {
                val dialog = TermKeysMappingDialog(KeyMapping())
                if (dialog.showAndGet()) {
                    model.addRow(dialog.result())
                }
            }
            .setEditAction {
                val row = table.selectedRow
                if (row < 0) return@setEditAction
                val dialog = TermKeysMappingDialog(working[row].copy())
                if (dialog.showAndGet()) {
                    val edited = dialog.result()
                    working[row].apply {
                        keystroke = edited.keystroke
                        sequence = edited.sequence
                        enabled = edited.enabled
                    }
                    model.fireTableRowsUpdated(row, row)
                }
            }
            .setRemoveAction {
                val row = table.selectedRow
                if (row >= 0) model.removeRow(row)
            }
            .createPanel()
    }

    override fun isModified(): Boolean = working != settings.mappings

    override fun apply() {
        settings.replaceMappings(working)
    }

    override fun reset() {
        working = settings.mappings.map { it.copy() }.toMutableList()
        model.items = working
    }

    private object EnabledColumn : ColumnInfo<KeyMapping, Boolean>("On") {
        override fun valueOf(item: KeyMapping): Boolean = item.enabled
        override fun getColumnClass(): Class<*> = java.lang.Boolean::class.java
        override fun isCellEditable(item: KeyMapping): Boolean = true
        override fun setValue(item: KeyMapping, value: Boolean) { item.enabled = value }
        override fun getWidth(table: javax.swing.JTable): Int = 40
    }

    private object KeystrokeColumn : ColumnInfo<KeyMapping, String>("Keystroke") {
        override fun valueOf(item: KeyMapping): String = item.keystroke
    }

    private object SequenceColumn : ColumnInfo<KeyMapping, String>("Sequence") {
        override fun valueOf(item: KeyMapping): String = item.sequence
    }
}
