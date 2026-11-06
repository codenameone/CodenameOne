int _v = 100;
int _tick() => 1000;

class PA {
  int _v = 200;
  int _tick() => 2000;
  int both() => _v + _tick();
}

class QA {
  int read() => _v + _tick();
}

Symbol symbolA() => #_v;
int readA() => _v + QA().read();
int memberA(PA p) => p._v + p.both();
void spyA(dynamic s) {
  s._v;
  s._v = 4;
}
