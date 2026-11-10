class Box {
  final String name;
  Box(this.name);
  String? tag(String? s) => s == null ? null : '$name:$s';
  void note(Object o) { log.add('$name noted $o'); }
}

final List<String> log = <String>[];
int picks = 0;
final Box shared = Box('b');
Box pick() { picks++; return shared; }

void main() {
  final fs = [pick().tag, pick().note].toSet();
  print(fs.length);
  for (final f in fs) {
    f('x');
  }
  print(log);
  print(picks);
  final gs = [shared.note, pick().tag];
  for (final g in gs) {
    g('y');
  }
  print(log);
  print(gs.contains(shared.tag));
  print(picks);
}
