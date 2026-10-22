/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.bloc;

import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;

import dart.runtime.Funcs;

/// What BlocBuilder, BlocListener, BlocConsumer and BlocSelector share: find the
/// bloc (the widget's own, or the nearest provided one of the widget's `B`),
/// listen to it while mounted, and let the subclass decide what a new state means.
abstract class BlocWidgetState<W extends StatefulWidget> extends State<W> {

    private BlocBase<Object> bloc;
    private Object previous;

    private final Funcs.VoidFunc0 listener = new Funcs.VoidFunc0() {
        @Override
        public void call() {
            if (bloc == null || !mounted()) {
                return;
            }
            Object next = bloc.state();
            Object before = previous;
            previous = next;
            onState(before, next);
        }
    };

    /// The widget's explicit `bloc:`, or null to look it up.
    protected abstract BlocBase<?> explicitBloc();

    private boolean fromExplicit;

    /// The widget's `B`, threaded in by the transpiler; null when it was not known.
    protected abstract Class<?> blocType();

    /// A state after `before`. Called while mounted, outside build.
    protected abstract void onState(Object before, Object next);

    /// Called from build only. A provided bloc is looked up on every build, with a
    /// dependency on the provider: when an ancestor BlocProvider swaps its bloc while
    /// this widget stays in place, the provider rebuilds us and the subscription moves
    /// to the new bloc, as flutter_bloc's context.select on the bloc does. Before, the
    /// lookup was made once without a dependency and kept the old, possibly closed, bloc.
    @SuppressWarnings("unchecked")
    protected final BlocBase<Object> bloc() {
        BlocBase<?> b = explicitBloc();
        if (b == null) {
            Class<?> type = blocType();
            Object found = context().providerValueOfType(type == null ? BlocBase.class : type, true);
            b = found instanceof BlocBase ? (BlocBase<?>) found : null;
        }
        if (b != bloc) {
            release();
            if (b != null) {
                fromExplicit = b == explicitBloc();
                bloc = (BlocBase<Object>) b;
                previous = bloc.state();
                bloc.addListener(listener);
            }
            onBlocChanged();
        }
        return bloc;
    }

    /// The bloc this widget follows was replaced (a new provider or `bloc:`). A
    /// subclass drops whatever it cached from the old bloc's states here, so the next
    /// build shows the new bloc's current state rather than the old one's last.
    protected void onBlocChanged() {
    }

    /// The state a builder should be shown, before any change arrives.
    protected final Object currentState() {
        BlocBase<Object> b = bloc();
        return b == null ? null : b.state();
    }

    protected final void rebuild() {
        setState(new Funcs.VoidFunc0() {
            @Override
            public void call() {
            }
        });
    }

    @Override
    public void didUpdateWidget(W oldWidget) {
        super.didUpdateWidget(oldWidget);
        BlocBase<?> explicit = explicitBloc();
        // A bloc the widget passed and then dropped (bloc: x -> bloc: null) is released
        // too, so the next build looks the provider up instead of keeping the old one.
        if (explicit != null ? explicit != bloc : fromExplicit) {
            release();
        }
    }

    @Override
    public void dispose() {
        release();
        super.dispose();
    }

    private void release() {
        if (bloc != null) {
            bloc.removeListener(listener);
            bloc = null;
        }
    }

    @SuppressWarnings("unchecked")
    static <S> boolean test(Funcs.Func2<S, S, Boolean> when, Object before, Object next) {
        return when == null || Boolean.TRUE.equals(when.call((S) before, (S) next));
    }
}
