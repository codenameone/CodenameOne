class Box<T> { T v; Box(this.v); }
class Cell<T> { List<T> items = []; void add(T t) => items.add(t); T first() => items.first; }
void put<T>(Box<T> b, T v) { b.v = v; }
void main() {
  Box<Object> bo = Box<String>('a');
  try { put<Object>(bo, 1); print('no throw ${bo.v}'); } catch (e) { print('TE ${e is TypeError}'); }
  Box<num?> bn = Box<int?>(null);
  try { bn.v = null; print('null ok'); } catch (e) { print('TE ${e is TypeError}'); }
  try { bn.v = 2.0; print('no throw'); } catch (e) { print('TE ${e is TypeError}'); }
  Box<Object?> bq = Box<int>(1);
  try { bq.v = null; print('no throw'); } catch (e) { print('TE ${e is TypeError}'); }
  Cell<Object> c = Cell<int>();
  try { c.add('x'); print('no throw'); } catch (e) { print('TE ${e is TypeError}'); }
  c.add(5); print(c.first());
  Box<List<num>> bl = Box<List<int>>([1]);
  try { bl.v = <num>[1.5]; print('no throw'); } catch (e) { print('TE ${e is TypeError}'); }
  try { bl.v = <int>[2]; print('ok ${bl.v}'); } catch (e) { print('TE ${e is TypeError}'); }
  Box<int Function(int)> bf = Box<int Function(int)>((x) => x);
  Box<Object> bfo = bf;
  try { bfo.v = (String s) => s; print('no throw'); } catch (e) { print('TE ${e is TypeError}'); }
  try { bfo.v = (int s) => s + 1; print('ok ${bf.v(1)}'); } catch (e) { print('TE ${e is TypeError}'); }
  Box<dynamic> bd = Box<double>(1.0);
  try { bd.v = 3; print('no throw'); } catch (e) { print('TE ${e is TypeError}'); }
}
