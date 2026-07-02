// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::io-java-001[]
InputStream i = getClass().getResourceAsStream("/myFile");
// end::io-java-001[]

// tag::io-java-002[]
InputStream i = Display.getInstance().getResourceAsStream(getClass(), "/myFile");
// end::io-java-002[]

// tag::io-java-003[]
InputStream i = Display.getInstance().getResourceAsStream(getClass(), "/res/myFile");
// end::io-java-003[]

// tag::io-java-004[]
InputStream i = Display.getInstance().getResourceAsStream(getClass(), "myFile");
// end::io-java-004[]

// tag::io-java-005[]
Toolbar.setGlobalToolbar(true);
Form hi = new Form("Storage", new BoxLayout(BoxLayout.Y_AXIS));
hi.getToolbar().addCommandToRightBar("+", null, e -> {
    TextField tf = new TextField("", "File Name", 20, TextField.ANY);
    TextArea body = new TextArea(5, 20);
    body.setHint("File Body");
    Command ok = new Command("OK");
    Command cancel = new Command("Cancel");
    Command result = Dialog.show("File Name", BorderLayout.north(tf).add(BorderLayout.CENTER, body), ok, cancel);
    if(ok == result) {
        try(OutputStream os = Storage.getInstance().createOutputStream(tf.getText());) {
            os.write(body.getText().getBytes("UTF-8"));
            createFileEntry(hi, tf.getText());
            hi.getContentPane().animateLayout(250);
        } catch(IOException err) {
            Log.e(err);
        }
    }
});
for(String file : Storage.getInstance().listEntries()) {
    createFileEntry(hi, file);
}
hi.show();

private void createFileEntry(Form hi, String file) {
   Label fileField = new Label(file);
   Button delete = new Button();
   Button view = new Button();
   FontImage.setMaterialIcon(delete, FontImage.MATERIAL_DELETE);
   FontImage.setMaterialIcon(view, FontImage.MATERIAL_OPEN_IN_NEW);
   Container content = BorderLayout.center(fileField);
   int size = Storage.getInstance().entrySize(file);
   content.add(BorderLayout.EAST, BoxLayout.encloseX(new Label(size + "bytes"), delete, view));
   delete.addActionListener(e -> {
       Storage.getInstance().deleteStorageFile(file);
       content.setY(hi.getWidth());
       hi.getContentPane().animateUnlayoutAndWait(150, 255);
       hi.removeComponent(content);
       hi.getContentPane().animateLayout(150);
   });
   view.addActionListener(e -> {
       try(InputStream is = Storage.getInstance().createInputStream(file);) {
           String s = Util.readToString(is, "UTF-8");
           Dialog.show(file, s, "OK", null);
       } catch(IOException err) {
           Log.e(err);
       }
   });
   hi.add(content);
}
// end::io-java-005[]

// tag::io-java-006[]
Preferences.set("token", myToken);
// end::io-java-006[]

// tag::io-java-007[]
String token = Preferences.get("token", null);
// end::io-java-007[]

// tag::io-java-008[]
Form hi = new Form("FileSystemTree", new BorderLayout());
TreeModel tm = new TreeModel() {
    @Override
    public Vector getChildren(Object parent) {
        String[] files;
        if(parent == null) {
            files = FileSystemStorage.getInstance().getRoots();
            return new Vector<Object>(Arrays.asList(files));
        } else {
            try {
                files = FileSystemStorage.getInstance().listFiles((String)parent);
            } catch(IOException err) {
                Log.e(err);
                files = new String[0];
            }
        }
        String p = (String)parent;
        Vector result = new Vector();
        for(String s : files) {
            result.add(p + s);
        }
        return result;
    }

    @Override
    public boolean isLeaf(Object node) {
        return !FileSystemStorage.getInstance().isDirectory((String)node);
    }
};
Tree t = new Tree(tm) {
    @Override
    protected String childToDisplayLabel(Object child) {
        String n = (String)child;
        int pos = n.lastIndexOf("/");
        if(pos < 0) {
            return n;
        }
        return n.substring(pos);
    }
};
hi.add(BorderLayout.CENTER, t);
hi.show();
// end::io-java-008[]

// tag::io-java-009[]
Database db = Display.getInstance().openOrCreate("databaseName");
// end::io-java-009[]

// tag::io-java-010[]
String path = Display.getInstance().getDatabasePath("databaseName");
// end::io-java-010[]

