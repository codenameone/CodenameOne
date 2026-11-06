import 'dart:async';
import 'dart:collection';

import 'ring_store.dart';

class Ring<E> extends ListBase<E> {
  final List<E> _items = <E>[];
  @override
  int get length => _items.length;
  @override
  set length(int n) => _items.length = n;
  @override
  E operator [](int i) => _items[i];
  @override
  void operator []=(int i, E v) => _items[i] = v;
  @override
  void add(E v) => _items.add(v);

  List<E> backwards() => reversed.toList();
}

class Ticket {
  final String name;
  Ticket(this.name);
}

class Hub<E> {
  final _events = StreamController<Ticket>.broadcast(sync: true);
  final _byName = Map.of(<String, int>{'a': 1});
  Map<Object, E> table = Map.of(<Object, E>{});
  Stream<Ticket> get events => _events.stream;
  void fire(String n) => _events.add(Ticket(n));
  int lookup(String k) => _byName[k] ?? -1;
}

// A private field of the same name as the implemented class's private field, in
// another library: Dart keeps the two apart.
class IntStore implements Store<int> {
  final List<int> _slots = <int>[];
  @override
  int get size => _slots.length;
  @override
  void put(int v) => _slots.add(v * 10);
  @override
  int? at(int i) => _slots[i];
  @override
  bool sameSlots(Store<int> other) => false;
}

void main() {
  final Store<int> s = IntStore();
  s.put(4);
  print('${s.size} ${s.at(0)}');
  final plain = Store<String>()..put('p');
  print('${plain.size} ${plain.at(0)} ${plain.sameSlots(plain)}');
  final r = Ring<int>();
  r.add(1);
  r.add(2);
  r.add(3);
  print(r.reversed.toList());
  print(r.backwards());
  print(r.reversed.first);
  final h = Hub<String>();
  h.events.listen((t) => print('got ${t.name}'));
  h.fire('x');
  print(h.lookup('a'));
  print(h.lookup('b'));
  h.table[1] = 'one';
  print(h.table);
}
