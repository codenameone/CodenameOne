package com.codename1.samples;

import com.codename1.components.InfiniteProgress;
import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.components.ToastBar.Status;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.animations.CommonTransitions;
import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

public class InfiniteProgressWithMessageSampleTest extends UITestBase {

    @FormTest
    public void testInfiniteProgressWithMessage() {
        Form hi = new Form("Hi World", BoxLayout.y());
        Button showProgress = new Button("Show InfiniteProgress");
        showProgress.addActionListener(e->{
            Display.getInstance().callSerially(()->{
                Dialog dlg = new Dialog();
                dlg.setDialogUIID("Container");
                dlg.setTintColor(0x0);
                dlg.setLayout(new BorderLayout());
                SpanLabel message = new SpanLabel("This is a progress message we wish to show");
                $("*", message).setFgColor(0xffffff);
                dlg.add(BorderLayout.CENTER, BoxLayout.encloseYCenter(FlowLayout.encloseCenter(new InfiniteProgress()), FlowLayout.encloseCenter(message)));

                dlg.setTransitionInAnimator(CommonTransitions.createEmpty());
                dlg.setTransitionOutAnimator(CommonTransitions.createEmpty());
                dlg.showPacked(BorderLayout.CENTER, false);

                // Instead of blocking, we just dispose immediately for test
                dlg.dispose();
            });
        });

        Button showToastProgress = new Button("Show Toast Progress");
        showToastProgress.addActionListener(e->{
            Display.getInstance().callSerially(()->{
                Status status = ToastBar.getInstance().createStatus();
                status.setMessage("This is a progress message we wish to show");
                status.setShowProgressIndicator(true);
                status.show();

                // Clear immediately for test
                status.clear();
            });
        });
        hi.addAll(showProgress, showToastProgress);
        hi.show();

        assertEquals("Hi World", hi.getTitle());
        assertEquals(2, hi.getComponentCount());

        showProgress.released();
        flushSerialCalls();

        showToastProgress.released();
        flushSerialCalls();
    }
}
