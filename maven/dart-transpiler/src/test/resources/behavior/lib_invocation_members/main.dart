class Recorder {
  final List<String> log = [];
  @override
  dynamic noSuchMethod(Invocation i) {
    final name = i.memberName.toString();
    log.add('$name get=${i.isGetter} set=${i.isSetter} method=${i.isMethod} '
        'pos=${i.positionalArguments} named=${i.namedArguments.length}');
    if (i.memberName == #answer) return 42;
    if (i.isGetter) return 'got';
    return null;
  }
}

void main() {
  dynamic r = Recorder();
  print(r.answer());
  print(r.colour);
  r.colour = 'red';
  r.paint(1, 'two', size: 3);
  for (final line in (r as Recorder).log) {
    print(line);
  }
  print(#answer == Symbol('answer'));
  const s = #answer;
  print(s.toString());
}
