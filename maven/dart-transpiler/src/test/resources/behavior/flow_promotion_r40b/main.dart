void sh<T>(T x) {
  print('$T');
}

String? maybe(bool b) => b ? 'm' : null;


class Holder {
  String? note = 'n';
  void run() {
    if (note != null) {
      sh(note);
      clear();
      sh(note);
    }
    String? note2 = note;
    sh(note2);
  }

  void clear() {
    note = null;
  }
}

void closures(bool f) {
  String? x = 'a';
  void g() {
    x = null;
  }
  sh(x);
  g();
  sh(x);
  if (x != null) sh(x);
  String? y = 'b';
  var h = () {
    y = null;
  };
  h();
  sh(y);
  String? z = 'c';
  sh(z);
  String? w;
  w = 'w';
  var r = () => sh(w);
  r();
  sh(w);
  String? q = 'q';
  void later() {
    sh(q);
  }
  later();
  q = null;
  later();
}

void loopClosure() {
  String? x = 'a';
  var fs = <void Function()>[];
  for (var i = 0; i < 2; i++) {
    fs.add(() {
      x = null;
    });
    sh(x);
  }
  fs[0]();
  sh(x);
}


void bang(String? x) {
  x!;
  sh(x);
}

void whileNull(bool f) {
  String? x;
  while (x == null) {
    x = maybe(true);
  }
  sh(x);
}

void main() {
  Holder().run();
  closures(true);
  loopClosure();
  bang('b');
  whileNull(true);
}
