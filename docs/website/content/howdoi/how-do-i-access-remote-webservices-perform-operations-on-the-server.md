---
title: ACCESS REMOTE WEBSERVICES? PERFORM OPERATIONS ON THE SERVER?
slug: how-do-i-access-remote-webservices-perform-operations-on-the-server
url: /how-do-i/how-do-i-access-remote-webservices-perform-operations-on-the-server/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-access-remote-webservices-perform-operations-on-the-server.html
tags:
- io
description: Invoke server functionality from the client side using the Codename One
  webservice wizard
youtube_id: sUhpCwd0YJg
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-23.jpg
---
{{< youtube "sUhpCwd0YJg" >}} 

Client/server work in Codename One usually starts with a simple question: do you want a portable client talking to a server over standard HTTP APIs, or do you want generated RPC-style plumbing that hides some of that protocol detail? The old webservice wizard was designed for the second case. It generated client-side proxies and matching server-side scaffolding so you could define methods and call them almost as if they were local functions.

That approach is still useful to understand conceptually, but it is no longer the default direction most modern projects should start with. Today, most teams are better served by ordinary REST-style HTTP APIs, `ConnectionRequest`, and the higher-level REST helpers described in the developer guide. Those approaches fit better with current backend tooling, current deployment habits, and Maven-era Codename One projects.

What the video still teaches well is the separation of concerns. The client side should know how to invoke a remote capability. The server side should contain the real business logic. Generated proxies can reduce boilerplate, but they do not remove the need to think carefully about API evolution, error handling, and backward compatibility.

One lesson from the older wizard flow is still especially important: remote APIs are contracts. Once an app is in production, you cannot casually change method signatures or response shapes and expect every installed client to keep working. If a service needs to evolve incompatibly, version it or add new endpoints instead of silently mutating the old ones.

The sync-versus-async distinction also matters regardless of whether you use the old wizard or a modern REST API. A synchronous call is simpler to read, but it blocks the current flow and demands careful error handling. An asynchronous call is usually the healthier default in mobile UI work because it keeps the application responsive and makes network latency explicit in the code.

For a current Codename One project, the practical advice is to design a normal server API first, then call it from the client using the networking tools that best match the service. If the service is HTTP-based, prefer the modern REST-oriented client approach. If you are maintaining an older project that already uses the wizard-generated proxies, treat them as an existing contract layer and evolve them carefully rather than rewriting method signatures casually.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Use HTTP, Sockets, Webservices And Websockets](/how-do-i/how-do-i-use-http-sockets-webservices-websockets/)
- [Terse REST API](/blog/terse-rest-api/)
- [REST API Design](/courses/course-03-build-real-world-full-stack-mobile-apps-java/rest-api-design/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
