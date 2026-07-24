package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A builder that owns a scrap of local state, rebuilt via a {@code setState}
 * handed to its {@code builder} — Flutter's {@code StatefulBuilder}. The
 * {@code builder} receives the {@link BuildContext} and a state-setter
 * ({@code void Function(VoidCallback)}); calling it runs the mutation. This pass
 * builds once (the setter runs the mutation but the localized rebuild is
 * deferred to the element machinery).
 */
public class StatefulBuilder extends StatelessWidget {

    private Funcs.Func2<BuildContext, Funcs.VoidFunc1<Funcs.VoidFunc0>, Widget> builder;

    public void builder(Funcs.Func2<BuildContext, Funcs.VoidFunc1<Funcs.VoidFunc0>, Widget> v) {
        this.builder = v;
    }

    @Override
    public Widget build(final BuildContext context) {
        if (builder == null) {
            return null;
        }
        Funcs.VoidFunc1<Funcs.VoidFunc0> setState = new Funcs.VoidFunc1<Funcs.VoidFunc0>() {
            @Override
            public void call(Funcs.VoidFunc0 fn) {
                if (fn != null) {
                    fn.call();
                }
            }
        };
        return builder.call(context, setState);
    }
}
