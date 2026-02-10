---
title: Toast Downloads
slug: toast-downloads
url: /blog/toast-downloads/
original_url: https://www.codenameone.com/blog/toast-downloads.html
aliases:
- /blog/toast-downloads.html
date: '2016-07-27'
author: Shai Almog
---

![Header Image](/blog/toast-downloads/toastbar-downloads.png)

I wanted to write about the new kitchen sink demo which I’m trying to finish…​  
But I haven’t finished it yet as it’s such a major undertaking. As part of that work I wanted to show some code that downloads themes and didn’t want to use the venerable infinite progress indicator which [I generally dislike](/blog/dont-block-the-ui.html)…​

Over these past few months I’ve enjoyed the `ToastBar` tremendously and I think it’s probably far better suited for such a use case than the infinite progress. The `ToastBar` has rudimentary progress indication capabilities that are perfect for such a use case.

I added a relatively simple method to `ToastBar` but I’m guessing we will add more elaborate versions of this code:
    
    
    public static void showConnectionProgress(String message, final ConnectionRequest cr, SuccessCallback<NetworkEvent> onSuccess, FailureCallback<NetworkEvent> onError)

This method shows a progress indicator `ToastBar` with a progress value for the connection request. On completion it will invoke the onSuccess/onError callbacks both of which may be null. E.g. for the case of theme download in the new KitchenSink demo I use the following code:
    
    
    ConnectionRequest cr = new ConnectionRequest(BASE_URL + currentThemeFile);
    cr.setDestinationStorage(currentThemeFile);
    ToastBar.showConnectionProgress("Downloading theme", cr, ee -> {
        setTheme(parentForm, currentThemeFile);
        theme.putClientProperty("downloading", null);
    }, (sender, err, errorCode, errorMessage) -> {
        ToastBar.showErrorMessage("There was an error downloading the file: " + err);
        Log.e(err);
    });
    cr.setFailSilently(true);
    NetworkManager.getInstance().addToQueue(cr);

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
