// Codename One Dart runtime API stubs -- dart:io.
//
// Backed by dart.io (maven/dart-runtime):
//  - File/Directory over FileSystemStorage. Dart's plain paths map to its
//    file:// URLs. Before Codename One starts, an in-memory file system stands
//    in, which is what the transpiler's own tests run against.
//  - Platform from Display.getPlatformName(), so the simulator answers for
//    the skin it shows.
//  - HttpClient over ConnectionRequest; the body of a response arrives as one
//    chunk of its stream.
//  - WebSocket over com.codename1.io.WebSocket.
// Same conventions as dart_async_stream.dart: named parameters of methods are
// passed in their declared order, instance getters are no-arg method calls.
// The asynchronous file methods do their I/O before returning a completed
// future; a failure still arrives through the future.

@JavaName('dart.io.IOException')
class IOException {
  external String get message;
}

@JavaName('dart.io.FileSystemException')
class FileSystemException extends IOException {
  external FileSystemException([String message = "", String? path]);
  external String? get path;
}

@JavaName('dart.io.SocketException')
class SocketException extends IOException {
  external SocketException(String message);
}

@JavaName('dart.io.HttpException')
class HttpException extends IOException {
  external HttpException(String message, {Uri? uri});
  external Uri? get uri;
}

@JavaName('dart.io.FileMode')
class FileMode {
  external static FileMode get read;
  external static FileMode get write;
  external static FileMode get append;
  external static FileMode get writeOnly;
  external static FileMode get writeOnlyAppend;
}

@JavaName('dart.io.FileSystemEntity')
abstract class FileSystemEntity {
  external String get path;
  external Uri get uri;
  external bool existsSync();
  external Future<bool> exists();
  external Directory get parent;
  external bool get isAbsolute;
  external void deleteSync({bool recursive = false});
  external Future<FileSystemEntity> delete({bool recursive = false});
  external static bool isDirectorySync(String path);
  external static Future<bool> isDirectory(String path);
  external static bool isFileSync(String path);
  external static Future<bool> isFile(String path);
}

@JavaName('dart.io.File')
class File extends FileSystemEntity {
  external File(String path);
  external static File fromUri(Uri uri);
  external File get absolute;
  external Uint8List readAsBytesSync();
  external Future<Uint8List> readAsBytes();
  external String readAsStringSync({Object? encoding});
  external Future<String> readAsString({Object? encoding});
  external List<String> readAsLinesSync({Object? encoding});
  external Future<List<String>> readAsLines({Object? encoding});
  external Stream<List<int>> openRead([int? start, int? end]);
  external void writeAsBytesSync(List<int> bytes, {FileMode mode = FileMode.write, bool flush = false});
  external Future<File> writeAsBytes(List<int> bytes, {FileMode mode = FileMode.write, bool flush = false});
  external void writeAsStringSync(String contents,
      {FileMode mode = FileMode.write, Object? encoding, bool flush = false});
  external Future<File> writeAsString(String contents,
      {FileMode mode = FileMode.write, Object? encoding, bool flush = false});
  external void createSync({bool recursive = false, bool exclusive = false});
  external Future<File> create({bool recursive = false, bool exclusive = false});
  external int lengthSync();
  external Future<int> length();
  external DateTime lastModifiedSync();
  external Future<DateTime> lastModified();
  external File copySync(String newPath);
  external Future<File> copy(String newPath);
  external File renameSync(String newPath);
  external Future<File> rename(String newPath);
}

@JavaName('dart.io.Directory')
class Directory extends FileSystemEntity {
  external Directory(String path);
  external static Directory get systemTemp;
  external static Directory get current;
  external static set current(Directory dir);
  external Directory get absolute;
  external void createSync({bool recursive = false});
  external Future<Directory> create({bool recursive = false});
  external List<FileSystemEntity> listSync({bool recursive = false, bool followLinks = true});
  external Stream<FileSystemEntity> list({bool recursive = false, bool followLinks = true});
  external Directory renameSync(String newPath);
  external Future<Directory> rename(String newPath);
  external Directory createTempSync([String? prefix]);
  external Future<Directory> createTemp([String? prefix]);
}

@JavaName('dart.io.Platform')
abstract class Platform {
  external static String get operatingSystem;
  external static String get operatingSystemVersion;
  external static bool get isAndroid;
  external static bool get isIOS;
  external static bool get isMacOS;
  external static bool get isWindows;
  external static bool get isLinux;
  external static bool get isFuchsia;
  external static String get localeName;
  external static int get numberOfProcessors;
  external static String get pathSeparator;
  external static String get localHostname;
  external static String get version;
  external static Map<String, String> get environment;
  external static String get resolvedExecutable;
  external static String get script;
}

@JavaName('dart.io.HttpStatus')
abstract class HttpStatus {
  external static int get continue_;
  external static int get ok;
  external static int get created;
  external static int get accepted;
  external static int get noContent;
  external static int get movedPermanently;
  external static int get found;
  external static int get seeOther;
  external static int get notModified;
  external static int get temporaryRedirect;
  external static int get permanentRedirect;
  external static int get badRequest;
  external static int get unauthorized;
  external static int get paymentRequired;
  external static int get forbidden;
  external static int get notFound;
  external static int get methodNotAllowed;
  external static int get notAcceptable;
  external static int get requestTimeout;
  external static int get conflict;
  external static int get gone;
  external static int get unprocessableEntity;
  external static int get tooManyRequests;
  external static int get internalServerError;
  external static int get notImplemented;
  external static int get badGateway;
  external static int get serviceUnavailable;
  external static int get gatewayTimeout;
}

