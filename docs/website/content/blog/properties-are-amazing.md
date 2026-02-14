---
title: Properties are Amazing
slug: properties-are-amazing
url: /blog/properties-are-amazing/
original_url: https://www.codenameone.com/blog/properties-are-amazing.html
aliases:
- /blog/properties-are-amazing.html
date: '2017-04-12'
author: Shai Almog
---

![Header Image](/blog/properties-are-amazing/properties.jpg)

I wrote about [properties](/blog/properties.html) before but I got a sense most people didn’t understand how amazing they truly are. In this post I would like to show you something they can do that’s **incredibly cool** and practical for your every day usage.

To recap properties are high level attributes of an object that expose a mutable getter/setter which we can observe/introspect. I discussed these terms in the [previous article](/blog/properties.html) so you can check there for a deeper recap. In this article I’ll show a cool demo and make it cooler with every stage!

Lets assume I have an object called `Contacts` which includes contact information of contact e.g.:
    
    
    public class Contact implements PropertyBusinessObject {
        public final IntProperty<Contact> id  = new IntProperty<>("id");
        public final Property<String, Contact> name = new Property<>("name");
        public final Property<String, Contact> email = new Property<>("email");
        public final Property<String, Contact> phone = new Property<>("phone");
        public final Property<Date, Contact> dateOfBirth = new Property<>("dateOfBirth", Date.class);
        public final Property<String, Contact> gender  = new Property<>("gender");
        public final IntProperty<Contact> rank  = new IntProperty<>("rank");
        public final PropertyIndex idx = new PropertyIndex(this, "Contact", id, name, email, phone, dateOfBirth, gender, rank);
    
        @Override
        public PropertyIndex getPropertyIndex() {
            return idx;
        }
    
        public Contact() {
            name.setLabel("Name");
            email.setLabel("E-Mail");
            phone.setLabel("Phone");
            dateOfBirth.setLabel("Date Of Birth");
            gender.setLabel("Gender");
            rank.setLabel("Rank");
        }
    }

For those of you who don’t recall something like this:
    
    
    public final Property<String, Contact> name = new Property<>("name");

Is roughly equivalent to this:
    
    
    private String name;
    
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

And you use it like this:
    
    
    contact.name.set("MyName");
    Log.p("My name is: " + contact.name.get());

So what’s so great about it?

  * Seamless persistence

  * Automatic UI Binding

  * Automatic UI generation

  * Seamless parsing

All of that is made possible because we can query information about the properties from the parent object and we can observe changes on the properties!

### Seamless Serialization

Normally to make a Codename One object serialize you need to implement our Externalizable interface and do some heavy lifting. You also need to register this object so the VM will be aware of it. Codename One business objects are seamlessly externalizable and you just need to register them.

E.g. you can do something like this in your `init(Object)` method:
    
    
    new Contact().getPropertyIndex().registerExternalizable();

After you do that once you can write/read contacts from storage if you so desire:
    
    
    Storage.getInstance().writeObject("MyContact", contact);
    
    Contact readContact = (Contact)Storage.getInstance().readObject("MyContact");

This will obviously also work for things like `List<Contact>` etc…​

But this gets better!

### Seamless SQL Storage

I don’t like writing SQL, I’d much rather work with objects as much as possible. Which is why `SQLMap` is such an important API for me in the properties support. `SQLMap` allows CRUD (Create Read Update Delete) operations on the builtin SQLite database using property objects.

If we continue the example from above to show persistence to the SQL database we can just do something like this:
    
    
    private Database db;
    private SQLMap sm;
    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        Log.bindCrashProtection(true);
    
        try {
            Contact c = new Contact();
            db = Display.getInstance().openOrCreate("propertiesdemo.db"); __**(1)**
            sm = SQLMap.create(db); __**(2)**
            sm.setPrimaryKeyAutoIncrement(c, c.id); __**(3)**
            sm.createTable(c); __**(4)**
        } catch(IOException err) {
            Log.e(err);
        }
    }

In the above code we do the following:

__**1** | Create or open an SQLite database using the standard syntax  
---|---  
__**2** | Create a properties binding instance  
__**3** | Define the primary key for contact as `id` and set it to `auto increment` which will give it a unique value from the database  
__**4** | Call SQL’s createTable if the table doesn’t exist yet!  
  
__ |  Notice that at this time altering a created table isn’t possible so if you add a new property you might need to detect that and do an `alter` call manually   
---|---  
  
We can then add entries to the contact table using:
    
    
    sm.insert(myContact);

We can update an entry using:
    
    
    sm.update(myContact);

And delete an entry using:
    
    
    sm.delete(myContact);

