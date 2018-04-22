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

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class HookRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(HookRegistry.class);
    private static final int DEFAULT_PROCEDURE_PRIORITY = 100;

    private final Map<String, SortedSet<HookHolder>> hooks = new HashMap<>();

    public void registerProcedure(final HookProcedure hookProcedure) {
        registerProcedure(hookProcedure, DEFAULT_PROCEDURE_PRIORITY);
    }

    public void registerProcedure(final HookProcedure hookProcedure, final int priority) {
        final HookType hookType = hookProcedure.getClass().getAnnotation(HookType.class);
        if (hookType != null)
            getProcedureSet(hookType.value()).add(new HookHolder(hookProcedure, priority));
        else
            LOG.error("Hook procedure class \"{}\" has no @HookType, ignoring", hookProcedure.getClass());
    }

    public Hook obtainHook(final String hookType) {
        return new Hook(getProcedureSet(hookType));
    }

    private SortedSet<HookHolder> getProcedureSet(final String hookType) {
        SortedSet<HookHolder> pSet = hooks.get(hookType);
        if (pSet == null) {
            pSet = new TreeSet<>();
            hooks.put(hookType, pSet);
        }
        return pSet;
    }

    /* package */ static class HookHolder implements Comparable<HookHolder> {
        public final HookProcedure procedure;
        public final int priority;

        private HookHolder(final HookProcedure procedure, final int priority) {
            this.procedure = procedure;
            this.priority = priority;
        }

        @Override
        public int compareTo(final HookHolder o) {
            return priority - o.priority;
        }
    }

}