// tag::io-java-011[]
Toolbar.setGlobalToolbar(true);
Style s = UIManager.getInstance().getComponentStyle("TitleCommand");
FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_QUERY_BUILDER, s);
Form hi = new Form("SQL Explorer", new BorderLayout());
hi.getToolbar().addCommandToRightBar("", icon, (e) -> {
    TextArea query = new TextArea(3, 80);
    Command ok = new Command("Execute");
    Command cancel = new Command("Cancel");
    if(Dialog.show("Query", query, ok, cancel) == ok) {
        Database db = null;
        Cursor cur = null;
        try {
            db = Display.getInstance().openOrCreate("MyDB.db");
            if(query.getText().startsWith("select")) {
                cur = db.executeQuery(query.getText());
                int columns = cur.getColumnCount();
                hi.removeAll();
                if(columns > 0) {
                    boolean next = cur.next();
                    if(next) {
                        ArrayList<String[]> data = new ArrayList<>();
                        String[] columnNames = new String[columns];
                        for(int iter = 0 ; iter < columns ; iter++) {
                            columnNames[iter] = cur.getColumnName(iter);
                        }
                        while(next) {
                            Row currentRow = cur.getRow();
                            String[] currentRowArray = new String[columns];
                            for(int iter = 0 ; iter < columns ; iter++) {
                                currentRowArray[iter] = currentRow.getString(iter);
                            }
                            data.add(currentRowArray);
                            next = cur.next();
                        }
                        Object[][] arr = new Object[data.size()][];
                        data.toArray(arr);
                        hi.add(BorderLayout.CENTER, new Table(new DefaultTableModel(columnNames, arr)));
                    } else {
                        hi.add(BorderLayout.CENTER, "Query returned no results");
                    }
                } else {
                    hi.add(BorderLayout.CENTER, "Query returned no results");
                }
            } else {
                db.execute(query.getText());
                hi.add(BorderLayout.CENTER, "Query completed successfully");
            }
            hi.revalidate();
        } catch(IOException err) {
            Log.e(err);
            hi.removeAll();
            hi.add(BorderLayout.CENTER, "Error: " + err);
            hi.revalidate();
        } finally {
            Util.cleanup(db);
            Util.cleanup(cur);
        }
    }
});
hi.show();
// end::io-java-011[]

// tag::io-java-012[]
ConnectionRequest request = new ConnectionRequest(url, false);
request.addResponseListener((e) -> {
    // process the response
});

// request will be handled asynchronously
NetworkManager.getInstance().addToQueue(request);
// end::io-java-012[]

// tag::io-java-013[]
ConnectionRequest request = new ConnectionRequest(url, false) {
   protected void readResponse(InputStream input) {
        // just read from the response input stream
   }

   protected void postResponse() {
        // invoked on the EDT after processing is complete to allow the networking code
        // to update the UI
   }

   protected void buildRequestBody(OutputStream os) {
        // writes post data, by default this "just works" but if you want to write this
       // manually then override this
   }
};
NetworkManager.getInstance().addToQueue(request);
// end::io-java-013[]

// tag::io-java-014[]
ConnectionRequest request = new ConnectionRequest(url, false);
// request will be handled synchronously
NetworkManager.getInstance().addToQueueAndWait(request);
byte[] resultOfRequest = request.getData();
// end::io-java-014[]

// tag::io-java-015[]
NetworkManager.getInstance().updateThreadCount(4);
// end::io-java-015[]

// tag::io-java-016[]
NetworkManager nm = NetworkManager.getInstance();
if (nm.isVPNDetectionSupported()) {
    boolean vpnActive = nm.isVPNActive();
}
// end::io-java-016[]

// tag::io-java-017[]
ConnectionRequest request = new ConnectionRequest(url, false);
request.addArgument("MyArgName", value);
// end::io-java-017[]

// tag::io-java-018[]
ConnectionRequest request = new ConnectionRequest(url +
    "MyArgName=" + Util.encodeUrl(value), false);
// end::io-java-018[]

// tag::io-java-019[]
ConnectionRequest request = new ConnectionRequest(url, false);
request.setHttpMethod("HEAD");
// end::io-java-019[]

// tag::io-java-020[]
ConnectionRequest request = new ConnectionRequest(url, true);
request.setContentType("text/xml");
// end::io-java-020[]

// tag::io-java-021[]
String authCode = user + ":" + password;
String authHeader = "Basic " + Base64.encode(authCode.getBytes());
request.addRequestHeader("Authorization", authHeader);
// end::io-java-021[]

// tag::io-java-022[]
String authCode = user + ":" + password;
String authHeader = "Basic " + Base64.encode(authCode.getBytes());
NetworkManager.getInstance().addDefaultHeader("Authorization", authHeader);
// end::io-java-022[]

// tag::io-java-023[]
ConnectionRequest request = new ConnectionRequest(url, false) {
    protected void readHeaders(Object connection) throws IOException {
        String[] headerNames = getHeaderFieldNames(connection);
        for(String headerName : headerNames) {
            String headerValue = getHeader(headerName);
            //....
        }
    }
    protected void readResponse(InputStream input) {
        // just read from the response input stream
    }
};
NetworkManager.getInstance().addToQueue(request);
// end::io-java-023[]

