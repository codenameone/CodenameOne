void sh<T>(T x) {
  print('$T');
}

String? maybe(int i) => i > 1 ? 'm' : null;

void forExit() {
  String? x;
  for (var i = 0; x == null; i++) {
    x = maybe(i);
  }
  sh(x);
}

void whileBreak(String? y) {
  String? x;
  while (x == null) {
    if (y == null) break;
    x = y;
  }
  sh(x);
}

void orAssign(String? x, String? y) {
  if (x == null || (y = x).isEmpty) {
    return;
  }
  sh(x);
}

void condAssign(String? x, bool f) {
  String? r = x != null ? (f ? x : 'q') : 'n';
  sh(r);
  sh(x == null ? 'n' : x);
}

void bangInCatch(String? x) {
  try {
    x!;
    sh(x);
  } catch (e) {
    sh(x);
  }
  sh(x);
}

void main() {
  forExit();
  whileBreak(null);
  whileBreak('a');
  orAssign('a', null);
  condAssign('a', true);
  bangInCatch(null);
}
