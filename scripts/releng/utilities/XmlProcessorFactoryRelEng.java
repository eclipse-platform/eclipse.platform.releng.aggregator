/*******************************************************************************
 *  Copyright (c) 2023, 2025 Joerg Kubitz and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Joerg Kubitz - initial API and implementation
 *******************************************************************************/
package utilities;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XML processing which prohibits external entities.
 *
 * @see <a href="https://rules.sonarsource.com/java/RSPEC-2755/">RSPEC-2755</a>
 */
public class XmlProcessorFactoryRelEng {
	private XmlProcessorFactoryRelEng() {
		// static Utility only
	}

	// using these factories is synchronized with creating & configuring them
	// potentially concurrently in another thread:
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY_ERROR_ON_DOCTYPE;
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY_IGNORING_DOCTYPE;

	static {
		try {
			DocumentBuilderFactory factory1 = DocumentBuilderFactory.newInstance();
			// completely disable DOCTYPE declaration:
			factory1.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
			DOCUMENT_BUILDER_FACTORY_ERROR_ON_DOCTYPE = factory1;

			DocumentBuilderFactory factory2 = DocumentBuilderFactory.newInstance();
			// completely disable external entities declarations:
			factory2.setFeature("http://xml.org/sax/features/external-general-entities", false); //$NON-NLS-1$
			factory2.setFeature("http://xml.org/sax/features/external-parameter-entities", false); //$NON-NLS-1$
			DOCUMENT_BUILDER_FACTORY_IGNORING_DOCTYPE = factory2;
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Parses the given XML files, throwing an SAXParseException when detecting
	 * external entities.
	 */
	public static synchronized Document parseDocumentWithErrorOnDOCTYPE(Path file)
			throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilder builder = DOCUMENT_BUILDER_FACTORY_ERROR_ON_DOCTYPE.newDocumentBuilder();
		return builder.parse(file.toFile());
	}

	/**
	 * Parses the given XML files, ignoring external entities and not loading remote
	 * DTDs.
	 */
	public static synchronized Document parseDocumentIgnoringDOCTYPE(Path file)
			throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilder builder = DOCUMENT_BUILDER_FACTORY_IGNORING_DOCTYPE.newDocumentBuilder();
		builder.setEntityResolver((__, ___) -> new InputSource(new ByteArrayInputStream(new byte[0])));
		return builder.parse((file.toFile()));
	}

	public static Iterable<Element> elements(NodeList list) {
		return () -> IntStream.range(0, list.getLength()).mapToObj(list::item).filter(Element.class::isInstance)
				.map(Element.class::cast).iterator();
	}

}