// tag::io-java-024[]
NetworkManager.getInstance().addToQueue(request);
NetworkManager.getInstance().addErrorListener((e) -> e.consume());
// end::io-java-024[]

// tag::io-java-025[]
ConnectionRequest request = new ConnectionRequest(url, false) {
    protected void handleErrorResponseCode(int code, String message) {
        if(code == 444) {
            // do something
        }
    }
    protected void readResponse(InputStream input) {
        // just read from the response input stream
    }
};
NetworkManager.getInstance().addToQueue(request);
// end::io-java-025[]

// tag::io-java-026[]
ConnectionRequest request = new ConnectionRequest(url, false);
request.setFailSilently(true);
NetworkManager.getInstance().addToQueueAndWait(request);
if(request.getResponseCode() != 200) {
    // probably an error...
}
// end::io-java-026[]

// tag::io-java-027[]
GZConnectionRequest con = new GZConnectionRequest();
con.addRequestHeader("Accept-Encoding", "gzip");
// end::io-java-027[]

// tag::io-java-028[]
MultipartRequest request = new MultipartRequest();
request.setUrl(url);
request.addData("myFileName", fullPathToFile, "text/plain");
NetworkManager.getInstance().addToQueue(request);
// end::io-java-028[]

// tag::io-java-029[]
@WebServlet(name = "UploadServlet", urlPatterns = {"/upload"})
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 100, // 10 MB
        maxFileSize = 1024 * 1024 * 150, // 50 MB
        maxRequestSize = 1024 * 1024 * 200)      // 100 MB
public class UploadServlet extends HttpServlet {

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        Collection<Part> parts = req.getParts();
        Part data = parts.iterator().next();
        try(InputStream is = data.getInputStream();) {
            // store or do something with the input stream
        }
    }
}
// end::io-java-029[]

// tag::io-java-030[]
Form hi = new Form("CSV Parsing", new BorderLayout());
CSVParser parser = new CSVParser();
try(Reader r = new CharArrayReader("1997,Ford,E350,\"Super, \"\"luxurious\"\" truck\"".toCharArray());) {
    String[][] data = parser.parse(r);
    String[] columnNames = new String[data[0].length];
    for(int iter=  0 ; iter < columnNames.length ; iter++) {
        columnNames[iter] = "Col " + (iter + 1);
    }
    TableModel tm = new DefaultTableModel(columnNames, data);
    hi.add(BorderLayout.CENTER, new Table(tm));
} catch(IOException err) {
    Log.e(err);
}
hi.show();
// end::io-java-030[]

// tag::io-java-031[]
JSONParser parser = new JSONParser();
Hashtable response = parser.parse(reader);
// end::io-java-031[]

// tag::io-java-032[]
Form hi = new Form("JSON Parsing", new BoxLayout(BoxLayout.Y_AXIS));
JSONParser json = new JSONParser();
try(Reader r = new InputStreamReader(Display.getInstance().getResourceAsStream(getClass(), "/anapioficeandfire.json"), "UTF-8");) {
    Map<String, Object> data = json.parseJSON(r);
    java.util.List<Map<String, Object>> content = (java.util.List<Map<String, Object>>)data.get("root"); // <1>
    for(Map<String, Object> obj : content) { // <2>
        String url = (String)obj.get("url");
        String name = (String)obj.get("name");
        java.util.List<String> titles =  (java.util.List<String>)obj.get("titles"); // <3>
        if(name == null || name.length() == 0) {
            java.util.List<String> aliases = (java.util.List<String>)obj.get("aliases");
            if(aliases != null && aliases.size() > 0) {
                name = aliases.get(0);
            }
        }
        MultiButton mb = new MultiButton(name);
        if(titles != null && titles.size() > 0) {
            mb.setTextLine2(titles.get(0));
        }
        mb.addActionListener((e) -> Display.getInstance().execute(url));
        hi.add(mb);
    }
} catch(IOException err) {
    Log.e(err);
}
hi.show();
// end::io-java-032[]

// tag::io-java-033[]
// One-shot encoding of any Map / List / String / Number / Boolean / null tree
String json = JSONWriter.toJson(Map.of("name", "ada", "values", List.of(1, 2, 3)));

// Streaming variants for large outputs (UTF-8)
JSONWriter.toJson(value, writer);
JSONWriter.toJson(value, outputStream);
// end::io-java-033[]

// tag::io-java-034[]
String body = JSONWriter.object()
        .put("email", email)
        .put("password", password)
        .toJson();

String coords = JSONWriter.array()
        .add(JSONWriter.object().put("lat", 37.7749).put("lng", -122.4194))
        .add(JSONWriter.object().put("lat", 51.5074).put("lng",   -0.1278))
        .toJson();
// end::io-java-034[]

// tag::io-java-035[]
XMLParser parser = new XMLParser();
Element elem = parser.parse(reader);
// end::io-java-035[]

