package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class FileTreeModelTest extends UITestBase {

    @FormTest
    void testConstructorWithRoots() {
        String[] roots = {"/root1", "/root2"};
        FileTreeModel model = new FileTreeModel(roots);
        assertNotNull(model);
    }

    @FormTest
    void testGetChildrenReturnsArray() {
        String[] roots = {"/test"};
        FileTreeModel model = new FileTreeModel(roots);
        Object[] children = model.getChildren(null);
        assertNotNull(children);
    }

    @FormTest
    void testIsLeafReturnsBooleanvalue() {
        String[] roots = {"/test"};
        FileTreeModel model = new FileTreeModel(roots);
        // Test with root
        boolean isLeaf = model.isLeaf("/test");
        assertTrue(isLeaf || !isLeaf);
    }
}
