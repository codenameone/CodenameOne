// bloc and flutter_bloc's logic side, headless: a Cubit and a Bloc driven the way
// apps drive them, with an observer, typed event handlers (on<E> must only see
// its own E) and Equatable-free states compared by ==.
import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';

class CounterCubit extends Cubit<int> {
  CounterCubit() : super(0);

  void increment() => emit(state + 1);
  void same() => emit(state);
}

abstract class CounterEvent {}

class Incremented extends CounterEvent {}

class Reset extends CounterEvent {
  Reset(this.to);
  final int to;
}

class CounterBloc extends Bloc<CounterEvent, int> {
  CounterBloc() : super(0) {
    on<Incremented>((event, emit) => emit(state + 1));
    on<Reset>(_onReset);
  }

  Future<void> _onReset(Reset event, Emitter<int> emit) async {
    await Future<void>.delayed(const Duration(milliseconds: 5));
    emit(event.to);
  }
}

class Log extends BlocObserver {
  final List<String> lines = [];

  @override
  void onChange(BlocBase bloc, Change change) {
    super.onChange(bloc, change);
    lines.add('${bloc.runtimeType} ${change.currentState}->${change.nextState}');
  }

  @override
  void onTransition(Bloc bloc, Transition transition) {
    super.onTransition(bloc, transition);
    lines.add('transition ${transition.event.runtimeType}');
  }
}

abstract class FeedEvent {}

class Follow extends FeedEvent {}

class Boom extends FeedEvent {}

class FeedBloc extends Bloc<FeedEvent, int> {
  FeedBloc(this.source) : super(0) {
    // A block handler returning nothing. Its forEach is not awaited, so when the
    // handler returns bloc completes the emitter, which disposes the subscription
    // (release behaviour; a debug build also asserts) -- nothing leaks past it.
    on<Follow>((event, emit) {
      emit.forEach<int>(source.stream, onData: (i) => i);
      return;
    });
    on<Boom>((event, emit) => throw StateError('boom'));
  }

  final StreamController<int> source;
}

Future<void> feed() async {
  final source = StreamController<int>();
  final bloc = FeedBloc(source);
  bloc.add(Follow());
  source.add(7);
  await Future<void>.delayed(const Duration(milliseconds: 5));
  print('feed ${bloc.state} listening ${source.hasListener}');
  bloc.add(Boom());
  print('add returned after a throwing handler');
  await bloc.close();
  print('after close listening ${source.hasListener}');
}

Future<void> main() async {
  final log = Log();
  Bloc.observer = log;

  final cubit = CounterCubit();
  final seen = <int>[];
  final sub = cubit.stream.listen(seen.add);
  cubit.increment();
  cubit.increment();
  cubit.same();
  await Future<void>.delayed(Duration.zero);
  print('cubit ${cubit.state} seen $seen');
  await sub.cancel();

  final bloc = CounterBloc();
  bloc.add(Incremented());
  bloc.add(Incremented());
  print('bloc ${bloc.state}');
  bloc.add(Reset(10));
  await Future<void>.delayed(const Duration(milliseconds: 50));
  print('after reset ${bloc.state}');

  await cubit.close();
  print('closed ${cubit.isClosed}');
  try {
    cubit.increment();
  } on StateError catch (e) {
    print('emit after close: ${e.message}');
  }
  for (final l in log.lines) {
    print(l);
  }
  Bloc.observer = BlocObserver();
  await feed();
}