// tag::io-java-036[]
Result result = Result.fromContent(input, Result.XML);
String country = result.getAsString("/result/address_component[type='country']/long_name");
String region = result.getAsString("/result/address_component[type='administrative_area_level_1']/long_name");
String city = result.getAsString("/result/address_component[type='locality']/long_name");
// end::io-java-036[]

// tag::io-java-037[]
Form hi = new Form("Location", new BoxLayout(BoxLayout.Y_AXIS));
hi.add("Pinpointing Location");
Display.getInstance().callSerially(() -> {
    Location l = Display.getInstance().getLocationManager().getCurrentLocationSync();
    ConnectionRequest request = new ConnectionRequest("http://maps.googleapis.com/maps/api/geocode/json", false) {
        private String country;
        private String region;
        private String city;
        private String json;

        @Override
        protected void readResponse(InputStream input) throws IOException {
                Result result = Result.fromContent(input, Result.JSON);
                country = result.getAsString("/results/address_components[types='country']/long_name");
                region = result.getAsString("/results/address_components[types='administrative_area_level_1']/long_name");
                city = result.getAsString("/results/address_components[types='locality']/long_name");
                json = result.toString();
        }

        @Override
        protected void postResponse() {
            hi.removeAll();
            hi.add(country);
            hi.add(region);
            hi.add(city);
            hi.add(new SpanLabel(json));
            hi.revalidate();
        }
    };
    request.setContentType("application/json");
    request.addRequestHeader("Accept", "application/json");
    request.addArgument("sensor", "true");
    request.addArgument("latlng", l.getLatitude() + "," + l.getLongitude());

    NetworkManager.getInstance().addToQueue(request);
});
hi.show();
[source,java]
// end::io-java-037[]

// tag::io-java-038[]
// get all address_component names anywhere in the document with a type "political"
String array[] = result.getAsStringArray("//address_component[type='political']/long_name");

// get all types anywhere under the second result (dimension is 0-based)
String array[] = result.getAsStringArray("/result[1]//type");
// end::io-java-038[]

// tag::io-java-039[]
int top2[] = result.getAsIntegerArray("//player[@rank < 3]/@id");
// end::io-java-039[]

// tag::io-java-040[]
String first2[] = result.getAsStringArray("//player[position() < 3]/firstname");

String secondLast = result.getAsString("//player[last() - 1]/firstName");
// end::io-java-040[]

// tag::io-java-041[]
int id = result.getAsInteger("//lastname[text()='Hewitt']/../@id");
// end::io-java-041[]

// tag::io-java-042[]
int id = result.getAsInteger("//player[lastname='Hewitt']/@id");
// end::io-java-042[]

// tag::io-java-043[]
String id=result.getAsInteger("//player[//address[country/isocode='CA']]/@id");
// end::io-java-043[]

// tag::io-java-044[]
int id[] = result.getAsIntegerArray("//player[@rank]/@id");
// end::io-java-044[]

// tag::io-java-045[]
int id[] = result.getAsIntegerArray("//player[@rank=null]/@id");
// end::io-java-045[]

// tag::io-java-046[]
int id[] = result.getAsIntegerArray("//player[middlename]/@id");
// end::io-java-046[]

// tag::io-java-047[]
request.downloadImageToStorage(url, (img) -> theImageIsHereDoSomethingWithIt(img));
// end::io-java-047[]

// tag::io-java-048[]
public static Image createCachedImage(String imageName, String url, Image placeholder, int resizeRule);
// end::io-java-048[]

// tag::io-java-049[]
Map<String, Object> jsonData = Rest.get(myUrl).getAsJsonMap().getResponseData();
// end::io-java-049[]

// tag::io-java-050[]
Map<String, Object> jsonData = Rest.get(myUrl).acceptJson().getAsJsonMap().getResponseData();
// end::io-java-050[]

// tag::io-java-051[]
Map<String, Object> jsonData = Rest.post(myUrl).body(bodyValueAsString).getAsJsonMap().getResponseData();
// end::io-java-051[]

// tag::io-java-052[]
Rest.get("https://api.example.com/items")
    .header("Authorization", "Bearer " + token)
    .acceptJson()
    .fetchAsJsonList(response -> {
        List items = response.getResponseData();
        renderItems(items);
    });
// end::io-java-052[]

// tag::io-java-053[]
// Model
@Mapped
public class Pet {
    @JsonProperty("id")        public long id;
    @JsonProperty("name")      public String name;
    @JsonProperty("photoUrls") public List<String> photoUrls;
}

// Call site
Rest.get(baseUrl + "/pet/" + petId)
    .header("Authorization", "Bearer " + token)
    .acceptJson()
    .fetchAsMapped(Pet.class, response -> {
        Pet pet = response.getResponseData();    // already typed -- no Map casts
        renderPet(pet);
    });
// end::io-java-053[]

