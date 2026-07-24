package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Builds itself from the latest snapshot of a {@code Future} — Flutter's
 * {@code FutureBuilder<T>}. This pass builds once with a waiting
 * {@link AsyncSnapshot} (the initial data, if any); resolving the future and
 * rebuilding on completion lands with the async-rebuild machinery.
 *
 * @param <T> the future's value type
 */
public class FutureBuilder<T> extends StatelessWidget {

    private Object future;
    private T initialData;
    private Funcs.Func2<BuildContext, AsyncSnapshot, Widget> builder;

    public void future(Object v) {
        this.future = v;
    }

    public void initialData(T v) {
        this.initialData = v;
    }

    public void builder(Funcs.Func2<BuildContext, AsyncSnapshot, Widget> v) {
        this.builder = v;
    }

    @Override
    public Widget build(BuildContext context) {
        if (builder == null) {
            return null;
        }
        AsyncSnapshot snapshot;
        if (initialData != null) {
            snapshot = new AsyncSnapshot(ConnectionState.waiting, initialData, null, null);
        } else {
            snapshot = new AsyncSnapshot();
        }
        return builder.call(context, snapshot);
    }
}
