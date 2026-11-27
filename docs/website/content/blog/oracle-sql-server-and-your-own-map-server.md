---
title: "Oracle, SQL Server and Your Own Map Server, From One Java Backend"
slug: oracle-sql-server-and-your-own-map-server
url: /blog/oracle-sql-server-and-your-own-map-server/
date: '2026-12-02'
author: Shai Almog
description: "The Codename One backend now talks to Oracle 19c and SQL Server 2017 and newer over their own wire protocols, with no driver, and the client can reach Oracle, SQL Server, PostgreSQL and MySQL directly too. The backend serves OpenStreetMap tiles, routes and geocoding itself, and moves blocking file and SQLite I/O off virtual thread hosts, through `io_uring` on Linux."
feed_html: "<img src=\"https://www.codenameone.com/blog/oracle-sql-server-and-your-own-map-server.jpg\" alt=\"Oracle, SQL Server and your own map server\" /> Oracle and SQL Server on the server and the client with no JDBC driver, OSRM and Nominatim compatible maps served by your own backend, and `io_uring` under virtual threads."
series: ["release-2026-11-27"]
---

![Oracle, SQL Server and your own map server](/blog/oracle-sql-server-and-your-own-map-server.jpg)

Enterprise data lives in Oracle and SQL Server. It has for thirty years and it will for thirty more. If your backend can't reach those databases, your project starts with a migration nobody approved. This week it can, and so can your app.

## Oracle and SQL Server on the backend

The backend already ran on SQLite, PostgreSQL, MySQL, and MariaDB. Now it adds SQL Server 2017 and newer, Azure SQL, and Oracle 19c and newer. You pick the database with a URL:

```properties
cn1.datasource.url=sqlserver://app:secret@db.example.com:1433/inventory
# or
cn1.datasource.url=oracle://app:secret@db.example.com:1521/ORCLPDB1
```

Your entities, repositories and JPQL queries don't change. The ORM maps each type to the right column for each engine (text is `NVARCHAR` on SQL Server) and handles the quirks you'd otherwise learn in production, like Oracle storing an empty string as `NULL`.

There's no JDBC driver underneath. The backend speaks TDS and Oracle's protocol directly, so nothing gets loaded by reflection, and the server compiles ahead of time to a native binary like the rest of it. SQL Server signs in with a SQL login or a `DOMAIN\user` over NTLMv2, and `sslmode=strict` uses TDS 8.0. Oracle connects over TLS on its own port, usually 2484, and the server has to prove it knows your password verifier before the session opens.

Only the engines you name are compiled in. A PostgreSQL app doesn't carry the Oracle client.

{{< mermaid >}}
flowchart LR
    App[Codename One app] -->|REST| B[Codename One backend]
    Admin[Desktop admin tool] -->|wire protocol| DB
    B -->|wire protocol, no driver| DB[(Oracle, SQL Server,<br/>PostgreSQL, MySQL,<br/>MariaDB, SQLite)]
    B --> T[Map tiles, routes,<br/>geocoding]
    App -->|tiles and routes| T
{{< /mermaid >}}

## The same databases, from the client

The database clients used to live only in the backend. Now they're shared with the client, and its `Database` API connects to PostgreSQL, MySQL, MariaDB, SQL Server and Oracle directly:

```java {class="cn1-no-try"}
Database db = new SqlServerDatabase("db.example.com", 1433, "inventory", "app", password);
Cursor c = db.executeQuery("SELECT id, name FROM item WHERE price < ?", 10.0);
while (c.next()) {
    Row r = c.getRow();
    Log.p(r.getLong(0) + " " + r.getString(1));
}
c.close();
db.close();
```

That's the API you already use for SQLite on the device, so your query code moves between local and remote without a rewrite. It opens a category of apps that used to need a native SDK per platform: a database admin tool for the desktop, a reporting app for the operations team, a migration tool that reads one engine and writes another. Calls run off the EDT and parameters use `?` on every engine. The client connects over plain TCP, so point it at a database on your own network or through a tunnel. For an app on strangers' phones, put your backend in between, which is what the backend is for.

