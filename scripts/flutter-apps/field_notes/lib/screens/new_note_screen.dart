import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:image_picker/image_picker.dart';

import '../services/note.dart';
import '../services/notifier.dart';

class NewNoteScreen extends StatefulWidget {
  const NewNoteScreen({super.key, this.initialTitle});

  final String? initialTitle;

  @override
  State<NewNoteScreen> createState() => _NewNoteScreenState();
}

class _NewNoteScreenState extends State<NewNoteScreen> {
  late final TextEditingController _title = TextEditingController(text: widget.initialTitle ?? '');
  final _picker = ImagePicker();
  Position? _position;
  XFile? _photo;
  String? _status;

  bool get _dirty => _title.text.isNotEmpty || _photo != null;

  @override
  void initState() {
    super.initState();
    _locate();
  }

  Future<void> _locate() async {
    if (!await Geolocator.isLocationServiceEnabled()) {
      setState(() => _status = 'Location is off');
      return;
    }
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.denied || permission == LocationPermission.deniedForever) {
      setState(() => _status = 'Location permission denied');
      return;
    }
    final position = await Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(accuracy: LocationAccuracy.high),
    );
    setState(() => _position = position);
  }

  Future<void> _takePhoto(ImageSource source) async {
    final photo = await _picker.pickImage(source: source, maxWidth: 1600, imageQuality: 85);
    if (photo != null) {
      setState(() => _photo = photo);
    }
  }

  Future<void> _save() async {
    final position = _position;
    if (_title.text.isEmpty || position == null) {
      return;
    }
    final info = await DeviceInfoPlugin().deviceInfo;
    debugPrint('saved on ${info.data['model'] ?? 'unknown device'}');
    await Notifier.instance.noteSaved(_title.text);
    if (!mounted) {
      return;
    }
    Navigator.of(context).pop(Note(
      title: _title.text,
      latitude: position.latitude,
      longitude: position.longitude,
      photo: _photo,
    ));
  }

  Future<bool> _confirmDiscard() async {
    final discard = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Discard this note?'),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Keep editing')),
          TextButton(onPressed: () => Navigator.of(context).pop(true), child: const Text('Discard')),
        ],
      ),
    );
    return discard ?? false;
  }

  @override
  void dispose() {
    _title.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final position = _position;
    return PopScope<Object?>(
      canPop: !_dirty,
      onPopInvokedWithResult: (didPop, result) async {
        if (didPop) {
          return;
        }
        if (await _confirmDiscard() && context.mounted) {
          Navigator.of(context).pop();
        }
      },
      child: Scaffold(
        appBar: AppBar(title: const Text('New note')),
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            TextField(
              controller: _title,
              decoration: const InputDecoration(labelText: 'What did you see?'),
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: 16),
            Text(position == null
                ? (_status ?? 'Finding your location...')
                : 'At ${position.latitude.toStringAsFixed(5)}, ${position.longitude.toStringAsFixed(5)}'),
            const SizedBox(height: 16),
            Row(
              children: [
                OutlinedButton.icon(
                  onPressed: () => _takePhoto(ImageSource.camera),
                  icon: const Icon(Icons.camera_alt),
                  label: const Text('Camera'),
                ),
                const SizedBox(width: 12),
                OutlinedButton.icon(
                  onPressed: () => _takePhoto(ImageSource.gallery),
                  icon: const Icon(Icons.photo_library),
                  label: const Text('Gallery'),
                ),
              ],
            ),
            if (_photo != null) Text('Photo: ${_photo!.name}'),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: position == null || _title.text.isEmpty ? null : _save,
              child: const Text('Save'),
            ),
          ],
        ),
      ),
    );
  }
}
