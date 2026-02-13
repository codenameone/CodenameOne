---
title: USE STORAGE, FILE SYSTEM AND SQL
slug: how-do-i-use-storage-file-system-sql
url: /how-do-i/how-do-i-use-storage-file-system-sql/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-use-storage-file-system-sql.html
tags:
- basic
- io
description: retain application data in Codename One
youtube_id: _EXEN52wQvs
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-20.jpg
---

{{< youtube "_EXEN52wQvs" >}} 

#### Transcript

In this short video I’d like to discuss the basic file storage API’s in Codename One. I’ll try to keep it relatively simple and skip over some of the more complex ideas such as parsing, externalization etc. I’ll start by talking about storage & filesystem.  
Storage is a high level abstraction, it might be implemented on top of file system but if you rely on that you might fail on the device as this isn’t always true. Storage is more portable, it doesn’t support file hierarchy such as directories. By contrast in file system we MUST use full file paths always.  
Storage is tied directly to the app and is usually private, this isn’t guaranteed for filesystem. Storage access is also cached for performance where filesystem access isn’t. But this brings us to a bigger core concept of mobile development, apps on mobile devices are completely isolated from one another and can’t access each others storage and usually can’t access each others filesystem files. This isn’t always the case, Android allows some restricted shared file system access but this is pretty tricky to pull off in a portable way.

Before we go to the code notice that in order to use this code you will need to import the CN class statics. Once you do that you can open a storage input or output stream and work with either one using standard Java IO calls without a problem.

Working with the filesystem is pretty similar to working with storage at least on the surface, but notice that the path to the file must include an absolute path and can’t be relative

SQLite is the de-facto standard for database storage on mobile devices, in fact it’s the worlds most popular database and is installed on billions of devices.  
Because SQLite is so easily embeddable and customizable it has fragmentation issues which I will discuss soon.  
It’s available in all the modern ports of Codename One however, the JavaScript port is problematic in this regard since HTML5 doesn’t have a finalized SQL standard. Some of the browsers have a deprecated version of the standard which is what we use there SQLite is great if you have a lot of data that you need to query, sort or filter often. Otherwise you will probably be better off using Storage which is far simpler and more portable.  
The SQLite database file uses a standard format and is physically stored within the device file system which means you can reach the physical file like any other file.

I mentioned fragmentation and the biggest one is probably thread safety. SQLite isn’t thread safe on iOS but is on Android. That means that if you don’t close a cursor and the GC closes it for you this might trigger a thread issue and a crash.  
There are also portability issues in the SQL language itself for instance in transaction isolation. The JavaScript port isn’t portable to all browsers and doesn’t support some features such as including an SQL database within your app bundle

The query API is pretty simple we can iterate over rows in a query and pull out the column values.

Notice that cleanup is crucial as the GC might be invoked if we don’t clean up ourselves. So handling the edge cases of the exceptions is essential!

A very common case is including an initial sql database that ships with your app. This allows you to include some initial data within the JAR or download initial data from a server.

You can “install” the database into place by using the getDatabasePath method and using the standard file system API to copy the data into the place where the database should be.

Here are a few tips and best practices when working with storage. The first is pretty crucial, you need to understand mobile app isolation as I discussed initially. Mobile apps don’t have desktop concepts like file open or file save dialogs since there is no concept of shared file system. That’s something you need to get used to as a mobile developer. The standard Storage should be the default mode you use. The other options are harder and less portable so pick them up only if you actually need them.  
When porting code we have some helper classes in the io package that you should notice. This is true for File, URL and other classes. Check the JavaDoc as many classes might not be in the same place but the functionality should still be there… Always use app home when working with filesystem unless there is a real special case in which case consider the fact you will be writing code that isn’t as portable.  
Preferences is great for simple data, I see developers completely forget it’s there and others try to overuse it for everything. It’s great for things like application settings which is the exact use case you should apply it to.

Thanks for watching, I hope you found this helpful

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
