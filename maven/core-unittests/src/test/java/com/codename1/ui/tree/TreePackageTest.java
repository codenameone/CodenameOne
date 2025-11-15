package com.codename1.ui.tree;

import com.codename1.components.SpanButton;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
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
        fireAction(leafComponent);
        assertEquals(SimpleModel.LEAF, recordingListener.lastSource);

        tree.expandPath(SimpleModel.PARENT);
        Component parentComponent = tree.findNodeComponent(SimpleModel.PARENT);
        assertTrue(tree.isExpanded(parentComponent));

        Tree.TreeState state = tree.getTreeState();
        tree.collapsePath(SimpleModel.PARENT);
        assertFalse(tree.isExpanded(parentComponent));

        Tree restored = new Tree(model);
        Form otherForm = new Form();
        otherForm.add(restored);
        otherForm.show();
        restored.setTreeState(state);

        Component restoredParent = restored.findNodeComponent(SimpleModel.PARENT);
        assertTrue(restored.isExpanded(restoredParent));
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

    private void fireAction(Component component) {
        if (component instanceof Button) {
            Button button = (Button) component;
            button.pressed();
            button.released(0, 0);
            return;
        }
        Component lead = component.getLeadComponent();
        if (lead != null && lead != component) {
            fireAction(lead);
            return;
        }
        if (component instanceof com.codename1.ui.Container) {
            com.codename1.ui.Container container = (com.codename1.ui.Container) component;
            if (container.getComponentCount() > 0) {
                fireAction(container.getComponentAt(0));
            }
        }
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
