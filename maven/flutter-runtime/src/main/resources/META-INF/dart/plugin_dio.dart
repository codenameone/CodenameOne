// package:dio, over com.codename1.flutter.plugins.dio and Codename One's
// ConnectionRequest: options, interceptors, JSON decoding, FormData uploads,
// cancellation. Not transpiled from pub: dio's IO adapter is dart:io sockets and
// isolates all the way down, and its dependency tree is most of pub's core.

@JavaName('com.codename1.flutter.plugins.dio.ResponseType')
enum ResponseType { json, stream, plain, bytes }

@JavaName('com.codename1.flutter.plugins.dio.DioExceptionType')
enum DioExceptionType {
  connectionTimeout, sendTimeout, receiveTimeout, badCertificate, badResponse, cancel, connectionError, unknown
}

@JavaName('com.codename1.flutter.plugins.dio.OptionsBase')
abstract class _DioOptionsBase {
  external String? get method;
  external set method(String? v);
  external Map<String, dynamic> get headers;
  external set headers(Map<String, dynamic>? v);
  external Map<String, dynamic> get extra;
  external set extra(Map<String, dynamic>? v);
  external String? get contentType;
  external set contentType(String? v);
  external ResponseType? get responseType;
  external set responseType(ResponseType? v);
  external Duration? get sendTimeout;
  external set sendTimeout(Duration? v);
  external Duration? get receiveTimeout;
  external set receiveTimeout(Duration? v);
  external bool? get followRedirects;
  external set followRedirects(bool? v);
}

@JavaName('com.codename1.flutter.plugins.dio.BaseOptions')
class BaseOptions extends _DioOptionsBase {
  external BaseOptions({String? method, String baseUrl, Map<String, dynamic>? queryParameters,
      Duration? connectTimeout, Duration? receiveTimeout, Duration? sendTimeout,
      Map<String, dynamic>? extra, Map<String, dynamic>? headers, ResponseType? responseType,
      String? contentType, bool Function(int? status)? validateStatus, bool? followRedirects});
  external String get baseUrl;
  external set baseUrl(String v);
  external Duration? get connectTimeout;
  external set connectTimeout(Duration? v);
  external Map<String, dynamic> get queryParameters;
  external set queryParameters(Map<String, dynamic>? v);
}

@JavaName('com.codename1.flutter.plugins.dio.Options')
class Options extends _DioOptionsBase {
  external Options({String? method, Duration? sendTimeout, Duration? receiveTimeout,
      Map<String, dynamic>? extra, Map<String, dynamic>? headers, ResponseType? responseType,
      String? contentType, bool Function(int? status)? validateStatus, bool? followRedirects});
}

@JavaName('com.codename1.flutter.plugins.dio.RequestOptions')
class RequestOptions extends _DioOptionsBase {
  external RequestOptions({String path, String? method, String? baseUrl, Object? data,
      Map<String, dynamic>? queryParameters, Map<String, dynamic>? headers, Map<String, dynamic>? extra,
      String? contentType, ResponseType? responseType});
  external String get path;
  external set path(String v);
  external String get baseUrl;
  external set baseUrl(String v);
  external Map<String, dynamic> get queryParameters;
  external set queryParameters(Map<String, dynamic>? v);
  external dynamic get data;
  external set data(dynamic v);
  external Duration? get connectTimeout;
  external set connectTimeout(Duration? v);
  external CancelToken? get cancelToken;
  external Uri get uri;
}

@JavaName('com.codename1.flutter.plugins.dio.Headers')
class Headers {
  external static String get contentTypeHeader;
  external static String get jsonContentType;
  external static String get formUrlEncodedContentType;
  external static String get textPlainContentType;
  external Headers();
  external Map<String, List<String>> get map;
  external List<String>? operator [](String name);
  external String? value(String name);
  external void add(String name, String value);
  external void set(String name, dynamic value);
  external void removeAll(String name);
  external void forEach(void Function(String name, List<String> values) f);
}

@JavaName('com.codename1.flutter.plugins.dio.ResponseBody')
class ResponseBody {
  external Stream<Uint8List> get stream;
  external int? get statusCode;
  external int get contentLength;
}

@JavaName('com.codename1.flutter.plugins.dio.Response')
class Response<T> {
  external Response({required RequestOptions requestOptions, T? data, int? statusCode,
      String? statusMessage, Headers? headers});
  external T? get data;
  external set data(T? v);
  external int? get statusCode;
  external set statusCode(int? v);
  external String? get statusMessage;
  external Headers get headers;
  external RequestOptions get requestOptions;
  external Map<String, dynamic> get extra;
  external Uri get realUri;
}

@JavaName('com.codename1.flutter.plugins.dio.DioException')
class DioException implements Exception {
  external DioException({required RequestOptions requestOptions, Response? response,
      DioExceptionType type, Object? error, String? message, StackTrace? stackTrace});
  external RequestOptions get requestOptions;
  external Response? get response;
  external DioExceptionType get type;
  external Object? get error;
  external String? get message;
}

// The pre-5.0 name, still common in app code; the same class, so `on DioError`
// catches what dio throws.
@JavaName('com.codename1.flutter.plugins.dio.DioException')
class DioError extends DioException {
  external DioError({required RequestOptions requestOptions, Response? response,
      DioExceptionType type, Object? error, String? message});
}

