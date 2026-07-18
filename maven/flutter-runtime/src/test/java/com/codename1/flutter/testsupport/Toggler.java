package com.codename1.flutter.testsupport;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.Widget;

/**
 * A stateful widget whose state simply exposes the child it builds, so tests
 * can flip the subtree via setState.
 */
public class Toggler extends StatefulWidget {

    private final Widget initialChild;

    public Toggler(Widget initialChild) {
        this.initialChild = initialChild;
    }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new TogglerState();
    }

    public class TogglerState extends State<Toggler> {
        public Widget child = initialChild;
        public int initStateCalls;
        public int disposeCalls;

        @Override
        public void initState() {
            initStateCalls++;
        }

        @Override
        public void dispose() {
            disposeCalls++;
        }

        @Override
        public Widget build(BuildContext context) {
            return child;
        }
    }
}
