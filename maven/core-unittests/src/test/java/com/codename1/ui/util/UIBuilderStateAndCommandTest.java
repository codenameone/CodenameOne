package com.codename1.ui.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Command;
import com.codename1.ui.Form;
import com.codename1.ui.List;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.list.DefaultListModel;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class UIBuilderStateAndCommandTest extends UITestBase {

    @FormTest
    void formStateRoundTripRestoresFocusAndSelection() {
        UIBuilder builder = new UIBuilder();
        Form source = new Form("Source", new BorderLayout());
        source.setName("sourceForm");

        List list = new List(new DefaultListModel(new String[]{"a", "b", "c"}));
        list.setName("list");
        source.add(BorderLayout.CENTER, list);
        source.show();
        list.setSelectedIndex(1);
        source.setFocused(list);

        Hashtable state = builder.getFormState(source);
        assertEquals("sourceForm", state.get(UIBuilder.FORM_STATE_KEY_NAME));
        assertEquals("list", state.get(UIBuilder.FORM_STATE_KEY_FOCUS));
        assertEquals(1, ((Integer) state.get(UIBuilder.FORM_STATE_KEY_SELECTION)).intValue());

        Form destination = new Form("Destination", new BorderLayout());
        destination.setName("destination");
        List destList = new List(new DefaultListModel(new String[]{"a", "b", "c"}));
        destList.setName("list");
        destination.add(BorderLayout.CENTER, destList);

        destination.show();
        builder.setFormState(destination, state);
        assertEquals("list", destination.getFocused().getName());
        assertEquals(1, destList.getSelectedIndex());
    }

    @FormTest
    void formListenerTriggersPlatformActions() {
        implementation.resetInvocationTracking();
        UIBuilder builder = new UIBuilder();
        Form form = new Form("Actions");
        form.show();

        UIBuilder.FormListener listener = builder.new FormListener();

        Command minimize = builder.createCommandImpl("Min", null, 1, "$Minimize", false, "");
        minimize.putClientProperty(UIBuilder.COMMAND_ACTION, "$Minimize");
        minimize.putClientProperty(UIBuilder.COMMAND_ARGUMENTS, "");
        listener.actionPerformed(new ActionEvent(minimize, ActionEvent.Type.Command, form, 0, 0));
        assertTrue(implementation.isMinimizeInvoked());

        implementation.resetInvocationTracking();
        Command exit = builder.createCommandImpl("Exit", null, 2, "$Exit", false, "");
        exit.putClientProperty(UIBuilder.COMMAND_ACTION, "$Exit");
        exit.putClientProperty(UIBuilder.COMMAND_ARGUMENTS, "");
        listener.actionPerformed(new ActionEvent(exit, ActionEvent.Type.Command, form, 0, 0));
        assertTrue(implementation.isExitInvoked());

        implementation.resetInvocationTracking();
        Command execute = builder.createCommandImpl("Exec", null, 3, "$Execute", false, "native://test");
        execute.putClientProperty(UIBuilder.COMMAND_ACTION, "$Execute");
        execute.putClientProperty(UIBuilder.COMMAND_ARGUMENTS, "native://test");
        listener.actionPerformed(new ActionEvent(execute, ActionEvent.Type.Command, form, 0, 0));
        assertEquals("native://test", implementation.getLastExecuteUrl());
    }
}
