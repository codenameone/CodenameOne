---
title: Open File & Rendering
slug: open-file-rendering
url: /blog/open-file-rendering/
original_url: https://www.codenameone.com/blog/open-file-rendering.html
aliases:
- /blog/open-file-rendering.html
date: '2016-04-20'
author: Shai Almog
---

![Header Image](/blog/open-file-rendering/bug.jpg)

As part of our continuing effort to squash bugs for the 3.4 release date we hit two major issues, the first of which is  
a long time RFE to [fix PDF viewing on iOS & Android to work consistently](https://github.com/codenameone/CodenameOne/issues/1651).  
This also applies to any file opening in iOS/Android which should now be trivial with the `Display.execute` method.  
Just use that method on any file within your home directory in [FileSystemStorage](https://www.codenameone.com/javadoc/com/codename1/io/FileSystemStorage.html)  
and it should launch the native app to view that file.

A common use case is to see a PDF file for help or guide e.g. like this:
    
    
    Form hi = new Form("PDF Viewer", BoxLayout.y());
    Button devGuide = new Button("Show PDF");
    devGuide.addActionListener(e -> {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String fileName = fs.getAppHomePath() + "pdf-sample.pdf";
        if(!fs.exists(fileName)) {
            Util.downloadUrlToFile("http://www.polyu.edu.hk/iaee/files/pdf-sample.pdf", fileName, true);
        }
        Display.getInstance().execute(fileName);
    });
    hi.add(devGuide);
    
    hi.show();

__ |  The original demo used the Developer guide but since that is a 25MB download that’s probably not a good  
idea for a mobile app demo…​   
---|---  
  
### Renderers On Android

The [other issue](https://github.com/codenameone/CodenameOne/issues/1645) we tackled was with renderers  
in Android following issues with the new Android pipeline. The asynchronous  
architecture of the newer Android pipeline made a lot of behaviors quite challenging especially for the renderer  
class where we share the `Style` object for multiple component instances.

As part of that fix we also improved performance of scrolling further by improving caching behavior on Android.

Part of the fix requires that all render components mark themselves properly as `setCellRenderer(true)`. This also  
applies to nesting so if you used something like a `Container` renderer containing multiple child components  
you need to call `setCellRenderer(true)` on all of the children. We don’t do it implicitly since that might lead to  
odd bugs where adding a child before and another child after will result in weird rendering…​

We intend to switch to the new Android pipeline a bit after the 3.4 release so be sure to update your renderer code  
if you use such an approach or better yet, avoid `List` and switch to [InfiniteContainer](https://www.codenameone.com/javadoc/com/codename1/ui/InfiniteContainer.html)  
or [InfiniteScrollAdapter](https://www.codenameone.com/javadoc/com/codename1/components/InfiniteScrollAdapter.html).

__ |  After publishing this Steve noted that I was quite unclear about a few of the things above so below  
are a couple of clarifications:   
---|---  
  
  * The `setCellRenderer` call only applies to you if you implemented the `ListCellRenderer` or `CellRenderer` interfaces.  
If you don’t use a `List` or used one of the standard lists (thru GUI builder etc.) this doesn’t apply as this is done  
implicitly (so nothing should be done for `GenericListCellRenderer`, `MultiList` etc.).

  * `setCellRenderer` always improved performance in Codename One lists, however the performance improvement  
I mentioned above relates mostly to some cache misses in the rendering pipeline that we ran into while fixing  
the issue.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Yaakov Gesher** — July 24, 2016 at 5:43 am ([permalink](https://www.codenameone.com/blog/open-file-rendering.html#comment-22894))

> Yaakov Gesher says:
>
> Hi Shai, I’ve been trying to use the execute() api and I’ve run into some unexpected behavior. It seems to try to open any local file as a pdf. It seemed from this post that it should work to open any file. If I try to open an mp3 file, for example, it should open with a media player app. But at least on Android (have yet to test on ios), even mp3 files get treated as pdfs (which obviously causes an error).


### **Shai Almog** — July 25, 2016 at 4:38 am ([permalink](https://www.codenameone.com/blog/open-file-rendering.html#comment-22546))

> Shai Almog says:
>
> Looking at the code I think it should work for MP3’s and I know people use it for video so it should be fine. Make sure the file extension is correct as this is important for this API.


### **Chris** — June 7, 2017 at 6:30 pm ([permalink](https://www.codenameone.com/blog/open-file-rendering.html#comment-23273))

> Chris says:
>
> Where is fs.getAppHomePath() in my project. How can I find the folder where should I place the PDF file to open from the App


### **Shai Almog** — June 8, 2017 at 5:29 am ([permalink](https://www.codenameone.com/blog/open-file-rendering.html#comment-23486))

> Shai Almog says:
>
> It’s not in your project. It’s a device specific path that varies from device to device. You can use Util.copy() to copy a resource file to that directory though.


### **Chris** — June 8, 2017 at 6:27 pm ([permalink](https://www.codenameone.com/blog/open-file-rendering.html#comment-23493))

> Chris says:
>
> Thank You Shai. I have the PDF in my default package of the src folder of CN1 package. I want to open the PDf on click of a button.I’m using the following code –  
> FileSystemStorage fs = FileSystemStorage.getInstance();  
> final String homePath = fs.getAppHomePath();  
> String fileName = homePath + “abc.pdf”;  
> Util.copy(fs.openInputStream(fileName), fs.openOutputStream(fileName));  
> Display.getInstance().execute(fileName);
>
> This is not working as it suppose to. All the examples are given for image saving but none of them is for PDF. In the following statement input and output streams need to have the filename or there should be any change –  
> Util.copy(fs.openInputStream(fileName), fs.openOutputStream(fileName)).  
> I have tried to save it to Storage as well – Util.copy(fs.openInputStream(fileName), Storage.getInstance().createOutputStream(fileName));


### **Shai Almog** — June 9, 2017 at 4:38 am ([permalink](https://www.codenameone.com/blog/open-file-rendering.html#comment-23366))

> Shai Almog says:
>
> Util.copy(Display.getInstance().getResourceAsStream(getClass(), “/abc.pdf”, fs.openOutputStream(fileName));
>
> But I suggest first checking if the file already exists or maybe even a version.


### **Chris** — June 9, 2017 at 9:04 pm ([permalink](https://www.codenameone.com/blog/open-file-rendering.html#comment-23283))

> Chris says:
>
> Perfect. It worked !! Thanks Shai.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
