---
title: Drag and Drop Uploader
slug: drag-and-drop-uploader
url: /blog/drag-and-drop-uploader/
original_url: https://www.codenameone.com/blog/drag-and-drop-uploader.html
aliases:
- /blog/drag-and-drop-uploader.html
date: '2016-08-07'
author: Shai Almog
---

![Header Image](/blog/drag-and-drop-uploader/generic-java-1.jpg)

One of our enterprise customers needed help with file upload from the desktop. E.g. in his Codename One app running in the browser or desktop he wanted the ability to drag a file. Somewhat like we can drag a file into gmails compose window.

This doesn’t make sense within Codename One as something like that doesn’t exist on mobile devices or even tablets. Creating a [cn1lib that implements this functionality](https://github.com/shannah/cn1-native-data-transfer) does make sense, so [Steve did just that](https://github.com/shannah/cn1-native-data-transfer).

You can get started by installing the library using our [extension manager tool](http://www.codenameone.com/blog/automatically-install-update-distribute-cn1libs-extensions.html).

Check out the live demo here on the right side, drag an image into the phone simulator to add it to the form.

__ |  I chose to use a phone skin out of habit but this obviously isn’t useful for actual devices…​   
---|---  
  
This is the code that makes it all happen:
    
    
    Form hi = new Form("Drag and Drop Demo");
    
    if (DropTarget.isSupported()) {
        DropTarget dnd = DropTarget.create((evt)->{
            String srcFile = (String)evt.getSource();
            System.out.println("Src file is "+srcFile);
            System.out.println("Location: "+evt.getX()+", "+evt.getY());
            if (srcFile != null) {
                try {
                    Image img = Image.createImage(FileSystemStorage.getInstance().openInputStream(srcFile));
                    hi.add(img);
                    hi.revalidate();
                } catch (IOException ex) {
                    Log.e(ex);
                }
            }
        }, Display.GALLERY_IMAGE);
    }
    hi.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
    hi.setScrollableY(true);
    hi.addComponent(new SpanLabel("Drag photos from your desktop into this app"));
    hi.show();

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
