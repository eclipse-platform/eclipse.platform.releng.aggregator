/*******************************************************************************
 *  Copyright (c) 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.internal.tools.pomversion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.eclipse.osgi.util.NLS;
import org.eclipse.releng.tools.RelEngPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IMarkerResolution;


/**
 * Marker resolution for when version in pom.xml does not match the plug-in version. 
 * Replaces the version string to one based on the version in the manifest.  The corrected
 * version must have been stored on the marker at creation time.
 */
public class PomVersionMarkerResolution implements IMarkerResolution {

	private static final String ELEMENT_VERSION = "version"; //$NON-NLS-1$
	private String correctedVersion;

	/**
	 * New marker resolution that will offer to replace the current POM version with corrected version
	 * @param correctedVersion new version to insert
	 */
	public PomVersionMarkerResolution(String correctedVersion) {
		this.correctedVersion = correctedVersion;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel() {
		return NLS.bind(Messages.PomVersionMarkerResolution_label, correctedVersion);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	public void run(IMarker marker) {
		if (correctedVersion == null || correctedVersion.trim().length() == 0) {
			return;
		}
		IResource resource = marker.getResource();
		if (resource.exists() && resource.getType() == IResource.FILE) {
			IFile file = (IFile) resource;
			if (!file.isReadOnly()) {
				InputStream fileInput = null;
				try {

					DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
					fileInput = file.getContents();
					Document doc = docBuilder.parse(fileInput);

					Node root = doc.getDocumentElement();
					NodeList list = root.getChildNodes();

					for (int i = 0; i < list.getLength(); i++) {
						Node node = list.item(i);
						if (ELEMENT_VERSION.equals(node.getNodeName())) {
							// TODO Need to check this method is as robust as setTextContent in 1.5
							node.getFirstChild().setNodeValue(correctedVersion);
//							node.setTextContent(correctedVersion);
						}
					}

					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					Transformer transformer = transformerFactory.newTransformer();
					DOMSource source = new DOMSource(doc);
					StreamResult result = new StreamResult(outputStream);
					transformer.transform(source, result);

					IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {file}, null);
					if (!status.isOK()) {
						throw new CoreException(status);
					}

					ByteArrayInputStream stream = new ByteArrayInputStream(outputStream.toByteArray());
					file.setContents(stream, true, false, null);

				} catch (ParserConfigurationException e) {
					RelEngPlugin.log(e);
				} catch (SAXException e) {
					RelEngPlugin.log(e);
				} catch (IOException e) {
					RelEngPlugin.log(e);
				} catch (TransformerException e) {
					RelEngPlugin.log(e);
				} catch (CoreException e) {
					RelEngPlugin.log(e);
				} finally {
					if (fileInput != null) {
						try {
							fileInput.close();
						} catch (IOException e) {
						}
					}
				}

			}

		}
	}
}
