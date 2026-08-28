---
title: "Put App Documents in the System File Browser"
slug: documents-in-system-file-browser
url: /blog/documents-in-system-file-browser/
date: '2026-08-29'
author: Shai Almog
description: "Document Provider publishes a selected read-only tree from a Codename One application into the iOS Files app and Android storage picker, including local and on-demand remote content."
feed_html: '<img src="https://www.codenameone.com/blog/documents-in-system-file-browser.jpg" alt="Selected application documents appearing in a phone file browser" /> Document Provider publishes a selected read-only tree from a Codename One application into the iOS Files app and Android storage picker, including local and on-demand remote content.'
series: ["release-2026-08-28"]
---

![Selected application documents appearing in a phone file browser](/blog/documents-in-system-file-browser.jpg)

An invoice, drawing, report, or project file can live inside an application for years without becoming a document the operating system understands. The user has to open the app before another app can preview or share it.

[PR #5607](https://github.com/codenameone/CodenameOne/pull/5607) adds `com.codename1.documents`. It publishes a selected, read-only document tree as a location in the iOS Files app and Android storage picker. The application decides which nodes exist, what they are called, and whether their bytes are local or fetched from a server.

For native desktop windows and the rest of this release, see the [weekly release overview](/blog/native-desktop-windows/).

## Publish a tree, not a directory permission

The root is a `DocumentNode`. Folder and file nodes underneath it describe the view exposed to the system browser:

```java
String shared = DocumentProvider.getSharedDirectory();
writePdf(shared + "/august-1042.pdf", invoiceBytes);

DocumentNode root = DocumentNode.folder("root", "My Invoices");
DocumentNode august = DocumentNode.folder("2026-08", "August 2026");
august.add(DocumentNode.file("inv-1042", "Invoice 1042.pdf")
        .setContentType("application/pdf")
        .setPath("august-1042.pdf")
        .setSize(invoiceBytes.length)
        .setLastModified(invoiceModified));
root.add(august);

DocumentProvider.publish(root);
```

The published tree does not have to mirror the application's internal storage. An internal object id can become a stable document id. A database title can become the visible filename. A nested folder can be a projection of records that are not folders on disk at all.

The location is read-only. The file browser does not offer rename, delete, or save operations that the application cannot honor. Updating the view means publishing another tree.

Publishing one selected tree is narrower than exposing the whole documents directory. The file browser sees only the nodes in the most recent index. Logging out can withdraw the whole location:

```java
DocumentProvider.clear();
```

If the requirement is simply to show the application's entire iOS documents directory, the existing `UIFileSharingEnabled` and `LSSupportsOpeningDocumentsInPlace` plist keys remain the smaller option. Document Provider is for a curated tree, a virtual layout, or content that does not live on the device yet.

## The iOS extension cannot call the application

Apple runs a File Provider extension in a separate process. The Files app can start it while the Codename One application is not running. The extension cannot call into the Java application to ask what a folder contains.

Codename One therefore uses a publication model. `DocumentProvider.publish()` serializes the tree into an App Group container shared by the application and the generated extension. The extension reads that index and serves the browser independently.

{{< mermaid >}}
sequenceDiagram
    participant App as Codename One app
    participant Group as App Group container
    participant Ext as Generated iOS extension
    participant Files as Files app
    App->>Group: Publish document index and local bytes
    App-->>App: May terminate
    Files->>Ext: Browse or open a document
    Ext->>Group: Read the latest published index
    Group-->>Ext: Metadata and local path or remote endpoint
    Ext-->>Files: Folder listing or document bytes
{{< /mermaid >}}

On Android, the generated `DocumentsProvider` runs inside the application process. It still consumes the same published model. Keeping one model avoids application code that works only because Android happens to have a different process boundary.

Local files must be written under `DocumentProvider.getSharedDirectory()`. The iOS extension cannot see a path under the application's ordinary home directory, even if Java code can.

Do not overwrite bytes in a file that is already published. Another app may be reading it. Write the new version under another name, publish a tree that points to it, and remove the old bytes after existing readers have had time to finish.

## Remote nodes can appear before their bytes arrive

A file node can carry a `remoteId` instead of a local path:

```java
DocumentProvider.setRemoteEndpoint(
        "https://api.example.com/drive", sessionToken);

DocumentNode root = DocumentNode.folder("root", "Example Drive");
root.add(DocumentNode.file("report-52", "Quarterly Report.pdf")
        .setContentType("application/pdf")
        .setRemoteId("server-object-52")
        .setSize(487120)
        .setLastModified(reportModified));
DocumentProvider.publish(root);
```

When the user opens that node, the extension sends:

```http
GET /drive/fetch?id=server-object-52
Authorization: Bearer <published token>
```

The token is the extension's only application state. It cannot borrow an in-memory login session from a process that may not exist. Publish a new endpoint token when the session renews, and clear the provider on logout.

A node can have both a local path and a remote id. The local copy wins, so a cached document opens without a round trip while the same index still describes how to fetch a file not cached on the device.

## iOS signing is part of the feature

Referencing `com.codename1.documents` generates the platform machinery. On iOS, set the explicit hint so the Certificate Wizard and preflight know to prepare extension signing before bytecode scanning occurs:

```properties
codename1.arg.ios.documentProvider.enabled=true
codename1.arg.ios.documentProvider.appGroup=group.com.example.myapp
codename1.arg.ios.documentProvider.displayName=Example Drive
```

The extension has its own bundle id, `<your.package>.CN1Documents`, and needs its own explicit App ID and provisioning profiles. Both the application and extension App IDs must carry the same App Group. The Certificate Wizard creates and installs these assets. A wildcard App ID cannot carry the App Group entitlement.

Android needs no configuration beyond referencing the API. The build declares an authority at `<your package>.documents`.

Mac Catalyst cannot host Apple's FileProvider framework. `DocumentProvider.isSupported()` returns `false` there and in the simulator. Simulator publication still writes the generated index under the application home directory so you can inspect what a device extension would receive.

## A week of narrower capabilities

Document Provider gives the operating system access to a named tree, not to the application sandbox. The rest of this week's work keeps similarly narrow boundaries. Native desktop windows get independent render and input surfaces without changing the phone model. Nearby device features opt in by package. Watch complications publish a timeline to a small system surface rather than starting the whole app.

Making the narrow path the ordinary API reduces security mistakes. A read-only provider, an explicit App Group, and a tree that disappears on logout reduce the amount of platform code an application team has to get right. Rootless jailbreak detection and backend attestation cover different parts of the threat model, but they follow the same rule: expose the signal, define its limit, and avoid pretending that local code grants trust.

The {{< post-link path="/blog/uwb-nearby-devices" text="next post measures the distance and direction to nearby devices" >}}.

---

## Discussion

_Which application data would be more useful as a document the operating system can open directly?_

{{< giscus >}}
