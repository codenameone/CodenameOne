// A closure invoked where it is written, whose returns are a String constant,
// null and a nullable cast: its value is a String?, passed to String? slots.
const String warm = 'warm';

class Tint {
  final String? name;
  final int weight;

  Tint(Object? choice)
      : this._(() {
          if (choice == true) return warm;
          if (choice == false) return null;
          return choice as String?;
        }(), 1);

  Tint._(this.name, this.weight);
}

String describe(String? s) => s == null ? '<none>' : s.toUpperCase();

void main() {
  print(Tint(true).name);
  print(Tint(false).name);
  print(Tint('cool').name);
  final String? local = () {
    if (warm.length > 9) return null;
    return warm;
  }();
  print(describe(local));
  print(describe(() {
    if (local == null) return 'x';
    return null;
  }()));
}
