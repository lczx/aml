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

import java.util.Arrays;

public class ReflectiveModuleLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ReflectiveModuleLoader.class);

    private final ModuleManager moduleManager;

    public ReflectiveModuleLoader(final ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public void addModules(final Class<? extends AMLTunnelModule>... classes) {
        for (final Class<? extends AMLTunnelModule> clazz : classes) load(clazz);
    }

    public void addModules(final String... classNames) {
        for (final String name : classNames) load(name);
    }

    private void load(final String moduleClassName) {
        try {
            final Class<?> moduleClass = Class.forName(moduleClassName);
            if (Arrays.asList(moduleClass.getInterfaces()).contains(AMLTunnelModule.class))
                load(moduleClass);
            else
                LOG.error("Module \"{}\" does not implement AMLTunnelModule and will not be loaded");
        } catch (final ClassNotFoundException e) {
            LOG.error("Module \"{}\" was not found in the current class path", e);
        }
    }

    private void load(final Class<?> moduleClass) {
        final AMLModule ann = moduleClass.getAnnotation(AMLModule.class);
        if (ann == null) {
            LOG.error("Module \"{}\" is not annotated with @AMLModule and will not be loaded", moduleClass.getName());
            return;
        }

        try {
            moduleManager.addModule(ann.name(), (AMLTunnelModule) moduleClass.newInstance(), ann.priority());
        } catch (final ReflectiveOperationException e) {
            LOG.error("Module \"" + moduleClass.getName() + "\" can not be instantiated", e);
        }
    }

}
