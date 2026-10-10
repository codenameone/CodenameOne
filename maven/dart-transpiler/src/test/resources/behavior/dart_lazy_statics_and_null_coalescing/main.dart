// ?? evaluates its left side where the expression is, int.tryParse answers null,
// and a class's statics initialise one at a time, on first read.

int _calls = 0;
int? next() {
  _calls++;
  return _calls < 3 ? _calls : null;
}

bool? sideEffect() {
  print('side effect ran');
  return null;
}

int fail() {
  throw StateError('expensive initialised too early');
}

int _made = 0;
int make() {
  _made++;
  print('make ran');
  return 40 + _made;
}

class Config {
  static const bool ready = true;
  static final int expensive = fail();
  static int counter = make();
  static String? label;
}

void main() {
  // ?? in a loop condition runs every iteration
  var seen = 0;
  while ((next() ?? 0) > 0) {
    seen++;
  }
  print('loop ran $seen times');

  // ?? in a short-circuited operand never runs
  final skipped = false && (sideEffect() ?? true);
  print(skipped);

  // tryParse / parse
  print(int.tryParse('x1'));
  print(int.tryParse(' 42 '));
  print(double.tryParse('nope'));
  try {
    int.parse('nope');
  } on FormatException {
    print('parse threw FormatException');
  }

  // statics: reading one does not initialise the others
  print(Config.ready);
  print(Config.counter);
  Config.counter += 2;
  print(Config.counter);
  print(Config.label ?? 'no label');
  Config.label ??= 'set once';
  Config.label ??= 'set twice';
  print(Config.label);
  try {
    print(Config.expensive);
  } on StateError {
    print('expensive initialised on its own first read');
  }
}
