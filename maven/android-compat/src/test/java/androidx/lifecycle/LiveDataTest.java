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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/// LiveData delivers only while its observer's lifecycle is started, hands a
/// returning observer the latest value once, and forgets an observer whose
/// owner is destroyed -- AndroidX's rules.
public class LiveDataTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final class Owner implements LifecycleOwner {
        final LifecycleRegistry registry = new LifecycleRegistry(this);

        @Override
        public Lifecycle getLifecycle() {
            return registry;
        }
    }

    private static Observer<String> recorder(final List<String> out) {
        return new Observer<String>() {
            @Override
            public void onChanged(String value) {
                out.add(value);
            }
        };
    }

    @Test
    public void deliversOnlyWhileStartedAndOnlyTheLatestValue() {
        Owner owner = new Owner();
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        MutableLiveData<String> data = new MutableLiveData<String>();
        List<String> seen = new ArrayList<String>();
        data.observe(owner, recorder(seen));
        data.setValue("a");
        assertTrue("created is not active", seen.isEmpty());
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        assertEquals("[a]", seen.toString());
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        assertEquals("not delivered twice", "[a]", seen.toString());
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        data.setValue("b");
        data.setValue("c");
        assertEquals("[a]", seen.toString());
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        assertEquals("[a, c]", seen.toString());
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        assertFalse("destroyed owner's observer removed", data.hasObservers());
    }

    @Test
    public void observeForeverSeesEverythingAndActivityIsCounted() {
        final List<String> events = new ArrayList<String>();
        MutableLiveData<String> data = new MutableLiveData<String>("start") {
            @Override
            protected void onActive() {
                events.add("active");
            }

            @Override
            protected void onInactive() {
                events.add("inactive");
            }
        };
        List<String> seen = new ArrayList<String>();
        Observer<String> o = recorder(seen);
        data.observeForever(o);
        data.setValue("x");
        data.postValue("y");
        data.postValue("z");
        MainThreadRule.drain();
        // Each post is delivered, in order: no value is shared between the
        // posting thread and the UI thread.
        assertEquals("[start, x, y, z]", seen.toString());
        data.removeObserver(o);
        assertEquals("[active, inactive]", events.toString());
    }

    @Test
    public void mapFollowsItsSourceWhileObserved() {
        MutableLiveData<Integer> source = new MutableLiveData<Integer>(2);
        LiveData<String> mapped = Transformations.map(source, new androidx.arch.core.util.Function<Integer, String>() {
            @Override
            public String apply(Integer input) {
                return "n" + input;
            }
        });
        List<String> seen = new ArrayList<String>();
        mapped.observeForever(recorder(seen));
        source.setValue(3);
        assertEquals("[n2, n3]", seen.toString());
    }
}
