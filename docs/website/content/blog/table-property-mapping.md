---
title: Table Property Mapping
slug: table-property-mapping
url: /blog/table-property-mapping/
original_url: https://www.codenameone.com/blog/table-property-mapping.html
aliases:
- /blog/table-property-mapping.html
date: '2018-08-14'
author: Shai Almog
---

![Header Image](/blog/table-property-mapping/properties.jpg)

If you aren’t using properties with Codename One, you probably should. Here’s a [post I wrote a while back covering them](/blog/properties-are-amazing.html), it should give you a lot of reasons for this. We are slowly integrating them into API’s such as `Rest` and as a result the code is simpler. A huge bonus is the type safety and flexibility that comes with this API.

Up until now binding in properties was mostly limited to “simple” classes such as `TextField`. Complex structures such as `Table` weren’t supported. This is no longer the case.

With the coming update `UIBinding` now includes a new API to bind a list of `PropertyBusinessObject` to a `TableModel`. This effectively allows the creation of table matching list of objects without writing any code!

### Storing Object Lists as JSON

Before we go into the table binding code we needed some API’s to support the following demo. One such API in `PropertyIndex` is:
    
    
    public static void storeJSONList(String name, List<? extends PropertyBusinessObject> objs);
    public <X extends PropertyBusinessObject> List<X> loadJSONList(String name);

These API’s let you store the data in the list of property objects into JSON. It also lets you load that JSON data into a newly created list. When we implemented this initially it failed for the date object. It turns out that storing Date objects into JSON isn’t standardized. Googling a bit showed that dates are often written like this in JSON: `2018-08-01T18:28:23.292`.

So this is now the standard for date objects stored/loaded into JSON.

### Sample

Lets look at how this work with a sample based on the old code I [wrote a while back](/blog/properties-are-amazing.html). You can check out the full project [here](https://github.com/codenameone/PropertiesTableBindingDemo). First we’ll start with the properties object itself which isn’t very different:
    
    
    public class Contact implements PropertyBusinessObject {
        public final IntProperty<Contact> id  = new IntProperty<>("id");
        public final Property<String, Contact> name = new Property<>("name");
        public final Property<String, Contact> email = new Property<>("email");
        public final Property<String, Contact> phone = new Property<>("phone");
        public final Property<Date, Contact> dateOfBirth = new Property<>("dateOfBirth", Date.class);
        public final Property<String, Contact> gender  = new Property<>("gender");
        public final IntProperty<Contact> rank  = new IntProperty<>("rank");
        private final PropertyIndex idx = new PropertyIndex(this, "Contact", id, name, email, phone, dateOfBirth, gender, rank);
    
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

There isn’t much to say about this object it’s pretty standard. So lets initialize it in the `start()` method:
    
    
    private List<Contact> listOfContacts;
    public void start() {
        if(current != null) {
            current.show();
            return;
        }
        if(!existsInStorage("contacts.json")) {
            listOfContacts = new ArrayList<>(); __**(1)**
            Contact c =new Contact().
                id.set(1).
                dateOfBirth.set(new Date()).
                name.set("Shai").
                gender.set("Male");
            listOfContacts.add(c);
            listOfContacts.add(new Contact().
                id.set(2).
                dateOfBirth.set(new Date()).
                name.set("Steve").
                gender.set("Male"));
            listOfContacts.add(new Contact().
                id.set(3).
                dateOfBirth.set(new Date()).
                name.set("Chen").
                gender.set("Male"));
            PropertyIndex.storeJSONList("contacts.json", listOfContacts); __**(2)**
        } else {
            listOfContacts = new Contact().getPropertyIndex().loadJSONList(
                "contacts.json"); __**(3)**
        }
    
        // rest of start method ...
    }

__**1** | If there are no entries I initialize the list to valid default values  
---|---  
__**2** | I store the list of objects to the `Storage` as JSON. Notice this is a static method as we already have object instances in the list  
__**3** | Loading the JSON does require an object type for context  
  
__ |  Notice that this won’t work for JSON that contains more than one type of object. So I can’t store `Contact` and `Person` in a single JSON file   
---|---  
  
The resulting JSON file looks like this:
    
    
    [{
      "gender": "Male",
      "name": "Shai",
      "dateOfBirth": "2018-08-14T18:27:43.585",
      "id": 1
    },
    {
      "gender": "Male",
      "name": "Steve",
      "dateOfBirth": "2018-08-14T18:27:43.585",
      "id": 2
    },
    {
      "gender": "Male",
      "name": "Chen",
      "dateOfBirth": "2018-08-14T18:27:43.585",
      "id": 3
    }]

Notice that the date is listed using the new standard format for date strings.

Now that the data is persistent lets create a table to edit this data.

### The Table UI

The table binding code from which this post originates produces this output for the `Contact` object:

![Property Table for the Contact Object](/blog/table-property-mapping/property-table-landscape.png)

Figure 1. Property Table for the Contact Object

This was essentially created using these three lines of code:
    
    
    Contact prot = new Contact();
    UiBinding.BoundTableModel tb = ui.createTableModel(listOfContacts, prot);
    Table t = new Table(tb);

The first line uses a prototype object based on which the table structure is determined. Next we create a table model of the type `BoundTableModel` which includes some special capabilities I’ll cover soon. The next line creates the table…​ That’s it!

The full code doesn’t contain all that much more:
    
    
    Form hi = new Form("Property Table", BoxLayout.y());
    UiBinding ui = new UiBinding();
    Contact prot = new Contact();
    UiBinding.BoundTableModel tb = ui.createTableModel(listOfContacts, prot);
    tb.setMultipleChoiceOptions(prot.gender, "Male", "Female", "Unspecified");
    Table t = new Table(tb);
    hi.add(t);
    hi.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_ADD, e ->
            tb.addRow(tb.getRowCount(), new Contact().
                name.set("Unnamed")));
    hi.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_REMOVE, e -> {
            if(t.getSelectedRow() > -1)
                tb.removeRow(t.getSelectedRow());
        });
    hi.getToolbar().addMaterialCommandToRightBar("",
        FontImage.MATERIAL_SAVE, e ->
            PropertyIndex.storeJSONList("contacts.json", listOfContacts));
    hi.show();

