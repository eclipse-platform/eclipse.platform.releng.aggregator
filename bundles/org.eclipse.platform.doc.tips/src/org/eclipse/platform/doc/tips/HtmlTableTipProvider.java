/*******************************************************************************
 * Copyright (c) 2018 vogella GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     simon.scholz@vogella.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.platform.doc.tips;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.core.TipProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class HtmlTableTipProvider extends TipProvider {

	private TipImage tipImage;

	@Override
	public String getDescription() {
		return Messages.HtmlTableProvider_0;
	}

	@Override
	public String getID() {
		return getClass().getName();
	}

	@Override
	public TipImage getImage() {
		if (tipImage == null) {
			Bundle bundle = FrameworkUtil.getBundle(getClass());

			try {
				tipImage = new TipImage(bundle.getEntry("icons/48/tips.png")); //$NON-NLS-1$
			} catch (IOException ex) {
				getManager().log(new Status(IStatus.ERROR, bundle.getSymbolicName(), ex.getMessage(), ex));
			}
		}
		return tipImage;
	}

	@Override
	public IStatus loadNewTips(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		Bundle bundle = Platform.getBundle("org.eclipse.platform.doc.user"); //$NON-NLS-1$
		URL platformTipsURL = bundle.getEntry("tips/platform_tips.html"); //$NON-NLS-1$
		try {
			String platformTipsHtmlContents = IOUtils.toString(platformTipsURL.openStream(),
					StandardCharsets.UTF_8.name());
			List<Tip> browserTips = HtmlExtractor.getTips(getID(), platformTipsHtmlContents, subMonitor);
			setTips(browserTips);
		} catch (IOException ex) {
			return new Status(IStatus.ERROR, bundle.getSymbolicName(), ex.getMessage(), ex);
		}
		return Status.OK_STATUS;
	}

	@Override
	public void dispose() {
	}
}
