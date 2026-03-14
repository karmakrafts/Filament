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

@file:OptIn(ExperimentalForeignApi::class)

package dev.karmakrafts.filament

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import platform.darwin.THREAD_AFFINITY_POLICY
import platform.darwin.thread_affinity_policy_data_t
import platform.darwin.thread_policy_set
import platform.darwin.thread_resume
import platform.posix.PTHREAD_CREATE_DETACHED
import platform.posix.PTHREAD_CREATE_JOINABLE
import platform.posix._SC_NPROCESSORS_ONLN
import platform.posix.pthread_attr_init
import platform.posix.pthread_attr_setdetachstate
import platform.posix.pthread_attr_setstacksize
import platform.posix.pthread_attr_t
import platform.posix.pthread_create_suspended_np
import platform.posix.pthread_mach_thread_np
import platform.posix.pthread_tVar
import platform.posix.sysconf

actual fun Thread( // @formatter:off
    affinity: Int,
    stackSize: Long,
    detached: Boolean,
    function: () -> Unit
): Thread = memScoped { // @formatter:on
    val handleAddress = alloc<pthread_tVar>()
    val attributes = alloc<pthread_attr_t>()
    check(pthread_attr_init(attributes.ptr) == 0) {
        "Could not initialize thread attributes"
    }
    if (stackSize != Thread.DEFAULT_STACK_SIZE) {
        check(pthread_attr_setstacksize(attributes.ptr, stackSize.convert()) == 0) {
            "Could not set thread stack size"
        }
    }
    val detachState = if (detached) PTHREAD_CREATE_DETACHED else PTHREAD_CREATE_JOINABLE
    check(pthread_attr_setdetachstate(attributes.ptr, detachState) == 0) {
        "Could not set thread detach state"
    }
    val trampolineAddress = staticCFunction(::threadTrampoline)
    val closureAddress = StableRef.create(function).asCPointer()
    check(pthread_create_suspended_np(handleAddress.ptr, attributes.ptr, trampolineAddress, closureAddress) == 0) {
        "Could not create thread"
    }
    val handle = checkNotNull(handleAddress.value) { "Could not obtain thread handle" }
    val kernelThread = pthread_mach_thread_np(handle)
    if (affinity != Thread.NO_AFFINITY) {
        val coreCount = sysconf(_SC_NPROCESSORS_ONLN).toInt()
        check(affinity < coreCount) { "Affinity must be less than the number of logical cores" }
        val affinityPolicy = alloc<thread_affinity_policy_data_t> {
            affinity_tag = affinity
        }
        thread_policy_set(kernelThread, THREAD_AFFINITY_POLICY.toUInt(), affinityPolicy.ptr.reinterpret(), 1U)
    }
    thread_resume(kernelThread)
    threadAffinity = affinity
    AppleThread(handle, affinity)
}