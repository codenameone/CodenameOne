void main() {
  dynamic n = 'x';
  try { print([1,2].contains(n)); } catch (e) { print('c $e'); }
  try { print('abc'.indexOf(n)); } catch (e) { print('idx ${e is TypeError}'); }
  dynamic i = 2.0; try { print('abc'.substring(i)); } catch (e) { print('sub ${e is TypeError}'); }
  try { print([1,2,3].sublist(i)); } catch (e) { print('sl ${e is TypeError}'); }
  dynamic l = [1, 2]; try { print([0].followedBy(l).toList()); } catch (e) { print('fb $e'); }
  dynamic ls = <dynamic>[1, 'a']; try { <int>[].addAll(ls); print('bad'); } catch (e) { print('aa ${e is TypeError}'); }
  dynamic li = <dynamic>[1, 2]; try { final x = <int>[]..addAll(li); print(x); } catch (e) { print("li ${e is TypeError}"); }
  dynamic sep = 1; try { print(['a'].join(sep)); } catch (e) { print('join ${e is TypeError}'); }
  dynamic mm = {'a': 1}; final mp = <String,int>{}; mp.addAll(mm); print(mp);
  dynamic bad = {'a': 'b'}; try { <String,int>{}.addAll(bad); print('bad'); } catch (e) { print('ma ${e is TypeError}'); }
  dynamic ds = 'x'; try { DateTime.now().add(ds); } catch (e) { print('dt ${e is TypeError}'); }
  dynamic pat = 5; try { 'a,b'.split(pat); } catch (e) { print('sp ${e is TypeError}'); }
  dynamic js = {'k': [1, 2]};
  final List<int> got = (js['k'] as List).cast<int>(); print(got.reduce((a, b) => a + b));
  final Map<String, List<int>> m2 = Map<String, List<int>>.from({'q': <int>[1]}); print(m2);
  dynamic mx = <String, dynamic>{'a': <dynamic>[1]};
  try { final Map<String, List<int>> m3 = Map<String, List<int>>.from(mx); print(m3['a']); } catch (e) { print('mf ${e is TypeError}'); }
  try { final mm4 = mx as Map<String, List<int>>; print(mm4); } catch (e) { print('mc ${e is TypeError}'); }
  final ok = (<String, Object>{'a': 1} as Map<String, Object>); print(ok);
  Map<String, Object> mo = <String, int>{'a': 1}; try { mo['b'] = 'x'; } catch (e) { print('cov ${e is TypeError}'); }
  final dts = [DateTime(2022), DateTime(2020)]..sort(); print(dts.first.year);
  print(DateTime(2020).compareTo(DateTime(2021)));
  Comparable cmp = DateTime(2020); print(cmp.compareTo(DateTime(2019)));
  final sorted = [Duration(seconds: 3), Duration(seconds: 1)]..sort(); print(sorted);
}
