---
title: Seamless Storage Encryption
slug: seamless-storage-encryption
url: /blog/seamless-storage-encryption/
original_url: https://www.codenameone.com/blog/seamless-storage-encryption.html
aliases:
- /blog/seamless-storage-encryption.html
date: '2016-12-06'
author: Shai Almog
---

![Header Image](/blog/seamless-storage-encryption/new-features-6.jpg)

We had support for bouncy castle encryption for quite a while but it is not as intuitive as we’d like it to be. This makes  
securing/encrypting your app painful and so we procrastinate and eventually skip that “feature” altogether.  
Frankly, I hate working on encryption it’s painful…​ That’s why we procrastinated on this feature until today!

We now support full encryption of the `Storage` (notice the distinction, `Storage` is not `FileSystemStorage`).  
This is available by installing the latest bouncy castle cn1lib from the extensions menu then using one line of code
    
    
    EncryptedStorage.install("your-pass-encryption-key");

__ |  Normally you would want that code within your `init(Object)` method   
---|---  
  
Notice that you can’t use storage or preferences to store this data as it would be encrypted (`Preferences` uses  
`Storage` internally). You can use a password for this key and it would make it way more secure **but** if a user  
changes his password you might have a problem. In that case you might need the old password to migrate to  
a new password.

This works thru a new mechanism we added to storage where you can replace the storage instance with another  
instance using:
    
    
    Storage.setStorageInstance(new MyCustomStorageSubclass());

We can leverage that knowledge to change the encryption password on the encryption storage using pseudo  
code like this:
    
    
    EncryptedStorage.install(oldKey);
    InputStream is = Storage.getInstance().createInputStream(storageFileName);
    byte[] data = Util.readInputStream(is);
    EncryptedStorage.install(newKey);
    OutputStream o = Storage.getInstance().createOutputStream("TestEncryption");
    o.write(data);
    o.close();

__ |  It’s not a good idea to replace storage objects when an app is running so this is purely for this special case…​   
---|---  
  
### Moving Forward

We’d like Codename One apps to be more secure than native apps and they are already pretty good to begin with.  
Because we use code and not a well known resource format and because we obfuscate by default our apps are  
much harder to reverse engineer.

However, we think we can do more and we hope to do so. This specific feature was sponsored by an enterprise  
customer who needed that capability, we have other pending features in the IO section related to other enterprise  
customer RFE’s that are coming soon.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
