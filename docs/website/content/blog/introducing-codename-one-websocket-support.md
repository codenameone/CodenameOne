---
title: Introducing Codename One WebSocket Support
slug: introducing-codename-one-websocket-support
url: /blog/introducing-codename-one-websocket-support/
original_url: https://www.codenameone.com/blog/introducing-codename-one-websocket-support.html
aliases:
- /blog/introducing-codename-one-websocket-support.html
date: '2015-08-09'
author: Steve Hannah
---

![Header Image](/blog/introducing-codename-one-websocket-support/html5-banner.jpg)

Codename One already has two separate socket APIs: [a low-level API](https://github.com/shannah/CN1Sockets) similar to `java.net.Socket` and [a higher-level event-based approach](http://www.codenameone.com/blog/sockets-multiline-trees.html). So why do we need WebSockets?

Here are 3 reasons:

### 1\. Simplifies Server Code

With low-level TCP sockets, you can’t just add a servlet to your existing Java web app to handle socket connections. You have to create your own server, and have it listen for connections on some custom port. You have to create your own sort of protocol. And all this just so you can pass messages.

With Web Sockets, you simply add an end point to your Java web app, and implement a few callbacks (e.g. `onMessage()`, `onOpen()`, `onClose()`, `onError()`), and you’re good to go.

E.g. Here is the full code for server-side of the [chat app demo](https://github.com/shannah/cn1-websockets#chat-demo):
    
    
    @ServerEndpoint("/chat")
    public class ChatDemoEndpoint {
    
    
        private static Set<Session> peers = Collections.synchronizedSet(new HashSet<Session>());
       @OnMessage
        public String onMessage(Session session, String message) {
            System.out.println(session.getUserProperties());
            if (!session.getUserProperties().containsKey("name")) {
                session.getUserProperties().put("name", message);
                return null;
            }
            for (Session peer: peers) {
                try {
                    peer.getBasicRemote().sendText(session.getUserProperties().get("name")+": "+message);
                } catch (IOException ex) {
                    Logger.getLogger(ChatDemoEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return null;
        }
    
        @OnOpen
        public void onOpen(Session peer) {
            peers.add(peer);
        }
    
        @OnClose
        public void onClose(Session peer) {
            peers.remove(peer);
        }
    
    }

The server just keeps a list of the open sessions, then it distributes messages received from any session to all sessions. Simple.

### 2\. Simplifies Client Code

Codename One’s existing high-level sockets API, being event-based, already takes some of the nasties out of socket programming. But Web sockets are still simpler. The symmetry between the client and server APIs makes for a pleasant experience. Just implement the same callbacks in the client, and you’re in business. Passing messages between server and clients couldn’t be simpler.

E.g. Here is the companion client code for the [chat app demo](https://github.com/shannah/cn1-websockets#chat-demo):
    
    
            sock = new WebSocket(SERVER_URL) {
    
                @Override
                protected void onOpen() {
                    System.out.println("In onOpen");
                }
    
                @Override
                protected void onClose(int statusCode, String reason) {
    
                }
    
                @Override
                protected void onMessage(final String message) {
                    System.out.println("Received message "+message);
                    Display.getInstance().callSerially(new Runnable() {
    
                        public void run() {
                            if (chatContainer == null) {
                                return;
                            }
                            SpanLabel label = new SpanLabel();
                            label.setText(message);
                            chatContainer.addComponent(label);
                            chatContainer.animateHierarchy(100);
                        }
    
                    });
                }
    
                @Override
                protected void onError(Exception ex) {
                    System.out.println("in onError");
                }
    
                 @Override
                 protected void onMessage(byte[] message) {
    
                 }
    
            };
            System.out.println("Sending connect");
            sock.connect();

This connects to the server using a url (just like any GET or POST request), then listens for messages from the server in the `onMessage()` method.

### 3\. Easier to Implement Cross-Platform

Implementing low-level TCP sockets in the [Javascript port](http://www.codenameone.com/blog/javascript-port.html) looked like it would be challenging, at best, and possibly impossible. But I wanted to be able to write cross-platform apps in Codename One that use sockets – and I wanted to be able to deploy to the Javascript port. Since WebSockets are a first-class citizen in the web, adding support to the Javascript port was trivial.

Since the websocket standard is now widely established and supported, it should be fairly easy to implement on other platforms as well. So far, I’ve implemented JavaSE, Android, iOS, and Javascript. Notably I’m missing Windows Phone, but it shouldn’t be hard to implement if a WinPhone developer wants to take on the challenge.

## Integrating WebSockets in your Project

Check out the [cn1-websockets project](https://github.com/shannah/cn1-websockets) for more information
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — December 19, 2017 at 12:33 pm ([permalink](/blog/introducing-codename-one-websocket-support/#comment-23794))

> Francesco Galgani says:
>
> I’m starting to search information on Codename One WebSockets because this discussion:  
> [https://stackoverflow.com/q…](<https://stackoverflow.com/questions/47876835/codename-one-how-to-correctly-add-the-pubnub-library-and-use-it/47880481#47880481>)  
> I didn’t find information on WebSockets in the developer guide, but I found this page. Please note that the following page gives a 404 error: [https://www.codenameone.com…](</blog/tags/websockets/>)
>



### **Shai Almog** — December 20, 2017 at 6:33 am ([permalink](/blog/introducing-codename-one-websocket-support/#comment-23647))

> Shai Almog says:
>
> Thanks, I’ll remove that tag.
>
> WebSockets is an external cn1lib so it’s not in the developer guide. I hope to add a chapter covering important cn1libs such as websockets, google maps etc. at some point in the future. Currently the best up to date docs for websocket support are here: [https://github.com/shannah/…](<https://github.com/shannah/cn1-websockets/>)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
