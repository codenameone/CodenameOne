import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../blocs/pets_cubit.dart';
import '../models/pet.dart';

class PetListScreen extends StatelessWidget {
  const PetListScreen({super.key});

  static final _listed = DateFormat.yMMMd();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Adopt a pet'),
        actions: [
          PopupMenuButton<Species?>(
            icon: const Icon(Icons.filter_list),
            onSelected: (s) => context.read<PetsCubit>().setFilter(s),
            itemBuilder: (context) => [
              const PopupMenuItem(value: null, child: Text('All')),
              for (final s in Species.values) PopupMenuItem(value: s, child: Text(s.name)),
            ],
          ),
        ],
      ),
      body: BlocBuilder<PetsCubit, PetsState>(
        builder: (context, state) {
          switch (state.status) {
            case PetsStatus.initial:
            case PetsStatus.loading:
              return const Center(child: CircularProgressIndicator());
            case PetsStatus.failure:
              return Center(
                child: TextButton(
                  onPressed: () => context.read<PetsCubit>().load(),
                  child: const Text('Could not load pets. Retry'),
                ),
              );
            case PetsStatus.loaded:
              final pets = state.visible;
              return RefreshIndicator(
                onRefresh: () => context.read<PetsCubit>().load(),
                child: ListView.separated(
                  itemCount: pets.length,
                  separatorBuilder: (_, __) => const Divider(height: 1),
                  itemBuilder: (context, i) {
                    final pet = pets[i];
                    final favorite = state.favorites.contains(pet.id);
                    return ListTile(
                      leading: CircleAvatar(
                        backgroundImage: pet.photoUrl == null ? null : CachedNetworkImageProvider(pet.photoUrl!),
                        child: pet.photoUrl == null ? Text(pet.name[0]) : null,
                      ),
                      title: Text(pet.name),
                      subtitle: Text('${pet.species.name}, ${pet.ageLabel} - listed ${_listed.format(pet.listedAt)}'),
                      trailing: IconButton(
                        icon: Icon(favorite ? Icons.favorite : Icons.favorite_border),
                        onPressed: () => context.read<PetsCubit>().toggleFavorite(pet.id),
                      ),
                      onTap: () => context.go('/pets/${pet.id}'),
                    );
                  },
                ),
              );
          }
        },
      ),
    );
  }
}
