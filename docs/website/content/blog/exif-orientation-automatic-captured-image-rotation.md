---
title: Exif Orientation Tag and Smart Downloads
slug: exif-orientation-automatic-captured-image-rotation
url: /blog/exif-orientation-automatic-captured-image-rotation/
original_url: https://www.codenameone.com/blog/exif-orientation-automatic-captured-image-rotation.html
aliases:
- /blog/exif-orientation-automatic-captured-image-rotation.html
date: '2020-07-02'
author: Shai Almog
---

![Header Image](/blog/exif-orientation-automatic-captured-image-rotation/guest-post-1.jpg)

On some devices, `Capture` APIs return images with the correct orientation, meaning that they do not need to be changed to display correctly; on other devices, they return images with a fixed orientation and an EXIF tag that indicates how they must be rotated or flipped to display correctly.

More precisely, the Orientation Tag indicates the orientation of the camera with respect to the captured scene and can take a value from 0 to 8, as illustrated on the page [Exif Orientation Tag](http://sylvana.net/jpegcrop/exif_orientation.html). For testing purposes, you can download landscape and portrait images with all possible orientation values from the [EXIF Orientation-flag example images repository](https://github.com/recurser/exif-orientation-examples).

### What happens if we ignore the Orientation Tag

Suppose we acquire an image with the following code:
    
    
    Form hi = new Form("Capture Test", BoxLayout.y());
    Button button = new Button("Take Photo");
    ScaleImageLabel photoLabel = new ScaleImageLabel();
    hi.addAll(button, photoLabel);
    hi.show();
    
    button.addActionListener(l -> {
        String photoTempPath = Capture.capturePhoto();
        if (photoTempPath != null) { __**(1)**
            try {
                String photoStoragePath = "myPhoto.jpg";
                Util.copy(FileSystemStorage.getInstance().openInputStream(photoTempPath), Storage.getInstance().createOutputStream(photoStoragePath)); __**(2)**
                photoLabel.setIcon(EncodedImage.create(Storage.getInstance().createInputStream(photoStoragePath), Storage.getInstance().entrySize(photoStoragePath))); __**(3)**
                hi.revalidate();
            } catch (IOException ex) {
                Log.p("Error after capturing photo", Log.ERROR);
                Log.e(ex);
                Log.sendLogAsync();
            }
    
        }
    });

A few remarks:

__**1** | `photoTempPath` is `null` if the user has cancelled the photo capture;  
---|---  
__**2** | in this case, copying the file from the `FileSystemStorage` temporary folder to a “secure” location in `FileSystemStorage` or `Storage` is not strictly necessary, but it is a good habit that in certain circumstances prevents issues;  
__**3** | it is always preferable to use `EncodedImage` when we want to keep the impact on memory low.  
  
#### Desired result

On my iPhone, the image is always in portrait orientation:

![85854279 64b80800 b7b4 11ea 88e2 98a6d57fc09f](/blog/exif-orientation-automatic-captured-image-rotation/85854279-64b80800-b7b4-11ea-88e2-98a6d57fc09f.jpg)

#### Unwanted result

This is the case of my Samsung Galaxy, the image was taken in portrait, but shown in a different orientation:

![85853565 050d2d00 b7b3 11ea 8053 695772ecbfd0](/blog/exif-orientation-automatic-captured-image-rotation/85853565-050d2d00-b7b3-11ea-8053-695772ecbfd0.jpg)

#### Solving This Problem

All it takes is a small change to the code to solve this issue.

Just replace:
    
    
    String photoStoragePath = "myPhoto.jpg";
    Util.copy(FileSystemStorage.getInstance().openInputStream(photoTempPath), Storage.getInstance().createOutputStream(photoStoragePath));
    photoLabel.setIcon(EncodedImage.create(Storage.getInstance().createInputStream(photoStoragePath), Storage.getInstance().entrySize(photoStoragePath)));

with:
    
    
    String photoSafePath = FileSystemStorage.getInstance().getAppHomePath() + "/myPhoto.jpg"; __**(1)**
    Image img = Image.exifRotation(photoTempPath, photoSafePath, 1000); __**(2)**
    photoLabel.setIcon(img); __**(3)**

__**1** | In this case, we have to use `FileSystemStorage` rather than `Storage` due to a limitation of the `exifRotation` API, which maybe will be solved in the future;  
---|---  
__**2** | the third parameter is optional, but as explained in the [exifRotation Javadoc](http://www.codenameone.com/javadoc/com/codename1/ui/Image.html#exifRotation-java.lang.String-java.lang.String-int-), the rotation of a high-resolution image is very inefficient, it is better to set the maximum size (width or height) that the image can assume, in this case 1000px, to obtain a significant advantage in processing time on less performing devices;  
__**3** | note that the instance of the `Image` object returned by `exifRotation` is an `EncodedImage`, to keep the impact on memory low.  
  
#### Final result

On my iPhone the result is the same (as expected), while on my Android, taking the same photo, I get:

![85862326 e82c2600 b7c1 11ea 9135 657f2a0eead7](/blog/exif-orientation-automatic-captured-image-rotation/85862326-e82c2600-b7c1-11ea-9135-657f2a0eead7.jpg)

This is the desired result. As a final note, I mention the [Image.getExifOrientationTag](http://www.codenameone.com/javadoc/com/codename1/ui/Image.html#getExifOrientationTag-java.lang.String-) API which allows you to get the EXIF orientation tag of an image, if available.

### Network error resistant downloads with automatic resume

When we download a big file, such as a high-resolution image or a video, there are problems that can prevent the download from finishing:

  * The user moves the app to the background or external conditions (such as a phone call) move the app to the background;

  * The operating system enters power saving mode;

  * The Internet connection is lost or any other network error interrupts the download.

A server-side error may also occur, but this cannot be resolved client-side. All the other circumstances mentioned above can, provided that the server supports partial downloads via HTTP headers. Fortunately, this is a feature available by default on most common servers (such as Apache or Spring Boot). Almost all download managers allow to resume interrupted downloads, so I thought it was important to add such a feature to Codename One.

#### The solution

THe new API [Util.downloadUrlSafely](https://www.codenameone.com/javadoc/com/codename1/io/Util.html#downloadUrlSafely-java.lang.String-java.lang.String-com.codename1.util.OnComplete-com.codename1.util.OnComplete-) safely download the given URL to the `Storage` or to the `FileSystemStorage`.

This method is resistant to network errors and capable of resume the download as soon as network conditions allow and in a completely transparent way for the user.

#### Server requirements

The server must correctly return the `Content-Length` header and it must supports partial downloads.

#### Global network error handling requirements

In the global network error handling, there must be an automatic `.retry()` of the `ConnectionRequest` in the case of a network error.

I think the best way to show the use of this API is an actual complete example, which you can try as it is in the Simulator and on real devices:
    
    
    public class MyApplication {
    
        private Form current;
        private Resources theme;
    
        public void init(Object context) {
            // use two network threads instead of one
            updateNetworkThreadCount(2);
    
            theme = UIManager.initFirstTheme("/theme");
    
            // Enable Toolbar on all Forms by default
            Toolbar.setGlobalToolbar(true);
    
            // Pro only feature
            Log.bindCrashProtection(true);
    
            // Manage both network errors (connectivity issues) and server errors (codes different from 2xx)
            addNetworkAndServerErrorListener();
        }
    
        public void start() {
            if(current != null){
                current.show();
                return;
            }
    
            String url = "https://www.informatica-libera.net/video/AVO_Cariati_Pasqua_2020.mp4"; // 38 MB
    
            Form form = new Form("Test Download 38MB", BoxLayout.y());
            Label infoLabel = new Label("Starting download...");
            form.add(infoLabel);
    
            try {
                Util.downloadUrlSafely(url, "myHeavyVideo.mp4", (percentage) -> {
                    // percentage callback
                    infoLabel.setText("Downloaded: " + percentage + "%");
                    infoLabel.repaint();
                }, (filename) -> {
                    // file saved callback
                    infoLabel.setText("Downloaded completed");
                    int fileSizeMB = Storage.getInstance().entrySize(filename) / 1048576;
                    form.add("Checking files size: " + fileSizeMB + " MB");
                    form.revalidate();
                });
            } catch (IOException ex) {
                Log.p("Error in downloading: " + url);
                Log.e(ex);
                form.add(new SpanLabel("Error in downloading:n" + url));
                form.revalidate();
            }
    
            form.show();
    
    
        }
    
        public void stop() {
            current = getCurrentForm();
            if(current instanceof Dialog) {
                ((Dialog)current).dispose();
                current = getCurrentForm();
            }
        }
    
        public void destroy() {
        }
    
        private void addNetworkAndServerErrorListener() {
            // The following way to manage network errors is discussed here:
            // https://stackoverflow.com/questions/61993127/distinguish-between-server-side-errors-and-connection-problems
            addNetworkErrorListener(err -> {
                // prevents the event from propagating
                err.consume();
    
                if (err.getError() != null) {
                    // this is the case of a network error,
                    // like: java.io.IOException: Unreachable
                    Log.p("Error connectiong to: " + err.getConnectionRequest().getUrl(), Log.ERROR);
                    // maybe there are connectivity issues, let's try again
                    ToastBar.showInfoMessage("Reconnect...");
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            err.getConnectionRequest().retry();
                        }
                    }, 2000);
                } else {
                    // this is the case of a server error
                    // logs the error
                    String errorLog = "REST ERRORnURL:" + err.getConnectionRequest().getUrl()
                            + "nMethod: " + err.getConnectionRequest().getHttpMethod()
                            + "nResponse code: " + err.getConnectionRequest().getResponseCode();
                    if (err.getConnectionRequest().getRequestBody() != null) {
                        errorLog += "nRequest body: " + err.getConnectionRequest().getRequestBody();
                    }
                    if (err.getConnectionRequest().getResponseData() != null) {
                        errorLog += "nResponse message: " + new String(err.getConnectionRequest().getResponseData());
                    }
                    if (err.getConnectionRequest().getResponseErrorMessage() != null) {
                        errorLog += "nResponse error message: " + err.getConnectionRequest().getResponseErrorMessage();
                    }
                    Log.p(errorLog, Log.ERROR);
    
                    Log.sendLogAsync();
                    ToastBar.showErrorMessage("Server Error", 10000);
                }
            });
        }
    
    }

#### Safe Uploads?

Implementing uploads with the same features (network error resistance and automatic resume) is more complex, because in this case we do not have a reference standard available by default on the most common servers.

Moreover, the possibility of partial uploads assumes that, after a network error, the server must keep the partially uploaded file and there are no ambiguities about which client has partially uploaded which file.

Applications such as Dropbox, Google Drive, OwnCloud and similar use specific internal standards. As far as I’m concerned, I’m almost completed deploying my own client-server solution to allow secure, network error-resistant with automatic resume uploads with Codename One and Spring Boot. This solution, however, is too specific to be included in the Codename One API and, anyway, I still have to do a lot of testing to make sure it works as it should. I’ll possibly publish a tutorial about it when it is finished.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Javier Anton** — July 3, 2020 at 10:45 pm ([permalink](https://www.codenameone.com/blog/exif-orientation-automatic-captured-image-rotation.html#comment-24283))

> Really useful stuff. Just wondering, the safe download always requires the app to be in fg to finish, right? I mean, the download won’t finish if the app goes into bg and never comes back? And my second question is: does the download effectively pause when the app is in bg? Thx
>



### **Shai Almog** — July 4, 2020 at 4:50 am ([permalink](https://www.codenameone.com/blog/exif-orientation-automatic-captured-image-rotation.html#comment-24284))

> This uses background fetch to download in the background so download continues automatically when the device is backgrounded. Normally when a device is sent to background a download will stop in this case it’s supposed to continue.
>



### **Francesco Galgani** — July 5, 2020 at 5:46 am ([permalink](https://www.codenameone.com/blog/exif-orientation-automatic-captured-image-rotation.html#comment-24285))

> I’m afraid there’s been a misunderstanding. As I wrote: “This method is resistant to network errors and capable of resume the download as soon as network conditions allow and in a completely transparent way for the user. This is regardless of whether the download continues or not when the app goes in background: if the operating system stops the download when the app goes in background, it will automatically resume when the app goes back in foreground, otherwise it will continue in background. More specifically, usually (but not necessarily always) the download will continue in the background on Android, while it will “pause” on iOS. Without this method, when the app goes into the background the download can be “killed” without finish, with this method the download will be restored to where it came from when the app returns to foreground. In this sense, it is normal to expect the download to end when the app is returned to foreground, although in some cases (such as Android) it may continue and complete in the background. Backgroundfetch is therefore not used.
>



### **Shai Almog** — July 5, 2020 at 6:17 am ([permalink](https://www.codenameone.com/blog/exif-orientation-automatic-captured-image-rotation.html#comment-24287))

> Shai Almog says:
>
> Thanks for the clarification. I didn’t recall the PR exactly but recalled our stackoverflow discussion.  
> What about adding something closer to what was done in stackoverflow with background fetch?
>
> Maybe as a secondary API we can add to the stop() call? E.g. convertOngoingDownloads()?
>
> I’m guessing we would need a DownloadManager sort of API to do something like that.
>



### **Francesco Galgani** — July 5, 2020 at 7:17 am ([permalink](https://www.codenameone.com/blog/exif-orientation-automatic-captured-image-rotation.html#comment-24288))

> [Francesco Galgani](https://lh6.googleusercontent.com/-4K0ax_DVJf4/AAAAAAAAAAI/AAAAAAAAAAA/AMZuuckEd1kcni0y8k6NMzNtxwOCEPatQQ/photo.jpg) says:
>
> I will try to study this problem to improve this API, the problem is that “background fetch” is not usable for heavy downloads. I quote:
>
> “You only have seconds to operate when doing a background fetch — the consensus figure is a maximum of 30 seconds, but shorter is better. If you need to download large resources as part of the fetch, this is where you need to use URLSession‘s background transfer service.” fonte: <https://www.raywenderlich.com/5817-background-modes-tutorial-getting-started>
>
> I don’t know this “URLSession‘s background transfer service”. Is this something that requires a native interface? Do you have any suggestions for me?
>



### **Shai Almog** — July 6, 2020 at 5:35 am ([permalink](https://www.codenameone.com/blog/exif-orientation-automatic-captured-image-rotation.html#comment-24290))

> Shai Almog says:
>
> Not sure. I’ll have to look into that too.
>



### **Javier Anton** — July 6, 2020 at 6:49 am ([permalink](https://www.codenameone.com/blog/exif-orientation-automatic-captured-image-rotation.html#comment-24289))

> [Javier Anton](https://lh3.googleusercontent.com/a-/AAuE7mDRjHkEvAZNXquh9p7Oo00ey1yOwNzZ0SrFwD0IVA) says:
>
> I hope you get this sorted – was on my todo list too. You can do iOS bg fetch and just catch whether the OS kills the download. The issue is that you will need to run it in a native interface and provide a callback static method somewhere in your java code. Perhaps you could also use some other method to notify your main thread (NSUserDefaults or writing a persisted file). I’m not sure which is best for your implementation. Good luck! 🙂
>



### **Javier Anton** — August 13, 2020 at 3:21 pm ([permalink](https://www.codenameone.com/blog/exif-orientation-automatic-captured-image-rotation.html#comment-24319))

> [Javier Anton](https://lh3.googleusercontent.com/a-/AAuE7mDRjHkEvAZNXquh9p7Oo00ey1yOwNzZ0SrFwD0IVA) says:
>
> Wow. I was just testing with some photos my wife had taken with her pro camera and noticed that some pictures that showed up properly in the OS were being rotated by the Simulator. I then remembered this post, decided to see if it would fix this and… voila! Thanks for this Francesco, really great stuff  
> One question I have is: if I don’t set a maximum px in the 3rd parameter of exifRotation, will it make it harder for images that need rotating, or will it make it harder for all images?  
> Edit: this will mistakenly rotate 90 degrees to the right images captured by my Galaxy A10 camera
>



### **Javier Anton** — August 13, 2020 at 4:15 pm ([permalink](https://www.codenameone.com/blog/exif-orientation-automatic-captured-image-rotation.html#comment-24320))

> [Javier Anton](https://lh3.googleusercontent.com/a-/AAuE7mDRjHkEvAZNXquh9p7Oo00ey1yOwNzZ0SrFwD0IVA) says:
>
> Another question I have is: why use the ImageIO.save so much? A lot of the operations can be done without needing to re-save the image to a different file. Am I missing something?
>



### **Shai Almog** — August 14, 2020 at 4:33 am ([permalink](https://www.codenameone.com/blog/exif-orientation-automatic-captured-image-rotation.html#comment-24322))

> Shai Almog says:
>
> If rotatedImage url is null it won’t save the rotated image to a file so there’s no need for that. Notice that it always returns an encoded image so there will always be an encoder overhead.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
