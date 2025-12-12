package com.codenameone.examples.hellocodenameone;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.impl.android.InPlaceEditView;
import com.codename1.impl.android.AndroidImplementation;

public class InPlaceEditViewNativeImpl {
    public void runReproductionTest(final ReproductionCallback callback) {
        Display.getInstance().callSerially(() -> {
            try {
                java.lang.reflect.Method getImplMethod = Display.class.getDeclaredMethod("getImplementation");
                getImplMethod.setAccessible(true);
                final Object impl = getImplMethod.invoke(Display.getInstance());

                if (!(impl instanceof AndroidImplementation)) {
                     Display.getInstance().callSerially(() -> callback.onResult(false, "Implementation is not AndroidImplementation: " + impl.getClass().getName()));
                     return;
                }
                final AndroidImplementation androidImpl = (AndroidImplementation) impl;

                Form f = new Form("Test NPE", new BoxLayout(BoxLayout.Y_AXIS));
                final TextArea ta = new TextField("Test");
                f.add(ta);
                f.show();
                f.revalidate();

                new Thread(() -> {
                    try {
                        for (int i = 0; i < 50; i++) {
                            // Start editing
                            Display.getInstance().callSeriallyAndWait(() -> {
                                try {
                                    InPlaceEditView.edit(androidImpl, ta, ta.getConstraint());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });

                            // Schedule reLayoutEdit calls
                            for (int j = 0; j < 5; j++) {
                                try {
                                    InPlaceEditView.reLayoutEdit();
                                    Thread.sleep(10);
                                } catch (Exception ex) {}
                            }

                            // Stop editing
                            Display.getInstance().callSeriallyAndWait(() -> {
                                try {
                                    InPlaceEditView.stopEdit();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                        Display.getInstance().callSerially(() -> callback.onResult(true, null));
                    } catch (Throwable t) {
                        t.printStackTrace();
                        Display.getInstance().callSerially(() -> callback.onResult(false, t.toString()));
                    }
                }).start();

            } catch (Throwable t) {
                t.printStackTrace();
                Display.getInstance().callSerially(() -> callback.onResult(false, t.toString()));
            }
        });
    }

    public boolean isSupported() {
        return true;
    }
}
