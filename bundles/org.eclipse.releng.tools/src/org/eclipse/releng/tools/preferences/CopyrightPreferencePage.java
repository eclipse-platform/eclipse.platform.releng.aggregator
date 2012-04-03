/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools.preferences;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.releng.tools.Messages;
import org.eclipse.releng.tools.RelEngPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Copyright tools preference page
 */
public class CopyrightPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private final String NEW_LINE = "\n"; //$NON-NLS-1$
	private Composite fComposite;
	private Label fCopyrightLabel;
	private SourceViewer fEditor;
	private Text fInstructions;
	private Label fCreationYearLabel;
	private Text fCreationYear;
	private Label fRevisionYearLabel;
	private Text fRevisionYear;
	private Button fUseDefaultRevisionYear;
	private Button fReplaceAllExisting;
	// disable fix up existing copyright till it works better
//	private Button fFixExisting;
	private Button fIgnoreProperties;
	private Button fIgnoreXml;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub

	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {

		//The main composite
		fComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		fComposite.setLayout(layout);
		fComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		// copyright template editor
		fEditor = createEditor(fComposite);
		
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		data.horizontalIndent = 0;
		fInstructions = new Text(fComposite, SWT.READ_ONLY);
		fInstructions.setText(Messages.getString("CopyrightPreferencePage.0")); //$NON-NLS-1$
		fInstructions.setLayoutData(data);
		
		// default creation year
		fCreationYearLabel = new Label(fComposite, SWT.NONE);
		fCreationYearLabel.setText(Messages.getString("CopyrightPreferencePage.1")); //$NON-NLS-1$
		fCreationYear = new Text(fComposite, SWT.BORDER);
		fCreationYear.setTextLimit(4);
		
		// default revision year
		fRevisionYearLabel = new Label(fComposite, SWT.NONE);
		fRevisionYearLabel.setText(Messages.getString("CopyrightPreferencePage.7")); //$NON-NLS-1$
		fRevisionYear = new Text(fComposite, SWT.BORDER);
		fRevisionYear.setTextLimit(4);
		
		// always use default revision year instead of cvs lookup
		fUseDefaultRevisionYear = new Button(fComposite, SWT.CHECK);
		fUseDefaultRevisionYear.setText(Messages.getString("CopyrightPreferencePage.8")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		fUseDefaultRevisionYear.setLayoutData(data);
		
		// replace all existing copyright statement
		fReplaceAllExisting = new Button(fComposite, SWT.CHECK);
		fReplaceAllExisting.setText(Messages.getString("CopyrightPreferencePage.2")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		fReplaceAllExisting.setLayoutData(data);
		
		// disable fix up existing copyright till it works better
//		// fix up existing copyright statement
//		fFixExisting = new Button(fComposite, SWT.CHECK);
//		fFixExisting.setText(Messages.getString("CopyrightPreferencePage.3")); //$NON-NLS-1$
//		data = new GridData();
//		data.horizontalSpan = 2;
//		fFixExisting.setLayoutData(data);
		
		// ignore properties files
		fIgnoreProperties = new Button(fComposite, SWT.CHECK);
		fIgnoreProperties.setText(Messages.getString("CopyrightPreferencePage.4")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		fIgnoreProperties.setLayoutData(data);
		
		// ignore xml files
		fIgnoreXml = new Button(fComposite, SWT.CHECK);
		fIgnoreXml.setText(Messages.getString("CopyrightPreferencePage.9")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		fIgnoreXml.setLayoutData(data);

		KeyListener listener1 = new KeyAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.KeyAdapter#keyReleased(org.eclipse.swt.events.KeyEvent)
			 */
			public void keyReleased(KeyEvent e) {
				validateValues();
			}
		};
		fCreationYear.addKeyListener(listener1);
		fRevisionYear.addKeyListener(listener1);
		
		// disable fix up existing copyright till it works better
//		SelectionListener listener2 = new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				handleReplaceAllEnabled(fReplaceAllExisting.getSelection(), fFixExisting.getSelection());
//			}
//		};
//		fReplaceAllExisting.addSelectionListener(listener2);
		
		initializeValues();
		applyDialogFont(fComposite);
		return fComposite;
	}

	/**
	 * Create the sourceviewer editor to be used to edit the copyright template
	 */
	private SourceViewer createEditor(Composite parent) {
		fCopyrightLabel = new Label(parent, SWT.NONE);
		fCopyrightLabel.setText(Messages.getString("CopyrightPreferencePage.5")); //$NON-NLS-1$
		GridData data= new GridData();
		data.horizontalSpan= 2;
		fCopyrightLabel.setLayoutData(data);
		
		SourceViewer viewer= createViewer(parent);

		IDocument document= new Document();
		viewer.setEditable(true);
		viewer.setDocument(document);
		
		// just use a default 10 lines
		int nLines = 10;
//		int nLines= document.getNumberOfLines();
//		if (nLines < 5) {
//			nLines= 5;
//		} else if (nLines > 12) {
//			nLines= 12;	
//		}
				
		Control control= viewer.getControl();
		data= new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint= convertWidthInCharsToPixels(80);
		data.heightHint= convertHeightInCharsToPixels(nLines);
		data.horizontalSpan = 2;
		control.setLayoutData(data);
		
		// TODO add content assist support
//		viewer.addTextListener(new ITextListener() {
//			public void textChanged(TextEvent event) {
//				if (event.getDocumentEvent() != null)
//					doSourceChanged(event.getDocumentEvent().getDocument());
//			}
//		});
//
//		viewer.addSelectionChangedListener(new ISelectionChangedListener() {			
//			public void selectionChanged(SelectionChangedEvent event) {
//				updateSelectionDependentActions();
//			}
//		});
//
//	 	viewer.prependVerifyKeyListener(new VerifyKeyListener() {
//			public void verifyKey(VerifyEvent event) {
//				handleVerifyKeyPressed(event);
//			}
//		});
		
		return viewer;
	}
	
	/**
	 * Creates the viewer to be used to display the copyright.
	 * 
	 * @param parent the parent composite of the viewer
	 * @return a configured <code>SourceViewer</code>
	 */
	private SourceViewer createViewer(Composite parent) {
		SourceViewer viewer= new SourceViewer(parent, null, null, false, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		SourceViewerConfiguration configuration= new SourceViewerConfiguration() {
			// TODO add content assist support
//			public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
//				
//				ContentAssistant assistant= new ContentAssistant();
//				assistant.enableAutoActivation(true);
//				assistant.enableAutoInsert(true);
//				assistant.setContentAssistProcessor(fTemplateProcessor, IDocument.DEFAULT_CONTENT_TYPE);
//				return assistant;
//			}
		};
		viewer.configure(configuration);
		return viewer;
	}
	
	/**
	 * Initialize the control values in this preference page
	 */
	private void initializeValues() {
		IPreferenceStore store = getPreferenceStore();

		fEditor.getDocument().set(store.getString(RelEngCopyrightConstants.COPYRIGHT_TEMPLATE_KEY));
		fCreationYear.setText(store.getString(RelEngCopyrightConstants.CREATION_YEAR_KEY));
		fRevisionYear.setText(store.getString(RelEngCopyrightConstants.REVISION_YEAR_KEY));
		fUseDefaultRevisionYear.setSelection(store.getBoolean(RelEngCopyrightConstants.USE_DEFAULT_REVISION_YEAR_KEY));
		fReplaceAllExisting.setSelection(store.getBoolean(RelEngCopyrightConstants.REPLACE_ALL_EXISTING_KEY));
		// disable fix up existing copyright till it works better
//		handleReplaceAllEnabled(fReplaceAllExisting.getSelection(), store.getBoolean(RelEngCopyrightConstants.FIX_UP_EXISTING_KEY));
		fIgnoreProperties.setSelection(store.getBoolean(RelEngCopyrightConstants.IGNORE_PROPERTIES_KEY));
		fIgnoreXml.setSelection(store.getBoolean(RelEngCopyrightConstants.IGNORE_XML_KEY));
	}
	
	/**
	 * Validate the control values in this preference page
	 */
	private void validateValues() {
		String ERROR_MESSAGE = Messages.getString("CopyrightPreferencePage.6"); //$NON-NLS-1$
		
		String errorMsg = null;
		
		// creation & revision year must be an integer
		String creationYear = fCreationYear.getText();
		String revisionYear = fRevisionYear.getText();
		try {
			int year = Integer.parseInt(creationYear);
			if (year < 0) {
				errorMsg = ERROR_MESSAGE;
			}
			year = Integer.parseInt(revisionYear);
			if (year < 0) {
				errorMsg = ERROR_MESSAGE;
			}
		} catch (NumberFormatException e) {
			errorMsg = ERROR_MESSAGE;
		}
		setErrorMessage(errorMsg);
		setValid(errorMsg == null);
	}
	
	// disable fix up existing copyright till it works better
//	/**
//	 * Handles when the Replace all copyrights checkbox is checked/unchecked.
//	 * When checked, fix up copyright checkbox is disabled and checked
//	 * When unchecked, fix up copyright checkbox is enabled and set to default value 
//	 * @param replaceAll
//	 * @param defaultValue
//	 */
//	private void handleReplaceAllEnabled(boolean replaceAll, boolean defaultValue) {
//		if (fReplaceAllExisting.isEnabled() && !replaceAll)
//			fFixExisting.setEnabled(true);
//		else
//			fFixExisting.setEnabled(false);
//		
//		if (replaceAll) {
//			fFixExisting.setSelection(replaceAll);
//		} else {
//			fFixExisting.setSelection(defaultValue);
//		}
//	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
	 */
	protected IPreferenceStore doGetPreferenceStore() {
		return RelEngPlugin.getDefault().getPreferenceStore();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore store = getPreferenceStore();
		
		fEditor.getDocument().set(store.getDefaultString(RelEngCopyrightConstants.COPYRIGHT_TEMPLATE_KEY));
		fCreationYear.setText(store.getDefaultString(RelEngCopyrightConstants.CREATION_YEAR_KEY));
		fRevisionYear.setText(store.getDefaultString(RelEngCopyrightConstants.REVISION_YEAR_KEY));
		fUseDefaultRevisionYear.setSelection(getPreferenceStore().getDefaultBoolean(RelEngCopyrightConstants.USE_DEFAULT_REVISION_YEAR_KEY));
		fReplaceAllExisting.setSelection(getPreferenceStore().getDefaultBoolean(RelEngCopyrightConstants.REPLACE_ALL_EXISTING_KEY));
		// disable fix up existing copyright till it works better
//		handleReplaceAllEnabled(fReplaceAllExisting.getSelection(), getPreferenceStore().getDefaultBoolean(RelEngCopyrightConstants.FIX_UP_EXISTING_KEY));
		fIgnoreProperties.setSelection(getPreferenceStore().getDefaultBoolean(RelEngCopyrightConstants.IGNORE_PROPERTIES_KEY));
		fIgnoreXml.setSelection(getPreferenceStore().getDefaultBoolean(RelEngCopyrightConstants.IGNORE_XML_KEY));
		
		super.performDefaults();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();

		store.setValue(RelEngCopyrightConstants.COPYRIGHT_TEMPLATE_KEY, fixupLineDelimiters(fEditor.getDocument()));
		store.setValue(RelEngCopyrightConstants.CREATION_YEAR_KEY, fCreationYear.getText());
		store.setValue(RelEngCopyrightConstants.REVISION_YEAR_KEY, fRevisionYear.getText());
		store.setValue(RelEngCopyrightConstants.USE_DEFAULT_REVISION_YEAR_KEY, fUseDefaultRevisionYear.getSelection());
		store.setValue(RelEngCopyrightConstants.REPLACE_ALL_EXISTING_KEY, fReplaceAllExisting.getSelection());
		// disable fix up existing copyright till it works better
//		store.setValue(RelEngCopyrightConstants.FIX_UP_EXISTING_KEY, fFixExisting.getSelection());
		store.setValue(RelEngCopyrightConstants.IGNORE_PROPERTIES_KEY, fIgnoreProperties.getSelection());
		store.setValue(RelEngCopyrightConstants.IGNORE_XML_KEY, fIgnoreXml.getSelection());
		
		try {
			InstanceScope.INSTANCE.getNode(RelEngPlugin.ID).flush();
		} catch (BackingStoreException e) {
			RelEngPlugin.log(IStatus.ERROR, "could not save preferences", e); //$NON-NLS-1$
		}
		
		return super.performOk();
	}
	
	/**
	 * Fix up line delimiters in doc to use only \n
	 * @param doc
	 * @return
	 */
	private String fixupLineDelimiters(IDocument doc) {
		String docContents = doc.get();
		String newText = ""; //$NON-NLS-1$
		int lineCount = doc.getNumberOfLines();
		for (int i = 0; i < lineCount; i++) {
			try {
				IRegion lineInfo = doc.getLineInformation(i);
				int lineStartOffset = lineInfo.getOffset();
				int lineLength = lineInfo.getLength();
				int lineEndOffset = lineStartOffset + lineLength;
				newText += docContents.substring(lineStartOffset, lineEndOffset);

				if ((i < lineCount - 1) && (fEditor.getDocument().getLineDelimiter(i) != null))
					newText += NEW_LINE;
			}
			catch (BadLocationException exception) {
				// exception
			}
		}
		return newText;
	}
}
