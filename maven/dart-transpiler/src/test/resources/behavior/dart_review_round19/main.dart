// Statements the emitter lifts out of an operand Java evaluates lazily or
// repeatedly -- a loop condition or update, the right side of && || ??, a
// conditional's arm, a null-short's arguments -- must run where that operand is
// evaluated: every iteration, and never on the branch not taken. Also a lazily
// initialised top-level whose initialiser throws runs again on the next read.
class Box {
  int _v = 0;
  int get value => _v;
  set value(int v) {
    _v = v;
  }
}

int calls = 0;
int nextIndex() {
  calls++;
  return 0;
}

bool nexts = false;
int nextCalls = 0;
bool next() {
  nextCalls++;
  return nexts;
}

Box made = Box();
int makes = 0;
Box make() {
  makes++;
  return made;
}

int attempts = 0;
int flaky() {
  attempts++;
  if (attempts == 1) {
    throw StateError('first read fails');
  }
  return 42;
}

final int lazyValue = flaky();

int staticAttempts = 0;
class Config {
  static final String name = load();
  static String load() {
    staticAttempts++;
    if (staticAttempts == 1) {
      throw StateError('first read fails');
    }
    return 'loaded';
  }
}

List<int> source(List<int> xs) {
  calls++;
  return xs;
}

void main() {
  // compound index assignment in a while condition: every test
  final items = <int>[0];
  calls = 0;
  var loops = 0;
  while ((items[nextIndex()] += 1) < 5) {
    loops++;
  }
  print('while ${items[0]} loops=$loops calls=$calls');

  // ... and in a for condition and update
  final counts = <int>[0, 0];
  calls = 0;
  var passes = 0;
  for (var i = 0; (counts[nextIndex()] += 1) < 4; counts[1 + nextIndex()] += 2) {
    passes++;
    if (passes == 1) {
      continue;
    }
  }
  print('for $counts passes=$passes calls=$calls');

  // compound index on the right of a false && / true ||: not evaluated
  calls = 0;
  final a = <int>[7];
  final f = false;
  final t = true;
  print(f && (a[nextIndex()] += 1) > 0);
  print(t || (a[nextIndex()] += 1) > 0);
  print('short ${a[0]} calls=$calls');
  print(t && (a[nextIndex()] += 1) > 0);
  print('taken ${a[0]} calls=$calls');

  // getter/setter compound assignment in a for update: every pass, the receiver
  // evaluated once per pass
  makes = 0;
  for (var k = 0; k < 3; k++, make().value += 3) {}
  print('makes=$makes value=${made.value}');

  // a collection-if in an untaken conditional arm
  nextCalls = 0;
  final flag = false;
  final l1 = flag ? [if (next()) 1] : <int>[];
  print('$l1 nextCalls=$nextCalls');
  nexts = true;
  final l2 = !flag ? [if (next()) 1] : <int>[];
  print('$l2 nextCalls=$nextCalls');

  // spreading a null list behind its own null check
  List<int>? none;
  final spread = none == null ? <int>[] : [...none];
  print('spread $spread');
  List<int>? some = [4, 5];
  final spread2 = some == null ? <int>[] : [...some, 6];
  print('spread $spread2');
  final List<int> spread3 = none == null ? [] : [...none];
  print('spread $spread3');

  // set and map literals with spreads/if in an untaken arm
  final Set<int>? noSet = null;
  final s = noSet == null ? <int>{} : {...noSet};
  print('set $s');
  final Map<String, int>? noMap = null;
  final m = noMap == null ? <String, int>{} : {...noMap, 'k': 1};
  print('map $m');

  // a spread list in a while condition is rebuilt every iteration
  final grow = <int>[];
  calls = 0;
  while ([...source(grow), 0].length < 4) {
    grow.add(grow.length);
  }
  print('grow $grow calls=$calls');

  // ?? and ??= with a lifting default
  calls = 0;
  List<int>? present = [1];
  final r1 = present ?? [...source(<int>[9])];
  print('$r1 calls=$calls');
  final r2 = none ?? [...source(<int>[9])];
  print('$r2 calls=$calls');
  List<int>? cache = [3];
  cache ??= [...source(<int>[8])];
  print('$cache calls=$calls');

  // a null-short's arguments
  List<int>? target;
  calls = 0;
  target?.addAll(<int>[...source(<int>[1])]);
  print('null-short calls=$calls');
  target = <int>[];
  target?.addAll(<int>[...source(<int>[1])]);
  print('null-short $target calls=$calls');
  List<int>? noList;
  calls = 0;
  print(noList?.contains(<int>[...source(<int>[1])].first));
  print('null-short value calls=$calls');
  final slots = <String, List<int>>{'k': <int>[1]};
  print(slots['k'] ??= <int>[...source(<int>[2])]);
  print(slots['j'] ??= <int>[...source(<int>[2])]);
  print('slots $slots calls=$calls');

  // a throwing lazy initialiser runs again on the next read
  try {
    print(lazyValue);
  } on StateError {
    print('caught');
  }
  print(lazyValue);
  print('attempts=$attempts');
  try {
    print(Config.name);
  } on StateError {
    print('caught static');
  }
  print(Config.name);
  print('staticAttempts=$staticAttempts');
}
