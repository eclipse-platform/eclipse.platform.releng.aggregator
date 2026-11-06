/*******************************************************************************
 * Copyright (c) 2026 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Utilities for OSGI tracing in tests.
 */
public class TracingUtils {

	/**
	 * Enables or disables debug traces.
	 *
	 * @param classFromBundle   A class from the bundle for which the debug tracing
	 *                          should be enabled or disabled.
	 * @param enable            whether to enable or disable debug tracing
	 * @param extraDebugOptions extra debug tracing options to enable or disable
	 */
	public static void setDebugEnabled(Class<?> classFromBundle, boolean enable, String... extraDebugOptions) {
		Bundle bundle = FrameworkUtil.getBundle(classFromBundle);
		BundleContext context = bundle.getBundleContext();
		ServiceReference<DebugOptions> reference = context.getServiceReference(DebugOptions.class);
		try {
			DebugOptions options = context.getService(reference);
			options.setDebugEnabled(enable);
			List<String> flags = new ArrayList<>();
			flags.add("debug");
			flags.addAll(Arrays.asList(extraDebugOptions));
			String prefix = bundle.getSymbolicName() + "/";
			String enabled = enable ? Boolean.TRUE.toString() : Boolean.FALSE.toString();
			for (String flag : flags) {
				options.setOption(prefix + flag, enabled);
			}
		} finally {
			context.ungetService(reference);
		}
	}
}
