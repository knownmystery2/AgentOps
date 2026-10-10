package com.opensource.autonomous.agent.network

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Singleton Mutex wrapper ensuring that concurrent async calls from multiple 
 * background workers don't overwhelm local host's Ollama instance and trigger crash errors.
 */
object InferenceLock {
    private val mutex = Mutex()

    suspend fun <T> executeSafeInference(block: suspend () -> T): T {
        return mutex.withLock {
            block()
        }
    }
}
