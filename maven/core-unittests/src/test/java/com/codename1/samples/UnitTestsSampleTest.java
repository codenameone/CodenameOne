package com.codename1.samples;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.UIManager;
import com.codename1.util.AsyncResource;
import com.codename1.util.StringUtil;
import java.util.List;
import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

public class UnitTestsSampleTest extends UITestBase {

    @FormTest
    public void testUnitTestsSample() {
        TestForm hi = new TestForm();
        hi.show();
        waitForForm(hi);

        // Execute the internal tests
        try {
            assertTrue(new BoxLayoutTests().runTest(), "BoxLayoutTests should pass");
            assertTrue(new StringUtilTests().runTest(), "StringUtilTests should pass");
            assertTrue(new AsyncResourceTests().runTest(), "AsyncResourceTests should pass");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Internal test failed: " + e.getMessage());
        }
    }

    private void waitForForm(Form form) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            if (Display.getInstance().getCurrent() == form) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        fail("Form did not become current in time");
    }

    public class TestForm extends Form {

        boolean hebrew = false;

        TestForm() {
            super("Hi World", BoxLayout.y());
            Toolbar tb = new Toolbar();
            Form hi = this;
            hi.setToolbar(tb);

            CheckBox rtl = new CheckBox("RTL");
            rtl.setSelected(isRTL());
            rtl.addActionListener(e -> {
                getUIManager().getLookAndFeel().setRTL(rtl.isSelected());
                hebrew = rtl.isSelected();
            });
            hi.add(rtl);
            hi.add(new SpanLabel("Test 1: The Labels below should each be rendered centered in its row "));
            int[] alignments = new int[]{LEFT, RIGHT, CENTER};
            for (int align : alignments) {
                FlowLayout fl = new FlowLayout();
                fl.setAlign(align);
                Container cnt = new Container(fl);
                $(cnt).selectAllStyles()
                        .setBorder(Border.createLineBorder(1))
                        .setPaddingMillimeters(0, 2, 2, 2);
                Label l = new Label();
                $(l).selectAllStyles().setPadding(0).setMargin(0);
                cnt.add(l);
                hi.add(FlowLayout.encloseCenter(cnt));
                l.setText("TEXT" + align);
            }
        }
    }

    public class StringUtilTests {
        public boolean runTest() throws Exception {
            String testStr = "1,2,3,,,,,,5,6,3";
            String expected = "[1, 2, 3, 5, 6, 3]";
            List<String> toks2 = StringUtil.tokenize(testStr, ",");
            assertEquals(expected, toks2.toString());
            return true;
        }
    }

    public class AsyncResourceTests {
        public boolean runTest() throws Exception {
            AsyncResource<Integer> r1 = new AsyncResource<>();
            // Since we are in a single threaded test environment or simulated one,
            // threading might behave differently. However, we can use callSerially or just basic threads if supported.
            new Thread(() -> {
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                r1.complete(new Integer(1));
            }).start();

            // We can't easily wait in the main test thread if it blocks the EDT in simulator,
            // but UITestBase allows some waiting.
            // However, the original test uses AsyncResource.await(r1) which blocks.
            // Let's see if we can run it.

            AsyncResource.await(r1);
            assertTrue(r1.isDone());
            assertEquals(new Integer(1), r1.get());

            // Test all()
            AsyncResource<Integer> r2 = new AsyncResource<>();
            AsyncResource<Integer> r3 = new AsyncResource<>();

            // Fix generics issue
            AsyncResource<?>[] resources = new AsyncResource<?>[] {r2, r3};
            AsyncResource<Boolean> r4 = AsyncResource.all(resources);

            r2.complete(new Integer(1));
            assertTrue(!r4.isDone());

            r3.complete(new Integer(2));
            assertTrue(r4.isDone());

            // Error handling
            AsyncResource<Integer> r5 = new AsyncResource<>();
            AsyncResource<Integer> r6 = new AsyncResource<>();
            AsyncResource<?>[] resources2 = new AsyncResource<?>[] {r5, r6};
            AsyncResource<Boolean> r7 = AsyncResource.all(resources2);
            r5.complete(new Integer(1));
            r6.error(new RuntimeException("Foo"));

            try {
                AsyncResource.await(r7);
                // Should not reach here if await throws, but await might wrap?
                // AsyncResource.await checks ready.
            } catch (Exception e) {
                // Ignore
            }
             // Check if error is propagated
             boolean hasError = false;
             try {
                 r7.get();
             } catch (Exception e) {
                 hasError = true;
             }
             assertTrue(hasError, "Should have error from combined resource");

            return true;
        }
    }

    public class BoxLayoutTests {

        public boolean runTest() throws Exception {
            BoxLayout l = new BoxLayout(BoxLayout.Y_AXIS);
            Container cnt = new Container(l);
            cnt.setRTL(false);
            int w = 500;
            int h = 500;

            cnt.setWidth(w);
            cnt.setHeight(h);

            Component child1 = createEmptyComponent(100, 100);
            Component child2 = createEmptyComponent(200, 50);
            cnt.add(child1).add(child2);

            $(child1, child2, cnt).setPadding(0).setMargin(0);
            cnt.layoutContainer();
            assertEquals(0, child1.getY(), "child1 should be aligned top");
            assertEquals(100, child2.getY(), "child 2 should be aligned top just after child1 ");
            assertEquals(0, child1.getX(), "Child1 not aligned left");
            assertEquals(w, child1.getWidth(), "Child1 not taking full width");
            assertEquals(0, child2.getX(), "Child2 not aligned left");
            assertEquals(w, child2.getWidth(), "Child2 not taking full width");

            // More tests from original sample can be added here...

            l.setAlign(Component.BOTTOM);
            cnt.setShouldCalcPreferredSize(true);
            cnt.layoutContainer();
            assertEquals(500, child2.getY() + child2.getHeight(), "Child2 should be aligned bottom");

            return true;
        }

        private Component createEmptyComponent(int width, int height) {
            return new Component() {
                @Override
                protected Dimension calcPreferredSize() {
                    return new Dimension(width, height);
                }
            };
        }
    }
}
