package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class FileTreeTest extends UITestBase {

    @FormTest
    void testDefaultConstructor() {
        FileTree tree = new FileTree();
        assertNotNull(tree);
        assertEquals("FileTree", tree.getUIID());
    }

    @FormTest
    void testConstructorWithModel() {
        FileTreeModel model = new FileTreeModel(true);
        FileTree tree = new FileTree(model);
        assertNotNull(tree);
        assertEquals("FileTree", tree.getUIID());
    }

    @FormTest
    void testConstructorWithShowFilesModel() {
        FileTreeModel model = new FileTreeModel(false);
        FileTree tree = new FileTree(model);
        assertNotNull(tree);
    }

    @FormTest
    void testTreeIsNotNull() {
        FileTree tree = new FileTree();
        assertNotNull(tree.getModel());
    }
}
