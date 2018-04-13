/*
 * Copyright 2018 Luca Zanussi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.lczx.aml.tunnel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskRunner implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(TaskRunner.class);
    private static final int IDLE_SLEEP_TIME_MS = 10;

    private final String name;
    private final Task[] tasks;

    public TaskRunner(final String name, final Task... tasks) {
        this.name = name;
        this.tasks = tasks;
    }

    @Override
    public void run() {
        for (final Task task : tasks) task.initialize();

        Task currentTask = null;
        try {
            while (!Thread.interrupted()) {
                boolean didWork = false;

                for (final Task task : tasks)
                    didWork = didWork || (currentTask = task).loop();

                // TODO: Sleep-looping can be a battery drainer, we could use blocking queues but
                //       then we may need separate threads or [https://stackoverflow.com/a/9706482]
                if (!didWork) Thread.sleep(IDLE_SLEEP_TIME_MS);
            }
        } catch (final InterruptedException e) {
            // We were stopped while idle
        } catch (final Exception e) {
            LOG.error("Unhandled exception caught by \"" + name + "\" from task \"" + currentTask + '"', e);
        } finally {
            LOG.debug("Stopping looper \"{}\"", name);
            for (final Task task : tasks) task.terminate();
        }
    }

    public interface Task {
        void initialize();

        boolean loop() throws Exception;

        void terminate();
    }

}
