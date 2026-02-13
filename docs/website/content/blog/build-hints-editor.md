---
title: Build-Hints Editor
slug: build-hints-editor
url: /blog/build-hints-editor/
original_url: https://www.codenameone.com/blog/build-hints-editor.html
aliases:
- /blog/build-hints-editor.html
date: '2022-03-01'
description: In the latest plugin update, we’ve added a Build-Hints Editor that accessible
  from a menu item inside the Codename One simulator. This is intended to make it
  easier to edit your app’s build hints, and also inform you of build hints that you
  may need to configure.
---

In the latest plugin update, we’ve added a Build-Hints Editor that accessible from a menu item inside the Codename One simulator. This is intended to make it easier to edit your app’s build hints, and also inform you of build hints that you may need to configure. 

![codename one - build hint editor](/blog/build-hints-editor/cn1-build-hint-editor-1024x536.jpg)

Unlike the existing mechanisms for editing build hints (in Codename One Settings, or directly editing the codenameone_settings.properties file), this editor can integrate the requirements of installed cn1libs to help guide you through their configuration.

> The latest release of the Google Maps cn1lib is the first module to make use of the build-hint editor. If you are developing a cn1lib that requires users to set specific build hints for its proper functioning, then you should follow the instructions in this article to integrate your library into the build hints editor. 

### Usage Instructions

In order to use the build-hints editor, you should launch your app in the Codename One simulator.

Once your app is running, select  _Tools_ >  _Edit Build Hints…​_.

![](/blog/build-hints-editor/edit-build-hints-menu.png)

This will launch the build-hints editor in a new window as shown below.

![](/blog/build-hints-editor/build-hints-editor.png)

## TIP

> The "Google Maps" tab appears in the above screenshot because the Google Maps cn1lib is installed in this app. If your app doesn’t use Google Maps, then it won’t include this tab. 

One nice feature of this editor vs using Codename One settings is that some build hints include help text and example content, as well as buttons to perform complementary actions. For example, take a look at the field for the `ios.afterFinishLaunching` build hint:

![](/blog/build-hints-editor/ios-afterfinishlaunching-fields.png)

It includes a text field for entering the build hint, some help text to let you know what this build hint is for and some example input to show you the proper format.

In this case it also includes a “Get Key” button that will open the webpage for creating an API key.

