// @dart=2.19
import 'dart:collection';
import 'dart:convert';

import 'shelf.dart';

// 1. An abstract class whose methods take function types over T.
abstract class Pipe<T> {
  Pipe<T> mapEach(T Function(T) f);
  Pipe<T> mapTwo(T f);
  List<T> get items;
}

class ListPipe<T> implements Pipe<T> {
  @override
  final List<T> items;
  ListPipe(this.items);
  @override
  Pipe<T> mapEach(T Function(T) f) => ListPipe<T>(items.map(f).toList());
  @override
  Pipe<T> mapTwo(T f) => ListPipe<T>([...items, f]);
}

class Doubler {
  Pipe<int> bind(Pipe<int> p) => p.mapEach((x) => x * 2).mapTwo(7);
}

// 2a. A generic method constructing a generic class with its own type parameter.
class Crate<V> {
  final String name;
  Crate(this.name);
  bool holds(Object? o) => o is V;
}

class Store {
  Crate<V> open<V>(String name, {Crate<V>? Function(String)? creator}) {
    final c = creator?.call(name) ?? Crate<V>(name);
    return c;
  }
}

// 2b. A mixin-style abstract class forwarding whereType.
abstract class ViewMixin<E> implements List<E> {
  List<E> get delegate;
  @override
  Iterable<T> whereType<T>() => delegate.whereType<T>();
}

class MixedList<E> extends ListBase<E> with ViewMixin<E> {
  @override
  final List<E> delegate;
  MixedList(this.delegate);
  @override
  int get length => delegate.length;
  @override
  set length(int n) => delegate.length = n;
  @override
  E operator [](int i) => delegate[i];
  @override
  void operator []=(int i, E v) => delegate[i] = v;
}

// 3. A getter of type Type used in string interpolation after an is-check.
abstract class Base<E> {
  Type get valueType => E;
}
class Plain<E> extends Base<E> {}
class Lazy<E> extends Base<E> {}

String describe(Base<Object?> b) {
  return b is Lazy ? 'Lazy<${b.valueType}>' : 'Plain<${b.valueType}>';
}

// 4. A bare name inside an extension on a program class resolves to the field.
class Journal {
  final List<String> log = [];
}

extension on Journal {
  void record(String s) {
    log.add('rec $s');
  }
}


// 6. A field initialised from a dart:collection constructor keeps its element type, so
// an index read promotes to the value class, not to an unrelated `is` interface.
abstract class Tagged {}

class Bin {
  final String label;
  Type get valueType => int;
  Bin(this.label);
}

class Rack {
  final _bins = HashMap<String, Bin>();
  final _seen = LinkedHashSet<String>();
  void put(String n) {
    _bins[n] = Bin(n);
    _seen.add(n);
  }
  String find(String n) {
    var bin = _bins[n];
    if (bin != null) {
      return bin is Tagged ? 'tagged ${bin.valueType}' : '${bin.label} ${bin.valueType}';
    }
    return 'none ${_seen.length}';
  }
}

// 7. A generic async method whose type argument comes from the assignment target.
abstract class Vessel<E> {}

class Jar<E> implements Vessel<E> {
  bool holds(Object? o) => o is E;
}

abstract class PantryApi {
  Future<Jar<E>> openJar<E>();
}

class Pantry implements PantryApi {
  @override
  Future<Jar<E>> openJar<E>() async => Jar<E>();
}

// ignore: non_constant_identifier_names
PantryApi Store2 = Pantry();

class Holder {
  Vessel? cached;
  Jar<String>? strings;
}

Future<void> main() async {
  print(await exerciseShelf());
  print(Doubler().bind(ListPipe<int>([1, 2])).items);
  final s = Store();
  print(s.open<int>('a').holds(3));
  print(s.open<String>('b').holds(3));
  final m = MixedList<Object>([1, 'x', 2]);
  print(m.whereType<int>().toList());
  print(describe(Lazy<int>()));
  print(describe(Plain<String>()));
  final j = Journal();
  j.record('one');
  print(j.log);
  // 5. A tear-off of jsonDecode held in a local and called with a named reviver.
  final decode = jsonDecode;
  print(decode('{"a":1,"b":[2]}', reviver: (k, v) => v is int ? v + 10 : v));
  final sh = Rack();
  sh.put('k');
  print(sh.find('k'));
  print(sh.find('q'));
  final h = Holder();
  h.cached = await Store2.openJar();
  print((h.cached as Jar).holds(3));
  h.strings = await Store2.openJar();
  print(h.strings!.holds(3));
}
