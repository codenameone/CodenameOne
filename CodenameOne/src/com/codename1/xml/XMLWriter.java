/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
/*
 * Copyright 2013 Paul Harrison Williams. All rights reserved.
 * 
 * Please contact pwillcode@gmail.com if you have licensing questions.
 */
/*
* This was contributed as part of this issue: https://github.com/codenameone/CodenameOne/issues/753
* The original contribution comments were partially modified with the migration from Google Code to github.
*/
package com.codename1.xml;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;

import com.codename1.util.StringUtil;

/**
 * <p>{@code XMLWriter} writes an XML {@link com.codename1.xml.Element} into an XML string/file
 * this allowing us to serialize previously parsed and modified XML.</p>
 * 
 * @author Paul Harrison Williams<pwillcode@gmail.com>
 */
public class XMLWriter {

    private boolean encodeText;

    private static final String[][] escapes = {
        {"&", "&amp;"},
        {"\"", "&quot;"},
        {"<", "&lt;"},
        {">", "&gt;"},
        {"'", "&apos;"}
    };

    /**
     * Creates a new XMLWriter.
     *
     * @param encodeText Determines whether or not text components (text
     * content, tag names, attribute names, and attribute values) should be
     * encoded with escapes when written. Use false if these values are already
     * encoded.
     */
    public XMLWriter(boolean encodeText) {
        this.encodeText = encodeText;
    }

    /**
     * Writes the XML of an Element to a Writer. Note: may output invalid XML if
     * you created text Elements using un-escaped Strings.
     *
     * @param writer The Writer to which the XML will be written.
     * @param element The element whose XML will be written.
     * @throws IOException if a write operation fails.
     */
    public void writeXML(Writer writer, Element element) throws IOException {
        writeXML(writer, element, new StringBuilder(), false);
    }

    /**
     * Writes the XML of an Element to a Writer using a given starting
     * indentation. Note: may output invalid XML if you created text Elements
     * using un-escaped Strings.
     *
     * @param writer The Writer to which the XML will be written.
     * @param element The element whose XML will be written.
     * @param indentation A starting indentation for the given Element.
     * @param isInline Whether or not the given element Element should be
     * treated as part of in-line content.
     * @throws IOException if a write operation fails.
     */
    private void writeXML(Writer writer, Element element,
            StringBuilder indentation, boolean isInline) throws IOException {
        if (!isInline) {
            writer.write(indentation.toString());
        }

        if (element.isTextElement()) {
            writer.write(encodeIfRequired(element.getText()));
        } else {
            writer.write('<');
            String elementName = encodeIfRequired(element.getTagName());
            writer.write(elementName);

            Hashtable attributes = element.getAttributes();
            if (attributes != null) {
                for (Enumeration keys = attributes.keys(); keys.hasMoreElements();) {
                    String attributeKey = (String) keys.nextElement();
                    String attributeValue = (String) attributes.get(attributeKey);
                    writer.write(' ');
                    writer.write(encodeIfRequired(attributeKey));
                    writer.write("=\"");
                    writer.write(encodeIfRequired(attributeValue));
                    writer.write('"');
                }
            }

            if (element.isEmpty()) {
                writer.write(" />");
            } else {
                writer.write('>');

                if (!isInline && !element.hasTextChild()) {
                    writer.write('\n');
                    indentation.append('\t');
                    for (Object child : element) {
                        if (child instanceof Element) {
                            writeXML(writer, (Element) child, indentation, isInline);
                        } else {
                            throw new IllegalStateException("Element contained child of invalid type");
                        }
                        writer.write('\n');
                    }
                } else {
                    isInline = true;
                    for (Object child : element) {
                        if (child instanceof Element) {
                            writeXML(writer, (Element) child, null, isInline);
                        } else {
                            throw new IllegalStateException("Element contained child of invalid type");
                        }
                    }
                }

                if (!isInline) {
                    indentation.deleteCharAt(indentation.length() - 1);
                    writer.write(indentation.toString());
                }
                writer.write("</");
                writer.write(elementName);
                writer.write('>');
            }
        }
    }

    private String encodeIfRequired(String text) {
        if (encodeText) {
            int elen = escapes.length;
            for (int i = 0; i < elen; i++) {
                text = StringUtil.replaceAll(text, escapes[i][0], escapes[i][1]);
            }
        }
        return text;
    }

}
