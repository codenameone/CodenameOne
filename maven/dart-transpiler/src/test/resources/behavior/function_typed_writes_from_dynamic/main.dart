class H { void Function(int) cb = (int x) {}; int Function()? g; }
void main() {
  var h = H();
  dynamic f = (String s) {};
  try { h.cb = f; print('assigned'); } catch (e) { print('cb ${e is TypeError}'); }
  dynamic g2 = () => 'str';
  try { h.g = g2; print('g assigned'); } catch (e) { print('g ${e is TypeError}'); }
  dynamic ok = (num x) {}; h.cb = ok; h.cb(3); print('ok');
  dynamic dh = h;
  try { dh.cb = (String s) {}; print('dyn assigned'); } catch (e) { print('dyn ${e is TypeError}'); }
  List<int Function(int)> fs = [];
  try { fs.add((dynamic x) => 1); print(fs.length); } catch (e) { print('add ${e is TypeError}'); }
  dynamic bad = (String s) => 1;
  try { fs.add(bad); } catch (e) { print('add2 ${e is TypeError}'); }
}
