/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
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