@JavaName('dart.io.ContentType')
class ContentType {
  external ContentType(String primaryType, String subType, {String? charset, Map<String, String?> parameters = const {}});
  external static ContentType parse(String value);
  external static ContentType get text;
  external static ContentType get html;
  external static ContentType get json;
  external static ContentType get binary;
  external String get primaryType;
  external String get subType;
  external String get mimeType;
  external String? get charset;
  external String get value;
}

@JavaName('dart.io.HttpHeaders')
abstract class HttpHeaders {
  external static String get acceptHeader;
  external static String get acceptEncodingHeader;
  external static String get acceptLanguageHeader;
  external static String get authorizationHeader;
  external static String get cacheControlHeader;
  external static String get contentEncodingHeader;
  external static String get contentLengthHeader;
  external static String get contentTypeHeader;
  external static String get cookieHeader;
  external static String get dateHeader;
  external static String get etagHeader;
  external static String get hostHeader;
  external static String get ifModifiedSinceHeader;
  external static String get ifNoneMatchHeader;
  external static String get lastModifiedHeader;
  external static String get locationHeader;
  external static String get setCookieHeader;
  external static String get userAgentHeader;
  external List<String>? operator [](String name);
  external String? value(String name);
  external void add(String name, Object value, {bool preserveHeaderCase = false});
  external void set(String name, Object value, {bool preserveHeaderCase = false});
  external void remove(String name, Object value);
  external void removeAll(String name);
  external void clear();
  external void forEach(Object action);
  external ContentType? get contentType;
  external set contentType(ContentType? type);
  external int get contentLength;
  external set contentLength(int length);
  external bool get chunkedTransferEncoding;
  external set chunkedTransferEncoding(bool v);
}

@JavaName('dart.io.HttpClient')
class HttpClient {
  external HttpClient({Object? context});
  external Duration? get connectionTimeout;
  external set connectionTimeout(Duration? d);
  external Duration get idleTimeout;
  external set idleTimeout(Duration d);
  external String? get userAgent;
  external set userAgent(String? ua);
  external bool get autoUncompress;
  external set autoUncompress(bool v);
  external set badCertificateCallback(Object? callback);
  external set maxConnectionsPerHost(int? n);
  external set findProxy(Object? f);
  external Future<HttpClientRequest> openUrl(String method, Uri url);
  external Future<HttpClientRequest> getUrl(Uri url);
  external Future<HttpClientRequest> postUrl(Uri url);
  external Future<HttpClientRequest> putUrl(Uri url);
  external Future<HttpClientRequest> patchUrl(Uri url);
  external Future<HttpClientRequest> deleteUrl(Uri url);
  external Future<HttpClientRequest> headUrl(Uri url);
  external Future<HttpClientRequest> open(String method, String host, int port, String path);
  external Future<HttpClientRequest> get(String host, int port, String path);
  external Future<HttpClientRequest> post(String host, int port, String path);
  external void close({bool force = false});
}

@JavaName('dart.io.HttpClientRequest')
abstract class HttpClientRequest {
  external String get method;
  external Uri get uri;
  external HttpHeaders get headers;
  external bool get followRedirects;
  external set followRedirects(bool v);
  external int get maxRedirects;
  external set maxRedirects(int v);
  external int get contentLength;
  external set contentLength(int v);
  external bool get persistentConnection;
  external set persistentConnection(bool v);
  external Object get encoding;
  external set encoding(Object e);
  external void add(List<int> data);
  external void write(Object? object);
  external void writeln([Object? object]);
  external Future addStream(Stream<List<int>> stream);
  external Future flush();
  external void abort([Object? exception, Object? stackTrace]);
  external Future<HttpClientResponse> get done;
  external Future<HttpClientResponse> close();
}

@JavaName('dart.io.HttpClientResponse')
abstract class HttpClientResponse extends Stream<List<int>> {
  external int get statusCode;
  external String get reasonPhrase;
  external HttpHeaders get headers;
  external int get contentLength;
  external bool get isRedirect;
  external bool get persistentConnection;
  external List get redirects;
  external List get cookies;
  external String get compressionState;
}

@JavaName('dart.io.WebSocket')
abstract class WebSocket extends Stream<dynamic> {
  external static int get connecting;
  external static int get open;
  external static int get closing;
  external static int get closed;
  external static Future<WebSocket> connect(String url,
      {Iterable<String>? protocols, Map<String, dynamic>? headers, Object? compression});
  external void add(Object? data);
  external void addUtf8Text(List<int> bytes);
  external Future addStream(Stream stream);
  external Future close([int? code, String? reason]);
  external Future get done;
  external int get readyState;
  external int? get closeCode;
  external String? get closeReason;
  external String? get protocol;
  external String get extensions;
  external set pingInterval(Duration? interval);
}
