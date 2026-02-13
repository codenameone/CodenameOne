---
title: Flamingo SVG Transcoder
slug: flamingo-svg-transcoder
url: /blog/flamingo-svg-transcoder/
original_url: https://www.codenameone.com/blog/flamingo-svg-transcoder.html
aliases:
- /blog/flamingo-svg-transcoder.html
date: '2017-05-30'
author: Shai Almog
---

![Header Image](/blog/flamingo-svg-transcoder/uidesign.jpg)

SVG (Scalable Vector Graphics) is an XML based image format that represents an image as vectors instead of pixels (raster). An SVG file is represented by the set of lines & shapes that make it and can thus be rendered at any resolution without quality degradation due to scaling. It has some other neat tricks up its sleeve but I’m only going to discuss that specific feature today.

There was a time where everyone believed SVG will take the world by storm. It found a niche but never really created the revolution everyone expected. Still it’s a pretty valuable tool but unfortunately mobile device support is “spotty” at best…​

All mobile devices support SVG in the browser since it is a web standard. However, in the native layer SVG doesn’t work because of too many nuances related to the web that make it really hard to support the full spec in a native app. Android has partial SVG support where it can convert an SVG file to a native drawable during development time and that’s actually a pretty good idea as it removes the overhead of parsing SVG and the need to support the full spec.

This isn’t a new idea. Swing had a third party tool from Kirill Grouchnikov who implemented a static translator to generate Java2D code from the given SVG. This tool went thru a couple of forks and recently I chose to fork it myself to create a Codename One version which was surprisingly easy. Unlike the Swing version I created an `Image` subclass so the generated code can be used anywhere images are used.

