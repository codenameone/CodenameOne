import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/pet.dart';

class PetRepository {
  PetRepository({Dio? dio})
      : _dio = dio ??
            Dio(BaseOptions(
              baseUrl: 'https://example.com/api',
              connectTimeout: const Duration(seconds: 5),
              receiveTimeout: const Duration(seconds: 5),
            ));

  final Dio _dio;
  static const _favoritesKey = 'favorites';

  Future<List<Pet>> fetchPets() async {
    try {
      final response = await _dio.get<List<dynamic>>('/pets');
      final data = response.data ?? const [];
      return data.map((e) => Pet.fromJson(e as Map<String, dynamic>)).toList();
    } on DioException {
      // Offline: fall back to the bundled sample list.
      final list = jsonDecode(_sample) as List<dynamic>;
      return list.map((e) => Pet.fromJson(e as Map<String, dynamic>)).toList();
    }
  }

  Future<Set<int>> loadFavorites() async {
    final prefs = await SharedPreferences.getInstance();
    final ids = prefs.getStringList(_favoritesKey) ?? const <String>[];
    return ids.map(int.parse).toSet();
  }

  Future<void> saveFavorites(Set<int> ids) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setStringList(_favoritesKey, ids.map((e) => '$e').toList());
  }
}

const _sample = '''
[
  {"id": 1, "name": "Biscuit", "species": "dog", "ageMonths": 8, "photoUrl": null, "listedAt": "2026-09-01T10:00:00.000"},
  {"id": 2, "name": "Miso", "species": "cat", "ageMonths": 30, "photoUrl": null, "listedAt": "2026-08-14T09:30:00.000"},
  {"id": 3, "name": "Clover", "species": "rabbit", "ageMonths": 14, "photoUrl": null, "listedAt": "2026-10-02T16:45:00.000"}
]
''';
