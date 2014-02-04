/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.xml;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The Element class defines a single XML element with its attributes and children.
 * Due to its hierarchial nature, this class can be used for a single "leaf" Element, for more complex elements (with child elements), and up to describing the entire document.
 *
 * @author Ofir Leitner
 */
public class Element {

    /**
     * A constant that can be used for the get descendants methods to denote infinite recursion
     */
    public static final int DEPTH_INFINITE = Integer.MAX_VALUE;

    /**
     * True if this is a text element, false otherwise
     */
    private boolean textElement;

    /**
     * The element's name (or text for text elements)
     */
    private String name;

   /**
     * A vector containing this element's children
     */
    private Vector children;

    /**
     * This element's parent
     */
    private Element parent;

    /**
     * A hashtable containing this element's attributes
     */
    private Hashtable attributes;

    boolean isComment;

    /**
     * Constructs and Element without specifying a name
     * This can be used by subclasses that do not require name assigments.
     */
    protected Element() {
    }


    /**
     * Constructs an Element with the specified name
     * 
     * @param tagName The tag name
     */
    public Element(String tagName) {
        this.name=tagName;
    }

    /**
     * Constructs an Element (or a text element) with the specified name or text.
     *
     * @param tagName The tag name, or in the case of a text element the element's text
     * @param isTextElement true for a text element, false otherwise
     */
    public Element(String tagName,boolean isTextElement) {
        this(tagName);
        textElement=isTextElement;
    }


    /**
     * Returns true if this is a text element, false otherwise
     * 
     * @return true if this is a text element, false otherwise
     */
    public boolean isTextElement() {
        return textElement;
    }

    /**
     * Returns this Element's tag name
     *
     * @return the Element's tag name
     * @throws IllegalStateException if this is a text element
     */
    public String getTagName() {
        if (textElement) {
                throw new IllegalStateException("Text elements do not have a tag name");
        }
        return name;
    }

    /**
     * Returns the attributes Hashtable
     *
     * @return the attributes Hashtable
     */
    public Hashtable getAttributes() {
        return attributes;
    }

    /**
     * Adds the specified attribute and value to this Element if it is supported for the Element and has a valid value.
     * This method allows creating a key that is non-string to be used by subclasses that optimize attributes retrieval
     * 
     * @param id The attribute ID
     * @param value The attribute value
     */
    protected void setAttribute(Object id,String value) {
        if (attributes==null) {
            attributes=new Hashtable();
        }
        attributes.put(id,value);
    }

    /**
     * Adds the specified Element as a child to this element.
     * If the specified element was found to be unsupported (i.e. it's ID is TAG_UNSUPPORTED, it is not added.
     *
     * @param childElement The child element
     */
    public void addChild(Element childElement) {
        setChildParent(childElement);
        children.addElement(childElement);
        //childElement.setParent(this);
    }

    /**
     * Sets this element parent, done interanlly in addChild
     *
     * @param parent The element's parent
     */
    protected void setParent(Element parent) {
        this.parent=parent;
    }

    /**
     * Returns this Element's parent
     *
     * @return this Element's parent
     */
    public Element getParent() {
        return parent;
    }


    /**
     Returns the number of this Element's children
     *
     * @return the number of this Element's children
     */
    public int getNumChildren() {
        if (children==null) {
            return 0;
        }
        return children.size();
    }

    /**
     * Returns the internal children vector
     *
     * @return the children vector
     */
    protected Vector getChildren() {
        return children;
    }

    /**
     * Sets the children vector of this Element
     *
     * @param children The vector to set as this Element's children
     */
    protected void setChildren(Vector children) {
        this.children=children;
    }

    /**
     * Sets the name or text (for text elements) of this tag
     *
     * @param name The name or text of this tag
     */
    protected void setTagName(String name) {
        this.name=name;
    }

    /**
     * Sets this element as a text element 
     * 
     * @param textElement true to set this as a text element, false otherwise
     */
    protected void setTextElement(boolean textElement) {
        this.textElement = textElement;
    }





    /**
     * Returns the Element's child positioned at the specified index
     *
     * @param index The requested index
     * @return child number index of this ELement
     * @throws ArrayIndexOutOfBoundsException if the index is bigger than the children's count or smaller than 0
     */
    public Element getChildAt(int index) {
        if ((index<0) || (children==null) || (index>=children.size())) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (Element)children.elementAt(index);

    }

