package com.codename1.flutter.animation;

import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.foundation.Listenable;

/**
 * A widget that rebuilds when a {@link Listenable} it is given notifies its
 * listeners — Flutter's {@code AnimatedWidget}. Subclasses implement
 * {@code build(BuildContext)} and read {@link #listenable()} (usually an
 * {@code Animation}) to derive the current frame. Modelled on top of
 * {@link StatelessWidget}: its build runs as a pure function of the current
 * listenable value.
 */
public abstract class AnimatedWidget extends StatelessWidget {

    private Listenable listenable;

    /** Named parameter setter for the Dart {@code listenable:} parameter. */
    public void listenable(Listenable v) {
        this.listenable = v;
    }

    /** Getter for the driving {@link Listenable}. */
    public Listenable listenable() {
        return listenable;
    }
}
