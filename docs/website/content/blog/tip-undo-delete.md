---
title: 'TIP: Undo Delete'
slug: tip-undo-delete
url: /blog/tip-undo-delete/
original_url: https://www.codenameone.com/blog/tip-undo-delete.html
aliases:
- /blog/tip-undo-delete.html
date: '2018-12-10'
author: Shai Almog
---

![Header Image](/blog/tip-undo-delete/tip.jpg)

One of my pet peeves is the “Are you sure?” dialog. I’ve used it a lot myself because it’s the “easy way out”, but when possible I try to avoid it. This is especially important in mobile where constant prompts really slow down the workflow.

The trick that lets us avoid the “Are you sure?” dialog is the undo option. Once you can undo an operation you can move fast and let users reconsider later. With file deletion this is a bit harder. Most OS’s provide a trash can abstraction. This doesn’t exist in mobile devices where apps are effectively segregated from one another. However, implementing our own trash can is pretty trivial.

These are methods I use in one of our util classes, they aren’t public API’s because they are too high level for the API. But they might be useful for you. First we need copy and move operations. Notice, I didn’t use file system rename since different file systems might be isolated on the device and might not allow a rename:
    
    
    public static void copyFile(String sourceFile, String destFile) throws IOException {
        try (InputStream source = openFileInputStream(sourceFile);
            OutputStream dest = openFileOutputStream(destFile)) {
            Util.copy(source, dest);
        }
    }
    
    public static void moveFile(String sourceFile, String destFile) throws IOException {
        copyFile(sourceFile, destFile);
        delete(sourceFile);
    }

The next stage is a trash abstraction. We add a `Trash` directory which we create dynamically. We then move the file to a dummy name in the trash to delete it. Empty trash might be something you can expose to the user or just invoke on startup/exit.
    
    
    public static String moveToTrash(String file) {
        try {
            String trash = getAppHomePath() + "Trash/";
            mkdir(trash);
            String trashName = trash + System.currentTimeMillis();
            moveFile(file, trashName);
            return trashName;
        } catch(IOException err) {
            Log.e(err);
            return null;
        }
    }
    
    public static void emptyTrash() {
        try {
            String trash = getAppHomePath() + "Trash/";
            mkdir(trash);
            String[] arr = listFiles(trash);
            for(String f : arr) {
                delete(trash + f);
            }
        } catch(IOException err) {
            Log.e(err);
        }
    }

Last but not least we have the undoable delete. The `autoFlash` option allows us to automatically delete the file after 10 seconds if it wasn’t restored. This is accomplished by launching a thread to delete the file later. We also send a callback in the case of an undelete so a user can update the UI.
    
    
    public static void undoableDelete(String file, boolean autoFlush, Runnable onUndelete) {
        String trashFile = moveToTrash(file);
        ToastBar.showMessage("Deleted " +
            file.substring(file.lastIndexOf('/') + 1) +
            "nTouch to undo", FontImage.MATERIAL_UNDO, e -> {
            try {
                moveFile(trashFile, file);
                onUndelete.run();
            } catch(IOException err) {
                Log.e(err);
                ToastBar.showErrorMessage("An error occurred on file restore");
            }
        });
        if(autoFlush) {
            startThread(() -> {
                Util.sleep(10000);
                if(existsInFileSystem(trashFile)) {
                    delete(trashFile);
                }
            }, "Delete Trash");
        }
    }

I used a toastbar to show the undo message, you might want to provide more ways to undo a delete depending on the case.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — January 6, 2019 at 9:10 am ([permalink](https://www.codenameone.com/blog/tip-undo-delete.html#comment-24061))

> Francesco Galgani says:
>
> Thank you for these useful methods. Can you please confirm that all of them are to be used with FIleSystemStorage and not with Storage?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-undo-delete.html)


### **Shai Almog** — January 6, 2019 at 11:53 am ([permalink](https://www.codenameone.com/blog/tip-undo-delete.html#comment-23996))

> Shai Almog says:
>
> This was specifically designed for file system storage but can easily be adapted to support Storage. E.g. moveToTrash can check if the file is in storage by checking if startsWith(“file://”) that would only change the opening of the file code. The Trash directory would still be in the file system storage.
>
> copyFile and deleteFile can be similarly adapted to detect a storage file and use the storage API instead of the FileSystemStorage API.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-undo-delete.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
