import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../blocs/adoption_bloc.dart';
import '../blocs/pets_cubit.dart';

class PetDetailScreen extends StatefulWidget {
  const PetDetailScreen({super.key, required this.petId});

  final int petId;

  @override
  State<PetDetailScreen> createState() => _PetDetailScreenState();
}

class _PetDetailScreenState extends State<PetDetailScreen> {
  final _name = TextEditingController();

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final pets = context.select((PetsCubit c) => c.state.pets);
    final matches = pets.where((p) => p.id == widget.petId);
    if (matches.isEmpty) {
      return Scaffold(appBar: AppBar(), body: const Center(child: Text('Pet not found')));
    }
    final pet = matches.first;
    return BlocProvider(
      create: (_) => AdoptionBloc(),
      child: Scaffold(
        appBar: AppBar(
          title: Text(pet.name),
          leading: BackButton(onPressed: () => context.go('/')),
        ),
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: BlocConsumer<AdoptionBloc, AdoptionState>(
            listener: (context, state) {
              if (state is AdoptionDone) {
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(state.message)));
              }
            },
            builder: (context, state) {
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text('${pet.name} is a ${pet.ageLabel} old ${pet.species.name}.',
                      style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 24),
                  TextField(controller: _name, decoration: const InputDecoration(labelText: 'Your name')),
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: state is AdoptionSubmitting
                        ? null
                        : () => context.read<AdoptionBloc>().add(AdoptionRequested(pet.id, _name.text)),
                    child: Text(state is AdoptionSubmitting ? 'Sending...' : 'Apply to adopt'),
                  ),
                ],
              );
            },
          ),
        ),
      ),
    );
  }
}
