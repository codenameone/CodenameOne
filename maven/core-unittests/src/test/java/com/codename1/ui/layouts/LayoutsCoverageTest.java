package com.codename1.ui.layouts;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextComponent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.table.TableLayout;
import com.codename1.ui.plaf.UIManager;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class LayoutsCoverageTest extends UITestBase {

    @FormTest
    void groupLayoutAutoPaddingAndLinking() {
        Form form = new Form();
        Container content = form.getContentPane();
        GroupLayout layout = new GroupLayout(content);
        layout.setAutocreateGaps(true);
        layout.setAutocreateContainerGaps(true);
        content.setLayout(layout);

        Label first = new Label("First");
        first.setPreferredSize(new Dimension(20, 10));
        Button second = new Button("Second");
        second.setPreferredSize(new Dimension(60, 10));
        Label third = new Label("Third");
        third.setPreferredSize(new Dimension(25, 10));

        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .add(first)
                        .add(second)
                        .add(layout.createParallelGroup(GroupLayout.LEADING)
                                .add(third))
        );

        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.BASELINE)
                        .add(first)
                        .add(second)
                        .add(third)
        );

        layout.linkSize(new Component[]{first, second}, GroupLayout.HORIZONTAL);
        form.show();
        TestCodenameOneImplementation.getInstance().dispatchPointerPressAndRelease(1, 1);

        assertTrue(layout.getAutocreateContainerGaps());
        assertTrue(layout.getAutocreateGaps());
        assertEquals(first.getWidth(), second.getWidth());
        assertEquals(third.getHeight(), second.getHeight());
        assertEquals(layout.getComponentConstraint(first), layout.getComponentConstraint(first));
    }

    @FormTest
    void gridBagLayoutStoresConstraintsAndLayouts() {
        Form form = new Form(new GridBagLayout());
        GridBagLayout layout = (GridBagLayout) form.getLayout();

        GridBagConstraints leftConstraints = new GridBagConstraints();
        leftConstraints.gridx = 0;
        leftConstraints.gridy = 0;
        leftConstraints.insets = new Insets(1, 2, 3, 4);

        Label left = new Label("L");
        form.add(leftConstraints, left);

        GridBagConstraints rightConstraints = new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.EAST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
        Button right = new Button("R");
        form.add(rightConstraints, right);

        form.show();
        TestCodenameOneImplementation.getInstance().dispatchPointerPressAndRelease(2, 2);

        GridBagConstraints stored = (GridBagConstraints) layout.getComponentConstraint(left);
        assertNotSame(leftConstraints, stored);
        assertEquals(2, stored.insets.left);
        assertEquals(GridBagConstraints.HORIZONTAL, ((GridBagConstraints) layout.getComponentConstraint(right)).fill);

        layout.removeLayoutComponent(right);
        assertEquals(leftConstraints.gridx, ((GridBagConstraints) layout.getComponentConstraint(left)).gridx);
    }

    @FormTest
    void gridBagConstraintsValidationAndInsetsEquality() {
        GridBagConstraints invalid = new GridBagConstraints();
        invalid.anchor = 99;
        assertThrows(IllegalArgumentException.class, invalid::verify);

        Insets a = new Insets(1, 2, 3, 4);
        Insets b = (Insets) a.clone();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        Insets c = new Insets(0, 0, 0, 0);
        c.set(5, 6, 7, 8);
        assertEquals("com.codename1.ui.layouts.Insets[left=6,top=5,right=8,bottom=7]", c.toString());
    }

    @FormTest
    void layoutStylePreferredAndContainerGaps() {
        LayoutStyle shared = LayoutStyle.getSharedInstance();
        LayoutStyle.setSharedInstance(shared);

        Label before = new Label("Before");
        Label after = new Label("After");
        Container parent = new Container();

        int related = shared.getPreferredGap(before, after, LayoutStyle.RELATED, GroupLayout.SOUTH, parent);
        int unrelated = shared.getPreferredGap(before, after, LayoutStyle.UNRELATED, GroupLayout.EAST, parent);
        assertTrue(unrelated >= related);

        assertThrows(IllegalArgumentException.class, () -> shared.getPreferredGap(before, after, 99, GroupLayout.NORTH, parent));
        assertThrows(IllegalArgumentException.class, () -> shared.getPreferredGap(before, null, LayoutStyle.RELATED, GroupLayout.NORTH, parent));

        int containerGap = shared.getContainerGap(before, GroupLayout.SOUTH, parent);
        assertTrue(containerGap > 0);
    }

    @FormTest
    void textModeLayoutGroupsInputsAndClonesConstraints() {
        Hashtable theme = new Hashtable();
        theme.put("textComponentOnTopBool", Boolean.FALSE);
        UIManager.getInstance().setThemeProps(theme);
        TextModeLayout textLayout = new TextModeLayout(2, 1);
        Container container = new Container(textLayout);

        TextComponent first = new TextComponent().label("First");
        first.onTopMode(false);
        TextComponent second = new TextComponent().label("Second");
        second.onTopMode(false);

        container.add(textLayout.createConstraint(), first);
        TableLayout.Constraint constraint = textLayout.createConstraint();
        constraint.setVerticalSpan(2);
        container.add(constraint, second);

        Form form = new Form(new BorderLayout());
        form.add(BorderLayout.CENTER, container);
        form.show();
        TestCodenameOneImplementation.getInstance().dispatchPointerPressAndRelease(5, 5);

        TableLayout.Constraint cloned = (TableLayout.Constraint) textLayout.cloneConstraint(constraint);
        assertEquals(constraint.getVerticalSpan(), cloned.getVerticalSpan());
        assertEquals(2, ((TableLayout.Constraint) textLayout.table.getComponentConstraint(second)).getVerticalSpan());
        assertNotSame(constraint, cloned);
    }

    @FormTest
    void layeredLayoutInsetsAndReferenceComponents() {
        Container container = new Container(new LayeredLayout());
        LayeredLayout layeredLayout = (LayeredLayout) container.getLayout();

        Button base = new Button("Base");
        Button overlay = new Button("Overlay");
        container.add(base);
        container.add(overlay);

        layeredLayout.setInsets(base, "1px 2px auto auto");
        layeredLayout.setInsets(overlay, "auto auto auto auto");
        layeredLayout.setReferenceComponentLeft(overlay, base, 1f);
        layeredLayout.setReferenceComponentTop(overlay, base, 0f);

        Form form = new Form(new BorderLayout());
        form.add(BorderLayout.CENTER, container);
        form.show();
        TestCodenameOneImplementation.getInstance().dispatchPointerPressAndRelease(10, 10);

        LayeredLayout.LayeredLayoutConstraint baseConstraint = layeredLayout.getLayeredLayoutConstraint(base);
        assertEquals(LayeredLayout.UNIT_PIXELS, baseConstraint.top().getUnit());
        assertEquals("2px", baseConstraint.right().getValueAsString());

        LayeredLayout.LayeredLayoutConstraint overlayConstraint = layeredLayout.getLayeredLayoutConstraint(overlay);
        assertSame(base, overlayConstraint.left().referenceComponent());
        assertEquals(1f, overlayConstraint.left().referencePosition());
        assertEquals(0f, overlayConstraint.top().referencePosition());
    }
}
