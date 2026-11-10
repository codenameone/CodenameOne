class Wide {}

int calls = 0;
bool t(bool v) {
  calls++;
  return v;
}

void postfix(Object o) {
  if (o is int) {
    var x = o;
    Object? y = o;
    if (y is int) {
      print(t(true) && y++ > 0);
      print(y);
      print(t(false) && y++ > 0);
      print(y);
      print(t(false) || y++ > 0);
      print(y);
      print(t(true) || y++ > 0);
      print(y);
      print(t(true) ? y++ : y--);
      print(y);
      print(t(false) ? y++ : y--);
      print(y);
      int? n = null;
      print(n ?? y++);
      print(y);
      n = 5;
      print(n ?? y++);
      print(y);
      print(x);
    }
    Object? w = o;
    var i = 0;
    while (w is int && w++ < 9) {
      i++;
    }
    print('$i $w');
    for (; w is int && w-- > 7;) {
      i++;
    }
    print('$i $w');
    do {
      i++;
    } while (w is int && --w > 3 && w++ > 0 && w-- > 0);
    print('$i $w');
    if (w is int) {
    }
  }
  print(calls);
}

String kind(Object? x) {
  final out = StringBuffer();
  if (x is String) {
    out.write('S');
    if (x is int) out.write('!int');
    if (x is! int) out.write('notint');
    if (x is Wide) out.write('!Wide');
    if (x is! Wide) out.write('notWide');
    if (x is num) out.write('!num');
    try {
      final v = x as int;
      out.write('as$v');
    } on TypeError {
      out.write('asFail');
    }
    switch (x) {
      case int i:
        out.write('case$i');
      case Wide _:
        out.write('caseWide');
      case String s when s.isEmpty:
        out.write('empty');
      default:
        out.write('dflt');
    }
    final r = switch (x) { int() => 'i', Wide() => 'L', _ => 'o' };
    out.write(r);
    if (x case int z) out.write('ic$z');
  } else if (x is int) {
    out.write('I');
    if (x is String) out.write('!str');
    if (x is Wide) out.write('!Wide');
    if (x is num) out.write('num');
    if (x is! double) out.write('notdbl');
  } else if (x is Wide) {
    out.write('L');
    if (x is String) out.write('!str');
    if (x is int) out.write('!int');
    out.write(x is Object ? 'obj' : '');
  }
  return out.toString();
}

void main() {
  postfix(1);
  print(kind('a'));
  print(kind(''));
  print(kind(3));
  print(kind(Wide()));
  print(kind(null));
}
