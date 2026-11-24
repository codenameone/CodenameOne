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
package com.codename1.presentation;

import com.codename1.wordprocessing.Block;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// One slide: a background, elements in painting order, speaker notes, a transition and the
/// builds that animate its elements. Immutable; `with...` methods answer changed copies.
public final class Slide {
    /// The id, unique within the presentation.
    public final String id;

    /// The name of the layout it was made from (`Title Slide`, `Title and Content`...), or
    /// null.
    public final String layout;

    /// The background.
    public final Fill background;

    /// The elements, back to front, never null.
    public final List<SlideElement> elements;

    /// The speaker notes as word processing blocks, never null.
    public final List<Block> notes;

    /// How the slide comes on screen.
    public final Transition transition;

    /// The animation steps, in order, never null.
    public final List<Build> builds;

    /// True when a slide show skips the slide.
    public final boolean hidden;

    /// An empty white slide.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    public Slide(String id) {
        this(id, null, Fill.solid(0xffffff), null, null, null, null, false);
    }

    /// A slide.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id, not empty
    ///
    /// - `layout`: the layout name, or null
    ///
    /// - `background`: the background, or null for white
    ///
    /// - `elements`: the elements, or null
    ///
    /// - `notes`: the speaker notes, or null
    ///
    /// - `transition`: the transition, or null for none
    ///
    /// - `builds`: the builds, or null
    ///
    /// - `hidden`: whether a slide show skips it
    public Slide(String id, String layout, Fill background, List<SlideElement> elements,
            List<Block> notes, Transition transition, List<Build> builds, boolean hidden) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("A slide needs an id");
        }
        this.id = id;
        this.layout = layout;
        this.background = background == null ? Fill.solid(0xffffff) : background;
        this.elements = list(elements);
        this.notes = list(notes);
        this.transition = transition == null ? Transition.DEFAULT : transition;
        this.builds = list(builds);
        this.hidden = hidden;
    }

    private static <T> List<T> list(List<T> in) {
        List<T> out = new ArrayList<T>();
        if (in != null) {
            for (T t : in) {
                if (t != null) {
                    out.add(t);
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    /// The index of an element by id among the top level elements.
    ///
    /// #### Parameters
    ///
    /// - `elementId`: the id
    ///
    /// #### Returns
    ///
    /// the index or -1
    public int indexOf(String elementId) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).id.equals(elementId)) {
                return i;
            }
        }
        return -1;
    }

    /// An element by id, searching inside groups.
    ///
    /// #### Parameters
    ///
    /// - `elementId`: the id
    ///
    /// #### Returns
    ///
    /// the element or null
    public SlideElement find(String elementId) {
        return find(elements, elementId);
    }

    private static SlideElement find(List<SlideElement> list, String elementId) {
        for (SlideElement e : list) {
            if (e.id.equals(elementId)) {
                return e;
            }
            if (e instanceof GroupElement) {
                SlideElement f = find(((GroupElement) e).children, elementId);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    /// The title: the text of the title placeholder, or else of the first element with text.
    ///
    /// #### Returns
    ///
    /// the title, empty when the slide has no text
    public String title() {
        for (SlideElement e : elements) {
            if (e instanceof ShapeElement && ShapeElement.TITLE.equals(((ShapeElement) e).placeholder)) {
                return e.plainText();
            }
        }
        for (SlideElement e : elements) {
            String t = e.plainText();
            if (t.length() > 0) {
                int nl = t.indexOf('\n');
                return nl < 0 ? t : t.substring(0, nl);
            }
        }
        return "";
    }

    /// All the slide's text, element by element.
    ///
    /// #### Returns
    ///
    /// the text
    public String plainText() {
        StringBuilder sb = new StringBuilder();
        for (SlideElement e : elements) {
            String t = e.plainText();
            if (t.length() > 0) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(t);
            }
        }
        return sb.toString();
    }

    /// This slide with other elements.
    ///
    /// #### Parameters
    ///
    /// - `value`: the elements
    ///
    /// #### Returns
    ///
    /// the slide
    public Slide withElements(List<SlideElement> value) {
        return new Slide(id, layout, background, value, notes, transition, builds, hidden);
    }

    /// This slide with one element replaced, inserted or removed.
    ///
    /// #### Parameters
    ///
    /// - `index`: the position
    ///
    /// - `element`: the new element
    ///
    /// #### Returns
    ///
    /// the slide
    public Slide withElement(int index, SlideElement element) {
        List<SlideElement> l = new ArrayList<SlideElement>(elements);
        l.set(index, element);
        return withElements(l);
    }

    /// This slide with an element inserted.
    ///
    /// #### Parameters
    ///
    /// - `index`: the position, 0 to the element count
    ///
    /// - `element`: the element
    ///
    /// #### Returns
    ///
    /// the slide
    public Slide insert(int index, SlideElement element) {
        List<SlideElement> l = new ArrayList<SlideElement>(elements);
        l.add(index, element);
        return withElements(l);
    }

    /// This slide without an element, and without the builds that animate it.
    ///
    /// #### Parameters
    ///
    /// - `index`: the position
    ///
    /// #### Returns
    ///
    /// the slide
    public Slide remove(int index) {
        List<SlideElement> l = new ArrayList<SlideElement>(elements);
        SlideElement gone = l.remove(index);
        List<Build> b = new ArrayList<Build>();
        for (Build x : builds) {
            if (!x.elementId.equals(gone.id)) {
                b.add(x);
            }
        }
        return new Slide(id, layout, background, l, notes, transition, b, hidden);
    }

    /// This slide with another background.
    ///
    /// #### Parameters
    ///
    /// - `value`: the background
    ///
    /// #### Returns
    ///
    /// the slide
    public Slide withBackground(Fill value) {
        return new Slide(id, layout, value, elements, notes, transition, builds, hidden);
    }

    /// This slide with other notes.
    ///
    /// #### Parameters
    ///
    /// - `value`: the notes
    ///
    /// #### Returns
    ///
    /// the slide
    public Slide withNotes(List<Block> value) {
        return new Slide(id, layout, background, elements, value, transition, builds, hidden);
    }

    /// This slide with another transition.
    ///
    /// #### Parameters
    ///
    /// - `value`: the transition
    ///
    /// #### Returns
    ///
    /// the slide
    public Slide withTransition(Transition value) {
        return new Slide(id, layout, background, elements, notes, value, builds, hidden);
    }

    /// This slide with other builds.
    ///
    /// #### Parameters
    ///
    /// - `value`: the builds
    ///
    /// #### Returns
    ///
    /// the slide
    public Slide withBuilds(List<Build> value) {
        return new Slide(id, layout, background, elements, notes, transition, value, hidden);
    }

    /// This slide hidden or shown.
    ///
    /// #### Parameters
    ///
    /// - `value`: true to skip it in a show
    ///
    /// #### Returns
    ///
    /// the slide
    public Slide withHidden(boolean value) {
        return new Slide(id, layout, background, elements, notes, transition, builds, value);
    }

    /// This slide with another id, for copies.
    ///
    /// #### Parameters
    ///
    /// - `value`: the id
    ///
    /// #### Returns
    ///
    /// the slide
    public Slide withId(String value) {
        return new Slide(value, layout, background, elements, notes, transition, builds, hidden);
    }

    /// This slide with another layout name.
    ///
    /// #### Parameters
    ///
    /// - `value`: the layout name
    ///
    /// #### Returns
    ///
    /// the slide
    public Slide withLayout(String value) {
        return new Slide(id, value, background, elements, notes, transition, builds, hidden);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Slide)) {
            return false;
        }
        Slide s = (Slide) o;
        return id.equals(s.id) && (layout == null ? s.layout == null : layout.equals(s.layout))
                && background.equals(s.background) && elements.equals(s.elements)
                && notes.equals(s.notes) && transition.equals(s.transition)
                && builds.equals(s.builds) && hidden == s.hidden;
    }

    @Override
    public int hashCode() {
        return id.hashCode() * 31 + elements.hashCode();
    }

    @Override
    public String toString() {
        return "Slide[" + id + " " + elements.size() + " elements]";
    }
}
