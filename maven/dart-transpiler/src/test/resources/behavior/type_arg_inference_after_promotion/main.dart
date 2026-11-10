void sh<T>(T x) {
  print('$T');
}

String name() => 'n';
String? maybe(bool b) => b ? 'm' : null;

class Box {
  final String label;
  String? note;
  Box(this.label);
  String get upper => label.toUpperCase();
}

void initRules(bool flag) {
  String? a = name();
  sh(a);
  Box bx = Box('q');
  String? b = bx.upper;
  sh(b);
  String? c = maybe(flag)!;
  sh(c);
  String? d = bx.label;
  sh(d);
  String? e = maybe(flag);
  sh(e);
  String? f = bx.note;
  sh(f);
  String? g = 'x';
  g = maybe(flag);
  sh(g);
  g = name();
  sh(g);
  g ??= 'y';
  sh(g);
  String? h = name();
  if (flag) {
    h = null;
  }
  sh(h);
  String? i;
  if (flag) {
    i = 'a';
  } else {
    i = name();
  }
  sh(i);
}

void nullChecks(String? p, String? q, String? r) {
  if (p != null) {
    sh(p);
  } else {
    sh(p);
  }
  sh(p);
  if (q == null) {
    return;
  }
  sh(q);
  if (r == null) {
    sh(r);
  } else {
    sh(r);
  }
  String? s = maybe(true);
  if (s != null && s.isNotEmpty) {
    sh(s);
  }
}

void noPromotion(bool flag) {
  String? a = name();
  void set() {
    a = null;
  }
  sh(a);
  set();
  String? b = maybe(flag);
  var reset = () {
    b = 'z';
  };
  if (b != null) {
    sh(b);
  }
  reset();
  String? c = name();
  for (int k = 0; k < 2; k++) {
    sh(c);
    c = maybe(flag);
  }
  sh(c);
  String? d = name();
  int n = 0;
  while (n < 1) {
    sh(d);
    d = name();
    n++;
  }
  sh(d);
  String? e = name();
  for (var x in [1]) {
    sh(e);
    e = null;
  }
  String? f = name();
  sh(f);
  var g = () {
    sh(f);
  };
  g();
}

void main() {
  initRules(true);
  print('--');
  nullChecks('p', 'q', null);
  print('--');
  noPromotion(true);
}
