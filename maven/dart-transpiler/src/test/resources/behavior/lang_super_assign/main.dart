class Base {
  int _v = 0;
  int count = 0;
  int get value => _v;
  set value(int v) {
    _v = v;
  }
}

class Clamped extends Base {
  @override
  set value(int v) {
    super.value = v < 0 ? 0 : v;
    super.count += 1;
  }
}

void main() {
  final c = Clamped();
  c.value = -5;
  print(c.value);
  c.value = 7;
  print(c.value);
  print(c.count);
}
