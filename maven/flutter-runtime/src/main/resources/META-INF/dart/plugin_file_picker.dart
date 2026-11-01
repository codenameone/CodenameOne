// package:file_picker, over Display.showOpenDialog / showSaveDialog. A cancelled
// dialog, or no display, answers null.

@JavaName('com.codename1.flutter.plugins.filepicker.FileType')
enum FileType { any, media, image, video, audio, custom }

@JavaName('com.codename1.flutter.plugins.filepicker.PlatformFile')
class PlatformFile {
  external PlatformFile({String? path, required String name, required int size, Uint8List? bytes,
      Object? readStream, String? identifier});
  external String? get path;
  external String get name;
  external int get size;
  external Uint8List? get bytes;
  external String? get identifier;
  external String? get extension;
}

@JavaName('com.codename1.flutter.plugins.filepicker.FilePickerResult')
class FilePickerResult {
  external FilePickerResult(List<PlatformFile> files);
  external List<PlatformFile> get files;
  external int get count;
  external bool get isSinglePick;
  external List<String?> get paths;
  external List<String> get names;
}

@JavaName('com.codename1.flutter.plugins.filepicker.FilePicker')
class FilePicker {
  external static FilePicker get platform;
  external static set platform(FilePicker instance);
  external Future<FilePickerResult?> pickFiles({String? dialogTitle, String? initialDirectory,
      FileType type, List<String>? allowedExtensions, Function? onFileLoading, bool allowCompression,
      int compressionQuality, bool allowMultiple, bool withData, bool withReadStream,
      bool lockParentWindow, bool readSequential});
  external Future<String?> getDirectoryPath({String? dialogTitle, bool lockParentWindow,
      String? initialDirectory});
  external Future<String?> saveFile({String? dialogTitle, String? fileName, String? initialDirectory,
      FileType type, List<String>? allowedExtensions, Uint8List? bytes, bool lockParentWindow});
  external Future<bool?> clearTemporaryFiles();
}