    /**
     * Returns an Element's child by a tag name
     *
     * @param name The child's tag name
     * @return the first child with the specified name, or null if not found
     */
    public Element getFirstChildByTagName(String name) {
        if (children==null) {
            return null;
        }
        int i=0;
        Element found=null;
        while ((found==null) && (i<children.size())) {
            Element child=(Element)children.elementAt(i);
            if ((!child.textElement) && (child.getTagName().equalsIgnoreCase(name))) {
                found=child;
            } else {
                i++;
            }
        }
        return found;
    }

    /**
     * Returns the element with the specified ID
     * 
     * @param id The id to find
     * @return An element with the id, or null if none found
     */
    public Element getElementById(String id) {
        String thisId = getAttribute("id");
        if ((thisId!=null) && (thisId.equals(id))) {
            return this;
        }
        if (children!=null) {
            int i=0;
            while (i<children.size()) {
                Element child=(Element)children.elementAt(i);
                Element match=child.getElementById(id);
                if (match!=null) {
                    return match;
                }
                i++;
            }

        }
        return null;
    }

//    private void getElementByIdInternal() {
//
//    }

    private void getDescendantsByTagNameInternal(Vector v,String name,int depth) {
        if (children!=null) {
            int i=0;
            while (i<children.size()) {
                Element child=(Element)children.elementAt(i);
                if (depth>1) {
                    child.getDescendantsByTagNameInternal(v, name,depth-1);
                }
                if ((!child.textElement) && (child.getTagName().equalsIgnoreCase(name))) {
                    v.addElement(child);
                }
                i++;
            }
        }

    }

    private void getDescendantsByTagNameAndAttributeInternal(Vector v,String name,String attribute, int depth) {
        if (children!=null) {
            int i=0;
            while (i<children.size()) {
                Element child=(Element)children.elementAt(i);
                if (depth>1) {
                    child.getDescendantsByTagNameAndAttributeInternal(v, name, attribute, depth-1);
                }
                if ((!child.textElement) && (child.getTagName().equalsIgnoreCase(name))) {
                    String a = child.getAttribute(attribute);
                    if(a != null && a.length() > 0) {
                        v.addElement(child);
                    }
                }
                i++;
            }
        }

    }

    /**
     *  Returns all descendants with the specified tag name and the none empty attribute
     * 
     * @param name The tag name to look for
     * @param attributeName the attribute that must exist on the tag
     * @param depth The search depth (1 - children, 2 - grandchildren .... DEPTH_INFINITE - for all descendants)
     *
     * @return A vector containing descendants with the specified tag name
     */
    public Vector getDescendantsByTagNameAndAttribute(String name, String attributeName, int depth) {
        if (depth<1) {
            throw new IllegalArgumentException("Depth must be 1 or higher");
        }
        if (children==null) {
            return null;
        }
        Vector v=new Vector();
        getDescendantsByTagNameAndAttributeInternal(v, name, attributeName, depth);
        return v;
    }
    
    /**
     *  Returns all descendants with the specified tag name
     * 
     * @param name The tag name to look for
     * @param depth The search depth (1 - children, 2 - grandchildren .... DEPTH_INFINITE - for all descendants)
     *
     * @return A vector containing descendants with the specified tag name
     */
    public Vector getDescendantsByTagName(String name,int depth) {
        if (depth<1) {
            throw new IllegalArgumentException("Depth must be 1 or higher");
        }
        if (children==null) {
            return null;
        }
        Vector v=new Vector();
        getDescendantsByTagNameInternal(v, name, depth);
        return v;
    }

    /**
     *  Returns all descendants with the specified tag name
     *
     * @param name The tag name to look for
     * @return A vector containing descendants with the specified tag name
     */
    public Vector getDescendantsByTagName(String name) {
        return getDescendantsByTagName(name, DEPTH_INFINITE);
    }


    /**
     * Returns all children with the specified tag name
     * 
     * @param name The tag name to look for
     * @return A vector containing children with the specified tag name
     */
    public Vector getChildrenByTagName(String name) {
        return getDescendantsByTagName(name, 1);
    }

