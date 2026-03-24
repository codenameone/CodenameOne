package com.codenameone.playground;

import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.CN;
import com.codename1.ui.util.Resources;

/**
 * Host objects and helpers exposed to user scripts.
 */
public class PlaygroundContext {
    private static final ThreadLocal<PlaygroundContext> CURRENT = new ThreadLocal<PlaygroundContext>();

    public interface Logger {
        void log(String message);
    }

    private final Form hostForm;
    private final Container previewRoot;
    private final Resources theme;
    private final Logger logger;
    private Form shownForm;

    public PlaygroundContext(Form hostForm, Container previewRoot, Resources theme, Logger logger) {
        this.hostForm = hostForm;
        this.previewRoot = previewRoot;
        this.theme = theme;
        this.logger = logger;
    }

    public Form getHostForm() {
        return hostForm;
    }

    public Container getPreviewRoot() {
        return previewRoot;
    }

    public Resources getTheme() {
        return theme;
    }

    static void pushCurrent(PlaygroundContext context) {
        CURRENT.set(context);
    }

    static void clearCurrent() {
        CURRENT.remove();
    }

    public static PlaygroundContext getCurrent() {
        return CURRENT.get();
    }

    public static void debug(String message) {
    }

    public static boolean interceptMethodInvocation(Object target, String methodName, Object[] args) {
        PlaygroundContext context = CURRENT.get();
        if (context == null) {
            return false;
        }
        if (!"show".equals(methodName) || (args != null && args.length != 0)) {
            return false;
        }
        if (target instanceof Dialog) {
            context.log("Dialog opened modelessly in the playground.");
            return true;
        }
        if (target instanceof Form) {
            context.captureShownForm((Form) target);
            return true;
        }
        return false;
    }

    public void log(String message) {
        logger.log(message);
    }

    public void captureShownForm(Form form) {
        shownForm = form;
    }

    public Form getShownForm() {
        return shownForm;
    }

    public void clearShownForm() {
        shownForm = null;
    }

    public void clearPreview() {
        previewRoot.removeAll();
        previewRoot.revalidate();
    }

    public void refreshPreview() {
        previewRoot.revalidate();
    }

    public void setTitle(String title) {
        hostForm.setTitle(title);
        hostForm.revalidate();
    }
}