// tag::io-java-054[]
Rest.get(baseUrl + "/albums")
    .header("Authorization", "Bearer " + token)
    .acceptJson()
    .fetchAsMappedList(Album.class, response -> {
        List<Album> albums = response.getResponseData();
        renderAlbums(albums);
    });
// end::io-java-054[]

// tag::io-java-055[]
String accountSID = "----------------";
String authToken = "---------------";
String fromPhone = "your Twilio phone number here";
// end::io-java-055[]

// tag::io-java-056[]
Response<Map> result = Rest.post("https://api.twilio.com/2010-04-01/Accounts/" + accountSID + "/Messages.json").
        queryParam("To", destinationPhone).
        queryParam("From", fromPhone).
        queryParam("Body", "Hello World").
        basicAuth(accountSID, authToken).
        getAsJsonMap();
// end::io-java-056[]

// tag::io-java-057[]
if(result.getResponseData() != null) {
    String error = (String)result.getResponseData().get("error_message");
    if(error != null) {
        ToastBar.showErrorMessage(error);
    }
} else {
    ToastBar.showErrorMessage("Error sending SMS: " + result.getResponseCode());
}
// end::io-java-057[]

// tag::io-java-058[]
public class GameOfThronesServiceServer {
    public static String[] getBookNames() {
        // your code goes here...
        return null;
    }

    public static String[] getBookPovCharacters(String bookName) {
        // your code goes here...
        return null;
    }
}
// end::io-java-058[]

// tag::io-java-059[]
public class GameOfThronesServiceServer {
    public static String[] getBookNames() {
        return new String[] {
            "A Game of Thrones", "A Clash Of Kings", "A Storm Of Swords", "A Feast For Crows",
            "A Dance With Dragons", "The Winds of Winter", "A Dream of Spring"
        };
    }

    public static String[] getBookPovCharacters(String bookName) {
        // your code goes here...
        return null;
    }
}
// end::io-java-059[]

// tag::io-java-060[]
public class GameOfThronesService {
    private static final String DESTINATION_URL = "http://localhost:8080/cn1proxy";

//...
}
// end::io-java-060[]

// tag::io-java-061[]
private static final String DESTINATION_URL = "http://localhost:8080/HelloWebServiceWizard/cn1proxy";
// end::io-java-061[]

// tag::io-java-062[]
Form hi = new Form("WebService Wizard", new BoxLayout(BoxLayout.Y_AXIS));
Button getNamesSync = new Button("Get Names - Sync");
Button getNamesASync = new Button("Get Names - ASync");
hi.add(getNamesSync).add(getNamesASync);

getNamesSync.addActionListener((e) -> {
    try {
        String[] books = GameOfThronesService.getBookNames();
        hi.add("--- SYNC");
        for(String b : books) {
            hi.add(b);
        }
        hi.revalidate();
    } catch(IOException err) {
        Log.e(err);
    }
});

getNamesASync.addActionListener((e) -> {
    GameOfThronesService.getBookNamesAsync(new Callback<String[]>() {
        @Override
        public void onSucess(String[] value) {
            hi.add("--- ASYNC");
            for(String b : value) {
                hi.add(b);
            }
            hi.revalidate();
        }

        @Override
        public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
            Log.e(err);
        }
    });
});
// end::io-java-062[]

// tag::io-java-063[]
protected InputStream getCachedData() throws IOException;
protected void cacheUnmodified() throws IOException;
public void purgeCache();
public static void purgeCacheDirectory() throws IOException;
// end::io-java-063[]

// tag::io-java-064[]
CachedDataService.register();
CachedData d = (CachedData)Storage.getInstance().readObject("LocallyCachedData");

if(d == null) {
  d = new CachedData();
  d.setUrl("http://....");
}
// check if there is a new version of this on the server
CachedDataService.updateData(d, new ActionListener() {
    public void actionPerformed(ActionEvent ev) {
        // invoked when/if the data arrives, we now have a fresh cache
        Storage.getInstance().writeObject("LocallyCachedData", d);
    }
});
// end::io-java-064[]

// tag::io-java-065[]
Map<String, Object> h = new HashMap<>();
h.put("Hi","World");
h.put("data", new byte[] {(byte)1});
Storage.getInstance().writeObject("Test", h);
// end::io-java-065[]

// tag::io-java-066[]
Util.register("MyClass", MyClass.class);
// end::io-java-066[]

// tag::io-java-067[]
public int getVersion();
public void externalize(DataOutputStream out) throws IOException;
public void internalize(int version, DataInputStream in) throws IOException;
public String getObjectId();
// end::io-java-067[]

// tag::io-java-068[]
public void externalize(DataOutputStream out) throws IOException {
    out.writeUTF(name);
    if(value != null) {
        out.writeBoolean(true);
        out.writeUTF(value);
    } else {
        out.writeBoolean(false);
    }
    if(domain != null) {
        out.writeBoolean(true);
        out.writeUTF(domain);
    } else {
        out.writeBoolean(false);
    }
    out.writeLong(expires);
}

