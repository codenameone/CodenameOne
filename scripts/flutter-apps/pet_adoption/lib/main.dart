import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import 'blocs/pets_cubit.dart';
import 'data/pet_repository.dart';
import 'screens/pet_detail_screen.dart';
import 'screens/pet_list_screen.dart';

void main() {
  runApp(PetAdoptionApp(repository: PetRepository()));
}

final _router = GoRouter(
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const PetListScreen(),
      routes: [
        GoRoute(
          path: 'pets/:id',
          builder: (context, state) => PetDetailScreen(petId: int.parse(state.pathParameters['id']!)),
        ),
      ],
    ),
  ],
);

class PetAdoptionApp extends StatelessWidget {
  const PetAdoptionApp({super.key, required this.repository});

  final PetRepository repository;

  @override
  Widget build(BuildContext context) {
    return RepositoryProvider.value(
      value: repository,
      child: BlocProvider(
        create: (_) => PetsCubit(repository)..load(),
        child: MaterialApp.router(
          title: 'Pet adoption',
          theme: ThemeData(colorSchemeSeed: Colors.teal, useMaterial3: true),
          routerConfig: _router,
        ),
      ),
    );
  }
}
