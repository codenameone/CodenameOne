---
title: Java is Superior To React Native In Practically Every Way
slug: java-is-superior-to-react-native-in-practically-every-way
url: /blog/java-is-superior-to-react-native-in-practically-every-way/
original_url: https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html
aliases:
- /blog/java-is-superior-to-react-native-in-practically-every-way.html
date: '2015-11-01'
author: Shai Almog
---

![Header Image](/blog/java-is-superior-to-react-native-in-practically-every-way/react-demo.png)

I got into a discussion with a colleague on the Java vs. JavaScript subject, which is a problematic subject to  
begin with. He then mentioned how great React Native is, I decided I have to look into it and maybe grab  
some ideas for Codename One.

There are some nice ideas there, but none of them is revolutionary or exceptional and most of them are pretty  
old news for Codename One developers running in Java 8.

One thing I did like was how short the [React demo code ](https://facebook.github.io/react-native/docs/tutorial.html#final-source-code)seemed to be, so I ported it to Codename One and ended up with roughly the same amount of code and arguably better/simpler code! Check out the full listing at the end of the article or in the github project [here](https://github.com/codenameone/codenameone-demos/tree/master/ReactDemo), but lets first review why the Java code is “better”.

### Synchronous Execution

JavaScript fans hate this but its still a fact that synchronous code is simpler to read, follow and debug. E.g. this  
is the React Native version of the code that fetches the data:
    
    
    fetchData: function() {
      fetch(REQUEST_URL) 
            .then((response) => response.json()) 
            .then((responseData) => { 
                 this.setState({ 
                      dataSource: this.state.dataSource.cloneWithRows(responseData.movies), 
                      loaded: true, 
                 }); 
             }) 
      .done(); 
    },

I have well over 20 years of professional programming experience and this is still hard to follow. Apparently if  
`done()` is omitted you won’t get any error handling?

Its weird and error prone. I feel like a lot of code is hidden behind this which makes the terseness more  
confusing than simplifying (kind of like following a political debate thru Twitter).

To me our code is **way** simpler:
    
    
    react.add(BorderLayout.CENTER, new InfiniteContainer() {
        public Component[] fetchComponents(int index, int amount) {
            try {
                Collection data = (Collection)ConnectionRequest.fetchJSON(REQUEST_URL).get("movies");
                Component[] response = new Component[data.size()];
                int offset = 0;
                for(Object movie : data) {
                    response[offset] = createMovieEntry(Result.fromContent((Map)movie));
                    offset++;
                }
                return response;
            } catch(IOException err) {
                Dialog.show("Error", "Error during connection: " + err, "OK", null);
            }
            return null;
        }
    });

Notice that this isn’t the exact equivalent of the code above as we also create components, add them to the UI  
and handle the resulting error!

A more fair comparison would be:
    
    
    try {
        Collection data = (Collection)ConnectionRequest.fetchJSON(REQUEST_URL).get("movies");
        ...
    } catch(IOException err) {
        ...
    }

That’s effectively one line of code that could even be shorter after which we have the result… No flow, no callback!

Developers often pour hate on the Java checked exceptions feature and I have to agree that they are sometimes painful.

(f’ing `InterruptedException` is stupid) but this is a great example of why checked exceptions matter.

We MUST handle errors properly and we can’t just ignore it until our code reaches production with this  
lovely “TODO” comment that no one bothered reading.

### One Language – Less Code

The listings seem roughly equivalent in size but you will notice the react code ignores the native platform  
specific code when dealing with the JavaScript code. Our listing is all encompassing, no additional code is needed  
and no further boilerplate, projects etc.

React Native takes this even further by mixing tags with the JavaScript code effectively mixing declarative code into  
the regular flow. Yes it shortens the code, but also removes a huge part of the value of declarative programming  
which is the separation of responsibilities.

### Reload == Apply Code Changes

React Native can be debugged by reloading which is there to help when working with the **awful** Android emulator.

Luckily Codename One doesn’t need that emulator, you also don’t need to restart your app to reload compiled changes… E.g. in NetBeans just use “Apply Code Changes” in the debugger and your changes are instantly mirrored into a running app.

### Scripting Languages Are Problematic “On Device”

This isn’t quite a “React Native” specific rant, its related to all tools packaging JavaScript in the app bundle.  
Scripting languages are great for the web, they are like “duct tape”. Show me a hacker who doesn’t LOVE duct tape!

The temptation to ship an app built with such duct tape is big, but unlike the web where you can just fix that “weird undefined” bug in production by deploying a new update. With apps you need to go thru Apples approval process… This means production bugs that stay while you watch your rating drop.

Yes, unit tests, lint and a lot of other solutions are supposed to catch those things but when you use a modern IDE and it detects potential null inference thanks to the strict language syntax its pretty amazing!

E.g. a great example for JavaScripts over simplification of problems would be in code like this:
    
    
    function reduce(var a) {
          if(...) {
             a = a - 1;
          } else {
             a = a + 1;
          }
    }

If this was Java code we could tell exactly what would happen here… In JavaScript this isn’t quite the case!

Lets assume that due to a bug a was somehow a string that is `"11"` as long as the condition is true (which might be the case in all test cases) this will act like a number. E.g. `a` will become `"10"`.

But in production if the condition becomes false for some reason `a` would become `"111"`.

If `a` represents something of value (e.g. debt, credit etc.) having an app with this bug in the store  
could be really painful.

### Environment

React native uses the native development environments which means it needs a Mac for iOS development.

It also means you do part of the work in the Android IDE, part of it in Xcode and the JavaScript work using a  
text editor.

Its amazing to me that developers are willing to throw away 30 years of IDE evolution for some syntactic  
candy???

Are we that traumatized by Eclipse?

Todays IDE’s are amazing and the fact you can track/debug your entire code via a single IDE is invaluable.

The ability we have as a team to instantly see who used what and for what purpose is astounding, I can’t fathom how something like this can be used by a team of more than 2 people especially in a distributed workforce.

### What I Liked About JavaScript

The one thing I really like about working with JavaScript is the ease of working with JSON, while in the code below I reduced it significantly almost to the same size it’s still not as elegant.

I’m still not a fan of duck typing or scripting languages but I’d really like to get something like property objects into Codename One and improve the integrated parsing.

### Final Word

One of the problems I find with terse programming is that people use it to hide basic concepts so too much happens in an “unspoken” way. This makes terse code as easy to read as a Tweet, unfortunately if you need to express even a moderately complex idea Twitter just doesn’t cut it and that’s a big problem with some of these API’s.

React native has its fans, after all its probably better than PhoneGap which has its own set of limitations. But its  
still a limited concept standing on the chicken legs of a scripting infrastructure. It has no real advantage when  
compared to Codename One and has some obvious potential issues.

### Java Listing
    
    
    public class ReactDemo {
        private static final String REQUEST_URL = "https://raw.githubusercontent.com/facebook/react-native/master/docs/MoviesExample.json";
        private Form current;
        private EncodedImage placeholder;
    
        public void init(Object context) {
            UIManager.initFirstTheme("/theme");
        }
        
        public void start() {
            if(current != null){
                current.show();
                return;
            }
            placeholder = EncodedImage.createFromImage(Image.createImage(53, 81, 0), false);
            Form react = new Form("React Demo", new BorderLayout());
            react.add(BorderLayout.CENTER, new InfiniteContainer() {
                public Component[] fetchComponents(int index, int amount) {
                    try {
                        Collection data = (Collection)ConnectionRequest.fetchJSON(REQUEST_URL).get("movies");
                        Component[] response = new Component[data.size()];
                        int offset = 0;
                        for(Object movie : data) {
                            response[offset] = createMovieEntry(Result.fromContent((Map)movie));
                            offset++;
                        }
                        return response;
                    } catch(IOException err) {
                        Dialog.show("Error", "Error during connection: " + err, "OK", null);
                    }
                    return null;
                }
            });
            react.show();
        }
        
        Component createMovieEntry(Result data) {
            Container entry = BorderLayout.center(
                    BoxLayout.encloseY(
                            new SpanLabel(data.getAsString("title"), "Line1"), 
                            new Label(data.getAsString("year"), "Line2"))).
                    add(BorderLayout.WEST, 
                            URLImage.createToStorage(placeholder, data.getAsString("id"), 
                                        data.getAsString("posters/thumbnail")));
            return entry;
        } 
    
        public void stop() {
            current = Display.getInstance().getCurrent();
        }
        
        public void destroy() {
        }
    }

# Write Once, Run Anywhere.

## Truly native cross-platform app development with Java or Kotlin for iOS, Android & Web.

[Get Started](/getting-started/)

[Why Codename one? ](/introduction/)
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **bryan** — November 2, 2015 at 10:20 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22520))

