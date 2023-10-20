/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.test.internal.performance.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.test.internal.performance.OSPerformanceMeter;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;
import org.junit.jupiter.api.Test;

public class SimplePerformanceMeterTest {

    @Test
    public void testPerformanceMeterFactory() {
        Performance performance = Performance.getDefault();
        PerformanceMeter meter = performance.createPerformanceMeter(performance.getDefaultScenarioId(this.getClass()));

        assertTrue(meter instanceof OSPerformanceMeter);

        meter.start();
        meter.stop();

        meter.commit();

        meter.dispose();
    }

}
