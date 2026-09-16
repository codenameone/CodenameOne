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
package com.codename1.ui;

import com.codename1.ui.events.PointerEvent;

/// Remembers which component a single top level container's pointer is over, and moves the
/// hover state as it changes.
///
/// This lives in its own class because there are two top level containers and they share no
/// base class that could hold it -- `Form` and `Window` both extend `Container` directly. The
/// first version of hover tracking was private to `Form`, which is exactly why a control in a
/// secondary `Window` could never show a declared hover style: the events reached the window
/// and nothing there recorded them.
///
/// Tracking has to live at this level rather than in `Component` because only the container
/// knows what the pointer LEFT. A component is never told the pointer moved off it; it simply
/// stops being the one underneath.
///
/// Desktop only in practice, because nothing else generates hover events.
class HoverTracker {
    // Capture this before release callbacks: nested dispatch may change the current device.
    // Desktop touch screens still send finger releases, which must never create hover.
    static boolean canHoverOnRelease() {
        Display display = Display.getInstance();
        int type = display.getPointerType();
        return display.isDesktop() && (type == PointerEvent.TYPE_MOUSE
                || type == PointerEvent.TYPE_STYLUS || type == PointerEvent.TYPE_ERASER);
    }

    private Component hovered;
    private Component lastInteractiveScrollHover;
    private Component pointerTarget;
    private int pointerX;
    private int pointerY;

    /// Reports where the pointer now is.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component the pointer resolved to, already passed through
    ///   `LeadUtil.leadParentImpl` by the caller, or **null** when the pointer is over
    ///   nothing -- which is how a desktop port reports the cursor leaving the window, as a
    ///   hover at (-1, -1). Both pieces of state have to be cleared in that case or the last
    ///   component and the last scrollbar thumb stay lit with the cursor elsewhere.
    ///
    /// - `x`: the position of the event
    ///
    /// - `y`: the position of the event
    void pointerOver(Component cmp, int x, int y) {
        if (cmp != null && cmp.isHidden(true)) {
            cmp = null;
        }
        pointerTarget = cmp;
        pointerX = x;
        pointerY = y;
        updateHovered(cmp);
        updateInteractiveScrollHover(cmp, x, y);
    }

    static HoverTracker prepareLeadChange(Component changed) {
        Container root = TopLevelSupport.rootOf(changed);
        HoverTracker tracker = root == null ? null : root.getHoverTracker();
        if (tracker == null || tracker.hovered == null
                || (!isInSubtree(changed, tracker.hovered)
                && !isInSubtree(LeadUtil.leadParentImpl(changed), tracker.hovered))) {
            return null;
        }
        // Release the old hierarchy while it still owns its flags and animations.
        // Keep the pointer coordinates so a stationary pointer can acquire the new lead.
        tracker.updateHovered(null);
        TooltipManager tooltip = TooltipManager.getInstance();
        if (tooltip != null && tracker.pointerTarget != null) {
            tooltip.clearTooltipFor(tracker.pointerTarget);
        }
        return tracker;
    }

    void finishLeadChange(Component changed) {
        Container root = TopLevelSupport.rootOf(changed);
        Component target = root instanceof Form ? ((Form) root).hoverTargetAt(pointerX, pointerY)
                : (root instanceof Window ? ((Window) root).hoverTargetAt(pointerX, pointerY) : null);
        // Reuse normal hit-testing, including focusable-parent capture. Resolve state
        // only; a topology mutation must not dispatch a pointer callback.
        pointerOver(target, pointerX, pointerY);
    }

    // A hover callback can remove its own component without deinitializing the
    // entire form/window. Drop that detached target before scheduling a tooltip.
    void clearDetached(TopLevelContainer owner) {
        if ((hovered != null && hovered.getTopLevelContainer() != owner) //NOPMD CompareObjectsWithEquals
                || (lastInteractiveScrollHover != null
                && lastInteractiveScrollHover.getTopLevelContainer() != owner)) { //NOPMD CompareObjectsWithEquals
            pointerOver(null, -1, -1);
        }
    }

    void clearFor(Component removed) {
        if (isInSubtree(removed, hovered) || isInSubtree(removed, lastInteractiveScrollHover)) {
            pointerOver(null, -1, -1);
        }
    }

    static boolean isInSubtree(Component root, Component target) {
        return target != null && (root == target //NOPMD CompareObjectsWithEquals
                || root instanceof Container && ((Container) root).contains(target));
    }

    boolean isOver(Component cmp) {
        return cmp != null && hovered == LeadUtil.leadComponentImpl(cmp); //NOPMD CompareObjectsWithEquals
    }

    private void updateHovered(Component cmp) {
        // The flag goes on the lead COMPONENT, not on the lead parent the pointer resolved
        // to, because the lead component is what Component.getStyle() consults: a component
        // inside a lead hierarchy returns out of the lead branch after asking
        // lead.isHovered(), and never reaches the plain hover check below it. Marking the
        // parent therefore left every MultiButton, SpanButton and toolbar command container
        // unable to show a hover style its theme declared.
        //
        // This is the rule the pressed state already follows -- LeadUtil.pointerPressed
        // delivers to leadComponentImpl(cmp) and getStyle() asks lead.isPressedStyle().
        // leadComponentImpl answers the component itself when there is no lead, so an
        // ordinary component is unaffected.
        Component target = cmp == null ? null : LeadUtil.leadComponentImpl(cmp);
        // Preferred-size collapse can precede layout, leaving stale hit-test bounds.
        if (cmp != null && (cmp.isHidden(true) || (target != null && target.isHidden(true)))) {
            target = null;
        }
        // Identity is the question being asked -- whether this is the same component
        // instance the pointer was already over -- so equals() would be wrong here as well
        // as slower.
        if (hovered == target) { //NOPMD CompareObjectsWithEquals
            return;
        }
        if (hovered != null) {
            repaintLeadParent(hovered);
            hovered.setHovered(false);
        }
        hovered = target;
        if (target != null) {
            target.setHovered(true);
            repaintLeadParent(target);
        }
    }

    /// Component.setHovered repaints the component whose flag moved, which is enough for an
    /// ordinary component and not enough for a lead hierarchy: the style change is read by
    /// the lead PARENT and by every sibling under it, none of which were asked to repaint.
    private static void repaintLeadParent(Component cmp) {
        Component parent = LeadUtil.leadParentImpl(cmp);
        if (parent != null && parent != cmp) { //NOPMD CompareObjectsWithEquals
            parent.repaint();
        }
    }

    /// Routes a hover to the nearest scrollable ancestor of the hovered component so an
    /// interactive (desktop) scrollbar can highlight its thumb, and clears the highlight on
    /// the previously hovered scrollable. Inert unless interactive scrollbars are enabled.
    private void updateInteractiveScrollHover(Component cmp, int x, int y) {
        if (cmp != null && !cmp.getUIManager().getLookAndFeel().isInteractiveScroll()) {
            return;
        }
        if (cmp == null && lastInteractiveScrollHover == null) {
            return;
        }
        Component scrollable = cmp;
        while (scrollable != null && !scrollable.isScrollableY() && !scrollable.isScrollableX()) {
            scrollable = scrollable.getParent();
        }
        if (lastInteractiveScrollHover != null && lastInteractiveScrollHover != scrollable) { //NOPMD CompareObjectsWithEquals
            lastInteractiveScrollHover.clearInteractiveScrollHover();
        }
        if (scrollable != null) {
            scrollable.updateInteractiveScrollHover(x, y);
        }
        lastInteractiveScrollHover = scrollable;
    }
}
