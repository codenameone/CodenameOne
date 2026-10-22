import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../data/pet_repository.dart';
import '../models/pet.dart';

enum PetsStatus { initial, loading, loaded, failure }

class PetsState extends Equatable {
  const PetsState({
    this.status = PetsStatus.initial,
    this.pets = const [],
    this.favorites = const {},
    this.filter,
  });

  final PetsStatus status;
  final List<Pet> pets;
  final Set<int> favorites;
  final Species? filter;

  List<Pet> get visible => filter == null ? pets : pets.where((p) => p.species == filter).toList();

  PetsState copyWith({PetsStatus? status, List<Pet>? pets, Set<int>? favorites, Species? Function()? filter}) {
    return PetsState(
      status: status ?? this.status,
      pets: pets ?? this.pets,
      favorites: favorites ?? this.favorites,
      filter: filter != null ? filter() : this.filter,
    );
  }

  @override
  List<Object?> get props => [status, pets, favorites, filter];
}

class PetsCubit extends Cubit<PetsState> {
  PetsCubit(this._repository) : super(const PetsState());

  final PetRepository _repository;

  Future<void> load() async {
    emit(state.copyWith(status: PetsStatus.loading));
    try {
      final pets = await _repository.fetchPets();
      final favorites = await _repository.loadFavorites();
      emit(state.copyWith(status: PetsStatus.loaded, pets: pets, favorites: favorites));
    } catch (_) {
      emit(state.copyWith(status: PetsStatus.failure));
    }
  }

  void setFilter(Species? species) => emit(state.copyWith(filter: () => species));

  Future<void> toggleFavorite(int id) async {
    final next = Set<int>.of(state.favorites);
    if (!next.remove(id)) {
      next.add(id);
    }
    emit(state.copyWith(favorites: next));
    await _repository.saveFavorites(next);
  }
}