You can check out [my fork here](https://github.com/codenameone/flamingo-svg-transcoder) it’s very experimental but if there is interest we can probably enhance it significantly. Gradients aren’t supported currently which can impact a lot of things. I think they should be doable though if we put some time into it.

I converted a standard SVG of duke waving and showed it using this code:
    
    
    Form current = new Form("SVG", new BorderLayout());
    current.add(CENTER, new ScaleImageLabel(new Duke_waving()));
    current.show();

![Duke waving image](/blog/flamingo-svg-transcoder/svg-duke.png)

Figure 1. Duke waving image

You will notice that Dukes nose isn’t red, that’s because it’s a radial gradient in the source SVG.

The generated `Duke_waving` class looks like this:
    
    
    public class Duke_waving extends com.codename1.ui.Image implements Painter {
        private int width, height;
    
        public Duke_waving() {
            super(null);
            width = getOrigWidth();
            height = getOrigHeight();
        }
    
        public Duke_waving(int width, int height) {
            super(null);
            this.width = width;
            this.height = height;
        }
    
        @Override
        public int getWidth() {
            return width;
        }
    
        @Override
        public int getHeight() {
            return height;
        }
    
        @Override
        public void scale(int width, int height) {
            this.width = width;
            this.height = height;
        }
    
        @Override
        public Image fill(int width, int height) {
            return new Duke_waving(width, height);
        }
    
        @Override
        public Image applyMask(Object mask) {
            return new Duke_waving(width, height);
        }
    
        @Override
        public boolean isAnimation() {
            return true;
        }
    
        @Override
        public boolean requiresDrawImage() {
            return true;
        }
    
        @Override
        protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
            drawImage(g, nativeGraphics, x, y, width, height);
        }
    
        @Override
        protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
            float hRatio = ((float)w) / ((float)getOrigWidth());
            float vRatio = ((float)h) / ((float)getOrigHeight());
            int origX = g.getTranslateX();
            int origY = g.getTranslateY();
            g.translate(-origX, -origY);
            g.scale(hRatio, vRatio);
            int tx = (int)(((float)x) / hRatio);
            int ty = (int)(((float)y) / vRatio);
            g.translate(tx, ty);
            paint(g);
            g.resetAffine();
            g.translate(origX - g.getTranslateX(), origY - g.getTranslateY());
        }
    
        private static void paint(Graphics g) {
            int origAlpha = g.getAlpha();
            Stroke baseStroke;
            Shape shape;
            g.setAntiAliased(true);
            g.setAntiAliasedText(true);
    
            //
    
            // _0
    
            // _0_0
            shape = new GeneralPath();
            ((GeneralPath) shape).moveTo(48.859, 43.518);
            ((GeneralPath) shape).curveTo(57.283, 61.158, 51.595, 184.35, 41.731003, 227.55);
            ((GeneralPath) shape).curveTo(31.867002, 270.822, 22.003002, 325.83002, 19.699003, 372.126);
            ((GeneralPath) shape).curveTo(18.691004, 391.854, 21.715004, 399.63, 34.603004, 399.63);
            ((GeneralPath) shape).curveTo(57.355003, 399.63, 86.227005, 351.678, 122.443, 352.758);
            ((GeneralPath) shape).curveTo(158.731, 353.83798, 170.251, 407.766, 187.24301, 407.406);
            ((GeneralPath) shape).curveTo(204.23502, 407.04602, 217.91501, 401.142, 218.059, 348.654);
            ((GeneralPath) shape).curveTo(218.563, 191.981, 87.235, 64.973, 48.859, 43.518);
            ((GeneralPath) shape).lineTo(48.859, 43.518);
            ((GeneralPath) shape).lineTo(48.859, 43.518);
            ((GeneralPath) shape).lineTo(48.859, 43.518);
            ((GeneralPath) shape).closePath();
    
            g.setColor(0);
            g.fillShape(shape);
    
            // _0_1
            shape = new GeneralPath();
            ((GeneralPath) shape).moveTo(162.763, 168.726);
            ((GeneralPath) shape).curveTo(170.755, 182.19, 191.131, 171.82199, 191.995, 156.63);
            ((GeneralPath) shape).curveTo(192.859, 141.438, 190.627, 122.286, 180.763, 121.56601);
            ((GeneralPath) shape).curveTo(170.899, 120.84601, 163.843, 110.98201, 153.979, 110.26201);
            ((GeneralPath) shape).curveTo(144.115, 109.54201, 133.531, 119.406006, 128.923, 106.30201);
            ((GeneralPath) shape).curveTo(124.315, 93.19801, 141.307, 87.65401, 153.979, 85.56601);
            ((GeneralPath) shape).curveTo(142.675, 74.26201, 136.05101, 61.73401, 131.08301, 48.70201);
            ((GeneralPath) shape).curveTo(126.115005, 35.670013, 122.44301, 23.718012, 136.339, 18.60601);
            ((GeneralPath) shape).curveTo(150.235, 13.494011, 149.08301, 39.77401, 164.99501, 51.79801);
            ((GeneralPath) shape).curveTo(160.31502, 34.086014, 158.587, 26.742012, 158.947, 16.44601);
            ((GeneralPath) shape).curveTo(159.307, 6.150009, 158.587, -1.6979885, 173.203, 0.31801033);
            ((GeneralPath) shape).curveTo(187.819, 2.3340104, 181.483, 32.71801, 192.85901, 45.102013);
            ((GeneralPath) shape).curveTo(196.315, 33.366013, 198.40302, 18.462013, 206.755, 12.342014);
            ((GeneralPath) shape).curveTo(215.107, 6.2220154, 234.115, 6.0780144, 222.01901, 31.206015);
            ((GeneralPath) shape).curveTo(209.92302, 56.334015, 225.54701, 69.94202, 221.87502, 89.74201);
            ((GeneralPath) shape).curveTo(218.20302, 109.54201, 206.82701, 105.94202, 201.93102, 118.68601);
            ((GeneralPath) shape).curveTo(197.03502, 131.43001, 204.81102, 160.44601, 195.59502, 173.47801);
            ((GeneralPath) shape).curveTo(186.37901, 186.51001, 184.72302, 206.52602, 190.69902, 222.51001);
            ((GeneralPath) shape).curveTo(172.339, 205.374, 162.763, 168.726, 162.763, 168.726);
            ((GeneralPath) shape).lineTo(162.763, 168.726);
            ((GeneralPath) shape).lineTo(162.763, 168.726);
            ((GeneralPath) shape).lineTo(162.763, 168.726);
            ((GeneralPath) shape).closePath();
    
            g.fillShape(shape);
    
            // _0_2
            shape = new GeneralPath();
            ((GeneralPath) shape).moveTo(48.355, 185.646);
            ((GeneralPath) shape).curveTo(40.939, 236.478, 15.162998, 242.526, 13.867001, 263.622);
            ((GeneralPath) shape).curveTo(12.571001, 284.71802, 20.707, 286.734, 20.203001, 306.246);
            ((GeneralPath) shape).curveTo(19.699001, 325.758, 2.3470001, 333.678, 0.25900078, 344.262);
            ((GeneralPath) shape).curveTo(-1.8289986, 354.84598, 9.187001, 358.22998, 15.595001, 358.22998);
            ((GeneralPath) shape).curveTo(22.003002, 358.22998, 28.411001, 330.15, 31.003002, 312.29398);
            ((GeneralPath) shape).curveTo(33.595, 294.43796, 22.507002, 283.92596, 22.507002, 271.68597);
            ((GeneralPath) shape).curveTo(22.507002, 259.44598, 38.563004, 237.05397, 35.611, 256.70996);
            ((GeneralPath) shape).curveTo(48.931, 235.686, 55.268, 208.901, 48.355, 185.646);
            ((GeneralPath) shape).lineTo(48.355, 185.646);
            ((GeneralPath) shape).lineTo(48.355, 185.646);
            ((GeneralPath) shape).lineTo(48.355, 185.646);
            ((GeneralPath) shape).closePath();
    
            g.fillShape(shape);
    
            // _0_3
            shape = new GeneralPath();
            ((GeneralPath) shape).moveTo(58.292, 205.013);
            ((GeneralPath) shape).curveTo(52.676, 232.517, 17.612, 386.09302, 35.468002, 388.037);
            ((GeneralPath) shape).curveTo(53.324005, 389.981, 78.740005, 340.517, 120.356, 340.87698);
            ((GeneralPath) shape).curveTo(162.044, 341.23697, 178.46, 396.317, 188.612, 395.95697);
            ((GeneralPath) shape).curveTo(198.764, 395.597, 205.45999, 399.55698, 206.54, 337.49298);
            ((GeneralPath) shape).curveTo(207.62, 275.429, 169.74799, 196.15698, 147.35599, 161.23698);
            ((GeneralPath) shape).curveTo(116.971, 163.181, 84.283, 187.518, 58.292, 205.013);
            ((GeneralPath) shape).lineTo(58.292, 205.013);
            ((GeneralPath) shape).lineTo(58.292, 205.013);
            ((GeneralPath) shape).lineTo(58.292, 205.013);
            ((GeneralPath) shape).closePath();
    
            g.setColor(0xffffff);
            g.fillShape(shape);
    
            // _0_4
    
            // _0_4_0
    
            // _0_4_1
            shape = new GeneralPath();
            ((GeneralPath) shape).moveTo(147.082, 171.533);
            ((GeneralPath) shape).curveTo(145.42, 154.33801, 132.675, 143.545, 116.187, 140.906);
            ((GeneralPath) shape).curveTo(100.263, 138.35701, 82.927, 145.904, 71.77899, 157.052);
            ((GeneralPath) shape).curveTo(59.24099, 169.59, 58.73999, 187.03, 67.51899, 201.88501);
            ((GeneralPath) shape).curveTo(76.17999, 216.542, 95.36599, 221.38602, 111.081985, 217.72002);
            ((GeneralPath) shape).curveTo(132.588, 212.705, 148.48, 193.896, 147.082, 171.533);
    
            g.setColor(0);
            g.fillShape(shape);
    
            // _0_4_2
            shape = new GeneralPath();
            ((GeneralPath) shape).moveTo(139.162, 177.941);
            ((GeneralPath) shape).curveTo(137.669, 193.568, 125.215004, 206.12299, 110.218, 209.765);
            ((GeneralPath) shape).curveTo(94.348, 213.619, 76.825, 207.508, 71.122, 191.189);
            ((GeneralPath) shape).curveTo(68.21, 182.857, 68.752, 174.31, 73.759, 166.991);
            ((GeneralPath) shape).curveTo(77.982, 160.81999, 84.792, 155.991, 91.538, 152.925);
            ((GeneralPath) shape).curveTo(105.462006, 146.599, 125.37, 145.896, 135.069, 160.002);
            ((GeneralPath) shape).curveTo(138.744, 165.348, 139.387, 171.641, 139.162, 177.941);
    
            g.setColor(0xffffffff);
            g.fillShape(shape);
            g.setColor(0);
            baseStroke = new Stroke(1, 0, 0, 4);
            g.drawShape(shape, baseStroke);
    
    
            g.setAlpha(origAlpha);
            g.resetAffine();
        }
    
        /**
         * Returns the X of the bounding box of the original SVG image.
         *
         * @return The X of the bounding box of the original SVG image.
         */
        public static int getOrigX() {
            return 0;
        }
    
        /**
         * Returns the Y of the bounding box of the original SVG image.
         *
         * @return The Y of the bounding box of the original SVG image.
         */
        public static int getOrigY() {
            return 0;
        }
    
        /**
         * Returns the width of the bounding box of the original SVG image.
         *
         * @return The width of the bounding box of the original SVG image.
         */
        public static int getOrigWidth() {
            return 226;
        }
    
        /**
         * Returns the height of the bounding box of the original SVG image.
         *
         * @return The height of the bounding box of the original SVG image.
         */
        public static int getOrigHeight() {
            return 408;
        }
    
        @Override
        public void paint(Graphics g, Rectangle rect) {
            drawImage(g, null, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        }
    }

