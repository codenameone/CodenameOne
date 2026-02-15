---
title: Avoiding Lists
slug: avoiding-lists
url: /blog/avoiding-lists/
original_url: https://www.codenameone.com/blog/avoiding-lists.html
aliases:
- /blog/avoiding-lists.html
date: '2016-08-21'
author: Shai Almog
---

![Header Image](/blog/avoiding-lists/property-cross-new.jpg)

When picking up a new UI API people often start with a list of items. Lists are often used for navigation, logic and data so it’s a natural place to start. Codename One’s List class is a bad place to start though…​ It’s complex and encumbered and has far better alternatives.

### How did we Get Here?

When we initially created the `List` API we were heavily inspired by Swing’s architecture. The ability to create an infinitely sized list without a performance penalty was attractive and seemed like a good direction for our original 2mb RAM target devices (back in 2006-7). We knew the renderer/model approach was hard for developers to perceive but we also assumed a lot of Swing developers would find it instantly familiar.

We made attempts to improve `List` in the years since e.g.: `MultiList`, `GenericListCellRenderer`, `ContainerList` etc.

These helped but the core problems of `List` remain.

### What Changed?

Modern interfaces are far more dynamic, we have features such as swipable containers, drag and drop to rearrange etc. The renderer approach complicates trivial tasks e.g.:

  * Variable sized entries – this is impossible in a standard `List` or `MultiList`. We designed `ContainerList` to solve this but it’s both ridiculously inefficient and buggy

  * More than one clickable item per row – it’s common to have more than one item within a row in the `List` that can handle an event. E.g. a delete button. This is a difficult (albeit possible) task for `List` items.

  * Performance – `List` can perform well but writing performant `List` code is a challenge. Anything under 5000 entries would perform better with alternative solutions. If you need more than 5000 rows, reconsider…​  
Scrolling beyond 1000 rows on a mobile device is challenging.

  * Customizability – You can customize the look of the `List` component but there are nuances and some limits.

  * Model – MVC is a good idea but it’s hard. Features like dynamic image download in lists challenge even experienced Codename One developers.

### What Should we use Instead?

This varies based on your needs but the general answer is a scrollable `BoxLayout.Y_AXIS` container.

#### The Simple Use Case

E.g. if I write a simple `List` such as this:
    
    
    com.codename1.ui.List<String> lst = new com.codename1.ui.List<String>("A", "B", "C");
    lst.addActionListener(e -> Log.p("You picked: " + lst.getSelectedItem()));

I can convert it to this:
    
    
    String[] abc = new String[] {"A", "B", "C"};
    Container list = new Container(BoxLayout.y());
    list.setScrollableY(true);
    for(String s : abc) {
        Button b = new Button(s);
        list.add(b);
        b.addActionListener(e -> Log.p("You picked: " + b.getText()));
    }

Admittedly there is more code in the second version, but it’s far more powerful and as your UI design grows the code will shrink by comparison!

E.g. if you don’t want the default look of the list or want thumbnail image, or want a single entry to behave differently the latter option is far simpler.

#### Lead Component

When you click an entry within the list you can click anywhere and it will work. If you compose an entry in the list from more than one piece those pieces act as one.

E.g. we have this code in the developer guide section covering `List` renderers:
    
    
    class ContactsRenderer extends Container implements ListCellRenderer {
     private Label name = new Label("");
     private Label email = new Label("");
     private Label pic = new Label("");
     private Label focus = new Label("");
    
     public ContactsRenderer() {
         setLayout(new BorderLayout());
         addComponent(BorderLayout.WEST, pic);
         Container cnt = new Container(new BoxLayout(BoxLayout.Y_AXIS));
         name.getAllStyles().setBgTransparency(0);
         name.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
         email.getAllStyles().setBgTransparency(0);
         cnt.addComponent(name);
         cnt.addComponent(email);
         addComponent(BorderLayout.CENTER, cnt);
    
         focus.getStyle().setBgTransparency(100);
     }
    
     public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected) {
         Contact person = (Contact) value;
         name.setText(person.getName());
         email.setText(person.getEmail());
         pic.setIcon(person.getPic());
         return this;
     }
    
     public Component getListFocusComponent(List list) {
         return focus;
     }
    }

We can create a similar container using this approach:
    
    
    Container list = new Container(BoxLayout.y());
    list.setScrollableY(true);
    for(Contact c : contacts) {
        list.add(createContactContainer(c));
    }
    
    private Container createContactContainer(Contact person) {
        Label name = new Label("");
        Label email = new Label("");
        Label pic = new Label("");
        Container cnt = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        name.getAllStyles().setBgTransparency(0);
        name.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        email.getAllStyles().setBgTransparency(0);
        cnt.add(name);
        cnt.add(email);
        name.setText(person.getName());
        email.setText(person.getEmail());
        pic.setIcon(person.getPic());
        return BorderLayout.center(cnt).
            add(BorderLayout.EAST, pic);
    }

