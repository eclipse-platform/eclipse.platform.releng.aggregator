/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

package org.eclipse.test.internal.performance.db;

import org.eclipse.test.internal.performance.data.Dim;

public class SummaryEntry {

    public String  scenarioName;
    public String  shortName;
    public Dim     dimension;
    public boolean isGlobal;
    public int     commentKind;
    public String  comment;

    SummaryEntry(String scenarioName, String shortName, Dim dimension, boolean isGlobal) {
        this.scenarioName = scenarioName;
        this.shortName = shortName;
        this.dimension = dimension;
        this.isGlobal = isGlobal;
    }

    SummaryEntry(String scenarioName, String shortName, Dim dimension, boolean isGlobal, int commentKind, String comment) {
        this.scenarioName = scenarioName;
        this.shortName = shortName;
        this.dimension = dimension;
        this.isGlobal = isGlobal;
        this.commentKind = commentKind;
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "SummaryEntry(" + isGlobal + "): <" + scenarioName + "> <" + shortName + "> <" + dimension + '>'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
}
