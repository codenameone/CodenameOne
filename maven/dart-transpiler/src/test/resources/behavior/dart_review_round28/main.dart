// Two unresolved-member diagnostics that fired on real dart:core API:
//
// `Iterable.whereType<T>()` has no predicate argument, so the collection
// method table (keyed on method name + positional args) had no case for it
// at all and fell through to the generic "cannot resolve method" error,
// E0137. The runtime DartIterable/DartList/DartSet already declare
// `whereType(Class<T> type)`, taking the requested type as a trailing
// witness; the emitter just never threaded it through.
//
// `Duration.zero`, `Duration.microsecondsPerSecond` and friends are
// compile-time constants Dart defines on the Duration class, but the static
// member dispatch (`isClassRef`) had no case for Duration at all, so every
// one of them fell through to the unresolved-static-member error, E0131.

class Animal {
  Animal(this.name);
  final String name;
}

class Dog extends Animal {
  Dog(String name) : super(name);
  String bark() => '${name} says Woof';
}

class Cat extends Animal {
  Cat(String name) : super(name);
}

void whereTypeMixedList() {
  final List<Object?> mixed = <Object?>[1, 'a', 2, 'b', 3.5, null, 'c', 4];
  final List<int> ints = mixed.whereType<int>().toList();
  final List<String> strings = mixed.whereType<String>().toList();
  print('ints ${ints.join(",")}');
  print('strings ${strings.join(",")}');
}

void whereTypeClassHierarchy() {
  final List<Animal> animals = <Animal>[Dog('Rex'), Cat('Tom'), Dog('Fido')];
  final Iterable<Dog> dogs = animals.whereType<Dog>();
  final List<String> barks = <String>[];
  for (final Dog d in dogs) {
    barks.add(d.bark());
  }
  print('dogs ${barks.join(",")}');
}

void durationConstants() {
  print(Duration.zero == Duration());
  print(Duration.microsecondsPerMillisecond);
  print(Duration.millisecondsPerSecond);
  print(Duration.secondsPerMinute);
  print(Duration.minutesPerHour);
  print(Duration.hoursPerDay);
  print(Duration.microsecondsPerSecond);
  print(Duration.microsecondsPerMinute);
  print(Duration.microsecondsPerHour);
  print(Duration.microsecondsPerDay);
  print(Duration.millisecondsPerMinute);
  print(Duration.millisecondsPerHour);
  print(Duration.millisecondsPerDay);
  print(Duration.secondsPerHour);
  print(Duration.secondsPerDay);
  print(Duration.minutesPerDay);
}

void main() {
  whereTypeMixedList();
  whereTypeClassHierarchy();
  durationConstants();
}