public void internalize(int version, DataInputStream in) throws IOException {
    name = in.readUTF();
    if(in.readBoolean()) {
        value = in.readUTF();
    }
    if(in.readBoolean()) {
        domain = in.readUTF();
    }
    expires = in.readLong();
}
// end::io-java-068[]

// tag::io-java-069[]
public void externalize(DataOutputStream out) throws IOException {
    Util.writeUTF(name, out);
    Util.writeUTF(value, out);
    Util.writeUTF(domain, out);
    out.writeLong(expires);
}

public void internalize(int version, DataInputStream in) throws IOException {
    name = Util.readUTF(in);
    value = Util.readUTF(in);
    domain = Util.readUTF(in);
    expires = in.readLong();
}
// end::io-java-069[]

// tag::io-java-070[]
public class MyClass implements Externalizable {
    private static final int VERSION = 2;
    private String name;
    private String value;
    private String domain;
    private Date date;
    private long expires;

    public MyClass() {}

    public int getVersion() {
        return VERSION;
    }

    public String getObjectId() {
        return "MyClass";
    }

    public void externalize(DataOutputStream out) throws IOException {
        Util.writeUTF(name, out);
        Util.writeUTF(value, out);
        Util.writeUTF(domain, out);
        if(date != null) {
            out.writeBoolean(true);
            out.writeLong(date.getTime());
        } else {
            out.writeBoolean(false);
        }
        out.writeLong(expires);
    }

    public void internalize(int version, DataInputStream in) throws IOException {
        name = Util.readUTF(in);
        value = Util.readUTF(in);
        domain = Util.readUTF(in);
        if(version > 1) {
            boolean hasDate = in.readBoolean();
            if(hasDate) {
                date = new Date(in.readLong());
            }
        }
        expires = in.readLong();
    }
}
// end::io-java-070[]

// tag::io-java-071[]
InfiniteProgress ip = new InfiniteProgress();
Dialog dlg = ip.showInifiniteBlocking();
request.setDisposeOnCompletion(dlg);
// end::io-java-071[]

// tag::io-java-072[]
Form hi = new Form("Download Progress", new BorderLayout());
Slider progress = new Slider();
Button download = new Button("Download");
download.addActionListener((e) -> {
    ConnectionRequest cr = new ConnectionRequest("https://www.codenameone.com/img/blog/new_icon.png", false);
    SliderBridge.bindProgress(cr, progress);
    NetworkManager.getInstance().addToQueueAndWait(cr);
    if(cr.getResponseCode() == 200) {
        hi.add(BorderLayout.CENTER, new ScaleImageLabel(EncodedImage.create(cr.getResponseData())));
        hi.revalidate();
    }
});
hi.add(BorderLayout.SOUTH, progress).add(BorderLayout.NORTH, download);
hi.show();
// end::io-java-072[]

// tag::io-java-073[]
Log.setReportingLevel(Log.REPORTING_DEBUG);
DefaultCrashReporter.init(true, 2);
// end::io-java-073[]

// tag::io-java-074[]
public class MyApplication {
    private Form current;

    public void init(Object context) {
        try {
            Resources theme = Resources.openLayered("/theme");
            UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void start() {
        if(current != null){
            current.show();
            return;
        }
        final Form soc = new Form("Socket Test");
        Button btn = new Button("Create Server");
        Button connect = new Button("Connect");
        final TextField host = new TextField("127.0.0.1");
        btn.addActionListener((evt) -> {
            soc.addComponent(new Label("Listening: " + Socket.getIP()));
            soc.revalidate();
            Socket.listen(5557, SocketListenerCallback.class);
        });
        connect.addActionListener((evt) -> {
            Socket.connect(host.getText(), 5557, new SocketConnection() {
                @Override
                public void connectionError(int errorCode, String message) {
                    System.out.println("Error");
                }

                @Override
                public void connectionEstablished(InputStream is, OutputStream os) {
                    try {
                        int counter = 1;
                        while(isConnected()) {
                            os.write(("Hi: " + counter).getBytes());
                            counter++;
                            Thread.sleep(2000);
                        }
                    } catch(Exception err) {
                        err.printStackTrace();
                    }
                }
            });
        });
        soc.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        soc.addComponent(btn);
        soc.addComponent(connect);
        soc.addComponent(host);
        soc.show();
    }

    public static class SocketListenerCallback extends SocketConnection {
        private Label connectionLabel;

        @Override
        public void connectionError(int errorCode, String message) {
            System.out.println("Error");
        }

        private void updateLabel(final String t) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    if(connectionLabel == null) {
                        connectionLabel = new Label(t);
                        Display.getInstance().getCurrent().addComponent(connectionLabel);
                    } else {
                        connectionLabel.setText(t);
                    }
                    Display.getInstance().getCurrent().revalidate();
                }
            });
        }

        @Override
        public void connectionEstablished(InputStream is, OutputStream os) {
            try {
                byte[] buffer = new byte[8192];
                while(isConnected()) {
                    int pending = is.available();
                    if(pending > 0) {
                        int size = is.read(buffer, 0, 8192);
                        if(size == -1) {
                            return;
                        }
                        if(size > 0) {
                            updateLabel(new String(buffer, 0, size));
                        }
                    } else {
                        Thread.sleep(50);
                    }
                }
            } catch(Exception err) {
                err.printStackTrace();
            }
        }
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }

    public void destroy() {
    }
}
// end::io-java-074[]

