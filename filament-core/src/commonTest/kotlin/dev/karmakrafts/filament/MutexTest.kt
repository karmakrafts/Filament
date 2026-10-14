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

class MutexTest {
    private var theValue: Int = 0
    private val mutex = Mutex()

    private fun tryLockFromAnotherThread(): Boolean = Thread<Boolean> {
        val acquired = mutex.tryLock()
        if (acquired) mutex.unlock()
        acquired
    }.awaitResult()

    @Test
    fun `Try lock succeeds on an unlocked mutex`() {
        val acquired = mutex.tryLock()
        try {
            assertTrue(acquired)
        }
        finally {
            if (acquired) mutex.unlock()
        }
    }

    @Test
    fun `Set value from another thread`() {
        val thread = Thread {
            mutex.withLock {
                theValue = 4444
            }
        }
        thread.join()
        mutex.withLock {
            assertEquals(4444, theValue)
        }
    }

    @Test
    fun `Create multiple mutexes in a loop`() {
        for (index in 0..<1000) {
            val mutex = Mutex()
            val thread = Thread {
                mutex.withLock {
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
    fun `Try lock fails while another thread owns the mutex`() {
        mutex.withLock {
            assertFalse(tryLockFromAnotherThread())
            assertFalse(tryLockFromAnotherThread())
        }
        assertTrue(tryLockFromAnotherThread())
    }

    @Test
    fun `Separate mutexes can be locked independently`() {
        val other = Mutex()
        mutex.withLock {
            val acquired = Thread<Boolean> {
                other.tryWithLock(false) { true }
            }.awaitResult()
            assertTrue(acquired)
            assertFalse(tryLockFromAnotherThread())
        }
    }

    @Test
    fun `With lock returns the result and releases the mutex`() {
        val result = mutex.withLock {
            assertFalse(tryLockFromAnotherThread())
            42
        }
        assertEquals(42, result)
        assertTrue(tryLockFromAnotherThread())
    }

    @Test
    fun `With lock releases the mutex when the block throws`() {
        val exception = IllegalStateException("Expected failure")
        assertSame(exception, assertFailsWith<IllegalStateException> {
            mutex.withLock<Unit> { throw exception }
        })
        assertTrue(tryLockFromAnotherThread())
    }

    @Test
    fun `Try with lock returns the result and releases the mutex`() {
        var calls = 0
        val result = mutex.tryWithLock(-1) {
            calls++
            assertFalse(tryLockFromAnotherThread())
            42
        }
        assertEquals(42, result)
        assertEquals(1, calls)
        assertTrue(tryLockFromAnotherThread())
    }

    @Test
    fun `Try with lock skips the block when another thread owns the mutex`() {
        mutex.withLock {
            val result = Thread<Pair<Int, Boolean>> {
                var called = false
                val value = mutex.tryWithLock(-1) {
                    called = true
                    42
                }
                value to called
            }.awaitResult()
            assertEquals(-1 to false, result)
            assertFalse(tryLockFromAnotherThread())
        }
        assertTrue(tryLockFromAnotherThread())
    }

    @Test
    fun `Try with lock releases the mutex when the block throws`() {
        val exception = IllegalStateException("Expected failure")
        assertSame(exception, assertFailsWith<IllegalStateException> {
            mutex.tryWithLock(Unit) { throw exception }
        })
        assertTrue(tryLockFromAnotherThread())
    }

    @Test
    fun `Lock helpers support null results`() {
        assertNull(mutex.withLock<String?> { null })
        assertTrue(tryLockFromAnotherThread())
        assertNull(mutex.tryWithLock<String?>("default") { null })
        assertTrue(tryLockFromAnotherThread())
    }

    @Test
    fun `Concurrent increments do not lose updates`() {
        val threads = mutableListOf<Thread>()
        try {
            mutex.withLock {
                repeat(4) {
                    threads += Thread {
                        repeat(1000) {
                            mutex.withLock {
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