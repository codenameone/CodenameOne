---
title: Ratatouille's Restaurant In Code
slug: ratatouilles-restaurant-in-code
url: /blog/ratatouilles-restaurant-in-code/
original_url: https://www.codenameone.com/blog/ratatouilles-restaurant-in-code.html
aliases:
- /blog/ratatouilles-restaurant-in-code.html
date: '2015-12-01'
author: Shai Almog
---

![Header Image](/blog/ratatouilles-restaurant-in-code/ratatouilles-restaurant-in-code.png)

One of my favorite Pixar movies is Ratatouille, maybe because I’m such a glutton. So when I was thinking  
about the next tutorial/demo and the idea of a restaurant app came up I knew it had to be based on Ratatouille.  
This is a relatively simple demo that can literally fit in a single blog post all the way thru and the real cool  
thing about it is that you can try the [JavaScript version right now](/demos/Restaurant/) from  
your browser without compiling anything… 

The demo shows off how attractive a really simple UI can be with the right font face and a few images/transparency  
effects. This is further emphasized via the nice expansion animation when clicking on an image. We also  
demonstrate navigation, maps and integration with table ordering systems as part of the demo (haven’t  
tested the latter since its only supposed to work in the states). You can check out the  
[full source code of the  
demo in github](https://github.com/codenameone/RestaurantDemo). 

#### The Main Class

The main class is pretty trivial with mostly boilerplate code: 
    
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        new MainUI(theme).show();
    }
    

This effectively says that all the logic is in the MainUI class. 

#### The Main UI Class

This class is slightly larger so we’ll break it down to obvious pieces of code: 
    
    
    public MainUI(Resources theme) {
        super(RESTAURANT_NAME);
        this.theme = theme;
        Toolbar t = new Toolbar();
        setToolBar(t);
        t.setScrollOffUponContentPane(true);
    
        Label rat = new Label(theme.getImage("round_logo.png"));
        rat.setTextPosition(Label.BOTTOM);
        rat.setText(RESTAURANT_NAME);
        rat.setUIID("SideMenuLogo");
        t.addComponentToSideMenu(rat);
        setLayout(new BorderLayout());
    
        Container dishes = createDishesContainer();
        addComponent(BorderLayout.CENTER, dishes);
        revalidate();
    
        Style iconStyle = UIManager.getInstance().getComponentStyle("SideCommandIcon");
    
        t.addCommandToSideMenu(new Command("Menu", FontImage.create(" ue93f ", iconStyle)) {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showDishesContainer();
            }
        });
        if(INCLUDE_RESERVATIONS) {
            t.addCommandToSideMenu(new Command("Reservation", FontImage.create(" ue838 ", iconStyle)) {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    Display.getInstance().execute("reserve://opentable.com/" + OPEN_TABLE_RESERVATION_ID +  "?partySize=2");
                }
            });
        }
        t.addCommandToSideMenu(new Command("Find Us", FontImage.create(" ue8d5 ", iconStyle)) {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showMap();
            }
        });
        t.addCommandToSideMenu(new Command("Contact Us", FontImage.create(" ue86b ", iconStyle)) {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showContactUs();
            }
        });
        t.addCommandToSideMenu(new Command("Navigate", FontImage.create(" ue85b ", iconStyle)) {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Display.getInstance().openNativeNavigationApp(RESTAURANT_LATITUDE, RESTAURANT_LONGITUDE);
            }
        });
    }

That’s pretty much the whole application. You’ll notice that commands are added to the side menu with icon  
fonts, that demo predated our new Material design icons and new native fonts so it uses neither. Newer  
code would be smaller and simpler.  
The “rat” label above shows the rounded version of the logo on the side menu that slides out as such: 

![Sidemenu](/blog/ratatouilles-restaurant-in-code/slide-2.png)