You will notice I set multiple choice options for the gender. That means that when someone clicks the gender column they will see this:

![The gender option is a picker](/blog/table-property-mapping/property-table-gender.png)

Figure 2. The gender option is a picker

That’s really cool but it gets better. The date column implicitly uses a date picker:

![The date is implicitly a picker](/blog/table-property-mapping/property-table-date-picker.png)

Figure 3. The date is implicitly a picker

Notice the commands in the source above, they include some familiar code (such as the save method). But the other commands include new methods that aren’t available in the standard `TableModel` class.

The `BoundTableModel` includes a few features that aren’t available in `TableModel` specifically:
    
    
    public void excludeProperty(PropertyBase b); __**(1)**
    public void setColumnOrder(PropertyBase... columnOrder); __**(2)**
    public void setEditable(PropertyBase pb, boolean editable); __**(3)**
    public void addRow(int index, PropertyBusinessObject b); __**(4)**
    public void removeRow(int index);
    public void setMultipleChoiceOptions(PropertyBase prop, String... values);
    public void setValidationConstraint(PropertyBase prop, Constraint c); __**(5)**

__**1** | Allows us to hide a column from the table  
---|---  
__**2** | Allows us to determine the order of the columns, normally they are in the order they are added to the `PropertyIndex`  
__**3** | We can flag a specific property as non-editable  
__**4** | Add/remove a row will also update the origin list or property  
__**5** | We can bind validation logic to a specific property  
  
### Abstract Table Model

A lot of the power of this class is enabled via the new `AbstractTableModel` class. Up until now `TableModel` was the base interface that was implemented by `DefaultTableModel`. It’s a good abstraction but we needed a way to add new API’s without changing the interface.

Had we migrated the core of Codename One to Java 8 we might have used default methods. Instead we added a new abstract class that implements all the new API’s we need from `TableModel`. Table now has special cases internally for `AbstractTableModel`.

Most of the API’s in `AbstractTableModel` map directly to the functionality you see above. E.g. `public Class getCellType(int row, int column)` allows the `Table` to to generate the right cell type by default.

### Final Word

This is relatively simple, I could have gone much further. I could have used an auto generated UI to edit individual rows etc. But I wanted to keep things simple and manageable.

As you can see from the blog, this week I’ve been playing a lot of catch up with updates on where we are. There is a lot more coming as we ramp up to Codename One 5.0 in September.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
