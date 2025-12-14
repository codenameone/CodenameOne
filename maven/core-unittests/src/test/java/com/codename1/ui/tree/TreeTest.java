package com.codename1.ui.tree;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import org.junit.jupiter.api.Assertions;

import java.util.Vector;

public class TreeTest extends UITestBase {

    @FormTest
    public void testStringArrayTreeModel() {
        Tree tree = new Tree(); // Default constructor uses StringArrayTreeModel
        TreeModel model = tree.getModel();
        Assertions.assertNotNull(model);
        Assertions.assertTrue(model.getClass().getName().contains("StringArrayTreeModel"));

        // Verify root children
        Vector rootChildren = model.getChildren(null);
        Assertions.assertNotNull(rootChildren);
        Assertions.assertEquals(3, rootChildren.size()); // "Colors", "Letters", "Numbers"
        Assertions.assertTrue(rootChildren.contains("Colors"));

        // Verify leaf check
        Assertions.assertFalse(model.isLeaf("Colors"));

        // Verify children of "Colors"
        Vector colorsChildren = model.getChildren("Colors");
        Assertions.assertNotNull(colorsChildren);
        Assertions.assertEquals(3, colorsChildren.size()); // "Red", "Green", "Blue"
        Assertions.assertTrue(colorsChildren.contains("Red"));

        Assertions.assertTrue(model.isLeaf("Red"));
        Assertions.assertEquals(0, model.getChildren("Red").size());
    }
}