The old way to set up Google maps was to follow the [README](https://github.com/codenameone/codenameone-google-maps#readme), and look manually walk through the required steps.

## IMPORTANT

> You should still read the installation instructions in the README for all cn1libs you are using. The Build-hints editor just provides a simpler way to deal with the build-hints portion of this. 

After making changes to your build hints, press  _Apply_ or  _Save_. The difference is that  _Apply_ won’t close the build-hints editor.  _Save_ will.

### Instructions for CN1lib Developers

If you are developing a cn1lib that requires users to add custom build hints to their app, consider adding a tab for your cn1lib in the build-hints editor. This can be done by defining some Display properties in your cn1lib.

## NOTE

> You can also define java.lang.System properties directly if you are working in JavaSE native code. When running in the simulator, calling CN1.setProperty("foo", "bar") will ultimately call System.setProperty("foo", "bar") under the hood. 

Before I walk through the intricacies of the syntax, let’s look at the [real-world example of the Google Maps library](https://github.com/codenameone/codenameone-google-maps/blob/13da47a7051084e30a1ce8fac0b762b898bfac72/GoogleMaps/javase/src/main/java/com/codename1/googlemaps/InternalNativeMapsImpl.java).
    
    
    				
    					System.setProperty(
            "codename1.arg.{{#googlemaps#javascript.googlemaps.key}}.label",
            "Javascript API Key"
    );
    System.setProperty(
            "codename1.arg.{{#googlemaps#javascript.googlemaps.key}}.description",
            "Please enter your Javascript API key."
    );
    System.setProperty(
            "codename1.arg.{{#googlemaps#javascript.googlemaps.key}}.link",
            "https://developers.google.com/maps/documentation/javascript/get-api-key Get Key"
    );
    
    System.setProperty(
            "codename1.arg.{{#googlemaps#android.xapplication}}.label",
            "android.xapplication"
    );
    System.setProperty(
            "codename1.arg.{{#googlemaps#android.xapplication}}.description",
            "Your android.xapplication build hint must inject your Android API key"
    );
    System.setProperty(
            "codename1.arg.{{#googlemaps#android.xapplication}}.hint",
            ""
    );
    System.setProperty(
            "codename1.arg.{{#googlemaps#android.xapplication}}.link",
            "https://developers.google.com/maps/documentation/android-sdk/get-api-key Get Key"
    );
    
    System.setProperty(
            "codename1.arg.{{#googlemaps#ios.afterFinishLaunching}}.label",
            "ios.afterFinishLaunching"
    );
    System.setProperty(
            "codename1.arg.{{#googlemaps#ios.afterFinishLaunching}}.description",
            "Your ios.afterFinishLaunching hint must inject your IOS API Key"
    );
    System.setProperty(
            "codename1.arg.{{#googlemaps#ios.afterFinishLaunching}}.hint",
            "[GMSServices provideAPIKey:@\"YOUR_IOS_API_KEY\"];"
    );
    System.setProperty(
            "codename1.arg.{{#googlemaps#ios.afterFinishLaunching}}.link",
            "https://developers.google.com/maps/documentation/ios-sdk/get-api-key Get Key"
    );
    
    System.setProperty(
            "codename1.arg.{{#googlemaps#android.min_sdk_version}}.label",
            "Android Minimum SDK Version"
    );
    System.setProperty(
            "codename1.arg.{{#googlemaps#android.min_sdk_version}}.description",
            "Your Android Minimum SDK Version must be at least 19"
    );
    System.setProperty(
            "codename1.arg.{{#googlemaps#android.min_sdk_version}}.hint",
            "19"
    );
    
    System.setProperty(
            "codename1.arg.{{@googlemaps}}.label",
            "Google Maps"
    );
    System.setProperty(
            "codename1.arg.{{@googlemaps}}.description",
            "The following build hints are required for the Google Maps cn1lib to operate correctly."
    );
    				
    			

Allow me to unpack this a little bit. By defining these properties, the build-hints editor will know to generate the appropriate fields for the user to edit these build hints.

The general syntax is:
    
    
    codename1.arg.{{ BUILD_HINT_NAME }}.PROPERTY_NAME

where `BUILD_HINT_NAME` is the name of the build hint that this pertains to, and `PROPERTY_NAME` is the specific metadata property that we are setting.

Some property names that you can define include:

## label

The label for this build hint as it will be rendered in the built-hints editor.

## description

The help text. A short blurb to let the user know what the build hint is for.

## hint

Some example input for this build hint.

## link

An optional link to learn more about this build hint, or perform an associated action. This will be rendered as a button next to the help text for the build hint. You can optionally include a label for this button after the URL by leaving a “space” between them. E.g. “http://example.com/foobar Go to Foobar”

## type

The widget type to use for editing this build hint. Supported values include “textfield”, “textarea”, “checkbox”, and “select”

## values

A list of options to use when the  _type_ property is “select”. Values should be delimited by a character, and the delimiter is defined by the trailing character. E.g. `red, green, blue,` or `red; green; blue;`. The import part is that the delimiter is included at the  _end_ as the build-hints editor will check the last character to find out what the delimiter is.

With this in mind, let’s take a look at the `ios.afterFinishLaunching` field settings:
    
    
    				
    					System.setProperty(
            "codename1.arg.{{#googlemaps#ios.afterFinishLaunching}}.label",
            "ios.afterFinishLaunching"
    );
    System.setProperty(
            "codename1.arg.{{#googlemaps#ios.afterFinishLaunching}}.description",
            "Your ios.afterFinishLaunching hint must inject your IOS API Key"
    );
    System.setProperty(
            "codename1.arg.{{#googlemaps#ios.afterFinishLaunching}}.hint",
            "[GMSServices provideAPIKey:@\"YOUR_IOS_API_KEY\"];"
    );
    System.setProperty(
            "codename1.arg.{{#googlemaps#ios.afterFinishLaunching}}.link",
            "https://developers.google.com/maps/documentation/ios-sdk/get-api-key Get Key"
    );
    				
    			

## NOTE

> The `googlemaps prefix to the build hint causes the field to be rendered inside its own "Google Maps" tab of the build-hints editor. 

![](/blog/build-hints-editor/build-hint-properties.png)

### Configuring Custom Tabs

Notice that the Google maps build hints are all rendered inside a nice “Google Maps” tab. This is accomplished by defining a “googlemaps” group, and then assigning each property to that group. The portion that defines the “googlemaps” group is shown below:
    
    
    				
    					System.setProperty(
            "codename1.arg.{{@googlemaps}}.label",
            "Google Maps"
    );
    System.setProperty(
            "codename1.arg.{{@googlemaps}}.description",
            "The following build hints are required for the Google Maps cn1lib to operate correctly."
    );
    				
    			

The property name follows the same syntax as for regular properties: “codename1.arg.{{ @GROUP_NAME }}.PROPERTY_NAME”, except that the `GROUP_NAME` here must be prefixed with `@`. This signifies that the property pertains to the group as a whole rather than a particular build hint.

The second part of ensuring that properties render in the  _googlemaps_ group is that the build hint name needs to be prefixed with `GROUPNAME`. In this case it would be `googlemaps`.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