## Your own map server

Every maps app hits the same wall. The free OpenStreetMap tile server and the public Nominatim geocoder ask you not to use them for production traffic (Nominatim allows one request a second), and the commercial alternatives bill per request. Now the backend serves your maps itself:

```java {class="cn1-no-try"}
OsmExtract town = OsmExtract.load("/data/monaco.osm.pbf");
Backend.builder()
    .handler(new MapTiles("/data/monaco.pmtiles", "/tiles"))
    .handler(new OsmRouting(town, "/osrm"))
    .handler(new OsmGeocoding(town, "/geo"))
    .start();
```

- **`MapTiles`** serves a PMTiles archive, vector or raster, with ETags, gzip passthrough and a TileJSON endpoint. Replace the archive and the next request serves the new one.
- **`OsmRouting`** is OSRM compatible: driving, walking and cycling, up to 25 waypoints, turn by turn steps and polyline geometry.
- **`OsmGeocoding`** is Nominatim compatible search and reverse geocoding, accent and case insensitive, with towns ranked first.

Because the endpoints speak OSRM and Nominatim, the client classes you already use, `OsrmRouteService`, `NominatimGeocoder` and `MvtTileSource`, work unchanged. Point them at your server and you're done. Cutting an extract for your city is one command:

```bash
pmtiles extract https://build.protomaps.com/20261120.pmtiles monaco.pmtiles \
    --bbox=7.40,43.72,7.44,43.76 --maxzoom=15
```

![Vector map tiles served by the backend](/developer-guide/img/maps-vector.png)

The [backend maps chapter](/developer-guide/#backend-maps) covers attribution, proxies and how to size an extract. For a country or a continent of routing, OSRM or Valhalla is still the tool; for a city, a region or a delivery zone, your backend now does it alone.

## `io_uring` under virtual threads

The backend runs every request on a virtual thread. Network sockets never block a host thread, because the poller parks the virtual thread until data arrives. Files are different. A regular file is always "ready" as far as epoll is concerned, so `read()` on a cold disk, or `sqlite3_step()` on a slow query, blocked the host thread and stalled every virtual thread scheduled on it. One slow report could slow down unrelated logins.

Now blocking local I/O leaves the host. It goes to a small pool of worker threads, and the virtual thread parks on a completion event that the existing poller watches. On Linux, plain opens, reads and writes go through `io_uring` instead, one ring per host. Your handler code doesn't change at all.

{{< mermaid >}}
sequenceDiagram
    participant VT as Virtual thread
    participant Host as Host thread
    participant IO as `io_uring` or I/O pool
    VT->>IO: read file / step SQLite
    VT-->>Host: park
    Host->>Host: run other virtual threads
    IO-->>Host: completion event
    Host->>VT: resume with the result
{{< /mermaid >}}

Where the kernel or a container's seccomp profile refuses `io_uring` (Docker and podman do by default), the backend falls back to the pool and says so once in the log. `CN1_BACKEND_IO_THREADS` sizes the pool, four threads by default. Small read-only queries still run inline, because handing a 10,000 row select to another thread would cost more than it saves.

## JPQL catches up

JPQL gained `CONCAT`, `SUBSTRING`, `LOCATE`, `MOD`, `FLOOR`, `CEILING`, `ROUND`, `SQRT`, `SIZE`, `CURRENT_DATE`, `CURRENT_TIMESTAMP` and `:param IS NULL`, rendered per dialect so SQLite, PostgreSQL and MySQL give the same answers. The backend also learned opt-in OAuth2 dynamic client registration, raw `byte[]` request bodies and a declared `/error` route.

Your app, your backend, your database and your maps, in one language, on infrastructure you control. Tomorrow: {{< post-link path="/blog/agents-that-freeze-time-and-on-device-ai" text="agents that freeze time, and the phone's own AI" >}}.
