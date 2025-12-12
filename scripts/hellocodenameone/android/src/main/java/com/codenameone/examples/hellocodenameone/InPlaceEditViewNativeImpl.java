package com.codenameone.examples.hellocodenameone;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import java.lang.reflect.Method;

public class InPlaceEditViewNativeImpl {
    public void runReproductionTest() {
        Display.getInstance().callSerially(() -> {
            try {
                Method getImplMethod = Display.class.getDeclaredMethod("getImplementation");
                getImplMethod.setAccessible(true);
                final Object impl = getImplMethod.invoke(Display.getInstance());
                Class<?> implClass = impl.getClass();

                if (!implClass.getName().equals("com.codename1.impl.android.AndroidImplementation")) {
                     System.out.println("Implementation is not AndroidImplementation: " + implClass.getName());
                     return;
                }

                // Get InPlaceEditView class
                Class<?> inPlaceEditViewClass = Class.forName("com.codename1.impl.android.InPlaceEditView");

                // Get methods
                final Method editMethod = inPlaceEditViewClass.getMethod("edit", implClass, com.codename1.ui.Component.class, int.class);
                final Method reLayoutEditMethod = inPlaceEditViewClass.getMethod("reLayoutEdit");
                final Method stopEditMethod = inPlaceEditViewClass.getMethod("stopEdit");

                Form f = new Form("Test NPE", new BoxLayout(BoxLayout.Y_AXIS));
                final TextArea ta = new TextField("Test");
                f.add(ta);
                f.show();
                f.revalidate();

                // We need to simulate the race condition.
                new Thread(() -> {
                    try {
                        for (int i = 0; i < 50; i++) {
                            // Start editing
                            Display.getInstance().callSeriallyAndWait(() -> {
                                try {
                                    editMethod.invoke(null, impl, ta, ta.getConstraint());
                                } catch (Exception e) {
                                    System.out.println("Failed to invoke edit: " + e);
                                    e.printStackTrace();
                                }
                            });

                            // Schedule reLayoutEdit calls
                            for (int j = 0; j < 5; j++) {
                                try {
                                    reLayoutEditMethod.invoke(null);
                                    Thread.sleep(10);
                                } catch (Exception ex) {}
                            }

                            // Stop editing
                            Display.getInstance().callSeriallyAndWait(() -> {
                                try {
                                    stopEditMethod.invoke(null);
                                } catch (Exception e) {
                                    System.out.println("Failed to invoke stopEdit: " + e);
                                    e.printStackTrace();
                                }
                            });
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }).start();

            } catch (Throwable t) {
                System.out.println("InPlaceEditViewNativeImpl error: " + t);
                t.printStackTrace();
            }
        });
    }

    public boolean isSupported() {
        return true;
    }
}
