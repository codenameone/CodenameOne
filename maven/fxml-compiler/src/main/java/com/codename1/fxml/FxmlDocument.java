/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.fxml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/// An FXML file as the compiler reads it: the element tree with the line
/// and column of every element, and the `<?import?>` instructions.
///
/// The position of an element is where its start tag ends, which is what
/// the XML parser reports; a message says which attribute it is about.
final class FxmlDocument {

    /// What an `fx:` name is in: the FXML namespace, whatever its version.
    private static final String FX = "http://javafx.com/fxml";

    /// One attribute.
    static final class Attribute {
        final String name;
        final boolean fx;
        final String value;

        Attribute(String name, boolean fx, String value) {
            this.name = name;
            this.fx = fx;
            this.value = value;
        }

        /// The attribute as it was written, for a message.
        String text() {
            return (fx ? "fx:" : "") + name + "=\"" + value + "\"";
        }
    }

    /// One element. A child is an [Element] or a `String` of text.
    static final class Element {
        final String name;
        final boolean fx;
        final int line;
        final int column;
        final List<Attribute> attributes = new ArrayList<Attribute>();
        final List<Object> children = new ArrayList<Object>();

        Element(String name, boolean fx, int line, int column) {
            this.name = name;
            this.fx = fx;
            this.line = line;
            this.column = column;
        }

        /// The value of an `fx:` attribute, or `null`.
        String fx(String attribute) {
            for (Attribute a : attributes) {
                if (a.fx && a.name.equals(attribute)) {
                    return a.value;
                }
            }
            return null;
        }

        /// The value of a plain attribute, or `null`.
        String plain(String attribute) {
            for (Attribute a : attributes) {
                if (!a.fx && a.name.equals(attribute)) {
                    return a.value;
                }
            }
            return null;
        }

        /// The start tag as it was written, for a message.
        String text() {
            StringBuilder s = new StringBuilder("<").append(fx ? "fx:" : "").append(name);
            for (Attribute a : attributes) {
                s.append(' ').append(a.text());
            }
            return s.append('>').toString();
        }

        /// The text directly inside the element, trimmed; empty when there
        /// is none.
        String content() {
            StringBuilder s = new StringBuilder();
            for (Object child : children) {
                if (child instanceof String) {
                    s.append((String) child);
                }
            }
            return s.toString().trim();
        }

        /// The child elements.
        List<Element> elements() {
            List<Element> out = new ArrayList<Element>();
            for (Object child : children) {
                if (child instanceof Element) {
                    out.add((Element) child);
                }
            }
            return out;
        }
    }

    /// The file, as messages name it.
    final String file;
    /// The resource path, without a leading slash.
    final String path;
    /// The imports in the order written: a class name, or a package
    /// followed by `.*`.
    final List<String> imports = new ArrayList<String>();
    Element root;

    private FxmlDocument(String file, String path) {
        this.file = file;
        this.path = path;
    }

    /// Reads a document. Answers `null`, with the reason recorded as an
    /// error, for a file that is not well formed XML or that uses a
    /// scripting language.
    static FxmlDocument parse(String file, String path, InputStream in, final Messages messages)
            throws IOException {
        final FxmlDocument doc = new FxmlDocument(file, path);
        final boolean[] failed = new boolean[1];
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            // A document is the application's own file, but nothing in FXML
            // needs a DTD or an external entity, and a build must not fetch
            // one.
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            SAXParser parser = factory.newSAXParser();
            InputSource source = new InputSource(in);
            source.setSystemId(file);
            parser.parse(source, new DefaultHandler() {
                private Locator locator;
                private final List<Element> stack = new ArrayList<Element>();

                @Override
                public void setDocumentLocator(Locator l) {
                    locator = l;
                }

                @Override
                public void processingInstruction(String target, String data) {
                    String value = data == null ? "" : data.trim();
                    if ("import".equals(target)) {
                        doc.imports.add(value);
                    } else if ("language".equals(target)) {
                        failed[0] = true;
                        doc.error(messages, locator, "<?language " + value + "?>: scripts are not supported. A"
                                + " document is compiled to Java; write the handler as a controller method and"
                                + " name it with onAction=\"#method\"");
                    }
                }

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    Element e = new Element(localName, isFx(uri), locator.getLineNumber(),
                            locator.getColumnNumber());
                    for (int i = 0; i < attributes.getLength(); i++) {
                        e.attributes.add(new Attribute(attributes.getLocalName(i), isFx(attributes.getURI(i)),
                                attributes.getValue(i)));
                    }
                    if (stack.isEmpty()) {
                        doc.root = e;
                    } else {
                        stack.get(stack.size() - 1).children.add(e);
                    }
                    stack.add(e);
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    stack.remove(stack.size() - 1);
                }

                @Override
                public void characters(char[] ch, int start, int length) {
                    if (!stack.isEmpty()) {
                        stack.get(stack.size() - 1).children.add(new String(ch, start, length));
                    }
                }

                @Override
                public void error(SAXParseException e) throws SAXException {
                    throw e;
                }
            });
        } catch (SAXParseException e) {
            messages.error(file, Math.max(1, e.getLineNumber()), Math.max(1, e.getColumnNumber()),
                    "not well formed XML: " + e.getMessage());
            return null;
        } catch (SAXException e) {
            messages.error(file, 1, 1, "not well formed XML: " + e.getMessage());
            return null;
        } catch (ParserConfigurationException e) {
            throw new IOException("No usable XML parser in this JDK: " + e.getMessage(), e);
        }
        if (failed[0] || doc.root == null) {
            return null;
        }
        return doc;
    }

    private static boolean isFx(String uri) {
        return uri != null && uri.startsWith(FX);
    }

    private void error(Messages messages, Locator locator, String message) {
        messages.error(file, locator == null ? 1 : locator.getLineNumber(),
                locator == null ? 1 : locator.getColumnNumber(), message);
    }
}
