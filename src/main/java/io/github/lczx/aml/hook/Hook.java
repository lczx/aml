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

import java.util.SortedSet;

public class Hook {

    private final SortedSet<HookRegistry.HookHolder> procedureSet;

    /* package */ Hook(final SortedSet<HookRegistry.HookHolder> procedureSet) {
        this.procedureSet = procedureSet;
    }

    public void execute(final Object... args) {
        for (final HookRegistry.HookHolder p : procedureSet)
            p.procedure.onEnter(args);
    }

}
