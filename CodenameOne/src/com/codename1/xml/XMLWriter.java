/*
 * Copyright 2013 Paul Harrison Williams. All rights reserved.
 * 
 * Please contact pwillcode@gmail.com if you have licensing questions.
 */

package com.codename1.xml;

import com.codename1.util.CStringBuilder;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;

import com.codename1.util.StringUtil;

public class XMLWriter {

	private boolean encodeText;
	
	private static final String[][] escapes = {
		{"\"", "&quot;"},
		{"&", "&amp;"},
		{"<", "&lt;"},
		{">", "&gt;"},
		{"'", "&apos;"}
    };
	
	/**
	 * Creates a new XMLWriter.
	 * @param encodeText Determines whether or not text components (text content, tag names, attribute names, and attribute values) should be encoded with escapes when written. Use false if these values are already encoded.
	 */
	public XMLWriter(boolean encodeText) {
		this.encodeText = encodeText;
	}
	
	/**
	 * Writes the XML of an Element to a Writer.
	 * Note: may output invalid XML if you created text Elements using un-escaped Strings.
	 * @param writer The Writer to which the XML will be written.
	 * @param element The element whose XML will be written.
	 * @throws IOException if a write operation fails.
	 */
	public void writeXML(Writer writer, Element element) throws IOException {
		writeXML(writer, element, new CStringBuilder(), false);
	}
	
	/**
	 * Writes the XML of an Element to a Writer using a given starting indentation.
	 * Note: may output invalid XML if you created text Elements using un-escaped Strings.
	 * @param writer The Writer to which the XML will be written.
	 * @param element The element whose XML will be written.
	 * @param indentation A starting indentation for the given Element.
	 * @param isInline Whether or not the given element Element should be treated as part of in-line content.
	 * @throws IOException if a write operation fails.
	 */
	private void writeXML(Writer writer, Element element,
			CStringBuilder indentation, boolean isInline) throws IOException {
		if (!isInline)
			writer.write(indentation.toString());
		
		if (element.isTextElement()) {
			writer.write(encodeIfRequired(element.getText()));
		} else {
			writer.write('<');
			String elementName = encodeIfRequired(element.getTagName());
			writer.write(elementName);
			
			Hashtable attributes = element.getAttributes();
			if (attributes != null) {
				for(Enumeration keys = attributes.keys(); keys.hasMoreElements();) {
	                String attributeKey = (String)keys.nextElement();
	                String attributeValue = (String)attributes.get(attributeKey);
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
					for (Object child : element.getChildren()) {
						if (child instanceof Element)
							writeXML(writer, (Element)child, indentation, isInline);
						else
							throw new IllegalStateException("Element contained child of invalid type");
						writer.write('\n');
					}
				} else {
					isInline = true;
					for (Object child : element.getChildren()) {
						if (child instanceof Element)
							writeXML(writer, (Element)child, null, isInline);
						else
							throw new IllegalStateException("Element contained child of invalid type");
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
			for (int i = 0; i < escapes.length; i++) {
				text = StringUtil.replaceAll(text, escapes[i][0], escapes[i][1]);
			}
		}
		return text;
	}
	
}
