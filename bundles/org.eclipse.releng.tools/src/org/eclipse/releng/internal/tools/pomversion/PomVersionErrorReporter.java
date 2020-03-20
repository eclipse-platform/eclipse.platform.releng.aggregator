/*******************************************************************************
 *  Copyright (c) 2013, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 489985
 *******************************************************************************/
package org.eclipse.releng.internal.tools.pomversion;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.jar.JarFile;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.releng.tools.RelEngPlugin;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Validates the content of the pom.xml.  Currently the only check is that the
 * version specified in pom.xml matches the bundle version.
 *
 */
public class PomVersionErrorReporter implements IResourceChangeListener, IEclipsePreferences.IPreferenceChangeListener {

	class PomResourceDeltaVisitor implements IResourceDeltaVisitor {

		@Override
		public boolean visit(IResourceDelta delta) {
			if (delta != null) {
				IResource resource = delta.getResource();
				switch(resource.getType()) {
				case IResource.PROJECT: {
					if(delta.getKind() == IResourceDelta.REMOVED) {
						return false;
					}
					IProject project = (IProject) resource;
					try {
						if(project.isAccessible() && (project.getDescription().hasNature("org.eclipse.pde.PluginNature") || project.getDescription().hasNature("org.eclipse.pde.FeatureNature"))) { //$NON-NLS-1$ //$NON-NLS-2$
							if((delta.getFlags() & IResourceDelta.OPEN) > 0) {
								validate(project);
								return false;
							}
							return true;
						}
					}
					catch(CoreException ce) {
						RelEngPlugin.log(ce);
					}
					return false;
				}
				case IResource.ROOT:
				case IResource.FOLDER: {
					return true;
				}
				case IResource.FILE: {
					switch(delta.getKind()) {
					case IResourceDelta.REMOVED: {
						//if manifest or feature removed, clean up markers
						if(resource.getProjectRelativePath().equals(FEATURE_PATH) ||
								resource.getProjectRelativePath().equals(MANIFEST_PATH)) {
							IProject p = resource.getProject();
							if(p.isAccessible()) {
								cleanMarkers(p);
							}
						}
						break;
					}
					case IResourceDelta.ADDED: {
						//if the POM, manifest or feature.xml has been added scan them
						if(resource.getProjectRelativePath().equals(FEATURE_PATH) ||
								resource.getProjectRelativePath().equals(MANIFEST_PATH) ||
								resource.getProjectRelativePath().equals(POM_PATH)) {
							validate(resource.getProject());
						}
						break;
					}
					case IResourceDelta.CHANGED: {
						//if the content has changed clean + scan
						if((delta.getFlags() & IResourceDelta.CONTENT) > 0) {
							if(resource.getProjectRelativePath().equals(FEATURE_PATH) ||
									resource.getProjectRelativePath().equals(MANIFEST_PATH) ||
									resource.getProjectRelativePath().equals(POM_PATH)) {
								validate(resource.getProject());
							}
						}
						break;
					}
					default: {
						break;
					}
					}
					return false;
				}
				}
			}
			return false;
		}
	}

	/**
	 * XML parsing handler to check the POM version infos
	 */
	class PomVersionHandler extends DefaultHandler {
		private Version version;
		private Stack<String> elements = new Stack<>();
		private boolean checkVersion = false;
		private boolean isFeatureProject = false;
		private Locator locator;
		IFile pom = null;
		String severity = null;

		public PomVersionHandler(IFile file, Version bundleVersion, String pref) {
			this(file, bundleVersion, pref, false);
		}

		public PomVersionHandler(IFile file, Version version, String pref, boolean isFeatureProject) {
			pom = file;
			severity = pref;
			this.version = version;
			this.isFeatureProject = isFeatureProject;
		}

