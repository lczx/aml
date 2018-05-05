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

import android.os.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Arrays;

public class ReflectiveModuleLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ReflectiveModuleLoader.class);

    private final ModuleManager moduleManager;

    public ReflectiveModuleLoader(final ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public void addModules(final Bundle modulesBundle) {
        for (final String name : modulesBundle.keySet())
            load(name, modulesBundle.getBundle(name));
    }

    private void load(final String moduleClassName, final Bundle parameters) {
        try {
            final Class<?> moduleClass = Class.forName(moduleClassName);
            if (Arrays.asList(moduleClass.getInterfaces()).contains(AMLTunnelModule.class))
                load(moduleClass, parameters);
            else
                LOG.error("Module \"{}\" does not implement AMLTunnelModule and will not be loaded");
        } catch (final ClassNotFoundException e) {
            LOG.error("Module \"{}\" was not found in the current class path", e);
        }
    }

    private void load(final Class<?> moduleClass, final Bundle parameters) {
        final AMLModule ann = moduleClass.getAnnotation(AMLModule.class);
        if (ann == null) {
            LOG.error("Module \"{}\" is not annotated with @AMLModule and will not be loaded", moduleClass.getName());
            return;
        }

        try {
            moduleManager.addModule(ann.name(), initModule(moduleClass, parameters), ann.priority());
        } catch (final NoSuchMethodException e) {
            LOG.error("Module \"" + moduleClass.getName() + "\" has parameters but no matching constructor was found");
        } catch (final ReflectiveOperationException e) {
            LOG.error("Module \"" + moduleClass.getName() + "\" can not be instantiated", e);
        }
    }

    private AMLTunnelModule initModule(final Class<?> moduleClass, final Bundle parameters)
            throws ReflectiveOperationException {
        try {
            // Try to get the parameterized constructor
            final Constructor<?> constructor = moduleClass.getConstructor(ModuleParameters.class);
            return (AMLTunnelModule) constructor.newInstance(new ModuleParametersBundle(parameters));
        } catch (final NoSuchMethodException e) {
            // If not found, and we have no parameters, try with the default constructor
            if (parameters == null) {
                return (AMLTunnelModule) moduleClass.newInstance();
            } else {
                // We have parameters but no appropriate constructor, throw
                throw e;
            }
        }
    }

    private class ModuleParametersBundle implements ModuleParameters {
        private final Bundle bundle;

        private ModuleParametersBundle(final Bundle bundle) {
            this.bundle = bundle;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getParameter(final String name) {
            return (T) bundle.get(name);
        }
    }

}
