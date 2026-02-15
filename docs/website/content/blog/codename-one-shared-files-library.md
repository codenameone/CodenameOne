---
title: Codename One Shared Files Library
slug: codename-one-shared-files-library
url: /blog/codename-one-shared-files-library/
original_url: https://www.codenameone.com/blog/codename-one-shared-files-library.html
aliases:
- /blog/codename-one-shared-files-library.html
date: '2023-07-26'
author: Steve Hannah
description: This library provides a wrapper over the Android shared files API to
  allow your Codename One apps to read and write shared files (i.e. files accessible
  to other apps).
---

This library provides a wrapper over the Android shared files API to allow your Codename One apps to read and write shared files (i.e. files accessible to other apps).

![codename-one-shared-files-library](/blog/codename-one-shared-files-library/Shared-Files-Library-1024x536.jpg)

> - See this on [GitHub.](https://github.com/shannah/cn1-shared-files-lib)
>     
>   - See example app using this API [here.](https://github.com/shannah/cn1-shared-files-lib-demo/tree/master/common/src/main/java/com/codename1/shfltest)
>       
>     - See use cases for accessing documents and other files [here.](https://developer.android.com/training/data-storage/shared/documents-files#use-cases)

### Background

#### Accessing documents and other files from shared storage (including external and cloud storage).

In the past, we used to be able to access files on external storage directly using ****FileSystemStorage****, but more recent android versions block this, requiring you to use their[Shared Document APIs](https://developer.android.com/training/data-storage/shared/documents-files). a.k.a Share Files API and Storage Access Framework.

On Android devices from version 4.4 and above, apps can use the ****Storage Access Framework**** to let users choose documents and files for the app without needing special permissions. This enhances user privacy and control, and the accessed files remain on the device even after uninstalling the app.

The new Codename One Shared Files Library provides a wrapper over the Android shared files API to allow your Codename One apps to read and write shared files (i.e. files accessible to other apps).

This is a clean, secure and well-supported way to save and open files that could also be accessed by other applications.

### Basic Usage

This API provides two abstractions:

1. `SharedFile` – Represents a single file or directory.

2. `SharedFileManager` – Provides access to the shared file system. Includes UI abstractions to select directories and files.

First step is to request access to a file:

```java
				
					// Open a directory
// Will open a file chooser for user to access a directory
SharedFileManager.getInstance().openDirectory().ready(sharedDirectory -> {
    // sharedDirectory is a SharedFile object
});

// Open a file
SharedFileManager.getInstance().openFile().ready(sharedFile -> {
    // sharedFile is a SharedFile object
});

// Open file of specific type
SharedFileManager.getInstance().openFile("text/plain").ready(sharedFile -> {
    // sharedFile is a SharedFile object
});
				
			
```

### Reading and Writing Files

Use `SharedFile.openInputStream()` and `SharedFile.openOutputStream(String mimetype)` for reading and writing files.

E.g.

```java
				
					String textContents = Util.readToString(sharedFile.openInputStream());

textContents += "Modified";
try (OutputStream output = sharedFile.openOutputStream(sharedFile.getMimetype())) {
    output.write(textContents.getBytes("UTF-8"));
}
				
			
```

### Creating New Files

1. Open a directory

2. Call `directory.getChild(relativePath)` to get reference to file.

3. Call `child.openOutputStream(mimetype)`

### Bookmarking For Later Use

By default, the files you obtain will no be accessible the next time you load the app. You need to create a bookmarked file which will provide you with a persistent path that you can use to access the file.

1. Use `SharedFile.createBookmark()` to create a bookmark.

2. Use `SharedFile.deleteBookmark()` do remove a bookmark.

3. Use `SharedFile.isBookmark()` to check if the file is a bookmarked file.

4. Use `SharedFileManager.openBookmark(String)` to open a file given its bookmarked path. (i.e. bookmarkedFile.getPath())

5. Use `SharedFileManager.getBookmarks()` for a list of all current bookmarks.

### Installation

Add the following maven dependency to your common/pom.xml file

```xml
				
					
    com.codenameone
    sharedfiles-lib
    0.1.0
    pom

				
			
```
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **Wits ICT** — August 1, 2023 at 10:26 pm ([permalink](/blog/codename-one-shared-files-library/#comment-24565))

> Wits ICT says:
>
> Thanks for this. And good to see blogs returning after a while.
>



### **Mobi Tribe** — October 10, 2023 at 12:49 pm ([permalink](/blog/codename-one-shared-files-library/#comment-24580))

> Mobi Tribe says:
>
> Thanks @Steve Hannah. This is awesome. Very useful… As @Wits ICT has said, glad to see you guys back. Hopefully you, Chen and Shai and the rest of the cn1 family still have good stuff in store.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
