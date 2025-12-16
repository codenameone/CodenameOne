package com.codename1.ui.layouts.mig;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import org.junit.jupiter.api.Assertions;
import com.codename1.junit.FormTest;

import java.util.HashMap;

public class GridWeakCellTest extends UITestBase {

    @FormTest
    public void testWeakCell() {
        // Grid.WeakCell is tested via Grid.getGridPositions().
        // We need to enable design time for the container to trigger Grid.saveGrid().
        // LayoutUtil.setDesignTime(cw, true) is the way.

        // But LayoutUtil is package private. We are in the same package.
        // Wait, ContainerWrapper is the argument. Container implements ContainerWrapper?
        // No, ContainerWrapper is an interface in com.codename1.ui.layouts.mig.
        // The CN1 implementation wraps CN1 Container.

        // But I cannot easily get the ContainerWrapper instance created by MigLayout.
        // MigLayout creates it internally.

        // LayoutUtil.setDesignTime(null, true) turns on design time globally (or generally).
        LayoutUtil.setDesignTime(null, true);

        try {
            Container cnt = new Container(new MigLayout(""));
            Button btn = new Button("Test");
            cnt.add(btn);

            // Force layout to trigger Grid creation (MigLayout creates Grid internally)
            cnt.layoutContainer();

            // Now verify Grid positions
            // Grid.getGridPositions takes Object parComp.
            HashMap<Object, int[]> positions = Grid.getGridPositions(cnt);
            Assertions.assertNotNull(positions, "Grid positions should not be null if design time is enabled");
            Assertions.assertTrue(positions.containsKey(btn), "Grid positions should contain the button");

            int[] bounds = positions.get(btn);
            Assertions.assertNotNull(bounds);
            Assertions.assertEquals(1, bounds[2]); // Span X
            Assertions.assertEquals(1, bounds[3]); // Span Y
        } finally {
            LayoutUtil.setDesignTime(null, false);
        }
    }
}
