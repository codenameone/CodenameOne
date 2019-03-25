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

package com.codename1.properties;

import com.codename1.io.Util;
import com.codename1.xml.Element;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * An XML element mapping to a property
 *
 * @author shai
 */
class PropertyXMLElement extends Element {
    private PropertyIndex parent;
    private int index = -1;
    private PropertyXMLElement parentElement;
    
    PropertyXMLElement(PropertyIndex parent) {
        this.parent = parent;
    }

    PropertyXMLElement(PropertyXMLElement parentElement, PropertyIndex parent) {
        this.parent = parent;
        this.parentElement = parentElement;
    }
    
    @Override
    public void addChild(Element childElement) {
        throw new IllegalStateException();
    }

    @Override
    public boolean contains(Element element) {
        if (this==element) {
            return true;
        }
        Vector children = getChildren();
        if (children != null) {
            int i=0;
            while (i<children.size()) {
                Element child=(Element)children.get(i);
                if (child.contains(element)) {
                    return true;
                }
                i++;
            }
        }
        return false;
    }

    @Override
    public String getAttribute(String name) {
        Object o = parent.get(name);
        if(o == null) return null;
        return o.toString();
    }

    @Override
    public int getAttributeAsInt(String name, int def) {
        Object o = parent.get(name);
        if(o == null) {
            return def;
        }
        return Util.toIntValue(o);
    }

    @Override
    public Hashtable getAttributes() {
        Hashtable h = new Hashtable();
        for(PropertyBase b : parent) {
            if(b.getGenericType() != null && b.getGenericType().isAssignableFrom(PropertyBusinessObject.class) || b instanceof CollectionProperty || b instanceof MapProperty) {
                continue;
            }
            Object o = b.get();
            if(o != null) {
                h.put(b.getName(), o.toString());
            }
        }
        return super.getAttributes();
    }

