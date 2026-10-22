import 'package:equatable/equatable.dart';
import 'package:json_annotation/json_annotation.dart';

part 'pet.g.dart';

enum Species { dog, cat, rabbit }

@JsonSerializable()
class Pet extends Equatable {
  final int id;
  final String name;
  final Species species;
  final int ageMonths;
  final String? photoUrl;
  final DateTime listedAt;

  const Pet({
    required this.id,
    required this.name,
    required this.species,
    required this.ageMonths,
    this.photoUrl,
    required this.listedAt,
  });

  factory Pet.fromJson(Map<String, dynamic> json) => _$PetFromJson(json);

  Map<String, dynamic> toJson() => _$PetToJson(this);

  String get ageLabel => ageMonths < 12 ? '$ageMonths mo' : '${ageMonths ~/ 12} yr';

  @override
  List<Object?> get props => [id, name, species, ageMonths, photoUrl, listedAt];
}
