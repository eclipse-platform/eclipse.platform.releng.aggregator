package org.eclipse.releng.tools;

import org.eclipse.core.resources.IFile;

public class JavaScriptFile extends SourceFile {

	public JavaScriptFile(IFile file) {
		super(file);
	}

	/* (non-Javadoc)
	 * @see Test.popup.actions.SourceFile#getCommentStart()
	 */
	public String getCommentStart() {
		return "/*";
	}

	/* (non-Javadoc)
	 * @see Test.popup.actions.SourceFile#getCommentEnd()
	 */
	public String getCommentEnd() {
		return "*/";
	}
	public int getFileType() {
		return CopyrightComment.JAVASCRIPT_COMMENT;
	}

}
