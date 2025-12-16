package com.codename1.samples;

import static com.codename1.ui.CN.*;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.io.Log;
import com.codename1.ui.Toolbar;
import java.io.IOException;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.io.NetworkEvent;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Button;
import com.codename1.ui.layouts.BorderLayout;

public class FullScreenWithBrowserComponentSample {

    private final BrowserComponent bc = new BrowserComponent();

    private BrowserForm browserForm;
    private Button button;

    public BrowserComponent getBrowserComponent() {
        return bc;
    }

    public Button getButton() {
        return button;
    }

    class BrowserForm extends Form {
        BrowserForm() {
            super(new BorderLayout());
            add(BorderLayout.CENTER, bc);
            button = new Button("Toggle Fullscreen");
            button.addActionListener(e->{
                if (Display.getInstance().isInFullScreenMode()) {
                    Display.getInstance().exitFullScreen();
                } else {
                    Display.getInstance().requestFullScreen();
                }
            });
            add(BorderLayout.SOUTH, button);
        }

        void setPage(String content) {
            bc.setPage(content, null);
        }
    }

    public void start() {
        browserForm = new BrowserForm();
        browserForm.show();
        browserForm.setPage("<html><body>HELLO WORLD</body><html>");
        test();
    }

    public void test()
    {
        browserForm.setPage("<html><body>HELLO WORLD</body><html>");
    }
}