The main UI itself is implemented in the `createDishesContainer` call: 
    
    
    private Container createDishesContainer() {
        Container cnt = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        cnt.setScrollableY(true);
    
        // allows elements to slide into view
        for(Dish d : DISHES) {
            Component dish = createDishComponent(d);
            cnt.addComponent(dish);            
        }        
        return cnt;
    }
    
    private Container createDishComponent(Dish d) {
        Image img = theme.getImage(d.getImageName());
        Container mb = new Container(new BorderLayout());
        mb.getUnselectedStyle().setBgImage(img);
        mb.getSelectedStyle().setBgImage(img);
        mb.getPressedStyle().setBgImage(img);
        mb.getUnselectedStyle().setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FILL);
        mb.getSelectedStyle().setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FILL);
        mb.getPressedStyle().setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FILL);
    
        Container box = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        Button title = new Button(d.getDishName());
        title.setUIID("DishTitle");
        Label highlights = new Label(d.getHighlights());
        TextArea details = new TextArea(d.getFullDescription());
        details.setUIID("DishBody");
        highlights.setUIID("DishBody");
        Label price = new Label(d.getPrice());
        price.setUIID("DishPrice");
        box.addComponent(title);
        box.addComponent(highlights);
    
        Container boxAndPrice = new Container(new BorderLayout());
        boxAndPrice.addComponent(BorderLayout.CENTER, box);
        boxAndPrice.addComponent(BorderLayout.EAST, price);
        mb.addComponent(BorderLayout.SOUTH, boxAndPrice);
    
        mb.setLeadComponent(title);
    
        title.addActionListener((e) -> {
            if(highlights.getParent() != null) {
                box.removeComponent(highlights);
                box.addComponent(details);
            } else {
                box.removeComponent(details);
                box.addComponent(highlights);
            }
            mb.getParent().animateLayout(300);
        });
        return mb;
    }

Here we just loop and create individual dish objects then create the expand/contract effect. This is done by  
adding/removing the details component and invoking the `animateLayout` method.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Jaanus Hansen** — December 2, 2015 at 8:50 pm ([permalink](https://www.codenameone.com/blog/ratatouilles-restaurant-in-code.html#comment-22444))

> Jaanus Hansen says:
>
> Well, I suggest to remove the JavaScript demo. It is not performing well on my Android LG G2, I am afraid, that it makes a wrong impression, that CN1 is very slow and sometimes even buggy. I think, that you should use for demos Android and iOS apps, where CN1 works very well.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fratatouilles-restaurant-in-code.html)


### **bryan** — December 2, 2015 at 10:36 pm ([permalink](https://www.codenameone.com/blog/ratatouilles-restaurant-in-code.html#comment-22378))

> bryan says:
>
> Javascript demo doesn’t work on FF 42.0 (Linux 3.13.0-71-generic #114-Ubuntu SMP Tue Dec 1 02:34:22 UTC 2015 x86_64 x86_64 x86_64 GNU/Linux). The rat icon displays then screen goes blank.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fratatouilles-restaurant-in-code.html)


### **Shai Almog** — December 3, 2015 at 3:31 am ([permalink](https://www.codenameone.com/blog/ratatouilles-restaurant-in-code.html#comment-22562))

> Shai Almog says:
>
> I’m testing on firefox (prefer it over chrome). It works for me on a Mac. There is a blank screen between the rat image and the full initialization of the UI but it should be there only for a couple of seconds, can you try reloading?  
> Is your connection very slow?  
> The app effectively runs locally so all data and resources are fetched before it runs.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fratatouilles-restaurant-in-code.html)


### **Shai Almog** — December 3, 2015 at 3:36 am ([permalink](https://www.codenameone.com/blog/ratatouilles-restaurant-in-code.html#comment-22570))

> Shai Almog says:
>
> I think it shows the drawbacks of JavaScript which is part of the point.  
> I wasn’t aiming for people to use it on the device but rather in the desktop to preview the UI.
>
> Notice we provide an Android APK as well in the demos section but you need external sources etc…
>
> I ran it on chrome on my oneplus one and it performed reasonably well. Not as fast as the native version but totally acceptable as a fallback option. Expansion is slightly slow the first time around as the images get lazily scaled but once that happens its smooth. The side menu is pretty slow on javascript though.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fratatouilles-restaurant-in-code.html)


### **bryan** — December 3, 2015 at 4:28 am ([permalink](https://www.codenameone.com/blog/ratatouilles-restaurant-in-code.html#comment-22575))

