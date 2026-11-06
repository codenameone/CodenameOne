// List literals of closures with no context type: the element type is the
// closures' common function shape, keeping parameter types they all declare.
void main() {
  var steps = [(int x) => x + 1, (int x) => x * 2];
  print(steps.map((g) => g(5)).toList());
  var labels = [(String s, int n) => s * n, (String s, int n) => '$s$n'];
  print(labels.map((f) => f('ab', 2)).toList());
  var mixed = [() => 1, () => 'two'];
  for (var f in mixed) {
    print(f());
  }
  var sums = [() => 1, () => 2];
  print(sums[0]() + sums[1]());
  var effects = [() { print('a'); }, () { print('b'); }];
  for (var e in effects) {
    e();
  }
}