    private void getTextDescendantsInternal(Vector v,String text,boolean caseSensitive,int depth) {
        if (children==null) {
            return;
        }
        int i=0;
        while (i<children.size()) {
            Element child=(Element)children.elementAt(i);
            if (depth>0) {
                child.getTextDescendantsInternal(v, text, caseSensitive, depth-1);
            }
            if (child.textElement) {
                if (text!=null) {
                    String childText=child.getText();
                    if (!caseSensitive) {
                        childText=childText.toLowerCase();
                    }
                    int index=childText.indexOf(text);
                    if (index!=-1) {
                        v.addElement(child);
                    }
                } else { // if text==null, it means we want all text children
                    v.addElement(child);
                }
            }
             i++;
        }
    }


    /**
     *  Returns all text descendants containing the specified text
     *
     * @param text The text to look for (null to return all text children)
     * @param caseSensitive true to perform a case sensitive match, false to ignore case
     * @param depth The search depth (1 - children, 2 - grandchildren .... DEPTH_INFINITE - for all descendants)
     * @return A vector containing descendants containing the specified text
     */
    public Vector getTextDescendants(String text,boolean caseSensitive,int depth) {
        if (depth<1) {
            throw new IllegalArgumentException("Depth must be 1 or higher");
        }
        if (children==null) {
            return null;
        }
        if ((!caseSensitive) && (text!=null)) {
            text=text.toLowerCase();
        }
        Vector v=new Vector();
        getTextDescendantsInternal(v,text,caseSensitive,depth);
        return v;
    }

    /**
     *  Returns all text descendants containing the specified text
     *
     * @param text The text to look for (null to return all text children)
     * @param caseSensitive true to perform a case sensitive match, false to ignore case
     * @return A vector containing decensants containing the specified text
     */
    public Vector getTextDescendants(String text,boolean caseSensitive) {
        return getTextDescendants(text, caseSensitive, DEPTH_INFINITE);
    }

    /**
     * Returns all children with the specified text
     *
     * @param text The text to look for (null to return all text children)
     * @param caseSensitive true to perform a case sensitive match, false to ignore case
     * @return A vector containing children containing the specified text
     */
    public Vector getTextChildren(String text,boolean caseSensitive) {
        return getTextDescendants(text, caseSensitive, 1);
    }

    /**
     * Returns true if the specified element is contained in this element's hierarchy (meaning it is one of its descendants)
     * 
     * @param element The element to look for
     * @return true if this element contains the specified element, false otherwise
     */
    public boolean contains(Element element) {
        if (this==element) {
            return true;
        }
        if (children!=null) {
            int i=0;
            while (i<children.size()) {
                Element child=(Element)children.elementAt(i);
                if (child.contains(element)) {
                    return true;
                }
                i++;
            }

        }
        return false;
    }



    /**
     * Adds the specified attribute and value to this Element if it is supported for the Element and has a valid value.
     *
     * @param attribute The attribute's name
     * @param value The attribute's value
     *
     * @return a positive error code or -1 if attribute is supported and valid
     */
    public int setAttribute(String attribute,String value) {
        if (textElement) {
            throw new IllegalStateException("Text elements cannot have attributes");
        }

        setAttribute((Object)attribute, value);
        /*if (attributes==null) {
            attributes=new Hashtable();
        }
        attributes.put(attribute, value);*/
        return -1;
    }

    /**
     * Removes the specified attribute
     * 
     * @param attribute The attribute to remove
     */
    public void removeAttribute(String attribute) {
        removeAttribute((Object)attribute);
    }


    /**
     * Removes the specified attribute if it exist in this Element
     * This method allows creating a key that is non-string to be used by subclasses that optimize attributes retrieval
     *
     * @param id The attribute ID
     */
    protected void removeAttribute(Object id) {
        if (attributes!=null) {
            attributes.remove(id);
            if (attributes.isEmpty()) {
                attributes=null;
            }
        }
    }


    /**
     * Returns the attribute value by its name (or null if it wasn't defined for this element)
     * 
     * @param name The attribute id
     * @return the attribute value by its name (or null if it wasn't defined for this element)
     */
    public String getAttribute(String  name) {
        if (attributes==null) {
            return null;
        }
        return (String)attributes.get(name);
    }

