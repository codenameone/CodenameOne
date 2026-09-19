/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The right-click menu. Codename One has fired the context-menu EVENT for a long time and
 * has never had anything that turns it into a menu.
 */
class ContextMenuTest extends UITestBase {

    private Command named(String name, final List<String> log) {
        return new Command(name) {
            @Override
            public void actionPerformed(ActionEvent evt) {
                log.add(getCommandName());
            }
        };
    }

    @FormTest
    void componentCommandsOpenTheMenuWithNoListener() {
        List<String> log = new ArrayList<String>();
        Label target = new Label("Right click me");
        target.setContextMenuCommands(named("Cut", log), named("Copy", log));

        Form f = new Form("Menu", BoxLayout.y());
        f.add(target);
        f.show();
        DisplayTest.flushEdt();

        assertSame(target, target.resolveContextMenuOwner(),
                "a component with commands handles its own context menu request");
    }

    @FormTest
    void aComponentWithNoCommandsAndNoListenerHandlesNothing() {
        Label target = new Label("Plain");
        Form f = new Form("Menu", BoxLayout.y());
        f.add(target);
        f.show();
        DisplayTest.flushEdt();

        assertNull(target.resolveContextMenuOwner(),
                "nothing to show means the request is not handled, so a long press still"
                        + " reaches the component underneath");
        assertFalse(target.fireContextMenu(10, 10),
                "and fireContextMenu says so, which is what leaves the long press to the"
                        + " component underneath");
    }

    @FormTest
    void aConsumingListenerWinsOverTheCommands() {
        List<String> log = new ArrayList<String>();
        final boolean[] listenerRan = new boolean[1];
        Label target = new Label("Both");
        target.setContextMenuCommands(named("Cut", log));
        target.addContextMenuListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                listenerRan[0] = true;
                evt.consume();
            }
        });

        Form f = new Form("Menu", BoxLayout.y());
        f.add(target);
        f.show();
        DisplayTest.flushEdt();

        assertTrue(target.fireContextMenu(10, 10));
        assertTrue(listenerRan[0], "the listener is asked first");
    }

    @FormTest
    void theNearestAncestorWithEitherAnswers() {
        // A row inside a table that has its own commands must not be overruled by the
        // table's listener declining to consume -- both are resolved in one walk.
        List<String> log = new ArrayList<String>();
        final boolean[] outerListenerRan = new boolean[1];

        Container outer = new Container(BoxLayout.y());
        outer.addContextMenuListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                outerListenerRan[0] = true;
                // deliberately does not consume
            }
        });
        Label inner = new Label("Row");
        inner.setContextMenuCommands(named("Delete row", log));
        outer.add(inner);

        Form f = new Form("Menu", BoxLayout.y());
        f.add(outer);
        f.show();
        DisplayTest.flushEdt();

        assertSame(inner, inner.resolveContextMenuOwner(), "the row's own commands answer");
        assertFalse(outerListenerRan[0],
                "and nothing has climbed to the container to ask it");
    }

    @FormTest
    void theWalkClimbsToAnAncestorWhenTheComponentHasNoMenuOfItsOwn() {
        List<String> log = new ArrayList<String>();
        Container outer = new Container(BoxLayout.y());
        outer.setContextMenuCommands(named("Paste", log));
        Label inner = new Label("No menu of its own");
        outer.add(inner);

        Form f = new Form("Menu", BoxLayout.y());
        f.add(outer);
        f.show();
        DisplayTest.flushEdt();

        assertSame(outer, inner.resolveContextMenuOwner(),
                "a right click on a plain child opens the container's menu");
    }

    @FormTest
    void settingCommandsToNullRemovesTheMenu() {
        List<String> log = new ArrayList<String>();
        Label target = new Label("Toggle");
        target.setContextMenuCommands(named("Cut", log));
        assertNotNull(target.getContextMenuCommands());

        target.setContextMenuCommands((Command[]) null);
        assertNull(target.getContextMenuCommands(), "null removes the menu");

        target.setContextMenuCommands(new Command[0]);
        assertNull(target.getContextMenuCommands(),
                "and so does an empty array -- an empty menu is a rectangle the user has to"
                        + " dismiss to learn it was empty");
    }

    @FormTest
    void theCommandArrayIsCopiedInBothDirections() {
        List<String> log = new ArrayList<String>();
        Command cut = named("Cut", log);
        Command[] given = {cut};
        Label target = new Label("Copy me");
        target.setContextMenuCommands(given);

        given[0] = named("Replaced", log);
        assertSame(cut, target.getContextMenuCommands()[0],
                "mutating the caller's array must not rewrite the menu");

        Command[] read = target.getContextMenuCommands();
        read[0] = named("Replaced again", log);
        assertSame(cut, target.getContextMenuCommands()[0],
                "and mutating what getContextMenuCommands returned must not either");
    }

    @FormTest
    void showRefusesToOpenAnEmptyMenu() {
        Label target = new Label("Anchor");
        Form f = new Form("Menu", BoxLayout.y());
        f.add(target);
        f.show();
        DisplayTest.flushEdt();

        assertNull(ContextMenu.show(target, 1, 1), "no commands opens nothing");
        assertNull(ContextMenu.show(target, 1, 1, (Command[]) null));
        assertNull(ContextMenu.show(null, 1, 1, new Command("X")), "and neither does no anchor");
    }
}
