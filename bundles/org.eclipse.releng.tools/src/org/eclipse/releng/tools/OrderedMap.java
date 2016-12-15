/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Don't implement the Map interface because we don't want people calling #entrySet
 * because order is important.
 */
public class OrderedMap<K, V> {

	private List<K> keys = new ArrayList<>();
	private List<V> values = new ArrayList<>();

	/* (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		keys = new ArrayList<>();
		values = new ArrayList<>();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(K key) {
		for (Iterator<K> iter = keys.iterator(); iter.hasNext();) {
			if (key.equals(iter.next()))
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(V value) {
		for (Iterator<V> iter = values.iterator(); iter.hasNext();) {
			if (value.equals(iter.next()))
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public V get(K key) {
		int index = indexOf(key);
		return index == -1 ? null : values.get(index);
	}

	private int indexOf(Object key) {
		int length = keys.size();
		for (int i = 0; i < length; i++) {
			Object tempKey = keys.get(i);
			if (key.equals(tempKey))
				return i;
		}
		return -1;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		return keys.size() == 0;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public V put(K key, V value) {
		int index = indexOf(key);
		if (index == -1) {
			keys.add(key);
			values.add(value);
			return null;
		}
		V oldValue = values.get(index);
		values.set(index, value);
		return oldValue;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map<K, V> other) {
		for (Iterator<Map.Entry<K, V>> iter = other.entrySet().iterator(); iter.hasNext();) {
			Map.Entry<K, V> entry = iter.next();
			put(entry.getKey(), entry.getValue());
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public V remove(K key) {
		int index = indexOf(key);
		if (index == -1)
			return null;
		keys.remove(index);
		return values.remove(index);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	public int size() {
		return keys.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	public Collection<V> values() {
		return values;
	}
	
	public Collection<K> keys() {
		return keys;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("{"); //$NON-NLS-1$
		for (Iterator<K> iter = keys().iterator(); iter.hasNext(); ) {
			K key = iter.next();
			V value = get(key);
			result.append(key);
			result.append('=');
			result.append(value);
			result.append(",\n"); //$NON-NLS-1$
		}
		// delete last 2 chars... comma and new-line
		if (result.length() > 2) {
			result = result.deleteCharAt(result.length() - 1);
			result = result.deleteCharAt(result.length() - 1);
		}
		result.append("}"); //$NON-NLS-1$
		return result.toString();
	}

}