> bryan says:
>
> Tried reloading – still doesn’t like me. Download speed is 7.26Mbps. Get these errors:
>
> unreachable code after return statement classes.js:7384:18  
> InvalidStateError fontmetrics.js:501:0  
> “Google Maps API warning: InvalidKey: [https://developers.google.c…](<https://developers.google.com/maps/documentation/javascript/error-messages>)” util.js:30:57  
> TypeError: $t is null classes.js:1940:16  
> uncaught exception: [object DOMError]  
> —————————–  
> Works OK in Chrome, except the location and ordering menu options cause the app to hang as with Android version:
>
> ‘window.webkitStorageInfo’ is deprecated. Please use ‘navigator.webkitTemporaryStorage’ or ‘navigator.webkitPersistentStorage’ instead.  
> [https://www.codenameone.com…](<https://www.codenameone.com/demos/Restaurant/assets/material-design-font.ttf>) Failed to load resource: the server responded with a status of 404 (OK)  
> [www.codenameone.com/:1](<http://www.codenameone.com/:1>) Failed to decode downloaded font: data:font/truetype;base64,PCFET0NUWVBFIGh0bWw+CjxodG1sIGRpcj0ibHRyIiBsYW5nP…5vdCBGb3VuZCcpOyAgCiAgICAgICAgPC9zY3JpcHQ+CiAgICA8L2JvZHk+CgoKPC9odG1sPg==  
> [www.codenameone.com/:1](<http://www.codenameone.com/:1>) OTS parsing error: invalid version tag  
> [maps.googleapis.com/maps-ap…](<http://maps.googleapis.com/maps-api-v3/api/js/23/2/intl/en_gb/util.js:30>) Google Maps API warning: InvalidKey: [https://developers.google.c…](<https://developers.google.com/maps/documentation/javascript/error-messages>)  
> runtime.js:486 Uncaught TypeError: Cannot read property ‘maxZoom’ of null  
> [maps.googleapis.com/maps-ap…](<http://maps.googleapis.com/maps-api-v3/api/js/23/2/intl/en_gb/common.js:269>) Failed to decode downloaded font: data:font/truetype;base64,PCFET0NUWVBFIGh0bWw+CjxodG1sIGRpcj0ibHRyIiBsYW5nP…5vdCBGb3VuZCcpOyAgCiAgICAgICAgPC9zY3JpcHQ+CiAgICA8L2JvZHk+CgoKPC9odG1sPg==  
> [maps.googleapis.com/maps-ap…](<http://maps.googleapis.com/maps-api-v3/api/js/23/2/intl/en_gb/common.js:269>) OTS parsing error: invalid version tag  
> js:68 Failed to decode downloaded font: data:font/truetype;base64,PCFET0NUWVBFIGh0bWw+CjxodG1sIGRpcj0ibHRyIiBsYW5nP…5vdCBGb3VuZCcpOyAgCiAgICAgICAgPC9zY3JpcHQ+CiAgICA8L2JvZHk+CgoKPC9odG1sPg==  
> js:68 OTS parsing error: invalid version tag  
> [www.codenameone.com/:1](<http://www.codenameone.com/:1>) Failed to decode downloaded font: data:font/truetype;base64,PCFET0NUWVBFIGh0bWw+CjxodG1sIGRpcj0ibHRyIiBsYW5nP…5vdCBGb3VuZCcpOyAgCiAgICAgICAgPC9zY3JpcHQ+CiAgICA8L2JvZHk+CgoKPC9odG1sPg==  
> [www.codenameone.com/:1](<http://www.codenameone.com/:1>) OTS parsing error: invalid version tag  
> [www.codenameone.com/:1](<http://www.codenameone.com/:1>) Failed to decode downloaded font: data:font/truetype;base64,PCFET0NUWVBFIGh0bWw+CjxodG1sIGRpcj0ibHRyIiBsYW5nP…5vdCBGb3VuZCcpOyAgCiAgICAgICAgPC9zY3JpcHQ+CiAgICA8L2JvZHk+CgoKPC9odG1sPg==  
> [www.codenameone.com/:1](<http://www.codenameone.com/:1>) OTS parsing error: invalid version tag  
> ——————–  
> I downloaded the source and created an Android app, and it works nicely. The location and ordering stuff stuff doesn’t work (I’m not in USA) – it makes the side menu go a bit weird when it hangs, so it might be worthwhile highlighting that this may not work less people think something has barfed.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fratatouilles-restaurant-in-code.html)


### **Shai Almog** — December 3, 2015 at 5:36 am ([permalink](https://www.codenameone.com/blog/ratatouilles-restaurant-in-code.html#comment-21549))

> Shai Almog says:
>
> Thanks we’re looking into it. Might be a Linux firefox issue (damn web technologies). We checked with two macs and this works there.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fratatouilles-restaurant-in-code.html)


### **Shai Almog** — December 4, 2015 at 8:39 am ([permalink](https://www.codenameone.com/blog/ratatouilles-restaurant-in-code.html#comment-22480))

> Shai Almog says:
>
> Steve made some fixes here and we just deployed an update. It might take a couple of hours for cloud flair to flush the caches. Can you check this and see if it resolves the issues and if not can you verify that the error messages changed?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fratatouilles-restaurant-in-code.html)


### **bryan** — December 6, 2015 at 4:04 am ([permalink](https://www.codenameone.com/blog/ratatouilles-restaurant-in-code.html#comment-22581))

> bryan says:
>
> I think I’ve got some weirdness my end. On my development system, it still does not work, but on another system (same OS version, same FF version) it works. Ahh… just found the “problem”. On my devel system I always use “Private Browsing” with ‘Tracking Protection” turned on. With a regular browser window it works fine. Sorry for confusion.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fratatouilles-restaurant-in-code.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
