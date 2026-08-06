// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::io-java-001[]
if (Database.isEncryptionSupported()) {
    DatabaseConfig config = DatabaseConfig.managed();
    Database db = Database.openOrCreate("secure.db", config);
    config.wipe();
}
// end::io-java-001[]

// tag::io-java-002[]
if (!Database.isEncrypted("myapp.db")) {
    Database.encrypt("myapp.db", DatabaseConfig.managed());
}
// end::io-java-002[]

// tag::io-java-003[]
Database db = new ThreadSafeDatabase(Database.openOrCreate("shared.db"));
// end::io-java-003[]
