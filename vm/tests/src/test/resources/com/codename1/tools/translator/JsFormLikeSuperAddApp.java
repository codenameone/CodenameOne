import java.util.ArrayList;

class JsFormLikeAnimationManager {
    boolean isAnimating() {
        return false;
    }
}

class JsFormLikeBaseContainer {
    final ArrayList<String> children = new ArrayList<String>();

    void addComponent(Object constraint, String cmp) {
        JsFormLikeAnimationManager a = getAnimationManager();
        if (a != null && a.isAnimating()) {
            throw new RuntimeException("Unexpected animation branch");
        }
        insertComponentAtImpl(cmp);
    }

    void insertComponentAtImpl(String cmp) {
        children.add(cmp);
    }

    int getComponentCount() {
        return children.size();
    }

    String getComponentAt(int index) {
        return children.get(index);
    }

    JsFormLikeAnimationManager getAnimationManager() {
        return null;
    }
}

public class JsFormLikeSuperAddApp extends JsFormLikeBaseContainer {
    static int result;

    final JsFormLikeAnimationManager animManager = new JsFormLikeAnimationManager();
    final ArrayList<String> delegated = new ArrayList<String>();
    final String contentPane = "content";
    String titleArea = "title";

    JsFormLikeSuperAddApp() {
        addComponentToForm("north", titleArea);
        addComponentToForm("center", contentPane);
    }

    final void addComponentToForm(Object constraint, String cmp) {
        super.addComponent(constraint, cmp);
    }

    @Override
    void addComponent(Object constraint, String cmp) {
        delegated.add(cmp);
    }

    @Override
    JsFormLikeAnimationManager getAnimationManager() {
        return animManager;
    }

    public static void main(String[] args) {
        JsFormLikeSuperAddApp app = new JsFormLikeSuperAddApp();
        int mask = 0;
        if (app.getComponentCount() == 2) {
            mask |= 1;
        }
        if ("title".equals(app.getComponentAt(0))) {
            mask |= 2;
        }
        if ("content".equals(app.getComponentAt(1))) {
            mask |= 4;
        }
        if (app.delegated.isEmpty()) {
            mask |= 8;
        }
        result = mask;
        System.exit(result);
    }
}
