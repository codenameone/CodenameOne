extension Ranges on int {
  Iterable<int> to(int end) sync* {
    for (var i = this; i <= end; i++) {
      yield i;
    }
  }

  Stream<int> countdown() async* {
    for (var i = this; i > 0; i--) {
      yield i;
    }
  }
}

class Cell<T> {
  final T value;
  Cell(this.value);
  @override
  String toString() => 'Cell($value)';
}

String pad(String s, [int width = 5, String fill = '.']) => s.padLeft(width, fill);

String describe(Object o) => switch (o) {
      String(length: var n) when n > 3 => 'long string $n',
      String(length: var n) => 'short string $n',
      List(length: var n) => 'list $n',
      _ => 'other',
    };

DateTime fixed() => DateTime(2024, 3, 15);

Future<void> main() async {
  print(1.to(4).toList());
  print(await 3.countdown().toList());
  var sb = StringBuffer();
  sb.write('a');
  sb.write(1);
  print(sb.toString());
  var sb2 = StringBuffer('x');
  sb2.writeln('y');
  print(sb2.length);
  var sym = #foo;
  Object o = sym;
  print(o is Symbol);
  print(sym == #foo);
  print(sym == #bar);
  print(sym);
  print(describe('hi'));
  print(describe('hello'));
  print(describe([1, 2]));
  print(describe(3));
  var i = 0;
  do {
    i += 2;
  } while (i < 7);
  print(i);
  print(pad('a'));
  print(Cell<int>(7));
  dynamic f = fixed;
  print((f)().year);
  dynamic d = const Duration(minutes: 2);
  print(d.inSeconds);
  dynamic u = Uri.parse('https://example.com/a?x=1');
  print(u.host);
  dynamic s = 'abc';
  print(s.length);
  dynamic l = [1, 2, 3];
  print(l.last);
}
