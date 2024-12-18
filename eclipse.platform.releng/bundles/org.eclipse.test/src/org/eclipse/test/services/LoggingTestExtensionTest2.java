/*******************************************************************************
 * Copyright (c) 2024 Joerg Kubitz and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Joerg Kubitz - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.services;

import org.junit.jupiter.api.Test;

// Manual demonstration:
public class LoggingTestExtensionTest2 {

	@Test
	public void testWritten() {
		System.out.println("real");
	}

	@Test
	public void testWritten2() {
		throw new RuntimeException("intended");
	}

}
