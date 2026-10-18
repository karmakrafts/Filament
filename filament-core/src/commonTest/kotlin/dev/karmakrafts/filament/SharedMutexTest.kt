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
    fun tryReadLockSucceedsOnAnUnlockedMutex() {
        val acquired = mutex.tryLock()
        try {
            assertTrue(acquired)
        }
        finally {
            if (acquired) mutex.unlock()
        }
    }

    @Test
    fun tryWriteLockSucceedsOnAnUnlockedMutex() {
        val acquired = mutex.tryLockWrite()
        try {
            assertTrue(acquired)
        }
        finally {
            if (acquired) mutex.unlockWrite()
        }
    }

    @Test
    fun setValueFromAnotherThread() {
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
    fun createMultipleSharedMutexesInALoop() {
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
    fun readLockPermitsOtherReadersButExcludesWriters() {
        mutex.withLock {
            assertTrue(tryReadLockFromAnotherThread())
            assertFalse(tryWriteLockFromAnotherThread())
            assertFalse(tryWriteLockFromAnotherThread())
        }
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun writeLockExcludesReadersAndWriters() {
        mutex.withWriteLock {
            assertFalse(tryReadLockFromAnotherThread())
            assertFalse(tryWriteLockFromAnotherThread())
        }
        assertTrue(tryReadLockFromAnotherThread())
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun separateSharedMutexesCanBeLockedIndependently() {
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
    fun withReadLockReturnsTheResultAndReleasesTheMutex() {
        assertEquals(42, mutex.withLock { 42 })
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun withReadLockReleasesTheMutexWhenTheBlockThrows() {
        val exception = IllegalStateException("Expected failure")
        assertSame(exception, assertFailsWith<IllegalStateException> {
            mutex.withLock<Unit> { throw exception }
        })
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun withWriteLockReturnsTheResultAndReleasesTheMutex() {
        assertEquals(42, mutex.withWriteLock { 42 })
        assertTrue(tryReadLockFromAnotherThread())
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun withWriteLockReleasesTheMutexWhenTheBlockThrows() {
        val exception = IllegalStateException("Expected failure")
        assertSame(exception, assertFailsWith<IllegalStateException> {
            mutex.withWriteLock<Unit> { throw exception }
        })
        assertTrue(tryReadLockFromAnotherThread())
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun tryWithReadLockReturnsTheResultAndReleasesTheMutex() {
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
    fun tryWithReadLockSkipsTheBlockWhileAWriterOwnsTheMutex() {
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
    fun tryWithReadLockReleasesTheMutexWhenTheBlockThrows() {
        val exception = IllegalStateException("Expected failure")
        assertSame(exception, assertFailsWith<IllegalStateException> {
            mutex.tryWithLock(Unit) { throw exception }
        })
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun tryWithWriteLockReturnsTheResultAndReleasesTheMutex() {
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
    fun tryWithWriteLockSkipsTheBlockWhileAReaderOwnsTheMutex() {
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
    fun tryWithWriteLockSkipsTheBlockWhileAWriterOwnsTheMutex() {
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
    fun tryWithWriteLockReleasesTheMutexWhenTheBlockThrows() {
        val exception = IllegalStateException("Expected failure")
        assertSame(exception, assertFailsWith<IllegalStateException> {
            mutex.tryWithWriteLock(Unit) { throw exception }
        })
        assertTrue(tryReadLockFromAnotherThread())
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun writeLockHelpersSupportNullResults() {
        assertNull(mutex.withWriteLock<String?> { null })
        assertTrue(tryWriteLockFromAnotherThread())
        assertNull(mutex.tryWithWriteLock<String?>("default") { null })
        assertTrue(tryWriteLockFromAnotherThread())
    }

    @Test
    fun writeLockableDelegatesToTheWriteLock() {
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
    fun concurrentWritersDoNotLoseUpdates() {
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