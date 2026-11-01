// A named parameter reassigned, then captured by a closure.
void run(void Function() f) => f();

String label({int? line, String? text}) {
  if (line != null) text = (line + 1).toString();
  var out = '';
  run(() {
    out = (text ?? '').padRight(4) + '|';
  });
  return out;
}

// A positional parameter, reassigned after a closure that reads it is made.
List<String Function()> counters(int start) {
  final out = <String Function()>[];
  out.add(() => 'first sees $start');
  start = start * 10;
  out.add(() => 'second sees $start');
  return out;
}

class Gauge {
  String reading = '';
  // A constructor parameter.
  Gauge(int level) {
    if (level < 0) level = 0;
    run(() {
      reading = 'level $level';
    });
  }

  // An instance method parameter, reassigned inside the closure itself.
  int drain(int amount) {
    run(() {
      amount = amount - 1;
    });
    return amount;
  }

  // A static method's optional positional parameter.
  static String tag([String prefix = 'p']) {
    prefix = prefix + '!';
    var s = '';
    run(() => s = prefix);
    return s;
  }
}

class Sidebar {
  final StringBuffer _buffer = StringBuffer();

  // Named parameters of an instance method, read only through a cascade in the closure.
  String write({int? line, String? text, String? end}) {
    if (line != null) text = (line + 1).toString();
    run(() {
      _buffer
        ..write((text ?? '').padRight(3))
        ..write(end ?? '|');
    });
    return _buffer.toString();
  }
}

extension Padding on String {
  // An extension member parameter.
  String around(String edge) {
    if (edge.isEmpty) edge = '*';
    var r = '';
    run(() => r = edge + this + edge);
    return r;
  }
}

void main() {
  print(label(line: 2));
  print(label(text: 'ab'));
  print(label());
  for (final f in counters(3)) {
    print(f());
  }
  print(Gauge(-4).reading);
  print(Gauge(7).reading);
  print(Gauge(1).drain(5));
  print(Gauge.tag());
  print(Gauge.tag('q'));
  print('mid'.around(''));
  print('mid'.around('#'));
  // A closure's own parameter, reassigned and captured by a nested closure.
  final twice = (int n) {
    n = n * 2;
    var seen = 0;
    run(() => seen = n);
    return seen;
  };
  print(twice(21));
  print(Sidebar().write(line: 8));
  print(Sidebar().write(text: 'z', end: ':'));
}