Listing the entries is more interesting:
    
    
    List<PropertyBusinessObject> contacts = sm.select(c, c.name, true, 1000, 0);
    
    for(PropertyBusinessObject cc : contacts) {
        Contact currentContact = (Contact)cc;
    
       // ...
    }

The arguments for the `select` method are:

  * The object type

  * The attribute by which we want to sort the result (can be null)

  * Whether sort is ascending

  * Number of elements to fetch

  * Page to start with – in this case if we have more than 1000 elements we can fetch the next page using `sm.select(c, c.name, true, 1000, 1)`

There are many additional configurations where we can fine tune how a specific property maps to a column etc.

#### What’s Still Missing

The `SQLMap` API is very simplistic and doesn’t try to be Hibernate/JPA for mobile. So basic things aren’t available at this time and just won’t work. This isn’t necessarily a problem as mobile databases don’t need to be as powerful as server databases.

##### Relational Mappings/JOIN

Right now we can’t map an object to another object in the database with the typical one-many, one-one etc. relationships that would could do with JPA. The `SQLMap` API is really simplistic and isn’t suited for that level of mapping at this time.

If there is demand for this it’s something we might add moving forward but our goal isn’t to re-invent hibernate.

##### Threading

SQLite is sensitive to threading issues especially on iOS. We mostly ignored the issue of threading and issue all calls in process. This can be a problem for larger data sets as the calls would usually go on the EDT.

This is something we might want to fix for the generic SQLite API so low level SQL queries will work with our mapping in a sensible way.

##### Alter

Right now we don’t support table altering to support updated schemas. This is doable and shouldn’t be too hard to implement correctly so if there is demand for doing it we’ll probably add support for this.

##### Complex SQL/Transactions

We ignored functions, joins, transactions and a lot of other SQL capabilities.

You can use SQL directly to use all of these capabilities e.g. if you begin a transaction before inserting/updating or deleting this will work as advertised however if a rollback occurs our mapping will be unaware of that so you will need to re-fetch the data.

You will notice we mapped auto-increment so we will generally try to map things that make sense for various use cases, if you have such a use case we’d appreciate pull requests and feedback on the implementation.

##### Caching/Collision

As mentioned above, we don’t cache anything and there might be a collision if you select the same object twice you will get two separate instances that might collide if you update both (one will “win”).

That means you need to pay attention to the way you cache objects to avoid a case of a modified version of an object kept with an older version.

### UI Binding

