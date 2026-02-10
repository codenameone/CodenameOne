---
title: Rating Widget
slug: rating-widget
url: /blog/rating-widget/
original_url: https://www.codenameone.com/blog/rating-widget.html
aliases:
- /blog/rating-widget.html
date: '2016-09-06'
author: Shai Almog
---

![Header Image](/blog/rating-widget/rating-widget.png)

The two key factors to improve any product are: get help from your biggest fans & learn from your detractors.  
Obviously there is a lot of nuance to that wide reaching advice…​  
Rating widgets embody this advice. They prompt a user for a rating. If it’s good we ask him to review the app in  
the appstore and thus bring more users. If it’s bad we ask him to tell us why!

For years people asked us how to build such a tool and we gave a general guideline, no one contributed it back.  
So we decided it’s time to build it ourselves:

![The initial prompt of the widget](/blog/rating-widget/rate-widget-1.png)

Figure 1. The initial prompt of the widget

![If the rating is 4 or higher it prompts for store rating](/blog/rating-widget/rate-widget-2.png)

Figure 2. If the rating is 4 or higher it prompts for store rating

![If the rating is lower than 4 it asks for feedback](/blog/rating-widget/rate-widget-3.png)

Figure 3. If the rating is lower than 4 it asks for feedback

### How Does it Work?

The widget is really simple and relies on our previous [star rating widget code](/blog/its-full-of-stars-terse-commands.html).

To use it we need the appstore URL first, there are other ways to do it but the simplest one is:
    
    
    public  String getAppstoreURL() {
        if(Display.getInstance().getPlatformName().equals("ios")) {
            return "https://itunes.apple.com/us/app/kitchen-sink-codename-one/id635048865";
        }
        if(Display.getInstance().getPlatformName().equals("and")) {
            return "https://play.google.com/store/apps/details?id=com.codename1.demos.kitchen";
        }
        return null;
    }

Once we have that we can bind the widget in the `start()` method:
    
    
    public void start(){
        if(getAppstoreURL() != null) {
            RatingWidget.bindRatingListener(180000, getAppstoreURL(), "[[email protected]](/cdn-cgi/l/email-protection)");
        }
        if(currentForm != null && !(currentForm instanceof Dialog)) {
            currentForm.show();
            return;
        }
        showSplashAnimation();
    }

Notice that the bind is invoked every time that start is invoked, which will happen also after an app is minimized.

We time the prompt to 3 minutes (180 seconds) but if the user minimizes the app after a minute we want to pause the  
counting…​ This allows us to resume the counting.

That is why the stop method includes this line of code:
    
    
    RatingWidget.suspendRating();

That way the rating countdown is suspended when the app is minimized.

### The Widget

The widget itself is really simple, it uses a thread to wait until the right time although we use notify to wakeup in case  
of suspend. This is more efficient than using a sleep loop.

