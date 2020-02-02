package me.fornever.avaloniarider.idea.editor

import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class AvaloniaPreviewEditorProvider : FileEditorProvider {
    override fun getEditorTypeId() = "AvaloniaPreviewerEditor"
    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    override fun accept(project: Project, file: VirtualFile) =
            file.extension == "xaml" // TODO: Backend XAML file check

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val textEditor = TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
        return TextEditorWithPreview(textEditor, AvaloniaPreviewEditor())
    }
}
