package com.minimo.launcher.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** "Removed · Undo" message; [undo] puts things back. */
class UndoRequest(val message: String, val undo: suspend () -> Unit)

/** Shows a short "Undo" bar after destructive actions (see UndoHost). */
@Singleton
class UndoController @Inject constructor() {
    private val _requests = MutableSharedFlow<UndoRequest>(extraBufferCapacity = 4)
    val requests: SharedFlow<UndoRequest> = _requests.asSharedFlow()

    fun offer(message: String, undo: suspend () -> Unit) {
        _requests.tryEmit(UndoRequest(message, undo))
    }
}
