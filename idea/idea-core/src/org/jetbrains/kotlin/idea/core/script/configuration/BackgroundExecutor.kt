/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Alarm
import com.intellij.util.containers.HashSetQueue
import org.jetbrains.kotlin.idea.core.script.debug
import java.util.*

/**
 * Sequentially loads script dependencies in background requested by [ensureScheduled].
 * Progress indicator will be shown
 *
 * States:
 *                                 silentWorker     underProgressWorker
 * - sleep
 * - silent                             x
 * - silent and under progress          x                 x
 * - under progress                                       x
 */
internal class BackgroundExecutor(
    val project: Project,
    val rootsManager: ScriptClassRootsManager
) {
    private var batchMaxSize = 0
    private val work = Any()
    private val queue: Queue<LoadTask> = HashSetQueue()

    private var silentWorker: SilentWorker? = null
    private var underProgressWorker: UnderProgressWorker? = null
    private val longRunningAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, project)
    private var longRunningAlaramRequested = false

    private var inTransaction: Boolean = false

    class LoadTask(val key: VirtualFile, val actions: () -> Unit) {
        override fun equals(other: Any?) =
            this === other || (other is LoadTask && key == other.key)

        override fun hashCode() = key.hashCode()
    }

    @Synchronized
    fun ensureScheduled(key: VirtualFile, actions: () -> Unit) {
        val task = LoadTask(key, actions)

        if (queue.add(task)) {
            debug(task.key) { "added to update queue" }

            batchMaxSize = maxOf(batchMaxSize, queue.size)

            // If the queue is longer than 3, show progress and cancel button
            if (queue.size > 3) {
                requireUnderProgressWorker()
            } else {
                requireSilentWorker()

                if (!longRunningAlaramRequested) {
                    longRunningAlaramRequested = true
                    longRunningAlarm.addRequest(
                        {
                            longRunningAlaramRequested = false
                            requireUnderProgressWorker()
                        },
                        1000
                    )
                }
            }
        }
    }

    @Synchronized
    private fun requireUnderProgressWorker() {
        if (queue.isEmpty() && silentWorker == null) return

        silentWorker?.stopGracefully()
        if (underProgressWorker == null) {
            underProgressWorker = UnderProgressWorker().also { it.start() }
        }
    }

    @Synchronized
    private fun requireSilentWorker() {
        if (silentWorker == null && underProgressWorker == null) {
            silentWorker = SilentWorker().also { it.start() }
        }
    }

    @Synchronized
    fun updateProgress(next: VirtualFile) {
        underProgressWorker?.progressIndicator?.let {
            it.isIndeterminate = true
            it.text2 = next.path
        }
    }

    @Synchronized
    private fun ensureInTransaction() {
        if (inTransaction) return
        inTransaction = true
        rootsManager.startTransaction()
    }

    @Synchronized
    private fun endBatch() {
        check(inTransaction)
        rootsManager.commit()
        inTransaction = false
    }

    private abstract inner class Worker {
        private var shouldStop = false

        open fun start() {
            ensureInTransaction()
        }

        fun stopGracefully() {
            shouldStop = true
        }

        protected open fun checkCancelled() = false
        protected abstract fun close()

        protected fun run() {
            try {
                while (true) {
                    // prevent parallel work in both silent and under progress
                    synchronized(work) {
                        val next = synchronized(this@BackgroundExecutor) {
                            if (shouldStop) return

                            if (checkCancelled() || queue.isEmpty()) {
                                endBatch()
                                return
                            }

                            queue.poll()?.also {
                                updateProgress(it.key)
                            }
                        }

                        next?.actions()
                    }
                }
            } finally {
                close()
            }
        }
    }

    private inner class UnderProgressWorker : Worker() {
        var progressIndicator: ProgressIndicator? = null

        override fun start() {
            super.start()

            object : Task.Backgroundable(project, "Kotlin: Loading script dependencies...", true) {
                override fun run(indicator: ProgressIndicator) {
                    progressIndicator = indicator
                    run()
                }
            }.queue()
        }

        override fun checkCancelled(): Boolean = progressIndicator?.isCanceled == true

        override fun close() {
            synchronized(this@BackgroundExecutor) {
                underProgressWorker = null
                progressIndicator = null
            }
        }
    }

    private inner class SilentWorker : Worker() {
        override fun start() {
            super.start()

            BackgroundTaskUtil.executeOnPooledThread(project, Runnable {
                run()
            })
        }

        override fun close() {
            synchronized(this@BackgroundExecutor) {
                silentWorker = null
            }
        }
    }
}