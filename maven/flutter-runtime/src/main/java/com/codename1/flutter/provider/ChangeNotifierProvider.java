package com.codename1.flutter.provider;

import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

/**
 * provider's {@code ChangeNotifierProvider<T extends ChangeNotifier>}: a
 * {@link Provider} specialised for {@code ChangeNotifier} values. The gallery
 * uses the {@code .value} form inside a {@code MultiProvider}; disposal of a
 * created notifier is not modeled in this pass.
 */
public class ChangeNotifierProvider extends Provider {

    /** The {@code ChangeNotifierProvider.value(value: ...)} named constructor. */
    public static ChangeNotifierProvider value(Key key, Object value, Widget child) {
        ChangeNotifierProvider p = new ChangeNotifierProvider();
        p.key(key);
        p.value(value);
        p.child(child);
        return p;
    }
}
