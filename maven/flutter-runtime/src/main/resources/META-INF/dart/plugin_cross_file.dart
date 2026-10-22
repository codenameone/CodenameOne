// package:cross_file, the XFile that image_picker and share_plus hand around:
// a path, or bytes held in memory, over FileSystemStorage.

@JavaName('com.codename1.flutter.plugins.crossfile.XFile')
class XFile {
  external XFile(String path, {String? mimeType, String? name});
  external static XFile fromData(Uint8List bytes, {String? mimeType, String? name, int? length,
      DateTime? lastModified, String? path});
  external String get path;
  external String get name;
  external String? get mimeType;
  external Future<Uint8List> readAsBytes();
  external Future<String> readAsString();
  external Future<int> length();
  external Future<void> saveTo(String path);
}
