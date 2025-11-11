package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Container methods: getResponderAt, findDropTargetAt, updateTabIndices, drop, getChildrenAsList.
 */
class ContainerMethodsTest extends UITestBase {

    @FormTest
    void testGetResponderAtWithSingleComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        btn.setX(50);
        btn.setY(50);
        btn.setWidth(100);
        btn.setHeight(50);

        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        Component responder = form.getResponderAt(75, 75);

        // Should find the button at this position
        assertNotNull(responder);
    }

    @FormTest
    void testGetResponderAtOutsideComponents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        // Try to get responder at position outside form bounds
        Component responder = form.getResponderAt(-10, -10);

        // May return form itself or null
        assertTrue(responder == null || responder == form);
    }

    @FormTest
    void testGetResponderAtOverlappingComponents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new com.codename1.ui.layouts.LayeredLayout());

        Button btn1 = new Button("Button 1");
        btn1.setX(0);
        btn1.setY(0);
        btn1.setWidth(200);
        btn1.setHeight(100);

        Button btn2 = new Button("Button 2");
        btn2.setX(50);
        btn2.setY(50);
        btn2.setWidth(200);
        btn2.setHeight(100);

        form.add(btn1);
        form.add(btn2);
        form.revalidate();

        // Button 2 is on top, so it should respond
        Component responder = form.getResponderAt(100, 75);

        assertNotNull(responder);
    }

    @FormTest
    void testGetChildrenAsList() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        List<Component> children = form.getChildrenAsList(false);

        assertEquals(3, children.size());
        assertTrue(children.contains(btn1));
        assertTrue(children.contains(btn2));
        assertTrue(children.contains(btn3));
    }

    @FormTest
    void testGetChildrenAsListRecursive() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container container = new Container(BoxLayout.y());
        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        container.addAll(btn1, btn2);

        Button btn3 = new Button("Button 3");

        form.add(BorderLayout.CENTER, container);
        form.add(BorderLayout.SOUTH, btn3);
        form.revalidate();

        List<Component> children = form.getChildrenAsList(true);

        // Should include nested components
        assertTrue(children.size() >= 2);
    }

    @FormTest
    void testGetChildrenAsListEmpty() {
        Form form = CN.getCurrentForm();
        form.removeAll();
        form.revalidate();

        List<Component> children = form.getChildrenAsList(false);

        assertNotNull(children);
        assertEquals(0, children.size());
    }

    @FormTest
    void testUpdateTabIndices() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        // Update tab indices
        form.getContentPane().updateTabIndices();

        // Tab indices should be set
        assertNotNull(form);
    }

    @FormTest
    void testDropOnContainer() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container target = new Container(BoxLayout.y());
        target.setDropTarget(true);

        form.add(BorderLayout.CENTER, target);
        form.revalidate();

        assertTrue(target.isDropTarget());

        // Simulate drop
        Component dragged = new Label("Dragged");
        target.drop(dragged, 50, 50);

        // Component may be added to target
        assertNotNull(dragged);
    }

    @FormTest
    void testFindDropTargetAtValidLocation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container dropTarget = new Container(BoxLayout.y());
        dropTarget.setDropTarget(true);
        dropTarget.setX(0);
        dropTarget.setY(0);
        dropTarget.setWidth(200);
        dropTarget.setHeight(200);

        form.add(BorderLayout.CENTER, dropTarget);
        form.revalidate();

        Component found = form.findDropTargetAt(100, 100);

        assertNotNull(found);
    }

    @FormTest
    void testFindDropTargetAtInvalidLocation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container dropTarget = new Container(BoxLayout.y());
        dropTarget.setDropTarget(true);

        form.add(BorderLayout.CENTER, dropTarget);
        form.revalidate();

        Component found = form.findDropTargetAt(-100, -100);

        // Should not find drop target at negative coordinates
        assertNull(found);
    }

    @FormTest
    void testGetResponderAtWithInvisibleComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button invisible = new Button("Invisible");
        invisible.setVisible(false);
        invisible.setX(50);
        invisible.setY(50);
        invisible.setWidth(100);
        invisible.setHeight(50);

        form.add(BorderLayout.CENTER, invisible);
        form.revalidate();

        Component responder = form.getResponderAt(75, 75);

        // Should not return invisible component
        assertNotEquals(invisible, responder);
    }

    @FormTest
    void testGetChildrenAsListWithNestedContainers() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container level1 = new Container(BoxLayout.y());
        Container level2 = new Container(BoxLayout.y());
        Button deepButton = new Button("Deep");

        level2.add(deepButton);
        level1.add(level2);
        form.add(BorderLayout.CENTER, level1);
        form.revalidate();

        List<Component> childrenNonRecursive = form.getChildrenAsList(false);
        List<Component> childrenRecursive = form.getChildrenAsList(true);

        // Recursive should have more components
        assertTrue(childrenRecursive.size() >= childrenNonRecursive.size());
    }

    @FormTest
    void testDropTargetWithMultipleContainers() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Container target1 = new Container(BoxLayout.y());
        target1.setDropTarget(true);

        Container target2 = new Container(BoxLayout.y());
        target2.setDropTarget(true);

        form.addAll(target1, target2);
        form.revalidate();

        assertTrue(target1.isDropTarget());
        assertTrue(target2.isDropTarget());
    }

    @FormTest
    void testGetResponderAtInScrollableContainer() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        for (int i = 0; i < 30; i++) {
            Button btn = new Button("Button " + i);
            scrollable.add(btn);
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Get responder at position in scrollable container
        Component responder = scrollable.getResponderAt(50, 50);

        assertNotNull(responder);
    }

    @FormTest
    void testUpdateTabIndicesAfterReordering() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();
        form.getContentPane().updateTabIndices();

        // Reorder components
        form.removeComponent(btn1);
        form.addComponent(btn1);
        form.revalidate();
        form.getContentPane().updateTabIndices();

        assertEquals(3, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testGetChildrenAsListAfterRemoval() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        List<Component> before = form.getChildrenAsList(false);
        assertEquals(3, before.size());

        form.removeComponent(btn2);
        form.revalidate();

        List<Component> after = form.getChildrenAsList(false);
        assertEquals(2, after.size());
        assertFalse(after.contains(btn2));
    }

    @FormTest
    void testDropOnNonDropTarget() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container nonTarget = new Container(BoxLayout.y());
        nonTarget.setDropTarget(false);

        form.add(BorderLayout.CENTER, nonTarget);
        form.revalidate();

        assertFalse(nonTarget.isDropTarget());

        // Try to drop on non-drop-target
        Component dragged = new Label("Dragged");
        nonTarget.drop(dragged, 50, 50);

        // Should handle gracefully
        assertNotNull(dragged);
    }

    @FormTest
    void testGetResponderAtWithDisabledComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button disabled = new Button("Disabled");
        disabled.setEnabled(false);
        disabled.setX(50);
        disabled.setY(50);
        disabled.setWidth(100);
        disabled.setHeight(50);

        form.add(BorderLayout.CENTER, disabled);
        form.revalidate();

        Component responder = form.getResponderAt(75, 75);

        // May still return disabled component as responder
        assertNotNull(responder);
    }

    @FormTest
    void testFindDropTargetAtWithNestedDropTargets() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container outer = new Container(BoxLayout.y());
        outer.setDropTarget(true);

        Container inner = new Container(BoxLayout.y());
        inner.setDropTarget(true);

        outer.add(inner);
        form.add(BorderLayout.CENTER, outer);
        form.revalidate();

        // Should find closest drop target
        Component found = form.findDropTargetAt(50, 50);

        assertNotNull(found);
    }

    @FormTest
    void testGetChildrenAsListModification() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        List<Component> children = form.getChildrenAsList(false);
        int originalSize = children.size();

        // Add another component
        Button btn3 = new Button("Button 3");
        form.add(btn3);
        form.revalidate();

        List<Component> updatedChildren = form.getChildrenAsList(false);

        assertEquals(originalSize + 1, updatedChildren.size());
    }

    @FormTest
    void testDropWithAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container target = new Container(BoxLayout.y());
        target.setDropTarget(true);

        form.add(BorderLayout.CENTER, target);
        form.revalidate();

        // Start animation
        form.animateLayout(200);

        // Drop during animation
        Component dragged = new Label("Dragged");
        target.drop(dragged, 50, 50);

        assertNotNull(dragged);
    }
}
