import 'dart:async';
import 'dart:collection';
import 'dart:convert';

abstract class Shape {
  int get sides;
}

abstract class Polygon implements Shape {
  String get label;
}

class Square implements Polygon {
  @override
  int get sides => 4;
  @override
  String get label => 'square';
}

extension Describe on Polygon {
  String describe() => '$label has $sides sides';
}

Future<void> main() async {
  print(Uri.encodeComponent('a b&c/d?e=f'));
  print(Uri.decodeComponent('a%20b%26c'));
  print(Uri.file('/tmp/my file.txt'));

  print(const HtmlEscape(HtmlEscapeMode.attribute).convert('<a href="x">\'</a>'));
  print(HtmlEscape(HtmlEscapeMode(escapeLtGt: true)).convert('<b>&"</b>'));
  print(const HtmlEscape().convert('<i>"x"</i>'));

  var err = AsyncError('boom', null);
  print(err.error);

  var recovered = await Future<int>.error('bad').onError((Object e, StackTrace s) => 7);
  print(recovered);

  var controller = StreamController<int>();
  var seen = <int>[];
  controller.stream.listen(seen.add);
  await controller.addStream(Stream.fromIterable([1, 2, 3]), cancelOnError: false);
  await controller.close();
  print(seen);

  Set<Object> objects = {1, 2};
  var view = Set.castFrom<Object, int>(objects);
  view.add(3);
  print(objects.length);

  var none = const Iterable<String>.empty();
  print(none.isEmpty);

  print(MapBase.mapToString({'a': 1, 'b': 2}));
  print(SetBase.setToString({'x', 'y'}));
  print(IterableBase.iterableToFullString([1, 2], '[', ']'));

  print(Square().describe());
}
