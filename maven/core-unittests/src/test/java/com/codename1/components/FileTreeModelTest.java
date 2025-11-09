package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

class FileTreeModelTest extends UITestBase {

    @FormTest
    void testConstructorWithShowFiles() {
        FileTreeModel model = new FileTreeModel(true);
        assertNotNull(model);
    }

    @FormTest
    void testConstructorWithoutShowFiles() {
        FileTreeModel model = new FileTreeModel(false);
        assertNotNull(model);
    }

    @FormTest
    void testGetChildrenWithNullParent() {
        FileTreeModel model = new FileTreeModel(true);
        Vector children = model.getChildren(null);
        assertNotNull(children);
    }

    @FormTest
    void testIsLeafWithNullArg() {
        FileTreeModel model = new FileTreeModel(true);
        boolean isLeaf = model.isLeaf(null);
        // Root is not a leaf
        assertFalse(isLeaf);
    }

    @FormTest
    void testAddExtensionFilter() {
        FileTreeModel model = new FileTreeModel(true);
        model.addExtensionFilter(".txt");
        // Should not throw exception
        assertNotNull(model);
    }

    @FormTest
    void testAddMultipleExtensionFilters() {
        FileTreeModel model = new FileTreeModel(true);
        model.addExtensionFilter(".txt");
        model.addExtensionFilter(".pdf");
        model.addExtensionFilter(".doc");
        assertNotNull(model);
    }
}
