/// SQLite database access API
///
///     Most new devices contain one version of sqlite or another; sqlite is a very lightweight SQL database
///     designed for embedding into devices. For portability we recommend avoiding SQL altogether since
///     it is both fragmented between devices (different sqlite versions) and isn't supported on all devices.
///
/// In general SQL seems overly complex for most embedded device programming tasks.
///
/// Portability Of SQLite
///
/// SQLite is supported on iOS, Android, UWP (Universal Windows Platform), RIM, Desktop & JavaScript builds. However,
///     the JavaScript
///     version of SQL has been deprecated and isn't supported on all platforms.
///
/// You will notice that at this time support is still missing from the Windows builds.
///
/// The biggest issue with SQLite portability is in iOS. The SQLite version for most platforms is
///     threadsafe and as a result very stable. However, the iOS version is not!
///
/// This might not seem like a big deal normally, however if you forget to close a connection the GC might
///     close it for you thus producing a crash. This is such a common occurrence that Codename One logs
///     a warning when the GC collects a database resource on the simulator.
///
/// Using SQLite
///
/// SQL is pretty powerful and very well suited for common tabular data. The Codename One SQL
///     API is similar in spirit to JDBC but considerably simpler since many of the abstractions of JDBC
///     designed for pluggable database architecture make no sense for a local database.
///
/// The `com.codename1.db.Database` API is a high level abstraction that allows you to open an
///     arbitrary database file using syntax such as:
///
/// ```java
/// Database db = Display.getInstance().openOrCreate(“databaseName”);
/// ```
///
/// Some SQLite apps ship with a "ready made" database. We allow you to replace the DB file by using the code:
///
/// ```java
/// String path = Display.getInstance().getDatabasePath(“databaseName”);
/// ```
///
/// You can then use the `com.codename1.io.FileSystemStorage` class to write the content of your
///     DB file into the path. Notice that it must be a valid SQLite file!
///
/// This is very useful for applications that need to synchronize with a central server or applications that
///     ship with a large database as part of their core product.
///
/// Working with a database is pretty trivial, the application logic below can send arbitrary queries to the
///     database and present the results in a `com.codename1.ui.table.Table`. You can probably integrate
///     this code into your app as a debugging tool:
///
/// ```java
/// Toolbar.setGlobalToolbar(true);
/// Style s = UIManager.getInstance().getComponentStyle("TitleCommand");
/// FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_QUERY_BUILDER, s);
/// Form hi = new Form("SQL Explorer", new BorderLayout());
/// hi.getToolbar().addCommandToRightBar("", icon, (e) -> {
///     TextArea query = new TextArea(3, 80);
///     Command ok = new Command("Execute");
///     Command cancel = new Command("Cancel");
///     if(Dialog.show("Query", query, ok, cancel) == ok) {
///         Database db = null;
///         Cursor cur = null;
///         try {
///             db = Display.getInstance().openOrCreate("MyDB.db");
///             if(query.getText().startsWith("select")) {
///                 cur = db.executeQuery(query.getText());
///                 int columns = cur.getColumnCount();
///                 hi.removeAll();
///                 if(columns > 0) {
///                     boolean next = cur.next();
///                     if(next) {
///                         ArrayList data = new ArrayList<>();
///                         String[] columnNames = new String[columns];
///                         for(int iter = 0 ; iter
package com.codename1.db;