The problem with this approach becomes obvious when we try to add an event listener…​.

We can make `name` into a `Button` but then what happens when a user clicks `email`?

We can make all the entries into buttons but that isn’t practical. That’s what [lead component](/manual/components/#lead-component-sidebar) is for, we can make one component into a button and it “takes the lead”. If we make name into a button and set it as the lead of the `Container` it will handle all the events and state changes for the entire row!

__ |  For more information on lead components check out [the sidebar](/manual/components/#lead-component-sidebar) in the developer guide.   
---|---  
  
We can change the code above like this and support lead components:
    
    
    private Container createContactContainer(Contact person) {
        Button name = new Button("", "Label");
        name.addActionListener(e -> Log.p("You clicked: " + person));
        // ...
        Container b = BorderLayout.center(cnt).
            add(BorderLayout.EAST, pic);
        b.setLeadComponent(name);
        return b;
    }

__ |  What do you do if you want to exclude an item from the lead component hierarchy (e.g. a delete button)?  
Check out [this blog post](/blog/unleading-mutating-accordion.html).   
---|---  
  
#### Infinite Scroll

One of our earliest demos showed off a million entries running on a 3mb Nokia mobile phone. While that is an impressive feat it isn’t useful.

Most real world UI’s use pagination to fetch more data when they reach the bottom of the scroll. This is predictable and easy to integrate both in the client and server code.

Two classes simplify the process of infinite scrolling list: `InfiniteContainer` and `InfiniteScrollAdapter`.

`InfiniteContainer` is an easy to use drop-in replacement to `Container`. `InfiniteScrollAdapter` is more versatile, you can apply it to any `Container` including the content pane. We have samples for both [InfiniteContainer](/javadoc/com/codename1/ui/InfiniteContainer/) and [InfiniteScrollAdapter](/javadoc/com/codename1/components/InfiniteScrollAdapter/) in the JavaDocs.

### Don’t Use Lists

In closing I’d like to re-iterate our recommendation: “Don’t use lists”. We didn’t deprecate those API’s because developers rely heavily on them & this might induce “panic”.  
There’s no valid reason to use a `List` as opposed to a `Container`. `List` is harder to use, slower & not as flexible.

We can’t cover every conceivable use case in this post so if you have a `List` or code you can’t imagine any other way, post it in the comments below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **bryan** — August 22, 2016 at 9:14 pm ([permalink](/blog/avoiding-lists/#comment-23004))

> bryan says:
>
> Agree with all this. I originally used Lists with custom cell renderers, and with the deprecation of the old GUI builder, I took the opportunity to refactor my code and change all Lists to Containers. Initially my thoughts were “it can’t work as well”, but in fact there appears to be zero performance penalty, and as Shai says, you can create a much better UI experience. Don’t use Lists !
>



### **Sadart** — August 23, 2016 at 4:31 am ([permalink](/blog/avoiding-lists/#comment-22749))

> Sadart says:
>
> True. Lists are horrible to deal with. I am still trying to recall when I used them. Stayed away from them years ago because stacking up containers made sense to me.
>



### **Jérémy MARQUER** — August 23, 2016 at 7:35 am ([permalink](/blog/avoiding-lists/#comment-22712))

> Jérémy MARQUER says:
>
> Totally agree and happy to read this post !! I initially work with complex List but I have refactored it recently. For example, I InfiniteProgress doesn’t animate correctly in items of my List -> I have changed it to InfiniteContainer and it works better !!
>



### **Shai Almog** — August 24, 2016 at 3:58 am ([permalink](/blog/avoiding-lists/#comment-23011))

> Shai Almog says:
>
> A user posted a question about searching within a list using the filter proxy model. That’s a great question that he seems to have deleted…
>
> The Toolbar JavaDoc contains two samples of searching within a container: [https://www.codenameone.com…](</javadoc/com/codename1/ui/Toolbar/>)
>
> Which also shows off animation within the search and quite a few other nice things. Notice that this isn’t demonstrated with an infinite container because searching thru that would require fetching all the data which might not be what you want to do so you will need to adapt the code to work with fetch logic (e.g. special webservice call for search like we do in property cross).
>



### **Carlos** — August 24, 2016 at 8:09 am ([permalink](/blog/avoiding-lists/#comment-22937))

> Carlos says:
>
> I did not delete the post, I have no idea what happened with it. Anyway, this is the code I posted before (thank you for the samples):
>
> textoFiltro.addDataChangeListener((int type, int index) -> {
>
> filtraProxy(listaRecetas, textoFiltro);
>
> });  
> ……..
>
> private void filtraProxy(final List listaRecetas, TextField textoFiltro) {
>
> Form f = Display.getInstance().getCurrent();
>
> FilterProxyListModel listaFiltro;
>
> if (listaRecetas.getModel() instanceof FilterProxyListModel) {
>
> listaFiltro = (FilterProxyListModel) listaRecetas.getModel();
>
> } else {
>
> if(textoFiltro.getText().length() == 0) {
>
> return;
>
> }
>
> listaFiltro = new FilterProxyListModel(listaRecetas.getModel()) {
>
> @Override
>
> protected boolean check(Object o, String str) {
>
> Hashtable h = (Hashtable) o;
>
> Object textoHash = h.get(“Listado”);
>
> return super.check(textoHash, str);
>
> }
>
> };
>
> listaRecetas.setModel(listaFiltro);
>
> }
>
> if (textoFiltro.getText().length() == 0) {
>
> listaRecetas.setModel(listaFiltro.getUnderlying());
>
> } else {
>
> listaFiltro.filter(textoFiltro.getText());
>
> }
>
> f.revalidate();
>
> }
>



### **Shai Almog** — August 25, 2016 at 5:21 am ([permalink](/blog/avoiding-lists/#comment-21457))

> Shai Almog says:
>
> Odd. I’ve seen messages disappear before but I always assumed they were deleted by the asker…
>



### **Jeff Crump** — September 1, 2016 at 5:44 pm ([permalink](/blog/avoiding-lists/#comment-22795))

> Jeff Crump says:
>
> I would prefer to continue to use the existing List class. When I first started to use Codename One I created an extend List class that utilizes a separate listmodel class, a multi-threaded downloader class, and a renderer class (which generates a prototype). I am able to place buttons, text and other components in the renderer class and have it manage states, mutable backgrounds and pass events to handle unique responses. The downloader class initially pulls two pages of images, then as the list scrolls it downloads additional pages, four at a time, then pauses until the next scroll. The list model class only fires an update when the image is still visible. My ListModel class also implements static filters on the data as the model is instantiated. All of my lists reside on tabs and work/scroll very well. The downloader class uses a two tier thread safe CacheMap. It easily handles 5,000 cells as the cell cache scrolls both directions. It is very fast and doesn’t suffer from pauses or jerky responses.
>



### **Shai Almog** — September 2, 2016 at 5:16 am ([permalink](/blog/avoiding-lists/#comment-22880))

> Shai Almog says:
>
> That might be one of the rare use cases for which list is indeed still superior.
>
> I think it’s pretty rare because navigating 5000 entries on mobile devices is probably too much for users and obviously the effort you had to put to get it going was pretty big… That’s the gist of this post. Yes there are edge cases that list can handle well but they are edge cases.
>



### **Jeff Crump** — September 2, 2016 at 12:52 pm ([permalink](/blog/avoiding-lists/#comment-23039))

> Jeff Crump says:
>
> I know, I just didn’t want to lose the existing List component. I wrote most of my code during the first few months after I started using Codename One. It was still very new and I had decided to go with it as is. So I added in what I wanted and worked around the rest. There have been many new components and upgrades since then and it has become a very capable platform. While we have tested very large lists, our target is actually about 500, and with the internal filters the length is between 65 and 85.
>



### **khmaies hassen** — April 20, 2017 at 10:32 pm ([permalink](/blog/avoiding-lists/#comment-23227))

> khmaies hassen says:
>
> when i reach the end of the list where there are no new pages to show and then i go up and pull the list to refresh, it gives me an empty page. how to reset “pageNumber” to 1 when i use pull to refresh?
>



### **Shai Almog** — April 21, 2017 at 4:44 am ([permalink](/blog/avoiding-lists/#comment-23455))

> Shai Almog says:
>
> In infinite container?
>
> Place a breakpoint in your callback code and make sure you return the right value on every call
>



### **khmaies hassen** — April 21, 2017 at 8:58 am ([permalink](/blog/avoiding-lists/#comment-21466))

> khmaies hassen says:
>
> Even in your example the same thing happenes when you reach the end
>



### **Shai Almog** — April 22, 2017 at 8:24 am ([permalink](/blog/avoiding-lists/#comment-23422))

> Shai Almog says:
>
> Which example
>



### **khmaies hassen** — April 22, 2017 at 12:22 pm ([permalink](/blog/avoiding-lists/#comment-23428))

> khmaies hassen says:
>
> [https://www.codenameone.com…](</javadoc/com/codename1/ui/InfiniteContainer/>)
>



### **Shai Almog** — April 23, 2017 at 5:33 am ([permalink](/blog/avoiding-lists/#comment-23521))

> Shai Almog says:
>
> I think that’s code that originally relied on InfiniteScrollAdapter which has no pull to refresh or index… You need to use the index value to determine the page you are on with InfiniteContainer
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
