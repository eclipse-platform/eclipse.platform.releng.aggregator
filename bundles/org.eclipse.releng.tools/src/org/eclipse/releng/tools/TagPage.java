/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.eclipse.team.internal.ccvs.core.CVSTag;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;


/**
 *This class extends <code>WizardPage<code>. It allows user to enter a tag name and make some additional 
 *options.
 */
public class TagPage extends WizardPage{
	
	private String tagString;
	private Combo tagCombo;
	private static final int COMBO_HISTORY_LENGTH = 5;
	private final String DEFAULT_TAG_PREFIX = "v";
	
	private Button moveButton;
	private Button validateButton;
	private Button compareButton;
	private Button commitButton;

	private boolean moveButtonSelected;
	private boolean compareButtonSelected;
	private boolean commitButtonSelected;
	private boolean validateButtonSelected;
	private boolean hasError;//for tag validation
	
	private IDialogSettings settings;
	private String TAG_KEY = Messages.getString("TagPage.1"); //$NON-NLS-1$
	private String COMPARE_BUTTON_KEY = Messages.getString("TagPage.2"); //$NON-NLS-1$
	private String COMMIT_BUTTON_KEY = Messages.getString("TagPage.3"); //$NON-NLS-1$
	private String MOVE_BUTTON_KEY = Messages.getString("TagPage.4"); //$NON-NLS-1$
	private String VALIDATE_BUTTON_KEY = Messages.getString("TagPage.5"); //$NON-NLS-1$

	/**
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	public TagPage(String pageName, String title, IDialogSettings settings, ImageDescriptor image) {
		super(pageName, title, image);
		this.settings = settings;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		composite.setFont(font);

		Label label = new Label(composite, SWT.HORIZONTAL);	 	 
		label.setText(Messages.getString("TagPage.6")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		label.setFont(font);
		
		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				tagString = null;
				modifyTag();
			}
		};
		
		tagCombo = new Combo(composite,SWT.NONE);
		tagCombo.addListener(SWT.Selection, listener);
		tagCombo.addListener(SWT.Modify, listener);
		tagCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		tagCombo.setFont(font);
				
		moveButton = new Button(composite, SWT.CHECK);
		moveButton.setVisible(true);
		moveButton.setText(Messages.getString("TagPage.7")); //$NON-NLS-1$
		moveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				moveButtonSelected = moveButton.getSelection();
			}
		});
		moveButton.setFont(font);
			
		validateButton = new Button(composite,SWT.CHECK);
		validateButton.setText(Messages.getString("TagPage.8")); //$NON-NLS-1$
		validateButton.addSelectionListener( new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				validateButtonSelected = validateButton.getSelection();
			}		
		});
		validateButton.setFont(font);
		
		Group group = new Group(composite, SWT.LEFT);
		group.setLayout(new GridLayout());
		GridData layoutData= new GridData(GridData.FILL, GridData.CENTER, true, false);
		group.setLayoutData(layoutData);
		group.setFont(font);
		group.setText(Messages.getString("TagPage.9")); //$NON-NLS-1$
		
		compareButton = new Button(group,SWT.RADIO);
		compareButton.setText(Messages.getString("TagPage.10")); //$NON-NLS-1$
		compareButton.addSelectionListener( new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				compareButtonSelected = compareButton.getSelection();
				updateFinishStatus();
			}		
		});
		compareButton.setFont(font);
		
		commitButton = new Button(group,SWT.RADIO);
		commitButton.setText(Messages.getString("TagPage.11")); //$NON-NLS-1$
		commitButton.setSelection(true);
		commitButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				commitButtonSelected = commitButton.getSelection();	
			}
		});	
		commitButton.setFont(font);
		
		initializePage();
		setControl(composite);
		
	}
	
	public String getTagString(){
		return tagString;
	}
	/**
	 * Validates tag name
	 */
	private void validateTag(String tag){
		String message = null;
		hasError = false;
		if(tag.length() == 0) {
			hasError = true;
		} else {		
			IStatus status = CVSTag.validateTagName(tag);
			if (!status.isOK()) {
				message = status.getMessage();
				hasError = true;
			}
		}
		setErrorMessage(message);
	}

	public boolean isMoveButtonSelected(){
		return moveButtonSelected;
	}
	public boolean compareButtonSelected(){
		return compareButtonSelected;
	}
	public boolean commitButtonSelected(){
		return commitButtonSelected ;
	}
	private boolean isPageCompleted(){
		return (!hasError);
	}

	public void saveSettings(){
		String[] tags = settings.getArray(TAG_KEY);
		if (tags == null) tags = new String[0];
		tags = addToTagList(tags, tagCombo.getText());
		settings.put(TAG_KEY,tags);
		settings.put(COMPARE_BUTTON_KEY,compareButtonSelected);
		settings.put(COMMIT_BUTTON_KEY ,commitButtonSelected);
		settings.put(MOVE_BUTTON_KEY ,moveButtonSelected);
		settings.put(VALIDATE_BUTTON_KEY,validateButtonSelected);
	}
	
