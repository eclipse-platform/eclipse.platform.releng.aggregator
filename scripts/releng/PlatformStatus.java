
/*******************************************************************************
 *  Copyright (c) 2000, 2025 IBM Corporation and others.
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
 *******************************************************************************/

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

record PlatformStatus(String id, String name, String fileName, String format, List<String> images) {

	static PlatformStatus create(Element anElement) {
		NamedNodeMap attributes = anElement.getAttributes();
		String id = attributes.getNamedItem("id").getNodeValue();
		Node node = attributes.getNamedItem("name");
		String name = node == null ? "" : node.getNodeValue();
		String fileName = attributes.getNamedItem("fileName").getNodeValue();
		node = attributes.getNamedItem("format");
		String format = node != null ? node.getNodeValue() : null;
		node = attributes.getNamedItem("images");
		List<String> images = node != null ? List.of(node.getNodeValue().split(",")) : null;
		return new PlatformStatus(id, name, fileName, format, images);
	}

	public void registerError() {
		// Had no effect. Callers should be removed/clean-up.
	}
}
