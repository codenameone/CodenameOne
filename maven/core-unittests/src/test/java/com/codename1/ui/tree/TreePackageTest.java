package com.codename1.ui.tree;

import com.codename1.components.SpanButton;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

class TreePackageTest extends UITestBase {

    @FormTest
    void treeStateRestoresExpansionAndLeafEvents() {
        SimpleModel model = new SimpleModel();
        Tree tree = new Tree(model);
        Form form = new Form();
        form.add(tree);
        form.show();

        RecordingListener recordingListener = new RecordingListener();
        tree.addLeafListener(recordingListener);

        Component leafComponent = tree.findNodeComponent(SimpleModel.LEAF);
        assertNotNull(leafComponent);
        assertTrue(fireAction(leafComponent));
        flushSerialCalls();
        DisplayTest.flushEdt();
        flushSerialCalls();
        DisplayTest.flushEdt();
        assertEquals(SimpleModel.LEAF, recordingListener.lastSource);

        tree.expandPath(SimpleModel.PARENT);
        flushSerialCalls();
        DisplayTest.flushEdt();
        Tree.TreeState state = tree.getTreeState();
        tree.collapsePath(SimpleModel.PARENT);

        Tree restored = new Tree(model);
        Form otherForm = new Form();
        otherForm.add(restored);
        otherForm.show();

        flushSerialCalls();
        DisplayTest.flushEdt();
        flushSerialCalls();

        restored.setTreeState(state);
        otherForm.revalidate();

        flushSerialCalls();
        DisplayTest.flushEdt();
        flushSerialCalls();
        DisplayTest.flushEdt();

        Component restoredParent = restored.findNodeComponent(SimpleModel.PARENT);
        assertNotNull(restoredParent);
        Component restoredChild = restored.findNodeComponent("Child 1");
        assertNotNull(restoredChild);
    }

    @FormTest
    void multilineModeUsesSpanButtons() {
        SimpleModel model = new SimpleModel();
        Tree tree = new Tree(model);
        Form form = new Form();
        form.add(tree);
        form.show();

        tree.setMultilineMode(true);
        tree.setModel(model);

        Component node = tree.findNodeComponent(SimpleModel.PARENT);
        assertTrue(node instanceof SpanButton);
    }

    private boolean fireAction(Component component) {
        if (component instanceof Button) {
            Button button = (Button) component;
            Form form = button.getComponentForm();
            if (form == null) {
                return false;
            }
            int targetX = button.getAbsoluteX() + Math.max(1, button.getWidth() / 2);
            int targetY = button.getAbsoluteY() + Math.max(1, button.getHeight() / 2);
            implementation.dispatchPointerPressAndRelease(targetX, targetY);
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();
            return true;
        }
        if (component instanceof Container) {
            Container container = (Container) component;
            int count = container.getComponentCount();
            for (int i = 0; i < count; i++) {
                if (fireAction(container.getComponentAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class SimpleModel implements TreeModel {
        static final String PARENT = "Parent";
        static final String LEAF = "Leaf";
        private final Vector<Object> roots;
        private final Vector<Object> children;

        SimpleModel() {
            roots = new Vector<Object>();
            roots.addElement(PARENT);
            roots.addElement(LEAF);
            children = new Vector<Object>();
            children.addElement("Child 1");
            children.addElement("Child 2");
        }

        public Vector getChildren(Object parent) {
            if (parent == null) {
                return roots;
            }
            if (PARENT.equals(parent)) {
                return children;
            }
            return new Vector();
        }

        public boolean isLeaf(Object node) {
            return !PARENT.equals(node);
        }
    }

    private static class RecordingListener implements ActionListener {
        private Object lastSource;

        public void actionPerformed(ActionEvent evt) {
            lastSource = evt.getSource();
        }
    }
}
