// An app's own User, Position and Priority win over the plugin stubs of the same
// names, which are global: a user enum used to be replaced by the stub's.
class User {
  final String name;
  User(this.name);
  String greet() => 'hi $name';
}

class Position {
  final int x;
  const Position(this.x);
}

enum Priority { low, urgent }

void main() {
  print(User('ann').greet());
  print(const Position(3).x);
  print(Priority.urgent.name);
}
