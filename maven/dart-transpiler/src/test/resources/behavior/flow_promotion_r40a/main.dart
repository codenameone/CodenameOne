void sh<T>(T x) {
  print('$T');
}

String? maybe(bool b) => b ? 'm' : null;
void boom(bool b) {
  if (b) throw StateError('x');
}

void labelled(String? x) {
  lbl:
  {
    if (x == null) break lbl;
    sh(x);
  }
  sh(x);
}

void labelLoop(String? x, String? y) {
  outer:
  for (var i = 0; i < 2; i++) {
    for (var j = 0; j < 2; j++) {
      if (x == null) continue outer;
      if (y == null) break outer;
      sh(x);
    }
    sh(x);
  }
  sh(x);
}

void tryCatch(bool f) {
  String? x;
  try {
    x = 'a';
    boom(f);
    x = null;
  } catch (e) {
    sh(x);
  }
  sh(x);
  String? y = 'b';
  try {
    y = null;
    boom(f);
    y = 'c';
  } catch (e) {
    sh(y);
  }
  sh(y);
}

void tryFinally(String? x) {
  try {
    if (x == null) return;
    sh(x);
  } finally {
    sh(x);
  }
  sh(x);
}

void tryFinally2(bool f) {
  String? x = 'a';
  try {
    x = null;
    boom(f);
    x = 'b';
  } finally {
    sh(x);
  }
  sh(x);
}

void catchAssign(bool f) {
  String? x = 'a';
  try {
    boom(f);
  } catch (e) {
    x = null;
  }
  sh(x);
  String? z;
  try {
    z = 'q';
  } catch (e) {
    z = 'r';
  }
  sh(z);
}

void sw(int k, String? x) {
  switch (k) {
    case 1:
      if (x == null) break;
      sh(x);
    case 2:
      sh(x);
    default:
      sh(x);
  }
  sh(x);
  String? y = 'a';
  switch (k) {
    case 1:
      y = null;
    case 2:
      sh(y);
  }
  sh(y);
}

void nested(bool f) {
  String? x = 'a';
  sh(x);
  if (f) {
    if (!f) {
      x = null;
    }
  }
  sh(x);
  {
    x = null;
  }
  sh(x);
  x = 'b';
  for (var i = 0; i < 1; i++) {
    sh(x);
    x = maybe(f);
  }
  sh(x);
  String? w = 'w';
  while (f) {
    sh(w);
    w = null;
    f = false;
  }
  sh(w);
  String? d = 'd';
  do {
    sh(d);
    d = null;
  } while (false);
  sh(d);
}

void lateLoc(bool f) {
  late String? x;
  x = 'a';
  sh(x);
  late String? y = maybe(f);
  sh(y);
  if (y != null) sh(y);
}

void condExpr(bool f) {
  String? x = maybe(f);
  String? v = x == null ? null : x;
  sh(v);
  sh(x != null ? x : 'z');
  if (x != null && x.isNotEmpty) sh(x);
  bool ok = x != null && (x = null) == null;
  sh(x);
  print(ok);
}

void main() {
  labelled(null);
  labelled('a');
  labelLoop('a', null);
  labelLoop(null, 'b');
  tryCatch(true);
  tryCatch(false);
  tryFinally(null);
  tryFinally('q');
  try {
    tryFinally2(true);
  } catch (e) {
    print('caught');
  }
  catchAssign(true);
  sw(1, 'a');
  sw(1, null);
  nested(true);
  lateLoc(true);
  condExpr(true);
}
