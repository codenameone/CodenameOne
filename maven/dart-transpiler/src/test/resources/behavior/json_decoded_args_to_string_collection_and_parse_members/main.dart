import 'dart:convert';
void main() {
  dynamic d = jsonDecode('{"s":"a,b;c","n":3,"l":["x","y"],"p":","}');
  String s = 'hello world';
  try { print(s.split(d['p'])); } catch (e) { print('split ${e is TypeError}'); }
  try { print(s.split(d['n'])); } catch (e) { print('splitn ${e is TypeError}'); }
  try { print(s.contains(d['n'])); } catch (e) { print('contains ${e is TypeError}'); }
  try { print(s.startsWith(d['p'])); } catch (e) { print('sw ${e is TypeError}'); }
  try { print(s.replaceAll(d['p'], d['n'])); } catch (e) { print('ra ${e is TypeError}'); }
  try { print(s.indexOf(d['l'])); } catch (e) { print('io ${e is TypeError}'); }
  try { print(s.substring(d['n'])); } catch (e) { print('sub ${e is TypeError}'); }
  try { print(s.padLeft(d['n'] + 10, d['p'])); } catch (e) { print('pl ${e is TypeError}'); }
  try { print(s.padLeft(15, d['n'])); } catch (e) { print('pl2 ${e is TypeError}'); }
  try { print(s.codeUnitAt(d['p'])); } catch (e) { print('cua ${e is TypeError}'); }
  try { print(s.split(RegExp(d['p']))); } catch (e) { print('re ${e is TypeError}'); }
  dynamic re = RegExp(r'o');
  print(s.replaceAll(re, '0'));
  print(s.split(re));
  List<String> ls = ['a'];
  try { ls.addAll(d['l']); print(ls); } catch (e) { print('aa ${e is TypeError}'); }
  try { ls.insert(d['n'] - 3, d['n']); } catch (e) { print('ins ${e is TypeError}'); }
  try { print(ls.join(d['n'])); } catch (e) { print('join ${e is TypeError}'); }
  try { print(ls.indexOf(d['n'])); } catch (e) { print('lio ${e is TypeError}'); }
  try { print(ls.contains(d['n'])); } catch (e) { print('lc ${e is TypeError}'); }
  try { print(ls.remove(d['n'])); } catch (e) { print('lr ${e is TypeError}'); }
  Set<int> st = {1};
  try { st.addAll(d['l']); print(st); } catch (e) { print('sa ${e is TypeError}'); }
  try { print(st.containsAll(d['l'])); } catch (e) { print('sca ${e is TypeError}'); }
  try { print(st.union(d['l'].toSet())); } catch (e) { print('su ${e is TypeError}'); }
  Map<String, int> m = {};
  try { m.addAll(d); } catch (e) { print('ma ${e is TypeError}'); }
  try { print(m.containsKey(d['n'])); } catch (e) { print('mck ${e is TypeError}'); }
  try { m.putIfAbsent(d['n'], () => 1); } catch (e) { print('mpa ${e is TypeError}'); }
  try { print(int.parse(d['n'])); } catch (e) { print('ip ${e is TypeError}'); }
  try { print(int.tryParse(d['s'])); } catch (e) { print('itp ${e is TypeError}'); }
  try { print(double.parse(d['s'])); } catch (e) { print('dp ${e is TypeError} ${e is FormatException}'); }
  try { print(DateTime.parse(d['n'])); } catch (e) { print('dtp ${e is TypeError}'); }
  try { print(Uri.parse(d['n'])); } catch (e) { print('up ${e is TypeError}'); }
  try { print((3).clamp(d['n'], 10)); } catch (e) { print('cl ${e is TypeError}'); }
  try { print(1.5.toStringAsFixed(d['n'])); } catch (e) { print('tsf ${e is TypeError}'); }
  try { print(1.5.toStringAsFixed(d['s'])); } catch (e) { print('tsf2 ${e is TypeError}'); }
  try { print((5).compareTo(d['s'])); } catch (e) { print('ct ${e is TypeError}'); }
  try { print('a'.compareTo(d['n'])); } catch (e) { print('sct ${e is TypeError}'); }
  try { print(Duration(seconds: d['n'])); } catch (e) { print('dur ${e is TypeError}'); }
  try { print(Duration(seconds: d['s'])); } catch (e) { print('dur2 ${e is TypeError}'); }
}
