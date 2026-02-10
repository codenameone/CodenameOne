---
title: Data Processing in Codename One apps
slug: data-processing-in-codename-one-apps
url: /blog/data-processing-in-codename-one-apps/
original_url: https://www.codenameone.com/blog/data-processing-in-codename-one-apps.html
aliases:
- /blog/data-processing-in-codename-one-apps.html
date: '2021-03-25'
author: Steve Hannah
description: The following recipes relate to data processing and conversion in Codename
  One apps. This includes parsing data like JSON, HTML and XML.
---

The following recipes relate to data processing and conversion in Codename One apps. This includes parsing data like JSON, HTML and XML.

### Parsing HTML

## Problem

You want to parse some HTML content into a data structure. You can’t simply use the [XMLParser](https://www.codenameone.com/javadoc/com/codename1/xml/XMLParser.html) class because the content is not well-formed XML, but you would like to be able to work with the parsed document using the same tools (e.g. [Result](https://www.codenameone.com/javadoc/com/codename1/processing/Result.html) and [Element](https://www.codenameone.com/javadoc/com/codename1/xml/Element.html).)

## Solution

Use the HTMLParser class from the [CN1HTMLParser cn1lib](https://github.com/shannah/CN1HTMLParser). It contains a simple API for parsing an HTML string into an [Element](https://www.codenameone.com/javadoc/com/codename1/xml/Element.html), the same type of element that [XMLParser](https://www.codenameone.com/javadoc/com/codename1/xml/XMLParser.html) returns.

## Usage Example:

```java
				
					HTMLParser parser = new HTMLParser();

Element root = parser.parse(htmlString).get(); (1)
Result r = Result.fromContent(root);

// Now modify the document
// In this example we're going to replace image src with placeholders
// so we can load them separately.
List images = r.getAsArray("//img");
int index = 0;
List toLoad = new ArrayList<>();
if (images != null) {
    for (Element img : images) {
        String src = img.getAttribute("src");
        if (src.startsWith("http://*/") || (!src.startsWith("http://") && !src.startsWith("data:") && !src.startsWith("https"))) {
            img.setAttribute("id", "nt-image-"+index);
            toLoad.add(src);
            img.setAttribute("src", "");
            index++;
        }
    }
}

// Now write the document as well-formed XML.
XMLWriter writer = new XMLWriter(true);
String pageContent = writer.toXML(root);
				
			
```

The `parse()` method returns an Async promise. If you want to use it synchronously, you can call `get()`, which will wait until parsing is done.

## Alternate Async Usage

The above example uses the `get()` method to wait until the result is ready, but you can use the parser asynchronously as well:

```java
				
					parser.parse(htmlString).ready(root->{
     // root is the root Element of the document.
});
				
			
```

## Discussion

The HTMLParser class wraps an off-screen BrowserComponent to use the platform’s native webview to actually parse the HTML. It then serializes the DOM as XML, which is then re-parsed using the Codename One XML parser. There are pitfalls to this approach, including performance (it takes time to pass data back-and forth between a webview, after all), and possibly different results on different platforms.

## NOTE

> The Codename One core library also includes an HTMLParser class at `com.codename1.ui.html.HTMLParser`. This parser is meant to be used as part of the deprecated HTMLComponent class, which is a light-weight web view component that used to be used on platforms that didn’t have a native webview, e.g. J2ME. Now all modern platforms have a native webview, so this component isn’t used much. Additionally the HTMLParser class in that package doesn’t support all HTML, and will fail in strange ways if you try to use it headlessly.

## Further Reading

[XMLParser Javadocs](https://www.codenameone.com/javadoc/com/codename1/xml/XMLParser.html) – Since the output of HTMLParser is the same as XMLParser, you can find some useful examples in the XMLParser javadocs.

### Using the Clipboard

## Problem

You want to copy and paste to and from the system clipboard.

## Solution

Use the `Display.copyToClipboard()` and `Display.getPasteDataFromClipboard()` to copy and paste to/from the system clipboard respectively.

## Example: Copying to the Clipboard

```java
				
					Display.getInstance().copyToClipboard("Some text to copy");

				
			
```

## Example: Copying text from clipboard into Label

```java
				
					Object pasteData = Display.getInstance().getPasteDataFromClipboard();
Label text = new Label();
if (pasteData instanceof String) {
    text.setText((String)pasteData);
} else {
    ToastBar.showInfoMessage("Paste data is not text");
}
				
			
```

## IMPORTANT

> In the Javascript port we are restricted by the browser’s sandbox. We can’t ****just**** access the system clipboard data for security reasons. However, if the user initiates a paste via `Ctrl-V`, `Command-V`, `Edit` → `Paste` etc, the system clipboard contents will be loaded into the Codename One clipboard, so that the next time you call `getPasteDataFromClipboard()`, it will include those contents.
>   
>   
> You can use `Form.addPasteListener(ActionListener)` to be notified when the clipboard contents are updated via this method so that you can respond appropriately - usually by calling `getPasteDataFromClipboard()` and doing something with the data.

## Full Example allowing copy and paste using the Clipboard API

```java
				
					package com.codename1.samples;

import com.codename1.components.ToastBar;
import static com.codename1.ui.CN.*;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.io.Log;
import com.codename1.ui.Toolbar;
import java.io.IOException;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.io.NetworkEvent;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.GridLayout;

public class ClipboardSample {

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

        addNetworkErrorListener(err -> {
            // prevent the event from propagating
            err.consume();
            if(err.getError() != null) {
                Log.e(err.getError());
            }
            Log.sendLogAsync();
            Dialog.show("Connection Error", "There was a networking error in the connection to " + err.getConnectionRequest().getUrl(), "OK", null);
        });
    }

    public void start() {
        if(current != null){
            current.show();
            return;
        }
        Form hi = new Form("Hi World", BoxLayout.y());
        TextField text = new TextField();
        Button copyBtn = new Button("Copy");
        copyBtn.addActionListener(evt->{
            Display.getInstance().copyToClipboard(text.getText());
        });
        Button pasteBtn = new Button("Paste");
        pasteBtn.addActionListener(evt->{
            if ("html5".equalsIgnoreCase(CN.getPlatformName())) {
                // In the browser, we don't have permission, in general, to read from the clipboard
                // but the user can initiate a paste using Ctrl-V or Cmd-V, or Edit > Paste,
                // and the data will be received in the paste listener.
                Dialog.show("Help", "Please key-codes or Edit > Paste to paste content.", "OK", null);
                return;
            }
            handlePaste(text);
        });

        // The paste listener is informed when the user initiates a paste using
        // key-codes or browser menu items (Edit > Paste).  This is currently only
        // used by the Javascript port.
        hi.addPasteListener(evt->{
            handlePaste(text);
        });

        hi.add(text)
                .add(GridLayout.encloseIn(2, copyBtn, pasteBtn));

        hi.show();
    }

    /**
     * Pastes the current clipboard data as text into the given TextArea.
     * @param text The textarea to paste into
     */
    private void handlePaste(TextArea text) {
        Object pasteData = Display.getInstance().getPasteDataFromClipboard();
        if (pasteData instanceof String) {
            text.setText((String)pasteData);
        } else {
            ToastBar.showInfoMessage("Paste data is not text");
        }
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

}
				
			
```

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