### SVG vs. Font Icons/Material Fonts

You can’t color SVG’s dynamically and they will probably never match the speed/convenience of font icons. For most use cases you should probably use the font icons or material icons. However, if you need a design element or icon that is vector based and has multiple colors this might be an interesting option.

In terms of performance it’s hard to know how well SVG will perform. If you have an image with a fixed size on device you can just convert it to a raster in runtime and use that to avoid potential performance overhead.

### Moving Forward

This depends a lot on demand/pull requests and needs. Supporting gradients will open up a lot of use cases.

It might be interesting to allow manipulation of some SVG object states as well…​ This might be required if we want to support SVG animations which would provide a great deal of benefit e.g. for animations in the style of Androids menu button animating to a back button etc.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — November 20, 2017 at 4:56 pm ([permalink](https://www.codenameone.com/blog/flamingo-svg-transcoder.html#comment-21633))

> I’m interested in converting an svg scalable logo to Codename One code, but… is the link you provided correct? There is no mention of Codename One:  
> [https://github.com/ebourg/f…](<https://github.com/ebourg/flamingo-svg-transcoder>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflamingo-svg-transcoder.html)


### **Shai Almog** — November 21, 2017 at 12:33 pm ([permalink](https://www.codenameone.com/blog/flamingo-svg-transcoder.html#comment-21533))

> Ugh, thanks! It’s [http://github.com/codenameo…](<http://github.com/codenameone/flamingo-svg-transcoder>)  
> I’ll fix it in the post.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflamingo-svg-transcoder.html)


### **Angelo** — June 23, 2020 at 5:31 pm ([permalink](https://www.codenameone.com/blog/flamingo-svg-transcoder.html#comment-24281))

> Angelo says:
>
> If I understand, the utility converts an SVG file that uses a reduced set of features to real Java code. Is it possible just to create a XML file to be loaded from resources with curves, lines, moves and so on? You could create a class like ImageFromSVGSubset and instances would be created with sort of new ImageFromSVGSubset(“id”). The xml has not to be an official format, just used by developers in Codename One that want to use the transcoder and that know what they are doing. I think it should take just one hour of development for you, starting from the current fork.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflamingo-svg-transcoder.html)


### **Shai Almog** — June 24, 2020 at 2:45 am ([permalink](https://www.codenameone.com/blog/flamingo-svg-transcoder.html#comment-21424))

> Shai Almog says:
>
> No it’s harder.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflamingo-svg-transcoder.html)


### **Angelo** — June 24, 2020 at 7:11 am ([permalink](https://www.codenameone.com/blog/flamingo-svg-transcoder.html#comment-24280))

> Angelo says:
>
> I take the occasion to inform you about a bug. In the code this line is encountered:  
> shape = new Rectangle2D.Double(0.008569182828068733, 0.0054579987190663815, 6.3905863761901855, 6.390747547149658);
>
> Some characters from a method signature went into the output.  
> I had to manually fix it but it is a minor thing.  
> I had problems in some cases with the paint method that is annotated @Override  
> Thank you.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflamingo-svg-transcoder.html)


