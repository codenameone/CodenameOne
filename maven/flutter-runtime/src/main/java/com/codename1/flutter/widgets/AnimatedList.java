package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Clip;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.AlwaysStoppedAnimation;
import com.codename1.flutter.animation.Animation;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A scrollable list that animates item insertion and removal — Flutter's
 * {@code AnimatedList}. This milestone eagerly materializes
 * {@code itemBuilder(context, index, animation)} for {@code 0..initialItemCount-1}
 * with a fully-arrived ({@code 1.0}) animation, laid out linearly; the
 * per-item enter/exit transitions driven through {@link AnimatedListState} are
 * deferred.
 */
public class AnimatedList extends StatelessWidget {

    private Funcs.Func3<BuildContext, Long, Animation<Double>, Widget> itemBuilder;
    private long initialItemCount;
    private boolean shrinkWrap;

    public void itemBuilder(Funcs.Func3<BuildContext, Long, Animation<Double>, Widget> v) {
        this.itemBuilder = v;
    }

    public void initialItemCount(long v) {
        this.initialItemCount = v;
    }

    public void scrollDirection(Object v) {
    }

    public void reverse(boolean v) {
    }

    public void controller(Object v) {
    }

    public void primary(Object v) {
    }

    public void physics(Object v) {
    }

    public void shrinkWrap(boolean v) {
        this.shrinkWrap = v;
    }

    public void padding(Object v) {
    }

    public void clipBehavior(Clip v) {
    }

    public static AnimatedListState of(BuildContext context) {
        return new AnimatedListState();
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = new DartList<Widget>();
        Animation<Double> arrived = new AlwaysStoppedAnimation<Double>(1.0);
        if (itemBuilder != null) {
            for (long i = 0; i < initialItemCount; i++) {
                Widget w = itemBuilder.call(context, i, arrived);
                if (w != null) {
                    kids.add(w);
                }
            }
        }
        ListView list = new ListView();
        list.children(kids);
        if (shrinkWrap) {
            list.shrinkWrap(true);
        }
        return list;
    }
}
