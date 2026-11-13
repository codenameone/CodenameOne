import 'dart:convert';
void main() {
  dynamic d = jsonDecode('{"s":"x","n":3,"p":","}');
  String s = 'a,b';
  try { print(s.split(d['p'])); } catch (e) { print('split ${e is TypeError}'); }
  try { print(s.split(d['n'])); } catch (e) { print('splitn ${e is TypeError}'); }
  try { print(s.contains(d['n'])); } catch (e) { print('contains ${e is TypeError}'); }
  try { print(s.startsWith(d['n'])); } catch (e) { print('sw ${e is TypeError}'); }
  try { print(s.indexOf(d['n'])); } catch (e) { print('io ${e is TypeError}'); }
  try { print(s.substring(d['s'])); } catch (e) { print('sub ${e is TypeError}'); }
  try { print(s.padLeft(5, d['n'])); } catch (e) { print('pl2 ${e is TypeError}'); }
  try { print(s.endsWith(d['n'])); } catch (e) { print('ew ${e is TypeError}'); }
  List<String> ls = ['a'];
  try { ls.add(d['n']); } catch (e) { print('add ${e is TypeError} ${ls.length}'); }
  try { print(ls.join(d['n'])); } catch (e) { print('join ${e is TypeError}'); }
  try { print(ls.indexOf(d['n'])); } catch (e) { print('lio ${e is TypeError}'); }
  try { print(ls.contains(d['n'])); } catch (e) { print('lc ${e is TypeError}'); }
  try { print(ls.remove(d['n'])); } catch (e) { print('lr ${e is TypeError}'); }
  try { print(ls.take(d['s'])); } catch (e) { print('tk ${e is TypeError}'); }
  List<num> ln = [1];
  try { ln.add(d['n']); print(ln); ln.add(d['s']); } catch (e) { print('ln ${e is TypeError} $ln'); }
  try { print(ln.indexOf(d['s'])); } catch (e) { print('lni ${e is TypeError}'); }
  Set<int> st = {1};
  try { st.addAll(d['p'].split('')); } catch (e) { print('sa ${e is TypeError}'); }
  try { print(st.add(d['n'])); print(st.add(d['s'])); } catch (e) { print('sadd ${e is TypeError}'); }
  Iterable<String> it = ls;
  try { print(it.elementAt(d['s'])); } catch (e) { print('ea ${e is TypeError}'); }
  try { print('a'.compareTo(d['n'])); } catch (e) { print('sct ${e is TypeError}'); }
  try { print((5).compareTo(d['s'])); } catch (e) { print('ct ${e is TypeError}'); }
  StringBuffer sb = StringBuffer();
  try { sb.writeCharCode(d['s']); } catch (e) { print('wcc ${e is TypeError}'); }
  try { sb.write(d['n']); print(sb); } catch (e) { print('w ${e is TypeError}'); }
  dynamic re = RegExp(r','); print(s.split(re)); print(s.replaceAll(re, ';')); print(s.contains(re));
  try { print(s.replaceFirst(d['p'], d['s'])); } catch (e) { print('rf ${e is TypeError}'); }
}