One of the bigger features of properties are their ability to bind UI to a property. E.g. if we continue the sample above with the `Contact` class let’s say I have a text field on the form and I want the property (which I mapped to the database) to have the value of the text field. I could do something like this:
    
    
    myNameTextField.setText(myNameTextField.getText());
    myNameTextField.addActionListener(e -> myContact.name.set(myNameTextField.getText());

That would work nicely but what if I changed property, that wouldn’t be reflected back into the text field?

Also that works nicely for text field but what about other types e.g. numbers, check boxes, pickers etc. this becomes a bit more tedious with those.

Binding makes this all seamless. E.g. the code above can be written as:
    
    
    UiBinding uib = new UiBinding();
    uib.bind(myNameTextField, myContact.name);

The cool thing is that this works with multiple component types and property types almost magically. Binding works by using an adapter class to convert the data to/from the component. The adapter itself works with a generic converter e.g. this code:
    
    
    uib.bind(myRankTextField, myContact.rank);

Seems similar to the one above but it takes a String that is returned by the text field and seamlessly converts it to the integer needed by rank. This also works in the other direction…​

We can easily build a UI that would allow us to edit the `Contact` property in memory:
    
    
    Container resp = new Container(BoxLayout.y());
    UiBinding uib = new UiBinding();
    
    TextField nameTf = new TextField();
    uib.bind(c.name, nameTf);
    resp.add(c.name.getLabel()). __**(1)**
            add(nameTf);
    
    TextField emailTf = new TextField();
    emailTf.setConstraint(TextField.EMAILADDR);
    uib.bind(c.email, emailTf);
    resp.add(c.email.getLabel()).
            add(emailTf);
    
    TextField phoneTf = new TextField();
    phoneTf.setConstraint(TextField.PHONENUMBER);
    uib.bind(c.phone, phoneTf);
    resp.add(c.phone.getLabel()).
            add(phoneTf);
    
    Picker dateOfBirth = new Picker();
    dateOfBirth.setType(Display.PICKER_TYPE_DATE); __**(2)**
    uib.bind(c.dateOfBirth, dateOfBirth);
    resp.add(c.dateOfBirth.getLabel()).
            add(dateOfBirth);
    
    ButtonGroup genderGroup = new ButtonGroup();
    RadioButton male = RadioButton.createToggle("Male", genderGroup);
    RadioButton female = RadioButton.createToggle("Female", genderGroup);
    RadioButton undefined = RadioButton.createToggle("Undefined", genderGroup);
    uib.bindGroup(c.gender, new String[] {"M", "F", "U"}, male, female, undefined); __**(3)**
    resp.add(c.gender.getLabel()).
            add(GridLayout.encloseIn(3, male, female, undefined));
    
    TextField rankTf = new TextField();
    rankTf.setConstraint(TextField.NUMERIC);
    uib.bind(c.rank, rankTf); __**(4)**
    resp.add(c.rank.getLabel()).
            add(rankTf);

__**1** | Notice I use the label of the property which allows better encapsulation  
---|---  
__**2** | We can bind picker seamlessly  
__**3** | We can bind multiple radio buttons to a single property to allow the user to select the gender, notice that labels and values can be different e.g. “Male” selection will translate to “M” as the value  
__**4** | Numeric bindings “just work”  
  
![Properties form for the contact](/blog/properties-are-amazing/properties-demo-binding.png)

Figure 1. Properties form for the contact

#### Binding Object & Auto Commit

I skipped a couple of fact about the `bind()` method. It has an additional version that accepts a `ComponentAdapter` which allows you to adapt the binding to any custom 3rd party component. That’s a bit advanced for now but I might discuss this later.

However, the big thing I “skipped” was the return value…​ `bind` returns a `UiBinding.Binding` object when performing the bind. This object allows us to manipulate aspects of the binding specifically unbind a component and also manipulate auto commit for a specific binding.

Auto commit determines if a property is changed instantly or on `commit`. This is useful for a case where we have an “OK” button and want the changes to the UI to update the properties only when “OK” is pressed (this might not matter if you keep different instances of the object). When auto-commit is on (the default which you can change via `setAutoCommit` in the `UiBinding`) changes reflect instantly, when it’s off you need to explicitly call `commit()` or `rollback()` on the `Binding` class.

`commit()` applies the changes in the UI to the properties, `rollback()` restores the UI to the values from the properties object (useful for a “reset changes” button).

Binding also includes the ability to “unbind” this is important if you have a global object that’s bound to a UI that’s discarded. Binding might hold a hard reference to the UI and the property object might create a memory leak.

By using the `disconnect()` method in `Binding` we can separate the UI from the object and allow the GC to cleanup.

#### UI Generation

Up until now this was pretty cool but if you looked at the UI construction code above you would see that it’s pretty full of boilerplate code. The thing about boilerplate is that it shows where automation can be applied, that’s the exact idea behind the magical “InstantUI” class. This means that the UI above can be generated using this code:
    
    
    InstantUI iui = new InstantUI();
    iui.excludeProperty(myContact.id); __**(1)**
    iui.setMultiChoiceLabels(myContact.gender, "Male", "Female", "Undefined"); __**(2)**
    iui.setMultiChoiceValues(myContact.gender, "M", "F", "U");
    Container cnt = iui.createEditUI(myContact, true); __**(3)**

__**1** | The id property is useful for database storage but we want to exclude it from the UI  
---|---  
__**2** | This implements the `gender` toggle button selection, we provide a hint to the UI so labels and values differ  
__**3** | We create the UI from the screenshot above with one line and it’s seamlessly bound to the properties of myContact. The second argument indicates the “auto commit” status.  
  
This still carries most of the flexibilities of the regular binding e.g. I can still get a binding object using:
    
    
    UiBinding.Binding b = iui.getBindings(cnt);

You might not have noticed this but in the previous vebose code we had lines like:
    
    
    emailTf.setConstraint(TextField.EMAILADDR);

You might be surprised to know that this will still work seamlessly without doing anything, as would the picker component used to pick a date…​

The picker component implicitly works for date type properties, numeric constraints and numbers are implicitly used for number properties and check boxes are used for booleans.

But how do we know to use an email constraint for the email property?

We have some special case defaults for some common property names, so if your property is named email it will use an email constraint by default. If it’s named url or password etc. it will do the “right thing” unless you explicitly state otherwise. You can customize the constraint for a specific property using something like:
    
    
    iui.setTextFieldConstraint(contact.email, TextArea.ANY);

This will override the defaults we have in place. The goal of this tool is to have sensible “magical” defaults that “just work” so if you can think of other cases like this that make sense let us know!  
email?

### The Code & Final Word

You can check out the code from this article [here](https://github.com/codenameone/PropertiesDemo).

I could go on, last time I discussed parsing (which is also seamless to/from JSON/XML) and there are many other features worth discussing. I can also go deeper into how this is all implemented and some of the history of this feature (it predated LWUIT, I started working on this back in 2005)…​

These will have to wait for another time and post when I can go more thoroughly into them or maybe I’ll do a video covering this. We used properties a lot in the bootcamp and so far I’m very happy that I brought them in.  
I think we can take this feature much further than what we have above, I think this is the “tip of the iceberg” that can bring Codename One to levels of productivity/RAD that we haven’t seen. I’d love to get feedback on all of these and how we can improve properties so you can leverage them better in your apps!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **kutoman** — April 14, 2017 at 10:27 am ([permalink](https://www.codenameone.com/blog/properties-are-amazing.html#comment-24144))

> thanks for the post! The UI binding part was new to me 🙂
>



### **Shai Almog** — April 15, 2017 at 4:26 am ([permalink](https://www.codenameone.com/blog/properties-are-amazing.html#comment-23390))

> Shai Almog says:
>
> It’s new for everyone it was just released 😉
>



### **Sachin Shah** — April 28, 2017 at 5:56 pm ([permalink](https://www.codenameone.com/blog/properties-are-amazing.html#comment-23312))

> Sachin Shah says:
>
> The UI binding sounds amazing. Do you see this forming the basis of a “reactive” UI within CN1?
>



### **Shai Almog** — April 29, 2017 at 6:17 am ([permalink](https://www.codenameone.com/blog/properties-are-amazing.html#comment-23464))

> Shai Almog says:
>
> I don’t know….
>
> I do agree we need to improve some processes such as tablet/phone code transferability, network/IO callback updates to the UI etc. It’s just hard for me to imagine the way a reactive pattern would fit here. If you have pseudo code you could imagine working (with some explanation of what it would do in theory) I’d be very interested in that.
>



### **james agada** — June 1, 2017 at 12:10 pm ([permalink](https://www.codenameone.com/blog/properties-are-amazing.html#comment-23402))

> james agada says:
>
> I think you just made my pet project possible. I will spend time on it and give more feedback. Fantastic.
>



### **james agada** — June 8, 2017 at 1:16 pm ([permalink](https://www.codenameone.com/blog/properties-are-amazing.html#comment-23544))

> james agada says:
>
> Been playing with this a bit. My use case is simply a survey app. I define the app in JSON or similar format and render it for data collection and viewing. Will look at using InstantUI to implement but will have to find a way to work without predefined PropertyBusinessObject class.
>



### **Francesco Galgani** — August 25, 2017 at 7:23 pm ([permalink](https://www.codenameone.com/blog/properties-are-amazing.html#comment-23670))

> Francesco Galgani says:
>
> (This is the third time that I try to post this comment… maybe there are technical problems)
>
> Thank you for this article. I need some clarification, maybe I don’t understand how Java Generics are used with a Property. I’m in trouble with the Property syntax.
>
> Look at these two lines of code:  
> public final Property<Date, Contact> dateOfBirth = new Property<>(“dateOfBirth”, Date.class);  
> public final Property<String, Contact> gender = new Property<>(“gender”);
>
> My questions:
>
> 1\. Why do you need to specify the “Contact” class inside the diamonds? Isn’t obvious that the Property dateOfBirth and the Property gender are referred to the parent class, that is Contact?
>
> 2\. Why do you need to pass “Date.class” in the Property constructor in the first row?
>
> 3\. Why do you used Log.p(“My name is: ” + contact.get()); instead of Log.p(“My name is: ” + contact.name.get());?
>
> 4\. Have “IntProperty<Contact>” or “Property<Integer,Contact>” the same meaning exactly?
>



### **Shai Almog** — August 26, 2017 at 4:45 am ([permalink](https://www.codenameone.com/blog/properties-are-amazing.html#comment-23583))

> Shai Almog says:
>
> I saw the emails but I assumed you deleted the comments. disqus is sometimes annoying, sorry about that.
>
> 1\. Unfortunately there is no way to get the “parent” declaring I explained the need for this here: [https://www.codenameone.com…](<https://www.codenameone.com/blog/properties.html>)  
> The only reason this is needed is for chained set calls e.g. MyObject m = new MyObject().dateOfBirth.set(date).gender.set(“M”);
>
> 2\. Erasure. Generics are syntactic sugar. They are removed during compile and we have no idea what they were during runtime where we might need them e.g. for things like database mapping. The default mapping is string so that works. That’s also why we added DoubleProperty, IntProperty etc.
>
> 3\. Ugh. Because my brain had a compiler bug 😉  
> Thanks for the catch.
>
> 4\. Almost. There is also a getInt() method but this might change so I’d use IntProperty. I suggest checking out the code see [https://github.com/codename…](<https://github.com/codenameone/CodenameOne/blob/master/CodenameOne/src/com/codename1/properties/IntProperty.java>)
>



### **Francesco Galgani** — August 28, 2017 at 1:23 am ([permalink](https://www.codenameone.com/blog/properties-are-amazing.html#comment-21429))

> Francesco Galgani says:
>
> Thank you 🙂
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