		@Override
		public void setDocumentLocator(Locator locator) {
			this.locator = locator;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (ELEMENT_VERSION.equals(qName)) {
				if (!elements.isEmpty() && ELEMENT_PROJECT.equals(elements.peek())) {
					checkVersion = true;
				}
			}
			elements.push(qName);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			elements.pop();
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (checkVersion) {
				checkVersion = false;

				try {
					// Remove the snapshot suffix
					String versionString = new String(ch, start, length);
					String origVer = versionString;
					int index = versionString.indexOf(SNAPSHOT_SUFFIX);
					if (index >= 0) {
						versionString = versionString.substring(0, index);
					}

					// Create corrected version (no qualifiers, add back snapshot suffix)
					Version bundleVersion2 = new Version(version.getMajor(), version.getMinor(), version.getMicro());
					String correctedVersion = bundleVersion2.toString();
					if (index >= 0) {
						correctedVersion = correctedVersion.concat(SNAPSHOT_SUFFIX);
					}

					// Check if the pom version is a valid OSGi version
					Version pomVersion = null;
					try {
						pomVersion = Version.parseVersion(versionString);
					} catch (IllegalArgumentException e){
						// Need to create a document to calculate the markers charstart and charend
						IDocument doc = createDocument(pom);
						int lineOffset = doc.getLineOffset(locator.getLineNumber() - 1); // locator lines start at 1
						int linLength = doc.getLineLength(locator.getLineNumber() - 1);
						String str = doc.get(lineOffset, linLength);
						index = str.indexOf(origVer);
						int charStart = lineOffset + index;
						int charEnd = charStart + origVer.length();

						String message = isFeatureProject ? Messages.PomVersionErrorReporter_pom_version_error_marker_message_feature : Messages.PomVersionErrorReporter_pom_version_error_marker_message;
						reportMarker(NLS.bind(message, versionString, bundleVersion2.toString()),
								locator.getLineNumber(),
								charStart,
								charEnd,
								correctedVersion,
								pom,
								severity);
					}

					if (pomVersion == null){
						return;
					}

					// Compare the versions
					Version pomVersion2 = new Version(pomVersion.getMajor(), pomVersion.getMinor(), pomVersion.getMicro());
					if (!bundleVersion2.equals(pomVersion2)) {
						// Need to create a document to calculate the markers charstart and charend
						IDocument doc = createDocument(pom);
						int lineOffset = doc.getLineOffset(locator.getLineNumber() - 1); // locator lines start at 1
						int linLength = doc.getLineLength(locator.getLineNumber() - 1);
						String str = doc.get(lineOffset, linLength);
						index = str.indexOf(origVer);
						int charStart = lineOffset + index;
						int charEnd = charStart + origVer.length();
						String message = isFeatureProject ? Messages.PomVersionErrorReporter_pom_version_error_marker_message_feature : Messages.PomVersionErrorReporter_pom_version_error_marker_message;
						reportMarker(NLS.bind(message, pomVersion2.toString(), bundleVersion2.toString()),
								locator.getLineNumber(),
								charStart,
								charEnd,
								correctedVersion,
								pom,
								severity);
					}
				} catch (IllegalArgumentException e) {
					// If the manifest version is broken, let PDE report the problem
				} catch (BadLocationException e) {
					RelEngPlugin.log(e);
				}
			}
		}
	}

	/**
	 * XML parsing handler to check the feature.xml version
	 */
	class FeatureVersionHandler extends DefaultHandler {
		private String featureVersion;

		public FeatureVersionHandler() {
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			// The version is on the root element, check for the attribute then throw exception to exit early
			featureVersion = attributes.getValue("version"); //$NON-NLS-1$
			throw new OperationCanceledException();
		}

		/**
		 * Returns the string version value found in the feature.xml or <code>null</code>
		 *
		 * @return string version from feature.xml or <code>null</code>
		 */
		public String getVersion() {
			return featureVersion;
		}

	}

	/**
	 * Project relative path to the pom.xml file.
	 */
	public static final IPath POM_PATH = new Path("pom.xml"); //$NON-NLS-1$

	/**
	 * Project relative path to the manifest file.
	 */
	public static final IPath MANIFEST_PATH = new Path(JarFile.MANIFEST_NAME);

	/**
	 * Version of the PomVersionErrorReporter. Needs to be incremented when the version check algorithm changes
	 * in a way that requires re-validation of the whole workspace.
	 */
	public static final int VERSION= 1;

	/**
	 * Project relative path to the feature.xml file.
	 */
	public static final IPath FEATURE_PATH = new Path("feature.xml"); //$NON-NLS-1$

	private static final String ELEMENT_PROJECT = "project"; //$NON-NLS-1$
	private static final String ELEMENT_VERSION = "version"; //$NON-NLS-1$
	private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT"; //$NON-NLS-1$


	/**
	 * Clean up all markers
	 *
	 * @param project
	 */
	void cleanMarkers(IResource resource) {
		try {
			resource.deleteMarkers(IPomVersionConstants.PROBLEM_MARKER_TYPE, false, IResource.DEPTH_INFINITE);
		}
		catch(CoreException e) {
			RelEngPlugin.log(e);
		}
	}

