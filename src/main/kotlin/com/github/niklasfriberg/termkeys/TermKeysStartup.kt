package com.github.niklasfriberg.termkeys

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/** Registers the Shift+Enter interceptor once per project, disposed with the project. */
class TermKeysStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        val disposable = project.service<TermKeysService>()
        IdeEventQueue.getInstance().addDispatcher(TermKeysDispatcher(project), disposable)
    }
}
