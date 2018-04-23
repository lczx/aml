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

package io.github.lczx.aml.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class EventDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(EventDispatcher.class);
    private static final int DEFAULT_PROCEDURE_PRIORITY = 100;

    private final Map<Class<? extends AMLEvent>, SortedSet<ListenerWrapper>> listeners = new HashMap<>();

    @SafeVarargs
    public final void addEventListener(final AMLEventListener eventListener, final Class<? extends AMLEvent>... events) {
        addEventListener(eventListener, DEFAULT_PROCEDURE_PRIORITY, events);
    }

    @SafeVarargs
    public final void addEventListener(final AMLEventListener eventListener, final int priority,
                                       final Class<? extends AMLEvent>... events) {
        for (final Class<? extends AMLEvent> eventClass : events)
            getListeners(eventClass).add(new ListenerWrapper(eventListener, priority));
        LOG.debug("Registered listener {} for event types {} (priority: {})",
                eventListener, Arrays.toString(events), priority);
    }

    public void sendEvent(final AMLEvent event) {
        for (final ListenerWrapper lw : getListeners(event.getClass())) {
            LOG.trace("Sending {} to {} (priority: {})", event, lw.listener, lw.priority);
            lw.listener.receiveEvent(event);
        }
    }

    private SortedSet<ListenerWrapper> getListeners(final Class<? extends AMLEvent> eventClass) {
        SortedSet<ListenerWrapper> lSet = listeners.get(eventClass);
        if (lSet == null) {
            lSet = new TreeSet<>();
            listeners.put(eventClass, lSet);
        }
        return lSet;
    }

    /* package */ static class ListenerWrapper implements Comparable<ListenerWrapper> {
        public final AMLEventListener listener;
        public final int priority;

        private ListenerWrapper(final AMLEventListener listener, final int priority) {
            this.listener = listener;
            this.priority = priority;
        }

        @Override
        public int compareTo(final ListenerWrapper o) {
            final int delta = this.priority - o.priority;
            return delta != 0 ? delta : 1; // If the two have the same priority, put the new one after
        }
    }

}