	/**
	 * Validates the manifest or feature version against the version in the <code>pom.xml</code> file
	 *
	 * @param project
	 * @param severity
	 */
	public void validate(IProject project) {
		if(project == null || !project.isAccessible()) {
			return;
		}
		// Clean up existing markers
		cleanMarkers(project);

		String severity = RelEngPlugin.getPlugin().getPreferenceStore().getString(IPomVersionConstants.POM_VERSION_ERROR_LEVEL);
		if (IPomVersionConstants.VALUE_IGNORE.equals(severity)) {
			return;
		}
		IFile pom = project.getFile(POM_PATH);
		if(!pom.exists()) {
			return;
		}

		IFile manifest = project.getFile(MANIFEST_PATH);
		if(manifest.exists()) {
			// Get the manifest version
			Version bundleVersion = Version.emptyVersion;
			try {
				Map<String, String> headers = new HashMap<>();
				ManifestElement.parseBundleManifest(manifest.getContents(), headers);
				String ver = headers.get(Constants.BUNDLE_VERSION);
				if(ver == null) {
					return;
				}
				bundleVersion = Version.parseVersion(ver);
			} catch (IOException e) {
				// Ignored, if there is a problem with the manifest, don't create a marker
				return;
			} catch (CoreException e){
				// Ignored, if there is a problem with the manifest, don't create a marker
				return;
			} catch (BundleException e) {
				// Ignored, if there is a problem with the manifest, don't create a marker
				return;
			} catch (IllegalArgumentException e){
				// Ignored, if there is a problem with the manifest, don't create a marker
				return;
			}
			// Compare it to the POM file version
			try {
				SAXParserFactory parserFactory = SAXParserFactory.newInstance();
				SAXParser parser = parserFactory.newSAXParser();
				PomVersionHandler handler = new PomVersionHandler(pom, bundleVersion, severity);
				parser.parse(pom.getContents(), handler);
			} catch (Exception e) {
				// Ignored, if there is a problem with the POM file don't create a marker
				return;
			}

		} else {
			IFile feature = project.getFile(FEATURE_PATH);
			if (feature.exists()){
				try {
					// Get the feature version
					Version featureVersion = Version.emptyVersion;
					SAXParserFactory parserFactory = SAXParserFactory.newInstance();
					SAXParser parser = parserFactory.newSAXParser();
					FeatureVersionHandler handler = new FeatureVersionHandler();
					try {
						parser.parse(feature.getContents(), handler);
					} catch (OperationCanceledException e){
						// Do nothing, used to avoid parsing the entire file
					}

					String version = handler.getVersion();
					if (version == null){
						// Ignored, if there is a problem with the feature, don't create a marker
						return;
					}
					featureVersion = Version.parseVersion(version);

					// Compare it to the POM file version
					PomVersionHandler pomHandler = new PomVersionHandler(pom, featureVersion, severity, true);
					parser.parse(pom.getContents(), pomHandler);
				} catch (Exception e) {
					// Ignored, if there is a problem with the POM file don't create a marker
					return;
				}
			}
		}
	}

	/**
	 * Creates a new POM version problem marker with the given attributes
	 * @param message the message for the marker
	 * @param lineNumber the line number of the problem
	 * @param charStart the starting character offset
	 * @param charEnd the ending character offset
	 * @param correctedVersion the correct version to be inserted
	 * @param pom the handle to the POM file
	 * @param severity the severity of the marker to create
	 */
	void reportMarker(String message, int lineNumber, int charStart, int charEnd, String correctedVersion, IFile pom, String severity) {
		try {
			HashMap<String, Object> attributes = new HashMap<>();
			attributes.put(IMarker.MESSAGE, message);
			if (severity.equals(IPomVersionConstants.VALUE_WARNING)){
				attributes.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_WARNING));
			} else {
				attributes.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_ERROR));
			}
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			attributes.put(IMarker.LINE_NUMBER, Integer.valueOf(lineNumber));
			attributes.put(IMarker.CHAR_START, Integer.valueOf(charStart));
			attributes.put(IMarker.CHAR_END, Integer.valueOf(charEnd));
			attributes.put(IPomVersionConstants.POM_CORRECT_VERSION, correctedVersion);
			MarkerUtilities.createMarker(pom, attributes, IPomVersionConstants.PROBLEM_MARKER_TYPE);
		} catch (CoreException e){
			RelEngPlugin.log(e);
		}
	}

	/**
	 * Creates a new {@link IDocument} for the given {@link IFile}. <code>null</code>
	 * is returned if the {@link IFile} does not exist or the {@link ITextFileBufferManager}
	 * cannot be acquired or there was an exception trying to create the {@link IDocument}.
	 *
	 * @param file
	 * @return a new {@link IDocument} or <code>null</code>
	 */
	protected IDocument createDocument(IFile file) {
		if (!file.exists()) {
			return null;
		}
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		if (manager == null) {
			return null;
		}
		try {
			manager.connect(file.getFullPath(), LocationKind.NORMALIZE, null);
			ITextFileBuffer textBuf = manager.getTextFileBuffer(file.getFullPath(), LocationKind.NORMALIZE);
			IDocument document = textBuf.getDocument();
			manager.disconnect(file.getFullPath(), LocationKind.NORMALIZE, null);
			return document;
		} catch (CoreException e) {
			RelEngPlugin.log(e);
		}
		return null;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if(delta != null) {
			final PomResourceDeltaVisitor visitor = new PomResourceDeltaVisitor();
			try {
				delta.accept(visitor);
			} catch (CoreException e) {
				RelEngPlugin.log(e);
			}
		}
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if(IPomVersionConstants.POM_VERSION_ERROR_LEVEL.equals(event.getKey())) {
			final String newSeverity = (String) event.getNewValue();
			final Object oldSeverity= event.getOldValue();
			if(newSeverity != null) {
				if(IPomVersionConstants.VALUE_IGNORE.equals(newSeverity)) {
					//we turned it off
					ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
				} else if(oldSeverity == null || IPomVersionConstants.VALUE_IGNORE.equals(oldSeverity)) {
					// we turned it on
					ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_BUILD);
				}
				validateWorkspace();
			}
		}
	}

	public void validateWorkspace() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		for (int i = 0; i < projects.length; i++) {
			validate(projects[i]);
		}
		RelEngPlugin.getPlugin().getPreferenceStore().setValue(IPomVersionConstants.WORKSPACE_VALIDATED, VERSION);
	}
}
