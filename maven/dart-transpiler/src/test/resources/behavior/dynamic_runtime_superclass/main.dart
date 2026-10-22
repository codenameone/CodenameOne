// A model class whose superclass comes from a runtime stub (Equatable) is still
// model data: read through `dynamic`, its fields and methods must resolve. It was
// left without dynamic members and threw NoSuchMethodError. Equal props with 1 and
// 1.0 must also hash alike, since Equatable compares them equal.
import 'package:equatable/equatable.dart';

class Pet extends Equatable {
  final String name;
  final num age;
  const Pet(this.name, this.age);

  String greet() => 'hi $name';

  @override
  List<Object?> get props => [name, age];
}

void main() {
  dynamic pet = Pet('Miso', 2);
  print(pet.name);
  print(pet.greet());
  final a = Pet('A', 1);
  final b = Pet('A', 1.0);
  print(a == b);
  print(a.hashCode == b.hashCode);
  print({a, b}.length);
}