	private void readSettings(){
		if(settings.get(COMPARE_BUTTON_KEY) != null){
			compareButton.setSelection(settings.getBoolean(COMPARE_BUTTON_KEY));
			compareButtonSelected = settings.getBoolean(COMPARE_BUTTON_KEY);
		}else{
			compareButton.setSelection(true);
			compareButtonSelected = true;
		}
		if(settings.get(COMMIT_BUTTON_KEY) != null){			
			commitButton.setSelection(settings.getBoolean(COMMIT_BUTTON_KEY));
			commitButtonSelected = settings.getBoolean(COMMIT_BUTTON_KEY);
		}else{
			commitButton.setSelection( false);
			commitButtonSelected = false;
		}
		if(settings.get(MOVE_BUTTON_KEY) != null){
			moveButton.setSelection(settings.getBoolean(MOVE_BUTTON_KEY));
			moveButtonSelected = settings.getBoolean(MOVE_BUTTON_KEY);
		}else{
			moveButton.setSelection(false);
			moveButtonSelected = false;
		}
		if(settings.get(VALIDATE_BUTTON_KEY) != null){
			validateButton.setSelection( settings.getBoolean(VALIDATE_BUTTON_KEY ));
			validateButtonSelected = settings.getBoolean(VALIDATE_BUTTON_KEY);
		}else{
			validateButton.setSelection(true);
			validateButtonSelected = true;
		}
		//insert the tag template to the head of the list and avoid duplicated items.
		if(settings.getArray(TAG_KEY) == null){
			tagCombo.add(getTagTemplate());
		}else{			
			String[] savedTags = settings.getArray(TAG_KEY);
			if (savedTags != null && savedTags.length > 0) {
				String[] newTags = addToTagList( savedTags,getTagTemplate());
				for(int i = 0; i< newTags.length; i++){
					tagCombo.add(newTags[i]);
				}
			}
			else{
				tagCombo.add(getTagTemplate());
			}
		}
	}
	
	private void initializePage(){
		if(settings != null){
			readSettings();
		}
		else{
			commitButton.setSelection(false);
			compareButton.setSelection(true);
			validateButton.setSelection(true);
			moveButton.setSelection(false);
			commitButtonSelected = false;
			compareButtonSelected = true;
			moveButtonSelected = false;
			validateButtonSelected = true;
			tagCombo.add(getTagTemplate());
		}
		hasError = false;
		setPageComplete(false);
	}


	private void modifyTag() {
		tagString = tagCombo.getText();
		validateTag(tagString);
		setPageComplete(isPageCompleted());
	}
	
	//The default tag format is "vYYYYMMDD"
	private String getTagTemplate(){
		String tag = getTagPrefix();
		Calendar today = Calendar.getInstance( );
		tag += today.get(Calendar.YEAR);
		int month = today.get(Calendar.MONTH) + 1;
		if(month < 10){
			tag += "0" + month; //$NON-NLS-1$
		}
		else{
			tag += month;
		}
		int day = today.get(Calendar.DAY_OF_MONTH);
		if (day < 10){
			tag += "0" + day;
		}
		else{
			tag += day;
		}
		return tag;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		ReleaseWizard wizard = (ReleaseWizard)getWizard();
		boolean b = wizard.getProjectSelectionPage().isCompareButtonChecked();
		if (visible && !b) {		
			//In case the wizard switches to this page from Project Selection Page and there are some projects have outgoing changes
			wizard.updateSelectedProject();
		}
		tagCombo.setFocus();
	}
	
	public boolean isValidateButtonSelected(){
		return validateButtonSelected;
	}
	
	private String[] addToTagList(String[] history, String newEntry) {
		ArrayList l = new ArrayList(Arrays.asList(history));
		addToTagList(l, newEntry);
		String[] r = new String[l.size()];
		l.toArray(r);
		return r;
	}
	
	private void addToTagList(List history, String newEntry) {
		history.remove(newEntry);
		history.add(0,newEntry);	
		// since only one new item was added, we can be over the limit
		// by at most one item
		if (history.size() > COMBO_HISTORY_LENGTH)
			history.remove(COMBO_HISTORY_LENGTH);
	}
	
	private String getTagPrefix(){
		if(settings != null){
			if(settings.getArray(TAG_KEY) != null){
				String[] tags = settings.getArray(TAG_KEY);
				if (tags != null && tags.length > 0) {
					String s =parseFirstTag(tags[0]);
					if(s != null) return s;
				}
			}
		}
		return DEFAULT_TAG_PREFIX;
	}
	
	private String parseFirstTag(String s){
		if(s == null)return null;
		int length = s.length();
		if(length == 0 || !Character. isLetter(s.charAt(0)))return null;
		int i = 0;
		for (i = 0; i < length; i++) {
			if (Character.isDigit(s.charAt(i))) {
				break;
			}
		}
		return s.substring(0, i);
	}
	
	private void updateFinishStatus(){
		((ReleaseWizard)getWizard()).getContainer().updateButtons();
	}
}
