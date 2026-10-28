package com.kturker.multiplatform.core.update

// Runs one block at a time: a run asked for while another is still going is dropped, not queued. Main thread
// only (button taps), so a plain flag is enough.
internal class OneAtATime {
    private var running = false

    suspend fun run(block: suspend () -> Unit) {
        if (running) return
        running = true
        try {
            block()
        } finally {
            running = false
        }
    }
}