@JavaName('com.codename1.flutter.plugins.dio.CancelToken')
class CancelToken {
  external CancelToken();
  external bool get isCancelled;
  external static bool isCancel(DioException e);
  external void cancel([Object? reason]);
}

@JavaName('com.codename1.flutter.plugins.dio.RequestInterceptorHandler')
class RequestInterceptorHandler {
  external void next(RequestOptions requestOptions);
  external void resolve(Response response, [bool callFollowingResponseInterceptor = false]);
  external void reject(DioException error, [bool callFollowingErrorInterceptor = false]);
}

@JavaName('com.codename1.flutter.plugins.dio.ResponseInterceptorHandler')
class ResponseInterceptorHandler {
  external void next(Response response);
  external void resolve(Response response);
  external void reject(DioException error, [bool callFollowingErrorInterceptor = false]);
}

@JavaName('com.codename1.flutter.plugins.dio.ErrorInterceptorHandler')
class ErrorInterceptorHandler {
  external void next(DioException error);
  external void resolve(Response response);
  external void reject(DioException error);
}

@JavaName('com.codename1.flutter.plugins.dio.Interceptor')
class Interceptor {
  external Interceptor();
  external void onRequest(RequestOptions options, RequestInterceptorHandler handler);
  external void onResponse(Response response, ResponseInterceptorHandler handler);
  external void onError(DioException err, ErrorInterceptorHandler handler);
}

@JavaName('com.codename1.flutter.plugins.dio.InterceptorsWrapper')
class InterceptorsWrapper extends Interceptor {
  external InterceptorsWrapper({
      void Function(RequestOptions options, RequestInterceptorHandler handler)? onRequest,
      void Function(Response response, ResponseInterceptorHandler handler)? onResponse,
      void Function(DioException error, ErrorInterceptorHandler handler)? onError});
}

@JavaName('com.codename1.flutter.plugins.dio.LogInterceptor')
class LogInterceptor extends Interceptor {
  external LogInterceptor({bool request, bool requestHeader, bool requestBody, bool responseHeader,
      bool responseBody, bool error, void Function(Object object)? logPrint});
}

@JavaName('com.codename1.flutter.plugins.dio.Interceptors')
class Interceptors {
  external void add(Interceptor interceptor);
  external void addAll(Iterable<Interceptor> interceptors);
  external void clear();
  external bool remove(Object? interceptor);
  external int get length;
}

@JavaName('com.codename1.flutter.plugins.dio.MultipartFile')
class MultipartFile {
  external static Future<MultipartFile> fromFile(String filePath, {String? filename, Object? contentType, Object? headers});
  external static MultipartFile fromFileSync(String filePath, {String? filename, Object? contentType, Object? headers});
  external static MultipartFile fromBytes(List<int> value, {String? filename, Object? contentType, Object? headers});
  external static MultipartFile fromString(String value, {String? filename, Object? contentType, Object? headers});
  external String? get filename;
  external int get length;
}

@JavaName('com.codename1.flutter.plugins.dio.FormData')
class FormData {
  external FormData();
  external static FormData fromMap(Map<String, dynamic> map, [Object? collectionFormat, bool camelCaseContentDisposition = false]);
  external int get length;
  external List<MapEntry<String, String>> get fields;
  external List<MapEntry<String, MultipartFile>> get files;
}

@JavaName('com.codename1.flutter.plugins.dio.Dio')
class Dio {
  external Dio([BaseOptions? options]);
  external BaseOptions get options;
  external set options(BaseOptions v);
  external Interceptors get interceptors;
  external void close({bool force});
  external Future<Response<T>> get<T>(String path, {Object? data, Map<String, dynamic>? queryParameters,
      Options? options, CancelToken? cancelToken, void Function(int count, int total)? onReceiveProgress});
  external Future<Response<T>> post<T>(String path, {Object? data, Map<String, dynamic>? queryParameters,
      Options? options, CancelToken? cancelToken, void Function(int count, int total)? onSendProgress, void Function(int count, int total)? onReceiveProgress});
  external Future<Response<T>> put<T>(String path, {Object? data, Map<String, dynamic>? queryParameters,
      Options? options, CancelToken? cancelToken, void Function(int count, int total)? onSendProgress, void Function(int count, int total)? onReceiveProgress});
  external Future<Response<T>> patch<T>(String path, {Object? data, Map<String, dynamic>? queryParameters,
      Options? options, CancelToken? cancelToken, void Function(int count, int total)? onSendProgress, void Function(int count, int total)? onReceiveProgress});
  external Future<Response<T>> delete<T>(String path, {Object? data, Map<String, dynamic>? queryParameters,
      Options? options, CancelToken? cancelToken});
  external Future<Response<T>> head<T>(String path, {Object? data, Map<String, dynamic>? queryParameters,
      Options? options, CancelToken? cancelToken});
  external Future<Response<T>> request<T>(String path, {Object? data, Map<String, dynamic>? queryParameters,
      CancelToken? cancelToken, Options? options, void Function(int count, int total)? onSendProgress, void Function(int count, int total)? onReceiveProgress});
  external Future<Response<dynamic>> download(String urlPath, dynamic savePath, {void Function(int count, int total)? onReceiveProgress,
      Map<String, dynamic>? queryParameters, CancelToken? cancelToken, bool? deleteOnError,
      Object? fileAccessMode, String? lengthHeader, Object? data, Options? options});
  external Future<Response<T>> fetch<T>(RequestOptions requestOptions);
}
