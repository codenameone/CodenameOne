/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package androidx.lifecycle;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

/// Observers move through every state in order, catch up when added late,
/// and hear events in AndroidX's order: first added first going up, last
/// added first going down.
public class LifecycleRegistryTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final class Owner implements LifecycleOwner {
        final LifecycleRegistry registry = new LifecycleRegistry(this);

        @Override
        public Lifecycle getLifecycle() {
            return registry;
        }
    }

    private static LifecycleEventObserver recorder(final String name, final List<String> log) {
        return new LifecycleEventObserver() {
            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                log.add(name + ":" + event);
            }
        };
    }

    @Test
    public void lateObserverCatchesUpThroughEveryState() {
        Owner o = new Owner();
        o.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        o.registry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        o.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        List<String> log = new ArrayList<String>();
        o.registry.addObserver(recorder("a", log));
        assertEquals("[a:ON_CREATE, a:ON_START, a:ON_RESUME]", log.toString());
    }

    @Test
    public void upInOrderOfAdditionDownInReverse() {
        Owner o = new Owner();
        List<String> log = new ArrayList<String>();
        o.registry.addObserver(recorder("a", log));
        o.registry.addObserver(recorder("b", log));
        o.registry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        assertEquals("[a:ON_CREATE, a:ON_START, b:ON_CREATE, b:ON_START]", log.toString());
        log.clear();
        o.registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        assertEquals("[b:ON_STOP, b:ON_DESTROY, a:ON_STOP, a:ON_DESTROY]", log.toString());
    }

    @Test
    public void defaultObserverHearsOnlyWhatItOverrides() {
        Owner o = new Owner();
        final List<String> log = new ArrayList<String>();
        o.registry.addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(LifecycleOwner owner) {
                log.add("resume");
            }
        });
        o.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        o.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        assertEquals("[resume]", log.toString());
        assertEquals(Lifecycle.State.STARTED, o.registry.getCurrentState());
    }

    @Test
    public void observerRemovedWhileDispatchingStops() {
        final Owner o = new Owner();
        final List<String> log = new ArrayList<String>();
        o.registry.addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                log.add(event.toString());
                if (event == Lifecycle.Event.ON_CREATE) {
                    o.registry.removeObserver(this);
                }
            }
        });
        o.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        assertEquals("[ON_CREATE]", log.toString());
    }

    @Test
    public void anObserverRemovingItselfDoesNotSkipTheNext() {
        final Owner o = new Owner();
        final List<String> log = new ArrayList<String>();
        o.registry.addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                log.add("a:" + event);
                o.registry.removeObserver(this);
            }
        });
        o.registry.addObserver(recorder("b", log));
        o.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        assertEquals("[a:ON_CREATE, b:ON_CREATE]", log.toString());
    }

    @Test
    public void observerAddedDuringCreateWaitsForOlderObserverToAdvance() {
        final Owner o = new Owner();
        final List<String> log = new ArrayList<String>();
        o.registry.addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                log.add("a:" + event);
                if (event == Lifecycle.Event.ON_CREATE) {
                    o.registry.addObserver(recorder("b", log));
                }
            }
        });
        o.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        assertEquals("[a:ON_CREATE, b:ON_CREATE, a:ON_START, a:ON_RESUME, b:ON_START, b:ON_RESUME]",
                log.toString());
    }

    @Test
    public void nestedObserverAddedDuringLateCatchUpEventuallyReachesCurrentState() {
        final Owner o = new Owner();
        o.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        final List<String> log = new ArrayList<String>();
        o.registry.addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                log.add("a:" + event);
                if (event == Lifecycle.Event.ON_CREATE) {
                    o.registry.addObserver(recorder("b", log));
                }
            }
        });
        assertEquals("[a:ON_CREATE, b:ON_CREATE, a:ON_START, a:ON_RESUME, b:ON_START, b:ON_RESUME]",
                log.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void cannotMoveBackToInitializedAfterCreation() {
        Owner o = new Owner();
        o.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        o.registry.setCurrentState(Lifecycle.State.INITIALIZED);
    }
}
