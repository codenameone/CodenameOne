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
package com.codename1.ui.html;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class is responsible for applying CSS directives to an HTMLComponent
 *
 * @author Ofir Leitner
 */
class CSSEngine {

    private static int DEFAULT_3D_BORDER_COLOR = 0x9a9a9a; // default color for outset/inset/ridge/groove

    //int count; //for debugging
    private static CSSEngine instance; // The instance of this singleton class
    private static Hashtable specialKeys; // A hashtable containing all recognized special key strings and their keycodes
    private Hashtable matchingFonts = new Hashtable(); // A hashtable used as a cache for quick find of matching fonts

    /**
     * A list of the attributes that can contain a URL, in order to scan them and update relative URLs to an absolute one
     */
    private static final int[] URL_ATTRIBUTES = {CSSElement.CSS_BACKGROUND_IMAGE,CSSElement.CSS_LIST_STYLE_IMAGE};

    /**
     * Denotes that the selector should be applied to the unselected style of the component
     */
    final static int STYLE_UNSELECTED=1;

    /**
     * Denotes that the selector should be applied to the selected style of the component
     */
    final static int STYLE_SELECTED=2;

    /**
     * Denotes that the selector should be applied to the pressed style of the component
     */
    final static int STYLE_PRESSED=4;

    /**
     * The indentation applied on a list when its 'list-style-position' is 'inside' vs. 'outside'
     */
    private final static int INDENT_LIST_STYLE_POSITION = 15;

    static final String CLIENT_PROPERTY_CSS_CONTENT = "cssContent";

    // The possible values of the 'text-transform' attribute
    private static final int TEXT_TRANSFORM_NONE = 0;
    private static final int TEXT_TRANSFORM_UPPERCASE = 1;
    private static final int TEXT_TRANSFORM_LOWERCASE = 2;
    private static final int TEXT_TRANSFORM_CAPITALIZE = 3;

    // The possible values of the 'text-decoration' attribute
    private static final int TEXT_DECOR_UNDERLINE = 0;
    private static final int TEXT_DECOR_LINETHROUGH = 1;
    private static final int TEXT_DECOR_NONE = 2;
    private static final int TEXT_DECOR_OVERLINE = 3;

    // The possible values of the '-wap-input-required' attribute
    private static final int INPUT_REQUIRED_TRUE = 0;
    private static final int INPUT_REQUIRED_FALSE = 1;

    // The possible values of the 'background-attachment' attribute
    private static final int BG_ATTACHMENT_FIXED = 0;
    private static final int BG_ATTACHMENT_SCROLL = 1;

    // The possible values of the 'white-space' attribute
    private static final int WHITE_SPACE_NORMAL = 0;
    private static final int WHITE_SPACE_PRE = 1;
    private static final int WHITE_SPACE_NOWRAP = 2;

    // The possible values of the 'display' attribute
    private static final int DISPLAY_INLINE=0;
    private static final int DISPLAY_BLOCK=1;
    private static final int DISPLAY_LIST_ITEM=2;
    private static final int DISPLAY_NONE=3;
    private static final int DISPLAY_MARQUEE=4;

    // The possible values of the 'font-variant' attribute
    private static final int FONT_VARIANT_NORMAL=0;
    private static final int FONT_VARIANT_SMALLCAPS=1;

    // The possible values of the 'list-style-position' attribute
    private static final int LIST_STYLE_POSITION_INSIDE = 0;
    private static final int LIST_STYLE_POSITION_OUTSIDE = 1;

    // The possible values of the 'border-style' attribute
    private static final int BORDER_STYLE_NONE = 0;
    private static final int BORDER_STYLE_SOLID = 1;
    private static final int BORDER_STYLE_DOTTED = 2;
    private static final int BORDER_STYLE_DASHED = 3;
    private static final int BORDER_STYLE_DOUBLE = 4;
    private static final int BORDER_STYLE_GROOVE = 5;
    private static final int BORDER_STYLE_RIDGE = 6;
    private static final int BORDER_STYLE_INSET = 7;
    private static final int BORDER_STYLE_OUTSET = 8;

    private static final int[][] BORDER_OUTLINE_PROPERTIES = {
        {CSSElement.CSS_BORDER_TOP_WIDTH,CSSElement.CSS_BORDER_TOP_STYLE,CSSElement.CSS_BORDER_TOP_COLOR},
        {CSSElement.CSS_OUTLINE_WIDTH,CSSElement.CSS_OUTLINE_STYLE,CSSElement.CSS_OUTLINE_COLOR}
    };

    private static final int BORDER = 0;
    private static final int OUTLINE = 1;

    private static final int WIDTH = 0;
    private static final int STYLE = 1;
    private static final int COLOR = 2;


    // The possible values of the 'visibility' attribute
    private static final int VISIBILITY_HIDDEN=0;
    private static final int VISIBILITY_VISIBLE=1;
    private static final int VISIBILITY_COLLAPSE=2; // collapse behaves the same as hidden in most browsers.

    // The possible values of the 'border-collapse' attribute
    private static final int BORDER_COLLAPSE_COLLAPSE = 0;
    private static final int BORDER_COLLAPSE_SEPARATE = 1;

    // The possible values of the 'empty-cells' attribute
    private static final int EMPTY_CELLS_HIDE = 0;
    private static final int EMPTY_CELLS_SHOW = 1;

    // The possible values of the 'caption-side' attribute
    private static final int CAPTION_SIDE_BOTTOM = 0;
    private static final int CAPTION_SIDE_TOP = 1;

    // The possible values of the 'direction' attribute
    private static final int DIRECTION_RTL = 0;
    private static final int DIRECTION_LTR = 1;
    
    /**
     * Returns the singleton instance of CSSEngine and creates it if necessary
     *
     * @return The singleton instance of CSSEngine
     */
    static CSSEngine getInstance() {
        if (instance==null) {
            instance=new CSSEngine();
        }
        return instance;
    }

    /**
     * Adds support for a special key to be used as an accesskey.
     * The CSS property -wap-accesskey supports special keys, for example "phone-send" that may have different key codes per device.
     * This method allows pairing between such keys to their respective key codes.
     * Note that these keys are valid only for -wap-aceesskey in CSS files, and not for the XHTML accesskey attribute.
     *
     * @param specialKeyName The name of the special key as denoted in CSS files
     * @param specialKeyCode The special key code
     */
    static void addSpecialKey(String specialKeyName,int specialKeyCode) {
        if (specialKeys==null) {
            specialKeys=new Hashtable();
        }
        specialKeys.put(specialKeyName,new Integer(specialKeyCode));
    }

    /**
     * Sorts the CSS directives by their specificity level
     * 
     * @param css A css vector holding CSSElements, where each element holds CSS selectors as its children
     * @return a flat vector containing CSS selectors, sorted by specificity
     */
    private CSSElement[] sortSelectorsBySpecificity(CSSElement[] css) {
        Vector sortedSelectors=new Vector();

        for(int s=0;s<css.length;s++) {
            CSSElement cssRoot=css[s];
            String cssPageURL=cssRoot.getAttributeById(CSSElement.CSS_PAGEURL);
            DocumentInfo cssDocInfo=null;
            if (cssPageURL!=null) {
                cssDocInfo=new DocumentInfo(cssPageURL);
            }
            for(int iter = 0 ; iter < cssRoot.getNumChildren() ; iter++) {
                CSSElement currentSelector = cssRoot.getCSSChildAt(iter);
                if (cssPageURL!=null) { // Since with external CSS pages, the base URL is that of the CSS file and not of the HTML document, we have to convert relative image URLs to absolute URLs
                    for(int i=0;i<URL_ATTRIBUTES.length;i++) {
                        String imageURL=getCSSUrl(currentSelector.getAttributeById(URL_ATTRIBUTES[i]));
                        if (imageURL!=null) {
                            imageURL=cssDocInfo.convertURL(imageURL);
                            currentSelector.setAttribute(currentSelector.getAttributeName(new Integer(URL_ATTRIBUTES[i])), "url("+imageURL+")");
                        }
                    }
                }
                int i=0;
                int specificity=currentSelector.getSelectorSpecificity(); //Note that it is important to get the specificity outside the loop, so it will necessarily get called (triggering the cal)
                while((i<sortedSelectors.size()) && (specificity>=((CSSElement)sortedSelectors.elementAt(i)).getSelectorSpecificity())) {
                    i++;
                }
                sortedSelectors.insertElementAt(currentSelector, i);
            }
        }

        css = new CSSElement[sortedSelectors.size()];
        for(int i=0;i<sortedSelectors.size();i++) {
            css[i]=(CSSElement)sortedSelectors.elementAt(i);
        }

        return css;
    }

    /**
     * Applies all CSS directives to the given document and HTMLComponent, including external CSS files, embedded CSS segments and inline CSS (Style attribute)
     * This is called by HTMLComponent after the document was fully parsed and all external CSS have been retrieved.
     * This method actually initializes a sorted CSS array to be used by the recursive private applyCSS method.
     *
     * @param document The HTML document to apply the CSS on
     * @param htmlC The HTMLComponent to apply the CSS on
     * @param externalCSS A vector containing CSSElelemnts each being the root of external CSS file (1 per file)
     * @param embeddedCSS A vector containing CSSElelemnts each being the root of embedded CSS segments (1 per segment)
     */
    void applyCSS(HTMLElement document,HTMLComponent htmlC,Vector externalCSS,Vector embeddedCSS) {
        //long startTime=System.currentTimeMillis();
        //count=0;
        int externalSize=0;
        int embeddedSize=0;
        
        if (externalCSS!=null) {
            externalSize=externalCSS.size();
        }
        if (embeddedCSS!=null) {
            embeddedSize=embeddedCSS.size();
        }

        if (externalSize+embeddedSize==0) {
            applyStyleAttributeRecursive(document, htmlC);
        } else {
            CSSElement[] css = new CSSElement[externalSize+embeddedSize];
            for(int i=0;i<externalSize;i++) {
                css[i]=(CSSElement)externalCSS.elementAt(i);
            }
            for(int i=0;i<embeddedSize;i++) {
                css[i+externalSize]=(CSSElement)embeddedCSS.elementAt(i);
            }

            css=sortSelectorsBySpecificity(css);
            applyCSS(document, htmlC, css, null, null);
        }
        //System.out.println("Total: "+count+", Time="+(System.currentTimeMillis()-startTime));
    }

    /**
     * Applies the style attribute in the specified element and all of its descendants (where exists)
     * This method is used when no external and embedded CSS segments exists, to speed up the process of CSS application
     *
     * @param element The element to apply the style to
     * @param htmlC The HTMLComponent
     */
    private void applyStyleAttributeRecursive(HTMLElement element,HTMLComponent htmlC) {
        applyStyleAttribute(element, htmlC);
        for(int i=0;i<element.getNumChildren();i++) {
            HTMLElement child=(HTMLElement)element.getChildAt(i);
            applyStyleAttributeRecursive(child, htmlC);
        }
    }

