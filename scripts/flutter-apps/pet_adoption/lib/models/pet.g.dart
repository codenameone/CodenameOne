// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'pet.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Pet _$PetFromJson(Map<String, dynamic> json) => Pet(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      species: $enumDecode(_$SpeciesEnumMap, json['species']),
      ageMonths: (json['ageMonths'] as num).toInt(),
      photoUrl: json['photoUrl'] as String?,
      listedAt: DateTime.parse(json['listedAt'] as String),
    );

Map<String, dynamic> _$PetToJson(Pet instance) => <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'species': _$SpeciesEnumMap[instance.species]!,
      'ageMonths': instance.ageMonths,
      'photoUrl': instance.photoUrl,
      'listedAt': instance.listedAt.toIso8601String(),
    };

const _$SpeciesEnumMap = {
  Species.dog: 'dog',
  Species.cat: 'cat',
  Species.rabbit: 'rabbit',
};
