import 'dart:convert';
typedef Shaper<T> = String Function(int count, T value);
typedef Labeler = String Function(String text, [String? suffix]);

String _plain(String text, [String? suffix]) => '$text${suffix ?? '.'}';

final Labeler defaultLabeler = _plain;
final String Function(String a, String b) joiner = (a, b) => '$a+$b';

String Function(int n) makeRepeater(String s) => (int n) => s * n;

class Panel {
  final Shaper<List<int>> shaper;
  final Labeler labeler;
  Panel(this.shaper, {Labeler? labeler}) : labeler = labeler ?? defaultLabeler;

  String render(List<int> xs) => labeler(shaper(xs.length, xs), '!');
}

class _Holder {
  final Shaper<String> builder;
  _Holder(this.builder);
}

class _HolderState {
  final _Holder widget;
  _HolderState(this.widget);
  String build() => widget.builder(2, 'w');
}

void main() {
  print(defaultLabeler('hi'));
  print(defaultLabeler('hi', '?'));
  print(joiner('x', 'y'));
  final p = Panel((c, v) => 'n=$c ${v.first}');
  print(p.render([7, 8]));
  print(_HolderState(_Holder((c, v) => v * c)).build());
  print(makeRepeater('ab')(3));
  final dec = jsonDecode;
  print(dec('{"a":2}'));
  print(dec('[1]'));
  print(['{"b":1}'].map(jsonDecode).toList());
}
