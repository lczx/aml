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

package io.github.lczx.aml.hook.monitoring;

import android.os.Bundle;

import java.util.*;

public class StatusMonitor {

    private final List<ProbeHolder> probes = new LinkedList<>();

    public MeasureHolder performMeasure() {
        final MeasureHolder measureHolder = new MeasureHolderBundle(new Bundle());
        for (final ProbeHolder holder: probes)
            holder.probe.onMeasure(measureHolder);
        measureHolder.putString(BaseMeasureKeys.PROBE_NAMES, probes.toString());
        return measureHolder;
    }

    public void attachProbe(final StatusProbe statusProbe) {
        probes.add(new ProbeHolder(statusProbe));
    }

    private static class ProbeHolder {
        private final StatusProbe probe;

        private ProbeHolder(final StatusProbe probe) {
            this.probe = probe;
        }

        @Override
        public String toString() {
            return "ProbeHolder{" +
                    "probe=" + probe +
                    '}';
        }
    }

}
