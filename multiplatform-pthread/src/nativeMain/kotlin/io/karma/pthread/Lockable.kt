package io.karma.pthread

/**
 * @author Alexander Hinze
 * @since 27/12/2024
 */
interface Lockable {
    fun lock()

    fun tryLock(): Boolean

    fun unlock()
}

inline fun <reified R> Lockable.guarded(closure: () -> R): R {
    try {
        lock()
        return closure()
    }
    finally {
        unlock()
    }
}

inline fun <reified R> Lockable.tryGuarded(defaultValue: R, closure: () -> R): R {
    if (!tryLock()) return defaultValue
    try {
        return closure()
    }
    finally {
        unlock()
    }
}