// Declares `_v`, `_w` and `_tick` too, so ls_b.dart's are renamed apart from these.
int _v = 1;
int _w = 2;
int _tick() => 3;

class LA {
  int _v = 10;
  int _w = 20;
  int _tick() => 30;
  int sum() => _v + _w + _tick();
}

Symbol symbolA() => #_v;

class Spy {
  @override
  dynamic noSuchMethod(Invocation i) {
    print('spy ${i.memberName} ${i.isGetter ? 'get' : i.isSetter ? 'set' : 'call'}');
    return null;
  }
}