### **Durank** — October 21, 2020 at 3:20 pm ([permalink](https://www.codenameone.com/blog/flamingo-svg-transcoder.html#comment-24344))

> [Durank](https://avatars0.githubusercontent.com/u/16245755?v=4) says:
>
> Please provide a solutions to use SVG images more easy.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflamingo-svg-transcoder.html)


### **Durank** — October 21, 2020 at 4:33 pm ([permalink](https://www.codenameone.com/blog/flamingo-svg-transcoder.html#comment-24360))

> [Durank](https://avatars0.githubusercontent.com/u/16245755?v=4) says:
>
> I did all steps in the flamingo page and I can’t see the generated java code to my svg image
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflamingo-svg-transcoder.html)


### **Shai Almog** — October 22, 2020 at 10:48 am ([permalink](https://www.codenameone.com/blog/flamingo-svg-transcoder.html#comment-24362))

> Shai Almog says:
>
> Does it print out that it’s processing the SVGs? What’s printed?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflamingo-svg-transcoder.html)


### **Duran k** — October 22, 2020 at 3:10 pm ([permalink](https://www.codenameone.com/blog/flamingo-svg-transcoder.html#comment-24364))

> [Duran k](https://lh3.googleusercontent.com/a-/AOh14GhIAakAlC4gLyRHwDzKuv6MUG2CDvLghf3zUAG8yA=s96-c) says:
>
> nothing  
> Microsoft Windows [Version 10.0.19041.572]  
> (c) 2020 Microsoft Corporation. All rights reserved.
>
> C:Usersdesarrollo1>cd F:data_kdpprogramsCodenameOneutils_programssvg
>
> C:Usersdesarrollo1>f:
>
> F:data_kdpprogramsCodenameOneutils_programssvg>dir  
> Volume in drive F is data_kdp  
> Volume Serial Number is 48F9-3B10
>
> Directory of F:data_kdpprogramsCodenameOneutils_programssvg
>
> 21/10/2020 12:38 p. m.  .  
> 21/10/2020 12:38 p. m.  ..  
> 20/10/2020 10:47 a. m. 38,154 automobile_automotive_car.svg  
> 21/10/2020 12:38 p. m.  ebourg-flamingo-svg-transcoder-1.2-3-gc12f421  
> 21/10/2020 12:38 p. m. 398,372 ebourg-flamingo-svg-transcoder-1.2-3-gc12f421.zip  
> 21/10/2020 11:25 a. m. 2,948,454 flamingo-svg-transcoder-core-1.2-jar-with-dependencies.jar  
> 21/10/2020 11:35 a. m. 4,642,088 fondo.svg  
> 21/10/2020 12:06 p. m. 50,833 method-draw-image.svg  
> 21/10/2020 12:23 p. m.  myfiles  
> 5 File(s) 8,077,901 bytes  
> 4 Dir(s) 111,844,356,096 bytes free
>
> F:data_kdpprogramsCodenameOneutils_programssvg>java -jar flamingo-svg-transcoder-core-1.2-jar-with-dependencies.jar /myfiles fondo.svg
>
> F:data_kdpprogramsCodenameOneutils_programssvg>
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflamingo-svg-transcoder.html)


### **Shai Almog** — October 23, 2020 at 6:53 am ([permalink](https://www.codenameone.com/blog/flamingo-svg-transcoder.html#comment-24356))

> Shai Almog says:
>
> The arguments should be the current directory followed by package name. So something like:  
> “`  
> . com.mypackage.svg  
> “`
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflamingo-svg-transcoder.html)


### **Duran k** — October 23, 2020 at 6:30 pm ([permalink](https://www.codenameone.com/blog/flamingo-svg-transcoder.html#comment-24363))

> [Duran k](https://lh3.googleusercontent.com/a-/AOh14GhIAakAlC4gLyRHwDzKuv6MUG2CDvLghf3zUAG8yA=s96-c) says:
>
> I don’t know what is the package name that you said. This documentation isn’t very clear. Please provide a Documentation more clear.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflamingo-svg-transcoder.html)


### **Shai Almog** — October 24, 2020 at 5:38 am ([permalink](https://www.codenameone.com/blog/flamingo-svg-transcoder.html#comment-24365))

> Shai Almog says:
>
> It’s your package name. This is a code generator. It generates source code to a specific package name.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflamingo-svg-transcoder.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
