// @dart=2.19
import 'dart:async';
import 'dart:collection';
import 'dart:convert';

// A generic class whose body uses T as a value, constructed with no type argument.
class Tally<T> {
  final List<Object?> seen = [];
  void add(Object? o) => seen.add(o);
  String report() => 'is T: ${seen.whereType<T>().length}';
}

class Owner {
  final _tally = Tally();
  String run() {
    _tally.add(1);
    _tally.add('a');
    return _tally.report();
  }
}

// An extension method torn off as a callback.
class Gate {
  bool open = false;
  final List<String> notes = [];
}

extension on Gate {
  void note(Object error, Object st) {
    if (open) return;
    notes.add('noted $error');
  }
}

class Watcher {
  final Gate gate;
  Watcher(this.gate);
  void watch(Object e) {
    final void Function(Object, Object) cb = gate.note;
    try {
      throw e;
    } catch (err, st) {
      cb(err, st);
    }
  }
}

// A plain abstract class with no constructor, mixed in after another mixin.
abstract class Delegating<E> {
  List<E> get inner;
  int get size => inner.length;
  E at(int i) => inner[i];
}

mixin Labelled {
  String get label => 'labelled';
}

class Wrapped<E> with Labelled, Delegating<E> {
  @override
  final List<E> inner;
  Wrapped(this.inner);
}

// `is` against an interface the static type does not implement does not promote.
abstract class Shape {
  String get kind;
}

abstract class Round {
  double get radius;
}

class ShapeImpl implements Shape {
  @override
  String get kind => 'shape';
  String get extra => 'extra';
}

class Disc extends ShapeImpl implements Round {
  @override
  double get radius => 2.0;
}

String describe(ShapeImpl s) => s is Round ? 'round ${s.extra}' : 'flat ${s.extra}';

void main() async {
  print(Owner().run());

  final gate = Gate();
  Watcher(gate).watch('boom');
  print(gate.notes);

  final w = Wrapped<int>([4, 5, 6]);
  print('${w.label} ${w.size} ${w.at(1)}');

  print(describe(Disc()));
  print(describe(ShapeImpl()));

}
