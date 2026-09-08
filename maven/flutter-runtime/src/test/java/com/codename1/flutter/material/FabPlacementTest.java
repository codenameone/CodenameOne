package com.codename1.flutter.material;

import com.codename1.flutter.rendering.Dp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Where a Scaffold puts its FloatingActionButton.
 *
 * <p>{@code floatingActionButtonLocation} was accepted and discarded, so every
 * FAB floated at the bottom right whatever it asked for. Reply's compose button
 * is centreDocked and drew in the corner on top of the bottom bar instead of
 * centred and straddling it.</p>
 *
 * <p>Headless, so one logical pixel is one device pixel.</p>
 */
class FabPlacementTest {

    private static final double SCAFFOLD_W = 400;
    private static final double SCAFFOLD_H = 800;
    private static final double FAB = 56;
    private static final double NAV = 80;

    private static double x(FloatingActionButtonLocation where) {
        return ScaffoldRenderElement.fabX(where, SCAFFOLD_W, FAB);
    }

    private static double y(FloatingActionButtonLocation where) {
        return ScaffoldRenderElement.fabY(where, SCAFFOLD_H, FAB, NAV);
    }

    @Test
    void centreLocationsCentreTheFab() {
        assertEquals((SCAFFOLD_W - FAB) / 2, x(FloatingActionButtonLocation.centerDocked), 0.001);
        assertEquals((SCAFFOLD_W - FAB) / 2, x(FloatingActionButtonLocation.centerFloat), 0.001);
        assertEquals((SCAFFOLD_W - FAB) / 2, x(FloatingActionButtonLocation.miniCenterTop), 0.001);
    }

    @Test
    void startAndEndSitAgainstTheirEdges() {
        double margin = Dp.px(16);
        assertEquals(margin, x(FloatingActionButtonLocation.startFloat), 0.001);
        assertEquals(SCAFFOLD_W - FAB - margin, x(FloatingActionButtonLocation.endFloat), 0.001);
    }

    @Test
    void noLocationIsFluttersEndFloat() {
        double margin = Dp.px(16);
        assertEquals(SCAFFOLD_W - FAB - margin, x(null), 0.001);
        assertEquals(SCAFFOLD_H - NAV - FAB - margin, y(null), 0.001);
    }

    @Test
    void aDockedFabStraddlesTheBottomStripsTopEdge() {
        // Its CENTRE sits on the edge, which is what lets a notched bar cut a
        // hole for it; a floating one clears the strip by the margin instead.
        double contentBottom = SCAFFOLD_H - NAV;
        assertEquals(contentBottom - FAB / 2, y(FloatingActionButtonLocation.centerDocked), 0.001);
        assertEquals(contentBottom - FAB - Dp.px(16),
                y(FloatingActionButtonLocation.centerFloat), 0.001);
    }

    @Test
    void aTopFabSitsAtTheTop() {
        assertEquals(Dp.px(16), y(FloatingActionButtonLocation.centerTop), 0.001);
    }

    @Test
    void aDockedFabNeverFallsOffTheBottom() {
        // Flutter clamps it to the scaffold; a bottom strip taller than the
        // scaffold would otherwise push it past the edge.
        double got = ScaffoldRenderElement.fabY(
                FloatingActionButtonLocation.centerDocked, SCAFFOLD_H, FAB, 0);
        assertEquals(SCAFFOLD_H - FAB - Dp.px(16), got, 0.001);
    }
}
