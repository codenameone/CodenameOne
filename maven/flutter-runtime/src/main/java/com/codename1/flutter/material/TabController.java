package com.codename1.flutter.material;

import com.codename1.flutter.animation.Animation;
import com.codename1.flutter.animation.AnimationController;
import com.codename1.flutter.animation.TickerProvider;
import com.codename1.flutter.foundation.ChangeNotifier;

import dart.core.Duration;

/**
 * Coordinates tab selection between a {@link TabBar} and a {@link TabBarView} —
 * Flutter's {@code TabController}. Holds the selected {@code index} over a fixed
 * {@code length}, notifies listeners on change (it is a {@link ChangeNotifier}),
 * and exposes an {@link Animation} whose value tracks the selected index
 * (0..length-1) so index-driven animations resolve. Tab-change gestures and the
 * cross-fade flight are not yet wired; {@link #animateTo} sets the index
 * immediately.
 */
public class TabController implements ChangeNotifier {

    private long length;
    private long index;
    private long previousIndex;
    private final AnimationController controller = new AnimationController();

    public TabController() {
        controller.lowerBound(0.0);
        controller.upperBound(Double.MAX_VALUE);
    }

    // Named-parameter setters.

    public void length(long v) {
        this.length = v;
        controller.upperBound(v <= 1 ? 1.0 : (double) (v - 1));
    }

    public void initialIndex(long v) {
        this.index = v;
        this.previousIndex = v;
        controller.value((double) v);
    }

    public void animationDuration(Duration v) {
        if (v != null) {
            controller.duration(v);
        }
    }

    public void vsync(TickerProvider v) {
        // self-driven; provider unused
    }

    // Dart getters / setters.

    public long index() {
        return index;
    }

    public void index(long v) {
        if (v == index) {
            return;
        }
        previousIndex = index;
        index = v;
        controller.value((double) v);
        notifyListeners();
    }

    public long length() {
        return length;
    }

    public long previousIndex() {
        return previousIndex;
    }

    public boolean indexIsChanging() {
        return false;
    }

    public double offset() {
        return 0.0;
    }

    public Animation<Double> animation() {
        return controller;
    }

    public void animateTo(long value, Duration duration, com.codename1.flutter.animation.Curve curve) {
        index(value);
    }
}
