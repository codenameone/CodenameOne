int apply(int value, int transform(int v)) => transform(value);

void each(List<String> items, void visit(String item, int index)) {
  for (var i = 0; i < items.length; i++) {
    visit(items[i], i);
  }
}

String? maybe(String s, String format(String x)?) => format == null ? null : format(s);

void main() {
  print(apply(20, (v) => v * 2 + 2));
  each(['a', 'b'], (item, index) => print('$index:$item'));
  print(maybe('x', (x) => '[$x]'));
  print(maybe('x', null));
}
