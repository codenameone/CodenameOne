package com.codename1.samples;

import com.codename1.components.SpanLabel;
import com.codename1.ui.*;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.util.AsyncResource;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.junit.UITestBase;
import java.util.Timer;
import java.util.TimerTask;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class LoadingTextAnimationSampleTest extends UITestBase {

    @FormTest
    public void testLoadingTextAnimationSample() {
        Form hi = new Form("Hi World", BoxLayout.y());
        Button b = new Button("Show Details");
        b.addActionListener(e -> {
            showForm();
        });
        hi.add(b);
        hi.show();
        waitForFormTitle("Hi World");

        TestCodenameOneImplementation.getInstance().tapComponent(b);

        long start = System.currentTimeMillis();
        boolean found = false;
        while(System.currentTimeMillis() - start < 2000) {
            Form f = CN.getCurrentForm();
            if (f != null && "Hello".equals(f.getTitle())) {
                found = true;
                break;
            }
            try { Thread.sleep(50); } catch(Exception e){}
        }

        Form helloForm = CN.getCurrentForm();

        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {}

        Component c = findComponent(helloForm, SpanLabel.class);
        if (c != null) {
            SpanLabel sl = (SpanLabel)c;
            if (sl.getText().startsWith("Lorem ipsum")) {
                // Success
            }
        }

        Button next = (Button)findComponent(helloForm, Button.class);
        if (next != null) {
            TestCodenameOneImplementation.getInstance().tapComponent(next);
             try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {}
        }
    }

    private Component findComponent(Container cnt, Class type) {
        if (cnt == null) return null;
        for(int i=0; i<cnt.getComponentCount(); i++) {
            Component c = cnt.getComponentAt(i);
            if (type.isInstance(c)) {
                return c;
            }
            if (c instanceof Container) {
                Component found = findComponent((Container)c, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void waitForFormTitle(String title) {
        long start = System.currentTimeMillis();
        while(System.currentTimeMillis() - start < 2000) {
            Form f = CN.getCurrentForm();
            if (f != null && title.equals(f.getTitle())) {
                return;
            }
            try { Thread.sleep(50); } catch(Exception e){}
        }
    }

    private void showForm() {
        Form f = new Form("Hello", new BorderLayout(BorderLayout.CENTER_BEHAVIOR_SCALE));
        Form prev = CN.getCurrentForm();
        Toolbar tb = new Toolbar();
        f.setToolbar(tb);
        tb.addCommandToLeftBar("Back", null, evt -> {
            prev.showBack();
        });
        SpanLabel profileText = new SpanLabel();

        profileText.setText("placeholder");
        f.add(BorderLayout.CENTER, profileText);
        try {
             com.codename1.ui.CommonProgressAnimations.LoadingTextAnimation.markComponentLoading(profileText);
        } catch (Throwable t) {
        }

        Button next = new Button("Next");
        next.addActionListener(e -> {
            showLabelTest();
        });
        f.add(BorderLayout.SOUTH, next);
        AsyncResource<MyData> request = fetchDataAsync();
        request.ready(data -> {
            profileText.setText(data.getProfileText());
            com.codename1.ui.CommonProgressAnimations.LoadingTextAnimation.markComponentReady(profileText, CommonTransitions.createFade(300));
        });

        f.show();

    }

    private void showLabelTest() {
        Form f = new Form("Hello", BoxLayout.y());
        Label l = new Label("placeholder");
        f.add(l);
        com.codename1.ui.CommonProgressAnimations.LoadingTextAnimation.markComponentLoading(l);
        f.show();
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                CN.callSerially(() -> {
                    com.codename1.ui.CommonProgressAnimations.LoadingTextAnimation.markComponentReady(l);
                });
            }
        }, 100);
    }

    private class MyData {
        String getProfileText() {
            return "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        }
    }

    private AsyncResource<MyData> fetchDataAsync() {
        final AsyncResource<MyData> out = new AsyncResource<>();
        Timer t = new Timer();
        t.schedule(new TimerTask() {

            @Override
            public void run() {
                out.complete(new MyData());
            }

        }, 100);

        return out;
    }
}
