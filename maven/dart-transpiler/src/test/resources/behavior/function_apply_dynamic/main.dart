// Function.apply over closures, tear-offs, static functions and runtime functions.
int add3(int a, int b, int c) => a + b + c;

class Counter {
  int n = 0;
  int bump(int by) => n += by;
  static String tag(String s) => '<$s>';
}

String describe(Function f, List args) => 'got ${Function.apply(f, args)}';

void main() {
  print(Function.apply(add3, [1, 2, 3]));
  print(Function.apply((String s) => s.toUpperCase(), ['abc']));
  final c = Counter();
  Function.apply(c.bump, [5]);
  print(Function.apply(c.bump, [2]));
  print(Function.apply(Counter.tag, ['x']));
  print(Function.apply(() => 'none', []));
  print(Function.apply(() => 'empty named', [], {}));
  dynamic d = (int x, int y) => x * y;
  print(Function.apply(d, [6, 7]));
  print(describe(add3, [10, 20, 30]));
  final List<Function> fs = [add3, (int a, int b, int c) => a * b * c];
  for (final f in fs) {
    print(Function.apply(f, [2, 3, 4]));
  }
  print(Function.apply(print, ['printed via apply']) == null);
  try {
    Function.apply(add3, [1]);
    print('no error');
  } on NoSuchMethodError {
    print('mismatch');
  }
}
