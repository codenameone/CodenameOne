// package:path_provider, over com.codename1.flutter.plugins.pathprovider: the
// app's private home and caches directories from FileSystemStorage.

@JavaName('com.codename1.flutter.plugins.pathprovider.PathProvider.getApplicationDocumentsDirectory')
external Future<Directory> getApplicationDocumentsDirectory();

@JavaName('com.codename1.flutter.plugins.pathprovider.PathProvider.getApplicationSupportDirectory')
external Future<Directory> getApplicationSupportDirectory();

@JavaName('com.codename1.flutter.plugins.pathprovider.PathProvider.getLibraryDirectory')
external Future<Directory> getLibraryDirectory();

@JavaName('com.codename1.flutter.plugins.pathprovider.PathProvider.getTemporaryDirectory')
external Future<Directory> getTemporaryDirectory();

@JavaName('com.codename1.flutter.plugins.pathprovider.PathProvider.getApplicationCacheDirectory')
external Future<Directory> getApplicationCacheDirectory();

@JavaName('com.codename1.flutter.plugins.pathprovider.PathProvider.getDownloadsDirectory')
external Future<Directory?> getDownloadsDirectory();

@JavaName('com.codename1.flutter.plugins.pathprovider.PathProvider.getExternalStorageDirectory')
external Future<Directory?> getExternalStorageDirectory();