// tag::io-java-075[]
public class Meeting {
     private Date when;
     private String subject;
     private int attendance;

     public Date getWhen() {
        return when;
    }

     public String getSubject() {
        return subject;
     }

     public int getAttendance() {
        return attendance;
     }

     public void setWhen(Date when) {
        this.when = when;
    }

     public void setSubject(String subject) {
        this.subject  = subject;
     }

     public void setAttendance(int attendance) {
        this.attendance = attendance;
     }
}
// end::io-java-075[]

// tag::io-java-076[]
public class Meeting implements PropertyBusinessObject {
     public final Property<Date,Meeting> when = new Property<>("when");
     public final Property<String,Meeting> subject = new Property<>("subject");
     public final Property<Integer,Meeting>  attendance = new Property<>("attendance");
     private final PropertyIndex idx = new PropertyIndex(this, "Meeting", when, subject, attendance);

    @Override
    public PropertyIndex getPropertyIndex() {
        return idx;
    }
}
// end::io-java-076[]

// tag::io-java-077[]
Meeting meet = new Meeting();
meet.setSubject("My Subject");
Log.p(meet.getSubject());
// end::io-java-077[]

// tag::io-java-078[]
Meeting meet = new Meeting();
meet.subject.set("My Subject");
Log.p(meet.subject.get());
// end::io-java-078[]

// tag::io-java-079[]
public final Property<String,Meeting> subject = new Property<>("subject");
// end::io-java-079[]

// tag::io-java-080[]
meet.subject = otherValue;
// end::io-java-080[]

// tag::io-java-081[]
public final Property<String,Meeting> subject = new Property<>("subject", "") {
     public Meeting set(String value) {
         if(value == null) {
            return Meeting.this;
         }
         return super.set(value);
     }
};
// end::io-java-081[]

// tag::io-java-082[]
meet.subject.addChangeListener((p) -> Log.p("New property value is: " + p.get()));
// end::io-java-082[]

// tag::io-java-083[]
public class Meeting implements PropertyBusinessObject {
     public final Property<Date,Meeting> when = new Property<>("when");
     public final Property<String,Meeting> subject = new Property<>("subject");
     public final Property<Integer,Meeting>  attendance = new Property<>("attendance");
     private final PropertyIndex idx = new PropertyIndex(this, "Meeting", when, subject, attendance);

    @Override
    public PropertyIndex getPropertyIndex() {
        return idx;
    }

    public String toString() {
        return idx.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == getClass() && idx.equals(((TodoTask)obj).getPropertyIndex());
    }

    @Override
    public int hashCode() {
        return idx.hashCode();
    }
}
// end::io-java-083[]

// tag::io-java-084[]
meet.getPropertyIndex().populateFromMap(jsonParsedData);
// end::io-java-084[]

// tag::io-java-085[]
String jsonString = meet.toJSON();
// end::io-java-085[]

// tag::io-java-086[]
Meeting meet = new Meeting().
        subject.set("My Subject").
        when.set(new Date());
// end::io-java-086[]

// tag::io-java-087[]
Meeting meet = new Meeting();
meet.subject.set("My Subject");
meet.when.set(new Date());
// end::io-java-087[]

// tag::io-java-088[]
public class Contact implements PropertyBusinessObject {
    public final IntProperty<Contact> id  = new IntProperty<>("id");
    public final Property<String, Contact> name = new Property<>("name");
    public final Property<String, Contact> email = new Property<>("email");
    public final Property<String, Contact> phone = new Property<>("phone");
    public final Property<Date, Contact> dateOfBirth = new Property<>("dateOfBirth", Date.class);
    public final Property<String, Contact> gender  = new Property<>("gender");
    public final IntProperty<Contact> rank  = new IntProperty<>("rank");
    public final PropertyIndex idx = new PropertyIndex(this, "Contact", id, name, email, phone, dateOfBirth, gender, rank);

    @Override
    public PropertyIndex getPropertyIndex() {
        return idx;
    }

