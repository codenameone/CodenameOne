import 'package:cross_file/cross_file.dart';

class Note {
  Note({required this.title, required this.latitude, required this.longitude, this.photo})
      : createdAt = DateTime.now();

  final String title;
  final double latitude;
  final double longitude;
  final XFile? photo;
  final DateTime createdAt;

  String get summary => '$title (${latitude.toStringAsFixed(4)}, ${longitude.toStringAsFixed(4)})';
}
