/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import com.codename1.impl.html5.JSOImplementations.KeyEvent;
import com.codename1.io.Util;
import static com.codename1.ui.CN.invokeAndBlock;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.spinner.SpinnerAccessor;
import com.codename1.ui.util.UITimer;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;


/**
 *
 * @author shannah
 */
public class NativePickerStrings extends NativePicker {
    private boolean complete;
    private boolean cancelled;
    private final Object lock=new Object();
    private boolean tabNext, tabPrev;
    
    
    protected NativePickerStrings(int type, Component source, Object currentValue, Object data) {
        super(type, source, currentValue, data);
    }
    
  
    private static void callSerially(Runnable r) {
        HTML5Implementation.callSerially(r);
    }
    
    private void resetState() {
        complete = false;
        cancelled = false;
        tabNext = false;
        tabPrev = false;
    }
    
    private EventListener createBlurEventListener() {
        return new EventListener() {

            @Override
            public void handleEvent(Event evt) {
                if (!complete) {
                    new Thread(new Runnable() {
                        public void run() {
                            synchronized(lock) {
                                complete = true;
                                cancelled = true;
                                lock.notify();
                            }
                        }
                    }).start();
                }
            }
            
        };
    }
    
    private EventListener createKeyUpEventListener() {
        return new EventListener<KeyEvent>() {

            @Override
            public void handleEvent(KeyEvent evt) {
                if (evt.getKeyCode() == 27) {
                    // escape key
                    new Thread(new Runnable() {
                        public void run() {
                            synchronized(lock) {
                                complete = true;
                                cancelled = true;
                                lock.notify();
                            }
                        }
                    });
                }
            }
            
        };
    };
    
    private FocusListener createFocusListener() {
        return new FocusListener() {

            @Override
            public void focusGained(Component cmpnt) {
                
            }

            @Override
            public void focusLost(Component cmpnt) {
                if (!complete) {
                    synchronized(lock) {
                        cancelled = true;
                        complete = true;
                        lock.notify();

                    }
                }
            }
            
        };
    }
    
    private ActionListener createFormPointerPressListener() {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent t) {
                if (!complete) {
                    synchronized(lock) {
                        cancelled = true;
                        complete = true;
                        lock.notify();

                    }
                }
            }
            
        };
    }
    
    private EventListener createPrevNextKeyListener() {
        return new EventListener() {

            @Override
            public void handleEvent(final Event evt) {

                callSerially(new Runnable() {
                    public void run() {
                        final KeyEvent kevt = (KeyEvent)evt;
                        switch (kevt.getKeyCode()) {
                            case 9 : // tab
                            case 11 : // vertical tab
                            case 10 : // lf
                            case 13 : // cr
                            {

                                boolean isNextButton = false;
                                if (HTML5Implementation.isPhoneOrTablet_()) {
                                    if (source != null) {
                                        Form f = source.getComponentForm();
                                        if (f != null) {
                                            Component next = f.getNextComponent(source);
                                            if (next != null) {
                                                isNextButton = true;
                                            }
                                        }
                                    }
                                }
                                if (kevt.getKeyCode() == 9 || kevt.getKeyCode() == 11 || isNextButton ) {
                                    tabNext = !kevt.isShiftKey();
                                    tabPrev = !tabNext;
                                }
                                synchronized(lock) {
                                    complete = true;
                                    lock.notify();
                                }
                            }
                        }

                    }
                });
            }

        };
    }
    
    private EventListener createChangeListener() {
        return new EventListener() {

            @Override
            public void handleEvent(Event evt) {
                new Thread(new Runnable() {
                    public void run() {
                        synchronized(lock) {
                            complete = true;
                            lock.notify();
                        }
                    }
                }).start();
            }
            
        };
    }
    
    private FocusListener focusListener;
    private ActionListener pointerPressListener;
    
    private void installListeners(Form form) {
        el.addEventListener("change", createChangeListener(), true);
        el.addEventListener("blur", createBlurEventListener());
        el.addEventListener("keyup", createKeyUpEventListener(), true);
        focusListener = createFocusListener();
        pointerPressListener = createFormPointerPressListener();
        el.addEventListener("keydown", createPrevNextKeyListener());
        source.addFocusListener(focusListener);
        if (form != null) {
            form.addPointerPressedListener(pointerPressListener);
            form.addKeyListener(27, pointerPressListener);
        }
    }
    
    private void uninstallListeners(Form form) {
        source.removeFocusListener(focusListener);
        if (form != null) {
            form.removePointerPressedListener(pointerPressListener);
            form.removeKeyListener(27, pointerPressListener);
        }
        
    }
    
    private void navigateToNextOrPrevious() {
        if (tabNext) {
            Form f = Display.getInstance().getCurrent();
            if (f != null) {
                Component focused = f.getNextComponent(source);

                if (focused != null) {
                    if (!(focused instanceof TextArea)) {
                        final Component fFocused = focused;
                        UITimer.timer(300, false, new Runnable() {
                            public void run() {
                                // This delay is necessary on Android 
                                // to give it time to close the keyboard
                                fFocused.requestFocus();
                                fFocused.startEditingAsync();
                                HTML5Implementation.getInstance().outputCanvas.focus();

                            }
                        });

                    } else {
                        focused.requestFocus();
                        focused.startEditingAsync();
                    }

                }

            }
        } else if (tabPrev) {
            Form f = Display.getInstance().getCurrent();
            if (f != null) {

                Component focused = f.getPreviousComponent(source);

                if (focused != null) {
                    if (!(focused instanceof TextArea)) {
                        final Component fFocused = focused;
                        UITimer.timer(300, false, new Runnable() {
                            public void run() {
                                // This delay is necessary on Android 
                                // to give it time to close the keyboard
                                fFocused.requestFocus();
                                fFocused.startEditingAsync();
                                HTML5Implementation.getInstance().outputCanvas.focus();

                            }
                        });
                    } else {
                        focused.requestFocus();
                        focused.startEditingAsync();
                    }
                }

            }
        }
    }
    
    @Override
    Object show() {
        Form form = source.getComponentForm();
        resetState();
        el = createSelect((String[])this.data, (String)this.currentValue);
        HTMLSelectElement selectEl = (HTMLSelectElement)el;
        el.setAttribute("class", "cn1-string-picker");
        installListeners(form);
        
        document.getBody().appendChild(el);
        resizeNativeElement();
        if (source instanceof Picker) {
            SpinnerAccessor.setSuppressPaint((Picker)source, true);
        }
        source.getParent().revalidate();
        el.focus();
        
        
        invokeAndBlock(new Runnable() {

            @Override
            public void run() {
                while (!complete) {
                    synchronized(lock) {
                        Util.wait(lock);
                    }
                }
            }
            
        });
        
        uninstallListeners(form);
        
        int selectedIndex = selectEl.getSelectedIndex();
        String value = cancelled && selectedIndex >= 0 ? null : selectEl.getOptions().item(selectedIndex).getText();
        document.getBody().removeChild(el);
        if (source instanceof Picker) {
            SpinnerAccessor.setSuppressPaint((Picker)source, false);
        }
        source.getParent().revalidate();
        
        if (tabNext || tabPrev) {
            UITimer.timer(50, false, new Runnable() {
                public void run() {
                    navigateToNextOrPrevious();
                }
            });
            
        }
        
        return value;
        
    }
    
}
