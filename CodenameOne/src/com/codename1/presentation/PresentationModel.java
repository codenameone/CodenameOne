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
import java.util.List;

/// The editable, undoable facade of a `Presentation`, the way `DocumentModel` is for a word
/// processing document.
///
/// Every edit replaces the immutable presentation and records the one it replaced, so undo
/// and redo are exact. Edits between `beginGroup` and `endGroup` undo as one step -- a drag
/// that moves a shape through a hundred positions is one undo. Listeners hear every change,
/// which is how a `SlideView` and its thumbnails follow the model.
///
/// ```java
/// PresentationModel model = new PresentationModel(new Presentation());
/// String slideId = model.addSlide(-1);
/// model.addElement(0, ShapeElement.textBox(model.newId('e'), 60, 40, 840, 90,
///         TextFrame.of("t", "Quarterly review", TextStyle.DEFAULT.withSize(40), "center")));
/// model.undo();
/// ```
public final class PresentationModel {
    private Presentation presentation;
    private final List<Presentation> undo = new ArrayList<Presentation>();
    private final List<Presentation> redo = new ArrayList<Presentation>();
    private final List<PresentationListener> listeners = new ArrayList<PresentationListener>();
    private int undoLimit = 100;
    private int groupDepth;
    private boolean groupRecorded;

    /// A model of an empty presentation.
    public PresentationModel() {
        this(new Presentation());
    }

    /// A model of a presentation.
    ///
    /// #### Parameters
    ///
    /// - `presentation`: the presentation, not null
    public PresentationModel(Presentation presentation) {
        if (presentation == null) {
            throw new IllegalArgumentException("presentation");
        }
        this.presentation = presentation;
    }

    /// The current presentation.
    ///
    /// #### Returns
    ///
    /// the presentation
    public Presentation getPresentation() {
        return presentation;
    }

    /// Replaces the presentation as one undoable edit.
    ///
    /// #### Parameters
    ///
    /// - `value`: the new presentation
    public void setPresentation(Presentation value) {
        if (value == null) {
            throw new IllegalArgumentException("presentation");
        }
        if (value.equals(presentation) && value.nextId == presentation.nextId) {
            return;
        }
        if (groupDepth == 0 || !groupRecorded) {
            push(undo, presentation);
            groupRecorded = groupDepth > 0;
        }
        redo.clear();
        replace(value);
    }

    /// Replaces the presentation and forgets the undo history, for opening another file.
    ///
    /// #### Parameters
    ///
    /// - `value`: the presentation
    public void reset(Presentation value) {
        if (value == null) {
            throw new IllegalArgumentException("presentation");
        }
        undo.clear();
        redo.clear();
        replace(value);
    }

    private void replace(Presentation value) {
        Presentation before = presentation;
        presentation = value;
        PresentationListener[] l = listeners.toArray(new PresentationListener[listeners.size()]);
        for (PresentationListener listener : l) {
            listener.presentationChanged(this, before, value);
        }
    }

    private void push(List<Presentation> stack, Presentation p) {
        stack.add(p);
        while (stack.size() > undoLimit) {
            stack.remove(0);
        }
    }

    /// Starts a group of edits that undo as one. Groups nest.
    public void beginGroup() {
        if (groupDepth == 0) {
            groupRecorded = false;
        }
        groupDepth++;
    }

    /// Ends a group started by `beginGroup`.
    public void endGroup() {
        if (groupDepth == 0) {
            throw new IllegalStateException("endGroup without beginGroup");
        }
        groupDepth--;
    }

    /// True when there is an edit to undo.
    ///
    /// #### Returns
    ///
    /// true when undo is possible
    public boolean canUndo() {
        return !undo.isEmpty();
    }

    /// True when there is an undone edit to redo.
    ///
    /// #### Returns
    ///
    /// true when redo is possible
    public boolean canRedo() {
        return !redo.isEmpty();
    }

