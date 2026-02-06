/// Networking, Storage, Filesystem & related API's
///
///     The IO package includes all of the main features related to storage and networking with the exception
///     of `SQL` & XML parsing.
///
/// Storage
///
///     `com.codename1.io.Storage` is accessed via the `Storage`
///     class. It is a flat filesystem like interface and contains the ability to list/delete and
///     write to named storage entries.
///
///     The `com.codename1.io.Storage` API also provides convenient methods to write objects to
///     `com.codename1.io.Storage` and read them from `com.codename1.io.Storage` specifically
///     `readObject` & `writeObject`.
///
///     Notice that objects in `com.codename1.io.Storage` are deleted when an app is uninstalled but are
///     retained between application updates.
///
///     The sample code below demonstrates listing the content of the storage, adding/viewing and
///     deleting entries within the storage:
///
/// ```java
/// public void pictureUpload(final Callback resultURL) {
///     String picture = Capture.capturePhoto(1024, -1);
///     if(picture!=null){
///         String filestack = "https://www.filestackapi.com/api/store/S3?key=MY_KEY&filename=myPicture.jpg";
///         MultipartRequest request = new MultipartRequest() {
///            protected void readResponse(InputStream input) throws IOException  {
///               JSONParser jp = new JSONParser();
///               Map result = jp.parseJSON(new InputStreamReader(input, "UTF-8"));
///               String url = (String)result.get("url");
///               if(url == null) {
///                  resultURL.onError(null, null, 1, result.toString());
///                  return;
///               }
///               resultURL.onSucess(url);
///            }
///         };
///         request.setUrl(filestack);
///         try {
///             request.addData("fileUpload", picture, "image/jpeg");
///             request.setFilename("fileUpload", "myPicture.jpg");
///             NetworkManager.getInstance().addToQueue(request);
///         } catch(IOException err) {
///             err.printStackTrace();
///         }
///     }
/// }
/// ```
///
/// The Preferences API
///
///     `com.codename1.io.Storage` also offers a very simple API in the form of the
///     `com.codename1.io.Preferences`
///     class. The `com.codename1.io.Preferences` class allows developers to store simple variables, strings,
///     numbers, booleans etc. in storage without writing any storage code. This is a common use case
///     within applications e.g. you have a server token that you need to store you can store & read it like this:
///
/// ```java
/// // save a token to storage
/// Preferences.set("token", myToken);
///
/// // get the token from storage or null if it isn't there
/// String token = Preferences.get("token", null);
/// ```
///
///     This gets somewhat confusing with primitive numbers e.g. if you use
///     `Preferences.set("primitiveLongValue", myLongNumber)` then invoke
///     `Preferences.get("primitiveLongValue", 0)` you might get an exception!
///
///     This would happen because the value is physically a `Long` object but you are
///     trying to get an `Integer`. The workaround is to remain consistent and use code
///     like this `Preferences.get("primitiveLongValue", (long)0)`.
///
/// File System
///
///     `com.codename1.io.FileSystemStorage` provides file system access. It maps to the underlying
///     OS's file system API providing most of the common operations expected from a file API somewhat in
///     the vain of `java.io.File` & `java.io.FileInputStream` e.g. opening,
///     renaming, deleting etc.
///
///     Notice that the file system API is somewhat platform specific in its behavior. All paths used the API
///     should be absolute otherwise they are not guaranteed to work.
///
///     The main reason `java.io.File` & `java.io.FileInputStream`
///     weren't supported directly has a lot to do with the richness of those two API's. They effectively
///     allow saving a file anywhere, however mobile devices are far more restrictive and don't allow
///     apps to see/modify files that are owned by other apps.
///
/// File Paths & App Home
///
///     All paths in `com.codename1.io.FileSystemStorage` are absolute, this simplifies the issue of portability
///     significantly since the concept of relativity and current working directory aren't very portable.
///
///     All URL's use the `/` as their path separator we try to enforce this behavior even in
///     Windows.
///
///     Directories end with the `/` character and thus can be easily distinguished by their name.
///
///     The `com.codename1.io.FileSystemStorage` API provides a `getRoots()` call to list the root
///     directories of the file system (you can then "dig in" via the `listFiles` API). However,
///     this is confusing and unintuitive for developers.
///
///     To simplify the process of creating/reading files we added the `getAppHomePath()` method.
///     This method allows us to obtain the path to a directory where files can be stored/read.
///
///     We can use this directory to place an image to share as we did in the
///     [share sample](https://www.codenameone.com/manual/components.html#sharebutton-section).
///
///     **Warning:** A common Android hack is to write files to the SDCard storage to share
///     them among apps. Android 4.x disabled the ability to write to arbitrary directories on the SDCard
///     even when the appropriate permission was requested.
///
///     A more advanced usage of the `com.codename1.io.FileSystemStorage` API can be a
///     `com.codename1.io.FileSystemStorage` `Tree`:
///
/// ```java
/// Form hi = new Form("FileSystemTree", new BorderLayout());
/// TreeModel tm = new TreeModel() {
/// @Override
///     public Vector getChildren(Object parent) {
///         String[] files;
///         if(parent == null) {
///             files = FileSystemStorage.getInstance().getRoots();
///             return new Vector(Arrays.asList(files));
///         } else {
///             try {
///                 files = FileSystemStorage.getInstance().listFiles((String)parent);
///             } catch(IOException err) {
///                 Log.e(err);
///                 files = new String[0];
///             }
///         }
///         String p = (String)parent;
///         Vector result = new Vector();
///         for(String s : files) {
///             result.add(p + s);
///         }
///         return result;
///     }
/// @Override
///     public boolean isLeaf(Object node) {
///         return !FileSystemStorage.getInstance().isDirectory((String)node);
///     }
/// };
/// Tree t = new Tree(tm) {
/// @Override
///     protected String childToDisplayLabel(Object child) {
///         String n = (String)child;
///         int pos = n.lastIndexOf("/");
///         if(pos
///
/// Storage vs. File System
///
///     The question of storage vs. file system is often confusing for novice mobile developers. This embeds
///     two separate questions:
///
///
/// -
///
///
/// Why are there 2 API's where one would have worked?
///
///
///
/// -
///
///
/// Which one should I pick?
///
///
///
///     The main reasons for the 2 API's are technical. Many OS's provide 2 ways of accessing data
///     specific to the app and this is reflected within the API. E.g. on Android the
///     `com.codename1.io.FileSystemStorage` maps to API's such as `java.io.FileInputStream`
///     whereas the `com.codename1.io.Storage` maps to `Context.openFileInput()`.
///
///     The secondary reason for the two API's is conceptual. `com.codename1.io.FileSystemStorage` is
///     more powerful and in a sense provides more ways to fail, this is compounded by the complex
///     on-device behavior of the API. `com.codename1.io.Storage` is designed to be friendlier to the
///     uninitiated and more portable.
///
///     You should pick `com.codename1.io.Storage` unless you have a specific requirement that prevents it.
///     Some API's such as `Capture` expect a `com.codename1.io.FileSystemStorage` URI
///     so in those cases this would also be a requirement.
///
///     Another case where `com.codename1.io.FileSystemStorage` is beneficial is the case of hierarchy or
///     native API usage. If you need a a directory structure or need to communicate with a native
///     API the `com.codename1.io.FileSystemStorage` approach is usually easier.
///
///     **Warning: **In some OS's the `com.codename1.io.FileSystemStorage` API can find the
///     content of the `com.codename1.io.Storage` API. As one is implemented on top of the other. This is
///     undocumented behavior that can change at any moment!
///
/// Network Manager & Connection Request
///
///     One of the more common problems in Network programming is spawning a new thread to handle
///     the network operations. In Codename One this is done seamlessly and becomes unessential
///     thanks to the `com.codename1.io.NetworkManager`.
///
///     `com.codename1.io.NetworkManager` effectively alleviates the need for managing network threads by
///     managing the complexity of network threading. The connection request class can be used to
///     facilitate web service requests when coupled with the JSON/XML parsing capabilities.
///
///     To open a connection one needs to use a `com.codename1.io.ConnectionRequest`
///     object, which has some similarities to the networking mechanism in JavaScript but is obviously somewhat more
///     elaborate.
///
/// You can send a get request to a URL using something like:
///
/// ```java
/// ConnectionRequest request = new ConnectionRequest(url, false);
/// request.addResponseListener((e) -> {
///     // process the response
/// });
///
/// // request will be handled asynchronously
/// NetworkManager.getInstance().addToQueue(request);
/// ```
///
///     Notice that you can also implement the same thing and much more by avoiding the response
///     listener code and instead overriding the methods of the `com.codename1.io.ConnectionRequest` class
///     which offers multiple points to override e.g.
///
/// ```java
/// ConnectionRequest request = new ConnectionRequest(url, false) {
///    protected void readResponse(InputStream input) {
///         // just read from the response input stream
///    }
///
///    protected void postResponse() {
///         // invoked on the EDT after processing is complete to allow the networking code
///         // to update the UI
///    }
///
///    protected void buildRequestBody(OutputStream os) {
///         // writes post data, by default this "just works" but if you want to write this
///        // manually then override this
///    }
/// };
/// NetworkManager.getInstance().addToQueue(request);
/// ```
///
///     Notice that overriding `buildRequestBody(OutputStream)` will only work for
///     `POST` requests and will replace writing the arguments.
///
///     **Important:** You don't need to close the output/input streams passed to the
///     request methods. They are implicitly cleaned up.
///
///     `com.codename1.io.NetworkManager` also supports synchronous requests which work in a similar
///     way to `Dialog` via the `invokeAndBlock` call and thus don't block
///     the EDT illegally. E.g. you can do something like this:
///
/// ```java
/// ConnectionRequest request = new ConnectionRequest(url, false);
/// // request will be handled synchronously
/// NetworkManager.getInstance().addToQueueAndWait(request);
/// byte[] resultOfRequest = request.getData();
/// ```
///
///     Notice that in this case the `addToQueueAndWait` method returned after the
///     connection completed. Also notice that this was totally legal to do on the EDT!
///
/// Threading
///
///     By default the `com.codename1.io.NetworkManager` launches with a single network thread. This is
///     sufficient for very simple applications that don't do too much networking but if you need to
///     fetch many images concurrently and perform web services in parallel this might be an issue.
///
///     **Warning:** Once you increase the thread count there is no guarantee of order for your requests.
///     Requests
///     might not execute in the order with which you added them to the queue!
///
/// To update the number of threads use:
///
/// ```java
/// NetworkManager.getInstance().updateThreadCount(4);
/// ```
///
///     All the callbacks in the `ConnectionRequest` occur on the network thread and
///     **not on the EDT**!
///
///     There is one exception to this rule which is the `postResponse()` method designed
///     to update the UI after the networking code completes.
///
///     **Important:** Never change the UI from a `com.codename1.io.ConnectionRequest`
///     callback. You can either use a listener on the `com.codename1.io.ConnectionRequest`, use
///     `postResponse()` (which is the only exception to this rule) or wrap your UI code with
///     `com.codename1.ui.Display#callSerially(java.lang.Runnable)`.
///
/// Arguments, Headers & Methods
///
///     HTTP/S is a complex protocol that expects complex encoded data for its requests. Codename
///     One tries to simplify and abstract most of these complexities behind common sense API's while
///     still providing the full low level access you would expect from such an API.
///
/// Arguments
///
///     HTTP supports several "request methods", most commonly `GET` &
///     `POST` but also a few others such as `HEAD`, `PUT`,
///     `DELETE` etc.
///
/// Arguments in HTTP are passed differently between `GET` and `POST`
///     methods. That is what the `setPost` method in Codename One determines, whether
///     arguments added to the request should be placed using the `GET` semantics or the
///     `POST` semantics.
///
/// So if we continue our example from above we can do something like this:
///
/// ```java
/// ConnectionRequest request = new ConnectionRequest(url, false);
/// request.addArgument("MyArgName", value);
/// ```
///
///     This will implicitly add a get argument with the content of `value`. Notice that we
///     don't really care what value is. It's implicitly HTTP encoded based on the get/post semantics.
///     In this case it will use the get encoding since we passed `false` to the constructor.
///
/// A simpler implementation could do something like this:
///
/// ```java
/// ConnectionRequest request = new ConnectionRequest(url +
///     "MyArgName=" + Util.encodeUrl(value), false);
/// ```
///
///     This would be almost identical but doesn't provide the convenience for switching back and
///     forth between `GET`/`POST` and it isn't as fluent.
///
/// We can skip the encoding in complex cases where server code expects illegal HTTP
///     characters (this happens) using the `addArgumentNoEncoding` method. We can
///     also add multiple arguments with the same key using `addArgumentArray`.
///
/// Methods
///
///     As we explained above, the `setPost()` method allows us to manipulate the
///     get/post semantics of a request. This implicitly changes the `POST`
///     or `GET` method submitted to the server.
///
///     However, if you wish to have finer grained control over the submission process e.g. for making a
///     `HEAD` request you can do this with code like:
///
/// ```java
/// ConnectionRequest request = new ConnectionRequest(url, false);
/// request.setHttpMethod("HEAD");
/// ```
///
/// Headers
///
///     When communicating with HTTP servers we often pass data within headers mostly for
///     authentication/authorization but also to convey various properties.
///
///     Some headers are builtin as direct API's e.g. content type is directly exposed within the API
///     since it's a pretty common use case. We can set the content type of a post request using:
///
/// ```java
/// ConnectionRequest request = new ConnectionRequest(url, true);
/// request.setContentType("text/xml");
/// ```
///
///     We can also add any arbitrary header type we want, e.g. a very common use case is basic
///     authorization where the authorization header includes the Base64 encoded user/password
///     combination as such:
///
/// ```java
/// String authCode = user + ":" + password;
/// String authHeader = "Basic " + Base64.encode(authCode.getBytes());
/// request.addRequestHeader("Authorization", authHeader);
/// ```
///
/// This can be quite tedious to do if you want all requests from your app to use this header.
///     For this use case you can just use:
///
/// ```java
/// String authCode = user + ":" + password;
/// String authHeader = "Basic " + Base64.encode(authCode.getBytes());
/// NetworkManager.getInstance().addDefaultHeader("Authorization", authHeader);
/// ```
///
/// Server Headers
///
///     Server returned headers are a bit trickier to read. We need to subclass the connection request
///     and override the `readHeaders` method e.g.:
///
/// ```java
/// ConnectionRequest request = new ConnectionRequest(url, false) {
///     protected void readHeaders(Object connection) throws IOException {
///         String[] headerNames = getHeaderFieldNames(connection);
///         for(String headerName : headerNames) {
///             String headerValue = getHeader(headerName);
///             //....
///         }
///     }
///     protected void readResponse(InputStream input) {
///         // just read from the response input stream
///     }
/// };
/// NetworkManager.getInstance().addToQueue(request);
/// ```
///
///     Here we can extract the headers one by one to handle complex headers such as cookies,
///     authentication etc.
///
/// Error Handling
///
///     As you noticed above practically all of the methods in the `ConectionRequest`
///     throw `IOException`. This allows you to avoid the `try`/`catch`
///     semantics and just let the error propagate up the chain so it can be handled uniformly by
///     the application.
///
/// There are two distinct placed where you can handle a networking error:
///
///
/// -
///
///
/// The `com.codename1.io.ConnectionRequest` - by overriding callback methods
///
///
///
/// -
///
///
/// The `com.codename1.io.NetworkManager` error handler
///
///
///
///     Notice that the `com.codename1.io.NetworkManager` error handler takes precedence thus allowing
///     you to define a global policy for network error handling by consuming errors.
///
///     E.g. if I would like to block all network errors from showing anything to the user I could do
///     something like this:
///
/// ```java
/// NetworkManager.getInstance().addToQueue(request);
/// NetworkManager.getInstance().addErrorListener((e) -> e.consume());
/// ```
///
///     The error listener is invoked first with the `com.codename1.io.NetworkEvent` matching the
///     error. Consuming the event prevents it from propagating further down the chain into the
///     `com.codename1.io.ConnectionRequest` callbacks.
///
///     We can also override the error callbacks of the various types in the request e.g. in the case of a
///     server error code we can do:
///
/// ```java
/// ConnectionRequest request = new ConnectionRequest(url, false) {
///     protected void handleErrorResponseCode(int code, String message) {
///         if(code == 444) {
///             // do something
///         }
///     }
///     protected void readResponse(InputStream input) {
///         // just read from the response input stream
///     }
/// };
/// NetworkManager.getInstance().addToQueue(request);
/// ```
///
///     **Important:** The error callback callback is triggered in the network thread!
///
///     As a result it can't access the UI to show a `Dialog` or anything like that.
///
///     Another approach is to use the `setFailSilently(true)` method on the
///     `com.codename1.io.ConnectionRequest`. This will prevent the
///     `com.codename1.io.ConnectionRequest` from displaying any errors to the user. It's a very
///     powerful strategy if you use the synchronous version of the API's e.g.:
///
/// ```java
/// ConnectionRequest request = new ConnectionRequest(url, false);
/// request.setFailSilently(true);
/// NetworkManager.getInstance().addToQueueAndWait(request);
/// if(request.getResponseCode() != 200) {
///     // probably an error...
/// }
/// ```
///
///     This code will only work with the synchronous "AndWait" version of the method since the response
///     code will take a while to return for the non-wait version.
///
/// Error Stream
///
///     When we get an error code that isn't 200/300 we ignore the result. This is problematic as the
///     result might contain information we need. E.g. many webservices provide further XML/JSON
///     based details describing the reason for the error code.
///
///     Calling `setReadResponseForErrors(true)` will trigger a mode where even errors
///     will receive the `readResponse` callback with the error stream. This also means
///     that API's like `getData` and the listener API's will also work correctly in
///     case of error.
///
/// GZIP
///
///     Gzip is a very common compression format based on the lz algorithm, it's used by web servers
///     around the world to compress data.
///
///     Codename One supports `com.codename1.io.gzip.GZIPInputStream` and
///     `com.codename1.io.gzip.GZIPOutputStream`, which allow you to compress data
///     seamlessly into a stream and extract compressed data from a stream. This is very useful and
///     can be applied to every arbitrary stream.
///
///     Codename One also features a `com.codename1.io.gzip.GZConnectionRequest`, which
///     will automatically unzip an HTTP response if it is indeed gzipped. Notice that some devices (iOS)
///     always request gzip'ed data and always decompress it for us, however in the case of iOS it
///     doesn't remove the gziped header. The `GZConnectionRequest` is aware of such
///     behaviors so its better to use that when connecting to the network (if applicable).
///
///     By default `GZConnectionRequest` doesn't request gzipped data (only unzips it
///     when its received) but its pretty easy to do so just add the HTTP header
///     `Accept-Encoding: gzip` e.g.:
///
/// ```java
/// GZConnectionRequest con = new GZConnectionRequest();
/// con.addRequestHeader("Accept-Encoding", "gzip");
/// ```
///
/// Do the rest as usual and you should have smaller responses from the servers.
///
/// File Upload
///
///     `com.codename1.io.MultipartRequest` tries to simplify the process of uploading a file from
///     the local device to a remote server.
///
///     You can always submit data in the `buildRequestBody` but this is flaky and has
///     some limitations in terms of devices/size allowed. HTTP standardized file upload capabilities
///     thru the multipart request protocol, this is implemented by countless servers and is well
///     documented. Codename One supports this out of the box.
///
///     Since we assume most developers reading this will be familiar with Java here is the way to
///     implement the multipart upload in the servlet API.
///
/// ```java
/// // File: MultipartClientSample.java
/// MultipartRequest request = new MultipartRequest();
/// request.setUrl(url);
/// request.addData("myFileName", fullPathToFile, "text/plain")
/// NetworkManager.getInstance().addToQueue(request);
/// ```
///
/// ```java
/// // File: UploadServlet.java
/// @WebServlet(name = "UploadServlet", urlPatterns = {"/upload"})
/// @MultipartConfig(fileSizeThreshold = 1024 * 1024 * 100, // 10 MB
///         maxFileSize = 1024 * 1024 * 150, // 50 MB
///         maxRequestSize = 1024 * 1024 * 200)      // 100 MB
/// public class UploadServlet extends HttpServlet {
/// @Override
///     public void doPost(HttpServletRequest req, HttpServletResponse res)
///             throws ServletException, IOException {
///         Collection parts = req.getParts();
///         Part data = parts.iterator().next();
///         try(InputStream is = data.getInputStream()) {}
///             // store or do something with the input stream
///         }
///     }
/// }
/// ```
///
///     `com.codename1.io.MultipartRequest` is a `com.codename1.io.ConnectionRequest`
///     most stuff you expect from there should work. Even addArgument etc.
///
/// Parsing
///
/// Codename One has several built in parsers for JSON, XML, CSV & Properties formats. You can
///     use those parsers to read data from the Internet or data that is shipping with your product. E.g. use the
///     CSV data to setup default values for your application.
///
///     All our parsers are designed with simplicity and small distribution size; they don't validate and will fail
///     in odd ways when faced with broken data. The main logic behind this is that validation takes up CPU
///     time on the device where CPU is a precious resource.
///
/// Parsing CSV
///
///     CSV is probably the easiest to use, the "Comma Separated Values" format is just a list of values
///     separated by commas (or some other character) with new lines to indicate another row in the table.
///     These usually map well to an Excel spreadsheet or database table and are supported by default in all
///     spreadsheets.
///
///     To parse a CSV just use the
///     [CSVParser](https://www.codenameone.com/javadoc/com/codename1/io/CSVParser.html) class as such:
///
/// ```java
/// Form hi = new Form("CSV Parsing", new BorderLayout());
/// CSVParser parser = new CSVParser();
/// try(Reader r = new com.codename1.io.CharArrayReader("1997,Ford,E350,\"Super, \"\"luxurious\"\" truck\"".toCharArray())) {
///     String[][] data = parser.parse(r);
///     String[] columnNames = new String[data[0].length];
///     for(int iter=  0 ; iter
///
///     The data contains a two dimensional array of the CSV content. You can change the delimiter character
///     by using the `CSVParser` constructor that accepts a character.
///
///     **IMPORTANT:** Notice that we used `com.codename1.io.CharArrayReader` for
///     this sample. Normally you would want to use `java.util.InputStreamReader` for real world data.
///
/// JSON
///
///     The JSON ("Java Script Object Notation") format is popular on the web for passing values to/from
///     webservices since it works so well with JavaScript. Parsing JSON is just as easy but has two
///     different variations. You can use the
///     `com.codename1.io.JSONParser` class to build a tree of the JSON data as such:
///
/// ```java
/// JSONParser parser = new JSONParser();
/// Hashtable response = parser.parse(reader);
/// ```
///
///     The response is a `Map` containing a nested hierarchy of `Collection` (`java.util.List`),
///     Strings and numbers to represent the content of the submitted JSON. To extract the data from a specific
///     path just iterate the `Map` keys and recurs into it.
///
///     The sample below uses results from [an API of ice and fire](https://anapioficeandfire.com/)
///     that queries structured data about the "Song Of Ice & Fire" book series. Here is a sample result
///     returned from the API for the query
///     [http://www.anapioficeandfire.com/api/characters?page=5&pageSize=3](http://www.anapioficeandfire.com/api/characters?page=5&pageSize=3):
///
/// ```java
/// [
///   {
///     "url": "http://www.anapioficeandfire.com/api/characters/13",
///     "name": "Chayle",
///     "culture": "",
///     "born": "",
///     "died": "In 299 AC, at Winterfell",
///     "titles": [
///       "Septon"
///     ],
///     "aliases": [],
///     "father": "",
///     "mother": "",
///     "spouse": "",
///     "allegiances": [],
///     "books": [
///       "http://www.anapioficeandfire.com/api/books/1",
///       "http://www.anapioficeandfire.com/api/books/2",
///       "http://www.anapioficeandfire.com/api/books/3"
///     ],
///     "povBooks": [],
///     "tvSeries": [],
///     "playedBy": []
///   },
///   {
///     "url": "http://www.anapioficeandfire.com/api/characters/14",
///     "name": "Gillam",
///     "culture": "",
///     "born": "",
///     "died": "",
///     "titles": [
///       "Brother"
///     ],
///     "aliases": [],
///     "father": "",
///     "mother": "",
///     "spouse": "",
///     "allegiances": [],
///     "books": [
///       "http://www.anapioficeandfire.com/api/books/5"
///     ],
///     "povBooks": [],
///     "tvSeries": [],
///     "playedBy": []
///   },
///   {
///     "url": "http://www.anapioficeandfire.com/api/characters/15",
///     "name": "High Septon",
///     "culture": "",
///     "born": "",
///     "died": "",
///     "titles": [
///       "High Septon",
///       "His High Holiness",
///       "Father of the Faithful",
///       "Voice of the Seven on Earth"
///     ],
///     "aliases": [
///       "The High Sparrow"
///     ],
///     "father": "",
///     "mother": "",
///     "spouse": "",
///     "allegiances": [],
///     "books": [
///       "http://www.anapioficeandfire.com/api/books/5",
///       "http://www.anapioficeandfire.com/api/books/8"
///     ],
///     "povBooks": [],
///     "tvSeries": [
///       "Season 5"
///     ],
///     "playedBy": [
///       "Jonathan Pryce"
///     ]
///   }
/// ]
/// ```
///
///     We will place that into a file named "anapioficeandfire.json" in the src directory to make the next
///     sample simpler:
///
/// ```java
/// Form hi = new Form("JSON Parsing", new BoxLayout(BoxLayout.Y_AXIS));
/// JSONParser json = new JSONParser();
/// try(Reader r = new InputStreamReader(Display.getInstance().getResourceAsStream(getClass(), "/anapioficeandfire.json"), "UTF-8")) {
///     Map data = json.parseJSON(r);
///     java.util.List> content = (java.util.List>)data.get("root"); //
///     for(Map obj : content) { //
///         String url = (String)obj.get("url");
///         String name = (String)obj.get("name");
///         java.util.List titles =  (java.util.List)obj.get("titles"); //
///         if(name == null || name.length() == 0) {
///             java.util.List aliases = (java.util.List)obj.get("aliases");
///             if(aliases != null && aliases.size() > 0) {
///                 name = aliases.get(0);
///             }
///         }
///         MultiButton mb = new MultiButton(name);
///         if(titles != null && titles.size() > 0) {
///             mb.setTextLine2(titles.get(0));
///         }
///         mb.addActionListener((e) -> Display.getInstance().execute(url));
///         hi.add(mb);
///     }
/// } catch(IOException err) {
///     Log.e(err);
/// }
/// hi.show();
/// ```
///
///
/// - The `JSONParser` returns a `Map` which is great if the root object is a `Map`
///         but in some cases its a list of elements (as is the case above). In this case a special case `"root"`
///         element is created to contain the actual list of elements.
///
///
/// - We rely that the entries are all maps, this might not be the case for every API type.
///
/// - Notice that the "titles" and "aliases" entries are both lists of elements. We use `java.util.List`
///         to avoid a clash with `com.codename1.ui.List`.
///
///
///     **Tip:** The structure of the returned map is sometimes unintuitive when looking at the raw JSON. The easiest
///     thing to do is set a breakpoint on the method and use the inspect variables capability of your IDE to
///     inspect the returned element hierarchy while writing the code to extract that data
///
///     An alternative approach is to use the static data parse() method of the `JSONParser` class and
///     implement a callback parser e.g.: `JSONParser.parse(reader, callback);`
///
///     Notice that a static version of the method is used! The callback object is an instance of the
///     `JSONParseCallback` interface, which includes multiple methods. These methods are invoked
///     by the parser to indicate internal parser states, this is similar to the way traditional XML SAX event
///     parsers work.
///
/// XML Parsing
///
///     The `com.codename1.xml.XMLParser` started its life as an HTML parser built for displaying
///     mobile HTML. That usage has since been deprecated but the parser can still parse many HTML
///     pages and is very "loose" in terms of verification. This is both good and bad as the parser will work
///     with invalid data without complaining.
///
///     The simplest usage of `com.codename1.xml.XMLParser` looks a bit like this:
///
/// ```java
/// XMLParser parser = new XMLParser();
/// Element elem = parser.parse(reader);
/// ```
///
///     The `com.codename1.xml.Element` contains children and attributes. It represents a tag within the
///     XML document and even the root document itself. You can iterate over the XML tree to extract the
///     data from within the XML file.
///
///     We have a great sample of working with `XMLParser` in the
///     `com.codename1.ui.tree.Tree` class.
///
/// XPath Processing
///
///     Codename One ships with support to a subset of XPath processing, you can read more about it in
///     the `processing package docs`.
///
/// Externalizable Objects
///
///     Codename One provides the `com.codename1.io.Externalizable` interface, which is similar
///     to the Java SE `com.codename1.io.Externalizable` interface.
///     This interface allows an object to declare itself as `com.codename1.io.Externalizable` for
///     serialization (so an object can be stored in a file/storage or sent over the network). However, due to the
///     lack of reflection and use of obfuscation these objects must be registered with the
///     `com.codename1.io.Util` class.
///
///     Codename One doesn't support the Java SE Serialization API due to the size issues and
///     complexities related to obfuscation.
///
///     The major objects that are supported by default in the Codename One
///     `com.codename1.io.Externalizable` are:
///     `String`, `Collection`, `Map`, `ArrayList`,
///     `HashMap`, `Vector`, `Hashtable`,
///     `Integer`, `Double`, `Float`, `Byte`,
///     `Short`, `Long`, `Character`, `Boolean`,
///     `Object[]`, `byte[]`, `int[]`, `float[]`,
///     `long[]`, `double[]`.
///
/// Externalizing an object such as h below should work just fine:
///
/// ```java
/// Map h = new HashMap<>();
/// h.put("Hi","World");
/// h.put("data", new byte[] {(byte)1});
/// Storage.getInstance().writeObject("Test", h);
/// ```
///
///     However, notice that some things aren't polymorphic e.g. if we will externalize a
///     `String[]` we will get back an `Object[]` since `String`
///     arrays aren't detected by the implementation.
///
///     **Important:** The externalization process caches objects so the app will seem to
///     work and only fail on restart!
///
///     Implementing the `com.codename1.io.Externalizable` interface is only important when we
///     want to store a proprietary object. In this case we must register the object with the
///     `com.codename1.io.Util` class so the externalization algorithm will be able to
///     recognize it by name by invoking:
///
/// ```java
/// Util.register("MyClass", MyClass.class);
/// ```
///
///     You should do this early on in the app e.g. in the `init(Object)` but you shouldn't do
///     it in a static initializer within the object as that might never be invoked!
///
///     An `com.codename1.io.Externalizable` object **must** have a
///     **default public constructor** and must implement the following 4 methods:
///
/// ```java
/// public int getVersion();
/// public void externalize(DataOutputStream out) throws IOException;
/// public void internalize(int version, DataInputStream in) throws IOException;
/// public String getObjectId();
/// ```
///
///     The `getVersion()` method returns the current version of the object allowing the
///     stored data to change its structure in the future (the version is then passed when internalizing
///     the object). The object id is a `String` uniquely representing the object;
///     it usually corresponds to the class name (in the example above the Unique Name should be
///     `MyClass`).
///
///     **Warning:** It's a common mistake to use `getClass().getName()`
///     to implement `getObjectId()` and it would **seem to work** in the
///     simulator. This isn't the case though!
///
///     Since devices obfuscate the class names this becomes a problem as data is stored in a random
///     name that changes with every release.
///
///     Developers need to write the data of the object in the externalize method using the methods in the
///     data output stream and read the data of the object in the internalize method e.g.:
///
/// ```java
/// public void externalize(DataOutputStream out) throws IOException {
///     out.writeUTF(name);
///     if(value != null) {
///         out.writeBoolean(true);
///         out.writeUTF(value);
///     } else {
///         out.writeBoolean(false);
///     }
///     if(domain != null) {
///         out.writeBoolean(true);
///         out.writeUTF(domain);
///     } else {
///         out.writeBoolean(false);
///     }
///     out.writeLong(expires);
/// }
///
/// public void internalize(int version, DataInputStream in) throws IOException {
///     name = in.readUTF();
///     if(in.readBoolean()) {
///         value = in.readUTF();
///     }
///     if(in.readBoolean()) {
///         domain = in.readUTF();
///     }
///     expires = in.readLong();
/// }
/// ```
///
///     Since strings might be null sometimes we also included convenience methods to implement such
///     externalization. This effectively writes a boolean before writing the UTF to indicate whether the string
///     is null:
///
/// ```java
/// public void externalize(DataOutputStream out) throws IOException {
///     Util.writeUTF(name, out);
///     Util.writeUTF(value, out);
///     Util.writeUTF(domain, out);
///     out.writeLong(expires);
/// }
///
/// public void internalize(int version, DataInputStream in) throws IOException {
///     name = Util.readUTF(in);
///     value = Util.readUTF(in);
///     domain = Util.readUTF(in);
///     expires = in.readLong();
/// }
/// ```
///
///     Assuming we added a new date field to the object we can do the following. Notice that a
///     `Date` is really a `long` value in Java that can be null.
///     For completeness the full class is presented below:
///
/// ```java
/// public class MyClass implements Externalizable {
///     private static final int VERSION = 2;
///     private String name;
///     private String value;
///     private String domain;
///     private Date date;
///     private long expires;
///
///     public MyClass() {}
///
///     public int getVersion() {
///         return VERSION;
///     }
///
///     public String getObjectId() {
///         return "MyClass";
///     }
///
///     public void externalize(DataOutputStream out) throws IOException {
///         Util.writeUTF(name, out);
///         Util.writeUTF(value, out);
///         Util.writeUTF(domain, out);
///         if(date != null) {
///             out.writeBoolean(true);
///             out.writeLong(date.getTime());
///         } else {
///             out.writeBoolean(false);
///         }
///         out.writeLong(expires);
///     }
///
///     public void internalize(int version, DataInputStream in) throws IOException {
///         name = Util.readUTF(in);
///         value = Util.readUTF(in);
///         domain = Util.readUTF(in);
///         if(version > 1) {
///             boolean hasDate = in.readBoolean();
///             if(hasDate) {
///                 date = new Date(in.readLong());
///             }
///         }
///         expires = in.readLong();
///     }
/// }
/// ```
///
/// Notice that we only need to check for compatibility during the reading process as the writing
///     process always writes the latest version of the data.
package com.codename1.io;