> bryan says:
>
> I think you’ll need to be wearing your flame proof suit for a while – the Javascript guys always seem to take great exception to anyone dissing their baby.
>
> What you say about throwing away 30 years of IDE development is so true. I came across this [http://www.fse.guru/how-to-…](<http://www.fse.guru/how-to-pick-a-frontend-web-framework>) the other day, and I thought to myself, back the day you could use Delphi, say, to develop a desktop app, and now to develop a web app you apparently need a 1001 bits of stuff, to create something that in almost every way is inferior to a desktop app. Weird.
>



### **Shai Almog** — November 3, 2015 at 4:27 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22243))

> Shai Almog says:
>
> They REALLY hate me on Reddit 😉
>
> I can take it, otherwise I wouldn’t have written it.
>
> That’s part of why we started Codename One, it seems innovation in this space is busy taking us backwards.
>



### **oojr** — November 7, 2015 at 10:15 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22504))

> oojr says:
>
> ES7 async functions is coming next year and would make the code look even better, Javascript is shaping up to be “The Next Big Language” [https://jakearchibald.com/2…](<https://jakearchibald.com/2014/es7-async-functions/>)
>



### **Shai Almog** — November 8, 2015 at 4:23 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22279))

> Shai Almog says:
>
> Its been brought to my attention that JavaScript fans can’t read my code and misunderstand me when I say that the code is sync…
>
> What I mean is that the code “looks” synchronous but really works like async code by running on the event dispatch thread and yet allowing for events etc. to still process seamlessly. That’s a pretty neat trick called invokeAndBlock:
>
> [http://www.codenameone.com/…](<http://www.codenameone.com/blog/callserially-the-edt-invokeandblock-part-1.html>)
>
> [http://www.codenameone.com/…](<http://www.codenameone.com/blog/callserially-the-edt-invokeandblock-part-2.html>)
>
> I think JavaScript has its place as an important language, but even huge JavaScript fans find it hard to create large maintainable projects in it. The question of where the line passes where you probably should switch to a more “strict” language like Java (Scala if you prefer a more dynamic language etc.) is a matter of personal choice.
>



### **oojr** — November 10, 2015 at 4:04 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22358))

> oojr says:
>
> I went into a large corporation that had several lines of Java server code and discovered that ” large maintainable apps in Javascript are harder to do than a language like Java” is a huge myth, writing several modular components that work seamlessly together is hard in any language, it is all about preference and what will make you more productive. Uber’s codebase is mostly Javascript/Node.js and they seem to be doing just fine at scale. It is better to have a language that has optional type checking than strict.
>
> Back to the blog though, I use React Native and can code in Objective-C and Java, why? React Native allows me to use a flexbox layout and has all the benefits of an open source platform/ecosystem
>



### **Shai Almog** — November 11, 2015 at 5:26 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22422))

> Shai Almog says:
>
> I would tend to agree that you can write horrible messy code in any language and great code in any language. Developer skills are important.
>
> One “illness” that Java developers have is over abstraction and over complicating everything, but it seems JavaScript developers have picked up some of that flu as well see: [http://geek-and-poke.com/ge…](<http://geek-and-poke.com/geekandpoke/2014/11/8/frameworks>)
>
> About Uber, its a startup hence no legacy code maintenance and highly controlled architecture. Keep in mind that no corporation will advertise the “pain” of working on their codebase.
>
> Back to the blog post, Codename One has been open source since its inception (which was far before react native was even conceived) and has had flexible design layouts that work for all devices from before that.
>
> If you prefer JavaScript as a language that’s totally fine, to each his own. The blog post is aimed at Java developers who sometimes get the sense of “the grass is greener” when JavaScript developers talk about how X is easier. That’s just not the case.
>



### **Jeff Carver** — January 16, 2016 at 8:06 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22533))

> Jeff Carver says:
>
> Hi Shai,
>
> I ran across information about Codename One while researching Google Flutter. CN1 looks interesting but Java is one of the few languages I never bothered with. Someone in an earlier post mentioned Delphi and I still think that was the greatest language/IDE ever created.
>
> Anyway, for someone who has focused mainly on Actionscript, Javascript, and PHP for the last several years, what suggestions could you provide for getting started in Java and CN1? I guess I’m trying to figure out how comfortable one has to be with Java before attempting to use CN1.
>
> I have already evaluated numerous other products in this space including React Native, Native Script, and various HTML/PhoneGap platforms. The best I’ve found so far is Tabris.js but it doesn’t appear to have any serious backing, is almost completely unknown, and thus may not be around for long.
>
> Thanks in advance for any information.
>



### **Shai Almog** — January 17, 2016 at 4:39 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22677))

> Shai Almog says:
>
> Hi,  
> We’re all big Java advocates around here and have been doing Java for so long we don’t even remember learning Java. So we’re possibly the worst possible reference for getting started if you don’t know Java to begin with.
>
> For complete newbies there is [http://codapps.io/](<http://codapps.io/>) but I think it will be too simple for you as its designed for people with no coding experience.
>
> Java is a relatively simple and strict language so this might be a bit of a culture shock when coming from loose languages like JavaScript. Also a lot of our code/demos predate the Java 8 support so a lot of those would look very verbose to a guy coming from that background.
>
> Since you already know several languages I think just picking up Java and looking at the code should be pretty intuitive without an explicit tutorial but I don’t really know…
>



### **void777** — January 25, 2016 at 11:19 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22524))

> void777 says:
>
> Java works for iOS apps?
>



### **Shai Almog** — January 26, 2016 at 3:13 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22659))

> Shai Almog says:
>
> Ahem [https://www.codenameone.com/](<https://www.codenameone.com/>)
>



### **adamski** — January 30, 2016 at 9:30 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22441))

> adamski says:
>
> I’m not a massive Javascript fan but I do think React Native has hit a good spot. I am using it for cross-platform UI and the core of my app is written in C++ with the JUCE framework. This gives me the best of both both worlds, tight performant code where I need it and quick to build UI across mobile platforms.  
> Had I heard of Codename One before I started I might have given it a look 🙂
>



### **Gabriel Matusevich** — March 11, 2016 at 6:43 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22609))

> Gabriel Matusevich says:
>
> hmmmmm I think you are being a bit unfair, react native has … a few years of development and it’s already cruising, also you are not talking about Rapid App Develpoment, with React Native I’m able to write apps in MUCH less time not to mention that Android Development in Java is a ginormous pain, with React and Redux, mobile dev is a paradise in comparison.
>



### **Shai Almog** — March 12, 2016 at 3:28 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22755))

> Shai Almog says:
>
> I’m comparing it to Codename One which has only a slight advantage in terms of years so this is totally fair.
>
> Android development does suck. One of the problems in React Native is the fact that you need to setup an Android environment to get started.
>



### **Simon** — March 12, 2016 at 5:02 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22619))

> Simon says:
>
> Wow I think you have to be a particular breed to think that the Java was simpler than the React code. For one it was about seven times as long and as soon as I see stuff like ‘public void’ and ‘private static final’ I have the urge to go running for the hills.
>



### **Shai Almog** — March 12, 2016 at 5:12 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22629))

> Shai Almog says:
>
> It isn’t longer as shown in the sample above its roughly the same size. Notice that Java 8 syntax is pretty terse when used effectively.
>
> Yes there is some boilerplate but boilerplate isn’t program logic hence doesn’t add to complexity.
>
> Type safety is commonly accepted as a preferable way for a large number of developers (e.g. Typescript) so if you prefer Java (which great many do) then React Native doesn’t add any advantage. If you prefer JavaScript then this becomes a religious debate as there is no debating programming preference.
>



### **Simon** — March 12, 2016 at 5:18 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22483))

> Simon says:
>
> Actually its just over twice as long, (I did a character count in a text editor). Yes that’s what I mean by a ‘particular’ breed ie programmers. Anyone more casual will likely prefer javascript. But React Native is far from being just javascript.
>



### **Shai Almog** — March 12, 2016 at 5:41 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22584))

> Shai Almog says:
>
> Did you include the embedded XML? Did you include their bootstrap files which aren’t listed in their code but do exist there? Our code is pretty complete (with the exception of the package/import statements) so it includes all the UI and lifecycle code. Their examples rely on some generated files that aren’t listed.
>
> Java IDEs make Java MUCH easier for novices and type safety removes a lot of newbie mistakes. For experienced developers the ability to refactor a Java application from a stranger is a huge advantage. So I would argue that Java is WAY easier than JavaScript but I’m obviously highly biased. Every coin has two sides and I gave the example above of JSON processing which JavaScript’s ducktyping really simplifies.
>



### **Simon** — March 12, 2016 at 5:49 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22765))

> Simon says:
>
> You can’t include things that are embedded or not listed ‘cos they don’t bother you or make things more complex. The more stuff that is embedded or behind the scenes, the easier it is to just get on and program and the code itself looks much more simple.
>
> I truly wish Java was simpler ‘cos I would love to learn it. Tried and failed. Actually my favourite is PHP which is even more straightforward that javascript. I actually prefer Javascript variants like JQuery and React Native. The less characters typed to achieve something, the simpler it appears.
>



### **Shai Almog** — March 12, 2016 at 7:34 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22773))

> Shai Almog says:
>
> Does it not bother me?
>
> You need to control lifecycle in any non-trivial application so it being hidden in a separate file is pointless. I could break my sample into two files and achieve the exact same results as react native so that doesn’t really measure anything realistic.
>
> If you don’t like Java then you are clearly not in the demographic I aimed this article at. It’s aimed at people who like and appreciate Java and its advantaged but are looking at React Native thinking the other side of the fence might have greener pastures.
>



### **Alex** — March 28, 2016 at 2:36 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22446))

> Alex says:
>
> I just threw up in my mouth a little.
>



### **adamski** — April 6, 2016 at 8:17 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22816))

> adamski says:
>
> React Native is only partly about Javascript. Its more about the architecture. And the amount of 3rd party modules available via npm. And the amazing developer community, that i’ve not seen anything like before.
>
> I’ve settled on RN for the UI of my app, and thought its not been without its hurdles I feel like its a good way to do cross platform mobile development. I’m actually more comfortable writing C++11 (the other half of the app is in C++) than Javascript but since ES6 and ES7 things are looking much better. Yes it still has its warts but so does every other language in one way or another. I started writing my app UI in Swift for iOS with the intention to write the Android UI in Java but RN looked like a much better proposal to save rewriting all the UI logic for each platform. I’d have given your product a spin if I’d known about it sooner.
>
> Your comment “With apps you need to go thru Apples approval process” is not true for javascript only updates – there are tools to enable pushing javascript only updates to installed apps thereby circumventing the Apple approval process.
>



### **Shai Almog** — April 7, 2016 at 3:18 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22565))

> Shai Almog says:
>
> You can say that about any major framework. E.g. Java itself is not about Java as much as the JVM… The Java developer community has it’s own benefits so I wouldn’t go into an argument over those.
>
> Dynamically downloading JavaScript is something that used to be prohibited, this makes sense as it somewhat eliminates the value of the human review process. Notice that you still can’t push out real updates and need to go thru review if you make major changes although it’s unclear how Apple can enforce such distinctions once they opened that door.
>



### **Osei Fortune** — April 7, 2016 at 8:19 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22573))

> Osei Fortune says:
>
> I think typescript should help make large JavaScript projects easier to maintain
>



### **Don't Bother** — April 20, 2016 at 11:22 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22693))

> Don't Bother says:
>
> >Javascript are harder to do than a language like Java” is a huge myth…
>
> It’s not a myth it’s a sad truth. The reality is that on large JavaScript project people just afraid to make any serious refactoring because you never know what will break and when. I have experienced this many times. If you have not seen this it means you have not seen any large and complex project.
>



### **Don't Bother** — April 20, 2016 at 11:24 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22527))

> Don't Bother says:
>
> True but TypeScript is a not JavaScript. It is different language. And it has huge difference which is defined by world “Type” :-), and it has other things which are not present in JavaScript. It is same as if you say that Swift will make javaScript apps easier…
>



### **Chromonav Kulkarni** — April 26, 2016 at 7:21 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22768))

> Chromonav Kulkarni says:
>
> bro decide are you novice or a pro. Javascript is for people who learnt everything and are in search of superpowers.Js because of fluidity and simplicity and ability to implement suitable programming design pattern needed to get things done.
>



### **Chromonav Kulkarni** — April 26, 2016 at 7:30 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22775))

> Chromonav Kulkarni says:
>
> Java is a beautiful language and carefully designed with gr8 toolset; but lets face it: Javascript happened to be in middle of web evolution. It has simply evolved to a whole new level.  
> Oldies like you may not appreciate beauty and simplicity of current javascript ecosystem.  
> But theres one thing you should keep in mind : Atwoods Law:  
> Any application that can be written in JavaScript will eventually be written in JavaScript. 😜😜
>



### **Chromonav Kulkarni** — April 26, 2016 at 7:46 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22596))

> Chromonav Kulkarni says:
>
> lets compare community following, rate of growth, current apps in production.
>



### **Shai Almog** — April 26, 2016 at 8:06 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-21509))

> Shai Almog says:
>
> So was Java, livescript was renamed to JavaScript to pick the traction of Java. Either way this isn’t so much a Java vs. JavaScript but rather a React Native vs. Java. So all/most of your points aren’t really relevant here. If you embed JavaScript in a native app you open up a lot of problems and lose a lot of benefits of JavaScript.
>



### **oojr** — April 27, 2016 at 5:21 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-21557))

> oojr says:
>
> True but you can say that about any large Java projects as well
>



### **Chromonav Kulkarni** — April 27, 2016 at 2:21 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22825))

> Chromonav Kulkarni says:
>
> React Native inherits all the features of javascript essentially it is javascript++  
> React way of combining all concerns and dividing of problem has one major advantage:  
> Code Reuse.  
> The massive scale of code reuse possible and with a vibrant Js community it is possible to production ready apps within fraction of time.  
> React Native inherits powers of both worlds and combines it with awesome react way (i love i don’t know abt you.)
>



### **Chromonav Kulkarni** — April 27, 2016 at 2:24 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22694))

> Chromonav Kulkarni says:
>
> End justifies the means. React works well for Facebook helps them churn feature updates faster. It is helping me the same way. I don’t care if i am going backwards and forward
>



### **Shai Almog** — April 27, 2016 at 2:25 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22528))

> Shai Almog says:
>
> So it inherits DOM access?  
> Can I take JavaScript code off the internet that relies on CSS and have it work?
>
> Nope…
>
> It’s either native or it’s web. React Native stands in the middle and it pays for that, compile time, build process, install native tools etc. It has some benefits from JavaScript (fast preview etc.) but don’t try to present it like a panacea.
>



### **Ben** — May 9, 2016 at 9:31 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22776))

> Ben says:
>
> Several moot arguments here:
>
> The proposed async/await syntax is supported by default. You needn’t trifle with promises and can wrap fetch calls in try/catch blocks.
>
> Synchronous live reloading is supported, however, hot reloading will swap out individual components in realtime.
>
> JSX is completely optional, and compiles to plain JavaScript React.createElement() calls. These calls can easily be sugared using React.createFactory().
>
> Facebook’s own [Nuclide.io](<http://Nuclide.io>) is conspicuously missing from the Environment debate, as is the wealth of open source modules available from NPM. Facebook Flow is likewise excellent for terse static type support replete with compile time errors, and Jest was built specifically to unit test React components.
>
> After installing the Android SDK and necessary APIs, it takes only several brief commands to bootstrap then run a React Native app using the provided CLI. The same can be said for physical devices.
>
> I’m aware some of these features may not have been implemented or mature last November, but would like to ensure nobody is given a wrong or unilateral impression should they wish to try React Native.
>



### **Shai Almog** — May 10, 2016 at 4:08 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22883))

> Shai Almog says:
>
> I don’t think react native lacks in the department of advocacy or visibility so if people get the wrong impression about it this usually biases in the other way.
>
> Notice that you need to install the Android SDK and then the iOS SDK and then the Windows SDK and then rewrite for web… You also need to adapt code as it isn’t a WORA solution… We actually allow you to install one plugin and it “just works” on everything.
>



### **khle** — June 25, 2016 at 12:14 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22633))

> khle says:
>
> Interesting take. You probably knew this would be an unpopular opinion. Anyway, I didn’t read through all the comments so maybe someone else already said this. But one advantage with React Native is the same skill set can be used to write web applications. And with JS, one can write NodeJS on the back end. So maybe the same devs can do mobile, web and back end. For some companies, this could be advantageous.
>



### **Shai Almog** — June 26, 2016 at 4:50 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22877))

> Shai Almog says:
>
> If you bring server programming into it then Java has a huge upper hand with more than a decade of scale in the enterprise and far more diversity there…
>



### **Nicolás Schürmann Lindemann** — July 5, 2016 at 12:25 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22772))

> Nicolás Schürmann Lindemann says:
>
> Java 8 seems to have a lot of good features now!, i remember back in the old days making an ajax call in java was a pain!, even trying to simplify it with libraries was difficult. I agree that strong typing adds a lot of ease in development and mitigates bugs. I think that the only complain about this is that you are comparing 2 synchronous codes while the JS call is being asynchronous. And also is the implementation of promises and the fetch API in ecmascript 6. Also the promises API implements a “catch” method that is used for errors management (therefore, no try/catch) it accept functions as callbacks so you can compose more easily. The code that you written was very imperative. Also the advantage/disadvantage of javascript is in fact the loose typing. Some loves it, others hate it. It gives you a lot of freedom in expresion, but you can get bugs that you will only get in production unless you do a lot of testing or add strong typing (typescript, flow).
>
> I believe that something like this may be more comparable and will let understand the benefits of promises and js:
>
> fetchData: function() {  
> fetch(REQUEST_URL)  
> .then(toJson)  
> .then((responseData) => dispatch(updateRows(responseData)))  
> .done();  
> }
>
> and with a helper:
>
> const disp = fn => data => dispatch(fn(data))
>
> fetchData: function() {  
> fetch(REQUEST_URL)  
> .then(toJson)  
> .then(disp(updateRows))  
> .done()  
> }
>
> I think it’s cleaner, less imperative though.
>



### **WhoIsMeekMill?&kidcudi** — August 2, 2016 at 6:16 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22887))

> WhoIsMeekMill?&kidcudi says:
>
> Just another java/android developer feeling threatened that his market is disappearing. Nothing to see here folks.
>



### **bertbeck** — August 13, 2016 at 3:54 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22984))

> bertbeck says:
>
> How much overhead (in app size) does Codename One add to a typical app (over native size) ? Any idea what is typical for React Native? I’m an IOS and Android developer – have worked with Xamarin – know all too well the pain of added and complex runtimes (and bugs created by the runtime by new releases). I’m looking for the best environment to co-develop IOS/Android and if possible Web and Native PC/OSX/Linux
>



### **Shai Almog** — August 14, 2016 at 4:19 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22921))

> Shai Almog says:
>
> Codename One Android apps are roughly 1mb and iOS apps between 3-5mb for hello world.  
> So the overhead is relatively low. There are fluctuations in the implementations which is why we have versioned build which allows pro users to build against a stable release.
>



### **yedidyak** — August 21, 2016 at 8:47 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22802))

> yedidyak says:
>
> You can simplify the JS asynchronous code by using async await, which is supported in React Native. The examle above would become:
>
> async function fetchData() {  
> const responseData = await fetch(REQUEST_URL).json();  
> this.setState({  
> dataSource: this.state.dataSource.cloneWithRows(responseData.movies),  
> loaded: true,  
> });  
> }
>
> Far simpler.
>



### **Shai Almog** — August 22, 2016 at 3:25 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22879))

> Shai Almog says:
>
> That does change the syntax and we use it quite a bit in our JavaScript port. But it doesn’t really change the language semantics. Native API’s rely on threads and the ability to control them, this is true both for Android and iOS. JavaScript relies on hiding the complexity of threads.  
> There is a conceptual disconnect.
>



### **Shailesh** — September 17, 2016 at 1:25 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22819))

> Shailesh says:
>
> Such a misleading title. Comparing Java and javascript is totally different than comparing react-native with codename. The reason react-native is way superior is because of react component model. Building complex UIs is such a breeze in react. It is a lot lot simpler to keep the UI in a consistent clean state in react compared to other frameworks where finding the view and updating it, keep it in sync is such a pain.
>



### **Shai Almog** — September 17, 2016 at 4:11 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-24222))

> Shai Almog says:
>
> Codename One is Java based and shows what can be done with Java for mobile. It’s not as known as Java so I chose to use Java in the title since a lot of the arguments (specifically terseness) relate to improvements in the Java language.
>
> Cleanliness is up to the programmer more than anything.
>
> What you are talking about is separation of concerns which sounds great. Until you try to use it in real life and need access to this thing from that place which you didn’t expect and you end up crossing the language barrier of the separation with constant zigzags. This makes grepping your code for issues and sources a huge pain.
>
> Concerns can be separated by convention as well all good programmers do that anyway and they don’t need a separate UI representation to perform that in Java. The big advantage in doing everything in Java is that I can place a breakpoint anywhere and inspect Java based UI state right in the debugger, I can mutate/animate the UI with the same code/syntax I use to construct it which makes refactoring much easier.
>



### **IsMyBlueYourBlue** — October 11, 2016 at 5:58 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22929))

> IsMyBlueYourBlue says:
>
> Hmmm weird I don’t know why you say Java is simpler. For me the Javascript version is A LOT easier to read. I only have 7 years professional programming, is that a problem…?
>



### **Hristo Vrigazov** — October 23, 2016 at 2:28 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22981))

> Hristo Vrigazov says:
>
> I think my opinion would be pretty interesting… I started with Ionic and created a few apps, (have not written in React), and I like a lot of concepts in both JS and Java. To me, Codename One gives you more control, although more verbose. It is extremely easy to reuse code in Codename One due to the fact that you have the full OOP power with all its patterns etc. What is also cool is that you just use the IDE you know (IntelliJ in my case), and you can just send iOS, Android and Windows builds in the cloud and this way not to worry about configurations. Codename One also has amazing support. Javascript frameworks on the other side are very convinient with things like parsing JSON, callbacks, promises, which to me simplify the web, but these things can be used in Codename One also, if one wants, although slightly verbose. JS frameworks also have a lot of third-party frameworks that simplify things a lot (Codename One also has CN1 libs, but there are not as much out there). In my opinion, one should go the way that he likes best. For example, in my current app, I am actually integrating Codename One with Node.js backend, since the Loopback framework makes it so easy to quicky create REST APIs. No need to choose guys, learn the best of both worlds, don’t be close-minded.
>



### **Shai Almog** — November 1, 2016 at 1:11 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22924))

> Shai Almog says:
>
> I used the react tools and Microsofts C# tools and xcode and Codename One… Obviously I’m biased but I am informed.
>
> Using Visual Studio is like traveling back thru time for a person using a modern Java IDE, it’s like using Eclipse after you used NetBeans or IntelliJ. It constantly fails on basic things, doesn’t provide valuable hints and its debugger is just plain painful (inspecting variables etc.). So while I see the theoretical logic of comparing it to Java I can tell you that in practice Java is far more refined.
>
> Atom is surprisingly good as an editor, in fact I use it a lot for asciidoc editing. But it’s no IDE. Most JavaScript tooling isn’t nearly in the same level. When I talk to JavaScript devs they argue about the “need” for tooling. As a guy who started Java during the beta of 1.0 I can totally sympathize, I used a text editor and command line rather than use C++ and preferred it over visual studio of the day (was it visual studio 92 back then?). Anyway, I was right at the time but when tooling came to Java they brought productivity to a completely different level and they might eventually do the same for JavaScript. JavaScript needs tooling more than Java because the code hides far more meaning than the Java code.
>
> That’s what you missed about the code problems in JavaScript. The problem is that the Java code is very clear in its intention you point at X in the IDE and the IDE will tell you it’s an integer. In JavaScript you don’t even know what “this” is.
>
> React native itself has a slew of other issues, it has one nice thing with is the live preview/update. That is something we have in the simulator with “apply code changes” but it’s still pretty cool to have it “on-device”.
>
> I’m not sure if our tooling will be simpler for you since obviously there is a “filter” when picking up any technology and getting your brain used to it’s “oddities” and if you are not a Java guy to begin with the habits might be too deep. But our tools are WAY simpler and that’s obvious even during the installation phase not to mention in final projects where the IDE can literally show you the where & what of everything.
>



### **Justin L Mills** — November 9, 2016 at 4:42 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23181))

> Justin L Mills says:
>
> Can I code Haxe Java for Codename, Haxe can allow you to access most jars via -java-lib. Obviously you feel there are some advantages of your system over React Native but I could also use Haxe JS with React-Native, so what’s left beyond IDE’s when you consider some of the cross target Haxe libraries like Kha, OpenFL, Flambe, SnowKit etc… do you still feel that Codename One – a paid product offers something extra that Haxe developers might want to tap into for mobile development? It’s not a retorical question I am curious as I am aware that Haxe ecosytem does lack component support in many areas but at sametime is maybe nicer than Java or Javascript as a language 🙂 . Perhaps you could take a proper look and write another post around use of haxe with Codename One against maybe other options like Haxe c++ and Haxe JS wrapped.
>



### **Shai Almog** — November 10, 2016 at 5:03 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22865))

> Shai Almog says:
>
> In the past there was a guy in the forum who ported Haxe to Codename One but hasn’t followed up. Codename One is an open source product with a commercial SaaS on top. I would argue that’s better than a completely free product as it guarantees professional continuity and support.
>
> I’m not familiar enough with Haxe and I’m not really sure why one would pick it. But lets do it the other way… You try Codename One and write a guest post from a Haxe developers perspective?
>



### **Justin L Mills** — November 10, 2016 at 11:33 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22846))

> Justin L Mills says:
>
> It seems I need to install lots of tools and it’s not really clear how I might use Codename One using just Textmate and Terminal, since fancy tools are great but would get in the way of a proof of concept of mixing Haxe with Codename One.  
> An old tutorial of mine on using Slick and lwjgl jars with Haxe.
>
> [http://old.haxe.org/doc/jav…](<http://old.haxe.org/doc/java/lwjgl>)  
> So is there a Jar file for the Codename One components, that I could hook up to the Haxe and then a way to wrap the haxe jar up and send to your conversion servers?
>
> In terms of React there are native externs support for Haxe and some addons, Haxe probably has better strict typing than Java, being a bit more functional inspired so the js typing issues disappear.  
> [https://github.com/tokomlab…](<https://github.com/tokomlabs/haxe-react-addons>)  
> [https://github.com/kevinres…](<https://github.com/kevinresol/haxe-react-native>)
>
> At moment where I work I am not excited by the android solutions looking inside put me off learning Android java, so expect Codename One approach might do better, my collegue is creating React/Reflux touchscreen app, and feels it’s simpler than the AIR approach we have used for Kiosks in the past. So I would really be interested to know if Codename One could be easily hooked up to Haxe Java and I could prototype a similar Kiosk app with it, but I have no interest in coding in Java or really Javascript 🙂 And the docs online do not really give me the information I would need to set up a project that used Haxe Java for application code.
>
> Having learnt code through flash, I am not convinced by the closed source support arguments you use, since I saw Adobe largely desert as3 developers. But I know at work they love the C# backend supported approach, so there are swings and roundabouts, often closed provides better tooling, but you can’t branch the project if you don’t like where it’s going.
>
> But don’t expect you have time to setup a codename one haxe java demo but I would be very curious if you did.
>



### **Shai Almog** — November 11, 2016 at 7:22 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23185))

> Shai Almog says:
>
> There is actually very little to install for Codename One but using it without the tooling goes a bit against the grain of what we are trying to accomplish. Codename One has far fewer dependencies than any other tool out there and is really just an ant project with no external dependencies other than a couple of jars.
>
> I’m not sure I can help you with Haxe as it’s not a point of interest/focus for us. If you don’t like Java then Codename One might not be the best solution for you at this time.
>
> The Adobe argument doesn’t fit since Adobe didn’t open source flash. Codename One is open source (including the VM, ports etc.)… It’s more like Android in that sense.
>



### **Jason Nathan** — December 18, 2016 at 12:41 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22968))

> Jason Nathan says:
>
> My sentiments exactly. I think a deep dive into JS is needed to make a proper comparison. The Promise and “then”-problems described are really the woes of someone newly discovering JS, for example.
>



### **Shai Almog** — December 18, 2016 at 4:45 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-22837))

> Shai Almog says:
>
> We use futures in our JavaScript port. Nope.
>



### **ie5x** — January 11, 2017 at 10:29 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23241))

> ie5x says:
>
> I am not sure why you are comparing a language with a tool? Shouldn’t you either be comparing codenameone with React Native,  
> or Java with JavaScript?
>
> Anyways, using async/await syntax which is completely supported in React-Native –
>
> fetchData: async function() {  
> try {  
> let response = await fetch(REQUEST_URL),  
> responseData = response.json();  
> this.setState({  
> dataSource: this.state.dataSource.cloneWithRows(responseData.movies),  
> loaded: true,  
> });  
> }  
> catch (err){  
> this.setState({  
> error:err,  
> loaded:false  
> });  
> }  
> },
>
> Done!
>



### **Shai Almog** — January 12, 2017 at 5:08 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23162))

> Shai Almog says:
>
> I did compare with Codename One. Java has better name recognition so I used it for syndication purposes.  
> The async/await approach was mentioned in other comments. It’s not exactly a thread alternative more like futures which is fine but not the same.
>



### **Nitin Bansal** — February 4, 2017 at 7:39 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-21647))

> Nitin Bansal says:
>
> If JavaScript was/is so great, then why has it started to seem more and more like Java after ES6? And so goes for Java too, where it has started to seem more like Python after Java8. The fact is none of the languages were perfect. But, as time lapsed, and so the programmers’ experience in using these languages, and also the fact that current projects require exposure to more than one language, people have started adding more and more best features from other languages. Hence, java introduced lambdas and functional interfaces, while javascript introduced classes, which it hated at one time so much. Newer languages such as Go and Rust and Swift already come with balanced set of these features. As time goes by, we’ll see more of unite among the way languages handle their syntax, getting more and more diversed on where they fit best.
>



### **Shai Almog** — February 5, 2017 at 8:17 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23249))

> Shai Almog says:
>
> I agree there is a lot of convergence, it’s an artifact of taking a language designed for one purpose and re-purposing it after the fact.
>
> The languages are still very different especially when it comes to types, encapsulation etc.
>



### **Stan** — February 8, 2017 at 5:41 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23089))

> Stan says:
>
> and I’m willing to bet that Facebook has got a whole bunch of lower level code to suplement the javascript. I’ve been a software engineer for over 2 decades and have coded in many languages (C/C++, Ada, Fortran, Pascal, assembler, etc) and I’ve been coding Java since 1998 and by far, it’s still me language of choice hence which is why I’m starting to use Codename One. Not knocking Javscriipt because I like that as bwell and have been using it since t6he early 2000s but not much comparison to java as a heavyweight language.
>



### **Stan** — February 8, 2017 at 5:44 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23244))

> Stan says:
>
> Totally agree and has anybody heard of this new phenomena called “Javascript Fatigue”? It’s dizzying the number of frameworks, libraries, packagers and add ons that one must know…
>



### **anonymyst** — February 17, 2017 at 11:51 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23310))

> anonymyst says:
>
> Ya well Java doesn’t hold a candle to Swift. Besides, your claim may be true in some ways, but in the way that’s most glaring and important to most, is speed… I’ve got junior dev react native programmers that can code outcomes much faster than your senior dev java developers. Another way in which it fails, is in it’s inability to deliver a consistent user experience across 2 platforms… react native gives my clients a single codebase, which makes better business sense, so in that sense, it’s a major fail for both Java and Swift. So I’d hold back in speaking in such extremes as “superior in every way,” because it makes you sound biased and ignorant.
>



### **Shai Almog** — February 18, 2017 at 5:59 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23378))

> Shai Almog says:
>
> To each his own. I don’t like Swift personally and even if you are a big fan of the language it needs years to reach the maturity level of Java in tooling, 3rd party support, resources etc.
>
> I agree that a lot of Java programmers tend to “overthink” problems especially when it comes to the monstrosity that is Java EE. There is a cultural problem there. In JavaScript react you have the exact opposite of patchwork and unawareness of production/security problems that might occur. I’m sure that you are comparing react programmers to native Android programming which is horribly broken.
>
> I am biased (notice the site you are on) but I’m quite well informed.
>



### **carlos** — March 21, 2017 at 7:41 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23260))

> carlos says:
>
> Hi Shai,
>
> A quick peak at your CodenameOne looks great. In the past I’ve done a lot of Java client side development but I think you should know that React Native is way better than using Java because it provides an evolved way of developing GUI client software. I mean your method looks and feels like the 90’s when Swing was the state of the art on GUI development. Techniques have evolved to include a combination of imperative and declarative techniques as well as auto binding data. There are several Javascript frameworks that use these techniques (Angular, Aurelia, Ember, etc) but React is by far the simpler and most powerful of the bunch and hence why it is so popular.
>
> Use React Native to develop a full blown app and experience what is great about it and hopefully you can incorporate that into your CodenameOne system.
>
> Best of luck.
>
> Carlos.
>



### **carlos** — March 21, 2017 at 7:48 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23100))

> carlos says:
>
> Yes, and Webstorm is a fantastic Javascript IDE
>



### **Shai Almog** — March 22, 2017 at 4:57 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23338))

> Shai Almog says:
>
> Hi,  
> you are comparing Swing an old and outdated implementation of the idea with new implementations of JS frameworks so naturally the former will feel old and the latter will feel new.
>
> The imperative nature of these frameworks isn’t there in the name of progress, it’s in place to workaround the oddities and pains of the DOM/JavaScript combo. Imperative frameworks are problematic as they are harder to learn and debug due to the underlying “magic”.
>
> Where do these imperative frameworks provide any real world concrete advantage?
>
> They aren’t more terse (as I demonstrated more than once).
>
> They aren’t easier to debug or understand since their flow isn’t linear. So where is the progress?
>



### **carlos** — March 22, 2017 at 6:29 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-21569))

> carlos says:
>
> Shai,
>
> Your system, CodenameOne, is new so it should feel new but instead it looks like old technology using old techniques. You could had done something like JavaFX or better yet React Native but you are doing things like Swing !!! Your choice.
>
> You got imperative and declarative mixed up. Markup is declarative, Java is imperative, React combines amazingly imperative and declarative techniques.
>
> > The imperative nature of these frameworks isn’t there in the name of progress, it’s in place to workaround the oddities and pains of the DOM/JavaScript combo.
>
> Wrong. Their declarative nature has nothing to do with pains of DOM/Javascript. React Native has no DOM. Adobe Flex uses declarative techniques without DOM, so does JavaFX, etc.
>
> > Imperative frameworks are problematic as they are harder to learn and debug due to the underlying “magic”.
>
> Wrong. Some of these frameworks are harder to learn and debug but React is super simple and easy to debug, reason, read, etc. Do you think that it would take off like this if it was hard?
>
> > Where do these imperative frameworks provide any real world concrete advantage?
>
> Code is simpler (and faster) to read, write, maintain, debug, better architecture, re-use, scale, etc
>
> > They aren’t more terse (as I demonstrated more than once).
>
> Your ‘demonstration’ is a joke. Do a full app, with UI building code and event handling, etc. Do something like this: [https://github.com/junedomi…](<https://github.com/junedomingo/movieapp>)
>
> > They aren’t easier to debug or understand since their flow isn’t linear. So where is the progress?
>
> Wrong. You really need to actually learn something new and appreciate why it is better than what was before. You sound like a proud horse carriage owner praising its advantages over the car. Pride is preventing you from reaching your full potential.
>
> Carlos.
>



### **Shai Almog** — March 23, 2017 at 6:23 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23121))

> Shai Almog says:
>
> Codename One is based on work we did at Sun Microsystems during the time JavaFXScript was developed. So it predated JavaFX and React by several years. JavaFX’s failure and counter-innovation is something I just don’t want to go into as it’s a long and deep argument.
>
> There are quite a few things I’d like to correct here but I’m pretty busy with the bootcamp to write something extensive. Our system mixes some concepts from UI and model like Swing used to. Some developers might think that’s old… The fact that iOS/Android take that approach natively (because they are old?) is probably important.
>
> React is simpler than doing barebones AJAX but it’s not simpler than just coding the UI directly. Not every idea that comes later and fits into a new structure is better e.g. Linus came back with monolithic kernels years after the debate for microkernels “won”. The reason for Reacts success relates to the problems of the system it is layered on top.
>
> We took an age old solution to a common problem and provide a lot of advantages/capabilities you just can’t do with React Native (and yes I based this code on the react tutorial demo so I’m very aware of what’s there).
>



### **Rick** — April 8, 2017 at 10:08 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23427))

> Rick says:
>
> But aren’t you missing the whole point? I hardly believe anyone will claim that non native code is better than the real deal.
>
> But React is a response to a time when we have a highly fragmented UI market (thanks Apple Store, Google Play, etc).  
> The idea is to leverage device homogeneity and at the same time minimize the performance impact as much as possible. Here React is just very good. You can quickly code an average mobile app in a short time that will run with minimal changes, if at all, on various devices. Try keeping a solid dev team with frequent releases using native only.
>
> And either you like it or not, functional style programming is very important now, more than ever actually. Functional style programming leverages the computing power of multi-core systems and aides with concurrency issues to render them almost trivial. Even the event driven paradigm helps with linear code design by dropping the need for stateful concurrency marshaling entirely. The code you say is hard to read is not when you learn how to go about these paradigms.
>
> And how about the top-down data flow model in react? This means no event/data cross binding hell.
>
> I have 19 years experience in this field, so I’m almost as old as you in this regard. And I repeat, I acknowledge you do have very good points, but your blog doesn’t make sense to me. It’s like comparing cars to planes A Ferrari is definitely an immensely better machine at its job than a Cessna, and although both address the same common need, transportation, they both respond to different circumstances and socio-historical driving forces. The Cessna is able to solve the much more sophisticated need for fast travel vs convenience vs cost debacle, and that is the point.
>



### **Shai Almog** — April 9, 2017 at 4:02 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23261))

> Shai Almog says:
>
> Codename One is more native than React Native. On iOS code is translated to native code and compiled using XCode not run thru JavaScript JIT. On Android Java is running directly on the Android VM. We allow embedding native widgets (e.g. Google Maps etc.) so in most aspects Codename One is more native.
>
> However, let me attack the “native is best” dogma that has undertaken our industry. That dogma didn’t exist 10 years ago. It came to be because iOS UI’s were so radically different there was no other way, the same was true for Android. Over the years both Android and iOS converged a lot. The cross platform tools have also made great strides and are now rivaling native.  
> The fact that you can take a design, caching system and features quickly everywhere is huge in terms of delivering fast functionality and update to your user base. Update speed for applications is probably more important than anything in mobile.
>
> I’m not a fan of functional programming for real applications. It’s great for math but without encapsulation or imperative style it becomes a maintenance problem down the road. The idea that multi-core will be leveraged by this style is one that the JS crowd has been pushing for years, it “might” be valid for NodeJS but it’s not for react native where the UI is single threaded and any bit of “adventure” off the main UI thread will demolish you. Our GC & networking run in their own threads and make use of multi-core and since our EDT is separate from the main OS thread we leverage multi-core better while increasing portability.
>



### **lambdatoast** — May 18, 2017 at 10:42 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23365))

> lambdatoast says:
>
> You don’t have to have your codebase in JavaScript when using React Native (e.g. I’m using React Native and the code I write is in a language whose static type system is light years ahead of what Java will ever reach).
>
> So this blog just post amounts to you showing that that bit of Java code that’s more familiar to you (and which has type checking, although caveman era-type checking that doesn’t track side-effects in the types, and is therefore doomed to become a pain to reason about as it grows…. sorry, that’s the Haskeller in me, ranting), reads better to you than that dynamic language garbage they used in the React Native example.
>
> BTW not only are we able to use something other than JavaScript with React Native, but we can even use plain old JavaScript, and for the parts we choose, add type annotations in the comments, and have FB’s Flow type check it *statically*, aaaand Flow’s type system is already more advanced than Java (as in stronger type guarantees. e.g. isn’t it pathetic how Java can’t type-track uses of NULL at compile time. Ugh).
>
> BTW, to all peoples interested in static typing: Please check out something like PureScript + React (or React Native, or both), in order to understand why languages like Java remain in the dark ages of static typing. Do NOT get your idea of what types can do for you based on OOP languages like Java, which ruin all typing guarantees with their out-of-control side-effects.
>



### **Shai Almog** — August 14, 2017 at 7:07 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23603))

> Shai Almog says:
>
> No. It’s the exact same number of lines and works perfectly on iOS. Clearly you haven’t read what I said.  
> The logic can be shared just as well in Java or even better… Great you can share 80% of the styles in CSS (I call bullshit but fine) in Codename One you can share 100% of the styles.
>



### **Albert Gao** — October 9, 2017 at 4:15 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23666))

> Albert Gao says:
>
> React native could share codes across web and native, even for the UI (Production ready for a long time and already some big names did that in production like MS and Airbnb). And it has an unique pattern to encapsulate the related UI and logic into a component. These are something CodeNameOne just can’t compete with.
>
> But I do see the use cases for CodeNameOne, as our company, we have a web site which is not based on react stack, and we adopted kotlin in part of our back-end. It makes CodeNameOne perfect sense for us when considering a solution for delivering mobile app. And as I can see from the doc, CN1’s app life cycle is pretty straightforward and easy to understand for a mobile developer.
>
> I just wish you guys could:  
> 1\. Make the import of kotlin lib (coroutine and reflection) viable, so we could use CN1 to write an app while still use our existing kotlin libs.  
> 2\. Wish the new UI designer could work with kotlin soon, come from a python cross-platform framework named Kivy, any time you start to code the UI via code (not only python, for java, kotlin, javascript…). It makes you such a pain when the UI becomes complex. I can see the UI designer will generate a XML which is a good sign.  
> 3\. Add some 2-way binding or at least 1-way binding mechanism to lessen the code to write.
>
> There are always good thing to take from new tech or ideas. Use that to strengthen CodeNameOne would be better.
>



### **Shai Almog** — October 9, 2017 at 7:59 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23787))

> Shai Almog says:
>
> I think a better place for this is in the kotlin post as it relates more closely to that. We can compile codename one apps directly to JavaScript with no code changes so I’d say we are way ahead if what you are trying to do is build an app and not a website.  
> I think partial code sharing is problematic, you end up having to constantly test any change to shared code lowering the value of the reward. A lot of the abstractions and patterns of react are there since there to abstract issues in the underlying platform. Abstraction is less valuable when you have one common abstraction for everything, when that exists you can just work directly and get more platforms as a result.
>
> 1\. I’m not sure about the complexities of those. I think some of that should be portable.  
> 2\. That’s doable. The main obstacle for doing this is people/resources.  
> 3\. We have a properties binding framework, it’s oriented at Java but might work well for kotlin see: [https://www.codenameone.com…](<https://www.codenameone.com/how-do-i-use-properties-to-speed-development.html>)
>



### **Albert Gao** — October 9, 2017 at 8:08 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23793))

> Albert Gao says:
>
> Thanks for the reply. If I understand you right, it’s more like a web-first or app-first approach difference now 🙂 And CN1 is the latter, makes sense now 🙂 Will surely look into that properties binding framework, as far as I can see, my simple example works really well with Kotlin.
>
> And thanks for the reply. You are very responsive for my questions which split over places 😀 (And you are even more responsive than some big name open source project) Your attitude to the product will SURELY help my evaluation! Appreciate!
>



### **nathan32** — October 13, 2017 at 7:54 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23574))

> nathan32 says:
>
> I think the biggest disadvantage of codename one is the “Environment”. React Native is truly opensource. As for mac version we can buy a mac mini and set it up like a server for the whole office and it costs me just $499, that equals just 6 months of professional subscription we pay for codename one. If we’re uncomfortable or prohibited by client contract from uploading the content to a 3rd party server we shell out $399 each month, honestly we can equip my whole office with macs in what we would pay CN1.  
> As for horrible Android emulator, React Native provides an easy way to build app using Expo and even preview it on ios/android mobile using the Expo client ([https://expo.io/)](<https://expo.io/>)), So just scan the code and it shows a preview on the device. Any code changes is reflected instantly on the device. That one feature has made our lives a lot easier. Easy DOM manipulation is good for business apps interacting frequently with a backend.  
> Except for that I find both toolkits similar, the standard gui principles are used. It boils down to the language of your preference (both JS and Java fall on the disliked side of things for me).  
> We went with React Native for just those reasons, If you could provide a truly opensource toolkit we’d have been on the side of Codename One. A simple VBox image of a pre-configured server would make CN1 more popular, to test this just release one vbox image and see for yourself. Just the response to a linux based server for android builds might surprise you.
>



### **Shai Almog** — October 13, 2017 at 8:20 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23801))

> Shai Almog says:
>
> Your response reads like one of someone who has made up his mind and is trying to justify it. Just in case it isn’t here’s the way I see it:
>
> – We are 100% open source just like react native. No less. React native doesn’t have build servers and sharing a Mac doesn’t really give you anything remotely close to that experience. Saying that this piece which doesn’t exist in the open source project is the reason we aren’t as open is like me saying that React Native’s VM isn’t open source like our VM’s are.
>
> – You can get a basic subscription for your office for less than a cost of a Mac if all you care about is the build server functionality. Having said that the pro subscription includes FAR more features than that and full support. That’s something you can’t buy fro any amount of money in react. You can’t pay Facebook (not a 3rd party) to give you professional support.  
> With the enterprise subscription you buy a lot of things that go well beyond offline build, specifically you buy partial control over the direction of the project and a direct line to us. There are many other features besides offline build etc. Its meant for enterprises not startups so if price is an issue don’t buy it. The fact that there are paid options is an advantage for us, not a disadvantage.
>
> – We support Kotlin not just Java although Java is the main target. We could support any language supported by the JVM from Ruby to Jython to Scala etc. There is even a nifty guide Steve wrote explaining the steps he took in porting Kotlin (FYI he also added unofficial support for Mirah a few years back): [https://www.codenameone.com…](<https://www.codenameone.com/blog/how-to-port-jvm-languages-to-codename-one.html>)
>
> – There used to be a 3rd party offline build project, we hired its maintainer (not to stop the project, he’s just a really great hacker). When that project ran very few people used it, it might still work but most people don’t really care about that stuff so the last paragraph should be reversed. “You would be surprised”.
>



### **nathan32** — October 13, 2017 at 8:52 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23762))

> nathan32 says:
>
> No its not, until you are using custom native modules refer [https://facebook.github.io/…](<https://facebook.github.io/react-native/docs/getting-started.html>) You can simply use expo client ([https://expo.io/)](<https://expo.io/>)). The opposite however is a point of concern. I can do a native build in case of React Native while Codename One native builds though not impossible are costly as hell $400/mo. I can dedicate a machine for doing builds for that amount. So effectively for one month of Codename One Build server I can get a react one for lifetime.
>



### **Shai Almog** — October 13, 2017 at 10:00 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23755))

> Shai Almog says:
>
> Native builds in Codename One are between free (with quota limits) to 19USD per month with no limits. I have no idea why you decided the $399 enterprise version is needed when about 1% of our users actually pick that option. From that 1% very few use the offline builds which even our promotional material recommends against see: [https://www.codenameone.com…](<https://www.codenameone.com/how-do-i-use-offline-build.html>)
>
> Again you totally ignore the fact that you can use our source code for free and that it delivers pretty much everything. You also ignored all the other points I made.
>
> About expo, that is indeed a nice feature. We have a proof of concept that will allow something similar in Codename One (on-device-debugging) without the build cycle. Unfortunately this would require some work to bring to production.
>



### **Shai Almog** — November 7, 2017 at 11:01 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23729))

> Shai Almog says:
>
> John. We make our money through the SaaS product which is our chief source of income. That guarantees sustainability unlike a company like Facebook or Google both of whom proved they have no problem throwing away a product used by millions because it didn’t gather revenue… We only support push notification in the pro level but if you look in the parse plugin for Codename One you will find that this isn’t a requirement there. It’s not something we advertise as we can’t guarantee it but it works fine.  
> There are also people and projects by third parties who use our source code to build their apps, if you look in our discussion group just the other day a user posted about a free plugin for building apps. Naturally that isn’t a sustainable model and you can’t expect us to support it.
>
> Legally we can’t publish “vbox images” as we can’t re-distribute Mac OS or Windows. Your assumption that our backend application is something a human being can reasonably setup with a guide off the internet is incorrect. Our architecture is ridiculously complex.
>
> About open source being free of charge. That’s just wrong. Free software != free beer.  
> The fact that we charge is a huge advantage. If something breaks or needs fixing in a “free” product you are stuck. There is no one there. You can pay a consultant but that’s a coin toss. Here you have a guarantee by the authors of the product that we provide support. With Facebook even if you are a huge company you can’t buy that guarantee because react isn’t a product that makes money for Facebook.  
> Furthermore, if you appreciate your craft you pay for good tools. There are 3 great Java IDE’s and the most popular among them isn’t completely free (IntelliJ). It’s still open source and it’s still a great IDE that is worth the money. We pay a large salary to our developers it makes sense to spend a little on their tools so they work more effectively.
>
> About expo it didn’t exist when I wrote the article. Regardless it’s nothing like Codename One which is a WORA solution something that React Native is not (based on Facebooks explicit definition). Build servers are possible with a WORA lightweight architecture but with a heavyweight architecture (one that relies on native peers for everything) this is problematic as you need a far larger set of testing devices to verify anything. Cloud build becomes a hindrance rather than an advantage in that case.
>



### **Nick** — February 26, 2018 at 4:48 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23951))

> Nick says:
>
> A little late but I found it amusing you speak very highly of “separation of responsibilities” but you have view rendering logic coupled tightly with your data fetching logic, which a framework like React encourages to actually separate (don’t know about Codename one).
>



### **Shai Almog** — March 3, 2018 at 12:12 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23765))

> Shai Almog says:
>
> Sure, it’s demo code so that’s how I treated this. I don’t think a framework should “force” separation.  
> It sounds good on paper but when you need to cross the separation bounds to do some back and forth logic things start getting “hazy” and you end up having a lot of clutter all over the place just to pass through that artificial bridge.
>
> React native didn’t do that because it’s good engineering. That separation exists because the platform is in a different language and difference system. We let you access everything if you need to. Yes, power allows you to shoot yourself in the foot (i.e. threads) but it’s still more powerful to have the choice and languages that support encapsulation.
>
> FYI this article is really old and actually pulls a lot of punches from React Native. I should seriously revisit it since things are pretty different by now.
>



### **James Line** — June 14, 2018 at 1:38 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23856))

> James Line says:
>
> To be fair the Android emulator has gotten a fair bit faster since you wrote the article.
>



### **Iftee Khar Ul Islam** — June 20, 2019 at 2:27 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-24117))

> Iftee Khar Ul Islam says:
>
> I like everything about CodenameOne, but I think there should be a **comparison with Flutter**, Google’s new framework cross-platform lover dudes may know it very well, I loved it but only thing that brought me here is that **CodenameOne doesn’t increase App size as flutter**, there’s other pros and cons but you should go with the one you like, I like both hence I will use both (there are migration tutorials for flutter, i.e. Flutter for Android devs, iOS devs, which enabled me to learn and use both)
>



### **Iftee Khar Ul Islam** — June 20, 2019 at 2:31 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23967))

> Iftee Khar Ul Islam says:
>
> A basic ‘Hello World’ app in flutter produces a 7.5mb file, where CodenameOne uses 1.7mb only, so for Small or relatively small apps using CodenameOne is a choice you won’t regret (Flutter may increase app size due to everything in Flutter is a Widget! and it also includes a C++ , Skia engine for performance which makes it’s size relatavely larger) …
>



### **Michael** — September 16, 2019 at 11:18 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-23912))

> Michael says:
>
> There are an awful lot of [comma splices](<https://www.grammarly.com/blog/comma-splice/>) in this article.
>



### **Merkle Groot** — February 7, 2020 at 11:51 pm ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-21377))

> [Merkle Groot](https://lh3.googleusercontent.com/-wgRg-3_Yiws/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rfK-pe_xm40uIAPWNZP0XwW8eHSwg/photo.jpg) says:
>
> React Native lets me use one code base for Android, IoS, Web, UWP, and cross-platform Desktop.  
> WHERE IS YOUR JAVA NOW?????
>



### **Shai Almog** — February 8, 2020 at 4:43 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-21380))

> Shai Almog says:
>
> Ahem. Did you look at the website you’re in???  
> We target all these platforms and unlike react native we’re even more portable in terms of the code base, it’s truly one code.
>



### **Crab Synth** — May 22, 2020 at 1:56 am ([permalink](https://www.codenameone.com/blog/java-is-superior-to-react-native-in-practically-every-way.html#comment-21407))

> [Crab Synth](https://lh3.googleusercontent.com/a-/AOh14GgDWPylqC3Tu2Tn2VKYsC4FGkgFlhb9leY7laYR) says:
>
> Fantastic Post… especially the comments… keep coming back to it, many times over the years… and i must commend you Shai… you had an appropriate answer for everything…. and even though conventions, preferrences and styles will continue to segragate users, causing them to pick a side… i believe by reading the comments and your replies i have a much better understanding of how to separate the hype from the features of React and how Java can achieve everything asked of it.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
