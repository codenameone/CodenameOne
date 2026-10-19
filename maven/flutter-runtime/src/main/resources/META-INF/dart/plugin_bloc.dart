// package:bloc and package:flutter_bloc, over com.codename1.flutter.plugins.bloc.
//
// Not transpiled from pub: flutter_bloc reads blocs by type (`context.read<B>()`,
// `BlocBuilder<B, S>`), and the type arguments that carries are erased in Java.
// Here they arrive as class tokens instead -- `providedType` on the widgets and
// the `witness` parameter of `on<E>` -- and a bloc is a ChangeNotifier, so the
// provider lookup and rebuild machinery the runtime already has does the rest.

@JavaName('com.codename1.flutter.plugins.bloc.Change')
class Change<S> {
  external Change({S currentState, S nextState});
  external S get currentState;
  external S get nextState;
}

@JavaName('com.codename1.flutter.plugins.bloc.Transition')
class Transition<E, S> extends Change<S> {
  external Transition({S currentState, E event, S nextState});
  external E get event;
}

@JavaName('com.codename1.flutter.plugins.bloc.BlocBase')
abstract class BlocBase<S> extends ChangeNotifier {
  external BlocBase(S initialState);
  external S get state;
  external Stream<S> get stream;
  external bool get isClosed;
  external void emit(S state);
  external void onChange(Change<S> change);
  external void addError(Object error, [Object? stackTrace]);
  external void onError(Object error, Object? stackTrace);
  external Future<void> close();
}

@JavaName('com.codename1.flutter.plugins.bloc.Cubit')
abstract class Cubit<S> extends BlocBase<S> {
  external Cubit(S initialState);
}

@JavaName('com.codename1.flutter.plugins.bloc.Emitter')
class Emitter<S> {
  external void call(S state);
  external bool get isDone;
  external Future<void> forEach<T>(Stream<T> stream, {required S Function(T data) onData, Object? onError});
  external Future<void> onEach<T>(Stream<T> stream, {required void Function(T data) onData, Object? onError});
}

@JavaName('com.codename1.flutter.plugins.bloc.Bloc')
abstract class Bloc<E, S> extends BlocBase<S> {
  external Bloc(S initialState);
  external static BlocObserver get observer;
  external static set observer(BlocObserver value);
  external void add(E event);
  external void onEvent(E event);
  external void onTransition(Transition<E, S> transition);
  // `witness` is filled in by the transpiler with the class of the call's <T>.
  external void on<T>(dynamic handler, [Type? witness]);
}

@JavaName('com.codename1.flutter.plugins.bloc.BlocObserver')
class BlocObserver {
  external BlocObserver();
  external void onCreate(BlocBase bloc);
  external void onEvent(Bloc bloc, Object? event);
  external void onChange(BlocBase bloc, Change change);
  external void onTransition(Bloc bloc, Transition transition);
  external void onError(BlocBase bloc, Object error, Object? stackTrace);
  external void onClose(BlocBase bloc);
}

@JavaName('com.codename1.flutter.plugins.bloc.BlocProvider')
class BlocProvider<T> extends ChangeNotifierProvider {
  external BlocProvider({Key? key, dynamic create, bool? lazy, Widget? child});
  external static BlocProvider value({Key? key, Object value, Widget? child});
  external static T of(BuildContext context, {bool listen});
}

@JavaName('com.codename1.flutter.plugins.bloc.RepositoryProvider')
class RepositoryProvider<T> extends Provider {
  external RepositoryProvider({Key? key, dynamic create, bool? lazy, dynamic dispose, Widget? child});
  external static RepositoryProvider value({Key? key, Object value, Widget? child});
  external static T of(BuildContext context, {bool listen});
}

@JavaName('com.codename1.flutter.plugins.bloc.MultiBlocProvider')
class MultiBlocProvider extends Widget {
  external MultiBlocProvider({Key? key, List<SingleChildWidget> providers, Widget child});
}

@JavaName('com.codename1.flutter.plugins.bloc.MultiBlocProvider')
class MultiRepositoryProvider extends Widget {
  external MultiRepositoryProvider({Key? key, List<SingleChildWidget> providers, Widget child});
}

@JavaName('com.codename1.flutter.plugins.bloc.BlocBuilder')
class BlocBuilder<B, S> extends StatefulWidget {
  external BlocBuilder({Key? key, B? bloc, Widget Function(BuildContext context, S state) builder,
      bool Function(S previous, S current)? buildWhen});
}

@JavaName('com.codename1.flutter.plugins.bloc.BlocSelector')
class BlocSelector<B, S, T> extends StatefulWidget {
  external BlocSelector({Key? key, B? bloc, T Function(S state) selector,
      Widget Function(BuildContext context, T state) builder});
}

@JavaName('com.codename1.flutter.plugins.bloc.BlocListener')
class BlocListener<B, S> extends StatefulWidget {
  external BlocListener({Key? key, B? bloc, void Function(BuildContext context, S state) listener,
      bool Function(S previous, S current)? listenWhen, Widget? child});
}

@JavaName('com.codename1.flutter.plugins.bloc.MultiBlocListener')
class MultiBlocListener extends StatelessWidget {
  external MultiBlocListener({Key? key, List<Widget> listeners, Widget child});
}

@JavaName('com.codename1.flutter.plugins.bloc.BlocConsumer')
class BlocConsumer<B, S> extends StatefulWidget {
  external BlocConsumer({Key? key, B? bloc, Widget Function(BuildContext context, S state) builder,
      void Function(BuildContext context, S state) listener,
      bool Function(S previous, S current)? buildWhen, bool Function(S previous, S current)? listenWhen});
}
