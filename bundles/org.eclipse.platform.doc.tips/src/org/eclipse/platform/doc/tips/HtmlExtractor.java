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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tips.core.DefaultHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public final class HtmlExtractor {

	/**
	 * This method is used to parse tables in a HTML document, which have a subject
	 * in the left column and content in the right column. New and noteworthy or
	 * help contents from Eclipse is usually structured like that.
	 * 
	 * @param html              String containing HTML
	 * @param lastModifiedValue last modified date of the tip
	 * @param monitor           {@link IProgressMonitor}
	 * @return a list of {@link Tip} objects
	 * @throws OperationCanceledException
	 * @throws IOException
	 */
	public static List<Tip> getTipsFromEclipseHtmlTable(String providerId, String html, Date lastModifiedValue,
			IProgressMonitor monitor) throws OperationCanceledException, IOException {
		Document doc = Jsoup.parse(html);
		Elements trElements = doc.select("tr"); //$NON-NLS-1$
		if (!trElements.isEmpty()) {
			List<Tip> subjectAndHtmlList = new ArrayList<>();
			SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.HtmlExtractor_0, trElements.size());
			for (Element element : trElements) {
				DefaultHtmlTip browserTip = createBrowserTip(providerId, lastModifiedValue, element,
						subMonitor.split(1));
				subjectAndHtmlList.add(browserTip);
			}
			return subjectAndHtmlList;
		}

		return Collections.emptyList();
	}

	private static DefaultHtmlTip createBrowserTip(String providerId, Date lastModifiedValue, Element element,
			SubMonitor subMonitor) throws IOException {
		Elements tdElements = element.select("td"); //$NON-NLS-1$
		Element subject = tdElements.first();
		Element content = tdElements.last();

		String html = createHtmlContent(subject, content);

		DefaultHtmlTip browserTip = new DefaultHtmlTip(providerId, lastModifiedValue, subject.text(), html);
		return browserTip;
	}

	private static String createHtmlContent(Element subject, Element content) throws IOException {

		base64ImageRefs(content);

		StringBuilder sb = new StringBuilder();
		sb.append("<html>"); //$NON-NLS-1$
		sb.append("<body>"); //$NON-NLS-1$
		sb.append("<h1>"); //$NON-NLS-1$
		sb.append(subject.text());
		sb.append("</h1>"); //$NON-NLS-1$
		sb.append(content.html());
		sb.append("</body>"); //$NON-NLS-1$
		sb.append("</html>"); //$NON-NLS-1$
		String html = sb.toString();
		return html;
	}

	private static void base64ImageRefs(Element content) throws IOException {

		Elements imgs = content.select("img[src]"); //$NON-NLS-1$
		for (Element imgElement : imgs) {
			String srcAttr = imgElement.attr("src"); //$NON-NLS-1$
			if (srcAttr.startsWith("PLUGINS_ROOT")) { //$NON-NLS-1$
				String[] splittedImgSrc = srcAttr.split("/"); //$NON-NLS-1$
				String bundleSymbolicName = splittedImgSrc[1];
				String imgPath = splittedImgSrc[2];
				changeImgSrc(imgElement, imgPath, bundleSymbolicName);
			} else if (srcAttr.startsWith("../")) { //$NON-NLS-1$
				changeImgSrc(imgElement, srcAttr.substring(3), "org.eclipse.platform.doc.user"); //$NON-NLS-1$
			} else {
				changeImgSrc(imgElement, "tips/" + srcAttr, "org.eclipse.platform.doc.user"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	private static void changeImgSrc(Element imgElement, String srcAttr, String bundleSymbolicName) throws IOException {
		Bundle bundle = Platform.getBundle(bundleSymbolicName);
		URL imgUrl = bundle.getEntry(srcAttr);
		if (imgUrl == null) {
			Bundle thisBundle = FrameworkUtil.getBundle(HtmlExtractor.class);
			Platform.getLog(thisBundle).log(new Status(IStatus.WARNING, thisBundle.getSymbolicName(),
					srcAttr + " not found in " + bundle.getSymbolicName() + " bundle.")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		TipImage tipImage = new TipImage(imgUrl);
		String base64Image = tipImage.getBase64Image();
		imgElement.attr("src", base64Image); //$NON-NLS-1$
	}
}
