class Base {
  int raw = 1;
  String? note;
}

class Sub extends Base {}

extension BaseX on Base {
  int get score => raw * 10;
  set score(int v) => raw = v ~/ 10;
  String? get memo => note;
  set memo(String? v) => note = v == null ? null : 'base:$v';
}

extension SubX on Sub {
  int get score => raw * 100;
  set score(int v) => raw = v ~/ 100;
}

extension ListX<T> on List<T> {
  T get head => first;
  set head(T v) => this[0] = v;
  int get size => length;
  set size(int n) {
    while (length > n) {
      removeLast();
    }
  }
}

extension IntListX on List<int> {
  int get total => fold(0, (a, b) => a + b);
  set total(int v) => this[length - 1] += v - total;
}

void main() {
  Base b = Base();
  b.score += 25;
  print(b.raw);
  b.score++;
  print(b.raw);
  ++b.score;
  print(b.raw);
  b.memo ??= 'x';
  print(b.note);
  b.memo ??= 'y';
  print(b.note);

  Base s = Sub();
  if (s is Sub) {
    s.score += 250;
    print(s.raw);
    s.score++;
    print(s.raw);
    s.score -= 100;
    print(s.raw);
  }
  final s2 = Sub();
  s2.score *= 3;
  print(s2.raw);
  final r = (s2.score += 100);
  print('$r ${s2.raw}');

  final names = <String>['a', 'b', 'c'];
  names.head = 'z';
  print(names);
  names.head += '!';
  print(names);
  names.size = 2;
  print(names);
  names.size -= 1;
  print(names);

  final nums = <int>[1, 2, 3];
  nums.head += 10;
  print(nums);
  nums.head++;
  print(nums);
  nums.total = 100;
  print(nums);
  nums.total += 1;
  print(nums);

  List<String?> maybe = <String?>[null, 'q'];
  maybe.head ??= 'filled';
  print(maybe);
}
