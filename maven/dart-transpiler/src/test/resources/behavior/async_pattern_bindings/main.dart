import 'dart:async';

class Pt {
  final int x;
  final int y;
  Pt(this.x, this.y);
}

Future<(int, String)> pair(int n) async => (n, 'n$n');

Future<String> destructure() async {
  var (a, b) = await pair(3);
  await Future.delayed(Duration.zero);
  final (c, :d) = (a * 2, d: b.toUpperCase());
  await null;
  var Pt(:x, y: py) = Pt(a + 1, c + 1);
  var [first, second] = [x, py];
  await Future.value(0);
  return '$a $b $c $d $x $py $first $second';
}

Future<List<String>> loopCaptures() async {
  var fns = <String Function()>[];
  for (var i = 0; i < 3; i++) {
    var (n, s) = await pair(i);
    fns.add(() => '$n$s');
    await null;
  }
  return [for (var f in fns) f()];
}

void main() async {
  print(await destructure());
  print(await loopCaptures());
}
