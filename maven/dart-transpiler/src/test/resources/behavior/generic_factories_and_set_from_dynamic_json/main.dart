import 'dart:convert';
class Item { final String id; final double price; final List<Tag> tags; Item(this.id, this.price, this.tags);
  factory Item.fromJson(Map<String, dynamic> j) => Item(j['id'] as String, (j['price'] as num).toDouble(),
    (j['tags'] as List<dynamic>? ?? []).map((t) => Tag.fromJson(t as Map<String, dynamic>)).toList());
  Map<String, dynamic> toJson() => {'id': id, 'price': price, 'tags': tags.map((t) => t.toJson()).toList()};
}
class Tag { final String n; Tag(this.n); factory Tag.fromJson(Map<String, dynamic> j) => Tag(j['n']); Map<String, dynamic> toJson() => {'n': n}; }
class Page<T> { final List<T> items; final int total; Page(this.items, this.total);
  static Page<T> fromJson<T>(Map<String, dynamic> j, T Function(Map<String, dynamic>) f) =>
    Page((j['items'] as List).map((e) => f(e)).toList(), j['total']); }
void main() {
  var src = '{"items":[{"id":"a","price":1,"tags":[{"n":"x"}]},{"id":"b","price":2.5}],"total":2}';
  var p = Page.fromJson(jsonDecode(src), Item.fromJson);
  print(p.items is List<Item>); print(p.total);
  print(jsonEncode(p.items.map((e) => e.toJson()).toList()));
  try { Page.fromJson<Item>({'items': [{'id': 1, 'price': 1}], 'total': 1}, Item.fromJson); } catch (e) { print('bad ${e is TypeError}'); }
  try { Page.fromJson<Item>({'items': [], 'total': '1'}, Item.fromJson); } catch (e) { print('tot ${e is TypeError}'); }
  try { Tag.fromJson({'n': 5}); } catch (e) { print('tag ${e is TypeError}'); }
  var m = jsonDecode('{"a":{"b":[1,{"c":true}]}}') as Map<String, dynamic>;
  print(m['a']['b'][1]['c']);
  print((m['a'] as Map).keys.toList() is List<dynamic>);
  Map<String, List<int>> typed = (jsonDecode('{"x":[1,2]}') as Map).map((k, v) => MapEntry(k as String, (v as List).cast<int>()));
  print(typed['x']!.reduce((a, b) => a + b));
  try { Map<String, List<int>> t2 = jsonDecode('{"x":[1]}'); print(t2); } catch (e) { print('direct ${e is TypeError}'); }
  try { List<String> l = jsonDecode('["a"]'); print(l); } catch (e) { print('dl ${e is TypeError}'); }
  Iterable<String> it = (jsonDecode('["a","b"]') as List).cast<String>(); print(it.join('-'));
  try { Set<String> ss = Set<String>.from(jsonDecode('["a",1]')); print(ss); } catch (e) { print('sf ${e is TypeError}'); }
  try { Iterable<int> ii = jsonDecode('[1]'); print(ii); } catch (e) { print('ii ${e is TypeError}'); }
}
