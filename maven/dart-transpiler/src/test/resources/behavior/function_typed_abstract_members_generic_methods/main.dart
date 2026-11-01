// @dart=2.19
import 'dart:collection';
import 'dart:convert';

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

void main() {
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
}
