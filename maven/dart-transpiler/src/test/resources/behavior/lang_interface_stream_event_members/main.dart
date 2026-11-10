import 'dart:async';

class Event {
  final dynamic key;
  final dynamic value;
  final bool deleted;
  Event(this.key, this.value, this.deleted);
}

abstract class StoreBase<E> {
  Stream<Event> watch({dynamic key});
  E? get(dynamic key);
}

abstract class Store<E> implements StoreBase<E> {
  Map<dynamic, E> toMap();
}

class MemStore<E> implements Store<E> {
  final StreamController<Event> _ctl = StreamController<Event>.broadcast(sync: true);
  final Map<dynamic, E> _data = {};

  @override
  Stream<Event> watch({dynamic key}) =>
      key == null ? _ctl.stream : _ctl.stream.where((e) => e.key == key);

  @override
  E? get(dynamic key) => _data[key];

  void put(dynamic key, E value) {
    _data[key] = value;
    _ctl.add(Event(key, value, false));
  }

  @override
  Map<dynamic, E> toMap() => Map<dynamic, E>.of(_data);
}

class Watcher {
  final Store store;
  final Set<dynamic>? keys;
  Watcher(this.store, this.keys);
}

void main() {
  final s = MemStore<String>();
  final w = Watcher(s, {'a'});
  w.store.watch().listen((event) {
    if (w.keys != null && !w.keys!.contains(event.key)) {
      print('skip ${event.key}');
      return;
    }
    print('seen ${event.key}=${event.value} ${event.deleted}');
  });
  s.put('a', 'one');
  s.put('b', 'two');
  final m = s.toMap().map((k, v) => MapEntry(Uri.decodeComponent(k.toString()), v));
  final String first = m.keys.first;
  print(first);
}
