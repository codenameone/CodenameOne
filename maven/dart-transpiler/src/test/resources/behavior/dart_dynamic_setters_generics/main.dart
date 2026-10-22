class Box {
  int _count = 0;
  int get count => _count;
  set count(int v) {
    _count = v * 2;
  }

  T pick<T>(T a, T b, bool first) => first ? a : b;

  List<T> pair<T>(T a) => [a, a];

  int add(int a, int b) => a + b;
}

void main() {
  dynamic b = Box();
  b.count = 3;
  print(b.count);
  print(b.pick('x', 'y', false));
  print(b.pair(7));
  try {
    b.add('1', 2);
    print('no error');
  } on TypeError {
    print('TypeError');
  }
  try {
    b.count = 'nine';
    print('no error');
  } on TypeError {
    print('TypeError');
  }
}
