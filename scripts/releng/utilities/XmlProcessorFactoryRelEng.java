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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

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
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY_ERROR_ON_DOCTYPE = createDocumentBuilderFactoryWithErrorOnDOCTYPE();
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY_IGNORING_DOCTYPE = createDocumentBuilderFactoryIgnoringDOCTYPE();

	/**
	 * Creates DocumentBuilderFactory which throws SAXParseException when detecting
	 * external entities. It's magnitudes faster to call
	 * {@link #createDocumentBuilderWithErrorOnDOCTYPE()}.
	 *
	 * @return javax.xml.parsers.DocumentBuilderFactory
	 */
	private static synchronized DocumentBuilderFactory createDocumentBuilderFactoryWithErrorOnDOCTYPE() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// completely disable DOCTYPE declaration:
		try {
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return factory;
	}

	/**
	 * Creates DocumentBuilderFactory which ignores external entities. Beware:
	 * DocumentBuilder created with this Factory may load DTDs from a remote Host -
	 * which may be slow and a security risk. It's recommended to call
	 * DocumentBuilder.setEntityResolver(EntityResolver) with fixed DTDs. <br>
	 * It's magnitudes faster to call
	 * {@link #createDocumentBuilderIgnoringDOCTYPE()}.
	 *
	 * @return javax.xml.parsers.DocumentBuilderFactory
	 * @see javax.xml.parsers.DocumentBuilder#setEntityResolver(EntityResolver)
	 */
	public static synchronized DocumentBuilderFactory createDocumentBuilderFactoryIgnoringDOCTYPE() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			// completely disable external entities declarations:
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false); //$NON-NLS-1$
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return factory;
	}

	/**
	 * Creates DocumentBuilder which throws SAXParseException when detecting
	 * external entities. The builder is not thread safe.
	 *
	 * @return javax.xml.parsers.DocumentBuilder
	 */
	public static synchronized DocumentBuilder createDocumentBuilderWithErrorOnDOCTYPE()
			throws ParserConfigurationException {
		return DOCUMENT_BUILDER_FACTORY_ERROR_ON_DOCTYPE.newDocumentBuilder();
	}

	/**
	 * Creates DocumentBuilder which ignores external entities and does not load
	 * remote DTDs. The builder is not thread safe.
	 *
	 * @return javax.xml.parsers.DocumentBuilder
	 */
	public static synchronized DocumentBuilder createDocumentBuilderIgnoringDOCTYPE()
			throws ParserConfigurationException {
		DocumentBuilder builder = DOCUMENT_BUILDER_FACTORY_IGNORING_DOCTYPE.newDocumentBuilder();
		builder.setEntityResolver((__, ___) -> new InputSource(new ByteArrayInputStream(new byte[0])));
		return builder;
	}

}