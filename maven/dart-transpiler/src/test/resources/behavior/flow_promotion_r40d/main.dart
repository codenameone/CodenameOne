void sh<T>(T x) {
  print('$T');
}

void boom(bool b) {
  if (b) throw StateError('x');
}

void fin(String? x) {
  try {
    if (x == null) return;
  } finally {
    print(x ?? 'dflt');
    print(x?.length);
    print(x.toString());
    print([x].runtimeType);
  }
}

void cat(bool f) {
  String? y = 'b';
  try {
    y = null;
    boom(f);
    y = 'c';
  } catch (e) {
    print(y ?? 'dflt');
    print(y?.length);
    print({y}.runtimeType);
  }
}

void blk(String? x) {
  lbl:
  {
    if (x == null) break lbl;
  }
  print(x ?? 'dflt');
  print(x?.length);
}

void andAssign(String? x) {
  if (x != null && (x = null) == null) {
    sh(x);
    print(x ?? 'dflt');
  }
}

void tryNested(bool f) {
  String? x = 'a';
  try {
    try {
      x = null;
      boom(f);
      x = 'b';
    } finally {
      sh(x);
    }
  } catch (e) {
    sh(x);
  }
  sh(x);
}

void main() {
  fin(null);
  cat(true);
  blk(null);
  andAssign('q');
  tryNested(true);
}
