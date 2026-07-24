package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Animation;

import dart.core.Duration;
import dart.runtime.Funcs;

/**
 * The mutable state of an {@link AnimatedList} — Flutter's
 * {@code AnimatedListState}, reached through a {@code GlobalKey}. Exposes the
 * imperative {@code insertItem}/{@code removeItem} operations. In this milestone
 * the list rebuilds from its item count on each frame rather than running
 * per-item insert/remove transitions, so these record the intent without a
 * flight animation.
 */
public class AnimatedListState {

    public void insertItem(long index, Duration duration) {
        // structural insert is reflected on the next rebuild; transition deferred
    }

    public void removeItem(long index,
                           Funcs.Func2<BuildContext, Animation<Double>, Widget> builder,
                           Duration duration) {
        // structural remove is reflected on the next rebuild; transition deferred
    }
}
