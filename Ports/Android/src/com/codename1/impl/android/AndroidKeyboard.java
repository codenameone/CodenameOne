/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.android;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.impl.VirtualKeyboardInterface;

/**
 *
 * @author cf130546
 */
public class AndroidKeyboard implements VirtualKeyboardInterface {

    private AndroidImplementation impl;

    public AndroidKeyboard(AndroidImplementation impl) {
        this.impl = impl;
    }

    public String getVirtualKeyboardName() {
        return "Android Keyboard";

    }

    public void showKeyboard(boolean show) {
//        manager.restartInput(myView);
//        if (keyboardShowing != show) {
//            manager.toggleSoftInputFromWindow(myView.getWindowToken(), 0, 0);
//            this.keyboardShowing = show;
//        }
        Form current = Display.getInstance().getCurrent();
        if(current != null){
            Component cmp = current.getFocused();
            if(cmp != null && cmp instanceof TextArea){
                TextArea txt = (TextArea)cmp;
                if(show){
                    Display.getInstance().editString(txt, txt.getMaxSize(), txt.getConstraint(), txt.getText(), 0);
                }
            }
        }
//        if(!show){
//            impl.saveTextEditingState();
//        }
    }

    public boolean isVirtualKeyboardShowing() {
        return InPlaceEditView.isEditing();
    }

    public void setInputType(int inputType) {
    }
}
