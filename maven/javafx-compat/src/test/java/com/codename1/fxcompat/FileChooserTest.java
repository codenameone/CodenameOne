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
package com.codename1.fxcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import com.codename1.fxcompat.runtime.FilePicker;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/// The file chooser: what it asks the platform's picker for, what it
/// makes of the answer, and the file it names for saving where no
/// dialog can.
public class FileChooserTest {

    private final List<String> asked = new ArrayList<String>();

    @After
    public void tearDown() {
        FilePicker.setSource(null);
    }

    private void answer(final String path) {
        FilePicker.setSource(new FilePicker.Source() {
            @Override
            public String choose(String accept) {
                asked.add(String.valueOf(accept));
                return path;
            }
        });
    }

    @Test
    public void openingAsksForTheExtensionsOfTheFiltersAndAnswersTheFile() {
        answer("/data/people.xml");
        FileChooser chooser = new FileChooser();
        assertNotNull(chooser.showOpenDialog(null));
        chooser.getExtensionFilters().add(new ExtensionFilter("XML files (*.xml)", "*.xml"));
        chooser.getExtensionFilters().add(new ExtensionFilter("Text", "*.txt", "*.TEXT"));
        File file = chooser.showOpenDialog(null);
        assertEquals("people.xml", file.getName());
        List<File> files = chooser.showOpenMultipleDialog(null);
        assertEquals(1, files.size());
        assertEquals(file, files.get(0));
        // A filter of any file lifts the limit.
        chooser.getExtensionFilters().add(new ExtensionFilter("All", "*.*"));
        chooser.showOpenDialog(null);
        assertEquals("[null, xml,txt,TEXT, xml,txt,TEXT, null]", asked.toString());
    }

    @Test
    public void aUserWhoCancelsIsNull() {
        answer(null);
        FileChooser chooser = new FileChooser();
        assertNull(chooser.showOpenDialog(null));
        assertNull(chooser.showOpenMultipleDialog(null));
    }

    @Test
    public void savingNamesAFileWithoutAsking() {
        answer("/never/asked");
        FileChooser chooser = new FileChooser();
        assertEquals("untitled", chooser.showSaveDialog(null).getName());
        ExtensionFilter xml = new ExtensionFilter("XML", "*.xml");
        ExtensionFilter csv = new ExtensionFilter("CSV", "*.csv");
        chooser.getExtensionFilters().addAll(xml, csv);
        assertEquals("untitled.xml", chooser.showSaveDialog(null).getName());
        chooser.setSelectedExtensionFilter(csv);
        assertSame(csv, chooser.selectedExtensionFilterProperty().get());
        assertEquals("untitled.csv", chooser.showSaveDialog(null).getName());
        chooser.setInitialFileName("people.xml");
        chooser.setInitialDirectory(new File("/data/out"));
        File file = chooser.showSaveDialog(null);
        assertEquals("people.xml", file.getName());
        assertEquals(new File("/data/out"), file.getParentFile());
        assertEquals("[]", asked.toString());
    }

    @Test
    public void propertiesAndTheRulesOfAFilter() {
        FileChooser chooser = new FileChooser();
        assertNull(chooser.getTitle());
        chooser.setTitle("Open");
        assertEquals("Open", chooser.titleProperty().get());
        assertNull(chooser.getInitialDirectory());
        assertNull(chooser.getInitialFileName());
        chooser.initialFileNameProperty().set("a.txt");
        assertEquals("a.txt", chooser.getInitialFileName());
        ExtensionFilter filter = new ExtensionFilter("Images", "*.png", "*.jpg");
        assertEquals("Images", filter.getDescription());
        assertEquals("[*.png, *.jpg]", filter.getExtensions().toString());
        try {
            filter.getExtensions().add("*.gif");
            fail("the extensions of a filter are fixed");
        } catch (UnsupportedOperationException expected) {
            assertEquals(2, filter.getExtensions().size());
        }
        try {
            new ExtensionFilter("", "*.png");
            fail("an empty description");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
        try {
            new ExtensionFilter("None");
            fail("no extension");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
        try {
            new ExtensionFilter(null, "*.png");
            fail("no description");
        } catch (NullPointerException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
