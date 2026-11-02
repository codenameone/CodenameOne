import 'dart:collection';

class Bag extends MapBase<String, int> {
  final Map<String, int> _m = {};
  int? operator [](Object? k) => _m[k];
  void operator []=(String k, int v) { _m[k] = v; }
  void clear() => _m.clear();
  Iterable<String> get keys => _m.keys;
  int? remove(Object? k) => _m.remove(k);
}

class Row extends ListBase<int> {
  final List<int> _l = [];
  int get length => _l.length;
  set length(int n) => _l.length = n;
  int operator [](int i) => _l[i];
  void operator []=(int i, int v) => _l[i] = v;
  void add(int v) => _l.add(v);
}

class Tags extends SetBase<String> {
  final Set<String> _s = {};
  bool add(String v) => _s.add(v);
  bool contains(Object? v) => _s.contains(v);
  String? lookup(Object? v) => _s.lookup(v);
  bool remove(Object? v) => _s.remove(v);
  Iterator<String> get iterator => _s.iterator;
  int get length => _s.length;
  Set<String> toSet() => Set.of(_s);
}

class Evens extends IterableBase<int> {
  Iterator<int> get iterator => [2, 4, 6].iterator;
}

class NotHere extends UnsupportedError { NotHere(String m) : super(m); }
class Todo extends UnimplementedError { Todo([String? m]) : super(m); }
class BadSlot extends IndexError {
  BadSlot(int i, int n) : super.withLength(i, n, name: 'slot', message: 'no such slot');
}

mixin Lazy { late String word = () { print('init word'); return 'w'; }(); }
class UsesLazy with Lazy {}

void show(Object e) => print('${e.runtimeType} ${e is Error} ${e is Exception} | $e');

void main() {
  var b = Bag(); b['x'] = 1; b['y'] = 2;
  print(b); print('map $b');
  var r = Row(); r.add(3); r.add(4);
  print(r); print('list $r');
  var t = Tags(); t.add('a'); t.add('b');
  print(t); print('set $t');
  print(Evens()); print('iter ${Evens()}');

  show(UnsupportedError('u'));
  show(UnimplementedError('x'));
  show(UnimplementedError());
  show(NotHere('n'));
  show(Todo('later'));
  show(Todo());
  Object u = UnimplementedError('y');
  print('${u is UnsupportedError}');
  try { throw NotHere('q'); } on Error catch (e) { print('caught Error $e'); } catch (e) { print('other'); }
  try { throw Todo(); } on UnsupportedError catch (e) { print('caught Unsupported $e'); }
  try { [1].toList(growable: false).add(2); } on UnsupportedError catch (e) { print('fixed ${e is Error}'); }
  try { List<int>.unmodifiable([1]).add(2); } on Error catch (e) { print('unmod ${e is Error} ${e is Exception}'); }

  try { throw BadSlot(5, 3); } on RangeError catch (e) {
    var ie = e as IndexError;
    print('${ie.length} ${ie.invalidValue} ${ie.name} ${ie.indexable}');
    print(e);
  }
  var ie2 = IndexError.withLength(-1, 4);
  print('${ie2.length} $ie2');

  print(FormatException('bad', 'abcdef', 3));
  print(FormatException('bad', 'abc', 0));
  print(FormatException('bad', 'one\ntwo\nthree', 6));
  print(FormatException('bad', 'one\r\ntwo', 6));
  print(FormatException('bad', 'abc'));
  print(FormatException('bad', 42, 1));
  print(FormatException('bad', null, 4));
  print(FormatException());
  print(FormatException('', 'xy', 1));

  var lz = UsesLazy();
  print('made');
  print(lz.word);
  print(lz.word);
}
