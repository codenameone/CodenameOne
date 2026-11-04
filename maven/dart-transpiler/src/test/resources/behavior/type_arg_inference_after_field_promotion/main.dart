void sh<T>(T x) {
  print('$T');
}

String? maybe(int n) => n > 0 ? 'v$n' : null;

class Holder {
  String? value;
  String get fixed => 'f';
}

void early(String? a, String? b, int n) {
  if (a == null || b == null) {
    return;
  }
  sh(a);
  sh(b);
  String? c = maybe(n) ?? 'd';
  sh(c);
  String? d = maybe(n);
  if (!(d == null)) {
    sh(d);
  }
  if (d == null) {
    d = 'set';
  }
  sh(d);
  Holder h = Holder();
  String? e = h.value;
  sh(e);
  String? f = h.fixed;
  sh(f);
  String? g = maybe(n);
  if (g == null) {
    throw StateError('no');
  }
  sh(g);
}

void captured(int n) {
  String? a = 'before';
  sh(a);
  var later = () {
    a = maybe(n);
  };
  later();
  sh(a);
  if (a != null) {
    sh(a);
  }
  String? b = maybe(n);
  while (b != null) {
    sh(b);
    break;
  }
  String? c = 'c';
  do {
    sh(c);
    c = null;
  } while (c != null);
  sh(c);
}

void main() {
  early('x', 'y', 2);
  print('--');
  captured(1);
}