    @Override
    public Element getChildAt(int index) {
        int current = -1;
        PropertyBase text = parent.getXmlTextElement();
        for(PropertyBase b : parent) {
            if(b == text) {
                current ++;
                if(current == index) {
                    Element e = new Element(b.getName(), true);
                    if(b.get() != null) {
                        e.setText(b.get().toString());
                    }
                    return e;
                }
            }
            if(b.getGenericType() != null && b.getGenericType().isAssignableFrom(PropertyBusinessObject.class)) {
                Object o = b.get();
                if(o != null) {
                    current++;
                    if(current == index) {
                        PropertyXMLElement i = new PropertyXMLElement(this, ((PropertyBusinessObject)o).getPropertyIndex());
                        i.index = current;
                        return i;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public int getChildIndex(Element child) {
        return ((PropertyXMLElement)child).index;
    }

    @Override
    protected Vector getChildren() {
        Vector v = new Vector();

        int n = getNumChildren();
        for(int iter = 0 ; iter < n ; iter++) {
            v.add(getChildAt(iter));
        }
        
        return v;
    }

    @Override
    public Vector getChildrenByTagName(String name) {
        Vector v = new Vector();
        PropertyBase p = parent.get(name);
        if(p != null) {
            Object o = p.get();
            if(o != null) {
                v.add(new PropertyXMLElement(this, ((PropertyBusinessObject)o).getPropertyIndex()));
            }
        }
        return v;
    }

    @Override
    public Vector getDescendantsByTagName(String name) {
        throw new RuntimeException();
    }

    @Override
    public Vector getDescendantsByTagName(String name, int depth) {
        throw new RuntimeException();
    }

    @Override
    public Vector getDescendantsByTagNameAndAttribute(String name,
        String attributeName, int depth) {
        throw new RuntimeException();
    }

    @Override
    public Element getElementById(String id) {
        String thisId = getAttribute("id");
        if ((thisId!=null) && (thisId.equals(id))) {
            return this;
        }
        Vector children = getChildren();
        if (children!=null) {
            int i=0;
            while (i<children.size()) {
                Element child=(Element)children.get(i);
                Element match=child.getElementById(id);
                if (match!=null) {
                    return match;
                }
                i++;
            }

        }
        return null;
    }

    @Override
    public Element getFirstChildByTagName(String name) {
        PropertyBase p = parent.get(name);
        if(p != null) {
            Object o = p.get();
            if(o != null) {
                return new PropertyXMLElement(this, ((PropertyBusinessObject)o).getPropertyIndex());
            }
        }
        return null;
    }

    @Override
    public int getNumChildren() {
        int current = 0;
        for(PropertyBase b : parent) {
            if(b.getGenericType() != null && b.getGenericType().isAssignableFrom(PropertyBusinessObject.class)) {
                Object o = b.get();
                if(o != null) {
                    current++;
                }
            }
        }
        PropertyBase text = parent.getXmlTextElement();
        if(text != null) {
            return current + 1;
        }
        return current;
    }

    @Override
    public Element getParent() {
        return parentElement;
    }

    @Override
    public String getTagName() {
        return parent.getName();
    }

    @Override
    public String getText() {
        Object o = parent.getXmlTextElement().get();
        return o == null ? null : o.toString();
    }

    @Override
    public Vector getTextChildren(String text, boolean caseSensitive) {
        throw new RuntimeException();
    }

    @Override
    public Vector getTextDescendants(String text,
        boolean caseSensitive) {
        throw new RuntimeException();
    }

    @Override
    public Vector getTextDescendants(String text,
        boolean caseSensitive, int depth) {
        throw new RuntimeException();
    }

    @Override
    public boolean hasTextChild() {
        return parent.getXmlTextElement() != null && parent.getXmlTextElement().get() != null;
    }

    @Override
    public void insertChildAt(Element child, int index) {
        throw new RuntimeException();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    @Override
    public boolean isTextElement() {
        return false;
    }

    @Override
    protected void removeAttribute(Object id) {
        throw new RuntimeException();        
    }

    @Override
    public void removeAttribute(String attribute) {
        throw new RuntimeException();
    }

    @Override
    public Iterator<Element> iterator() {
        final int num = getNumChildren();
        return new Iterator<Element>() {
            private int offset = 0;
          
            @Override
            public boolean hasNext() {
                return offset < num;
            }

            @Override
            public Element next() {
                offset++;
                return getChildAt(index);
            }
            
            @Override
            public void remove() {
            }
        };
    }

    @Override
    public void removeChildAt(int index) {
        throw new RuntimeException();
    }

    @Override
    public void replaceChild(Element oldChild, Element newChild) {
        throw new RuntimeException();
    }

    @Override
    protected void setAttribute(Object id, String value) {
        throw new RuntimeException();
    }

    @Override
    public int setAttribute(String attribute, String value) {
        PropertyBase pb = parent.get(attribute);
        if(parent.setSimpleObject(pb, value)) {
            return -1;
        } 
        return 1;
    }

    @Override
    protected void setChildren(Vector children) {
        throw new RuntimeException();
    }

    @Override
    protected void setParent(Element parent) {
    }

    @Override
    protected void setTagName(String name) {
    }

    @Override
    public void setText(String str) {
    }

    @Override
    protected void setTextElement(boolean textElement) {
    }

    @Override
    public String toString() {
        return toString("");
    }

    @Override
    public String toString(String spacing) {

        String str=spacing;
        str+="<"+getTagName();
        Hashtable attributes = getAttributes();
        if (attributes!=null) {
            for(Enumeration e=attributes.keys();e.hasMoreElements();) {
                String attrStr=(String)e.nextElement();
                String val=(String)attributes.get(attrStr);
                str+=" "+attrStr+"='"+val+"'";
            }
        }
        str+=">\n";
        Vector children = getChildren();
        if (children!=null) {
            for(int i=0;i<children.size();i++) {
                str+=((Element)children.get(i)).toString(spacing+' ');
            }
        }
        str+=spacing+"</"+getTagName()+">\n";
        return str;
   }
}
