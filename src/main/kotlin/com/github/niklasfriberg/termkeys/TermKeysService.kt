package com.github.niklasfriberg.termkeys

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service

/** Project-scoped Disposable used to tie the event dispatcher's lifetime to the project. */
@Service(Service.Level.PROJECT)
class TermKeysService : Disposable {
    override fun dispose() {}
}