    /**
     * Applies the style attribute in the specified element, if exists
     * 
     * @param element The element to apply the style to
     * @param htmlC The HTMLComponent
     */
    private void applyStyleAttribute(HTMLElement element,HTMLComponent htmlC) {
        String styleStr=element.getAttributeById(HTMLElement.ATTR_STYLE);
        if (styleStr!=null) {
            CSSElement style=null;
            styleStr="{"+styleStr+"}"; // So it will be parsed correctly
            try {
                style = CSSParser.getInstance().parseCSS(new InputStreamReader(new ByteArrayInputStream(styleStr.getBytes())),htmlC);
                applyStyle(element, style, htmlC);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * A recursive method that tries to match all CSS selectors with the specified element
     *
     * @param element The specific element in the document to apply the CSS on
     * @param htmlC The HTMLComponent to apply the CSS on
     * @param css An array containing selectors sorted by specificity from all the external CSS files and then the embedded CSS segments
     * @param nestedSelectors A vector containing nested selectors, or null if none
     */
    private Vector applyCSS(HTMLElement element,HTMLComponent htmlC,CSSElement[] css,Vector nestedSelectors,Vector siblingSelectors) { //Vector styleAttributes
        String id=element.getAttributeById(HTMLElement.ATTR_ID);
        String className=element.getAttributeById(HTMLElement.ATTR_CLASS);

        Vector nextNestedSelectors=new Vector();
        Vector nextSiblingSelectors=null;
        if (!HTMLComponent.PROCESS_HTML_MP1_ONLY) { // sibling selectors are not supported in HTML-MP1
            nextSiblingSelectors=new Vector();
        }
        for (int e=0;e<css.length;e++) {
            CSSElement currentSelector = css[e];
            checkSelector(currentSelector, element, htmlC, className, id,nextNestedSelectors,nextSiblingSelectors);
        }

        if (nestedSelectors!=null) {
            for (Enumeration e=nestedSelectors.elements();e.hasMoreElements();) {
                CSSElement currentSelector=(CSSElement)e.nextElement();
                checkSelector(currentSelector, element, htmlC, className, id,nextNestedSelectors,nextSiblingSelectors);
            }
        }

        if ((!HTMLComponent.PROCESS_HTML_MP1_ONLY) && (siblingSelectors!=null)) {
            for (Enumeration e=siblingSelectors.elements();e.hasMoreElements();) {
                CSSElement currentSelector=(CSSElement)e.nextElement();
                checkSelector(currentSelector, element, htmlC, className, id,nextNestedSelectors,nextSiblingSelectors);
            }
        }


        if (nextNestedSelectors.size()==0) {
            nextNestedSelectors=null;
        }

        if ((!HTMLComponent.PROCESS_HTML_MP1_ONLY) && (nextSiblingSelectors.size()==0)) {
            nextSiblingSelectors=null;
        }

        applyStyleAttribute(element, htmlC);

        Vector curSiblingSelectors=null;
        for(int i=0;i<element.getNumChildren();i++) {
            HTMLElement child=(HTMLElement)element.getChildAt(i);
            Vector v=applyCSS(child, htmlC,css,nextNestedSelectors,curSiblingSelectors);
            if (!child.isTextElement()) { // Sibling selectors skip text elements
                curSiblingSelectors=v;
            }
        }

        return nextSiblingSelectors;
    }
    
    /**
     * Checks if the given selector matches the given element either by its tag name, class name or id.
     * If there's a match but the selector has children, it means that it is a nested selector, and thus 
     * its only child is added to the nested selectors vector to be checked against the children of this element in the next recursion of applyCSS
     * 
     * @param currentSelector The current CSS selector to check
     * @param element The element to check
     * @param htmlC The HTMLComponent
     * @param className The element's class name (Can be derived from element but since this method is called a lot it is extracted before and sent as a parameter)
     * @param id The element's id (Same comment as in className)
     * @param nextNestedSelectors A vector containing the nested selectors
     */
    private void checkSelector(CSSElement currentSelector,HTMLElement element,HTMLComponent htmlC,String className,String id,Vector nextNestedSelectors,Vector nextSiblingSelectors) {

        if (((currentSelector.getSelectorTag()==null) || ((!element.isTextElement()) && (currentSelector.getSelectorTag().equalsIgnoreCase(element.getTagName())))) &&
            ((currentSelector.getSelectorClass()==null) || (containsClass(className,currentSelector.getSelectorClass()))) &&
            ((currentSelector.getSelectorId()==null) || (currentSelector.getSelectorId().equalsIgnoreCase(id))) &&
            (((currentSelector.getSelectorPseudoClass() & (CSSElement.PC_FIRST_CHILD))==0) || (element.isFirstChild())) && //element.getParent().getChildIndex(element)==0)) &&
            (currentSelector.matchAttributeSelections(element))) {
                if (currentSelector.getNumChildren()==0) {
                    if ((element.getTagId()!=HTMLElement.TAG_A) ||
                        ((currentSelector.getSelectorPseudoClass() & (CSSElement.PC_LINK+CSSElement.PC_VISITED))==0) || // not link/visited (but can be active/focus)
                        ((element.getUi().size()>0) && !(element.getUi().firstElement() instanceof HTMLLink)) ||
                        ((element.getUi().size()>0) && (!((HTMLLink)element.getUi().firstElement()).linkVisited) && ((currentSelector.getSelectorPseudoClass() & CSSElement.PC_LINK)!=0)) ||
                        ((element.getUi().size()>0) && ((HTMLLink)element.getUi().firstElement()).linkVisited) && ((currentSelector.getSelectorPseudoClass() & CSSElement.PC_VISITED)!=0)) {
                        applyStyle(element, currentSelector,htmlC);
                    }
                } else {
                    CSSElement child=currentSelector.getCSSChildAt(0);
                    if (child.siblingSelector) {
                        if (!HTMLComponent.PROCESS_HTML_MP1_ONLY) { // sibling selectors are not supported in HTML-MP1
                            nextSiblingSelectors.addElement(child);
                        }
                    } else {
                        nextNestedSelectors.addElement(child);
                        // Check if this is a Descendant selector (i.e. div b - which means match any b that is the descendant of div
                        // If so then we pass not only the child selector (i.e. the b) but also the "* b" to allow matching later decendants
                        if (child.descendantSelector) {
                            CSSElement elem=new CSSElement("*");
                            elem.addChild(new CSSElement(child));
                            nextNestedSelectors.addElement(elem);
                        }
                    }
                }
        }
    }

    /**
     * Checks if the specified class is contained in the specified text
     * This is used for elements that have several classes i.e. class="class1 class2"
     * Note: A simple indexOf could not be used since we need to find whole words and not frgaments of words
     *
     * @param selectorClass The text
     * @param elementClass The word to find in the text
     * @return true if the word is found, false otherwise
     */
    private boolean containsClass(String elementClass,String selectorClass) {
        if ((elementClass==null) || (selectorClass==null)) {
            return false;
        }
        // The spaces addition is to make sure we get a whole word and not a fragment of a word
        elementClass=" "+elementClass+" ";
        
        // Selector can require multiple classes, i.e. class.1class2 (which needs to match to "class 1 class2" and "class2 class1" and also "class1 otherclasses class2"
        int dotIndex=selectorClass.indexOf('.');
        while (dotIndex!=-1) {
            String curWord=selectorClass.substring(0, dotIndex);
            if (elementClass.indexOf(" "+curWord+" ")==-1) {
                return false;
            }
            selectorClass=selectorClass.substring(dotIndex+1);
            dotIndex=selectorClass.indexOf('.');
        }

        return (elementClass.indexOf(" "+selectorClass+" ")!=-1);
    }

    /**
     * Applies the given style attributes to the HTML DOM entry
     *
     * @param element The element to apply the style to
     * @param selector The selector containing the style directives
     * @param htmlC The HTMLComponent
     */
    private void applyStyle(HTMLElement element, CSSElement selector, HTMLComponent htmlC) {
        if ((element.getUi() != null) && (element.getUi().size()>0)) {
            if (!HTMLComponent.PROCESS_HTML_MP1_ONLY) {
                String reset=selector.getAttributeById(CSSElement.CSS_COUNTER_RESET);
                if (reset!=null) {
                    htmlC.incCounter(reset, true);
                }
                String inc=selector.getAttributeById(CSSElement.CSS_COUNTER_INCREMENT);
                if (inc!=null) {
                    htmlC.incCounter(inc, false);
                }

                if ((selector.getSelectorPseudoClass() & (CSSElement.PC_BEFORE|CSSElement.PC_AFTER))!=0) {
                    handleContentProperty(element,selector,htmlC);
                    return;
                }
            }
            for(int iter = 0 ; iter < element.getUi().size() ; iter++) {
                Object o = element.getUi().elementAt(iter);
                if(o != null && o instanceof Component) {
                    final Component cmp = (Component)o;
                    applyStyleToUIElement(cmp, selector,element,htmlC);                        
                }
            }
        }
    }

    /**
     * Returns a mask of the STYLE_* constants of which CodenameOne styles this selector should be applied to
     *
     * @param cmp The component in question
     * @param selector The selector
     * @return a mask of the STYLE_* constants of which CodenameOne styles this selector should be applied to
     */
    private int getApplicableStyles(Component cmp,CSSElement selector) {
        int result=0;
        if (cmp instanceof HTMLLink) {
            int pseudoClass=selector.getSelectorPseudoClass();
            boolean done=false;
            if ((pseudoClass & CSSElement.PC_FOCUS)!=0) { // Focused (i.e. CSS focus/hover)
                result|=STYLE_SELECTED;
                done=true;
            }
            if ((pseudoClass & CSSElement.PC_ACTIVE)!=0) { // active in CSS means pressed in CodenameOne
                result|=STYLE_PRESSED;
                done=true;
            }

            if (!done) {
                result|=STYLE_SELECTED|STYLE_UNSELECTED;
            }
        } else {
                result|=STYLE_SELECTED|STYLE_UNSELECTED;
        }
        return result;
    }

    /**
     * Sets the specified color as the foreground color of the component and all its children
     * 
     * @param cmp The component to work on
     * @param color The color to set
     * @param selector The selector with the color directive
     */
    private void setColorRecursive(Component cmp,int color,CSSElement selector) {
        int styles=getApplicableStyles(cmp, selector);
        if ((styles & STYLE_UNSELECTED)!=0) {
            cmp.getUnselectedStyle().setFgColor(color);
        }
        if ((styles & STYLE_SELECTED)!=0) {
            cmp.getSelectedStyle().setFgColor(color);
        }
        if ((styles & STYLE_PRESSED)!=0) {
            ((HTMLLink)cmp).getPressedStyle().setFgColor(color);
        }

        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            for(int i=0;i<cont.getComponentCount();i++) {
                if (!(cont.getComponentAt(i) instanceof HTMLLink)) { // A link color is a special case, it is not inherited and applied only if the selector selects the link directly
                    setColorRecursive(cont.getComponentAt(i), color,selector);
                }
            }
        }
    }

    /**
     * Sets the font of the component and all its children to the closest font that can be found according to the specified properties
     * 
     * @param htmlC The HTMLComponent this component belongs to (For the available bitmap fonts table)
     * @param cmp The component to work on
     * @param fontFamily The font family
     * @param fontSize The font size in pixels
     * @param fontStyle The font style - either Font.STYLE_PLAIN or Font.STYLE_ITALIC
     * @param fontWeight The font weight - either Font.STYLE_PLAIN ot Font.STYLE_BOLD
     * @param selector The selector with the font directive
     */
    private void setFontRecursive(HTMLComponent htmlC, Component cmp,String fontFamily,int fontSize,int fontStyle,int fontWeight,CSSElement selector) {
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            for(int i=0;i<cont.getComponentCount();i++) {
                setFontRecursive(htmlC,cont.getComponentAt(i), fontFamily,fontSize,fontStyle,fontWeight,selector);
            }
        } else if (cmp instanceof Label) {
            setMatchingFont(htmlC, cmp, fontFamily, fontSize, fontStyle, fontWeight,selector);
        }
    }

    /**
     * Usually we don't have to set visibility in a recursive manner, i.e. suffices to set a top level container as invisible and all its contents are invisible.
     * However, in CSS it is possible that a top level element has visibility:hidden and some child of his has visibility:visible, and then what we do
     * is use the setVisibleParents to make sure all containers containing this child are visible.
     * But since other child components still need to be invsibile - we make sure that all are invisible with this method.
     *
     * @param cmp The component to set visibility on
     * @param visible true to set visible and enabled, false otherwise
     */
    private void setVisibleRecursive(Component cmp,boolean visible) {
        cmp.setEnabled(visible);
        cmp.setVisible(visible);
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            for(int i=0;i<cont.getComponentCount();i++) {
                setVisibleRecursive(cont.getComponentAt(i), visible);
            }
        }
    }

