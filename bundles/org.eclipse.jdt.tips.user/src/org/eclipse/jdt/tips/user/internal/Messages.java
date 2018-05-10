package org.eclipse.jdt.tips.user.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.jdt.tips.user.internal.messages"; //$NON-NLS-1$
	public static String JDTTipProvider_1;
	public static String JDTTipProvider_2;
	public static String JDTTipProvider_3;
	public static String JDTTipProvider_5;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
