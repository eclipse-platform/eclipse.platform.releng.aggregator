/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

package org.eclipse.test.internal.performance.eval;

import org.eclipse.test.performance.PerformanceMeter;

/**
 * The empty evaluator. Does nothing.
 */
public class EmptyEvaluator implements IEvaluator {

    @Override
    public void evaluate(PerformanceMeter performanceMeter) throws RuntimeException {
        // empty
    }

    @Override
    public void setAssertCheckers(AssertChecker[] asserts) {
        // empty
    }

    @Override
    public void setReferenceFilterProperties(String driver, String timestamp) {
        // empty
    }
}
