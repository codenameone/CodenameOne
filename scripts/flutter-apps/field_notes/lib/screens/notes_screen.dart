import 'dart:async';

import 'package:app_links/app_links.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:share_plus/share_plus.dart';

import '../services/note.dart';
import 'new_note_screen.dart';

class NotesScreen extends StatefulWidget {
  const NotesScreen({super.key, required this.user});

  final User user;

  @override
  State<NotesScreen> createState() => _NotesScreenState();
}

class _NotesScreenState extends State<NotesScreen> {
  final List<Note> _notes = [];
  final _links = AppLinks();
  StreamSubscription<List<ConnectivityResult>>? _connectivity;
  StreamSubscription<Uri>? _linkSub;
  StreamSubscription<RemoteMessage>? _pushSub;
  bool _offline = false;
  bool _showMap = false;
  String _version = '';
  GoogleMapController? _map;

  @override
  void initState() {
    super.initState();
    _connectivity = Connectivity().onConnectivityChanged.listen((results) {
      setState(() => _offline = results.contains(ConnectivityResult.none));
    });
    _linkSub = _links.uriLinkStream.listen(_openLink);
    _pushSub = FirebaseMessaging.onMessage.listen((message) {
      final text = message.notification?.body ?? 'New message';
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
    });
    FirebaseMessaging.instance.requestPermission();
    PackageInfo.fromPlatform().then((info) {
      setState(() => _version = '${info.version}+${info.buildNumber}');
    });
  }

  void _openLink(Uri uri) {
    final title = uri.queryParameters['title'];
    if (title != null) {
      _addNote(context, initialTitle: title);
    }
  }

  @override
  void dispose() {
    _connectivity?.cancel();
    _linkSub?.cancel();
    _pushSub?.cancel();
    _map?.dispose();
    super.dispose();
  }

  Future<void> _addNote(BuildContext context, {String? initialTitle}) async {
    final note = await Navigator.of(context).push<Note>(
      MaterialPageRoute(builder: (_) => NewNoteScreen(initialTitle: initialTitle)),
    );
    if (note != null) {
      setState(() => _notes.insert(0, note));
      await _map?.animateCamera(CameraUpdate.newLatLng(LatLng(note.latitude, note.longitude)));
    }
  }

  Future<void> _share(Note note) async {
    final photo = note.photo;
    if (photo != null) {
      await SharePlus.instance.share(ShareParams(text: note.summary, files: [photo]));
    } else {
      await SharePlus.instance.share(ShareParams(text: note.summary));
    }
  }

  Set<Marker> get _markers => {
        for (final n in _notes)
          Marker(
            markerId: MarkerId(n.createdAt.toIso8601String()),
            position: LatLng(n.latitude, n.longitude),
            infoWindow: InfoWindow(title: n.title),
          ),
      };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.user.email ?? 'Field Notes'),
        actions: [
          IconButton(
            icon: Icon(_showMap ? Icons.list : Icons.map),
            onPressed: () => setState(() => _showMap = !_showMap),
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: () => FirebaseAuth.instance.signOut(),
          ),
        ],
      ),
      body: Column(
        children: [
          if (_offline)
            Container(
              width: double.infinity,
              color: Colors.amber,
              padding: const EdgeInsets.all(8),
              child: const Text('Offline -- notes are kept on this device'),
            ),
          Expanded(
            child: _showMap
                ? GoogleMap(
                    initialCameraPosition: const CameraPosition(target: LatLng(32.08, 34.78), zoom: 11),
                    markers: _markers,
                    onMapCreated: (controller) => _map = controller,
                  )
                : _notes.isEmpty
                    ? const Center(child: Text('No notes yet'))
                    : ListView.builder(
                        itemCount: _notes.length,
                        itemBuilder: (context, i) {
                          final note = _notes[i];
                          return Dismissible(
                            key: ValueKey(note.createdAt),
                            direction: DismissDirection.endToStart,
                            background: Container(color: Colors.red),
                            onDismissed: (_) => setState(() => _notes.removeAt(i)),
                            child: ListTile(
                              leading: Icon(note.photo == null ? Icons.note : Icons.photo),
                              title: Text(note.title),
                              subtitle: Text(note.summary),
                              trailing: IconButton(
                                icon: const Icon(Icons.share),
                                onPressed: () => _share(note),
                              ),
                            ),
                          );
                        },
                      ),
          ),
          if (_version.isNotEmpty)
            Padding(
              padding: const EdgeInsets.all(4),
              child: Text('v$_version', style: Theme.of(context).textTheme.bodySmall),
            ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _addNote(context),
        child: const Icon(Icons.add),
      ),
    );
  }
}