    /// Undoes the last edit or group.
    ///
    /// #### Returns
    ///
    /// false when there was nothing to undo
    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        push(redo, presentation);
        replace(undo.remove(undo.size() - 1));
        return true;
    }

    /// Redoes the last undone edit.
    ///
    /// #### Returns
    ///
    /// false when there was nothing to redo
    public boolean redo() {
        if (redo.isEmpty()) {
            return false;
        }
        push(undo, presentation);
        replace(redo.remove(redo.size() - 1));
        return true;
    }

    /// Sets how many edits are kept for undo; the default is 100.
    ///
    /// #### Parameters
    ///
    /// - `limit`: at least 1
    public void setUndoLimit(int limit) {
        undoLimit = Math.max(1, limit);
        while (undo.size() > undoLimit) {
            undo.remove(0);
        }
    }

    /// Adds a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addPresentationListener(PresentationListener l) {
        listeners.add(l);
    }

    /// Removes a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removePresentationListener(PresentationListener l) {
        listeners.remove(l);
    }

    /// Reserves an id no slide or element has. Reserving is not an edit.
    ///
    /// #### Parameters
    ///
    /// - `prefix`: the letter the id starts with
    ///
    /// #### Returns
    ///
    /// the id
    public String newId(char prefix) {
        String id = presentation.newId(prefix);
        presentation = presentation.withNextId(presentation.nextId + 1);
        return id;
    }

    /// The number of slides.
    ///
    /// #### Returns
    ///
    /// the count
    public int getSlideCount() {
        return presentation.slides.size();
    }

    /// A slide.
    ///
    /// #### Parameters
    ///
    /// - `index`: the position
    ///
    /// #### Returns
    ///
    /// the slide
    public Slide getSlide(int index) {
        return presentation.slides.get(index);
    }

    /// Adds an empty white slide.
    ///
    /// #### Parameters
    ///
    /// - `index`: the position, or -1 for the end
    ///
    /// #### Returns
    ///
    /// the new slide's id
    public String addSlide(int index) {
        String id = newId('s');
        insertSlide(index, new Slide(id));
        return id;
    }

    /// Inserts a slide.
    ///
    /// #### Parameters
    ///
    /// - `index`: the position, or -1 for the end
    ///
    /// - `slide`: the slide
    public void insertSlide(int index, Slide slide) {
        int at = index < 0 || index > getSlideCount() ? getSlideCount() : index;
        setPresentation(presentation.insert(at, slide));
    }

    /// Replaces a slide.
    ///
    /// #### Parameters
    ///
    /// - `index`: the position
    ///
    /// - `slide`: the slide
    public void setSlide(int index, Slide slide) {
        setPresentation(presentation.withSlide(index, slide));
    }

    /// Removes a slide.
    ///
    /// #### Parameters
    ///
    /// - `index`: the position
    public void removeSlide(int index) {
        setPresentation(presentation.remove(index));
    }

    /// Moves a slide.
    ///
    /// #### Parameters
    ///
    /// - `from`: its position
    ///
    /// - `to`: the position it ends up at
    public void moveSlide(int from, int to) {
        if (from == to) {
            return;
        }
        List<Slide> l = new ArrayList<Slide>(presentation.slides);
        Slide s = l.remove(from);
        l.add(Math.max(0, Math.min(l.size(), to)), s);
        setPresentation(presentation.withSlides(l));
    }

    /// Copies a slide after itself, giving the copy and its elements new ids.
    ///
    /// #### Parameters
    ///
    /// - `index`: the slide
    ///
    /// #### Returns
    ///
    /// the copy's id
    public String duplicateSlide(int index) {
        Slide s = getSlide(index);
        List<SlideElement> copies = new ArrayList<SlideElement>();
        List<Build> builds = new ArrayList<Build>();
        for (SlideElement e : s.elements) {
            SlideElement c = copy(e);
            copies.add(c);
            for (Build b : s.builds) {
                if (b.elementId.equals(e.id)) {
                    builds.add(b.withElement(c.id));
                }
            }
        }
        String id = newId('s');
        insertSlide(index + 1, s.withId(id).withElements(copies).withBuilds(builds));
        return id;
    }

    private SlideElement copy(SlideElement e) {
        if (e instanceof GroupElement) {
            List<SlideElement> kids = new ArrayList<SlideElement>();
            for (SlideElement k : ((GroupElement) e).children) {
                kids.add(copy(k));
            }
            return ((GroupElement) e.withId(newId('e'))).withChildren(kids);
        }
        return e.withId(newId('e'));
    }

    /// Adds an element in front of the others.
    ///
    /// #### Parameters
    ///
    /// - `slide`: the slide index
    ///
    /// - `element`: the element
    public void addElement(int slide, SlideElement element) {
        Slide s = getSlide(slide);
        setSlide(slide, s.insert(s.elements.size(), element));
    }

    /// Replaces an element, found by its id among a slide's top level elements.
    ///
    /// #### Parameters
    ///
    /// - `slide`: the slide index
    ///
    /// - `element`: the new element, with the id of the one it replaces
    ///
    /// #### Returns
    ///
    /// false when the slide has no element with that id
    public boolean replaceElement(int slide, SlideElement element) {
        Slide s = getSlide(slide);
        int i = s.indexOf(element.id);
        if (i < 0) {
            return false;
        }
        setSlide(slide, s.withElement(i, element));
        return true;
    }

    /// Removes an element and its builds.
    ///
    /// #### Parameters
    ///
    /// - `slide`: the slide index
    ///
    /// - `elementId`: the element
    ///
    /// #### Returns
    ///
    /// false when there is no such element
    public boolean removeElement(int slide, String elementId) {
        Slide s = getSlide(slide);
        int i = s.indexOf(elementId);
        if (i < 0) {
            return false;
        }
        setSlide(slide, s.remove(i));
        return true;
    }

    /// Moves an element in the painting order.
    ///
    /// #### Parameters
    ///
    /// - `slide`: the slide index
    ///
    /// - `elementId`: the element
    ///
    /// - `to`: its new position, 0 at the back
    ///
    /// #### Returns
    ///
    /// false when there is no such element
    public boolean moveElement(int slide, String elementId, int to) {
        Slide s = getSlide(slide);
        int i = s.indexOf(elementId);
        if (i < 0) {
            return false;
        }
        List<SlideElement> l = new ArrayList<SlideElement>(s.elements);
        SlideElement e = l.remove(i);
        l.add(Math.max(0, Math.min(l.size(), to)), e);
        setSlide(slide, s.withElements(l));
        return true;
    }

    /// Moves and resizes an element.
    ///
    /// #### Parameters
    ///
    /// - `slide`: the slide index
    ///
    /// - `elementId`: the element
    ///
    /// - `x`: left edge in points
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    ///
    /// #### Returns
    ///
    /// false when there is no such element
    public boolean setBounds(int slide, String elementId, float x, float y, float width,
            float height) {
        SlideElement e = getSlide(slide).find(elementId);
        return e != null && replaceElement(slide, e.withBounds(x, y, width, height));
    }

    /// Rotates an element.
    ///
    /// #### Parameters
    ///
    /// - `slide`: the slide index
    ///
    /// - `elementId`: the element
    ///
    /// - `degrees`: clockwise
    ///
    /// #### Returns
    ///
    /// false when there is no such element
    public boolean setRotation(int slide, String elementId, float degrees) {
        SlideElement e = getSlide(slide).find(elementId);
        return e != null && replaceElement(slide, e.withRotation(degrees));
    }

    /// Replaces a shape's text.
    ///
    /// #### Parameters
    ///
    /// - `slide`: the slide index
    ///
    /// - `elementId`: a `ShapeElement`
    ///
    /// - `blocks`: the new text
    ///
    /// #### Returns
    ///
    /// false when there is no such shape
    public boolean setText(int slide, String elementId, List<Block> blocks) {
        SlideElement e = getSlide(slide).find(elementId);
        if (!(e instanceof ShapeElement)) {
            return false;
        }
        ShapeElement s = (ShapeElement) e;
        TextFrame f = s.text == null ? new TextFrame(blocks) : s.text.withBlocks(blocks);
        return replaceElement(slide, s.withText(f));
    }

    /// Replaces a slide's speaker notes.
    ///
    /// #### Parameters
    ///
    /// - `slide`: the slide index
    ///
    /// - `blocks`: the notes
    public void setNotes(int slide, List<Block> blocks) {
        setSlide(slide, getSlide(slide).withNotes(blocks));
    }

    /// Replaces a slide's transition.
    ///
    /// #### Parameters
    ///
    /// - `slide`: the slide index
    ///
    /// - `transition`: the transition
    public void setTransition(int slide, Transition transition) {
        setSlide(slide, getSlide(slide).withTransition(transition));
    }

    /// Appends a build to a slide's animation.
    ///
    /// #### Parameters
    ///
    /// - `slide`: the slide index
    ///
    /// - `build`: the build
    public void addBuild(int slide, Build build) {
        Slide s = getSlide(slide);
        List<Build> l = new ArrayList<Build>(s.builds);
        l.add(build);
        setSlide(slide, s.withBuilds(l));
    }
}