    public Contact() {
        name.setLabel("Name");
        email.setLabel("E-Mail");
        phone.setLabel("Phone");
        dateOfBirth.setLabel("Date Of Birth");
        gender.setLabel("Gender");
        rank.setLabel("Rank");
    }
}
// end::io-java-088[]

// tag::io-java-089[]
new Contact().getPropertyIndex().registerExternalizable();
// end::io-java-089[]

// tag::io-java-090[]
Storage.getInstance().writeObject("MyContact", contact);

Contact readContact = (Contact)Storage.getInstance().readObject("MyContact");
// end::io-java-090[]

// tag::io-java-091[]
private Database db;
private SQLMap sm;
public void init(Object context) {
    theme = UIManager.initFirstTheme("/theme");
    Toolbar.setGlobalToolbar(true);
    Log.bindCrashProtection(true);

    try {
        Contact c = new Contact();
        db = Display.getInstance().openOrCreate("propertiesdemo.db"); // <1>
        sm = SQLMap.create(db); // <2>
        sm.setPrimaryKeyAutoIncrement(c, c.id); // <3>
        sm.createTable(c); // <4>
    } catch(IOException err) {
        Log.e(err);
    }
}
// end::io-java-091[]

// tag::io-java-092[]
sm.insert(myContact);
// end::io-java-092[]

// tag::io-java-093[]
sm.update(myContact);
// end::io-java-093[]

// tag::io-java-094[]
sm.delete(myContact);
// end::io-java-094[]

// tag::io-java-095[]
List<PropertyBusinessObject> contacts = sm.select(c, c.name, true, 1000, 0);

for(PropertyBusinessObject cc : contacts) {
    Contact currentContact = (Contact)cc;

   // ...
}
// end::io-java-095[]

// tag::io-java-096[]
PreferencesObject.create(settingsInstance).bind();
// end::io-java-096[]

// tag::io-java-097[]
PreferencesObject.create(settingsInstance).
    setPrefix("MySettings-").
    setName(settingsInstance.companyName, "company").
    bind();
// end::io-java-097[]

// tag::io-java-098[]
myNameTextField.setText(myNameTextField.getText());
myNameTextField.addActionListener(e -> myContact.name.set(myNameTextField.getText()));
// end::io-java-098[]

// tag::io-java-099[]
UiBinding uib = new UiBinding();
uib.bind(myNameTextField, myContact.name);
// end::io-java-099[]

// tag::io-java-100[]
uib.bind(myRankTextField, myContact.rank);
// end::io-java-100[]

// tag::io-java-101[]
Container resp = new Container(BoxLayout.y());
UiBinding uib = new UiBinding();

TextField nameTf = new TextField();
uib.bind(c.name, nameTf);
resp.add(c.name.getLabel()). // <1>
        add(nameTf);

TextField emailTf = new TextField();
emailTf.setConstraint(TextField.EMAILADDR);
uib.bind(c.email, emailTf);
resp.add(c.email.getLabel()).
        add(emailTf);

TextField phoneTf = new TextField();
phoneTf.setConstraint(TextField.PHONENUMBER);
uib.bind(c.phone, phoneTf);
resp.add(c.phone.getLabel()).
        add(phoneTf);

Picker dateOfBirth = new Picker();
dateOfBirth.setType(Display.PICKER_TYPE_DATE); // <2>
uib.bind(c.dateOfBirth, dateOfBirth);
resp.add(c.dateOfBirth.getLabel()).
        add(dateOfBirth);

ButtonGroup genderGroup = new ButtonGroup();
RadioButton male = RadioButton.createToggle("Male", genderGroup);
RadioButton female = RadioButton.createToggle("Female", genderGroup);
RadioButton undefined = RadioButton.createToggle("Undefined", genderGroup);
uib.bindGroup(c.gender, new String[] {"M", "F", "U"}, male, female, undefined); // <3>
resp.add(c.gender.getLabel()).
        add(GridLayout.encloseIn(3, male, female, undefined));

TextField rankTf = new TextField();
rankTf.setConstraint(TextField.NUMERIC);
uib.bind(c.rank, rankTf); // <4>
resp.add(c.rank.getLabel()).
        add(rankTf);
// end::io-java-101[]

// tag::io-java-102[]
InstantUI iui = new InstantUI();
iui.excludeProperty(myContact.id); // <1>
iui.setMultiChoiceLabels(myContact.gender, "Male", "Female", "Undefined"); // <2>
iui.setMultiChoiceValues(myContact.gender, "M", "F", "U");
Container cnt = iui.createEditUI(myContact, true); // <3>
// end::io-java-102[]

// tag::io-java-103[]
UiBinding.Binding b = iui.getBindings(cnt);
// end::io-java-103[]

// tag::io-java-104[]
emailTf.setConstraint(TextField.EMAILADDR);
// end::io-java-104[]

// tag::io-java-105[]
iui.setTextFieldConstraint(contact.email, TextArea.ANY);
// end::io-java-105[]
