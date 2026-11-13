import 'dart:collection';

class Bag extends SetBase<String> {
  final List<String> _items = <String>[];

  @override
  bool add(String value) {
    if (_items.contains(value)) return false;
    _items.add(value);
    return true;
  }

  @override
  bool contains(Object? element) => _items.contains(element);

  @override
  String? lookup(Object? element) => contains(element) ? element as String : null;

  @override
  bool remove(Object? value) => _items.remove(value);

  @override
  Iterator<String> get iterator => _items.iterator;

  @override
  int get length => _items.length;

  @override
  Set<String> toSet() => Set<String>.of(_items);
}

class Registry extends MapBase<String, int> {
  final Map<String, int> _store = <String, int>{};
  int writes = 0;

  @override
  int? operator [](Object? key) => _store[key];

  @override
  void operator []=(String key, int value) {
    writes++;
    _store[key] = value;
  }

  @override
  Iterable<String> get keys => _store.keys;

  @override
  int? remove(Object? key) => _store.remove(key);

  @override
  void clear() => _store.clear();
}

void fillSet(Set<String> s) {
  s.add('x');
  s.add('y');
  s.add('x');
}

void fillMap(Map<String, int> m) {
  m['one'] = 1;
  m['two'] = 2;
  m.remove('one');
}

Set<String> echoSet(Set<String> s) => s;
Map<String, int> echoMap(Map<String, int> m) => m;

Bag backToBag(Set<String> s) => s as Bag;

void main() {
  final bag = Bag();
  fillSet(bag);
  print(bag.length);
  print(bag.contains('y'));
  print(bag.join(','));
  print(identical(echoSet(bag), bag));
  final Set<String> asSet = bag;
  print(asSet.length);
  final Bag again = backToBag(bag);
  print(identical(again, bag));
  print(echoSet(bag) is Bag);
  print(bag.toSet().length);

  final reg = Registry();
  fillMap(reg);
  print(reg.length);
  print(reg['two']);
  print(reg.containsKey('one'));
  print(reg.writes);
  print(identical(echoMap(reg), reg));
  final Map<String, int> asMap = reg;
  asMap['three'] = 3;
  print(reg.keys.join(','));
  print(reg.writes);
  final Registry back = echoMap(reg) as Registry;
  print(back.writes);

  final r = [3, 1, 2];
  final copy = List<int>.of(r);
  copy.add(4);
  print(copy);
  print(r.length);
}
