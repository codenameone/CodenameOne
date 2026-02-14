---
title: New Improved Native Google Maps
slug: new-improved-native-google-maps
url: /blog/new-improved-native-google-maps/
original_url: https://www.codenameone.com/blog/new-improved-native-google-maps.html
aliases:
- /blog/new-improved-native-google-maps.html
date: '2017-03-13'
author: Steve Hannah
---

![Header Image](/blog/new-improved-native-google-maps/maps.jpg)

One of the primary use-cases that benefits from our recent improvements for native peer integration, is “map apps”. That is, apps that use native maps in some shape or form. This is an extremely common uses case for mobile apps these days. Codename One has supported native maps for quite some time, but (up until recently), they were limited by a couple of factors:

  1. **Native Widgets Were Always In Front** – Since Google Maps were “native” widgets Codename One couldn’t paint over top of the map. Native widgets were always placed in front of the Codename One UI. We could place markers on the map, and draw paths using MapContainer APIs (which were backed by native code on each platform), but we couldn’t, for example, place a Button over top of the map.
  2. **The Simulator Still Used the Old MapComponent** – The simulator didn’t have support for native maps. It would just use the light-weight Codename One MapComponent, which uses tiles (rather than vector graphics like the native maps), and didn’t behave the same as native maps in some cases. E.g. you **could** draw over top of the MapComponent, which would cause a bit of a surprise if you were counting on that, only to find out after building for iOS that your beautiful buttons were rendered behind the map.

