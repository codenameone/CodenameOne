import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

sealed class AdoptionEvent extends Equatable {
  const AdoptionEvent();
  @override
  List<Object?> get props => [];
}

final class AdoptionRequested extends AdoptionEvent {
  const AdoptionRequested(this.petId, this.applicant);
  final int petId;
  final String applicant;
  @override
  List<Object?> get props => [petId, applicant];
}

final class AdoptionReset extends AdoptionEvent {
  const AdoptionReset();
}

sealed class AdoptionState extends Equatable {
  const AdoptionState();
  @override
  List<Object?> get props => [];
}

final class AdoptionIdle extends AdoptionState {
  const AdoptionIdle();
}

final class AdoptionSubmitting extends AdoptionState {
  const AdoptionSubmitting();
}

final class AdoptionDone extends AdoptionState {
  const AdoptionDone(this.message);
  final String message;
  @override
  List<Object?> get props => [message];
}

class AdoptionBloc extends Bloc<AdoptionEvent, AdoptionState> {
  AdoptionBloc() : super(const AdoptionIdle()) {
    on<AdoptionRequested>(_onRequested);
    on<AdoptionReset>((event, emit) => emit(const AdoptionIdle()));
  }

  Future<void> _onRequested(AdoptionRequested event, Emitter<AdoptionState> emit) async {
    emit(const AdoptionSubmitting());
    await Future<void>.delayed(const Duration(milliseconds: 300));
    emit(AdoptionDone('Thanks ${event.applicant}! We will be in touch about pet #${event.petId}.'));
  }
}
