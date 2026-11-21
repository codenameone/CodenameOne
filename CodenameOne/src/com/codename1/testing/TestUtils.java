/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.testing;

import com.codename1.compat.java.util.Objects;
import com.codename1.io.Storage;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.SideMenuBar;
import com.codename1.ui.TextArea;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.list.ContainerList;
import com.codename1.ui.spinner.GenericSpinner;
import com.codename1.ui.util.ImageIO;

import java.io.IOException;
import java.util.ArrayList;

/// Various utility classes to automate UI testing
public final class TestUtils {
    private static boolean verbose;

    private TestUtils() {
    }

    /// Activates/deactivates the verbose test mode
    ///
    /// #### Parameters
    ///
    /// - `v`: true for verbosity
    public static void setVerboseMode(boolean v) {
        verbose = v;
    }

    /// Waits for the given number of milliseconds even if the waiting is on the EDT thread
    ///
    /// #### Parameters
    ///
    /// - `millis`: the number of milliseconds to wait
    public static void waitFor(final int millis) {
        if (verbose) {
            log("waitFor(" + millis + ")");
        }
        if (Display.getInstance().isEdt()) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException ex) {
                    }
                }
            });
        } else {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
            }
        }
    }

    /// Finds a component with the given name, works even with UI's that weren't created with the GUI builder
    ///
    /// #### Parameters
    ///
    /// - `componentName`: the name of the component to find
    ///
    /// #### Returns
    ///
    /// the component with the given name within the tree
    public static Component findByName(final String componentName) {
        if (verbose) {
            log("findByName(" + componentName + ")");
        }
        Lookup byName = new Lookup() {
            @Override
            Object find(Form root) {
                return findByName(root, componentName);
            }
        };
        Component c = (Component) callOnEdt(byName);
        if (c == null) {
            waitFor(30);
            return (Component) callOnEdt(byName);
        }
        return c;
    }

    /// Work that touches the UI and therefore has to run on the EDT.
    private abstract static class EdtCall {
        abstract Object call();
    }

    /// A walk of the current form, which is skipped when no form is showing.
    private abstract static class Lookup extends EdtCall {
        abstract Object find(Form root);

        @Override
        Object call() {
            Form root = Display.getInstance().getCurrent();
            return root == null ? null : find(root);
        }
    }

    /// Runs an `EdtCall` and records how it ended, so that the outcome can be
    /// read on the thread that asked for it. `RunnableWrapper` has no finally,
    /// so without this a throw on the EDT would leave the caller polling to the
    /// timeout and reporting a stuck EDT instead of the real failure.
    private static final class EdtTask implements Runnable {
        private final EdtCall work;
        private Object result;
        private Throwable failure;
        private boolean done;

        EdtTask(EdtCall work) {
            this.work = work;
        }

        @Override
        public void run() {
            try {
                result = work.call();
            } catch (Throwable t) {
                failure = t;
            } finally {
                done = true;
            }
        }

        Object outcome() {
            if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            }
            if (failure instanceof Error) {
                throw (Error) failure;
            }
            if (failure != null) {
                throw new RuntimeException(failure.toString());
            }
            return result;
        }
    }

    /// True when the work can run on the calling thread: it is the EDT, or no
    /// Codename One is running and so no other thread is mutating the UI.
    private static boolean runHere() {
        return Display.getInstance().isEdt() || !Display.isInitialized();
    }

    /// Reads UI state on the EDT and returns it. A test runs off the EDT while
    /// the application keeps rebuilding its forms there, so a walk from the test
    /// thread read a child count and then indexed a container the EDT had
    /// emptied in between (IndexOutOfBoundsException from
    /// `Container.getComponentAt`). The component tree is EDT-only state, so the
    /// access is marshalled rather than guarded.
    ///
    /// Never the unbounded `callSeriallyAndWait(Runnable)`: it polls forever, so
    /// an EDT that died, or is blocked waiting on this very thread, would hang the
    /// whole test run instead of failing the one test. The bound is generous so a
    /// slow form build on a CI runner is not mistaken for it.
    ///
    /// Only for work that cannot block in a nested event loop; anything that can
    /// show a modal dialog goes through `actOnEdt`.
    ///
    /// Unlike the old inline call, a handler that runs synchronously for longer
    /// than the bound now fails the helper, and the task stays queued and runs
    /// when the EDT frees. A test that legitimately holds the EDT that long is
    /// indistinguishable from a wedged one, and failing it is the point.
    private static Object callOnEdt(EdtCall work) {
        EdtTask task = new EdtTask(work);
        if (runHere()) {
            task.run();
            return task.outcome();
        }
        Display.getInstance().callSeriallyAndWait(task, edtTimeout);
        if (!task.done) {
            throw new RuntimeException(edtStuck());
        }
        return task.outcome();
    }

    /// Performs an interaction on the EDT -- an event, a command, a press --
    /// which may legitimately not return for as long as the test likes: a click
    /// that opens a modal dialog blocks inside its own handler until the dialog
    /// is dismissed, and dismissing it is the test's next step. Waiting for the
    /// action itself would time out on every such click.
    ///
    /// So a marker is queued behind the action and the wait is for the marker.
    /// It runs once the action has finished, or once the action has entered a
    /// nested event loop (a modal dialog's `invokeAndBlock` keeps serving the
    /// serial queue). Either way the EDT is alive and the test may go on; only an
    /// EDT that is dead or wedged fails. A failure the action raises before the
    /// marker runs is rethrown here; one raised later, after a dialog it opened
    /// closes, goes to the EDT's own error handling.
    private static void actOnEdt(EdtCall work) {
        EdtTask task = new EdtTask(work);
        if (runHere()) {
            task.run();
            task.outcome();
            return;
        }
        final boolean[] reached = new boolean[1];
        Display d = Display.getInstance();
        d.callSerially(task);
        d.callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                reached[0] = true;
            }
        }, edtTimeout);
        if (!reached[0]) {
            throw new RuntimeException(edtStuck());
        }
        if (task.done) {
            task.outcome();
        }
    }

    private static String edtStuck() {
        return "The EDT did not respond within " + edtTimeout
                + "ms; it is blocked or no longer running";
    }

    /// How long a helper waits for the EDT before failing. Package private so the
    /// tests of this class can shorten it instead of waiting a minute.
    static int edtTimeout = 60000; //NOPMD MutableStaticState - test-only bound, set from the same package

    /// Fails the test unless `isKind` holds for the component `c`. Every cast below is
    /// guarded by this: ParparVM's CHECKCAST is unchecked, so a wrong cast on iOS
    /// reads the wrong object instead of throwing ClassCastException.
    private static void assertKind(Object c, boolean isKind, String kind, String what) {
        assertBool(c != null, "No component found for " + what);
        assertBool(isKind, what + " is not a " + kind + ": " + c);
    }

    /// The text of a label or text area, read on the EDT.
    private static String textOf(final Component c) {
        return (String) callOnEdt(new EdtCall() {
            @Override
            Object call() {
                if (c instanceof Label) {
                    return ((Label) c).getText();
                }
                if (c instanceof TextArea) {
                    return ((TextArea) c).getText();
                }
                return null;
            }
        });
    }

    /// A point inside `c`, given as fractions of its size, read on the EDT.
    private static int[] pointIn(final Component c, final float x, final float y, final String what) {
        assertBool(c != null, "No component found for " + what);
        return (int[]) callOnEdt(new EdtCall() {
            @Override
            Object call() {
                return new int[]{c.getAbsoluteX() + (int) (x * c.getWidth()),
                        c.getAbsoluteY() + (int) (y * c.getHeight())};
            }
        });
    }

    private static final int POINTER_PRESS = 0;
    private static final int POINTER_RELEASE = 1;
    private static final int POINTER_DRAG = 2;

    /// Delivers a pointer event to the current form on the EDT.
    private static void pointerEvent(final int kind, final int x, final int y) {
        actOnEdt(new EdtCall() {
            @Override
            Object call() {
                Form f = Display.getInstance().getCurrent();
                assertBool(f != null, "No form is showing");
                if (kind == POINTER_PRESS) {
                    f.pointerPressed(x, y);
                } else if (kind == POINTER_RELEASE) {
                    f.pointerReleased(x, y);
                } else {
                    f.pointerDragged(x, y);
                }
                return null;
            }
        });
    }

    /// Delivers a key event to the current form on the EDT.
    private static void keyEvent(final boolean press, final int keyCode) {
        actOnEdt(new EdtCall() {
            @Override
            Object call() {
                Form f = Display.getInstance().getCurrent();
                assertBool(f != null, "No form is showing");
                if (press) {
                    f.keyPressed(keyCode);
                } else {
                    f.keyReleased(keyCode);
                }
                return null;
            }
        });
    }

    /// Presses and releases a button on the EDT, with the pauses the helpers
    /// always had between the two.
    private static void click(Component c, String what, int pause) {
        assertKind(c, c instanceof Button, "Button", what);
        final Button b = (Button) c;
        actOnEdt(new EdtCall() {
            @Override
            Object call() {
                b.pressed();
                return null;
            }
        });
        waitFor(pause);
        actOnEdt(new EdtCall() {
            @Override
            Object call() {
                b.released();
                return null;
            }
        });
        waitFor(pause);
    }

    /// Selects the given offset in a list
    ///
    /// #### Parameters
    ///
    /// - `listName`: the name of the list component
    ///
    /// - `offset`: the offset to select
    public static void selectInList(String listName, int offset) {
        selectListOffset(findByName(listName), offset, listName);
    }

    private static void selectListOffset(final Component c, final int offset, String what) {
        assertBool(c != null, "List not found: " + what);
        assertBool(c instanceof List || c instanceof ContainerList || c instanceof GenericSpinner,
                "Unsupported list type: " + c.getName());
        actOnEdt(new EdtCall() {
            @Override
            Object call() {
                if (c instanceof List) {
                    ((List) c).setSelectedIndex(offset);
                } else if (c instanceof ContainerList) {
                    ((ContainerList) c).setSelectedIndex(offset);
                } else {
                    ((GenericSpinner) c).getModel().setSelectedIndex(offset);
                }
                return null;
            }
        });
    }

    /// Selects the given offset in a list
    ///
    /// #### Parameters
    ///
    /// - `offset`: the offset to select
    public static void selectInList(int[] path, int offset) {
        selectListOffset(getComponentByPath(path), offset, toString(path));
    }

    /// Finds a component with the given name, works even with UI's that weren't created with the GUI builder
    ///
    /// #### Parameters
    ///
    /// - `componentName`: the name of the component to find
    ///
    /// #### Returns
    ///
    /// the component with the given name within the tree
    private static Component findByName(Container root, String componentName) {
        if (verbose) {
            log("findByName(" + root + ", " + componentName + ")");
        }
        int count = root.getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            Component c = root.getComponentAt(iter);
            String n = c.getName();
            if (n != null && n.equals(componentName)) {
                return c;
            }
            if (c instanceof Container) {
                c = findByName((Container) c, componentName);
                if (c != null) {
                    return c;
                }
            }
        }
        return null;
    }

    /// Finds a component with the given name, works even with UI's that weren't created with the GUI builder
    ///
    /// #### Parameters
    ///
    /// - `text`: the text of the label/button
    ///
    /// #### Returns
    ///
    /// the component with the given label text within the tree
    public static Label findLabelText(final String text) {
        if (verbose) {
            log("findLabelText(" + text + ")");
        }
        return (Label) callOnEdt(new Lookup() {
            @Override
            Object find(Form root) {
                return findLabelText(root, text);
            }
        });
    }


    /// Finds a component with the given name, works even with UI's that weren't created with the GUI builder
    ///
    /// #### Parameters
    ///
    /// - `text`: the text of the label/button
    ///
    /// #### Returns
    ///
    /// the component with the given label text within the tree
    private static Label findLabelText(Container root, String text) {
        if (verbose) {
            log("findLabelText(" + root + ", " + text + ")");
        }
        int count = root.getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            Component c = root.getComponentAt(iter);
            if (c instanceof Label) {
                String n = ((Label) c).getText();
                if (n != null && n.equals(text)) {
                    return (Label) c;
                }

                // will work for cases of upcase due to Android theme upcasing of buttons
                n = (String) c.getClientProperty("cn1$origText");
                if (n != null && n.equals(text)) {
                    return (Label) c;
                }
                continue;
            }
            if (c instanceof Container) {
                Label l = findLabelText((Container) c, text);
                if (l != null) {
                    return l;
                }
            }
        }
        return null;
    }

    /// Clicks the button with the given label
    ///
    /// #### Parameters
    ///
    /// - `text`: the text on the button
    public static void clickButtonByLabel(String text) {
        if (verbose) {
            log("clickButtonByLabel(" + text + ")");
        }
        Component c = findLabelText(text);
        waitFor(20);
        click(c, "label " + text, 20);
    }

    /// Clicks the button with the given label
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the button
    public static void clickButtonByName(String name) {
        if (verbose) {
            log("clickButtonByName(" + name + ")");
        }
        Component c = findByName(name);
        waitFor(20);
        click(c, "name " + name, 20);
    }

    private static String toString(int[] p) {
        if (p == null) {
            return "null";
        }
        if (p.length == 0) {
            return "{}";
        }
        StringBuilder s = new StringBuilder("{" + p[0]);
        for (int iter = 1; iter < p.length; iter++) {
            s.append(", ").append(p[iter]);

        }
        return s.append("}").toString();
    }

    /// Clicks the button with the given component path
    ///
    /// #### Parameters
    ///
    /// - `path`: the path
    public static void clickButtonByPath(int[] path) {
        if (verbose) {
            log("clickButtonByPath(" + toString(path) + ")");
        }
        click(getComponentByPath(path), "path " + toString(path), 10);
    }

    /// Executes the back command for the current form, similarly to pressing the back button
    public static void goBack() {
        if (verbose) {
            log("goBack()");
        }
        actOnEdt(new EdtCall() {
            @Override
            Object call() {
                Form f = Display.getInstance().getCurrent();
                assertBool(f != null, "No form is showing");
                Command c = f.getBackCommand();
                assertBool(c != null, "The current form doesn't have a back command at this moment! for form name " + f.getName());
                f.dispatchCommand(c, new ActionEvent(c, ActionEvent.Type.Command));
                return null;
            }
        });
        waitFor(20);
    }

    /// Executes a menu command with the given name
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the command
    public static void clickMenuItem(final String name) {
        if (verbose) {
            log("clickMenuItem(" + name + ")");
        }
        actOnEdt(new EdtCall() {
            @Override
            Object call() {
                Form f = Display.getInstance().getCurrent();
                assertBool(f != null, "No form is showing");
                for (int iter = 0; iter < f.getCommandCount(); iter++) {
                    Command c = f.getCommand(iter);
                    if (name.equals(c.getCommandName())) {
                        f.dispatchCommand(c, new ActionEvent(c, ActionEvent.Type.Command));
                        return null;
                    }
                }
                throw new RuntimeException("Command not found: " + name);
            }
        });
    }

    /// Returns all the command objects from the toolbar in the order of left, right, overflow & sidemenu
    ///
    /// #### Returns
    ///
    /// the set of commands
    public static Command[] getToolbarCommands() {
        return (Command[]) callOnEdt(new EdtCall() {
            @Override
            Object call() {
                return toolbarCommands();
            }
        });
    }

    private static Command[] toolbarCommands() {
        Form f = Display.getInstance().getCurrent();
        assertBool(f != null, "No form is showing");
        Toolbar tb = f.getToolbar();
        assertBool(tb != null, "The current form has no toolbar");
        ArrayList<Command> result = new ArrayList<Command>();
        addAllCommands(tb.getLeftBarCommands(), result);
        addAllCommands(tb.getRightBarCommands(), result);
        addAllCommands(tb.getOverflowCommands(), result);
        addAllCommands(tb.getSideMenuCommands(), result);
        Command[] carr = new Command[result.size()];
        result.toArray(carr);
        return carr;
    }

    private static void addAllCommands(Iterable<Command> cs, ArrayList<Command> result) {
        if (cs != null) {
            for (Command c : cs) {
                result.add(c);
            }
        }
    }

    /// Shows the sidemenu UI
    public static void showSidemenu() {
        actOnEdt(new EdtCall() {
            @Override
            Object call() {
                Form f = Display.getInstance().getCurrent();
                assertBool(f != null, "No form is showing");
                Toolbar tb = f.getToolbar();
                if (tb != null) {
                    tb.openSideMenu();
                } else {
                    Object bar = f.getMenuBar();
                    assertBool(bar instanceof SideMenuBar, "The current form has no side menu");
                    ((SideMenuBar) bar).openMenu(null);
                }
                return null;
            }
        });
    }

    /// Executes a command from the offset returned by `#getToolbarCommands()`
    ///
    /// #### Parameters
    ///
    /// - `offset`: the offset of the command we want to execute
    public static void executeToolbarCommandAtOffset(final int offset) {
        actOnEdt(new EdtCall() {
            @Override
            Object call() {
                Command[] commands = toolbarCommands();
                assertBool(offset >= 0 && offset < commands.length, "No toolbar command at offset " + offset
                        + "; the toolbar has " + commands.length);
                Command cmd = commands[offset];
                Display.getInstance().getCurrent().dispatchCommand(cmd, new ActionEvent(cmd));
                return null;
            }
        });
    }

    /// Scrolls to show the component in case it is invisible currently
    ///
    /// #### Parameters
    ///
    /// - `c`: the component
    public static void ensureVisible(final Component c) {
        if (verbose) {
            log("ensureVisible(" + c + ")");
        }
        assertBool(c != null, "ensureVisible: no component");
        actOnEdt(new EdtCall() {
            @Override
            Object call() {
                Form f = Display.getInstance().getCurrent();
                assertBool(f != null, "No form is showing");
                f.scrollComponentToVisible(c);
                return null;
            }
        });
    }

    /// Scrolls to show the component in case it is invisible currently
    ///
    /// #### Parameters
    ///
    /// - `componentName`: the component
    public static void ensureVisible(String componentName) {
        if (verbose) {
            log("ensureVisible(" + componentName + ")");
        }
        ensureVisible(findByName(componentName));
    }

    /// Scrolls to show the component in case it is invisible currently
    ///
    /// #### Parameters
    ///
    /// - `path`: the path to the component
    public static void ensureVisible(int[] path) {
        if (verbose) {
            log("ensureVisible(" + toString(path) + ")");
        }
        ensureVisible(getComponentByPath(path));
    }

    /// Waits for a form change and if no form change occurred after a given timeout then fail the test
    ///
    /// #### Parameters
    ///
    /// - `title`: the title of the form to wait for
    ///
    /// - `timeout`: Timeout in ms.
    public static void waitForFormTitle(final String title, final long timeout) {
        if (verbose) {
            log("waitForFormTitle(" + title + ")");
        }
        if (Display.getInstance().isEdt()) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    waitForFormTitleImpl(title, timeout);
                }
            });
        } else {
            waitForFormTitleImpl(title, timeout);
        }
        waitFor(50);
    }

    /// Waits for a form change and if no form change occurred after a given timeout then fail the test.  Timeout is 90 seconds.
    ///
    /// #### Parameters
    ///
    /// - `title`: the title of the form to wait for
    public static void waitForFormTitle(final String title) {
        waitForFormTitle(title, 90000);
    }

    private static String currentFormTitle() {
        return (String) callOnEdt(new Lookup() {
            @Override
            Object find(Form root) {
                return getFormTitle(root);
            }
        });
    }

    private static String currentFormName() {
        return (String) callOnEdt(new Lookup() {
            @Override
            Object find(Form root) {
                return root.getName();
            }
        });
    }

    private static String getFormTitle(Form f) {
        if (f.getToolbar() != null) {
            Component c = f.getToolbar().getTitleComponent();
            if (c instanceof Label) {
                return ((Label) c).getText();
            }
            return null;
        } else {
            return f.getTitle();
        }
    }

    private static void waitForFormTitleImpl(String title, long timeout) {
        long t = System.currentTimeMillis() + timeout;
        while (!title.equals(currentFormTitle())) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
            if (System.currentTimeMillis() > t) {
                assertBool(false, "Waiting for form " + title + " timed out! Current form title is: " + currentFormTitle());
            }
        }
    }

    /// Waits for a form change and if no form change occurred after a given timeout then fail the test
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the form to wait for
    ///
    /// - `timeout`: Timeout in ms
    public static void waitForFormName(final String name, final long timeout) {
        if (verbose) {
            log("waitForFormName(" + name + ")");
        }
        if (Display.getInstance().isEdt()) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    waitForFormNameImpl(name, timeout);
                }
            });
        } else {
            waitForFormNameImpl(name, timeout);
        }
        waitFor(50);
    }

    /// Waits for a form change and if no form change occurred after a given timeout then fail the test.  Timeout is 90 seconds.
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the form to wait for
    public static void waitForFormName(final String name) {
        waitForFormName(name, 90000);
    }

    private static void waitForFormNameImpl(String title, long timeout) {
        long t = System.currentTimeMillis() + timeout;
        while (!title.equals(currentFormName())) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
            if (System.currentTimeMillis() > t) {
                assertBool(false, "Waiting for form " + title + " timed out! Current form name is: " + currentFormName());
            }
        }
    }

    /// Waits for a form change and if no form change occurred after a given timeout then fail the test
    ///
    /// #### Parameters
    ///
    /// - `timeout`: Timeout in milliseconds.
    public static void waitForUnnamedForm(final long timeout) {
        if (verbose) {
            log("waitForUnnamedForm()");
        }
        if (Display.getInstance().isEdt()) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    waitForUnnamedFormImpl(timeout);
                }
            });
        } else {
            waitForUnnamedFormImpl(timeout);
        }
        waitFor(50);
    }

    /// Waits for a form change and if no form change occurred after a given timeout then fail the test
    public static void waitForUnnamedForm() {
        waitForUnnamedForm(90000);
    }


    private static void waitForUnnamedFormImpl(long timeout) {
        long t = System.currentTimeMillis() + timeout;
        while (currentFormName() != null) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
            if (System.currentTimeMillis() > t) {
                assertBool(false, "Waiting for form unnamed form timed out! Current form name is: " + currentFormName());
            }
        }
    }

    /// The screenshot test takes a screenshot of the screen and compares it to
    /// a prior screenshot, if both are 100% identical the test passes. If not
    /// the test fails.
    ///
    /// If this is the first time the test is run then the screenshot is taken
    /// and saved under the given name in the devices storage. The test passes
    /// for this case but a warning is printed to the console. The name will have
    /// .png appended to it so it will be identified.
    ///
    /// This test will only work on devices that support the ImageIO API with PNG
    /// file format.
    ///
    /// #### Parameters
    ///
    /// - `screenshotName`: the name to use for the storage, must be unique!
    ///
    /// #### Returns
    ///
    /// @return true if the screenshots are identical or no prior screenshot exists
    /// or if the test can't be run on this device. False if a screenshot exists and
    /// it isn't 100% identical.
    public static boolean screenshotTest(String screenshotName) {
        if (verbose) {
            log("screenshotTest(" + screenshotName + ")");
        }
        try {
            ImageIO io = ImageIO.getImageIO();
            if (io == null || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
                log("screenshot test skipped due to no image IO support for PNG format");
                return true;
            }


            Image mute = (Image) callOnEdt(new EdtCall() {
                @Override
                Object call() {
                    Image img = Display.getInstance().captureScreen();
                    Form f = Display.getInstance().getCurrent();
                    assertBool(f != null, "No form is showing");
                    f.paintComponent(img.getGraphics(), true);
                    return img;
                }
            });
            screenshotName = screenshotName + ".png";
            if (Storage.getInstance().exists(screenshotName)) {
                Image orig = Image.createImage(Storage.getInstance().createInputStream(screenshotName));
                if (!imagesWithinTolerance(mute, orig)) {
                    log("screenshots do not match within tolerance; saving additional image under " + screenshotName + ".fail");
                    io.save(mute, Storage.getInstance().createOutputStream(screenshotName + ".fail"), ImageIO.FORMAT_PNG, 1);
                    return false;
                }
            } else {
                io.save(mute, Storage.getInstance().createOutputStream(screenshotName), ImageIO.FORMAT_PNG, 1);
            }
            return true;
        } catch (IOException err) {
            log(err);
            return false;
        }
    }

    private static boolean imagesWithinTolerance(Image candidate, Image baseline) {
        int[] candidateRgba = candidate.getRGBCached();
        int[] baselineRgba = baseline.getRGBCached();
        if (candidateRgba.length != baselineRgba.length) {
            log("Screenshot sizes differ: candidate has " + candidateRgba.length + " pixels while baseline has " + baselineRgba.length);
            return false;
        }

        // Allow small, widespread differences and a tiny proportion of slightly larger
        // differences to accommodate minor rendering artifacts. These values are tuned
        // to keep comparisons strict while ignoring harmless noise we see across
        // devices/runs.
        final int toleratedChannelDelta = 3; // Difference that triggers "pixel differs" counting
        final double maxDifferentPixelsRatio = 0.01; // Up to 1% of pixels may exceed toleratedChannelDelta
        final double maxAverageChannelDelta = 2.5; // Average absolute delta per channel across whole image
        final double maxRmsChannelDelta = 5.0; // RMS delta per channel across whole image

        int differingPixels = 0;
        long totalChannelDelta = 0;
        long totalChannelDeltaSquared = 0;

        for (int iter = 0; iter < candidateRgba.length; iter++) {
            int candidatePixel = candidateRgba[iter];
            int baselinePixel = baselineRgba[iter];

            int deltaR = Math.abs(((candidatePixel >> 16) & 0xff) - ((baselinePixel >> 16) & 0xff));
            int deltaG = Math.abs(((candidatePixel >> 8) & 0xff) - ((baselinePixel >> 8) & 0xff));
            int deltaB = Math.abs((candidatePixel & 0xff) - (baselinePixel & 0xff));
            int deltaA = Math.abs(((candidatePixel >> 24) & 0xff) - ((baselinePixel >> 24) & 0xff));

            int maxChannelDelta = Math.max(Math.max(deltaR, deltaG), Math.max(deltaB, deltaA));
            if (maxChannelDelta > toleratedChannelDelta) {
                differingPixels++;
            }

            totalChannelDelta += deltaR + deltaG + deltaB + deltaA;
            totalChannelDeltaSquared += deltaR * (long) deltaR
                    + deltaG * (long) deltaG
                    + deltaB * (long) deltaB
                    + deltaA * (long) deltaA;
        }

        double differentPixelRatio = differingPixels / (double) candidateRgba.length;
        double averageChannelDelta = totalChannelDelta / (double) (candidateRgba.length * 4);
        double rmsChannelDelta = Math.sqrt(totalChannelDeltaSquared / (double) (candidateRgba.length * 4));

        boolean withinThresholds = differentPixelRatio <= maxDifferentPixelsRatio
                && averageChannelDelta <= maxAverageChannelDelta
                && rmsChannelDelta <= maxRmsChannelDelta;

        if (!withinThresholds) {
            log("Screenshot differs: "
                    + (differentPixelRatio * 100.0) + "% pixels exceed delta " + toleratedChannelDelta
                    + ", avg channel delta=" + averageChannelDelta
                    + ", rms channel delta=" + rmsChannelDelta);
        }

        return withinThresholds;
    }

    /// Log to the test log
    ///
    /// #### Parameters
    ///
    /// - `t`: the string to log
    public static void log(String t) {
        TestReporting.getInstance().logMessage(t);
    }

    /// Log to the test log
    ///
    /// #### Parameters
    ///
    /// - `t`: exception to log
    public static void log(Throwable t) {
        TestReporting.getInstance().logException(t);
    }


    /// Simulates a device key press
    ///
    /// #### Parameters
    ///
    /// - `keyCode`: the keycode
    public static void keyPress(int keyCode) {
        if (verbose) {
            log("keyPress(" + keyCode + ")");
        }
        keyEvent(true, keyCode);
        waitFor(10);
    }

    /// Simulates a device key release
    ///
    /// #### Parameters
    ///
    /// - `keyCode`: the keycode
    public static void keyRelease(int keyCode) {
        if (verbose) {
            log("keyRelease(" + keyCode + ")");
        }
        keyEvent(false, keyCode);
        waitFor(10);
    }

    /// Simulates a game key press
    ///
    /// #### Parameters
    ///
    /// - `gameKey`: the game key (arrows etc.)
    public static void gameKeyPress(int gameKey) {
        if (verbose) {
            log("gameKeyPress(" + gameKey + ")");
        }
        keyEvent(true, Display.getInstance().getKeyCode(gameKey));
        waitFor(10);
    }

    /// Simulates a game key release
    ///
    /// #### Parameters
    ///
    /// - `gameKey`: the game key (arrows etc.)
    public static void gameKeyRelease(int gameKey) {
        if (verbose) {
            log("gameKeyRelease(" + gameKey + ")");
        }
        keyEvent(false, Display.getInstance().getKeyCode(gameKey));
        waitFor(10);
    }

    /// A component press on a given named component at x/y where x and y are **NOT pixels**
    /// but rather a number between 0 to 1 representing the percentage within the component where the
    /// event took place. E.g. For a 100x100 component a press within 10,5 would be 0.1f, 0.05f.
    ///
    /// #### Parameters
    ///
    /// - `x`: the offset within the component as a number between 0 and 1
    ///
    /// - `y`: the offset within the component as a number between 0 and 1
    ///
    /// - `componentName`: the name of the component
    public static void pointerPress(float x, float y, String componentName) {
        if (verbose) {
            log("pointerPress(" + x + ", " + y + ", " + componentName + ")");
        }
        waitFor(20);
        if (componentName != null) {
            int[] p = pointIn(findByName(componentName), x, y, componentName);
            pointerEvent(POINTER_PRESS, p[0], p[1]);
        } else {
            pointerEvent(POINTER_PRESS, (int) (x * Display.getInstance().getDisplayWidth()),
                    (int) (y * Display.getInstance().getDisplayHeight()));
        }
        waitFor(10);
    }

    /// A component release on a given named component at x/y where x and y are **NOT pixels**
    /// but rather a number between 0 to 1 representing the percentage within the component where the
    /// event took place. E.g. For a 100x100 component a press within 10,5 would be 0.1f, 0.05f.
    ///
    /// #### Parameters
    ///
    /// - `x`: the offset within the component as a number between 0 and 1
    ///
    /// - `y`: the offset within the component as a number between 0 and 1
    ///
    /// - `componentName`: the name of the component
    public static void pointerRelease(float x, float y, String componentName) {
        if (verbose) {
            log("pointerRelease(" + x + ", " + y + ", " + componentName + ")");
        }
        int[] p = pointIn(findByName(componentName), x, y, componentName);
        pointerEvent(POINTER_RELEASE, p[0], p[1]);
        waitFor(30);
    }

    /// A component drag on a given named component at x/y where x and y are **NOT pixels**
    /// but rather a number between 0 to 1 representing the percentage within the component where the
    /// event took place. E.g. For a 100x100 component a press within 10,5 would be 0.1f, 0.05f.
    ///
    /// #### Parameters
    ///
    /// - `x`: the offset within the component as a number between 0 and 1
    ///
    /// - `y`: the offset within the component as a number between 0 and 1
    ///
    /// - `componentName`: the name of the component
    public static void pointerDrag(float x, float y, String componentName) {
        if (verbose) {
            log("pointerDrag(" + x + ", " + y + ", " + componentName + ")");
        }
        int[] p = pointIn(findByName(componentName), x, y, componentName);
        pointerEvent(POINTER_DRAG, p[0], p[1]);
    }

    /// A component press on a given named component at x/y where x and y are **NOT pixels**
    /// but rather a number between 0 to 1 representing the percentage within the component where the
    /// event took place. E.g. For a 100x100 component a press within 10,5 would be 0.1f, 0.05f.
    ///
    /// #### Parameters
    ///
    /// - `x`: the offset within the component as a number between 0 and 1
    ///
    /// - `y`: the offset within the component as a number between 0 and 1
    ///
    /// - `path`: the path to the component
    public static void pointerPress(float x, float y, int[] path) {
        if (verbose) {
            log("pointerPress(" + x + ", " + y + ", " + toString(path) + ")");
        }
        int[] p = pointIn(getComponentByPath(path), x, y, toString(path));
        pointerEvent(POINTER_PRESS, p[0], p[1]);
        waitFor(10);
    }

    /// A component release on a given named component at x/y where x and y are **NOT pixels**
    /// but rather a number between 0 to 1 representing the percentage within the component where the
    /// event took place. E.g. For a 100x100 component a press within 10,5 would be 0.1f, 0.05f.
    ///
    /// #### Parameters
    ///
    /// - `x`: the offset within the component as a number between 0 and 1
    ///
    /// - `y`: the offset within the component as a number between 0 and 1
    ///
    /// - `path`: the path to the component
    public static void pointerRelease(float x, float y, int[] path) {
        if (verbose) {
            log("pointerRelease(" + x + ", " + y + ", " + toString(path) + ")");
        }
        int[] p = pointIn(getComponentByPath(path), x, y, toString(path));
        pointerEvent(POINTER_RELEASE, p[0], p[1]);
        waitFor(10);
    }

    /// A component drag on a given named component at x/y where x and y are **NOT pixels**
    /// but rather a number between 0 to 1 representing the percentage within the component where the
    /// event took place. E.g. For a 100x100 component a press within 10,5 would be 0.1f, 0.05f.
    ///
    /// #### Parameters
    ///
    /// - `x`: the offset within the component as a number between 0 and 1
    ///
    /// - `y`: the offset within the component as a number between 0 and 1
    ///
    /// - `path`: the path to the component
    public static void pointerDrag(float x, float y, int[] path) {
        if (verbose) {
            log("pointerDrag(" + x + ", " + y + ", " + toString(path) + ")");
        }
        int[] p = pointIn(getComponentByPath(path), x, y, toString(path));
        pointerEvent(POINTER_DRAG, p[0], p[1]);
    }

    /// Gets the component from the current form based on its path. A path is a
    /// set of offsets starting from the content pane and moving inwards so a path
    /// of { 0, 3 } would mean that the first component within the Content pane (by
    /// index) is a Container whose 3rd component (again by index) is the component we
    /// want.
    ///
    /// #### Parameters
    ///
    /// - `path`: an array
    ///
    /// #### Returns
    ///
    /// a component
    public static Component getComponentByPath(final int[] path) {
        return (Component) callOnEdt(new Lookup() {
            @Override
            Object find(Form root) {
                Component current = root.getContentPane();
                for (int offset : path) {
                    if (!(current instanceof Container)
                            || offset < 0 || offset >= ((Container) current).getComponentCount()) {
                        return null;
                    }
                    current = ((Container) current).getComponentAt(offset);
                }
                return current;
            }
        });
    }


    /// Sets the text for the given component
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the component
    ///
    /// - `text`: the text to set
    public static void setText(String name, final String text) {
        if (verbose) {
            log("setText(" + name + ", " + text + ")");
        }
        final Component c = findByName(name);
        assertBool(c instanceof Label || c instanceof TextArea, "No label or text area found for " + name);
        actOnEdt(new EdtCall() {
            @Override
            Object call() {
                if (c instanceof Label) {
                    ((Label) c).setText(text);
                    return null;
                }
                ((TextArea) c).setText(text);
                Display.getInstance().onEditingComplete(c, text);
                return null;
            }
        });
    }

    /// Sets the text for the given component
    ///
    /// #### Parameters
    ///
    /// - `path`: the path to the component
    ///
    /// - `text`: the text to set
    public static void setText(int[] path, final String text) {
        if (verbose) {
            log("setText(" + toString(path) + ", " + text + ")");
        }
        final Component c = getComponentByPath(path);
        assertBool(c instanceof Label || c instanceof TextArea, "No label or text area found for " + toString(path));
        actOnEdt(new EdtCall() {
            @Override
            Object call() {
                if (c instanceof Label) {
                    ((Label) c).setText(text);
                    return null;
                }
                ((TextArea) c).setText(text);
                return null;
            }
        });
    }

    /// Assertions allow for simpler test code
    ///
    /// #### Parameters
    ///
    /// - `b`: must be true, otherwise an exception is thrown thus failing the test
    public static void assertBool(boolean b) {
        if (verbose) {
            log("assertBool(" + b + ")");
        }
        if (!b) {
            throw new RuntimeException();
        }
    }

    /// Assertions allow for simpler test code
    ///
    /// #### Parameters
    ///
    /// - `b`: must be true, otherwise an exception is thrown thus failing the test
    public static void assertBool(boolean b, String errorMessage) {
        if (verbose) {
            log("assertBool(" + b + ", " + errorMessage + ")");
        }
        if (!b) {
            log("Assert failed on: " + errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    /// An assertion that always fails a test.
    public static void fail() {
        if (verbose) {
            log("fail()");
        }
        assertBool(false);
    }

    /// An assertion that always fails a test.
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void fail(String errorMessage) {
        if (verbose) {
            log("fail(" + errorMessage + ")");
        }
        assertBool(false, errorMessage);
    }

    /// Asserts that the given expression evaluates to true
    public static void assertTrue(boolean value) {
        if (verbose) {
            log("assertTrue(" + value + ")");
        }
        assertBool(value);
    }

    /// Asserts that the given expression evaluates to true
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertTrue(boolean value, String errorMessage) {
        if (verbose) {
            log("assertTrue(" + value + ", " + errorMessage + ")");
        }
        assertBool(value, errorMessage);
    }

    /// Asserts that the given expression evaluates to false
    public static void assertFalse(boolean value) {
        if (verbose) {
            log("assertFalse(" + value + ")");
        }
        assertBool(!value);
    }

    /// Asserts that the given expression evaluates to false
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertFalse(boolean value, String errorMessage) {
        if (verbose) {
            log("assertFalse(" + value + ", " + errorMessage + ")");
        }
        assertBool(!value, errorMessage);
    }

    /// Asserts that the given expression evaluates to null
    public static void assertNull(Object object) {
        if (verbose) {
            log("assertNull(" + object + ")");
        }
        assertBool(object == null);
    }

    /// Asserts that the given expression evaluates to null
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertNull(Object object, String errorMessage) {
        if (verbose) {
            log("assertNull(" + object + ", " + errorMessage + ")");
        }
        assertBool(object == null, errorMessage);
    }

    /// Asserts that the given expression does not evaluate to null
    public static void assertNotNull(Object object) {
        if (verbose) {
            log("assertNotNull(" + object + ")");
        }
        assertBool(object != null);
    }

    /// Asserts that the given expression does not evaluate to null
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertNotNull(Object object, String errorMessage) {
        if (verbose) {
            log("assertNotNull(" + object + ", " + errorMessage + ")");
        }
        assertBool(object != null, errorMessage);
    }

    /// Asserts that the given parameters reference the same object
    public static void assertSame(Object expected, Object actual) {
        if (verbose) {
            log("assertSame(" + expected + ", " + actual + ")");
        }
        assertBool(expected == actual); //NOPMD CompareObjectsWithEquals
    }

    /// Asserts that the given parameters reference the same object
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertSame(Object expected, Object actual, String errorMessage) {
        if (verbose) {
            log("assertSame(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected == actual, errorMessage); //NOPMD CompareObjectsWithEquals
    }

    /// Asserts that the given parameters do not reference the same object
    public static void assertNotSame(Object expected, Object actual) {
        if (verbose) {
            log("assertNotSame(" + expected + ", " + actual + ")");
        }
        assertBool(expected != actual); //NOPMD CompareObjectsWithEquals
    }

    /// Asserts that the given parameters do not reference the same object
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertNotSame(Object expected, Object actual, String errorMessage) {
        if (verbose) {
            log("assertNotSame(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected != actual, errorMessage); //NOPMD CompareObjectsWithEquals
    }

    private static void assertRelativeErrorExceeded(float expected, float actual, double minRelativeError) {
        if (verbose) {
            log("assertRelativeErrorExceeded(" + expected + ", " + actual + ", " + minRelativeError + ")");
        }
        double relativeError = Math.abs((expected - actual) / actual) * 100;
        if (relativeError < minRelativeError) {
            assertBool(false);
        }
    }

    private static void assertRelativeErrorExceeded(double expected, double actual, double minRelativeError) {
        if (verbose) {
            log("assertRelativeErrorExceeded(" + expected + ", " + actual + ", " + minRelativeError + ")");
        }
        double relativeError = Math.abs((expected - actual) / actual) * 100;
        if (relativeError < minRelativeError) {
            assertBool(false);
        }
    }

    private static void assertRelativeErrorExceeded(float expected, float actual, double minRelativeError, String errorMessage) {
        if (verbose) {
            log("assertRelativeErrorExceeded(" + expected + ", " + actual + ", " + minRelativeError + ", " + errorMessage + ")");
        }
        double relativeError = Math.abs((expected - actual) / actual) * 100;
        if (relativeError < minRelativeError) {
            assertBool(false, errorMessage);
        }
    }

    private static void assertRelativeErrorExceeded(double expected, double actual, double minRelativeError, String errorMessage) {
        if (verbose) {
            log("assertRelativeErrorExceeded(" + expected + ", " + actual + ", " + minRelativeError + ", " + errorMessage + ")");
        }
        double relativeError = Math.abs((expected - actual) / actual) * 100;
        if (relativeError < minRelativeError) {
            assertBool(false, errorMessage);
        }
    }

    private static void assertRelativeErrorNotExceeded(float expected, float actual, double maxRelativeError, String errorMessage) {
        if (verbose) {
            log("assertRelativeErrorNotExceeded(" + expected + ", " + actual + ", " + maxRelativeError + ", " + errorMessage + ")");
        }
        double relativeError = Math.abs((expected - actual) / actual) * 100;
        if (relativeError > maxRelativeError) {
            assertBool(false, errorMessage);
        }
    }

    private static void assertRelativeErrorNotExceeded(double expected, double actual, double maxRelativeError, String errorMessage) {
        if (verbose) {
            log("assertRelativeErrorNotExceeded(" + expected + ", " + actual + ", " + maxRelativeError + ", " + errorMessage + ")");
        }
        double relativeError = Math.abs((expected - actual) / actual) * 100;
        if (relativeError > maxRelativeError) {
            assertBool(false, errorMessage);
        }
    }

    private static void assertErrorNotExceeded(double expected, double actual, double maxError, String errorMessage) {
        if (verbose) {
            log("assertErrorNotExceeded(" + expected + ", " + actual + ", " + maxError + ", " + errorMessage + ")");
        }
        double error = Math.abs((expected - actual));
        if (error > maxError) {
            assertBool(false, errorMessage);
        }
    }

    /// Asserts that the given bytes are equal
    public static void assertEqual(byte expected, byte actual) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected == actual);
    }

    /// Asserts that the given bytes are equal
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertEqual(byte expected, byte actual, String errorMessage) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected == actual, errorMessage);
    }

    /// Asserts that the given shorts are equal
    public static void assertEqual(short expected, short actual) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected == actual);
    }

    /// Asserts that the given shorts are equal
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertEqual(short expected, short actual, String errorMessage) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected == actual, errorMessage);
    }

    /// Asserts that the given ints are equal
    public static void assertEqual(int expected, int actual) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected == actual, "Expected [" + expected + "], Actual [" + actual + "]");
    }

    /// Asserts that the given ints are equal
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertEqual(int expected, int actual, String errorMessage) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected == actual, errorMessage + ";" + "Expected [" + expected + "], Actual [" + actual + "]");
    }

    /// Asserts that the given longs are equal
    public static void assertEqual(long expected, long actual) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected == actual, "Expected [" + expected + "], Actual [" + actual + "]");
    }

    /// Asserts that the given longs are equal
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertEqual(long expected, long actual, String errorMessage) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected == actual, errorMessage + "; " + "Expected [" + expected + "], Actual [" + actual + "]");
    }

    /// Asserts that the given floats are equal
    ///
    /// #### Parameters
    ///
    /// - `maxRelativeError`: is the maximum allowed error, a value of 1 represents a 1% error.
    public static void assertEqual(float expected, float actual, double maxRelativeError) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        if (expected != actual) {
            assertRelativeErrorNotExceeded(expected, actual, maxRelativeError, "Expected [" + expected + "], Actual [" + actual + "]");
        }
    }

    /// Asserts that the given floats are equal
    ///
    /// #### Parameters
    ///
    /// - `maxRelativeError`: is the maximum allowed error, a value of 1 represents a 1% error.
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertEqual(float expected, float actual, double maxRelativeError, String errorMessage) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        if (expected != actual) {
            assertRelativeErrorNotExceeded(expected, actual, maxRelativeError, errorMessage + "; " + "Expected [" + expected + "], Actual [" + actual + "]");
        }
    }

    /// Asserts that the given doubles are equal
    ///
    /// #### Parameters
    ///
    /// - `maxRelativeError`: is the maximum allowed error, a value of 1 represents a 1% error.
    public static void assertEqual(double expected, double actual, double maxRelativeError) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        if (expected != actual) {
            assertRelativeErrorNotExceeded(expected, actual, maxRelativeError, "Expected [" + expected + "], Actual [" + actual + "]");
        }
    }

    /// Asserts that the given doubles are equal
    ///
    /// #### Parameters
    ///
    /// - `expected`: Expected value
    ///
    /// - `actual`: Actual value
    ///
    /// - `absoluteError`: is the maximum allowed error, a value of 1 represents a 1% error.
    ///
    public static void assertRange(double expected, double actual, double absoluteError) {
        if (verbose) {
            log("assertRange(" + expected + ", " + actual + ")");
        }
        if (expected != actual) {
            assertErrorNotExceeded(expected, actual, absoluteError, "Expected [" + expected + "], Actual [" + actual + "]");
        }
    }


    /// Asserts that the given doubles are equal
    ///
    /// #### Parameters
    ///
    /// - `maxRelativeError`: is the maximum allowed error, a value of 1 represents a 1% error.
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertEqual(double expected, double actual, double maxRelativeError, String errorMessage) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        if (expected != actual) {
            assertRelativeErrorNotExceeded(expected, actual, maxRelativeError, errorMessage + "; " + "Expected [" + expected + "], Actual [" + actual + "]");
        }
    }

    /// Asserts that the given doubles are equal
    ///
    /// #### Parameters
    ///
    /// - `maxRelativeError`: is the maximum allowed error, a value of 1 represents a 1% error.
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertRange(double expected, double actual, double maxAbsoluteError, String errorMessage) {
        if (verbose) {
            log("assertRange(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        if (expected != actual) {
            assertErrorNotExceeded(expected, actual, maxAbsoluteError, errorMessage + "; " + "Expected [" + expected + "], Actual [" + actual + "]");
        }
    }

    /// Asserts that the given objects are equal using the first object's .equal() method
    public static void assertEqual(Object expected, Object actual) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ")");
        }
        assertBool(Objects.equals(expected, actual), "Expected [" + expected + "], Actual [" + actual + "]");
    }

    /// Asserts that the given objects are equal using the first object's .equal() method
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertEqual(Object expected, Object actual, String errorMessage) {
        if (verbose) {
            log("assertEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(Objects.equals(expected, actual), errorMessage + "; " + "Expected [" + expected + "], Actual [" + actual + "]");
    }

    /// Asserts that the given bytes are not equal
    public static void assertNotEqual(byte expected, byte actual) {
        if (verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected != actual);
    }

    /// Asserts that the given bytes are not equal
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertNotEqual(byte expected, byte actual, String errorMessage) {
        if (verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected != actual, errorMessage);
    }

    /// Asserts that the given shorts are not equal
    public static void assertNotEqual(short expected, short actual) {
        if (verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected != actual);
    }

    /// Asserts that the given shorts are not equal
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertNotEqual(short expected, short actual, String errorMessage) {
        if (verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected != actual, errorMessage);
    }

    /// Asserts that the given ints are not equal
    public static void assertNotEqual(int expected, int actual) {
        if (verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ")");
        }
        assertBool(expected != actual);
    }

    /// Asserts that the given ints are not equal
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertNotEqual(int expected, int actual, String errorMessage) {
        if (verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        assertBool(expected != actual, errorMessage);
    }

    /// Asserts that the given longs are not equal
    public static void assertNotEqual(long expected, long actual, double minRelativeError) {
        if (verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + minRelativeError + ")");
        }
        if (expected == actual) {
            assertBool(false);
        } else {
            assertRelativeErrorExceeded(expected, actual, minRelativeError);
        }
    }

    /// Asserts that the given longs are not equal
    ///
    /// #### Parameters
    ///
    /// - `minRelativeError`: is the minimum allowed error, a value of 1 represents a 1% error.
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertNotEqual(long expected, long actual, double minRelativeError, String errorMessage) {
        if (verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + minRelativeError + ", " + errorMessage + ")");
        }
        if (expected == actual) {
            TestUtils.assertBool(false, errorMessage);
        } else {
            assertRelativeErrorExceeded(expected, actual, minRelativeError, errorMessage);
        }
    }

    /// Asserts that the given doubles are not equal
    ///
    /// #### Parameters
    ///
    /// - `minRelativeError`: is the minimum allowed error, a value of 1 represents a 1% error.
    public static void assertNotEqual(double expected, double actual, double minRelativeError) {
        if (verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + minRelativeError + ")");
        }
        if (expected == actual) {
            TestUtils.assertBool(false);
        } else {
            assertRelativeErrorExceeded(expected, actual, minRelativeError);
        }
    }

    /// Asserts that the given doubles are not equal
    ///
    /// #### Parameters
    ///
    /// - `minRelativeError`: is the minimum allowed error, a value of 1 represents a 1% error.
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertNotEqual(double expected, double actual, double minRelativeError, String errorMessage) {
        if (verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + minRelativeError + ", " + errorMessage + ")");
        }
        if (expected == actual) {
            TestUtils.assertBool(false, errorMessage);
        } else {
            assertRelativeErrorExceeded(expected, actual, minRelativeError, errorMessage);
        }
    }

    /// Asserts that the given objects are not equal using the first object's .equal() method
    public static void assertNotEqual(Object expected, Object actual) {
        if (verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ")");
        }
        if (Objects.equals(expected, actual)) {
            TestUtils.fail("Assertion failed.  Expected not equal, but found that " + expected + " equals " + actual);
        }

    }

    /// Asserts that the given objects are not equal using the first object's .equal() method
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertNotEqual(Object expected, Object actual, String errorMessage) {
        if (verbose) {
            log("assertNotEqual(" + expected + ", " + actual + ", " + errorMessage + ")");
        }
        if (Objects.equals(expected, actual)) {
            TestUtils.fail("Assertion failed.  Expected not equal, but found that " + expected + " equals " + actual + ".  " + errorMessage);
        }
    }

    /// Pretty-prints a byte array
    private static String arrayToString(byte[] array) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /// Pretty-prints a short array
    private static String arrayToString(short[] array) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /// Pretty-prints a int array
    private static String arrayToString(int[] array) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /// Pretty-prints a long array
    private static String arrayToString(long[] array) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /// Pretty-prints a float array
    private static String arrayToString(float[] array) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /// Pretty-prints a double array
    private static String arrayToString(double[] array) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /// Pretty-prints an object array
    private static String arrayToString(Object[] array) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < array.length - 1; ++index) {
            result.append(array[index]).append(",");
        }
        if (array.length > 0) {
            result.append(array[array.length - 1]);
        }
        result.append("]");
        return result.toString();
    }

    /// Asserts that the given byte arrays are equal
    public static void assertArrayEqual(byte[] expected, byte[] actual) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index]);
        }
    }

    /// Asserts that the given byte arrays are equal
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertArrayEqual(byte[] expected, byte[] actual, String errorMessage) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index], errorMessage);
        }
    }

    /// Asserts that the given short arrays are equal
    public static void assertArrayEqual(short[] expected, short[] actual) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index]);
        }
    }

    /// Asserts that the given short arrays are equal
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertArrayEqual(short[] expected, short[] actual, String errorMessage) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index], errorMessage);
        }
    }

    /// Asserts that the given int arrays are equal
    public static void assertArrayEqual(int[] expected, int[] actual) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index]);
        }
    }

    /// Asserts that the given int arrays are equal
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertArrayEqual(int[] expected, int[] actual, String errorMessage) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index], errorMessage);
        }
    }

    /// Asserts that the given long arrays are equal
    ///
    /// #### Parameters
    ///
    /// - `maxRelativeError`: is the maximum allowed error, a value of 1 represents a 1% error.
    public static void assertArrayEqual(long[] expected, long[] actual, double maxRelativeError) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index], maxRelativeError);
        }
    }

    /// Asserts that the given long arrays are equal
    ///
    /// #### Parameters
    ///
    /// - `maxRelativeError`: is the maximum allowed error, a value of 1 represents a 1% error.
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertArrayEqual(long[] expected, long[] actual, double maxRelativeError, String errorMessage) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index], maxRelativeError, errorMessage);
        }
    }

    /// Asserts that the given float arrays are equal
    ///
    /// #### Parameters
    ///
    /// - `maxRelativeError`: is the maximum allowed error, a value of 1 represents a 1% error.
    public static void assertArrayEqual(float[] expected, float[] actual, double maxRelativeError) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index], maxRelativeError);
        }
    }

    /// Asserts that the given double arrays are equal
    ///
    /// #### Parameters
    ///
    /// - `maxRelativeError`: is the maximum allowed error, a value of 1 represents a 1% error.
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertArrayEqual(float[] expected, float[] actual, double maxRelativeError, String errorMessage) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index], maxRelativeError, errorMessage);
        }
    }

    /// Asserts that the given double arrays are equal
    ///
    /// #### Parameters
    ///
    /// - `maxRelativeError`: is the maximum allowed error, a value of 1 represents a 1% error.
    public static void assertArrayEqual(double[] expected, double[] actual, double maxRelativeError) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index], maxRelativeError);
        }
    }

    /// Asserts that the given double arrays are equal
    ///
    /// #### Parameters
    ///
    /// - `maxRelativeError`: is the maximum allowed error, a value of 1 represents a 1% error.
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertArrayEqual(double[] expected, double[] actual, double maxRelativeError, String errorMessage) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index], maxRelativeError, errorMessage);
        }
    }

    /// Asserts that the given object arrays are equal
    public static void assertArrayEqual(Object[] expected, Object[] actual) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index]);
        }
    }

    /// Asserts that the given object arrays are equal
    ///
    /// #### Parameters
    ///
    /// - `errorMessage`: is a string describing the failure
    public static void assertArrayEqual(Object[] expected, Object[] actual, String errorMessage) {
        if (verbose) {
            log("assertArrayEqual(" + arrayToString(expected) + ", " + arrayToString(actual) + ", " + errorMessage + ")");
        }
        if (expected.length != actual.length) {
            TestUtils.assertBool(false);
        }
        for (int index = 0; index < expected.length; ++index) {
            assertEqual(expected[index], actual[index], errorMessage);
        }
    }

    /// Asserts that a certain exception is raised
    ///
    /// #### Parameters
    ///
    /// - `exception`
    ///
    /// - `expression`
    public static void assertException(RuntimeException exception, Runnable expression) {
        if (verbose) {
            log("assertException(" + exception + ", " + expression + ")");
        }
        try {
            expression.run();
        } catch (RuntimeException failure) {
            if (exception.getClass() != failure.getClass()) {
                TestUtils.fail();
            }
            return;
        }
        TestUtils.fail();
    }

    /// Asserts that a certain exception is raised
    ///
    /// #### Parameters
    ///
    /// - `exception`
    ///
    /// - `expression`
    ///
    /// - `errorMessage`
    public static void assertException(RuntimeException exception, Runnable expression, String errorMessage) {
        if (verbose) {
            log("assertException(" + exception + ", " + expression + ", " + errorMessage + ")");
        }
        try {
            expression.run();
        } catch (RuntimeException failure) {
            if (exception.getClass() != failure.getClass()) {
                TestUtils.fail(errorMessage);
            }
            return;
        }
        TestUtils.fail(errorMessage);
    }

    /// Asserts that no exception is raised
    ///
    /// #### Parameters
    ///
    /// - `expression`
    public static void assertNoException(Runnable expression) {
        if (verbose) {
            log("assertNoException(" + expression + ")");
        }
        try {
            expression.run();
        } catch (RuntimeException failure) {
            TestUtils.fail();
        }
    }

    /// Asserts that no exception is raised
    ///
    /// #### Parameters
    ///
    /// - `expression`
    ///
    /// - `errorMessage`
    public static void assertNoException(Runnable expression, String errorMessage) {
        if (verbose) {
            log("assertNoException(" + expression + ", " + errorMessage + ")");
        }
        try {
            expression.run();
        } catch (RuntimeException failure) {
            TestUtils.fail(errorMessage);
        }
    }

    /// Verifies the current title is the same otherwise throws an exception
    ///
    /// #### Parameters
    ///
    /// - `title`: the tile to verify
    public static void assertTitle(String title) {
        if (verbose) {
            log("assertTitle(" + title + ")");
        }
        String current = (String) callOnEdt(new Lookup() {
            @Override
            Object find(Form root) {
                return root.getTitle();
            }
        });
        assertBool(title != null && title.equals(current), title);
    }

    /// Asserts that we have a label with the given text baring the given name
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the label
    ///
    /// - `text`: the text of the label
    public static void assertLabel(String name, String text) {
        if (verbose) {
            log("assertLabel(" + name + ", " + text + ")");
        }
        Component l = findByName(name);
        assertKind(l, l instanceof Label, "Label", name);
        assertBool(Objects.equals(text, textOf(l)), name + " != " + text);
    }

    /// Asserts that we have a label with the given text baring the given name
    ///
    /// #### Parameters
    ///
    /// - `path`: the path of the label
    ///
    /// - `text`: the text of the label
    public static void assertLabel(int[] path, String text) {
        if (verbose) {
            log("assertLabel(" + toString(path) + ", " + text + ")");
        }
        Component l = getComponentByPath(path);
        assertKind(l, l instanceof Label, "Label", toString(path));
        String actual = textOf(l);
        assertBool(Objects.equals(text, actual), actual + " != " + text);
    }

    /// Asserts that we have a label with the given text baring the given name
    ///
    /// #### Parameters
    ///
    /// - `text`: the text of the label
    public static void assertLabel(String text) {
        if (verbose) {
            log("assertLabel(" + text + ")");
        }
        Label l = findLabelText(text);
        assertBool(l != null, "Null label " + text);
    }

    /// Asserts that we have a TextArea with the given text and the given name
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the TextArea
    ///
    /// - `text`: the text of the TextArea
    public static void assertTextArea(String name, String text) {
        if (verbose) {
            log("assertTextArea(" + name + ", " + text + ")");
        }
        Component l = findByName(name);
        assertKind(l, l instanceof TextArea, "TextArea", name);
        String actual = textOf(l);
        assertBool(actual != null && actual.equals(text), "assertTextArea: " + actual + " != " + text);
    }

    /// Asserts that we have a TextArea with the a text contains the given text and with the given name
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the TextArea
    ///
    /// - `text`: the sequence to search for in the TextArea
    public static void assertTextAreaContaining(String name, String text) {
        if (verbose) {
            log("assertTextAreaContaining(" + name + ", " + text + ")");
        }
        Component l = findByName(name);
        assertKind(l, l instanceof TextArea, "TextArea", name);
        String actual = textOf(l);
        assertBool(actual != null && actual.indexOf(text) > -1, "assertTextArea: \"" + actual + "\" is not containing: \"" + text + "\"");
    }


    /// Asserts that we have a TextArea with the a text starting with the given text and with the given name
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the TextArea
    ///
    /// - `text`: the prefix to search for in the TextArea
    public static void assertTextAreaStartingWith(String name, String text) {
        if (verbose) {
            log("assertTextAreaStartingWith(" + name + ", " + text + ")");
        }
        Component l = findByName(name);
        assertKind(l, l instanceof TextArea, "TextArea", name);
        String actual = textOf(l);
        assertBool(actual != null && actual.startsWith(text), "assertTextArea: \"" + actual + "\" is not starting with: \"" + text + "\"");
    }

    /// Asserts that we have a TextArea with the a text ending with the given text and with the given name
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the TextArea
    ///
    /// - `text`: the suffix to search for in the TextArea
    public static void assertTextAreaEndingWith(String name, String text) {
        if (verbose) {
            log("assertTextAreaEndingWith(" + name + ", " + text + ")");
        }
        Component l = findByName(name);
        assertKind(l, l instanceof TextArea, "TextArea", name);
        String actual = textOf(l);
        assertBool(actual != null && actual.endsWith(text), "assertTextArea: \"" + actual + "\" is not ending with: \"" + text + "\"");
    }

    /// Asserts that we have a label with the given text baring the given name
    ///
    /// #### Parameters
    ///
    /// - `path`: the path to the text area
    ///
    /// - `text`: the text of the label
    public static void assertTextArea(int[] path, String text) {
        if (verbose) {
            log("assertTextArea(" + toString(path) + ", " + text + ")");
        }
        Component l = getComponentByPath(path);
        assertKind(l, l instanceof TextArea, "TextArea", toString(path));
        String actual = textOf(l);
        assertBool(actual != null && actual.equals(text), "assertTextArea: " + actual + " != " + text);
    }

    /// Asserts that we have a label with the given text baring the given name
    ///
    /// #### Parameters
    ///
    /// - `text`: the text of the label
    public static void assertTextArea(String text) {
        if (verbose) {
            log("assertTextArea(" + text + ")");
        }
        TextArea l = findTextAreaText(text);
        assertBool(l != null, "Null text " + text);
    }

    /// Finds a component with the given name, works even with UI's that weren't created with the GUI builder
    ///
    /// #### Parameters
    ///
    /// - `text`: the text of the label/button
    ///
    /// #### Returns
    ///
    /// the component with the given TextArea text within the tree
    public static TextArea findTextAreaText(final String text) {
        return (TextArea) callOnEdt(new Lookup() {
            @Override
            Object find(Form root) {
                return findTextAreaText(root, text);
            }
        });
    }

    /// Finds a component with the given name, works even with UI's that weren't created with the GUI builder
    ///
    /// #### Parameters
    ///
    /// - `text`: the text of the label/button
    ///
    /// #### Returns
    ///
    /// the component with the given label text within the tree
    private static TextArea findTextAreaText(Container root, String text) {
        int count = root.getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            Component c = root.getComponentAt(iter);
            if (c instanceof TextArea) {
                String n = ((TextArea) c).getText();
                if (n != null && n.equals(text)) {
                    return (TextArea) c;
                }
                continue;
            }
            if (c instanceof Container) {
                TextArea l = findTextAreaText((Container) c, text);
                if (l != null) {
                    return l;
                }
            }
        }
        return null;
    }
}
