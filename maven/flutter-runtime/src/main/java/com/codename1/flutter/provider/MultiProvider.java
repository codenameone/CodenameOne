package com.codename1.flutter.provider;

import java.util.List;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * provider's {@code MultiProvider}: nests its {@code providers} around
 * {@code child} so each becomes an ancestor of the app subtree. The list order
 * is outermost-first (Flutter semantics), so the first provider ends up highest
 * in the tree.
 */
public class MultiProvider extends StatelessWidget {

    private List<SingleChildWidget> providers;
    private Widget child;

    public void providers(List<SingleChildWidget> v) {
        this.providers = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget build(BuildContext context) {
        Widget acc = child;
        if (providers != null) {
            for (int i = providers.size() - 1; i >= 0; i--) {
                SingleChildWidget p = providers.get(i);
                if (p != null) {
                    p.child(acc);
                    acc = p;
                }
            }
        }
        return acc;
    }
}