    private void setChildParent(Element child) {
        if (textElement) {
            throw new IllegalStateException("Text elements cannot have children");
        }

        if (child.getParent()!=null) {
            throw new IllegalStateException("An Element can't have two parents.");
        }
        if (children==null) {
            children=new Vector();
        }
        child.setParent(this);
    }

    /**
     * Removes the child at the given index
     * 
     * @param index The child's index
     */
    public void removeChildAt(int index) {
        if ((index<0) || (children==null) || (index>=children.size())) {
            throw new ArrayIndexOutOfBoundsException();
        }
        Element child=(Element)children.elementAt(index);
        child.setParent(null);
        children.removeElementAt(index);
    }

    /**
     * Returns the child index
     *  
     * @param child The child element to look for
     * @return The child position, or -1 if the child does not belong to this element.
     */
    public int getChildIndex(Element child) {
        int result=-1;
        if (children!=null) {
            for(int i=0;i<children.size();i++) {
                if (child==children.elementAt(i)) {
                    result=i;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Inserts the given child at the specified index
     * 
     * @param child The child to insert
     * @param index The index to insert it at
     */
    public void insertChildAt(Element child,int index) {
        setChildParent(child);
        children.insertElementAt(child, index);
    }

    /**
     * Replaces one child with another
     * 
     * @param oldChild The child to replace (Must belong to this element, otherwise a call to this method will have no effect)
     * @param newChild The child to replace it with
     */
    public void replaceChild(Element oldChild,Element newChild) {
        if (children!=null) {
            setChildParent(newChild);
            int index=children.indexOf(oldChild);
            if (index!=-1) {
                children.insertElementAt(newChild, index);
                removeChildAt(index+1);
//                children.removeElement(oldChild);
//                oldChild.setParent(null);
                return;
            }
        }
        throw new IllegalArgumentException("The oldChild element specified must be this element's child");
    }

    /**
     * Returns the text of this element (for text elements only)
     * 
     * @return the text of this element (for text elements only)
     * @throws IllegalStateException if this is not a text element
     */
    public String getText() {
        if (!textElement) {
            throw new IllegalStateException("Only text elements can get text");
        }
        return name;
    }

    /**
     * Sets the text of this element to the specified string (For text elements only)
     * 
     * @param str The text to set
     * @throws IllegalStateException if this is not a text element
     */
    public void setText(String str) {
        if (!textElement) {
            throw new IllegalStateException("Only text elements can set text");
        }
        name=str;

    }

    /**
     * Returns a printable string representing this element 
     * 
     * @return a printable string representing this element
     */
    public String toString() {
        return toString("");
    }

    /**
     * A recursive method for creating a printout of a full tag with its entire hierarchy.
     * This is used by the public method toString().
     *
     * @param spacing Increased by one in each recursion phase to provide with indentation
     * @return the printout of this tag
     */
    private String toString(String spacing) {

        String str=spacing;
        if (!textElement) {
            str+="<"+getTagName();
            if (attributes!=null) {
                for(Enumeration e=attributes.keys();e.hasMoreElements();) {
                    String attrStr=(String)e.nextElement();
                    String val=(String)attributes.get(attrStr);
                    str+=" "+attrStr+"='"+val+"'";
                }
            }
            str+=">\n";
            if (children!=null) {
                for(int i=0;i<children.size();i++) {
                    str+=((Element)children.elementAt(i)).toString(spacing+' ');
                }
            }
            str+=spacing+"</"+getTagName()+">\n";
        } else {
            str+="'"+name+"'\n";
        }
        return str;
   }

    /**
     * Determines whether or not this Element has any text children.
     * @return true if any of this Elements children are text Elements.
     */
    public boolean hasTextChild() {
        if(children != null) {
            for (int iter = 0 ; iter < children.size() ; iter++) {
                Object child = children.elementAt(iter);
                if (child instanceof Element && ((Element)child).isTextElement()) {
                    return true;
                }
            }
        }
        return false;
    }
   
    /**
     * Determines whether or not this Element has no children.
     * @return true if this Element has no children.
     */
    public boolean isEmpty() {
        return children == null || children.isEmpty();
    }
}