    // TODO - This is a problematic implementation since if a text has been converted to UPPERCASE and then due to a child's style attribute it has to change back to none/capitalize - there's no way to restore the original text.
    // Also it has a problem with FIXED_WIDTH mode since when uppercasing for example, labels will grow in size which will take some of them out of the screen, The correct way is working on the elements and not the text, and reconstruct the labels
    /**
     * Sets the specified text transform to the component and all its children
     * 
     * @param cmp The component to work on
     * @param transformType The text transform type, one of the TEXT_TRANSFORM_* constants
     */
    private void setTextTransformRecursive(Component cmp,int transformType) {
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            for(int i=0;i<cont.getComponentCount();i++) {
                setTextTransformRecursive(cont.getComponentAt(i), transformType);
            }
        } else if (cmp instanceof Label) {
            Label label=(Label)cmp;
            switch(transformType) {
                case TEXT_TRANSFORM_UPPERCASE:
                    label.setText(label.getText().toUpperCase());
                    break;
                case TEXT_TRANSFORM_LOWERCASE:
                    label.setText(label.getText().toLowerCase());
                    break;
                case TEXT_TRANSFORM_CAPITALIZE:

                    String text=label.getText();

                    String newText="";
                    boolean capNextLetter=true;
                    for(int i=0;i<text.length();i++) {
                        char c=text.charAt(i);
                        if (CSSParser.isWhiteSpace(c)) {
                            capNextLetter=true;
                        } else if (capNextLetter) {
                            if ((c>='a') && (c<='z')) {
                                c-=32; // 'A' is ASCII 65, and 'a' is ASCII 97, difference: 32
                            }
                            capNextLetter=false;
                        }
                        newText+=c;
                    }
                    label.setText(newText);
                    break;
            }
        }

    }

    /**
     * Sets the alignment of the component and all its children according to the given alignment
     * 
     * @param cmp The component to set the alignment on
     * @param align The alignment - one of left,center,right
     */
    private void setTextAlignmentRecursive(Component cmp,int align) {
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            if (cont.getLayout() instanceof FlowLayout) {
                cont.setLayout(new FlowLayout(align));
            }
            for(int i=0;i<cont.getComponentCount();i++) {
                setTextAlignmentRecursive(cont.getComponentAt(i), align);
            }
        } else if ((HTMLComponent.FIXED_WIDTH) && (cmp instanceof Label)) { // In FIXED_WIDTH mode labels are aligned by appling alignment on themselves and enlarging the label size to take the whole width of the screen
            ((Label)cmp).setAlignment(align);
        }
    }

    
    /**
     * Sets the given text indentation to the component and all its children
     * Note: This doesn't really work well with HTMLComponent.FIXED_WIDTH mode since labels there are not single words but rather the whole line, so they get pushed out of the screen
     * 
     * @param cmp The component to set the indentation on
     * @param indent The indentation in pixels
     */
    private void setTextIndentationRecursive(Component cmp,int indent) {
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            if ((cont.getLayout() instanceof FlowLayout) && (cont.getComponentCount()>0)) {
                // Note that we don't need to consider the "applicable" styles, as this is a container and will always return selected+unselected
                cont.getComponentAt(0).getUnselectedStyle().setMargin(Component.LEFT, indent);
                cont.getComponentAt(0).getSelectedStyle().setMargin(Component.LEFT, indent);
            }
            for(int i=0;i<cont.getComponentCount();i++) {
                setTextIndentationRecursive(cont.getComponentAt(i), indent);
            }
        }
    }

    /**
     * Turns on the visibilty of all ancestors of the given component
     * 
     * @param cmp The component to work on
     */
    private void setParentsVisible(Component cmp) {
        Container cont=cmp.getParent();
        while (cont!=null) {
            cont.setVisible(true);
            cont=cont.getParent();
        }
    }

    /**
     * Replaces an unwrapped text with a wrapped version, while copying the style of the original text.
     * 
     * @param label The current label that contains the unwrapped text
     * @param words A vector containing one word of the text (without white spaces) in each element
     * @param element The text element
     */
    private void setWrapText(Label label,Vector words,HTMLElement element,HTMLComponent htmlC) {
        Style selectedStyle=label.getSelectedStyle();
        Style unselectedStyle=label.getUnselectedStyle();
        Vector ui=new Vector();
        label.setText((String)words.elementAt(0)+' ');
        HTMLLink link=null;
        if (label instanceof HTMLLink) {
            link=(HTMLLink)label;
        }
        ui.addElement(label);
        for(int i=1;i<words.size();i++) {
            Label word=null;
            if (link!=null) {
                word=new HTMLLink((String)words.elementAt(i)+' ',link.link,htmlC,link,link.linkVisited);
            } else {
                word=new Label((String)words.elementAt(i)+' ');
            }
            word.setSelectedStyle(selectedStyle);
            word.setUnselectedStyle(unselectedStyle);
            label.getParent().addComponent(word);
            ui.addElement(word);
        }
        element.setAssociatedComponents(ui);
        label.getParent().revalidate();
    }


    /**
     * Sets this element and all children to have wrapped text.
     * In cases where text is already wrapped no change will be made.
     * This will work only in FIXED_WIDTH mode (Checked before called)
     * Technically all this logic can be found in HTMLComponent.showText, but since we don't want to get into
     * the context of this element (i.e. what was the indentation, alignment etc.), we use this algorithm.
     * 
     * @param element The element to apply text wrapping on
     * @param htmlC The HTMLComponent
     */
    private void setWrapRecursive(HTMLElement element,HTMLComponent htmlC) {
        if (element.isTextElement()) {
            String text=element.getText();
            final Vector ui=element.getUi();
            if ((text!=null) && (ui!=null) && (ui.size()==1)) { //If it's already wrapped, no need to process
                final Vector words=htmlC.getWords(text, Component.LEFT, false);
                final Label label=(Label)ui.elementAt(0);
                setWrapText(label, words, element,htmlC);
            }
        }

        for(int i=0;i<element.getNumChildren();i++) {
            setWrapRecursive((HTMLElement)element.getChildAt(i),htmlC);
        }

    }

    /**
     * Replaces a wrapped text with an unwrapped version.
     * This in fact removes all the labels that contains a single word each, and replaces them with one label that contains the whole text.
     * This way the label is not wrapped.
     *
     * @param label The first label of this text element (can be derived from ui but already fetched before the call)
     * @param ui The vector consisting all components (labels) that contain the text's words
     * @param newText The new text that should replace current components (unwrapped text without extra white spaces)
     * @param element The text element
     */
    private void setNowrapText(Label label,Vector ui,String newText,HTMLElement element) {
        label.setText(newText);
        for(int i=1;i<ui.size();i++) {
            Component cmp=(Component)ui.elementAt(i);
            cmp.getParent().removeComponent(cmp);
        }
        if (label instanceof HTMLLink) {
            ((HTMLLink)label).childLinks=new Vector(); // Reset all associated link fragments so we don't have unneeded references
        }
        element.setAssociatedComponents(label);
        label.getParent().revalidate();
    }

    /**
     * Sets this element and all children to have unwrapped text.
     * In cases where text is already unwrapped no change will be made.
     * This will work only in FIXED_WIDTH mode (Checked before called)
     * Technically a lot of this logic can be found in HTMLComponent, but since we don't want to get into
     * the context of this element (i.e. what was the indentation, alignment etc.), we use this algorithm.
     *
     * @param element The element to apply text wrapping on
     */
    private void setNowrapRecursive(final HTMLElement element) {
        //if (element.getId()==HTMLElement.TAG_TEXT) {
        if (element.isTextElement()) {
            //String text=element.getAttributeById(HTMLElement.ATTR_TITLE);
            String text=element.getText();
            final Vector ui=element.getUi();
            if ((text!=null) && (ui!=null) && (ui.size()>1)) { //If it's just one word or already no-wrapped, no need to process
                String word="";
                String newText="";
                for(int c=0;c<text.length();c++) {
                    char ch=text.charAt(c);
                    if ((ch==' ') || (ch==10) || (ch==13) || (ch=='\t') || (ch=='\n')) {
                        if (!word.equals("")) {
                            newText+=word+" ";
                            word="";
                        }
                    } else {
                        word+=ch;
                    }
                }
                if (!word.equals("")) {
                    newText+=word+" ";
                }

                final Label label=(Label)ui.elementAt(0);
                setNowrapText(label, ui, newText, element);
            }
        }

        for(int i=0;i<element.getNumChildren();i++) {
            setNowrapRecursive((HTMLElement)element.getChildAt(i));
        }

        element.recalcUi(); // If children elements' UI was changed, we need to recalc the UI of the parent 

    }


    ////////
    // CSS2 additions - the following are not in the WCSS spec, but rather in the CSS2 spec
    ///////

    /**
     * Sets the text direction of the component
     *
     * @param cmp The component to set
     * @param rtl true for right-to-left, false for left-to-right
     */
    private void setDirectionRecursive(Component cmp,boolean rtl) {
        cmp.setRTL(rtl);
        if (cmp instanceof Container) {
            Container c = (Container)cmp;
            for(int i=0;i<c.getComponentCount();i++) {
                setDirectionRecursive(c.getComponentAt(i), rtl);
            }
        }
    }

    /**
     * Sets the given spacing to all words in this component and its children
     *
     * @param cmp The component to set the spacing on
     * @param spacing The spacing in pixels
     */
    private void setWordSpacingRecursive(Component cmp,int spacing) {
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            for(int i=0;i<cont.getComponentCount();i++) {
                setWordSpacingRecursive(cont.getComponentAt(i), spacing);
            }
        } else if ((cmp instanceof Label) &&
                   (cmp.getParent().getComponentIndex(cmp)<cmp.getParent().getComponentCount()-1))  { // don't apply to the last word
            cmp.getUnselectedStyle().setPadding(Component.RIGHT, spacing);
            if (cmp instanceof HTMLLink) {
                cmp.getSelectedStyle().setPadding(Component.RIGHT, spacing);
                ((HTMLLink)cmp).getPressedStyle().setPadding(Component.RIGHT, spacing);
            }
        }
    }

    /**
     * Sets the given spacing to all words in this component and its children
     *
     * @param cmp The component to set the spacing on
     * @param halfHeight Half of the line height in pixels (will be added to top and bottom margins to make for a full height)
     */
    private void setLineHeightRecursive(Component cmp,int halfHeight) {
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            for(int i=0;i<cont.getComponentCount();i++) {
                setLineHeightRecursive(cont.getComponentAt(i), halfHeight);
            }
        } else if (cmp instanceof Label) {
            cmp.getUnselectedStyle().setMargin(Component.TOP, halfHeight);
            cmp.getUnselectedStyle().setMargin(Component.BOTTOM, halfHeight);
            if (cmp instanceof HTMLLink) {
                cmp.getSelectedStyle().setPadding(Component.TOP, halfHeight);
                cmp.getSelectedStyle().setPadding(Component.BOTTOM, halfHeight);
                ((HTMLLink)cmp).getPressedStyle().setPadding(Component.TOP, halfHeight);
                ((HTMLLink)cmp).getPressedStyle().setPadding(Component.BOTTOM, halfHeight);
            }
        }
    }

    /**
     * Utility method to add a specific text decoration if it does not exist
     * 
     * @param style The style to add the decoration to
     * @param decoration The deocration (One of Style.TEXT_DECORATION_* constants)
     */
    private void applyDecorationOnStyle(Style style,int decoration) {
        int curDecoration=style.getTextDecoration();
        if ((curDecoration & decoration)==0) {
            style.setTextDecoration(curDecoration|decoration);
        }
    }

    /**
     * Sets the specified decoration to the specified components and all of its Label descendants
     * 
     * @param cmp The component to apply the decoration to
     * @param decoration The deocration (One of Style.TEXT_DECORATION_* constants)
     * @param selector The selector with the text direction directive
     */
    private void setTextDecorationRecursive(Component cmp,int decoration,CSSElement selector) {
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            for(int i=0;i<cont.getComponentCount();i++) {
                setTextDecorationRecursive(cont.getComponentAt(i), decoration,selector);
            }
        } else if (cmp instanceof Label) {
            int styles=getApplicableStyles(cmp, selector);
            if ((styles & STYLE_UNSELECTED)!=0) {
                applyDecorationOnStyle(cmp.getUnselectedStyle(),decoration);
            }
            if ((styles & STYLE_SELECTED)!=0) {
                applyDecorationOnStyle(cmp.getSelectedStyle(),decoration);
            }
            if ((styles & STYLE_PRESSED)!=0) {
                applyDecorationOnStyle(((HTMLLink)cmp).getPressedStyle(),decoration);
            }
        }
    }

    /**
     * Removes all text decorations from the specified components and its Label descendants
     * This will be used for {text-decoration: none}
     * 
     * @param cmp The component to remove decorations from
     * @param selector The selector with the text direction directive
     */
    private void removeTextDecorationRecursive(Component cmp,CSSElement selector) {
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            for(int i=0;i<cont.getComponentCount();i++) {
                removeTextDecorationRecursive(cont.getComponentAt(i),selector);
            }
        } else if (cmp instanceof Label) {
            int styles=getApplicableStyles(cmp, selector);
            if ((styles & STYLE_UNSELECTED)!=0) {
                cmp.getUnselectedStyle().setTextDecoration(Style.TEXT_DECORATION_NONE);
            }
            if ((styles & STYLE_SELECTED)!=0) {
                cmp.getSelectedStyle().setTextDecoration(Style.TEXT_DECORATION_NONE);
            }
            if ((styles & STYLE_PRESSED)!=0) {
                ((HTMLLink)cmp).getPressedStyle().setTextDecoration(Style.TEXT_DECORATION_NONE);
            }
        }
    }


    /**
     * Changes the quotes marking of a certain block
     *
     * @param cmp The component to change the quotes in
     * @param quotes an array with 4 strings representing how quotes should look like (primary start,primary end,secondary start,secondary end)
     */
    private void setQuotesRecursive(Component cmp,String[] quotes) {
        if (cmp instanceof Container) {
            Container cont=(Container)cmp;
            for(int i=0;i<cont.getComponentCount();i++) {
                setQuotesRecursive(cont.getComponentAt(i), quotes);
            }
        } else if (cmp instanceof Label) {
            Object o = cmp.getClientProperty(HTMLComponent.CLIENT_PROPERTY_QUOTE);
            if (o!=null) {
                ((Label)cmp).setText(quotes[((Integer)o).intValue()]);
            }
        }
    }


    ////////
    // CSS2 additions end
    ///////


    /**
     * Applies the given CSS directives to the component
     * 
     * @param ui The component representing (part of) the element that the style should be applied to
     * @param selector The style attributes relating to this element
     * @param element The element the style should be applied to
     * @param htmlC The HTMLComponent to which this element belongs to
     * @param focus true if the style should be applied only to the selected state iof the ui (a result of pseudo-class selector a:focus etc.)
     */
    private void applyStyleToUIElement(Component ui, CSSElement selector,HTMLElement element,HTMLComponent htmlC) {
        //count++;
        int styles = getApplicableStyles(ui, selector); // This is relevant only for non recursive types - otherwise we need to recheck everytime since it depends on the specific UI component class

        // White spaces
        if (HTMLComponent.FIXED_WIDTH) { // This works well only in fixed width mode (Since we cannot "force" a newline in FlowLayout)
            // TODO - enable in FIXED_WIDTH for pre vs. normal/nowrap
            int space=selector.getAttrVal(CSSElement.CSS_WHITE_SPACE);

            if (space!=-1) {
                switch(space) {
                    case WHITE_SPACE_NORMAL:
                        setWrapRecursive(element, htmlC);
                        break;
                    case WHITE_SPACE_NOWRAP:
                        setNowrapRecursive(element);
                        break;
                    case WHITE_SPACE_PRE:
                        // TODO - Not implemented yet
                        break;
                }
            }
        }

        // Input format
        String v=selector.getAttributeById(CSSElement.CSS_WAP_INPUT_FORMAT);
        if ((v!=null) && ((element.getTagId()==HTMLElement.TAG_TEXTAREA) || (element.getTagId()==HTMLElement.TAG_INPUT)) && (ui instanceof TextArea)) {
                v=omitQuotesIfExist(v);
            ui=htmlC.setInputFormat((TextArea)ui, v); // This may return a new instance of TextField taht has to be updated in the tree. This is alos the reason why input format is the first thing checked - see HTMLInputFormat.applyConstraints
            element.setAssociatedComponents(ui);
        }

        // Input emptyOK
        int inputRequired=selector.getAttrVal(CSSElement.CSS_WAP_INPUT_REQUIRED);
        if ((inputRequired!=-1) && ((element.getTagId()==HTMLElement.TAG_TEXTAREA) || (element.getTagId()==HTMLElement.TAG_INPUT)) && (ui instanceof TextArea)) {
            if (inputRequired==INPUT_REQUIRED_TRUE) {
                htmlC.setInputRequired(((TextArea)ui), true);
            } else if (inputRequired==INPUT_REQUIRED_FALSE) {
                htmlC.setInputRequired(((TextArea)ui), false);
            }
        }

        // Display
        int disp=selector.getAttrVal(CSSElement.CSS_DISPLAY);
        switch(disp) {
            case DISPLAY_NONE:
                    if (ui.getParent()!=null) {
                        ui.getParent().removeComponent(ui);
                    } else { //special case for display in the BODY tag
                        if(ui instanceof Container){
                            ((Container)ui).removeAll();
                        }
                    }
                    return;
            case DISPLAY_MARQUEE: // Animate component (ticker-like)
                htmlC.marqueeComponents.addElement(ui);
                break;
           //TODO - support also: block, inline and list-item (All mandatory in WCSS)
        }

        // Visibility
        int visibility=selector.getAttrVal(CSSElement.CSS_VISIBILITY);
        if (visibility!=-1) {
            boolean visible=(visibility==VISIBILITY_VISIBLE);
            setVisibleRecursive(ui,visible);
            if (!visible) {
                return; // Don't waste time on processing hidden elements, though technically the size of the element is still reserved and should be according to style
            } else {
                setParentsVisible(ui); // Need to turn on visibility of all component's parents, in case they were declared hidden
            }
        }

        //
        // Dimensions
        //

        // TODO - debug: Width and Height don't always work - for simple components they usually do, but for containers either they don't have any effect or some inner components (with size restrictions) disappear
        // We use the entire display width and height as reference since htmlC still doesn't have a preferred size or actual size
        // Width
        
        // TODO - Width/Height is disabled currently, since it causes a lot of side effects, making some components disappear
        /*
        int width=selector.getAttrLengthVal(CSSElement.CSS_WIDTH,ui,Display.getInstance().getDisplayWidth());

        // Height
        int height=selector.getAttrLengthVal(CSSElement.CSS_HEIGHT,ui,Display.getInstance().getDisplayHeight());

        if (!HTMLComponent.PROCESS_HTML_MP1_ONLY) {
            int minWidth=selector.getAttrLengthVal(CSSElement.CSS_MIN_WIDTH,ui,Display.getInstance().getDisplayWidth());
            int maxWidth=selector.getAttrLengthVal(CSSElement.CSS_MAX_WIDTH,ui,Display.getInstance().getDisplayWidth());
            int minHeight=selector.getAttrLengthVal(CSSElement.CSS_MIN_HEIGHT,ui,Display.getInstance().getDisplayHeight());
            int maxHeight=selector.getAttrLengthVal(CSSElement.CSS_MAX_HEIGHT,ui,Display.getInstance().getDisplayHeight());

            if (width==-1) { // process min/max only if exact was not specified
                if ((minWidth!=-1) && (minWidth>ui.getPreferredW())) {
                    width=minWidth;
                }
                if ((maxWidth!=-1) && (maxWidth<ui.getPreferredW())) {
                    width=maxWidth;
                }
            }
            if (height==-1) { // process min/max only if exact was not specified
                if ((minHeight!=-1) && (minHeight>ui.getPreferredH())) {
                    height=minHeight;
                }
                if ((maxHeight!=-1) && (maxHeight<ui.getPreferredH())) {
                    height=maxHeight;
                }
            }
        }

        if ((width!=-1) || (height!=-1)) {
            if (width==-1) {
                width=ui.getPreferredW();
            }
            if (height==-1) {
                height=ui.getPreferredH();
            }
            ui.setPreferredSize(new Dimension(width,height));
        }
        */

        //
        // Colors
        //
        
        // Background Color
        int bgColor=selector.getAttrVal(CSSElement.CSS_BACKGROUND_COLOR);
        if (bgColor!=-1)  {
                if ((styles & STYLE_UNSELECTED)!=0) {
                    ui.getUnselectedStyle().setBgColor(bgColor);
                    ui.getUnselectedStyle().setBgTransparency(255);

                }
                if ((styles & STYLE_SELECTED)!=0) {
                    ui.getSelectedStyle().setBgColor(bgColor);
                    ui.getSelectedStyle().setBgTransparency(255);
                }
                if ((styles & STYLE_PRESSED)!=0) {
                    ((HTMLLink)ui).getPressedStyle().setBgColor(bgColor);
                    ((HTMLLink)ui).getPressedStyle().setBgTransparency(255);
                }

        }

        // Foreground color
        int fgColor=selector.getAttrVal(CSSElement.CSS_COLOR);
        if (fgColor != -1) {
            setColorRecursive(ui, fgColor,selector);
        }
        
        // Background Image
        v = selector.getAttributeById(CSSElement.CSS_BACKGROUND_IMAGE);
        if (v!=null) {
            String url=getCSSUrl(v);

            if (url!=null) {

                // Setting an alternative bgPainter that can support CSS background properties
                CSSBgPainter bgPainter=new CSSBgPainter(ui);

                // Background tiling
                byte bgType = (byte)selector.getAttrVal(CSSElement.CSS_BACKGROUND_REPEAT);
                if (bgType==-1) {
                    bgType=Style.BACKGROUND_IMAGE_TILE_BOTH; // default value
                }

                // Note that we don't set transparency to 255, since the image may have its own transparency/opaque areas - we don't want to block the entire component/container entirely
                if ((styles & STYLE_SELECTED)!=0) {
                    ui.getSelectedStyle().setBgPainter(bgPainter);
                    ui.getSelectedStyle().setBackgroundType(bgType);
                }
                if ((styles & STYLE_UNSELECTED)!=0) {
                    ui.getUnselectedStyle().setBgPainter(bgPainter);
                    ui.getUnselectedStyle().setBackgroundType(bgType);
                }
                if ((styles & STYLE_PRESSED)!=0) {
                    ((HTMLLink)ui).getPressedStyle().setBgPainter(bgPainter);
                    ((HTMLLink)ui).getPressedStyle().setBackgroundType(bgType);
                }

                // The background image itself
                if (htmlC.showImages) {
                    if (htmlC.getDocumentInfo()!=null) {
                        htmlC.getThreadQueue().addBgImage(ui,htmlC.convertURL(url),styles);
                    } else {
                        if (DocumentInfo.isAbsoluteURL(url)) {
                            htmlC.getThreadQueue().addBgImage(ui,url,styles);
                        } else {
                            if (htmlC.getHTMLCallback()!=null) {
                                htmlC.getHTMLCallback().parsingError(HTMLCallback.ERROR_NO_BASE_URL, selector.getTagName() , selector.getAttributeName(new Integer(CSSElement.CSS_BACKGROUND_IMAGE)),url,"Ignoring background image file referred in a CSS file/segment ("+url+"), since page was set by setBody/setHTML/setDOM so there's no way to access relative URLs");
                            }
                        }
                    }
                }

                for(int i=CSSElement.CSS_BACKGROUND_POSITION_X;i<=CSSElement.CSS_BACKGROUND_POSITION_Y;i++) {
                    int pos=selector.getAttrVal(i);
                    if (pos!=-1) {
                        bgPainter.setPosition(i,pos);
                    }
                }

                // Background attachment: Either 'fixed' (i.e. the bg image is fixed in its position even when scrolling)
                // or 'scroll' (default) which means the it moves with scrolling (Like usually in CodenameOne backgrounds)
                if (selector.getAttrVal((CSSElement.CSS_BACKGROUND_ATTACHMENT))==BG_ATTACHMENT_FIXED) {
                       bgPainter.setFixed();
                }

            }
        }

        // TODO - float: none/left/right
        // TODO - clear: none/left/right/both

        // Margin
        Component marginComp=ui;
        if (ui instanceof Label) { // If this is a Label/HTMLLink we do not apply the margin individually to each word, but rather to the whole block
            marginComp=ui.getParent();
        } else if ((element.getTagId()==HTMLElement.TAG_LI) && (ui.getParent().getLayout() instanceof BorderLayout)) {
            marginComp=ui.getParent();
        }
        for(int i=CSSElement.CSS_MARGIN_TOP;i<=CSSElement.CSS_MARGIN_RIGHT;i++) {
            int marginPixels=-1;

            if ((i==CSSElement.CSS_MARGIN_TOP) || (i==CSSElement.CSS_MARGIN_BOTTOM)) {
                marginPixels=selector.getAttrLengthVal(i, ui, htmlC.getHeight()); // Here the used component is ui and not marginComp, since we're interested in the font size (which will be corrent in Labels not in their containers)
            } else {
                marginPixels=selector.getAttrLengthVal(i, ui, htmlC.getWidth());
            }
            if (marginPixels>=0 && marginComp != null) { // Only positive or 0

                if ((styles & STYLE_SELECTED)!=0) {
                    marginComp.getSelectedStyle().setMargin(i-CSSElement.CSS_MARGIN_TOP, marginPixels);
                    // If this is a link and the selector applies only to Selected, it means this is an 'a:focus'
                    // Since the marginComp is not focusable (as it is the container holding the link), HTMLLink takes care of focusing the
                    // parent when the link focuses
                    if ((ui instanceof HTMLLink) && (styles==STYLE_SELECTED)) {
                        ((HTMLLink)ui).setParentChangesOnFocus();
                    }
                }
                if ((styles & STYLE_UNSELECTED)!=0) {
                    marginComp.getUnselectedStyle().setMargin(i-CSSElement.CSS_MARGIN_TOP, marginPixels);
                    
                }
                // Since we don't apply the margin/padding on the component but rather on its parent
                // There is no point in setting the PRESSED style since we don't have a pressed event from Button, nor do we have a pressedStyle for containers
                // That's why we can't do the same trick as in selected style, and the benefit of this rather "edge" case (That is anyway not implemented in all browsers) seems rather small
                //    if ((styles & STYLE_PRESSED)!=0) {
                //        ((HTMLLink)ui).getPressedStyle().setMargin(i-CSSElement.CSS_MARGIN_TOP, marginPixels);
                //    }
            }
        }

        Component padComp=ui;
        if (ui instanceof Label) {
            padComp=ui.getParent();
        } else if ((element.getTagId()==HTMLElement.TAG_LI) && (ui.getParent().getLayout() instanceof BorderLayout)) {
            padComp=ui.getParent();
        }

        for(int i=CSSElement.CSS_PADDING_TOP;i<=CSSElement.CSS_PADDING_RIGHT;i++) {
            int padPixels=-1;

            if ((i==CSSElement.CSS_PADDING_TOP) || (i==CSSElement.CSS_PADDING_BOTTOM)) {
                padPixels=selector.getAttrLengthVal(i, ui, htmlC.getHeight());
            } else {
                padPixels=selector.getAttrLengthVal(i, ui, htmlC.getWidth());
            }
            if (padPixels>=0) { // Only positive or 0
                if ((styles & STYLE_SELECTED)!=0) {
                    if(padComp != null){
                        padComp.getSelectedStyle().setPadding(i-CSSElement.CSS_PADDING_TOP, padPixels);
                    }
                    if ((ui instanceof HTMLLink) && (styles==STYLE_SELECTED)) { // See comment on margins
                        ((HTMLLink)ui).setParentChangesOnFocus();
                    }
                }
                if ((styles & STYLE_UNSELECTED)!=0) {
                    if(padComp != null){
                        padComp.getUnselectedStyle().setPadding(i-CSSElement.CSS_PADDING_TOP, padPixels);
                    }
                }
                // See comment in margin on why PRESSED was dropped
                //    if ((styles & STYLE_PRESSED)!=0) {
                //        ((HTMLLink)padComp).getPressedStyle().setPadding(i-CSSElement.CSS_PADDING_TOP, padPixels);
                //    }
            }
        }

        //
        // Text
        //
        
        // Text Alignment
        int align=selector.getAttrVal(CSSElement.CSS_TEXT_ALIGN);
        if (align!=-1) {
            switch(element.getTagId()) {
                case HTMLElement.TAG_TD:
                case HTMLElement.TAG_TH:
                    setTableCellAlignment(element, ui, align, true);
                    break;
                case HTMLElement.TAG_TR:
                    setTableCellAlignmentTR(element, ui, align, true);
                    break;
                case HTMLElement.TAG_TABLE:
                    setTableAlignment(ui, align, true);
                    break;
                default:
                    setTextAlignmentRecursive(ui, align); // TODO - this sometimes may collide with the HTML align attribute. If the style of the same tag has alignment it overrides the align attribute, but if it is inherited, the align tag prevails
            }
        }

        // Vertical align
            int valign=selector.getAttrVal(CSSElement.CSS_VERTICAL_ALIGN);
            if (valign!=-1) {
            switch(element.getTagId()) {
                case HTMLElement.TAG_TD:
                case HTMLElement.TAG_TH:
                    setTableCellAlignment(element, ui, valign, false);
                    break;
                case HTMLElement.TAG_TR:
                    setTableCellAlignmentTR(element, ui, valign, false);
                    break;
//                case Element.TAG_TABLE: // vertical alignment denoted in the table tag doesn't affect it in most browsers
//                     setTableAlignment(element, ui, valign, false);
//                     break;
                default:
                   //TODO - implement vertical alignment for non-table elements
            }
        }

        // Text Transform
        int transform=selector.getAttrVal(CSSElement.CSS_TEXT_TRANSFORM);
        if (transform!=-1) {
            setTextTransformRecursive(ui, transform);
        }

        // Text indentation
        int indent=selector.getAttrLengthVal(CSSElement.CSS_TEXT_INDENT, ui, htmlC.getWidth());
        if (indent>=0) { // Only positive (0 also as it may cancel previous margins)
            setTextIndentationRecursive(ui, indent);
        }

        //
        // Font
        //
        
        // Font family
        String fontFamily=selector.getAttributeById(CSSElement.CSS_FONT_FAMILY);
        if (fontFamily!=null) {
            int index=fontFamily.indexOf(',');
            if (index!=-1) { // Currently we ignore font families fall back (i.e. Arial,Helvetica,Sans-serif) since even finding a match for one font is quite expensive performance-wise
                fontFamily=fontFamily.substring(0, index);
            }
        }
        // Font Style
        int fontStyle=selector.getAttrVal(CSSElement.CSS_FONT_STYLE);


        // Font Weight
        int fontWeight=selector.getAttrVal(CSSElement.CSS_FONT_WEIGHT);

        int fontSize=selector.getAttrLengthVal(CSSElement.CSS_FONT_SIZE,ui, ui.getStyle().getFont().getHeight());
        if (fontSize<-1) {
            int curSize=ui.getStyle().getFont().getHeight();
            if (fontSize==CSSElement.FONT_SIZE_LARGER) {
                fontSize=curSize+2;
            } else if (fontSize==CSSElement.FONT_SIZE_SMALLER) {
                fontSize=curSize-2;
            }
        }

        // Since J2ME doesn't support small-caps fonts, when a small-caps font varinat is requested
        // the font-family is changed to "smallcaps" which should be loaded to HTMLComponent and the theme as a bitmap font
        // If no smallcaps font is found at all, then the family stays the same, but if even only one is found - the best match will be used.
        int fontVariant=selector.getAttrVal(CSSElement.CSS_FONT_VARIANT);
        if ((fontVariant==FONT_VARIANT_SMALLCAPS) && (htmlC.isSmallCapsFontAvailable())) {
            fontFamily=CSSElement.SMALL_CAPS_STRING;
        }

        // Process font only if once of the font CSS properties was mentioned and valid
        if ((fontFamily!=null) || (fontSize!=-1) || (fontStyle!=-1) || (fontWeight!=-1)) {
            setFontRecursive(htmlC, ui, fontFamily, fontSize, fontStyle, fontWeight,selector);
        }

        // List style
        int listType=-1;

        String listImg=null;
        Component borderUi=ui;

        if ((element.getTagId()==HTMLElement.TAG_LI) || (element.getTagId()==HTMLElement.TAG_UL) || (element.getTagId()==HTMLElement.TAG_OL) || (element.getTagId()==HTMLElement.TAG_DIR) || (element.getTagId()==HTMLElement.TAG_MENU)) {
            int listPos=selector.getAttrVal(CSSElement.CSS_LIST_STYLE_POSITION);
            if (listPos==LIST_STYLE_POSITION_INSIDE) {
                // Padding and not margin since background color should affect also the indented space
                ui.getStyle().setPadding(Component.LEFT, ui.getStyle().getMargin(Component.LEFT)+INDENT_LIST_STYLE_POSITION);
                Container parent=ui.getParent();

                if (parent.getLayout() instanceof BorderLayout) {
                    borderUi=parent;
                }
            }

            listType=selector.getAttrVal(CSSElement.CSS_LIST_STYLE_TYPE);
            listImg=getCSSUrl(selector.getAttributeById(CSSElement.CSS_LIST_STYLE_IMAGE));
        }

        // Border
        Border[] borders = new Border[4];
        boolean leftBorder=false; // Used to prevent drawing a border in the middle of two words in the same segment
        boolean rightBorder=false; // Used to prevent drawing a border in the middle of two words in the same segment
        boolean hasBorder=false;
        if ((borderUi==ui) && (element.getUi().size()>1)) {
            if (element.getUi().firstElement()==borderUi) {
                leftBorder=true;
            } else if (element.getUi().lastElement()==borderUi) {
                rightBorder=true;
            }
        } else {
            leftBorder=true;
            rightBorder=true;
        }

        for(int i=Component.TOP;i<=Component.RIGHT;i++) {
            if ((i==Component.BOTTOM) || (i==Component.TOP) ||
                ((i==Component.LEFT) && (leftBorder)) ||
                ((i==Component.RIGHT) && (rightBorder))) { 
                borders[i]=createBorder(selector, borderUi, i,styles,BORDER);
                if (borders[i]!=null) {
                    hasBorder=true;
                }
            }
        }
        if (hasBorder) {
            Border curBorder=borderUi.getUnselectedStyle().getBorder();
            if (((styles & STYLE_SELECTED)!=0) && ((styles & STYLE_UNSELECTED)==0)) {
                curBorder=borderUi.getSelectedStyle().getBorder();
            }
            if ((styles & STYLE_PRESSED)!=0) {
                curBorder=((HTMLLink)borderUi).getSelectedStyle().getBorder();
            }

            // In case this element was assigned a top border for instance, and then by belonging to another tag/class/id it has also a bottom border - this merges the two (and gives priority to the new one)
            if ((curBorder!=null) && (curBorder.getCompoundBorders()!=null)) { // TODO - This doesn't cover the case of having another border (i.e. table/fieldset?) - Can also assign the non-CSS border to the other corners?
                //curBorder.
                Border[] oldBorders = curBorder.getCompoundBorders();
                for(int i=Component.TOP;i<=Component.RIGHT;i++) {
                    if (borders[i]==null) {
                        borders[i]=oldBorders[i];
                    }
                }
            }
            Border border=Border.createCompoundBorder(borders[Component.TOP], borders[Component.BOTTOM], borders[Component.LEFT], borders[Component.RIGHT]);
            if (border!=null) {
                if ((styles & STYLE_SELECTED)!=0) {
                    borderUi.getSelectedStyle().setBorder(border);
                }
                if ((styles & STYLE_UNSELECTED)!=0) {
                    borderUi.getUnselectedStyle().setBorder(border);
                }
                if ((styles & STYLE_PRESSED)!=0) {
                    ((HTMLLink)borderUi).getPressedStyle().setBorder(border);
                }
                if (borderUi.getParent()!=null) {
                    borderUi.getParent().revalidate();
                } else if (borderUi instanceof Container) {
                    ((Container)borderUi).revalidate();
                }
            }
        }

        //
        // Specific elements styling
        //
        
        // Access keys
        v=selector.getAttributeById(CSSElement.CSS_WAP_ACCESSKEY);
        if ((v!=null) && (v.length()>=1) &&
                ((element.getTagId()==HTMLElement.TAG_INPUT) ||  // These are the only tags that can accpet an access key
                 (element.getTagId()==HTMLElement.TAG_TEXTAREA) || (element.getTagId()==HTMLElement.TAG_LABEL) ||
                 ((element.getTagId()==HTMLElement.TAG_A) && (ui instanceof HTMLLink) && ((HTMLLink)ui).parentLink==null)) // For A tags this is applied only to the first word, no need to apply it to each word of the link
                ) {

            // The accesskey string may consist fallback assignments (comma seperated) and multiple assignments (space seperated) and any combination of those
            // For example: "send *, #" (meaning: assign both the send and * keys, and if failed to assign one of those assign the # key instead)
            int index=v.indexOf(',');
            boolean assigned=false;
            while (index!=-1) { // Handle fallback access keys
                String key=v.substring(0,index).trim();
                v=v.substring(index+1);
                assigned=processAccessKeys(key, htmlC, ui);
                if (assigned) {
                    break; // comma denotes fallback, and once we succeeded assigning the accesskey, the others are irrelevant
                }
                index=v.indexOf(',');
            }
            if (!assigned) {
                processAccessKeys(v.trim(), htmlC, ui);
            }

        }


        if (!HTMLComponent.PROCESS_HTML_MP1_ONLY) {

            // Text decoration (In HTML-MP1 the only mandatory decoration is 'none')
            int decoration=selector.getAttrVal(CSSElement.CSS_TEXT_DECORATION);
            if (decoration==TEXT_DECOR_NONE) {
                removeTextDecorationRecursive(ui,selector);
            } else if (decoration==TEXT_DECOR_UNDERLINE) {
                setTextDecorationRecursive(ui, Style.TEXT_DECORATION_UNDERLINE,selector);
            } else if (decoration==TEXT_DECOR_LINETHROUGH) {
                setTextDecorationRecursive(ui, Style.TEXT_DECORATION_STRIKETHRU,selector);
            } else if (decoration==TEXT_DECOR_OVERLINE) {
                setTextDecorationRecursive(ui, Style.TEXT_DECORATION_OVERLINE,selector);
            }

            // Word spacing
            if (!HTMLComponent.FIXED_WIDTH) {
                int wordSpace=selector.getAttrLengthVal(CSSElement.CSS_WORD_SPACING, ui, 0); // The relative dimension is 0, since percentage doesn't work with word-spacing in browsers
                if (wordSpace!=-1) {
                    setWordSpacingRecursive(ui, wordSpace);
                }
            }

            // Line height

            // Technically the font height should be queried when actually resizing the line (since it may differ for a big block) - but since this would be ery time consuming and also major browsers don't take it into account - we'll do the same
            //int lineHeight=selector.getAttrLengthVal(CSSElement.CSS_LINE_HEIGHT, ui, ui.getStyle().getFont().getHeight());
            int lineHeight=selector.getAttrLengthVal(CSSElement.CSS_LINE_HEIGHT, ui, ui.getStyle().getFont().getHeight());

            if (lineHeight!=-1) {
                lineHeight=Math.max(0, lineHeight-ui.getStyle().getFont().getHeight()); // 100% means normal line height (don't add margin). Sizes below will not work, even they do in regular browsers
                setLineHeightRecursive(ui, lineHeight/2);
            }

            // Quotes
            String quotesStr=selector.getAttributeById(CSSElement.CSS_QUOTES);
            if (quotesStr!=null) {
                Vector quotes = htmlC.getWords(quotesStr, Component.LEFT, false);
                int size=quotes.size();
                if ((size==2) || (size==4)) {
                    String[] quotesArr = new String[4];
                    for(int i=0;i<size;i++) {
                        quotesArr[i]=omitQuotesIfExist((String)quotes.elementAt(i));
                    }
                    if (size==2) { // If only 2 quotes are specified they are used both as primary and secondary
                        quotesArr[2]=quotesArr[0];
                        quotesArr[3]=quotesArr[1];
                    }
                    setQuotesRecursive(ui,quotesArr);
                }
            }

            // Outline
            Border outline=createBorder(selector, borderUi, 0, styles, OUTLINE);
            if (outline!=null) {
                if ((styles & STYLE_SELECTED)!=0) {
                    addOutlineToStyle(borderUi.getSelectedStyle(),outline);
                }
                if ((styles & STYLE_UNSELECTED)!=0) {
                    addOutlineToStyle(borderUi.getUnselectedStyle(),outline);
                }
                if ((styles & STYLE_PRESSED)!=0) {
                    addOutlineToStyle(((HTMLLink)borderUi).getPressedStyle(),outline);
                }
                if (borderUi.getParent()!=null) {
                    borderUi.getParent().revalidate();
                } else if (borderUi instanceof Container) {
                    ((Container)borderUi).revalidate();
                }
            }

            // Direction
            int dir=selector.getAttrVal(CSSElement.CSS_DIRECTION);
            if (dir!=-1) {
                setDirectionRecursive(ui,dir==DIRECTION_RTL);
            }

            // Table properties
            if (ui instanceof HTMLTable) {
                int tableProp=selector.getAttrVal(CSSElement.CSS_BORDER_COLLAPSE);
                if (tableProp!=-1) {
                    ((HTMLTable)ui).setCollapseBorder(tableProp==BORDER_COLLAPSE_COLLAPSE);
                }
                tableProp=selector.getAttrVal(CSSElement.CSS_EMPTY_CELLS);
                if (tableProp!=-1) {
                    ((HTMLTable)ui).setDrawEmptyCellsBorder(tableProp==EMPTY_CELLS_SHOW);
                }

                tableProp=selector.getAttrVal(CSSElement.CSS_CAPTION_SIDE); // bottom = 0 , top = 1
                if (tableProp!=-1) {
                    Container tableParentCont = ui.getParent();
                    int tablePos=tableParentCont.getComponentIndex(ui); // should result in 0 when the caption is at the bottom, and 1 when the caption is on top
                    if (tableProp!=tablePos) {
                        Component caption=tableParentCont.getComponentAt((tablePos+1)%2);
                        tableParentCont.removeComponent(caption);
                        tableParentCont.addComponent(tablePos, caption);
                    }
                }

                String spacing=selector.getAttributeById(CSSElement.CSS_BORDER_SPACING);
                if (spacing!=null) {
                    spacing=spacing.trim();
                    int index=spacing.indexOf(' ');
                    int spaceH=0;
                    int spaceV=0;
                    if (index==-1) { // one value only
                        spaceH=CSSElement.convertLengthVal(CSSElement.convertUnitsOrPercentage(spacing), ui, ui.getPreferredW());
                        spaceV=spaceH;
                    } else {
                        String spaceHoriz=spacing.substring(0, index);
                        String spaceVert=spacing.substring(index+1);
                        spaceH=CSSElement.convertLengthVal(CSSElement.convertUnitsOrPercentage(spaceHoriz), ui, ui.getPreferredW());
                        spaceV=CSSElement.convertLengthVal(CSSElement.convertUnitsOrPercentage(spaceVert), ui, ui.getPreferredH());
                    }
                    ((HTMLTable)ui).setBorderSpacing(spaceH, spaceV);
                }

            }
        }

        // Note that we carefully check the structure of the LI/UL/OL element with instanceof and checking component count
        // This is since in some cases other elements can come between a OL/UL and its LI items (Though illegal in HTML, it can occur)
        if ((listType!=-1) || (listImg!=null)) {
            if (element.getTagId()==HTMLElement.TAG_LI) {
                if (ui instanceof Container) {
                Container liCont=(Container)ui;
                    Container liParent=liCont.getParent();
                    Component firstComp=liParent.getComponentAt(0);
                    if (firstComp instanceof Container) {
                        Container bulletCont=(Container)firstComp;
                        if (bulletCont.getComponentCount()>0) {
                            Component listItemCmp=bulletCont.getComponentAt(0);
                            if (listItemCmp instanceof Component) {
                                HTMLListItem listItem=((HTMLListItem)listItemCmp);
                                listItem.setStyleType(listType);
                                listItem.setImage(listImg);
                            }
                        }
                    }
                }
            } else if ((element.getTagId()==HTMLElement.TAG_UL) || (element.getTagId()==HTMLElement.TAG_OL) || (element.getTagId()==HTMLElement.TAG_DIR) || (element.getTagId()==HTMLElement.TAG_MENU)) {
                Container ulCont = (Container)ui;
                for(int i=0;i<ulCont.getComponentCount();i++) {
                    Component cmp=ulCont.getComponentAt(i);
                    if (cmp instanceof Container) {
                        Container liCont=(Container)cmp;
                        if (liCont.getComponentCount()>=1) {
                            cmp=liCont.getComponentAt(0);
                            if (cmp instanceof Container) {
                                Container liContFirstLine=(Container)cmp;
                                if (liContFirstLine.getComponentCount()>=1) {
                                    cmp=liContFirstLine.getComponentAt(0);
                                    if (cmp instanceof HTMLListItem) {
                                        HTMLListItem listItem=(HTMLListItem)cmp;
                                        listItem.setStyleType(listType);
                                        listItem.setImage(listImg);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private void addOutlineToStyle(Style style,Border outline) {
        Border curBorder=style.getBorder();
        if (curBorder!=null) {
            curBorder.addOuterBorder(outline);
        } else {
            style.setBorder(outline);
        }
    }

    /**
     * Sets the alignment of all cells in the table 
     * 
     * @param ui The component representing the table (a HTMLTable)
     * @param align The alignment
     * @param isHorizontal true for horizontal alignment, false for vertical alignment
     */
    private void setTableAlignment(Component ui,int align,boolean isHorizontal) {
        HTMLTable table=(HTMLTable)ui; 
        HTMLTableModel model= ((HTMLTableModel)table.getModel());
        model.setAlignToAll(isHorizontal, align);
        table.setModel(model);
    }
    
    /**
     * Sets the table cell 'ui' to the requested alignment
     * 
     * @param tdTag The element representing the table cell (TD/TH tag)
     * @param ui The component representing the table (a HTMLTable)
     * @param align The alignment
     * @param isHorizontal true for horizontal alignment, false for vertical alignment
     */
    private void setTableCellAlignment(HTMLElement tdTag,Component ui,int align,boolean isHorizontal) {
        HTMLElement trTag=(HTMLElement)tdTag.getParent();
        while ((trTag!=null) && (trTag.getTagId()!=HTMLElement.TAG_TR)) { // Though in strict XHTML TR can only contain TD/TH - in some HTMLs TR doesn't have to be the direct parent of the tdTag, i.e.: <tr><b><td>...</td>... </b></tr>
            trTag=(HTMLElement)trTag.getParent();
        }
        setTableCellAlignmentTR(trTag, ui, align, isHorizontal);
    }

    /**
     * Sets the table cell 'ui' to the requested alignment
     * Note that when called directly (on TAG_TR) this is actually called multiple times, each with a different cell of the row as 'ui'.
     * This happens since TR elements contain all their cells as their UI and as such, applyStyle will call applyToUIElement each time with another cell
     *
     * @param trTag The element representing the table row (TR tag) who is the parent of the cell we want to modify
     * @param ui The component representing the table (a HTMLTable)
     * @param align The alignment
     * @param isHorizontal true for horizontal alignment, false for vertical alignment
     */
    private void setTableCellAlignmentTR(HTMLElement trTag,Component ui,int align,boolean isHorizontal) {
        if ((trTag!=null) && (trTag.getTagId()==HTMLElement.TAG_TR)) {
            HTMLElement tableTag=(HTMLElement)trTag.getParent();
            while ((tableTag!=null) && (tableTag.getTagId()!=HTMLElement.TAG_TABLE)) { // Though in strict XHTML TABLE can only contain TR - in some HTMLs it might be different
                tableTag=(HTMLElement)tableTag.getParent();
            }
            if ((tableTag!=null) && (tableTag.getTagId()==HTMLElement.TAG_TABLE)) {
                HTMLTable table=(HTMLTable)tableTag.getUi().elementAt(0);
                HTMLTableModel model= ((HTMLTableModel)table.getModel());
                CellConstraint cConstraint=model.getConstraint(ui);

                if (isHorizontal) {
                    cConstraint.setHorizontalAlign(align);
                } else {
                    cConstraint.setVerticalAlign(align);
                }
                table.setModel(model); // Setting the same model again causes re-evaluation of the constraints

            }
        }

    }

    /**
     * Tries to assign the given key string as an access key to the specified component
     * The key string given here may consist of a multiple key assignment, i.e. several keys seperated with space
     *
     * @param keyStr The string representing the key (either a character, a unicode escape sequence or a special key name
     * @param htmlC The HTMLComponent
     * @param ui The component to set the access key on
     * @return true if successful, false otherwise
     */
    private boolean processAccessKeys(String keyStr,HTMLComponent htmlC,Component ui) {
        int index=keyStr.indexOf(' ');
        boolean isFirstKey=true; // Keeps track of whether this is the first key we are adding (In order to override XHTML accesskey or failed multiple assignments)
        while (index!=-1) { // Handle multiple/fallback access keys
            String key=keyStr.substring(0,index).trim();
            keyStr=keyStr.substring(index+1);
            if (!processAccessKey(key, htmlC, ui, isFirstKey)) {
                return false; // If failing to set one of the keys - we return a failure
            }
            isFirstKey=false;
            index=keyStr.indexOf(' ');
        }
        return processAccessKey(keyStr, htmlC, ui, isFirstKey);
    }

    /**
     * Tries to assign the given key string as an access key to the specified component
     * The key string given here is a single key
     *
     * @param keyStr The string representing the key (either a character, a unicode escape sequence or a special key name
     * @param htmlC The HTMLComponent
     * @param ui The component to set the access key on
     * @param override If true overrides other keys assigned previously for this component
     * @return true if successful, false otherwise
     */
    private boolean processAccessKey(String keyStr,HTMLComponent htmlC,Component ui,boolean override) {
        if (keyStr.startsWith("\\")) { // Unicode escape sequence, may be used to denote * and # which technically are illegal as values
            try {
                int keyCode=Integer.parseInt(keyStr.substring(1), 16);
                htmlC.addAccessKey((char)keyCode, ui, override);
                return true;
            } catch (NumberFormatException nfe) {
                return false;

            }
        } else if (keyStr.length()==1) {
            htmlC.addAccessKey(keyStr.charAt(0), ui, override);
            return true;
        } else { //special key shortcut
            if (specialKeys!=null) {
                Integer key=(Integer)specialKeys.get(keyStr);
                if (key!=null) {
                    htmlC.addAccessKey(key.intValue(), ui, override);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     *  Returns a border for a specific side of the component
     *
     * @param styleAttributes The style attributes element containing the border directives
     * @param ui The component we want to set the border on
     * @param location One of Component.TOP/BOTTOM/LEFT/RIGHT
     * @return
     */
    Border createBorder(CSSElement styleAttributes,Component ui,int location,int styles,int type) {
        int borderStyle=styleAttributes.getAttrVal(BORDER_OUTLINE_PROPERTIES[type][STYLE]+location);
        if ((borderStyle==-1) || (borderStyle==BORDER_STYLE_NONE)) {
            return null;
        }

        int borderColor=styleAttributes.getAttrVal(BORDER_OUTLINE_PROPERTIES[type][COLOR]+location);
        int borderWidth=styleAttributes.getAttrLengthVal(BORDER_OUTLINE_PROPERTIES[type][WIDTH]+location, ui,0);
        if (borderWidth==-1) {
            borderWidth=CSSElement.BORDER_DEFAULT_WIDTH; // Default value
        }
        if (type==OUTLINE) {
            location=-1; //all
        }

        if ((styles & STYLE_SELECTED)!=0) {
            incPadding(ui.getSelectedStyle(), location, borderWidth);
        }
        if ((styles & STYLE_UNSELECTED)!=0) {
            incPadding(ui.getUnselectedStyle(), location, borderWidth);
        }
        if ((styles & STYLE_PRESSED)!=0) {
            incPadding(((HTMLLink)ui).getPressedStyle(), location, borderWidth);
        }
        Border border=null;
        if ((borderColor==-1) && (borderStyle>=BORDER_STYLE_GROOVE)) {
            borderColor=DEFAULT_3D_BORDER_COLOR;
        }
        switch(borderStyle) {
            case BORDER_STYLE_SOLID:
                if (borderColor==-1) {
                    border=Border.createLineBorder(borderWidth);
                } else {
                    border=Border.createLineBorder(borderWidth, borderColor);
                }
                break;
            case BORDER_STYLE_DOUBLE:
                if (borderColor==-1) {
                    border=Border.createDoubleBorder(borderWidth);
                } else {
                    border=Border.createDoubleBorder(borderWidth,borderColor);
                }
                break;
            case BORDER_STYLE_GROOVE:
                border=Border.createGrooveBorder(borderWidth, borderColor);
                break;
            case BORDER_STYLE_RIDGE:
                border=Border.createRidgeBorder(borderWidth, borderColor);
                break;
            case BORDER_STYLE_INSET:
                border=Border.createInsetBorder(borderWidth, borderColor);
                break;
            case BORDER_STYLE_OUTSET:
                border=Border.createOutsetBorder(borderWidth, borderColor);
                break;
            case BORDER_STYLE_DOTTED:
                if (borderColor==-1) {
                    border=Border.createDottedBorder(borderWidth);
                } else {
                    border=Border.createDottedBorder(borderWidth,borderColor);
                }
                break;
            case BORDER_STYLE_DASHED:
                if (borderColor==-1) {
                    border=Border.createDashedBorder(borderWidth);
                } else {
                    border=Border.createDashedBorder(borderWidth,borderColor);
                }

                break;
        }

        return border;
    }

    private void incPadding(Style style,int location,int padding) {
        if (location==-1) {
            int pad[] = new int[4];
            for(int i=Component.TOP;i<=Component.RIGHT;i++) {
                pad[i]=style.getPadding(i)+padding;
            }
            style.setPadding(pad[Component.TOP], pad[Component.BOTTOM], pad[Component.LEFT], pad[Component.RIGHT]);
        } else {
            style.setPadding(location, style.getPadding(location)+padding);
        }
    }

    /**
     * Omits quotes of all kinds if they exist in the string
     * 
     * @param str The string to check
     * @return A quoteless string
     */
    static String omitQuotesIfExist(String str) {
            if (str==null) {
                return null;
            }
            if (((str.charAt(0)=='\"') || (str.charAt(0)=='\'')) && (str.length()>=2)) {
                str=str.substring(1, str.length()-1); // omit quotes from both sides
            }
            return str;
    }

    /**
     * Sets the font of the component to the closest font that can be found according to the specified properties
     * Note that system fonts will be matched only with system fonts and same goes for bitmap fonts
     *
     * @param htmlC The HTMLComponent this component belongs to (For the available bitmap fonts table)
     * @param cmp The component to work on
     * @param fontFamily The font family
     * @param fontSize The font size in pixels
     * @param fontStyle The font style - either Font.STYLE_PLAIN or Font.STYLE_ITALIC
     * @param fontWeight The font weight - either Font.STYLE_PLAIN ot Font.STYLE_BOLD
     */
    private void setMatchingFont(HTMLComponent htmlC, Component cmp,String fontFamily,int fontSize,int fontStyle,int fontWeight,CSSElement selector) {

        int styles=getApplicableStyles(cmp, selector);
        Font curFont=cmp.getUnselectedStyle().getFont();

        if (((styles & STYLE_SELECTED)!=0) && ((styles & STYLE_UNSELECTED)==0)) { // Focus
            curFont=cmp.getSelectedStyle().getFont();
        }
        if ((styles & STYLE_PRESSED)!=0) { // Active
            curFont=((HTMLLink)cmp).getPressedStyle().getFont();
        }

            int curSize=0;
            boolean isBold=false;
            boolean isItalic=false;
            String curFamily=null;
            if (curFont.getCharset()==null) { //system font
                // The family string in system fonts is just used to index the font in the matchingFonts cache hashtable
                switch (curFont.getFace()) {
                    case Font.FACE_SYSTEM:
                        curFamily="system";
                        break;
                    case Font.FACE_PROPORTIONAL:
                        curFamily="proportional";
                        break;
                    default:
                        curFamily="monospace";
                }
                curSize=curFont.getHeight()-2; // Font height is roughly 2-3 pixels above the font size, and is the best indicator we have to what the system font size is
                isBold=((curFont.getStyle() & Font.STYLE_BOLD)!=0);
                isItalic=((curFont.getStyle() & Font.STYLE_ITALIC)!=0);
            } else { // bitmap font
                HTMLFont hFont=htmlC.getHTMLFont(curFont);
                
                if (hFont!=null) {
                    curSize=hFont.getSize();
                    isBold=hFont.isBold();
                    isItalic=hFont.isItalic();
                    curFamily=hFont.getFamily();
                }
            }

            if (((fontFamily!=null) && (curFamily!=null) && (!fontFamily.equalsIgnoreCase(curFamily))) ||
                (fontSize!=curSize) ||
                ((isBold)!=(fontWeight==Font.STYLE_BOLD)) ||
                ((isItalic)!=(fontWeight==Font.STYLE_ITALIC))) { // This checks if there's a need to set the font, or if the current font matches the properties of the current one

                // Set the unspecified attributes of the requested font to match those of the current one
                    if ((fontFamily==null) && (curFamily!=null)) {
                        fontFamily=curFamily.toLowerCase();
                    }
                    if (fontSize==-1) {
                        fontSize=curSize;
                    }
                    if (fontStyle==-1) {
                        if (isItalic) {
                            fontStyle=Font.STYLE_ITALIC;
                        } else {
                            fontStyle=0;
                        }
                    }
                    if (fontWeight==-1) {
                        if (isBold) {
                            fontWeight=Font.STYLE_BOLD;
                        } else {
                            fontWeight=0;
                        }
                    }

                    String fontKey=fontFamily+"."+fontSize+"."+fontStyle+"."+fontWeight;
                    Object obj=matchingFonts.get(fontKey);
                    if (obj!=null) {
                        Font font=(Font)obj;
                        setFontForStyles(styles, cmp, font);
                        return;
                    }

                    Font font=null;
                    if (curFont.getCharset()==null) { //system font
                        int systemFontSize=curFont.getSize();
                        if (fontSize>curSize) { //bigger font
                            if (systemFontSize==Font.SIZE_SMALL) {
                                systemFontSize=Font.SIZE_MEDIUM;
                            } else if (systemFontSize==Font.SIZE_MEDIUM) {
                                systemFontSize=Font.SIZE_LARGE;
                            }
                        } else if (fontSize<curSize) { //smaller font
                            if (systemFontSize==Font.SIZE_LARGE) {
                                systemFontSize=Font.SIZE_MEDIUM;
                            } else if (systemFontSize==Font.SIZE_MEDIUM) {
                                systemFontSize=Font.SIZE_SMALL;
                            }
                        }
                        font=Font.createSystemFont(curFont.getFace(), fontStyle+fontWeight, systemFontSize);
                    } else {
                        font=htmlC.getClosestFont(fontFamily, fontSize, fontStyle,fontWeight);
                    }
                    if (font!=null) {
                        matchingFonts.put(fontKey, font);
                        setFontForStyles(styles, cmp, font);
                    }
            }

    }

    private void setFontForStyles(int styles,Component cmp,Font font) {
        if ((styles & STYLE_UNSELECTED)!=0) {
            cmp.getUnselectedStyle().setFont(font);
        }
        if ((styles & STYLE_SELECTED)!=0) {
            cmp.getSelectedStyle().setFont(font);
        }
        if ((styles & STYLE_PRESSED)!=0) {
            ((HTMLLink)cmp).getPressedStyle().setFont(font);
        }
        cmp.setShouldCalcPreferredSize(true);

    }

    /**
     * Extracts a url from a CSS URL value
     *
     * @param cssURL the CSS formatted URL - url(someurl)
     * @return A regular URL - someurl
     */
    static String getCSSUrl(String cssURL) {
        if ((cssURL!=null) && (cssURL.toLowerCase().startsWith("url("))) {
            int index=cssURL.indexOf(')');
            if (index!=-1) {
                cssURL=cssURL.substring(4,index);
                cssURL=cssURL.trim(); // According to the CSS spec, a URL can have white space before/after the quotes
                cssURL=omitQuotesIfExist(cssURL);
                return cssURL;
            }
        }
        return null;
    }

    ///////////////////
    // Methods relevant to CSS2 only (not WCSS)
    ///////////////////

    /**
     * Returns a quote label with the proper client property
     *
     * @param open true if this is an opening quote, false otherwise
     * @return The quote label
     */
    private Label getQuote(boolean open) {
        Label quoteLabel=new Label("\"");
        quoteLabel.putClientProperty(HTMLComponent.CLIENT_PROPERTY_QUOTE, new Integer(open?0:1)); // 0 is open quote, 1 is closed quote (both stand for primary quotes - see HTMLComponent.addQuote)
        return quoteLabel;

    }

    /**
     * Evaluates a CSS content property expression and returns the matching label component
     *
     * @param htmlC The HTMLComponent
     * @param exp The expression to evaluate
     * @param element The element this content property
     * @param selector The CSS selector that includes the content property (mainly for error messages)
     * @return A label representing the evaluated expression or null if not found
     */
    private Label evalContentExpression(HTMLComponent htmlC,String exp,HTMLElement element,CSSElement selector) {
        if (exp.length()!=0) {
            if (exp.startsWith("counter(")) {
                exp=exp.substring(8);
                int index=exp.indexOf(")");
                if (index!=-1) {
                    return new Label(""+htmlC.getCounterValue(exp.substring(0,index)));
                }
            } else if (exp.startsWith("attr(")) {
                exp=exp.substring(5);
                int index=exp.indexOf(")");
                if (index!=-1) {
                    String attr=exp.substring(0,index);
                    String attrValue=element.getAttribute(attr);
                    return new Label(attrValue==null?"":attrValue);
                }
            } else if (exp.equals("open-quote")) {
                return getQuote(true);
            } else if (exp.equals("close-quote")) {
                return getQuote(false);
            } else if (exp.startsWith("url(")) {
                String url=getCSSUrl(exp);
                Label imgLabel=new Label();
                if (htmlC.showImages) {
                    if (htmlC.getDocumentInfo()!=null) {
                        htmlC.getThreadQueue().add(imgLabel,htmlC.convertURL(url));
                    } else {
                        if (DocumentInfo.isAbsoluteURL(url)) {
                            htmlC.getThreadQueue().add(imgLabel,url);
                        } else {
                            if (htmlC.getHTMLCallback()!=null) {
                                htmlC.getHTMLCallback().parsingError(HTMLCallback.ERROR_NO_BASE_URL, selector.getTagName() , selector.getAttributeName(new Integer(CSSElement.CSS_CONTENT)),url,"Ignoring image file referred in a CSS file/segment ("+url+"), since page was set by setBody/setHTML/setDOM so there's no way to access relative URLs");
                            }
                        }
                    }
                }
                return imgLabel;
            }
        }
        return null;
    }

    /**
     * Handles a CSS content property
     *
     * @param element The element this content property
     * @param selector The CSS selector that includes the content property
     * @param htmlC The HTMLComponent
     */
    private void handleContentProperty(HTMLElement element, CSSElement selector, HTMLComponent htmlC) {
        boolean after=((selector.getSelectorPseudoClass() & CSSElement.PC_AFTER)!=0);
        String content=selector.getAttributeById(CSSElement.CSS_CONTENT);
        if (content!=null) { // if there's no content, we don't add anything
            Component cmp=after?(Component)element.getUi().lastElement():(Component)element.getUi().firstElement();
            Component styleCmp=cmp;
            Container parent=null;
            int pos=0;
            if (cmp instanceof Container) {
                parent=((Container)cmp);
                while ((parent.getComponentCount()>0) && (parent.getComponentAt(after?parent.getComponentCount()-1:0) instanceof Container)) {
                    parent=(Container)parent.getComponentAt(after?parent.getComponentCount()-1:0); // find the actual content
                }

                if (parent.getComponentCount()>0) {
                    pos=after?parent.getComponentCount()-1:0;
                    styleCmp=parent.getComponentAt(pos);
                }
            } else {
                parent=cmp.getParent();
                pos=cmp.getParent().getComponentIndex(cmp);
            }
            if (after) {
                pos++;
            }
            int initPos=pos;

            String str="";
            content=content+" "; // to make sure the last expression is evaluated, note that this will not print an extra space in any case, since it is out of the quotes if any
            boolean segment=false;
            for(int i=0;i<content.length();i++) {
                char c=content.charAt(i);
                Label lbl=null;
                if (c=='"') {
                    segment=!segment;
                    if ((!segment) && (str.length()>0)) {
                        lbl = new Label(str);
                        str="";
                    }
                } else if (CSSParser.isWhiteSpace(c)) {
                    if (segment) {
                        str+=c;
                        lbl = new Label(str);
                    } else if (str.length()>0) {
                        lbl = evalContentExpression(htmlC, str, element, selector);
                        if (lbl==null) { // if we didn't find a match we search for the following expressions which are used to remove added content
                            int removeQuoteType=-1;
                            boolean removeAll=false;
                            if ((str.equals("none")) || (str.equals("normal"))) { // normal/none means remove all content
                                removeAll=true;
                            } else if (str.equals("no-open-quote")) {
                                removeQuoteType=0; // 0 is the quote type for open quote, 1 for closed one
                            } else if (str.equals("no-close-quote")) {
                                removeQuoteType=1;
                            }
                            if ((removeAll) || (removeQuoteType!=-1)) {
                                Vector v=element.getUi();
                                if (v!=null) {
                                    Vector toRemove = new Vector();
                                    for(Enumeration e=v.elements();e.hasMoreElements();) {
                                        Component ui = (Component)e.nextElement();
                                        String conStr=(String)ui.getClientProperty(CLIENT_PROPERTY_CSS_CONTENT);

                                        if  ((conStr!=null) && (((after) && (conStr.equals("a"))) ||
                                                                ((!after) && (conStr.equals("b"))))) {
                                            boolean remove=true;
                                            if (removeQuoteType!=-1) {
                                                Object obj=ui.getClientProperty(HTMLComponent.CLIENT_PROPERTY_QUOTE);
                                                if (obj!=null) {
                                                    int quoteType=((Integer)obj).intValue();
                                                    remove=(quoteType==removeQuoteType);
                                                } else {
                                                    remove=false;
                                                }
                                            }
                                            if (remove) {
                                                parent.removeComponent(ui);
                                                toRemove.addElement(ui);
                                            }
                                        }
                                    }
                                    for(Enumeration e=toRemove.elements();e.hasMoreElements();) {
                                        v.removeElement(e.nextElement());
                                    }
                                }
                                return; //stop processing after removal clauses such as none/normal
                            }
                        }

                    }
                    str="";
                } else {
                    str+=c;
                }
                if (lbl!=null) {
                    if (after) {
                        element.addAssociatedComponent(lbl);
                    } else {
                        element.addAssociatedComponentAt(pos-initPos, lbl);
                    }
                    lbl.setUnselectedStyle(new Style(styleCmp.getUnselectedStyle()));
                    lbl.putClientProperty(CLIENT_PROPERTY_CSS_CONTENT, after?"a":"b");
                    if(parent.getComponentCount() == 0){
                        parent.addComponent(lbl);
                    }else{
                        parent.addComponent(pos, lbl);
                    }
                    pos++;
                    applyStyleToUIElement(lbl, selector,element,htmlC);
                }
            }

        }
    }

}
