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

class ReflectiveModuleLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ReflectiveModuleLoader.class);

    ModuleManager.ModuleHolder loadModule(final String moduleClassName) {
        try {
            final Class<?> moduleClass = Class.forName(moduleClassName);
            if (!Arrays.asList(moduleClass.getInterfaces()).contains(AMLTunnelModule.class)) {
                LOG.error("Module \"{}\" does not implement AMLTunnelModule and will not be loaded");
                return null;
            }
            return load(moduleClass);
        } catch (final ClassNotFoundException e) {
            LOG.error("Module \"{}\" was not found in the current class path", e);
            return null;
        }
    }

    ModuleManager.ModuleHolder loadModule(final Class<? extends AMLTunnelModule> moduleClass) {
        return load(moduleClass);
    }

    private ModuleManager.ModuleHolder load(final Class<?> moduleClass) {
        final AMLModule ann = moduleClass.getAnnotation(AMLModule.class);
        if (ann == null) {
            LOG.error("Module \"{}\" is not annotated with @AMLModule and will not be loaded", moduleClass.getName());
            return null;
        }

        try {
            return new ModuleManager.ModuleHolder(
                    ann.name(), ann.priority(), (AMLTunnelModule) moduleClass.newInstance());
        } catch (final ReflectiveOperationException e) {
            LOG.error("Module \"" + moduleClass.getName() + "\" can not be instantiated", e);
            return null;
        }
    }

}
