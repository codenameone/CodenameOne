// equatable value equality and the code json_serializable generates, as an app
// commits it: `part` file, $enumDecode, `(json['x'] as num).toInt()`, DateTime
// round trips, and the `int.parse` tear-off.
import 'dart:convert';

import 'package:equatable/equatable.dart';
import 'package:json_annotation/json_annotation.dart';

part 'pet.g.dart';

enum Species { dog, cat }

@JsonSerializable()
class Pet extends Equatable {
  const Pet({required this.id, required this.name, required this.species, required this.tags, required this.born});

  final int id;
  final String name;
  final Species species;
  final List<String> tags;
  final DateTime born;

  factory Pet.fromJson(Map<String, dynamic> json) => _$PetFromJson(json);
  Map<String, dynamic> toJson() => _$PetToJson(this);

  @override
  List<Object?> get props => [id, name, species, tags, born];
}

void main() {
  final json = jsonDecode('{"id": 7, "name": "Rex", "species": "dog", "tags": ["a", "b"], '
      '"born": "2024-03-05T10:20:30.000"}') as Map<String, dynamic>;
  final a = Pet.fromJson(json);
  final b = Pet.fromJson(jsonDecode(jsonEncode(a.toJson())) as Map<String, dynamic>);
  print('equal ${a == b} hash ${a.hashCode == b.hashCode}');
  print('identical ${identical(a, b)}');
  print(a);
  print(a.toJson());
  final c = Pet(id: 7, name: 'Rex', species: Species.cat, tags: const ['a', 'b'], born: a.born);
  print('differs ${a == c}');
  print({a, b, c}.length);
  print(['1', '2', '30'].map(int.parse).toList());
  try {
    $enumDecode(_$SpeciesEnumMap, 'fish');
  } on ArgumentError catch (e) {
    print('bad enum: ${e.message}');
  }
}
