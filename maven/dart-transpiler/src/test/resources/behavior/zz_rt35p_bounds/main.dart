class F<T extends Comparable<T>> {
  final T v;
  F(this.v);
  String toString() => 'F<$T>($v)';
}

void attempt(String label, Object? Function() body) {
  try {
    print('$label ${body()}');
  } on TypeError {
    print('$label TypeError');
  }
}

void main() {
  dynamic d = F.new;
  Function f = F.new;
  Object o = F.new;
  print(d is Function);
  attempt('d', () => d(3));
  attempt('f', () => f('x'));
  attempt('o', () => (o as Function)(2.5));
  attempt('apply', () => Function.apply(F.new, [5]));
  F<String> Function(String) typed = F.new;
  print(typed("t"));
}
