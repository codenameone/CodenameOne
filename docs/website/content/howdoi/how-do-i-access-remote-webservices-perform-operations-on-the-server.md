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

#### Script

The webservice wizard is very similar to the GWT RPC functionality if you are familiar with that. We generate a set of simple static functions on the client side and then generate for you a servlet with some helper classes on the server side in order to implement client/server functionality. Lets start by adding a simple webservice to a hello world application. First lets launch the webservice wizard by right clicking and selecting it. This tool allows us to define methods and their arguments. Arguments can be the basic Java types, arrays and Externalizable classes. For simplicities sake I’ll just add a sayHello method with String argument and return value. When we press generate code is generated into the project, notice you migth get prompted if files will be overwritten. We also need to select the source folder of a web server project. I created a standard Java web project and I just selected it here in the tool. The first thing we need to do is open the WebServiceProxy class, its a generated class so its not very important however we need to fix the destination URL to point at the right address including the server context. Now lets look at the server generated code we have the classes in the io package and the proxy.server package which are fixed utility classes. Then we have the CN1WebServiceServlet which is generated to intercept your calls, the only class you need to concern yourself with is the class ending with the word Server which is the one where you should write your source code. Lets fill up the sayHello method with a simple implementation. In the client side lets just add a couple of buttons to the form and demonstrate the difference between a sync call and an async call. In a sync call we just invoke the method directly as if it was a local method. Notice sync calls will throw an IO exception if there was an issue in the server side. The async methods never throw an exception and will communicate success/failure thru a callback mechanism similar to GWT. Success is invoked when the method returns and passes the appropriate response value if relevant. Lets run this code. Notice that when you create a webservice you can’t change the methods after the fact, so if you want to add additional functionality you will need new method names so as not to break applications in production. You can also generate several servlets in several packages and just point to a separate URL. Thanks for watching I hope you found this tutorial educational and helpful.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
