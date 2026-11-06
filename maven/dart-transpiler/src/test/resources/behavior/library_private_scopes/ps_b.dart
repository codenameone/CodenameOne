int _v = 7;
int _tick() => 70;

class PB {
  int _v = 700;
  int _tick() => 7000;
  int both() => _v + _tick();
}

Symbol symbolB() => #_v;
int readB() => _v + _tick();
int memberB(PB p) => p._v + p.both();
// Another library's `_v` is not this one's: the PA instance has no `_v` here.
String memberB2(dynamic p) {
  try {
    return '${p._v}';
  } on NoSuchMethodError {
    return 'no _v';
  }
}
void spyB(dynamic s) {
  s._tick();
}
