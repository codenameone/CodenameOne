package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class FileTreeTest extends UITestBase {

    @FormTest
    void testDefaultConstructor() {
        FileTree tree = new FileTree();
        assertNotNull(tree);
    }

    @FormTest
    void testConstructorWithRoots() {
        String[] roots = {"/root1", "/root2"};
        FileTree tree = new FileTree(roots);
        assertNotNull(tree);
    }

    @FormTest
    void testConstructorWithModel() {
        String[] roots = {"/test"};
        FileTreeModel model = new FileTreeModel(roots);
        FileTree tree = new FileTree(model);
        assertNotNull(tree);
    }

    @FormTest
    void testGetModelReturnsModel() {
        FileTree tree = new FileTree();
        assertNotNull(tree.getModel());
    }
}
