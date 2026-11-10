class Holder<T> {
  final T Function(T) fn;
  final List<T> Function() mk;
  Holder(this.fn, this.mk);
}
class Cell<T> { T Function()? get; Cell(this.get); }
List<T> mkList<T>() => <T>[];
T twice<T>(T Function(T) f, T x) => f(f(x));
void main() {
  var h = Holder<int>((x) => x + 1, () => <int>[]);
  print(h.fn(1));
  print(h.mk().runtimeType);
  Holder<num> hn = h;
  try { hn.fn(2.5); print('no'); } catch (e) { print('TypeError ${e is TypeError}'); }
  var h2 = Holder<String>((s) => s + '!', mkList);
  print(h2.mk().runtimeType);
  dynamic dh = h2;
  print(dh.fn('a'));
  print(dh.mk().runtimeType);
  var c = Cell<int>(() => 7);
  print(c.get!());
  print(twice<int>((x) => x * 2, 3));
  print(twice<String>((String s) => s + s, 'ab'));
  List<String> Function() m = mkList;
  var l = m(); l.add('q'); print(l.runtimeType);
  try { (l as dynamic).add(1); } catch (e) { print('TE ${e is TypeError}'); }
}
