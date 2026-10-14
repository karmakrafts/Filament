/*
 * Copyright 2026 Karma Krafts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.karmakrafts.filament

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SharedMutexTest {
    private val mutex: SharedMutex = SharedMutex()
    private var theValue: Int = 0

    private fun tryReadLockFromAnotherThread(): Boolean = Thread<Boolean> {
        val acquired = mutex.tryLock()
        if (acquired) mutex.unlock()
        acquired
    }.awaitResult()

    private fun tryWriteLockFromAnotherThread(): Boolean = Thread<Boolean> {
        val acquired = mutex.tryLockWrite()
        if (acquired) mutex.unlockWrite()
        acquired
    }.awaitResult()

    @Test
    fun `Try read lock succeeds on an unlocked mutex`() {
        val acquired = mutex.tryLock()
        try {
            assertTrue(acquired)
        }
        finally {
            if (acquired) mutex.unlock()
        }
    }

    @Test
    fun `Try write lock succeeds on an unlocked mutex`() {
        val acquired = mutex.tryLockWrite()
        try {
            assertTrue(acquired)
        }
        finally {
            if (acquired) mutex.unlockWrite()
        }
    }

    @Test
    fun `Set value from another thread`() {
        val thread = Thread {
            mutex.withWriteLock {
                theValue = 4444
            }
        }
        thread.join()
        val thread2 = Thread<Int> {
            mutex.withLock {
                theValue
            }
        }
        val result = thread2.awaitResult()
        assertEquals(4444, result)
        mutex.withLock {
            assertEquals(4444, theValue)
        }
    }

    @Test
    fun `Create multiple shared mutexes in a loop`() {
        for (index in 0..<1000) {
            val mutex = SharedMutex()
            val thread = Thread {
                mutex.withWriteLock {
                    theValue = index
                }
            }
            thread.join()
            mutex.withLock {
                assertEquals(index, theValue)
            }
        }
    }

    @Test
    fun `Read lock permits other readers but excludes writers`() {
        mutex.withLock {
            assertTrue(tryReadLockFromAnotherThread())
            assertFalse(tryWriteLockFromAnotherThread())
            assertFalse(tryWriteLockFromAnotherThread())
        }
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `Write lock excludes readers and writers`() {
        mutex.withWriteLock {
            assertFalse(tryReadLockFromAnotherThread())
            assertFalse(tryWriteLockFromAnotherThread())
        }
        assertTrue(tryReadLockFromAnotherThread())
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `Separate shared mutexes can be locked independently`() {
        val other = SharedMutex()
        mutex.withWriteLock {
            val acquired = Thread<Boolean> {
                other.tryWithWriteLock(false) { true }
            }.awaitResult()
            assertTrue(acquired)
            assertFalse(tryReadLockFromAnotherThread())
            assertFalse(tryWriteLockFromAnotherThread())
        }
    }

    @Test
    fun `With read lock returns the result and releases the mutex`() {
        assertEquals(42, mutex.withLock { 42 })
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `With read lock releases the mutex when the block throws`() {
        val exception = IllegalStateException("Expected failure")
        assertSame(exception, assertFailsWith<IllegalStateException> {
            mutex.withLock<Unit> { throw exception }
        })
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `With write lock returns the result and releases the mutex`() {
        assertEquals(42, mutex.withWriteLock { 42 })
        assertTrue(tryReadLockFromAnotherThread())
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `With write lock releases the mutex when the block throws`() {
        val exception = IllegalStateException("Expected failure")
        assertSame(exception, assertFailsWith<IllegalStateException> {
            mutex.withWriteLock<Unit> { throw exception }
        })
        assertTrue(tryReadLockFromAnotherThread())
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `Try with read lock returns the result and releases the mutex`() {
        var calls = 0
        val result = mutex.tryWithLock(-1) {
            calls++
            assertTrue(tryReadLockFromAnotherThread())
            assertFalse(tryWriteLockFromAnotherThread())
            42
        }
        assertEquals(42, result)
        assertEquals(1, calls)
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `Try with read lock skips the block while a writer owns the mutex`() {
        mutex.withWriteLock {
            val result = Thread<Pair<Int, Boolean>> {
                var called = false
                val value = mutex.tryWithLock(-1) {
                    called = true
                    42
                }
                value to called
            }.awaitResult()
            assertEquals(-1 to false, result)
            assertFalse(tryWriteLockFromAnotherThread())
        }
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `Try with read lock releases the mutex when the block throws`() {
        val exception = IllegalStateException("Expected failure")
        assertSame(exception, assertFailsWith<IllegalStateException> {
            mutex.tryWithLock(Unit) { throw exception }
        })
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `Try with write lock returns the result and releases the mutex`() {
        var calls = 0
        val result = mutex.tryWithWriteLock(-1) {
            calls++
            assertFalse(tryReadLockFromAnotherThread())
            assertFalse(tryWriteLockFromAnotherThread())
            42
        }
        assertEquals(42, result)
        assertEquals(1, calls)
        assertTrue(tryReadLockFromAnotherThread())
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `Try with write lock skips the block while a reader owns the mutex`() {
        mutex.withLock {
            val result = Thread<Pair<Int, Boolean>> {
                var called = false
                val value = mutex.tryWithWriteLock(-1) {
                    called = true
                    42
                }
                value to called
            }.awaitResult()
            assertEquals(-1 to false, result)
            assertFalse(tryWriteLockFromAnotherThread())
        }
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `Try with write lock skips the block while a writer owns the mutex`() {
        mutex.withWriteLock {
            val result = Thread<Pair<Int, Boolean>> {
                var called = false
                val value = mutex.tryWithWriteLock(-1) {
                    called = true
                    42
                }
                value to called
            }.awaitResult()
            assertEquals(-1 to false, result)
            assertFalse(tryReadLockFromAnotherThread())
        }
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `Try with write lock releases the mutex when the block throws`() {
        val exception = IllegalStateException("Expected failure")
        assertSame(exception, assertFailsWith<IllegalStateException> {
            mutex.tryWithWriteLock(Unit) { throw exception }
        })
        assertTrue(tryReadLockFromAnotherThread())
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `Write lock helpers support null results`() {
        assertNull(mutex.withWriteLock<String?> { null })
        assertTrue(tryWriteLockFromAnotherThread())
        assertNull(mutex.tryWithWriteLock<String?>("default") { null })
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun `Write lockable delegates to the write lock`() {
        mutex.writeLockable.withLock {
            assertFalse(tryReadLockFromAnotherThread())
            assertFalse(tryWriteLockFromAnotherThread())
        }
        assertTrue(tryWriteLockFromAnotherThread())
        assertTrue(mutex.writeLockable.tryWithLock(false) {
            assertFalse(tryReadLockFromAnotherThread())
            assertFalse(tryWriteLockFromAnotherThread())
            true
        })
        assertTrue(tryWriteLockFromAnotherThread())
        mutex.withLock {
            val acquired = Thread<Boolean> {
                mutex.writeLockable.tryWithLock(false) { true }
            }.awaitResult()
            assertFalse(acquired)
        }
    }

    @Test
    fun `Concurrent writers do not lose updates`() {
        val threads = mutableListOf<Thread>()
        try {
            mutex.withWriteLock {
                repeat(4) {
                    threads += Thread {
                        repeat(1000) {
                            mutex.withWriteLock {
                                theValue++
                            }
                        }
                    }
                }
            }
        }
        finally {
            threads.forEach { it.join() }
        }
        mutex.withLock {
            assertEquals(4000, theValue)
        }
    }
}