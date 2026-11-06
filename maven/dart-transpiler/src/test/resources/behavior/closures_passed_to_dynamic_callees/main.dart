class Cache<K, V> { final _m = <K, V>{}; V getOrPut(K k, V Function() mk) => _m.putIfAbsent(k, mk);
  R fold<R>(R init, R Function(R, V) f) { var r = init; for (var v in _m.values) r = f(r, v); return r; } }
void apply(void Function(int) f) => f(1);
int twice(int Function(int) f, int x) => f(f(x));
void main() {
  var c = Cache<String, int>();
  print(c.getOrPut('a', () => 1)); print(c.getOrPut('a', () => 2));
  print(c.fold<String>('', (r, v) => '$r$v'));
  dynamic dc = c;
  try { dc.getOrPut('b', () => 'x'); print('noerr'); } catch (e) { print('gop ${e is TypeError}'); }
  try { print(dc.fold(0, (r, v) => r + v)); } catch (e) { print('fold ${e is TypeError}'); }
  dynamic ap = apply;
  try { ap((String s) {}); } catch (e) { print('ap ${e is TypeError}'); }
  ap((num n) => print('num $n'));
  ap((x) => print('dyn $x'));
  dynamic tw = twice;
  print(tw((int x) => x + 1, 3));
  try { print(tw((int x) => 'a', 3)); } catch (e) { print('tw ${e is TypeError}'); }
  try { print(tw((x) => x * 2, 3)); } catch (e) { print('tw2 ${e is TypeError}'); }
  Function g = (int a, [int b = 2]) => a + b;
  print(Function.apply(g, [1])); print(Function.apply(g, [1, 5]));
  try { Function.apply(g, ['a']); } catch (e) { print('fa ${e is TypeError}'); }
  var fs = <int Function(int)>[(x) => x + 1, (x) => x * 3];
  print(fs.fold<int>(2, (a, f) => f(a)));
  print(fs is List<Function>); print(fs is List<num Function(int)>); print(fs is List<int Function(num)>);
  int Function(int) comp = fs.reduce((f, g) => (x) => g(f(x)));
  print(comp(2));
}
