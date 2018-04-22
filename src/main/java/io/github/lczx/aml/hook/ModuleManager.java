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

import io.github.lczx.aml.AMLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.TreeSet;

public class ModuleManager {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleManager.class);

    private final HookRegistry hookRegistry = new HookRegistry();
    private final Set<ModuleHolder> modules = new TreeSet<>();

    public void startModules(final AMLContext amlContext) {
        for (final ModuleHolder m : modules)
            m.moduleInstance.onStart(amlContext);
    }

    public void stopModules() {
        for (final ModuleHolder m : modules)
            m.moduleInstance.onStop();
    }

    public void addModule(final String name, final AMLTunnelModule module, final int priority) {
        module.initialize(hookRegistry);
        modules.add(new ModuleHolder(name, priority, module));
        LOG.debug("Loaded module\"{}\", priority {}", name, priority);
    }

    public HookRegistry getHookRegistry() {
        return hookRegistry;
    }

    private static class ModuleHolder implements Comparable<ModuleHolder> {
        private final String name;
        private final int priority;
        private final AMLTunnelModule moduleInstance;

        private ModuleHolder(final String name, final int priority, final AMLTunnelModule moduleInstance) {
            this.name = name;
            this.priority = priority;
            this.moduleInstance = moduleInstance;
        }

        @Override
        public int compareTo(final ModuleHolder o) {
            return this.priority - o.priority;
        }
    }

}