I am happy to announce that on Friday we released an update for the Google maps library the resolves both of these issues.

  1. **You Can Place CN1 Widgets In Front of the Map Now** – Now, you can integrate your native maps into your UI seamlessly with the rest of your Codename One UI. Place your codename one widgets under, over, beside, and around your maps…​ but especially **over** your maps. You don’t need to do anything special for this to happen. The recent native peer improvements cause native peers to **just work**.
  2. **The Simulator Now Behaves More Like Actual Devices** – The simulator now uses an internal BrowserComponent with the GoogleMaps Javascript API, which behaves much more like then native maps on device. (Don’t worry, you don’t have to use any Javascript…​ the Java <→ Javascript interop is all hidden. You just use the `MapContainer` API, and it will take care of the rest.

## Configuration

Adding the native maps library is easy. Just open Codename One Settings, click on “Extensions”, and install the “Google Maps” library.

![Install Google Maps library in Codename One Settings](/blog/new-improved-native-google-maps/google-maps-lib-install.png)

A little bit has changed since the [last time we blogged about native maps](https://www.codenameone.com/blog/mapping-natively.html). Configuration has gotten a little bit easier. You need to provide separate keys for Android, iOS, and Javascript (If you plan to use the Javascript port). The build hints to provide these values are as follows:
    
    
    javascript.googlemaps.key=YOUR_JAVASCRIPT_API_KEY
    android.xapplication=<meta-data android:name="com.google.android.maps.v2.API_KEY" android:value="YOUR_ANDROID_API_KEY"/>
    ios.afterFinishLaunching=[GMSServices provideAPIKey:@"YOUR_IOS_API_KEY"];

Make sure to replace the values YOUR_ANDROID_API_KEY, YOUR_IOS_API_KEY, and YOUR_JAVASCRIPT_API_KEY with the values you  
obtained from the Google Cloud console by following the instructions for [Android](https://developers.google.com/maps/documentation/android/start)  
, for [iOS](https://developers.google.com/maps/documentation/ios/start/), and for [Javascript](https://developers.google.com/maps/documentation/javascript/).

Additionally, if you want to use the Javascript maps in the simulator (highly recommended), you’ll need to provide your JAVASCRIPT_API_KEY as a parameter to the `MapContainer` constructor:
    
    
    MapContainer map = new MapContainer(JAVASCRIPT_API_KEY);

Now you’re ready to use native maps. Here is a sample app that demonstrates adding markers and paths, as well as using `LayeredLayout` to layer Codename One widgets over top of a native map.
    
    
    public class GoogleMapsTestApp {
    
        private static final String HTML_API_KEY = "*********************************";
        private Form current;
    
        public void init(Object context) {
            try {
                Resources theme = Resources.openLayered("/theme");
                UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);
                UIManager.getInstance().getLookAndFeel().setMenuBarClass(SideMenuBar.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
        public void start() {
            if (current != null) {
                current.show();
                return;
            }
            Form hi = new Form("Native Maps Test");
            hi.setLayout(new BorderLayout());
            final MapContainer cnt = new MapContainer(HTML_API_KEY);
    
            Button btnMoveCamera = new Button("Move Camera");
            btnMoveCamera.addActionListener(e->{
                cnt.setCameraPosition(new Coord(-33.867, 151.206));
            });
            Style s = new Style();
            s.setFgColor(0xff0000);
            s.setBgTransparency(0);
            FontImage markerImg = FontImage.createMaterial(FontImage.MATERIAL_PLACE, s, Display.getInstance().convertToPixels(3));
    
            Button btnAddMarker = new Button("Add Marker");
            btnAddMarker.addActionListener(e->{
    
                cnt.setCameraPosition(new Coord(41.889, -87.622));
                cnt.addMarker(
                        EncodedImage.createFromImage(markerImg, false),
                        cnt.getCameraPosition(),
                        "Hi marker",
                        "Optional long description",
                         evt -> {
                                 ToastBar.showMessage("You clicked the marker", FontImage.MATERIAL_PLACE);
                         }
                );
    
            });
    
            Button btnAddPath = new Button("Add Path");
            btnAddPath.addActionListener(e->{
    
                cnt.addPath(
                        cnt.getCameraPosition(),
                        new Coord(-33.866, 151.195), // Sydney
                        new Coord(-18.142, 178.431),  // Fiji
                        new Coord(21.291, -157.821),  // Hawaii
                        new Coord(37.423, -122.091)  // Mountain View
                );
            });
    
            Button btnClearAll = new Button("Clear All");
            btnClearAll.addActionListener(e->{
                cnt.clearMapLayers();
            });
    
            cnt.addTapListener(e->{
                TextField enterName = new TextField();
                Container wrapper = BoxLayout.encloseY(new Label("Name:"), enterName);
                InteractionDialog dlg = new InteractionDialog("Add Marker");
                dlg.getContentPane().add(wrapper);
                enterName.setDoneListener(e2->{
                    String txt = enterName.getText();
                    cnt.addMarker(
                            EncodedImage.createFromImage(markerImg, false),
                            cnt.getCoordAtPosition(e.getX(), e.getY()),
                            enterName.getText(),
                            "",
                            e3->{
                                    ToastBar.showMessage("You clicked "+txt, FontImage.MATERIAL_PLACE);
                            }
                    );
                    dlg.dispose();
                });
                dlg.showPopupDialog(new Rectangle(e.getX(), e.getY(), 10, 10));
                enterName.startEditingAsync();
            });
    
            Container root = LayeredLayout.encloseIn(
                    BorderLayout.center(cnt),
                    BorderLayout.south(
                            FlowLayout.encloseBottom(btnMoveCamera, btnAddMarker, btnAddPath, btnClearAll)
                    )
            );
    
            hi.add(BorderLayout.CENTER, root);
            hi.show();
    
        }
    
        public void stop() {
            current = Display.getInstance().getCurrent();
        }
    
        public void destroy() {
        }
    }

![Native Maps Demo App Screensshot](/blog/new-improved-native-google-maps/google-maps-test-app-screenshot.png)

Figure 1. Native Maps Demo App Screensshot

You can view the Javascript version of this app [here](https://www.codenameone.com/demos/GoogleMaps/index.html).

Read more about the Google Maps library in its [Github repository](https://github.com/codenameone/codenameone-google-maps).

## Interaction Dialog vs Dialog

When using native peers, you’ll find it preferable to use `InteractionDialog` rather than `Dialog` whenever possible. This is because `Dialog` is actually a `Form`, and Codename One uses a “trick” (displaying a screen shot) to be able to display the existing form underneath it. InteractionDialog, on the other hand, is a light-weight component that acts like a dialog, but is rendered in the LayeredPane in front of the elements of the current form. This plays with the new native peers quite nicely.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Diamond** — March 16, 2017 at 2:09 pm ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23380))

> Diamond says:
>
> Hi Steve,
>
> There is an exception in the javascript bridge on simulator when calling map.getMaxZoom(), see below:
>
> [EDT] 0:0:19,357 – Exception: java.lang.RuntimeException – Waited too long for browser bridge  
> java.lang.RuntimeException: Waited too long for browser bridge  
> at com.codename1.googlemaps.MapContainer$BrowserBridge.waitForReady([MapContainer.java](<http://MapContainer.java>):258)  
> at com.codename1.googlemaps.MapContainer$BrowserBridge.access$700([MapContainer.java](<http://MapContainer.java>):221)  
> at com.codename1.googlemaps.MapContainer.getMaxZoom([MapContainer.java](<http://MapContainer.java>):531)


### **shannah78** — March 16, 2017 at 4:18 pm ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23192))

> shannah78 says:
>
> Please file an issue with a test case. I use getMaxZoom() in the GoogleMapsTest app and it works fine. So could be a race condition, or something specific to your test that is triggering this problem.


### **Blessing Mahlalela** — March 18, 2017 at 7:41 pm ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23141))

> Blessing Mahlalela says:
>
> Hi Steve, Thanks for this lib! Works great on iOS & Android, on JS Port there is an issue. I have filed it: [https://github.com/codename…](<https://github.com/codenameone/codenameone-google-maps/issues/13>)


### **Patrick Hills** — March 24, 2017 at 2:57 pm ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23268))

> Patrick Hills says:
>
> Hello Shai please how to get nearby places around my current location and if possible see their movement in real time


### **Shai Almog** — March 25, 2017 at 5:49 am ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23436))

> Shai Almog says:
>
> You can get location via the location API. Finding nearby elements would require a webservice like googles location API. I dabbled with it a bit here: [https://www.codenameone.com…](<https://www.codenameone.com/blog/dynamic-autocomplete.html>)


### **Blessing Mahlalela** — March 26, 2017 at 3:10 pm ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23472))

> Blessing Mahlalela says:
>
> Hi Steve, the MapContainer addPointerReleasedListener is not working on my side. I tried this code:
>
> mapContainer.addPointerReleasedListener((ActionListener) (ActionEvent evt) -> {  
> Log.p(“Lat released= ” + lat, Log.DEBUG);  
> Log.p(“Long released= ” + lng, Log.DEBUG);  
> });
>
> Note the lat & lng values are obtained inside addMapListener which is working well.


### **Julio Valeriron Ochoa** — May 26, 2021 at 6:28 pm ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-24458))

> Julio Valeriron Ochoa says:
>
> Hello steve, please provide a way to addPointerReleasedListener to MapContainer


### **Patrick Hills** — March 27, 2017 at 5:39 pm ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23179))

> Patrick Hills says:
>
> Hello Shai have gone through the tutorial and is working, i can do my live searches of places from the web service Google API but i want it limited to search nearby places around me OR the user (that the user current location,to search around) and not the whole world places.  
> Secondly the search is a little slow.  
> So what should i do? Especially the former question, ie. searching places of my current location  
> Thank you.


### **shannah78** — March 27, 2017 at 5:42 pm ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23030))

> shannah78 says:
>
> Since the map is a native component, these lightweight events aren’t supported. Use addTapListener() to detect when the user taps on the map.


### **Blessing Mahlalela** — March 27, 2017 at 7:16 pm ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-21582))

> Blessing Mahlalela says:
>
> Hi Steve, Thanks! addTapListener() achieves what I am trying to do.


### **Mounir** — May 3, 2017 at 6:47 am ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23382))

> Mounir says:
>
> Hi , I’m facing a problem with your code , it keeps generation an illegal argument exception width(0) and height(0) cannot be <=0  
> couldn’t find the root of the issue
>



### **Shai Almog** — May 4, 2017 at 7:33 am ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23551))

> Shai Almog says:
>
> Make sure the map is in the center of a border layout in the form and not an absolute center mode or some other special case
>



### **Arun raj** — August 25, 2017 at 5:54 am ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23728))

> Arun raj says:
>
> how to add search functionality to the map component along with auto complete
>



### **Shai Almog** — August 26, 2017 at 4:51 am ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23590))

> Shai Almog says:
>
> See the link I provided above [https://www.codenameone.com…](<https://www.codenameone.com/blog/dynamic-autocomplete.html>)
>
> This isn’t a part of the map API from Google it’s something you need to do via a rest request.
>



### **Synapsido** — September 12, 2018 at 2:50 am ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23983))

> Synapsido says:
>
> I got this error, trying to run app in smatphone: [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/6cf975d4eb5625da8782a90f74a8ad860093873a8f43458e7059c5b157125cbd.png>)
>



### **Shai Almog** — September 12, 2018 at 4:09 am ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23956))

> Shai Almog says:
>
> Try to get the stack trace from the device either by connecting a cable and looking through DDMS or through the new native logging cn1lib.
>



### **Synapsido** — September 18, 2018 at 3:00 am ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23800))

> Synapsido says:
>
> I solved the problem…  
> But this example runs without problems in the simulator, buttons work good, but running on the phone, buttons isn’t work… whats the problem…?
>



### **Shai Almog** — September 20, 2018 at 6:51 am ([permalink](https://www.codenameone.com/blog/new-improved-native-google-maps.html#comment-23708))

> Shai Almog says:
>
> It’s hard to tell from that description. How did you add the buttons, what did you do exactly?  
> Do you see an error in the console etc.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
