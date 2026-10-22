// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'main.dart';

Pet _$PetFromJson(Map<String, dynamic> json) => Pet(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      species: $enumDecode(_$SpeciesEnumMap, json['species']),
      tags: (json['tags'] as List<dynamic>).map((e) => e as String).toList(),
      born: DateTime.parse(json['born'] as String),
    );

Map<String, dynamic> _$PetToJson(Pet instance) => <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'species': _$SpeciesEnumMap[instance.species]!,
      'tags': instance.tags,
      'born': instance.born.toIso8601String(),
    };

const _$SpeciesEnumMap = {
  Species.dog: 'dog',
  Species.cat: 'cat',
};
