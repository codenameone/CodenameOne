package com.codename1.flutter.material;

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * BottomNavigationBar item layout, headless (Dp scale 1): 80lp bar height,
 * equal-width slots, icons centered per slot (vertically centered when the
 * item has no label), onTap(index) dispatch.
 */
class BottomNavigationBarLayoutTest {

    private static BottomNavigationBarItem item(Widget icon, String label) {
        BottomNavigationBarItem i = new BottomNavigationBarItem();
        i.icon(icon);
        i.label(label);
        return i;
    }

    private BottomNavigationBarRenderElement mountAndLayout(BottomNavigationBar bar, BoxConstraints c) {
        RenderHost host = new RenderHost();
        BottomNavigationBarRenderElement e =
                (BottomNavigationBarRenderElement) FlutterUI.mount(bar, host, new BuildOwner());
        e.layout(c);
        e.position(0, 0);
        return e;
    }

    @Test
    void threeUnlabeledItemsCenterTheirIconsInEqualSlots() {
        BottomNavigationBar bar = new BottomNavigationBar();
        bar.items(DartList.<BottomNavigationBarItem>of(
                item(new ProbeBox(10, 10), null),
                item(new ProbeBox(10, 10), null),
                item(new ProbeBox(10, 10), null)));

        BottomNavigationBarRenderElement e =
                mountAndLayout(bar, BoxConstraints.loose(300, Double.POSITIVE_INFINITY));

        assertEquals(new Size(300, 80), e.size(), "80lp bar, full width");
        for (int i = 0; i < 3; i++) {
            RenderElement icon = e.iconRenderElement(i);
            assertEquals(i * 100 + 45, icon.x(), "icon " + i + " centered in its 100px slot");
            assertEquals(35, icon.y(), "no label: icon vertically centered (80-10)/2");
        }
    }

    @Test
    void overlayCoversTheWholeBarAndSitsLast() {
        BottomNavigationBar bar = new BottomNavigationBar();
        bar.items(DartList.<BottomNavigationBarItem>of(
                item(new ProbeBox(10, 10), null),
                item(new ProbeBox(10, 10), null)));

        BottomNavigationBarRenderElement e =
                mountAndLayout(bar, BoxConstraints.loose(200, Double.POSITIVE_INFINITY));

        List<RenderElement> children = e.renderChildren();
        assertEquals(3, children.size(), "2 icons + overlay");
        RenderElement overlay = children.get(children.size() - 1);
        assertEquals(new Size(200, 80), overlay.size());
        assertEquals(0, overlay.x());
        assertEquals(0, overlay.y());
    }

    @Test
    void unboundedWidthFallsBackTo80lpSlots() {
        BottomNavigationBar bar = new BottomNavigationBar();
        bar.items(DartList.<BottomNavigationBarItem>of(
                item(new ProbeBox(10, 10), null),
                item(new ProbeBox(10, 10), null)));

        BottomNavigationBarRenderElement e = mountAndLayout(bar,
                BoxConstraints.loose(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        assertEquals(new Size(160, 80), e.size(), "2 items x 80lp fallback slots");
    }

    @Test
    void userTapDispatchesTheItemIndexAsLong() {
        final List<Long> taps = new ArrayList<Long>();
        BottomNavigationBar bar = new BottomNavigationBar();
        bar.items(DartList.<BottomNavigationBarItem>of(
                item(new ProbeBox(10, 10), null),
                item(new ProbeBox(10, 10), null),
                item(new ProbeBox(10, 10), null)));
        bar.currentIndex(0);
        bar.onTap(taps::add);

        BottomNavigationBarRenderElement e =
                mountAndLayout(bar, BoxConstraints.loose(300, Double.POSITIVE_INFINITY));

        e.userTapped(2);
        e.userTapped(0);
        e.userTapped(7); // out of range: ignored
        assertEquals(List.of(2L, 0L), taps);
    }

    @Test
    void currentIndexChangeRebuildsTintedIconsInPlace() {
        BottomNavigationBar bar = new BottomNavigationBar();
        bar.items(DartList.<BottomNavigationBarItem>of(
                item(new ProbeBox(10, 10), null),
                item(new ProbeBox(10, 10), null)));
        bar.currentIndex(0);

        BottomNavigationBarRenderElement e =
                mountAndLayout(bar, BoxConstraints.loose(200, Double.POSITIVE_INFINITY));
        RenderElement firstIconBefore = e.iconRenderElement(0);

        BottomNavigationBar updated = new BottomNavigationBar();
        updated.items(DartList.<BottomNavigationBarItem>of(
                item(new ProbeBox(10, 10), null),
                item(new ProbeBox(10, 10), null)));
        updated.currentIndex(1);
        e.update(updated);

        assertEquals(firstIconBefore, e.iconRenderElement(0),
                "same widget type: the icon element is reused in place");
    }
}
