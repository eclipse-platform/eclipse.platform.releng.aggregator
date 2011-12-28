/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.tools.internal.versioning;

import java.io.*;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.tools.versioning.ICompareResult;
import org.eclipse.pde.tools.versioning.IVersionCompare;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class VersionCompareDispatcher implements IVersionCompare {
	private static final String NAME_STRING = "Name"; //$NON-NLS-1$
	private static final String ERROR_STRING = "Error"; //$NON-NLS-1$
	private static final String WARNING_STRING = "Warning"; //$NON-NLS-1$
	private static final String INFO_STRING = "Information"; //$NON-NLS-1$
	private static final String SEVERITY_CODE_STRING = "SeverityCode"; //$NON-NLS-1$
	private static final String VERSION_STRING = "Version"; //$NON-NLS-1$
	private static final String XML_VERSION = "1.0"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkFeatureVersions(java.lang.String, java.lang.String, boolean, File, IProgressMonitor)
	 */
	public IStatus checkFeatureVersions(String path1, String path2, boolean needPluginCompare, File versionOptionFile, IProgressMonitor monitor) throws CoreException {
		return new FeatureVersionCompare().checkFeatureVersions(path1, path2, needPluginCompare, versionOptionFile, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkFeatureVersions(java.net.URL, java.net.URL, boolean, File, IProgressMonitor)
	 */
	public IStatus checkFeatureVersions(URL configURL1, URL configURL2, boolean needPluginCompare, File compareOptionFile, IProgressMonitor monitor) throws CoreException {
		return new FeatureVersionCompare().checkFeatureVersions(configURL1, configURL2, needPluginCompare, compareOptionFile, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkFeatureVersions(java.io.File, java.io.File, boolean, File, IProgressMonitor)
	 */
	public IStatus checkFeatureVersions(File file1, File file2, boolean needPluginCompare, File compareOptionFile, IProgressMonitor monitor) throws CoreException {
		return new FeatureVersionCompare().checkFeatureVersions(file1, file2, needPluginCompare, compareOptionFile, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkPluginVersions(String, String, IProgressMonitor)
	 */
	public ICompareResult checkPluginVersions(String plugin1, String plugin2, IProgressMonitor monitor) throws CoreException {
		MultiStatus finalResult = new MultiStatus(VersionCompareConstants.PLUGIN_ID, IStatus.OK, Messages.FeatureVersionCompare_errorReasonMsg, null);
		return new CompareResult(new PluginVersionCompare().checkPluginVersions(finalResult, plugin1, plugin2, monitor), finalResult);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkPluginVersions(URL, URL, IProgressMonitor)
	 */
	public ICompareResult checkPluginVersions(URL pluginURL1, URL pluginURL2, IProgressMonitor monitor) throws CoreException {
		MultiStatus finalResult = new MultiStatus(VersionCompareConstants.PLUGIN_ID, IStatus.OK, Messages.FeatureVersionCompare_errorReasonMsg, null);
		return new CompareResult(new PluginVersionCompare().checkPluginVersions(finalResult, pluginURL1, pluginURL2, monitor), finalResult);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkPluginVersions(File, File, IProgressMonitor)
	 */
	public ICompareResult checkPluginVersions(File pluginFile1, File pluginFile2, IProgressMonitor monitor) throws CoreException {
		MultiStatus finalResult = new MultiStatus(VersionCompareConstants.PLUGIN_ID, IStatus.OK, Messages.FeatureVersionCompare_errorReasonMsg, null);
		return new CompareResult(new PluginVersionCompare().checkPluginVersions(finalResult, pluginFile1, pluginFile2, monitor), finalResult);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkJavaClassVersions(String, String, IProgressMonitor)
	 */
	public ICompareResult checkJavaClassVersions(String javaClass1, String javaClass2, IProgressMonitor monitor) throws CoreException {
		MultiStatus finalResult = new MultiStatus(VersionCompareConstants.PLUGIN_ID, IStatus.OK, Messages.FeatureVersionCompare_errorReasonMsg, null);
		return new CompareResult(new JavaClassVersionCompare().checkJavaClassVersions(finalResult, javaClass1, javaClass2, monitor), finalResult);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkJavaClassVersions(URL, URL, IProgressMonitor)
	 */
	public ICompareResult checkJavaClassVersions(URL javaClassURL1, URL javaClassURL2, IProgressMonitor monitor) throws CoreException {
		MultiStatus finalResult = new MultiStatus(VersionCompareConstants.PLUGIN_ID, IStatus.OK, Messages.FeatureVersionCompare_errorReasonMsg, null);
		return new CompareResult(new JavaClassVersionCompare().checkJavaClassVersions(finalResult, javaClassURL1, javaClassURL2, monitor), finalResult);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkJavaClassVersions(File, File, IProgressMonitor)
	 */
	public ICompareResult checkJavaClassVersions(File javaClassFile1, File javaClassFile2, IProgressMonitor monitor) throws CoreException {
		MultiStatus finalResult = new MultiStatus(VersionCompareConstants.PLUGIN_ID, IStatus.OK, Messages.FeatureVersionCompare_errorReasonMsg, null);
		return new CompareResult(new JavaClassVersionCompare().checkJavaClassVersions(finalResult, javaClassFile1, javaClassFile1, monitor), finalResult);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkJavaClassVersions(InputStream, InputStream, IProgressMonitor)
	 */
	public ICompareResult checkJavaClassVersions(InputStream javaClassInputStream1, InputStream javaClassInputStream2, IProgressMonitor monitor) throws CoreException {
		MultiStatus finalResult = new MultiStatus(VersionCompareConstants.PLUGIN_ID, IStatus.OK, Messages.FeatureVersionCompare_errorReasonMsg, null);
		return new CompareResult(new JavaClassVersionCompare().checkJavaClassVersions(finalResult, javaClassInputStream1, javaClassInputStream2, monitor), finalResult);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkJavaClassVersions(IClassFileReader, IClassFileReader, IProgressMonitor)
	 */
	public ICompareResult checkJavaClassVersions(IClassFileReader classFileReader1, IClassFileReader classFileReader2, IProgressMonitor monitor) throws CoreException {
		MultiStatus finalResult = new MultiStatus(VersionCompareConstants.PLUGIN_ID, IStatus.OK, Messages.FeatureVersionCompare_errorReasonMsg, null);
		return new CompareResult(new JavaClassVersionCompare().checkJavaClassVersions(finalResult, classFileReader1, classFileReader2, monitor), finalResult);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#processCompareResult(IStatus status, int infoChoice)
	 */
	public IStatus processCompareResult(IStatus status, int infoChoice) {
		if (!status.isMultiStatus())
			return status;
		// create a new multi-status
		MultiStatus multiStatus = new MultiStatus(VersionCompareConstants.PLUGIN_ID, IStatus.OK, status.getMessage(), null);
		// get children status from result status
		IStatus[] childStatus = status.getChildren();
		for (int i = 0; i < childStatus.length; i++) {
			if ((childStatus[i].getCode() & infoChoice) != 0) {
				multiStatus.merge(childStatus[i]);
			}
		}
		return multiStatus;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#processExclusionListFile(File file)
	 */
	public Map processInclusionListFile(File file) throws CoreException {
		Map table = new Hashtable();
		FileInputStream fileInputStream = null;
		try {
			//create a properties instance
			Properties ppt = new Properties();
			// get InputStream of exclusion-list-file
			fileInputStream = new FileInputStream(file);
			// load property file
			ppt.load(fileInputStream);
			for (Iterator iterator = ppt.keySet().iterator(); iterator.hasNext();) {
				Object key = iterator.next();
				String property = ppt.getProperty((String) key);
				if (property == null || property.trim().equals(VersionCompareConstants.EMPTY_STRING))
					continue;
				List propertyValueList = generateList(property.split(VersionCompareConstants.COMMA_MARK));
				table.put(key, propertyValueList);
			}
		} catch (FileNotFoundException fnfe) {
			throw new CoreException(new Status(IStatus.ERROR, VersionCompareConstants.PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.VersionCompareDispatcher_fileNotFoundMsg, file.getAbsolutePath()), fnfe));
		} catch (IOException ioe) {
			throw new CoreException(new Status(IStatus.ERROR, VersionCompareConstants.PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.VersionCompareDispatcher_readPropertyFailedMsg, file.getAbsolutePath()), ioe));
		} finally {
			if (fileInputStream != null) {
				// close FileInputStream
				try {
					fileInputStream.close();
				} catch (IOException ioe) {
					throw new CoreException(new Status(IStatus.ERROR, VersionCompareConstants.PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.VersionCompareDispatcher_closeFileFailedMsg, file.getAbsolutePath()), ioe));
				}
				fileInputStream = null;
			}
		}
		return table;
	}

	/**
	 * generates a List which stores instances in array <code>objects</code>
	 * 
	 * @param objects instance objects
	 * @return List 
	 */
	private List generateList(Object[] objects) {
		ArrayList list = new ArrayList(0);
		if (objects == null || objects.length == 0)
			return list;
		for (int i = 0; i < objects.length; i++)
			list.add(objects[i]);
		return list;
	}

	/**
	 * writes out children statuses of <code>status</code> to XML file denoted by <code>fileName</code>
	 * @param status IStatus instance
	 * @param fileName String name of a XML file
	 * @throws CoreException <p>if any nested CoreException has been caught</p>
	 */
	public void writeToXML(IStatus status, String fileName) throws CoreException {
		Document doc = createXMLDoc(status);
		writeToXML(doc, fileName);
	}

	/**
	 * creates a Document instance containing elements each of which represent a child status
	 * of <code>status</code>
	 * @param status IStatus instance
	 * @throws CoreException <p>if any ParserConfigurationException, or FactoryConfigurationError has been caught</p>
	 */
	private Document createXMLDoc(IStatus status) throws CoreException {
		DocumentBuilder docBuilder = null;
		// create a DocumentBuilder instance
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			throw new CoreException(new Status(IStatus.ERROR, VersionCompareConstants.PLUGIN_ID, IStatus.ERROR, Messages.VersionCompareDispatcher_failedCreateDocMsg, pce));
		} catch (FactoryConfigurationError fce) {
			throw new CoreException(new Status(IStatus.ERROR, VersionCompareConstants.PLUGIN_ID, IStatus.ERROR, Messages.VersionCompareDispatcher_failedCreateDocMsg, fce));
		}
		// create a Document instance
		Document doc = docBuilder.newDocument();
		// create the root element
		Element rootElement = doc.createElement(VersionCompareConstants.ROOT_ELEMENT_NAME);
		rootElement.setAttribute(VERSION_STRING, XML_VERSION);
		doc.appendChild(rootElement);
		// create sub elements to contain different type of status
		Element errorElement = doc.createElement(VersionCompareConstants.SEVERITY_ELEMENT_NAME);
		errorElement.setAttribute(NAME_STRING, ERROR_STRING);
		errorElement.setAttribute(SEVERITY_CODE_STRING, String.valueOf(IStatus.ERROR)); 
		rootElement.appendChild(errorElement);
		Element warningElement = doc.createElement(VersionCompareConstants.SEVERITY_ELEMENT_NAME);
		warningElement.setAttribute(NAME_STRING, WARNING_STRING); 
		warningElement.setAttribute(SEVERITY_CODE_STRING, String.valueOf(IStatus.WARNING)); 
		rootElement.appendChild(warningElement);
		Element infoElement = doc.createElement(VersionCompareConstants.SEVERITY_ELEMENT_NAME);
		infoElement.setAttribute(NAME_STRING, INFO_STRING);
		infoElement.setAttribute(SEVERITY_CODE_STRING, String.valueOf(IStatus.INFO)); 
		rootElement.appendChild(infoElement);
		// get children statuses
		IStatus[] children = status.getChildren();
		if (children.length == 0)
			return doc;
		// create element for each children status
		for (int i = 0; i < children.length; i++) {
			switch (children[i].getSeverity()) {
				case IStatus.ERROR : {
					Element childElement = doc.createElement(VersionCompareConstants.CHILDREN_ELEMENT_NAME);
					childElement.setAttribute(VersionCompareConstants.CODE_ATTRIBUTE_NAME, String.valueOf(children[i].getCode()));
					childElement.setAttribute(VersionCompareConstants.MESSAGE_ATTRIBUTE_NAME, String.valueOf(children[i].getMessage()));
					errorElement.appendChild(childElement);
					break;
				}
				case IStatus.WARNING : {
					Element childElement = doc.createElement(VersionCompareConstants.CHILDREN_ELEMENT_NAME);
					childElement.setAttribute(VersionCompareConstants.CODE_ATTRIBUTE_NAME, String.valueOf(children[i].getCode()));
					childElement.setAttribute(VersionCompareConstants.MESSAGE_ATTRIBUTE_NAME, String.valueOf(children[i].getMessage()));
					warningElement.appendChild(childElement);
					break;
				}
				case IStatus.INFO : {
					Element childElement = doc.createElement(VersionCompareConstants.CHILDREN_ELEMENT_NAME);
					childElement.setAttribute(VersionCompareConstants.CODE_ATTRIBUTE_NAME, String.valueOf(children[i].getCode()));
					childElement.setAttribute(VersionCompareConstants.MESSAGE_ATTRIBUTE_NAME, String.valueOf(children[i].getMessage()));
					infoElement.appendChild(childElement);
					break;
				}
			}
		}
		return doc;
	}

	/**
	 * writes out <code>doc</code> to the xml file denoted by <code>fileName</code>
	 * @param doc Document instance
	 * @param fileName String which denotes a xml file
	 * @throws CoreException <p>if any TransformerConfigurationException, or TransformerException has been caught</p>
	 */
	private void writeToXML(Document doc, String fileName) throws CoreException {
		if (!isXMLfile(fileName))
			throw new CoreException(new Status(IStatus.ERROR, VersionCompareConstants.PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.VersionCompareDispatcher_invalidXMLFileNameMsg, fileName), null));
		// create a DOMSource instance
		DOMSource doms = new DOMSource(doc);
		// create a File instance
		File file = new File(fileName);
		// create a StreamResult instance of file
		StreamResult streamResult = new StreamResult(file);
		try {
			// set output properties
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			Properties properties = transformer.getOutputProperties();
			properties.setProperty(OutputKeys.ENCODING, VersionCompareConstants.ENCODING_TYPE);
			transformer.setOutputProperties(properties);
			// write out doc
			transformer.transform(doms, streamResult);
		} catch (TransformerConfigurationException tce) {
			throw new CoreException(new Status(IStatus.ERROR, VersionCompareConstants.PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.VersionCompareDispatcher_failedWriteXMLFileMsg, fileName), tce));
		} catch (TransformerException te) {
			throw new CoreException(new Status(IStatus.ERROR, VersionCompareConstants.PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.VersionCompareDispatcher_failedWriteXMLFileMsg, fileName), te));
		}
	}

	/**
	 * checks whether <code>file</code> represents a XML file
	 * @param fileName String name of a file
	 * @return <code>true</code> if <code>file</code> represents a XML file,
	 *         <code>false</coc
	 */
	private boolean isXMLfile(String fileName) {
		IPath path = new Path(fileName);
		if (path.isValidPath(fileName)) {
			String extension = path.getFileExtension();
			if (extension == null)
				return false;
			if (!extension.equals(VersionCompareConstants.XML_FILE_EXTENSION))
				return false;
			return true;
		}
		return false;
	}
}
