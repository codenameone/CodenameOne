import 'dart:convert';
class User { final String name; final int age; final List<String> tags; final Map<String,int> scores;
  User(this.name, this.age, this.tags, this.scores);
  factory User.fromJson(Map<String, dynamic> j) => User(j['name'] as String, j['age'] as int,
     (j['tags'] as List).cast<String>(), Map<String,int>.from(j['scores'] as Map));
}
void main() {
  var js = jsonDecode('[{"name":"a","age":3,"tags":["x"],"scores":{"m":1}},{"name":"b","age":"4","tags":[],"scores":{}}]') as List;
  for (var o in js) {
    try { var u = User.fromJson(o); print('${u.name} ${u.age} ${u.tags} ${u.scores}'); }
    catch (e) { print('bad ${e is TypeError}'); }
  }
  dynamic d = jsonDecode('{"l":[1,2,"3"]}');
  try { List<int> li = (d['l'] as List).cast<int>(); print(li.length); print(li[2]); } catch (e) { print('cast ${e is TypeError}'); }
  try { List<int> li2 = List<int>.from(d['l']); print(li2); } catch (e) { print('from ${e is TypeError}'); }
  List<int> l3 = [1,2];
  try { l3.addAll(d['l']); } catch (e) { print('addAll ${e is TypeError} ${l3.length}'); }
  dynamic n = d['l'][0];
  int i = n; print(i);
  try { String s = n; print(s); } catch (e) { print('local ${e is TypeError}'); }
  var lit = <String>[n.toString()]; print(lit);
  try { var lit2 = <String>[n]; print(lit2); } catch (e) { print('lit ${e is TypeError}'); }
  try { var m = <String,int>{'a': d['l'][2]}; print(m); } catch (e) { print('maplit ${e is TypeError}'); }
}
