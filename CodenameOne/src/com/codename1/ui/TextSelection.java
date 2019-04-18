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
package com.codename1.ui;

import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.ComponentSelector.ComponentClosure;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionEvent.Type;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.util.EventDispatcher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * Text selection support for Codename One applications.  The class provides a light-weight text selection 
 * implementation, allowing users to select and copy text from a form. 
 * 
 * <h2>Enabling Text Selection</h2>
 * 
 * <p>Text selection needs to be enabled on a per-form basis.</p>
 * <p>
 * <pre>{@code 
 * myForm.getTextSelection().setEnabled(true);
 * }</pre>
 * </p>
 * 
 * <p>If text selection is enabled on a form, then non-editable text fields and text areas will allow text
 * selection by default.  Labels and SpanLabels have text selection disabled by default, but can be enabled using 
 * {@link Label#setTextSelectionEnabled(boolean) }, and {@link SpanLabel#setTextSelectionEnabled(boolean)} respectively.
 * Similarly text selection can be disabled on TextFields and TextAreas using {@link TextArea#setTextSelectionEnabled(boolean) }.</p>
 * 
 * 
 * @author shannah
 * @since 7.0
 */
public class TextSelection {
    
    /**
     * Creates a new TextSelection handler with the given root component.  Package private.  Use {@link Form#getTextSelection() } to obtain
     * an instance of the Form's TextSelection.
     * @param root 
     */
    TextSelection(Component root) {
        this.root = root;
    }

    /**
     * Returns true if text selection is enabled.  Default is false.
     * @return 
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * An interface that can be returned from any Component's {@link Component#getTextSelectionSupport() } method to provide
     * text selection support on that component.
     */
    public static interface TextSelectionSupport {
        
        /**
         * Gets the spans that should be selected for the given bounds.
         * @param sel The TextSelection instance.
         * @param bounds The bounds to check.  Relative to {@link #getSelectionRoot() }
         * @return Spans of text that should be selected.
         */
        public Spans getTextSelectionForBounds(TextSelection sel, Rectangle bounds);
        
        /**
         * Checks if text selection is enabled for this component.
         * @param sel The TextSelection instance.
         * @return True if text selection is enabled.
         */
        public boolean isTextSelectionEnabled(TextSelection sel);
        
        /**
         * Checks if this component can be used to trigger a text selection.  On mobile devices
         * text selection is triggered with a long press over a component.
         * @param sel The TextSelection instance.
         * @return True if text selection can be triggered on this component.
         */
        public boolean isTextSelectionTriggerEnabled(TextSelection sel);
        
        /**
         * Trigger a text selection at a given point.  
         * @param sel The TextSelection instance
         * @param x The x coordinate of the event.  Relative to {@link #getSelectionRoot() }
         * @param y The y coordinate of the event.  Relative to {@link #getSelectionRoot() }
         * @return The span that should be selected by a long press at this point.
         */
        public Span triggerSelectionAt(TextSelection sel, int x, int y);

        /**
         * Returns the text for a given span.
         * @param sel The TextSelection instance.
         * @param span The span describing the range of text that should be returned.
         * @return The text that is contained.
         */
        public String getTextForSpan(TextSelection sel, Span span);
        
    }
    
    /**
     * Trigger types for text selection.
     */
    public static enum TextSelectionTrigger {
        /**
         * TextSelection is triggered by a pointer press and drag.  This is 
         * consistent with desktop mouse text selection behaviour.
         */
        Press,
        
        /**
         * Text selection is triggered by a long press on some text.
         */
        LongPress
    }
    
    /**
     * Gets the default trigger type for text selection.  This will vary by platform.
     * On mobile/touch devices, it will return {@link TextSelectionTrigger#LongPress},
     * and on desktop environments with a mouse, it will return {@link TextSelectionTrigger#Press}.
     * @return The default trigger type for text selection.
     */
    public static TextSelectionTrigger getDefaultTextSelectionTrigger() {
        return Display.impl.isDesktop() ? TextSelectionTrigger.Press : TextSelectionTrigger.LongPress;
    }
    
    
    /**
     * Comparator used for ordering components in left-to-right mode.
     */
    private static final Comparator<Component> LTRComparator = new Comparator<Component>() {
        
        /**
         * We can't just use component's AbsoluteY coordinates for ordering because of scrolling,
         * so we create a scaled coorindate that will order components properly.
         * @param cmp
         * @return 
         */
        private double getScaledY(Component cmp) {
            double y = 0;
            while (cmp != null) {
                double ratio = cmp.getHeight()/(double)Math.max(cmp.getScrollDimension().getHeight(), cmp.getHeight());
                y = ratio * y;
                y += cmp.getY() + cmp.getScrollY();
                cmp = cmp.getParent();
            }
            return y;
        }
        
        @Override
        public int compare(Component o1, Component o2) {
            int x1 = o1.getAbsoluteX();
            int x2 = o2.getAbsoluteX();
            double y1 = getScaledY(o1);
            double y2 = getScaledY(o2);
            
            if (y1 < y2) {
                return -1;
            } else if (y1 > y2) {
                return 1;
            } else { //(y1 == y2) {
                if (x1 < x2) {
                    return -1;
                } else if (x1 > x2) {
                    return 1;
                } else { // x1==x2
                    int w1 = o1.getWidth();
                    int w2 = o2.getWidth();
                    int h1 = o1.getHeight();
                    int h2 = o2.getHeight();
                    
                    // larger goes first
                    if (h1 > h2) {
                        return -1;
                    } else if (h1 < h2) {
                        return 1;
                    } else { // h1==h2
                        if (w1 > w2) {
                            return -1;
                        } else if (w1 > w2) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }
            }
        }
        
    };
    
    /**
     * Comparator used for ordering components in left-to-right mode.
     */
    private static final Comparator<Component> RTLComparator = new Comparator<Component>() {
        /**
         * We can't just use component's AbsoluteY coordinates for ordering because of scrolling,
         * so we create a scaled coorindate that will order components properly.
         * @param cmp
         * @return 
         */
        private double getScaledY(Component cmp) {
            double y = 0;
            while (cmp != null) {
                double ratio = cmp.getHeight()/(double)Math.max(cmp.getScrollDimension().getHeight(), cmp.getHeight());
                y = ratio * y;
                y += cmp.getY() + cmp.getScrollY();
                cmp = cmp.getParent();
            }
            return y;
        }
        @Override
        public int compare(Component o1, Component o2) {
            int x1 = o1.getAbsoluteX();
            int x2 = o2.getAbsoluteX();
            double y1 = getScaledY(o1);
            double y2 = getScaledY(o2);
            
            if (y1 < y2) {
                return -1;
            } else if (y1 > y2) {
                return 1;
            } else { //(y1 == y2) {
                if (x1 < x2) {
                    return 1;
                } else if (x1 > x2) {
                    return -1;
                } else { // x1==x2
                    int w1 = o1.getWidth();
                    int w2 = o2.getWidth();
                    int h1 = o1.getHeight();
                    int h2 = o2.getHeight();
                    
                    // larger goes first
                    if (h1 > h2) {
                        return -1;
                    } else if (h1 < h2) {
                        return 1;
                    } else { // h1==h2
                        if (w1 > w2) {
                            return -1;
                        } else if (w1 > w2) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }
            }
        }
        
    };
    
    /**
     * Gets the selection root for the current text selection.  The selection root will be 
     * the nearest scrollable parent of the component that triggered the text selection.
     * 
     * <p>Note:  All Span coordinates are relative to the selection root</p>
     * @return 
     */
    public Component getSelectionRoot() {
        if (selectionRoot == null) {
            selectionRoot = root;
        }
        return selectionRoot;
    }
    
    /**
     * Creates a new Char box.
     * @param pos The position of the character that this is referencing within its text component.
     * @param x The x coordinate of the box, relative to {@link #getSelectionRoot() }
     * @param y The y coordinate of the box, relative to {@link #getSelectionRoot() }
     * @param w The width of the box.
     * @param h The height of the box.
     * @return 
     */
    public Char newChar(int pos, int x, int y, int w, int h) {
        return new Char(pos, x, y, w, h);
    }
    
    /**
     * Creates a new Char box
     * @param pos The position of the character that this is referencing within its text component.
     * @param bounds The bounds of the box, relative to {@link #getSelectionRoot() }
     * @return 
     */
    public Char newChar(int pos, Rectangle bounds) {
        return newChar(pos, bounds);
    }
    
    /**
     * Encapsulates a box around a single character/glyph in the UI, storing the component
     * that it belongs to, the position of its corresponding character in the component's text,
     * and the bounds of the box, relative to {@link #getSelectionRoot() }.
     */
    public class Char {
        
        /**
         * The bounds of the box, relative to {@link #getSelectionRoot() }
         */
        private Rectangle bounds=new Rectangle();
        
        /**
         * The position of the character in the text model.
         */
        private int pos;
        
        /**
         * Creates a new Char 
         * @param pos The position of the character.
         * @param bounds The bounds of the box, relative to {@link #getSelectionRoot() }
         */
        public Char(int pos, Rectangle bounds) {
            this.pos = pos;
            this.bounds.setBounds(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        }
        
        /**
         * Creates a new Char
         * @param pos THe position of the character.
         * @param x The x-coord of the box, relative to {@link #getSelectionRoot() } 
         * @param y The y-coord of the box, relative to {@link #getSelectionRoot() }
         * @param w The width of the box.
         * @param h The height of the box.
         */
        public Char(int pos, int x, int y, int w, int h) {
            this.pos = pos;
            this.bounds.setBounds(x, y, w, h);

        }
        
        /**
         * Gets the character position.  This can be used by the Component that contains
         * the text to map it back to its model.
         * @return The position of the character.
         */
        public int getPosition() {
            return pos;
        }
        
        public String toString() {
            return "Char{pos:"+pos+", bounds:"+bounds+"}";
        }

        /**
         * Translates the Char box.
         * @param tx Translate x pixels.
         * @param ty Translate y pixels
         * @return A new Char translated.
         */
        public Char translate(int tx, int ty) {
            Char out = new Char(pos, bounds);
            out.bounds.setX(out.bounds.getX() + tx);
            out.bounds.setY(out.bounds.getY() + ty);
            return out;
        }
    }
    
    /**
     * Creates a new Span based on content in the given component.
     * @param component
     * @return A new span
     */
    public Span newSpan(Component component) {
        return new Span(component);
    }
    
    /**
     * Encapsulates a span of text on the screen.  This can only represent
     * a contiguous, single row of characters. 
     */
    public class Span implements Iterable<Char> {

        
        private List<Char> chars = new ArrayList<Char>();
        private Component component;
        private int startPos, endPos;
        private final Rectangle bounds = new Rectangle();
        private boolean boundsDirty = true;
        

        /**
         * Creates a new span for the given component.
         * @param c 
         */
        public Span(Component c) {
            component = c;
        }
        
        /**
         * Gets the start position of the text.
         * @return 
         */
        public int getStartPos() {
            return startPos;
        }
        
        /**
         * Gets th end position of the text.  (exclusive).
         * @return 
         */
        public int getEndPos() {
            return endPos;
        }

        @Override
        public Iterator<Char> iterator() {
            return chars.iterator();
        }
        
        public String toString() {
            return "Span{"+chars+"; Bounds: "+getBounds()+"}";
        }
        
        /**
         * Calculates the bounds of the span based on the characters in the span
         */
        private void calculateBounds() {
            Char first = first();
            if (first != null) {
                
                bounds.setBounds(first.bounds.getX(), first.bounds.getY(), first.bounds.getWidth(), first.bounds.getHeight());
                for (Char c : chars) {
                    bounds.setWidth(c.bounds.getX() + c.bounds.getWidth() - bounds.getX());
                            
                }
                
            } else {
                bounds.setBounds(0, 0, 0, 0);
            }
            boundsDirty = false;
            
        }
        
        /**
         * Adds a character to the span, updating the bounds.
         * @param character 
         */
        public void add(Char character) {
            boundsDirty = true;
            if (chars.isEmpty()) {
                startPos = character.pos;
                endPos = character.pos+1;
            } else {
                startPos = Math.min(startPos, character.pos);
                endPos = Math.max(endPos-1, character.pos)+1;
            }
            chars.add(character);
        }
        
        /**
         * Obtains an intersection span including only the characters that intersect the given rectangle.
         * @param x The x-coord of the intersection box, relative to {@link #getSelectionRoot() }
         * @param y The y-coord of the intersection box to {@link #getSelectionRoot() }
         * @param w The width of the intersection box.
         * @param h The height of the intersection box.
         * @return A new span containing only characters that intersect the given bounds.
         */
        public Span getIntersection(int x, int y, int w, int h) {
            return getIntersection(x, y, w, h, false);
        }
        
        /**
         * Obtains an intersection span including only the characters that intersect the given rectangle.
         * @param x The x-coord of the intersection box, relative to {@link #getSelectionRoot() }
         * @param y The y-coord of the intersection box to {@link #getSelectionRoot() }
         * @param w The width of the intersection box.
         * @param h The height of the intersection box.
         * @param withFlow If true, this will also include any characters that should logically be selected if the 
         * user dragged over the given rectangle.  E.g. If the selection began above the span, and stretches below it,
         * the entire span should be selected (included in the intersection).
         * @return A new span with the intersection.
         */
        public Span getIntersection(int x, int y, int w, int h, boolean withFlow) {
            Span out = new Span(component);
            if (withFlow) {
                if (y < getBounds().getY() && y + h > getBounds().getY() + getBounds().getHeight()/2) {
                    // Selection starts above and covers full height of the span
                    // In this case select the whole line
                    int newX = getBounds().getX();
                    int newW = Math.max(0, getBounds().getWidth());
                    x = newX;
                    w = newW;
                } else if (y < getBounds().getY() && y + h > getBounds().getY() && y+h <= getBounds().getY() + getBounds().getHeight()) {
                    // Selection starts above the span, and vertically ends somewhere in the span.
                    // In this case, we select from the beginning of the line to the span box
                    int newX = getBounds().getX();
                    int newW = Math.max(0, x+w-newX);
                    x = newX;
                    w = newW;
                    
                } else if (y < getBounds().getY() + getBounds().getHeight() && y + h > getBounds().getY() + getBounds().getHeight() + CN.convertToPixels(2)) {
                    // Selection starts inside vertically inside the span, and covers below.
                    // In this case select from selection start to end of line.
                    w = Math.max(0, getBounds().getX() + getBounds().getWidth() - x);
                }
            }
            for (Char c : chars) {
                if (c.bounds.intersects(x, y, w, h)) {
                    out.add(c);
                }
            }
            return out;
        }
        
        /**
         * Obtains an intersection span including only the characters that intersect the given rectangle.
         * @param bounds The bounds of the intersection box, relative to {@link #getSelectionRoot() }
         * @param withFlow If true, this will also include any characters that should logically be selected if the 
         * user dragged over the given rectangle.  E.g. If the selection began above the span, and stretches below it,
         * the entire span should be selected (included in the intersection).
         * @return A new span with the intersection.
         */
        public Span getIntersection(Rectangle bounds, boolean withFlow) {
            int x = bounds.getX();
            int y = bounds.getY();
            int w = bounds.getWidth();
            int h = bounds.getHeight();
            return getIntersection(x, y, w, h, withFlow);
            
        }
        
        /**
         * Obtains an intersection span including only the characters that intersect the given rectangle.
         * @param bounds The bounds of the intersection box, relative to {@link #getSelectionRoot() }
         * @return A new span with the intersection.
         */
        public Span getIntersection(Rectangle bounds) {
            return getIntersection(bounds, false);
        }
        
        /**
         * Gets the char at the given coordinate or null if there isn't a char there.
         * @param x x-coordinate relative to {@link #getSelectionRoot() }
         * @param y y-coordinate relative to {@link #getSelectionRoot() }
         * @return 
         */
        public Char charAt(int x, int y) {
            for (Char c : chars) {
                if (c.bounds.contains(x, y)) {
                    return c;
                }
            }
            return null;
        }
        
        /**
         * Gets the first Char in the span, or null if span is empty.
         * @return The first Char, or null.
         */
        public Char first() {
            if (chars.size() == 0) {
                return null;
            }
            return chars.get(0);
        }
        
        /**
         * Gets the last Char in the span, or null if the span is empty.
         * @return The last Char or null.
         */
        public Char last() {
            if (chars.size() == 0) {
                return null;
            }
            return chars.get(chars.size()-1);
        }
        
        /**
         * Gets the number of Chars in the span.
         * @return 
         */
        public int size() {
            return chars.size();
        }
        
        /**
         * Gets a subspan containing the Chars between start (inclusive), and end (exclusive).
         * @param start The start position of the Char to retrieve.
         * @param end The end position of the Char to retrieve.
         * @return A new span including only the Chars at the given positions.
         */
        public Span subspan(int start, int end) {
            Span out = new Span(component);
            for (Char c : chars) {
                if (c.pos >= start && c.pos < end) {
                    
                    out.add(c);
                }
            }
            return out;
        }
        
        /**
         * Gets the bounds of the span.
         * @return the bounds
         */
        public Rectangle getBounds() {
            if (boundsDirty) {
                calculateBounds();
            }
            return bounds;
        }

        /**
         * Creates a translated span based on this one.
         * @param tx x translation in pixels.
         * @param ty y translation in pixels.
         * @return A new span translated.
         */
        public Span translate(int tx, int ty) {
            Span out = new Span(component);
            out.component = component;
            for (Char c : chars) {
                out.add(c.translate(tx, ty));
            }
            return out;
        }
        
        /**
         * Returns true if the span is empty.
         * @return 
         */
        public boolean isEmpty() {
            return chars.isEmpty();
        }
        
    }
    
    /**
     * Creates a new Spans (a collection of Spans).
     * @return 
     */
    public Spans newSpans() {
        return new Spans();
    }

    private static int getX(Component cmp, Component relativeTo) {
        return cmp.getAbsoluteX() - relativeTo.getAbsoluteX();
    }
    
    private static int getY(Component cmp, Component relativeTo) {
        return cmp.getAbsoluteY() - relativeTo.getAbsoluteY();
    }
    
    /**
     * Encapsulates a collection of Spans.
     */
    public class Spans implements Iterable<Span> {
        private List<Span> spans = new ArrayList<Span>();

        @Override
        public Iterator<Span> iterator() {
            return spans.iterator();
        }
        
        /**
         * Adds all of the non-empty spans in the given spans collection to the current
         * spans collection.
         * @param spans 
         */
        public void add(Spans spans) {
            for (Span span : spans) {
                if (span.isEmpty()) {
                    continue;
                }
                this.spans.add(span);
            }
        }
        
        /**
         * Removes all spans.
         */
        public void clear() {
            spans.clear();
        }

        /**
         * Adds the given span to the collection, if it is non-empty.
         * @param span 
         */
        public void add(Span span) {
            if (!span.isEmpty()) {
                this.spans.add(span);
            }
        }

        /**
         * Gets the first span in the collection.
         * @return 
         */
        public Span first() {
            if (spans.size() > 0) {
                return spans.get(0);
            }
            return null;
        }
        
        /**
         * Gets the last span in the collection.
         * @return 
         */
        public Span last() {
            if (spans.size() > 0) {
                return spans.get(spans.size()-1);
            }
            return null;
        }
        
        /**
         * Gets the text contained in this spans collection.
         * @return 
         */
        public String getText() {
            StringBuilder sb = new StringBuilder();
            Component currCmp = null;
            String lineSep = Display.getInstance().getLineSeparator();
            //int originX = selectionRoot.getAbsoluteX();
            int originY = selectionRoot.getAbsoluteY();
            for (Span span : spans) {
                if (currCmp != span.component) {
                    if (currCmp != null) {
                        if (span.getBounds().getY() > currCmp.getAbsoluteY() + currCmp.getHeight() - originY) {
                            sb.append(lineSep);
                        } else {
                            sb.append("\t");
                        }
                    }
                }
                currCmp = span.component;
                TextSelectionSupport ts = span.component.getTextSelectionSupport();
                if (ts != null) {
                    sb.append(ts.getTextForSpan(TextSelection.this, span));
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Spans{");
            for (Span span : spans) {
                sb.append(span.toString());
            }
            sb.append("}");
            return sb.toString();
        }

        public Spans getIntersection(Rectangle bounds, boolean b) {
            Spans out = new Spans();
            for (Span span : spans) {
                out.add(span.getIntersection(bounds, b));
            }
            return out;
        }

        public boolean isEmpty() {
            for (Span span : spans) {
                if (!span.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        public Char charAt(int x, int y) {
            for (Span span : spans) {
                Char c = span.charAt(x, y);
                if (c != null) {
                    return c;
                }
            }
            return null;
        }
        
        
        public Span spanOfCharAt(int x, int y) {
            for (Span span : spans) {
                Char c = span.charAt(x, y);
                if (c != null) {
                    return span;
                }
            }
            return null;
        }
                
        
        
    }
    
    /**
     * Gets the selected text as a string.
     * @return 
     */
    public String getSelectionAsText() {
        return selectedSpans.getText();
    }
    
   
    private Rectangle tmpRect = new Rectangle();
    
    /**
     * Updates the text selected spans based on the selected bounds.
     */
    public void update() {
        if (selectionRoot == null) {
            selectionRoot = root;
            
        }
        final TreeSet<Component> selectedComponents = new TreeSet<Component>(ltr ? LTRComparator : RTLComparator);
        $("*", selectionRoot).each(new ComponentClosure() {
            @Override
            public void call(Component c) {
                TextSelectionSupport ts = c.getTextSelectionSupport();
                if (ts != null && ts.isTextSelectionEnabled(TextSelection.this)) {
                    selectedComponents.add(c);
                }
            }

        });

        
        Spans spans = selectedSpans;
        spans.clear();
        tmpRect.setBounds(selectedBounds);
        for (Component cmp : selectedComponents) {
            TextSelectionSupport st = cmp.getTextSelectionSupport();
            if (st == null) {
                continue;
            }
            if (isVerticallyCoveredByBounds(cmp, selectedBounds)) {
                // When selecting scrollable components, we need to adjust the bounds
                // so that we get the entire contents - not just the visible viewport
                selectedBounds.setX(getX(cmp, selectionRoot));
                selectedBounds.setY(getY(cmp, selectionRoot));
                selectedBounds.setWidth(Math.max(cmp.getScrollDimension().getWidth(), cmp.getWidth()));
                selectedBounds.setHeight(Math.max(cmp.getScrollDimension().getHeight(), cmp.getHeight()));
                
            }
            spans.add(st.getTextSelectionForBounds(this, selectedBounds));
            // In case selectedBounds was changed we reset
            selectedBounds.setBounds(tmpRect);
        }

    }
    

    private boolean isVerticallyCoveredByBounds(Component cmp, Rectangle bounds) {
        int cmpX = getX(cmp, selectionRoot) + cmp.getScrollX();
        int cmpY = getY(cmp, selectionRoot) + cmp.getScrollY();
        boolean isVerticallyCovered = cmpY >= bounds.getY() && cmpY+cmp.getHeight() <= bounds.getY() + bounds.getHeight();
        if (isVerticallyCovered) {
            return true;
        }
        if (cmp == selectionRoot) {
            return false;
        }
        Container parent = cmp.getParent();
        if (parent != null) {
            return isVerticallyCoveredByBounds(parent, bounds);
        }
        return false;
    }
    
    private boolean shouldCoverToEndOfLine(Span span, Rectangle bounds) {
        int spy = span.getBounds().getY();
        int sph = span.getBounds().getHeight();
        boolean shouldCoverToEndOfLine = spy + 2*sph/3 > bounds.getY() && spy+sph <= bounds.getY() + bounds.getHeight();
        if (shouldCoverToEndOfLine) {
            return true;
        }
        return false;
    }
    
    
    private SelectionMask selectionMask;
    
    /**
     * The listener that handles all of the pointer events to update the selections.
     */
    private ActionListener pressListener = new ActionListener() {
        int startX, startY;
        int startDragHandleX, startDragHandleY;
        Rectangle startSelectedBounds = new Rectangle();
        boolean inSelectionDrag;
        private int ONE_MM = CN.convertToPixels(1);
        @Override
        public void actionPerformed(final ActionEvent evt) {
            if (ignoreEvents || Display.getInstance().isRightMouseButtonDown()) {
                return;
            }
            if (trigger == TextSelectionTrigger.Press) {
                if (evt.getEventType() == ActionEvent.Type.PointerPressed) {
                    selectedBounds.setBounds(-1, -1, 0, 0);
                    update();
                    textSelectionListeners.fireActionEvent(new ActionEvent(TextSelection.this, Type.Change));
                    if (selectionMask != null) {
                        selectionMask.remove();
                        getLayeredPane().remove();
                        selectionMask = null;
                        root.getComponentForm().revalidate();
                    }
                    startX = evt.getX();
                    startY = evt.getY();
                    inSelectionDrag = false;
                    Component cmp = ((Container)root).getComponentAt(startX, startY);
                    if (cmp == null) {
                        return;
                    }
                    selectionRoot = findSelectionRoot(cmp);
                    //System.out.println("SelectionRoot ="+selectionRoot);
                    startX = startX - selectionRoot.getAbsoluteX();
                    startY = startY - selectionRoot.getAbsoluteY();
                    TextSelectionSupport ts = cmp.getTextSelectionSupport();
                    if (ts != null) {
                        if (ts.isTextSelectionEnabled(TextSelection.this) && ts.isTextSelectionTriggerEnabled(TextSelection.this)) {
                            evt.consume();
                            inSelectionDrag = true;
                        }
                    }
                } else if (evt.getEventType() == ActionEvent.Type.PointerDrag) {
                    if (!inSelectionDrag) {
                        return;
                    }
                    evt.consume();





                    int x = evt.getX() - selectionRoot.getAbsoluteX();
                    int y = evt.getY() - selectionRoot.getAbsoluteY();
                    selectedBounds.setBounds(
                            Math.min(startX, x), 
                            Math.min(startY, y), 
                            Math.abs(startX-x), 
                            Math.abs(startY-y)
                    );
                    update();
                    if (selectionMask == null) {
                        selectionMask = new SelectionMask();

                        getLayeredPane().add(selectionMask);

                    }
                    root.getComponentForm().revalidate();
                    if (selectionRoot.isScrollableX() && evt.getX() > selectionRoot.getAbsoluteX() + selectionRoot.getScrollX() + selectionRoot.getWidth() - ONE_MM * 5) {
                        Component.setDisableSmoothScrolling(true);
                        int scrollX = selectionRoot.getScrollX();
                        selectionRoot.setScrollX(selectionRoot.getScrollX() + ONE_MM);
                        if (scrollX != selectionRoot.getScrollX()) {
                            selectionRoot.repaint();
                            CN.callSerially(new Runnable() {
                                public void run() {
                                    Form f = selectionRoot.getComponentForm();
                                    if (f != null) {
                                        f.pointerDragged(evt.getX(), evt.getY());
                                    }
                                }
                            });
                        }
                        Component.setDisableSmoothScrolling(false);
                    } else if (selectionRoot.isScrollableX() && evt.getX() < selectionRoot.getAbsoluteX() + selectionRoot.getScrollX() + ONE_MM * 5) {
                        Component.setDisableSmoothScrolling(true);
                        int scrollX = selectionRoot.getScrollX();
                        selectionRoot.setScrollX(selectionRoot.getScrollX() - ONE_MM);
                        if (scrollX != selectionRoot.getScrollX()) {
                            selectionRoot.repaint();
                            CN.callSerially(new Runnable() {
                                public void run() {
                                    Form f = selectionRoot.getComponentForm();
                                    if (f != null) {
                                        f.pointerDragged(evt.getX(), evt.getY());
                                    }
                                }
                            });
                        }
                        Component.setDisableSmoothScrolling(false);
                    } else if (selectionRoot.isScrollableY() && evt.getY() < selectionRoot.getAbsoluteY() + selectionRoot.getScrollY() + ONE_MM * 5) {
                        Component.setDisableSmoothScrolling(true);
                        int scrollY = selectionRoot.getScrollY();
                        selectionRoot.setScrollY(selectionRoot.getScrollY() - ONE_MM);
                        if (scrollY != selectionRoot.getScrollY()) {
                            selectionRoot.repaint();
                            CN.callSerially(new Runnable() {
                                public void run() {
                                    Form f = selectionRoot.getComponentForm();
                                    if (f != null) {
                                        f.pointerDragged(evt.getX(), evt.getY());
                                    }
                                }
                            });
                        }
                        Component.setDisableSmoothScrolling(false);
                    } else if (selectionRoot.isScrollableY() && evt.getY() > selectionRoot.getAbsoluteY() + selectionRoot.getScrollY() + selectionRoot.getHeight() - ONE_MM * 5) {
                        Component.setDisableSmoothScrolling(true);
                        int scrollY = selectionRoot.getScrollY();
                        selectionRoot.setScrollY(selectionRoot.getScrollY() + ONE_MM);
                        if (scrollY != selectionRoot.getScrollY()) {
                            selectionRoot.repaint();
                            CN.callSerially(new Runnable() {
                                public void run() {
                                    Form f = selectionRoot.getComponentForm();
                                    if (f != null) {
                                        f.pointerDragged(evt.getX(), evt.getY());
                                    }
                                }
                            });
                        }
                        Component.setDisableSmoothScrolling(false);
                    } 
                } else if (evt.getEventType() == ActionEvent.Type.PointerReleased || evt.getEventType() == ActionEvent.Type.DragFinished) {
                    if (inSelectionDrag) {
                        textSelectionListeners.fireActionEvent(new ActionEvent(TextSelection.this, Type.Change));
                        evt.consume();
                    }
                }
            } else {
                // Long press trigger
                if (evt.getSource() instanceof DragHandle) {
                    DragHandle dh = (DragHandle)evt.getSource();
                    
                    if (evt.getEventType() == ActionEvent.Type.PointerPressed) {
                        startX = evt.getX();
                        startY = evt.getY();
                        startDragHandleX = dh.getAbsoluteX();
                        startDragHandleY = dh.getAbsoluteY();
                        startSelectedBounds.setBounds(selectedBounds);
                        inSelectionDrag = true;
                        evt.consume();
                        dh.pointerDragged(evt.getX(), evt.getY());
                        
                    } else if (inSelectionDrag && evt.getEventType() == ActionEvent.Type.PointerDrag) {
                        evt.consume();
                        int offX = evt.getX()-startX;
                        int offY = evt.getY()-startY;
                        if (dh.start) {
                            selectedBounds.setX(startSelectedBounds.getX()+offX);
                            selectedBounds.setWidth(startSelectedBounds.getWidth()-offX);
                            selectedBounds.setY(startSelectedBounds.getY()+offY);
                            selectedBounds.setHeight(startSelectedBounds.getHeight()-offY);
                        } else {
                            selectedBounds.setWidth(startSelectedBounds.getWidth() + offX);
                            selectedBounds.setHeight(startSelectedBounds.getHeight() + offY);
                        }
                        update();
                        root.getComponentForm().revalidate();
                    } else if (inSelectionDrag && evt.getEventType() == ActionEvent.Type.PointerReleased || evt.getEventType() == ActionEvent.Type.DragFinished) {
                        evt.consume();
                        inSelectionDrag = false;
                        update();
                        root.getComponentForm().revalidate();
                        textSelectionListeners.fireActionEvent(new ActionEvent(TextSelection.this, Type.Change));
                    }
                } else {
                    if (evt.getEventType() == ActionEvent.Type.LongPointerPress) {
                        //System.out.println("In long press");
                        Component cmp = ((Container)root).getComponentAt(evt.getX(), evt.getY());
                        if (cmp == null) {
                            return;
                        }
                        TextSelectionSupport st = cmp.getTextSelectionSupport();
                        if (st != null) {

                            if (!st.isTextSelectionEnabled(TextSelection.this) || !st.isTextSelectionTriggerEnabled(TextSelection.this)) {
                                return;
                            }
                            selectionRoot = findSelectionRoot(cmp);
                            int x = evt.getX() - selectionRoot.getAbsoluteX();

                            int y = evt.getY() - selectionRoot.getAbsoluteY();
                            Span selSpan = st.triggerSelectionAt(TextSelection.this, x, y);
                            //System.out.println(selSpan);
                            if (selSpan == null) {
                                return;
                            }
                            evt.consume();
                            Rectangle sel = selSpan.getBounds();
                            selectedBounds.setBounds(sel.getX(), sel.getY(), sel.getWidth(), sel.getHeight());
                            update();
                            textSelectionListeners.fireActionEvent(new ActionEvent(TextSelection.this, Type.Change));
                            if (selectionMask == null) {
                                selectionMask = new SelectionMask();
                                Container layeredPane = getLayeredPane();
                                layeredPane.add(selectionMask);

                            }
                            root.getComponentForm().revalidate();


                        }
                    } else if (evt.getEventType() == ActionEvent.Type.PointerPressed) {
                        Component cmp = ((Container)root).getComponentAt(evt.getX(), evt.getY());
                        if (cmp instanceof DragHandle) {
                            return;
                        }
                        if (selectionMask != null) {
                            if (selectionMask.startHandle.contains(evt.getX(), evt.getY())) {
                                return;
                            }
                            if (selectionMask.endHandle.contains(evt.getX(), evt.getY())) {
                                return;
                            }
                            if (selectionMask.selectionMenu.contains(evt.getX(), evt.getY())) {
                                return;
                            }
                        }
                        selectedBounds.setBounds(-1, -1, 0, 0);
                        update();
                        textSelectionListeners.fireActionEvent(new ActionEvent(TextSelection.this, Type.Change));
                        if (selectionMask != null) {
                            selectionMask.remove();
                            getLayeredPane().remove();
                            selectionMask = null;
                            root.getComponentForm().revalidate();
                        }
                    }
                }
            }
        }
        
    };
    
    /**
     * Enables or disables text selection.
     * @param enabled 
     */
    public void setEnabled(boolean enabled) {
        if (enabled != this.enabled) {
            this.enabled = enabled;
            Component f = root.getComponentForm();
            if (enabled) {
                Form form = f.getComponentForm();
                form.setEnableCursors(true);
                f.addPointerPressedListener(pressListener);
                f.addPointerDraggedListener(pressListener);
                f.addPointerReleasedListener(pressListener);
                f.addDragFinishedListener(pressListener);
                f.addLongPressListener(pressListener);
                Display.impl.initializeTextSelection(this);
            } else {
                f.removePointerPressedListener(pressListener);
                f.removePointerDraggedListener(pressListener);
                f.removePointerReleasedListener(pressListener);
                f.removeDragFinishedListener(pressListener);
                f.addLongPressListener(pressListener);
                Display.impl.deinitializeTextSelection(this);
            }
        }
    }
    
    
    private final Rectangle snappedSelectedBounds = new Rectangle();
    private void updateSnappedSelectedBounds() {
        snappedSelectedBounds.setBounds(selectedBounds.getX(), selectedBounds.getY(), selectedBounds.getWidth(), selectedBounds.getHeight());
        for (Span span : selectedSpans) {
            int x = Math.min(span.getBounds().getX(), snappedSelectedBounds.getX());
            int y = Math.min(span.getBounds().getY() , snappedSelectedBounds.getY());
            int w = Math.max(span.getBounds().getX() + span.getBounds().getWidth(), selectedBounds.getX() + selectedBounds.getWidth()) - x;
            int h = Math.max(span.getBounds().getY() + span.getBounds().getHeight(), selectedBounds.getY() + selectedBounds.getHeight()) - y;
            snappedSelectedBounds.setBounds(x, y, w, h);
        }
    }
    
    private class SelectionMask extends Container {
        private DragHandle startHandle=new DragHandle(true), endHandle=new DragHandle(false);
        private SelectionMenu selectionMenu = new SelectionMenu();
        
        SelectionMask() {
            if (trigger == TextSelectionTrigger.LongPress) {
                addAll(startHandle, endHandle, selectionMenu);
            }
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.getDisplayWidth(), CN.getDisplayHeight());
        }

        
        
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.setColor(0x0000ff);
            int alph = g.getAlpha();
            g.setAlpha(50);
            int tx = g.getTranslateX();
            int ty = g.getTranslateY();
            g.translate(-tx, -ty);
            int originX = selectionRoot.getAbsoluteX();
            int originY = selectionRoot.getAbsoluteY();
            g.translate(originX, originY);
            int clipX = g.getClipX();
            int clipY = g.getClipY();
            int clipW = g.getClipWidth();
            int clipH = g.getClipHeight();
            g.clipRect(selectionRoot.getScrollX(), selectionRoot.getScrollY(), selectionRoot.getWidth(), selectionRoot.getHeight());
            
            for (Span span : selectedSpans) {
                int innerClipX = g.getClipX();
                int innerClipY = g.getClipY();
                int innerClipW = g.getClipWidth();
                int innerClipH = g.getClipHeight();
                clipTo(g, span.component, originX, originY);
                //g.translate(span.component.getAbsoluteX(), span.component.getAbsoluteY());
                g.fillRect(span.getBounds().getX(), span.getBounds().getY(), span.getBounds().getWidth(), span.getBounds().getHeight());
                //g.translate(-span.component.getAbsoluteX(), -span.component.getAbsoluteY());
                g.setClip(innerClipX, innerClipY, innerClipW, innerClipH);
            }
            updateSnappedSelectedBounds();
            //g.drawRect(snappedSelectedBounds.getX(), snappedSelectedBounds.getY(), snappedSelectedBounds.getWidth(), snappedSelectedBounds.getHeight());
            //g.drawRect(selectedBounds.getX(), selectedBounds.getY(), selectedBounds.getWidth(), selectedBounds.getHeight());
            g.setClip(clipX, clipY, clipW, clipH);
            g.translate(-originX, -originY);
            g.translate(tx, ty);
            g.setAlpha(alph);
        }
        
        private void clipTo(Graphics g, Component cmp, int originX, int originY) {
            g.clipRect(cmp.getAbsoluteX() - originX + cmp.getScrollX(), cmp.getAbsoluteY()-originY+cmp.getScrollY(), cmp.getWidth(), cmp.getHeight());
            if (cmp.getParent() != null) {
                clipTo(g, cmp.getParent(), originX, originY);
            }
        }

        @Override
        public void layoutContainer() {
            super.layoutContainer();
            Span first = selectedSpans.first();
            Span last = selectedSpans.last();
            if (first == null || last == null || trigger != TextSelectionTrigger.LongPress) {
                startHandle.setVisible(false);
                endHandle.setVisible(false);
                return;
            }
            int offX = selectionRoot.getAbsoluteX() - getAbsoluteX();
            int offY = selectionRoot.getAbsoluteY() - getAbsoluteY();
            startHandle.setVisible(true);
            endHandle.setVisible(true);
            startHandle.setX(offX + first.getBounds().getX() - startHandle.getPreferredW());
            startHandle.setY(offY + first.getBounds().getY());
            startHandle.setHeight(startHandle.getPreferredH() + first.getBounds().getHeight());
            startHandle.setWidth(startHandle.getPreferredW());
            
            endHandle.setX(offX + last.getBounds().getX() + last.getBounds().getWidth());
            endHandle.setY(offY + last.getBounds().getY());
            endHandle.setWidth(endHandle.getPreferredW());
            endHandle.setHeight(endHandle.getPreferredH() + last.getBounds().getHeight());
            
            int menuW = selectionMenu.getPreferredW();
            int menuH = selectionMenu.getPreferredH();
            int menuX = first.getBounds().getX() + offX;
            if (menuX + menuW > getWidth()) {
                menuX = getWidth() - menuW;
            }
            int menuY = offY + first.getBounds().getY() - menuH;
            if (menuY < 0) {
                menuY = offY + last.getBounds().getY() + last.getBounds().getHeight();
            }
            selectionMenu.setX(menuX);
            selectionMenu.setY(menuY);
            selectionMenu.setWidth(menuW);
            selectionMenu.setHeight(menuH);
            
        }
        
        
        
        
    }
    
    private class SelectionMenu extends Container implements ActionListener  {
        Button copy = new HeavyButton("Copy");
        Button selectAll = new HeavyButton("Select All");
        
        SelectionMenu() {
            $(this).selectAllStyles()
                    .setBgColor(0x111111)
                    .setBgTransparency(255)
                    .setBorder(RoundRectBorder.create().cornerRadius(1));
            $(copy, selectAll).selectAllStyles()
                    .setFgColor(0xffffff);
            addAll(copy, selectAll);
            copy.addActionListener(this);
            selectAll.addActionListener(this);
        }
        
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == selectAll) {
                selectAll();
            } else {
                copy();
            }
        }
    }
    
    private class DragHandle extends Button {
        boolean start;
        int diameter = CN.convertToPixels(4);
        
        DragHandle(boolean start) {
            this.start = start;
            this.setDraggable(true);
            
        }

        @Override
        protected Image getDragImage() {
            return null;
        }

        @Override
        void drawDraggedImage(Graphics g) {
            
        }

        @Override
        protected void drawDraggedImage(Graphics g, Image img, int x, int y) {
            
        }

        @Override
        protected int getDragRegionStatus(int x, int y) {
            return DRAG_REGION_IMMEDIATELY_DRAG_XY;
        }

        
        
        
        
        public int getAbsolutePointerX() {
            return getAbsoluteX() + (start ? getWidth() : 0);
            
        }
        
        public int getAbsolutePointerY() {
            return getAbsoluteY() + (start ? 0 : getHeight() - getPreferredH());
        }
        
        @Override
        protected void initComponent() {
            super.initComponent();
            addPointerPressedListener(pressListener);
            addPointerDraggedListener(pressListener);
            addPointerReleasedListener(pressListener);
            addDragFinishedListener(pressListener);
            addLongPressListener(pressListener);
        }

        @Override
        protected void deinitialize() {
            removePointerPressedListener(pressListener);
            removePointerDraggedListener(pressListener);
            removePointerReleasedListener(pressListener);
            removeDragFinishedListener(pressListener);
            addLongPressListener(pressListener);
            super.deinitialize();
        }
        
        
        
        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(diameter, diameter);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            boolean antialias = g.isAntiAliased();
            g.setAntiAliased(true);
            g.setColor(0x0000ff);
            int x = getX();
            int y = getY() + getHeight() - getPreferredH();
            int w = getWidth();
            int h = getPreferredH();
            if (start) {
                g.fillArc(x, y, w, h, 0, 360);
                g.fillRect(x+w/2, y, w/2, h/2);
                g.drawRect(x+w/2, y, w/2, h/2);
            } else {
                g.fillArc(x, y, w, h, 0, 360);
                g.fillRect(x, y, w/2, h/2);
                g.drawRect(x, y, w/2, h/2);
            }
            g.setAntiAliased(antialias);
        }

        
        
        
        
        
    }
    
    /**
     * Adds a listener to be notified when the text selection changes.
     * @param l 
     */
    public void addTextSelectionListener(ActionListener l) {
        textSelectionListeners.addListener(l);
    }
    
    /**
     * Removes a listener so it no longer is notified when text selection changes.
     * @param l 
     */
    public void removeTextSelectionListener(ActionListener l) {
        textSelectionListeners.removeListener(l);
    }
    
    /**
     * Finds the selection root for a component.  This is generally just the first
     * scrollable component discovered with crawling up the component hierarchy
     * from the given component.
     * @param cmp The component we start with.
     * @return The selection root for a given component.
     */
    public static Component findSelectionRoot(Component cmp) {
        if (cmp.scrollableYFlag() || cmp.scrollableXFlag()) {
            return cmp;
        }
        Container parent = cmp.getParent();
        if (parent == null) {
            return cmp;
        }
        return findSelectionRoot(parent);
    }
    
    
    private Container getLayeredPane() {
        //return root.getComponentForm().getLayeredPane(TextSelection.class, true);
        return root.getComponentForm().getFormLayeredPane(TextSelection.class, true);
    }
    
    /**
     * Copies the current selection to the system clipboard.
     */
    public void copy() {
        //Display.impl.copyToClipboard(getSelectionAsText());
        Display.impl.copySelectionToClipboard(this);
    }
    
    /**
     * Selects all of the selectable text in the TextSelection (generally on the current form).
     */
    public void selectAll() {
        if (selectionMask == null) {
            selectionMask = new SelectionMask();

            getLayeredPane().add(selectionMask);

        }
        selectionRoot = root;
        selectedBounds.setBounds(0, 0, selectionRoot.getWidth(), selectionRoot.getHeight());
        update();
        selectionRoot.getComponentForm().revalidateWithAnimationSafety();
        textSelectionListeners.fireActionEvent(new ActionEvent(TextSelection.this, Type.Change));
    }
    
    /**
     * This flag can be set to cause text selection to ignore pointer events which might cause
     * the selection to get lost or changed.  This is used internally when a context menu is displayed
     * so that clicking on the context menu doesn't cause the current text selection to be lost.
     * @param ignore 
     */
    public void setIgnoreEvents(boolean ignore) {
        ignoreEvents = ignore;
    }
    
    private Component root;
    private boolean ltr=true;
    private final Rectangle selectedBounds = new Rectangle();
    private final Spans selectedSpans = new Spans();
    private boolean enabled;
    private TextSelectionTrigger trigger = getDefaultTextSelectionTrigger();
    private final EventDispatcher textSelectionListeners = new EventDispatcher();
    
    // The nearest scrollable parent of the component that triggered a text
    // selection event. 
    private Component selectionRoot;
    private boolean ignoreEvents;
    
    
    
    
}
