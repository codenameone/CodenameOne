---
title: Obfuscated Constants
slug: obfuscated-constants
url: /blog/obfuscated-constants/
original_url: https://www.codenameone.com/blog/obfuscated-constants.html
aliases:
- /blog/obfuscated-constants.html
date: '2017-02-14'
author: Shai Almog
---

![Header Image](/blog/obfuscated-constants/security.jpg)

One of the first things a hacker will do when compromising an app is look at it. E.g. if I want to exploit a bank’s login UI I would look at the label next to the login and then search for it in the decompiled code. So if the UI has the String “enter user name and password” I can search for that.

It won’t lead directly to a hack or exploit but it will show you the approximate area of the code where we should look and it makes the first step that much easier. Obfuscation helps as it removes descriptive method names but it can’t hide the Strings we use in constants. So if an app has a secret encoding it even slightly can make a difference…​

Notice that this is a temporary roadblock as any savvy hacker would compile the app and connect a debugger eventually (although this is blocked in release builds) and would be able to inspect values of variables/flow. But the road to reverse engineering the app would be harder even with a simple xor obfuscation.

__ |  I’m not calling this encoding or encryption since it is neither. It’s a simple obfuscation of the data   
---|---  
  
We thought about adding this to all the constants in the application as an additional security measure but this would cost in size/performance so eventually we decided to leave this to you. We added two simple methods to the `Util` class:
    
    
    public static String xorDecode(String s);
    public static String xorEncode(String s);

They use a simple xor based obfuscation to make a String less readable. E.g. if you have code like this:
    
    
    private static final String SECRET = "Don't let anyone see this....";

And you’d obviously be concerned about secret, then this would make it slightly harder to find:
    
    
    // Don't let anyone see this....
    private static final String SECRET = Util.xorDecode("RW1tI3Ema219KmpidGFhdTFhdnE1Yn9xajQ1MjM=");

Notice that this is **not secure** , if you have a crucial value that must not be found you need to store it in the server. There is no alternative as everything that is sent to the client can be compromised by a determined hacker

__ |  Use the comment to help you find the string in the code   
---|---  
  
Our builtin user specific constants will now be obfuscated with this method, e.g. normally an app built with Codename One carries some internal data such as the user who built the app etc. This will be obfuscated now. We built this small app to encode strings easily so we can copy and paste them into our app easily:
    
    
    Form hi = new Form("Encoder", BoxLayout.y());
    TextField bla = new TextField("", "Type Text Here", 20, TextArea.ANY);
    TextArea encoded = new TextArea();
    SpanLabel decoded = new SpanLabel();
    hi.addAll(bla, encoded, decoded);
    bla.addDataChangedListener((a, b) -> {
        String s = bla.getText();
        String e = Util.xorEncode(s);
        encoded.setText(e);
        decoded.setText(Util.xorDecode(e));
        hi.getContentPane().animateLayout(100);
    });
    
    hi.show();

This allows you to type in the first text field and the second text area shows the encoded result. We used a text area so copy/paste would be easy.

For your convenience check out the demo below that encodes strings. You can copy and paste the encoded String to your application. It was built using the JavaScript port of Codename One and can thus run in the browser.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