You can check out the  
[full up to date code](https://github.com/codenameone/KitchenSink/blob/master/src/com/codename1/demos/kitchen/RatingWidget.java)  
in the [kitchen sink github project](https://github.com/codenameone/KitchenSink/).

Pasted below for your convenience is the current version:
    
    
    public class RatingWidget {
        private static RatingWidget instance;
        private boolean running;
    
        private int timeForPrompt;
    
        private String appstoreUrl;
        private String supportEmail;
    
        private RatingWidget() {
        }
    
        private void init(String appstoreUrl, String supportEmail) {
            this.appstoreUrl = appstoreUrl;
            this.supportEmail = supportEmail;
            running = true;
            Thread t = Display.getInstance().startThread(() -> checkTimerThread(), "Review thread");
            t.start();
        }
    
        void checkTimerThread() {
            while(running) {
                long lastTime = System.currentTimeMillis();
                int timeEllapsedInApp = Preferences.get("timeElapsedInApp", 0);
                Util.wait(this, timeForPrompt - timeEllapsedInApp);
                long total = System.currentTimeMillis() - lastTime;
                if(total + timeEllapsedInApp < timeForPrompt) {
                    Preferences.set("timeElapsedInApp", (int)(total + timeEllapsedInApp));
                } else {
                    Display.getInstance().callSerially(() -> showReviewWidget());
                    running = false;
                    instance  = null;
                    return;
                }
            }
        }
    
        void showReviewWidget() {
            // block this from happening twice
            Preferences.set("alreadyRated", true);
            InteractionDialog id = new InteractionDialog("Please Rate "  + Display.getInstance().getProperty("AppName", "The App"));
            int height = id.getPreferredH();
            Form f = Display.getInstance().getCurrent();
            id.setLayout(new BorderLayout());
            Slider rate = createStarRankSlider();
            Button ok = new Button("OK");
            Button no = new Button("No Thanks");
            id.add(BorderLayout.CENTER, FlowLayout.encloseCenterMiddle(rate)).
                    add(BorderLayout.SOUTH, GridLayout.encloseIn(2, no, ok));
            id.show(f.getHeight()  - height - f.getTitleArea().getHeight(), 0, 0, 0);
            no.addActionListener(e -> id.dispose());
            ok.addActionListener(e -> {
                id.dispose();
                if(rate.getProgress() >= 9) {
                    if(Dialog.show("Rate On Store", "Would you mind rating us in the appstore?", "Go To Store", "Dismiss")) {
                        Display.getInstance().execute(appstoreUrl);
                    }
                } else {
                    if(Dialog.show("Tell Us Why?", "Would you mind writing us a short message explaining how we can improve?", "Write", "Dismiss")) {
                        Message m = new Message("Heres how you can improve  " + Display.getInstance().getProperty("AppName", "the app"));
                        Display.getInstance().sendMessage(new String[] {supportEmail}, "Improvement suggestions for " + Display.getInstance().getProperty("AppName", "your app"), m);
                    }
                }
            });
        }
    
        private void initStarRankStyle(Style s, Image star) {
            s.setBackgroundType(Style.BACKGROUND_IMAGE_TILE_BOTH);
            s.setBorder(Border.createEmpty());
            s.setBgImage(star);
            s.setBgTransparency(0);
        }
    
        private Slider createStarRankSlider() {
            Slider starRank = new Slider();
            starRank.setEditable(true);
            starRank.setMinValue(0);
            starRank.setMaxValue(10);
            Font fnt = Font.createTrueTypeFont("native:MainLight", "native:MainLight").
                    derive(Display.getInstance().convertToPixels(5, true), Font.STYLE_PLAIN);
            Style s = new Style(0xffff33, 0, fnt, (byte)0);
            Image fullStar = FontImage.createMaterial(FontImage.MATERIAL_STAR, s).toImage();
            s.setOpacity(100);
            s.setFgColor(0);
            Image emptyStar = FontImage.createMaterial(FontImage.MATERIAL_STAR, s).toImage();
            initStarRankStyle(starRank.getSliderEmptySelectedStyle(), emptyStar);
            initStarRankStyle(starRank.getSliderEmptyUnselectedStyle(), emptyStar);
            initStarRankStyle(starRank.getSliderFullSelectedStyle(), fullStar);
            initStarRankStyle(starRank.getSliderFullUnselectedStyle(), fullStar);
            starRank.setPreferredSize(new Dimension(fullStar.getWidth() * 5, fullStar.getHeight()));
            return starRank;
        }
    
        /**
         * Binds the rating widget to the UI if the app wasn't rated yet
         *
         * @param time time in milliseconds for the widget to appear
         * @param appstoreUrl the app URL in the store
         * @param supportEmail support email address if the rating is low
         */
        public static void bindRatingListener(int time, String appstoreUrl, String supportEmail) {
            if(Preferences.get("alreadyRated", false)) {
                return;
            }
            instance = new RatingWidget();
            instance.timeForPrompt = time;
            instance.init(appstoreUrl, supportEmail);
        }
    
        /**
         * This should be invoked by the stop() method as we don't want rating countdown to proceed when the app isn't
         * running
         */
        public static void suspendRating() {
            if(instance != null) {
                synchronized(instance) {
                    instance.notify();
                }
                instance.running  = false;
                instance = null;
            }
        }
    }

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
