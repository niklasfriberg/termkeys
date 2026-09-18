package com.github.niklasfriberg.termkeys

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.KeyStroke

/** Add/edit dialog for a single [KeyMapping]. The keystroke field captures the next key combo pressed. */
class TermKeysMappingDialog(initial: KeyMapping) : DialogWrapper(true) {

    private val keystrokeField = JBTextField(initial.keystroke).apply { isEditable = false }
    private val sequenceField = JBTextField(initial.sequence)
    private val enabledBox = JBCheckBox("Enabled", initial.enabled)
    private val preview = JBLabel(previewText(initial.sequence))

    init {
        title = "Terminal Key Override"
        keystrokeField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_UNDEFINED) return
                keystrokeField.text = KeyStroke.getKeyStrokeForEvent(e).toString()
                e.consume()
            }
        })
        sequenceField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) = update()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) = update()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) = update()
            private fun update() { preview.text = previewText(sequenceField.text) }
        })
        init()
    }

    override fun createCenterPanel(): JComponent =
        FormBuilder.createFormBuilder()
            .addLabeledComponent("Keystroke:", keystrokeField)
            .addComponentToRightColumn(JBLabel("Click the field, then press the key combo."))
            .addLabeledComponent("Sequence:", sequenceField)
            .addComponentToRightColumn(JBLabel("Escapes: \\n \\r \\t \\e \\xHH \\\\"))
            .addLabeledComponent("Bytes:", preview)
            .addComponent(enabledBox)
            .panel

    override fun doValidate(): ValidationInfo? {
        if (keystrokeField.text.isBlank()) return ValidationInfo("Press a keystroke", keystrokeField)
        if (sequenceField.text.isEmpty()) return ValidationInfo("Sequence cannot be empty", sequenceField)
        return null
    }

    override fun getPreferredFocusedComponent(): JComponent = keystrokeField

    fun result(): KeyMapping =
        KeyMapping(keystrokeField.text, sequenceField.text, enabledBox.isSelected)

    private fun previewText(sequence: String): String =
        KeySequenceCodec.toHexPreview(sequence).ifEmpty { "(empty)" }
}
