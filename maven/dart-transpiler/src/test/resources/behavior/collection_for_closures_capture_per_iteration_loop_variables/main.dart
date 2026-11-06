void main() {
  final fs = [for (var i = 0; i < 3; i++) () => i];
  print(fs.map((f) => f()).toList());
  final gs = [for (final x in [1, 2, 3]) () => x * 10];
  print(gs.map((f) => f()).toList());
  final hs = {for (var i = 0; i < 2; i++) i: () => i};
  print(hs.values.map((f) => f()).toList());
  var cs = <Function>[];
  for (var i = 0; i < 3; i++) { cs.add(() => i); }
  print(cs.map((f) => f()).toList());
  final nested = [for (var i = 0; i < 2; i++) for (var j = 0; j < 2; j++) () => '$i$j'];
  print(nested.map((f) => f()).toList());
  final cond = [for (var i = 0; i < 4; i++) if (i.isEven) () => i];
  print(cond.map((f) => f()).toList());
  var k = 0; final mut = [for (; k < 3; k++) () => k]; print(mut.map((f) => f()).toList());
  final spread = [for (var i = 0; i < 2; i++) ...[() => i, () => i + 100]]; print(spread.map((f) => f()).toList());
  final mod = [for (var i = 0; i < 3; i++) () { i += 10; return i; }]; print(mod.map((f) => f()).toList());
}
