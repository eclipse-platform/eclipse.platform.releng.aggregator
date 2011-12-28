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

import java.io.InputStream;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.util.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.tools.versioning.IVersionCompare;

/**
 * ClassSourceVersionCompare
 */
public class JavaClassVersionCompare implements VersionCompareConstants {
	// MultiStatus instance used to store IStatus objects which indicate changes between two java classes
	private MultiStatus finalResult;
	// 
	private boolean hasMajorChange;
	private boolean hasMinorChange;
	private boolean hasMicroChange;
	private boolean hasError;

	/**
	 * Constructor
	 */
	public JavaClassVersionCompare() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.internal.versioning.VersionCompareDispatcher#checkJavaClassVersions(String, String, IProgressMonitor)
	 */
	public int checkJavaClassVersions(MultiStatus status, Object javaClassObj1, Object javaClassObj2, IProgressMonitor monitor) throws CoreException {
		try {
			monitor = VersioningProgressMonitorWrapper.monitorFor(monitor);
			monitor.beginTask(Messages.JavaClassVersionCompare_comparingClassMsg, 100);
			finalResult = status;
			// get IClassFileReader instances of javaClassObj1 and javaClassObj2
			IClassFileReader classFileReader1 = ClassFileHelper.getReader(javaClassObj1);
			if (classFileReader1 == null) {
				if (javaClassObj1 instanceof InputStream)
					finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, Messages.JavaClassVersionCompare_inputStreamErrMsg, null));
				else
					finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.JavaClassVersionCompare_classFileNotLoadedMsg, javaClassObj1), null));
				return IVersionCompare.ERROR_OCCURRED;
			}
			IClassFileReader classFileReader2 = ClassFileHelper.getReader(javaClassObj2);
			// worked 5%
			monitor.worked(5);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			if (classFileReader2 == null) {
				if (javaClassObj2 instanceof InputStream)
					finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, Messages.JavaClassVersionCompare_inputStreamErrMsg, null));
				else
					finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.JavaClassVersionCompare_classFileNotLoadedMsg, javaClassObj2), null));
				return IVersionCompare.ERROR_OCCURRED;
			}
			// worked 5%
			monitor.worked(5);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			// initialize flags
			hasError = false;
			hasMajorChange = false;
			hasMinorChange = false;
			hasMicroChange = false;
			// compare two classes ( 90% workload)
			compareJavaClasses(classFileReader1, classFileReader2, new SubProgressMonitor(monitor, 90));
			// analysis result
			if (hasError) {
				finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.CLASS_OVERALL_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.JavaClassVersionCompare_classErrorOccurredMsg, charsToString(classFileReader1.getClassName())), null));
				return IVersionCompare.ERROR_OCCURRED;
			}
			if (hasMajorChange) {
				finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_OVERALL_STATUS, NLS.bind(Messages.JavaClassVersionCompare_classMajorChangeMsg, charsToString(classFileReader1.getClassName())), null));
				return IVersionCompare.MAJOR_CHANGE;
			}
			if (hasMinorChange) {
				finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_OVERALL_STATUS, NLS.bind(Messages.JavaClassVersionCompare_classMinorChangeMsg, charsToString(classFileReader1.getClassName())), null));
				return IVersionCompare.MINOR_CHANGE;
			}
			if (hasMicroChange) {
				finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_OVERALL_STATUS, NLS.bind(Messages.JavaClassVersionCompare_classMicroChange, charsToString(classFileReader1.getClassName())), null));
				return IVersionCompare.MICRO_CHANGE;
			}
			return IVersionCompare.NO_CHANGE;
		} finally {
			monitor.done();
		}
	}

	/**
	 * compares two java classes denoted by <code>classFileReader1</code> and <code>classFileReader2</code>,
	 * and check if there is any change from <code>classFileReader2</code> to <code>classFileReader1</code>.
	 * 
	 * @param classFileReader1
	 * @param classFileReader2
	 * @param monitor IProgressMonitor instance
	 * @return compare result IStatus instance
	 */
	private IStatus compareJavaClasses(IClassFileReader classFileReader1, IClassFileReader classFileReader2, IProgressMonitor monitor) {
		try {
			monitor.beginTask("", 100); //$NON-NLS-1$
			// compare class names
			String name1 = charsToString(classFileReader1.getClassName());
			String name2 = charsToString(classFileReader2.getClassName());
			if (!name1.equals(name2)) {
				finalResult.merge(resultStatusHandler(IStatus.WARNING, IVersionCompare.PROCESS_ERROR_STATUS, NLS.bind(Messages.JavaClassVersionCompare_differentClassNameMsg, name1, name2), null));
				return finalResult;
			}
			// worked 5%
			monitor.worked(5);
			// compare super classes
			checkSuperClasses(name1, classFileReader1.getSuperclassName(), classFileReader2.getSuperclassName());
			// worked 5%
			monitor.worked(5);
			// compare interfaces
			checkInterfaces(name1, generateList(classFileReader1.getInterfaceNames()), generateList(classFileReader2.getInterfaceNames()));
			// worked 5%
			monitor.worked(5);
			// compare modifier of classes
			checkClassModifiers(name1, classFileReader1.getAccessFlags(), classFileReader2.getAccessFlags());
			// worked 5%
			monitor.worked(5);
			// compare fields
			checkElements(name1, generateTable(classFileReader1.getFieldInfos()), generateTable(classFileReader2.getFieldInfos()));
			// worked 40%
			monitor.worked(40);
			// compare methods (40%)
			checkElements(name1, generateTable(classFileReader1.getMethodInfos()), generateTable(classFileReader2.getMethodInfos()));
			return finalResult;
		} finally {
			monitor.done();
		}
	}

	/**
	 * checks if super class has been changed
	 * 
	 * @param className name of the class we are comparing
	 * @param superClassName1
	 * @param superClassName2
	 */
	private void checkSuperClasses(String className, char[] superClassName1, char[] superClassName2) {
		if (!charsToString(superClassName1).equals(charsToString(superClassName2))) {
			Object[] msg = {className, charsToString(superClassName2), charsToString(superClassName1)};
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MAJOR_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_differentSuperClassMsg, msg), null));
		}
	}

	/**
	 * checks if there is any change between <code>interfaceList1</code> and <code>interfaceList2</code>
	 * 
	 * @param className name of the class we are comparing
	 * @param interfaceList1 List of interface name
	 * @param interfaceList2 List of interface name
	 */
	private void checkInterfaces(String className, List interfaceList1, List interfaceList2) {
		for (Iterator nameIterator1 = interfaceList1.iterator(); nameIterator1.hasNext();) {
			// get a name of interface from interfaceNames1
			String name1 = (String) nameIterator1.next();
			// check if the interface is in interfaceNames2
			if (!interfaceList2.remove(name1))
				finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MAJOR_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_newAddedInterfaceMsg, name1, className), null));
		}
		// check what does no longer exist
		for (Iterator nameIterator2 = interfaceList2.iterator(); nameIterator2.hasNext();) {
			String name2 = (String) nameIterator2.next();
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MAJOR_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_deletedInterfaceMsg, name2, className), null));
		}
	}

	/**
	 * checks two class modifiers to see if there is any change from <code>accessFlag2</code> to <code>accessFlag1</code>
	 * @param accessFlag1
	 * @param accessFlag2
	 */
	private void checkClassModifiers(String className, int accessFlag1, int accessFlag2) {
		int change = accessFlag1 ^ accessFlag2;
		if (change != 0) {
			// if the visibility of a method has been narrowed, it is a change should be caught 
			Object[] msg = {className, createChangedModifierString(accessFlag2, change), createChangedModifierString(accessFlag1, change)};
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MAJOR_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_classModifierChangedMsg, msg), null));
		}
	}

	/**
	 * checks corresponding elements(IMethodInfo instances or IFieldInfo instances), 
	 * to see if there is any change
	 * @param className className name of class to which values in <code>elementMap1</code>
	 * 		  (and <code>elementMap2</code>) belong
	 * @param elementMap1 map contains IMethodInfo instances or IFieldInfo instances
	 * @param elementMap2 map contains IMethodInfo instances or IFieldInfo instances
	 */
	private void checkElements(String className, Map elementMap1, Map elementMap2) {
		for (Iterator iterator1 = elementMap1.keySet().iterator(); iterator1.hasNext();) {
			// get a key and value from methodMap1
			String key = (String) iterator1.next();
			Object value1 = elementMap1.get(key);
			// try to get the corresponding method from methodMap2
			Object value2 = elementMap2.get(key);
			if (value2 != null) {
				if (value1 instanceof IMethodInfo)
					// compare the corresponding methods 
					compareMethodInfos(className, (IMethodInfo) value1, (IMethodInfo) value2);
				else
					// compare the corresponding fields in fieldInfoMap1 and fieldInfoMap2
					compareFieldInfos(className, (IFieldInfo) value1, (IFieldInfo) value2);
				elementMap2.remove(key);
			} else {
				if (value1 instanceof IMethodInfo) {
					if (Flags.isPublic(((IMethodInfo) value1).getAccessFlags()) || Flags.isProtected(((IMethodInfo) value1).getAccessFlags())) {
						Object[] msg = new Object[] {METHOD_TITLE, getSignatureString(value1), className};
						finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MINOR_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_newAddedMsg, msg), null));
					}
				} else {
					if (Flags.isPublic(((IFieldInfo) value1).getAccessFlags()) || Flags.isProtected(((IFieldInfo) value1).getAccessFlags())) {
						Object[] msg = new Object[] {FIELD_TITLE, getSignatureString(value1), className};
						finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MINOR_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_newAddedMsg, msg), null));
					}
				}
			}
		}
		// if there are anythings left in fieldInfoMap1, they no longer exist in the class 
		for (Iterator iterator2 = elementMap2.values().iterator(); iterator2.hasNext();) {
			Object object = iterator2.next();
			if (object instanceof IMethodInfo) {
				// we only care which is protected or public
				if (Flags.isPublic(((IMethodInfo) object).getAccessFlags()) || Flags.isProtected(((IMethodInfo) object).getAccessFlags())) {
					Object[] msg = new Object[] {METHOD_TITLE, getSignatureString(object), className};
					finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MAJOR_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_noLongerExistMsg, msg), null));
				}
			} else {
				if (Flags.isPublic(((IFieldInfo) object).getAccessFlags()) || Flags.isProtected(((IFieldInfo) object).getAccessFlags())) {
					Object[] msg = new Object[] {FIELD_TITLE, getSignatureString(object), className};
					finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MAJOR_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_noLongerExistMsg, msg), null));
				}
			}

		}
	}

	/**
	 * compares two IMethodInfo instances to find any change from <code>method2</code> to <code>method1</code>
	 * 
	 * @param className name of class to which <code>method1</code>(and <code>method2</code>) belongs 
	 * @param method1 IMethodInfo instance
	 * @param method2 IMethodInfo instance
	 */
	private void compareMethodInfos(String className, IMethodInfo method1, IMethodInfo method2) {
		// compare AccessFlags (AccessFlags defines the modifiers of a method (e.g. public static final compareTo())
		int accessFlag1 = method1.getAccessFlags();
		int accessFlag2 = method2.getAccessFlags();
		// we just care about field who was public or protected
		if (!(Flags.isPublic(accessFlag2) || Flags.isProtected(accessFlag2)))
			return;
		if (isModifierNarrowed(accessFlag1, accessFlag2)) {
			// get what modifiers have been changed
			int change = accessFlag1 ^ accessFlag2;
			// if the visibility of a method has been narrowed, it is a change should be caught 
			Object[] msg = {METHOD_TITLE, getSignatureString(method1), createChangedModifierString(accessFlag2, change), createChangedModifierString(accessFlag1, change), className};
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MAJOR_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_ModifierChangedMsg, msg), null));
		}
		// compare isDeprecated(if a method has been deprecated)
		if (method1.isDeprecated() && !method2.isDeprecated()) {
			Object[] msg = {METHOD_TITLE, getSignatureString(method1), className};
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MICRO_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_deprecatedChangedMsg, msg), null));
		}
		// compare exceptions
		IExceptionAttribute exceptionAtrributes1 = method1.getExceptionAttribute();
		IExceptionAttribute exceptionAtrributes2 = method2.getExceptionAttribute();
		checkExceptions(className, getSignatureString(method1), exceptionAtrributes1, exceptionAtrributes2);
	}

	/**
	 * check two IExceptionAttribute instances to see if there is any change
	 * @param className name of the class to which the method denoted by <code>methodName</code> belongs
	 * @param methodName name of the method to which exceptionsAttribute1(exceptionsAttribute2) belongs 
	 * @param exceptionsAttribute1
	 * @param exceptionsAttribute2
	 */
	private void checkExceptions(String className, String methodName, IExceptionAttribute exceptionsAttribute1, IExceptionAttribute exceptionsAttribute2) {
		List exceptionList1 = null;
		List exceptionList2 = null;
		if (exceptionsAttribute1 != null && exceptionsAttribute2 != null) {
			exceptionList1 = generateList(exceptionsAttribute1.getExceptionNames());
			exceptionList2 = generateList(exceptionsAttribute2.getExceptionNames());
		} else if (exceptionsAttribute1 != null) {
			exceptionList1 = generateList(exceptionsAttribute1.getExceptionNames());
		} else if (exceptionsAttribute2 != null) {
			exceptionList2 = generateList(exceptionsAttribute2.getExceptionNames());
		} else {
			return;
		}
		StringBuffer newAddedExceptions = new StringBuffer();
		if (exceptionList1 != null) {
			// check what is new added
			for (Iterator iterator1 = exceptionList1.iterator(); iterator1.hasNext();) {
				// get a exception from exceptionList1
				String exception1 = (String) iterator1.next();
				if (exceptionList2 != null) {
					// check if the exception is in exceptionList2
					if (!exceptionList2.remove(exception1)) {
						newAddedExceptions.append(exception1);
						newAddedExceptions.append(COMMA_MARK);
					}
				} else {
					// if exceptionList2 is null, it is new added
					newAddedExceptions.append(exception1);
					newAddedExceptions.append(COMMA_MARK);
				}
			}
			if (!newAddedExceptions.toString().trim().equals(EMPTY_STRING)) {
				Object[] msg = {newAddedExceptions.toString(), methodName, className};
				finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MINOR_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_newAddedExceptionMsg, msg), null));
			}
		}
		if (exceptionList2 != null) {
			StringBuffer notExistExceptions = new StringBuffer();
			// check what does no longer exist
			for (Iterator iterator2 = exceptionList2.iterator(); iterator2.hasNext();) {
				String exception2 = (String) iterator2.next();
				notExistExceptions.append(exception2);
				notExistExceptions.append(COMMA_MARK);
			}
			if (!notExistExceptions.toString().trim().equals(EMPTY_STRING)) {
				Object[] msg2 = {notExistExceptions.toString(), methodName, className};
				finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MINOR_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_noLongerExistExceptionMsg, msg2), null));
			}
		}
	}

	/**
	 * checks if there is any change from <code>accessFlag2</code> to <code>accessFlag1</code>
	 * we check changes which narrowed down the visibility of a field or method with protected or public modifier
	 * 
	 * @param accessFlag1 denotes combined modifiers of a field or method
	 * @param accessFlag2 denotes combined modifiers of a field or method
	 * @return <code>true</code> if visibility has been narrowed down from <code>accessFlag2</code> to <code>accessFlag1</code>,
	 *         <code>false</code> otherwise
	 */
	private boolean isModifierNarrowed(int accessFlag1, int accessFlag2) {
		if (Flags.isProtected(accessFlag2)) {
			// from protected to modifier other than public and protected
			if (!(Flags.isPublic(accessFlag1) || Flags.isProtected(accessFlag1))) {
				return true;
			}
		} else if (Flags.isPublic(accessFlag2)) {
			// from public to modifier other than public
			if (!Flags.isPublic(accessFlag1)) {
				return true;
			}
		} else {
			// only check which was protected or public
			return false;
		}
		// from non-final to final  
		if (!Flags.isFinal(accessFlag2) && Flags.isFinal(accessFlag1)) {
			return true;
		}
		// from static to non-static
		if (Flags.isStatic(accessFlag2) && !Flags.isStatic(accessFlag1)) {
			return true;
		}
		// from non-abstract to abstract
		if (!Flags.isAbstract(accessFlag2) && Flags.isAbstract(accessFlag1)) {
			return true;
		}
		// volatile modifier changed
		if (Flags.isVolatile(accessFlag2) != Flags.isVolatile(accessFlag1)) {
			return true;
		}
		return false;
	}

	/**
	 * check if <code>accessFlag</code> is <code>default</code>
	 * @param accessFlag
	 * @return <code>true</code> if <code>addessFlag</code> is <code>default</code>,
	 * 		   <code>false</code> otherwise 
	 */
	private boolean isDefault(int accessFlag) {
		return (accessFlag & DEFAULT_MODIFIER_TESTER) == 0 ? true : false;
	}

	/**
	 * compares two IFieldInfo instances to find any change from <code>field1</code> to <code>field2</code>
	 * 
	 * @param className name of class to which <code>field1</code>(and <code>field2</code>) belongs
	 * @param field1 IFieldInfo instance
	 * @param field2 IFieldInfo instance
	 */
	private void compareFieldInfos(String className, IFieldInfo field1, IFieldInfo field2) {
		// compare AccessFlags (AccessFlags defines the modifiers of a field (e.g. public static final IVersionCompare.PLUGIN_ID = "org.eclipse.pde.tools.versioning")
		int accessFlag1 = field1.getAccessFlags();
		int accessFlag2 = field2.getAccessFlags();
		// we just care about field who was public or protected
		if (!(Flags.isPublic(accessFlag2) || Flags.isProtected(accessFlag2))) {
			return;
		}
		if (isModifierNarrowed(accessFlag1, accessFlag2)) {
			// get what modifiers have been changed
			int change = accessFlag1 ^ accessFlag2;
			// if the visibility of a field which was public or protected has been narrowed, it is a change should be caught 
			Object[] msg = {FIELD_TITLE, charsToString(field1.getName()), createChangedModifierString(accessFlag2, change), createChangedModifierString(accessFlag1, change), className};
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MAJOR_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_ModifierChangedMsg, msg), null));
		}
		// compare Descriptor (Descriptor describes the type of a field)
		String descriptor1 = charsToString(field1.getDescriptor());
		String descriptor2 = charsToString(field2.getDescriptor());
		if (!descriptor1.equals(descriptor2)) {
			String[] msg = {charsToString(field1.getName()), Signature.toString(descriptor2), Signature.toString(descriptor1), className};
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MAJOR_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_descriptorChangedMsg, msg), null));
		}
		// compare isDeprecated (if a field has been deprecated)
		if (field1.isDeprecated() && !field2.isDeprecated()) {
			Object[] msg = {FIELD_TITLE, charsToString(field1.getName()), className};
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_DETAIL_STATUS | IVersionCompare.MICRO_CHANGE, NLS.bind(Messages.JavaClassVersionCompare_deprecatedChangedMsg, msg), null));
		}
	}

	/**
	 * creates modifier string which includes multiple modifiers(e.g. "public static final")
	 * 
	 * @param accessFlags accessFlags of field or method
	 * @param changedFlags indicates change on accessFlags
	 * @return modifier string
	 */
	private String createChangedModifierString(int accessFlags, int changedFlags) {
		StringBuffer buffer = new StringBuffer();
		if (Flags.isPublic(changedFlags)) {
			if (Flags.isPublic(accessFlags)) {
				buffer.append(PUBLIC_STRING);
				buffer.append(SPACE);
			} else if (isDefault(accessFlags)) {
				buffer.append(DEFAULT_STRING);
				buffer.append(SPACE);
			}
		}
		if (Flags.isProtected(changedFlags)) {
			if (Flags.isProtected(accessFlags)) {
				buffer.append(PROTECTED_STRING);
				buffer.append(SPACE);
			} else if (isDefault(accessFlags)) {
				buffer.append(DEFAULT_STRING);
				buffer.append(SPACE);
			}
		}
		if (Flags.isPrivate(changedFlags)) {
			if (Flags.isPrivate(accessFlags)) {
				buffer.append(PRIVATE_STRING);
				buffer.append(SPACE);
			} else if (isDefault(accessFlags)) {
				buffer.append(DEFAULT_STRING);
				buffer.append(SPACE);
			}
		}
		if (Flags.isStatic(changedFlags)) {
			if (Flags.isStatic(accessFlags)) {
				buffer.append(STATIC_STRING);
				buffer.append(SPACE);
			} else {
				buffer.append(NON_STATIC_STRING);
				buffer.append(SPACE);
			}
		}
		if (Flags.isFinal(changedFlags)) {
			if (Flags.isFinal(accessFlags)) {
				buffer.append(FINAL_STRING);
				buffer.append(SPACE);
			} else {
				buffer.append(NON_FINAL_STRING);
				buffer.append(SPACE);
			}
		}
		if (Flags.isAbstract(changedFlags)) {
			if (Flags.isAbstract(accessFlags)) {
				buffer.append(ABSTRACT_STRING);
				buffer.append(SPACE);
			} else {
				buffer.append(NON_ABSTRACT_STRING);
				buffer.append(SPACE);
			}
		}
		if (Flags.isVolatile(changedFlags)) {
			if (Flags.isVolatile(accessFlags)) {
				buffer.append(VOLATILE_STRING);
				buffer.append(SPACE);
			} else {
				buffer.append(NON_VOLATILE_STRING);
				buffer.append(SPACE);
			}
		}
		return buffer.toString().trim();
	}

	/**
	 * converts char array to String
	 * @param chars array of char
	 * @return String which represents the content of <code>chars</code>
	 */
	private String charsToString(char[] chars) {
		return new String(chars);
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
		if (objects[0] instanceof char[])
			for (int i = 0; i < objects.length; i++)
				list.add(charsToString((char[]) objects[i]));
		else
			for (int i = 0; i < objects.length; i++)
				list.add(objects[i]);
		return list;
	}

	/**
	 * converts the given object signature to a readable string
	 * @param object IFieldInfo instance of IMethodInfo instance
	 * @return String type signature
	 */
	private String getSignatureString(Object object) {
		StringBuffer buffer = new StringBuffer();
		if (object instanceof IMethodInfo) {
			IMethodInfo method = (IMethodInfo) object;
			buffer.append(Flags.toString(method.getAccessFlags()));
			buffer.append(SPACE);
			buffer.append(Signature.toString(charsToString(method.getDescriptor()), charsToString(method.getName()), null, false, true));
		} else {
			IFieldInfo field = (IFieldInfo) object;
			buffer.append(Flags.toString(field.getAccessFlags()));
			buffer.append(SPACE);
			buffer.append(Signature.toString(charsToString(field.getDescriptor())));
			buffer.append(SPACE);
			buffer.append(charsToString(field.getName()));
		}
		return buffer.toString();
	}

	/**
	 * generates a map which stores IField instances or IMethod instances
	 * 
	 * @param objects array of IMethodInfo or IFieldInfo instances
	 * @return Map instance contains IField instances or IMethod instances
	 */
	private Map generateTable(Object[] objects) {
		Hashtable hashTable = new Hashtable(0);
		if (objects == null || objects.length == 0) {
			return hashTable;
		}
		// set hashtable
		if (objects[0] instanceof IFieldInfo) {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] != null)
					hashTable.put(charsToString(((IFieldInfo) objects[i]).getName()), objects[i]);
			}
		} else if (objects[0] instanceof IMethodInfo) {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] != null)
					hashTable.put(getMethodKey((IMethodInfo) objects[i]), objects[i]);
			}
		}
		return hashTable;
	}

	/**
	 * get key of a method. Key of a method is a String which 
	 * includes the retype of the method,name of the method and the parameter types of the method
	 * (e.g. key of public int foo(int , String) is "int#foo#int#String"
	 * @param method
	 * @return key of a method
	 */
	private String getMethodKey(IMethodInfo method) {
		StringBuffer buffer = new StringBuffer();
		// append return type
		buffer.append(method.getDescriptor());
		// append name of method
		buffer.append(charsToString(method.getName()));
		char[][] parameters = Signature.getParameterTypes(method.getDescriptor());
		// append parameter types
		if (parameters != null) {
			for (int i = 0; i < parameters.length; i++) {
				buffer.append(KEY_SEPARATOR);
				buffer.append(charsToString(parameters[i]));
			}
		}
		return buffer.toString();
	}

	/**
	 * Return a new status object populated with the given information.
	 * 
	 * @param severity severity of status
	 * @param code indicates type of this IStatus instance, it could be one of: FEATURE_OVERALL_STATUS, 
	 * 		 FEATURE_DETAIL_STATUS, PLUGIN_OVERALL_STATUS, PLUGIN_DETAIL_STATUS, PROCESS_ERROR_STATUS,
	 * 		 CLASS_OVERALL_STATUS, CLASS_DETAIL_STATUS
	 * @param message the status message
	 * @param exception exception which has been caught, or <code>null</code>
	 * @return the new status object
	 */
	private IStatus resultStatusHandler(int severity, int code, String message, Exception exception) {
		processed(code);

		if (message == null) {
			if (exception != null)
				message = exception.getMessage();
			// extra check because the exception message can be null
			if (message == null)
				message = EMPTY_STRING;
		}
		return new Status(severity, PLUGIN_ID, code, message, exception);
	}

	/**
	 * checks what kind of change does <code>code</code> represent
	 * @param code 
	 */
	private void processed(int code) {
		if ((code & IVersionCompare.ERROR_OCCURRED) != 0){
			hasError = true;
			return;
		}
		if ((code & IVersionCompare.MAJOR_CHANGE) != 0){
			hasMajorChange = true;
			return;
		}
		if ((code & IVersionCompare.MINOR_CHANGE) != 0){
			hasMinorChange = true;
			return;
		}
		if ((code & IVersionCompare.MICRO_CHANGE) != 0)
			hasMicroChange = true;
	}
}
