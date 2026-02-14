---
title: Sortable Table
slug: sortable-table
url: /blog/sortable-table/
original_url: https://www.codenameone.com/blog/sortable-table.html
aliases:
- /blog/sortable-table.html
date: '2017-08-14'
author: Shai Almog
---

![Header Image](/blog/sortable-table/new-features-5.jpg)

Sometimes I get a question on stack overflow that triggers a “yes this should be easy” reflex. I got such a question a couple of weeks ago when I implemented the [animated gif support cn1lib](/blog/animated-gif-support.html) and I had [another one last week](https://stackoverflow.com/questions/45330419/codename-one-sort-table-with-data-from-sql-database/45341816) which asked about sorting tables.

This is actually pretty easy to do but it’s just something we didn’t get around to. So I just implemented it and decided this would make a lot of sense in the core API. Sortable tables make a lot of sense and ideally should be the default.

So I added support for this directly into Codename One and you can now make a table sortable like this:
    
    
    Form hi = new Form("Table", new BorderLayout());
    TableModel model = new DefaultTableModel(new String[] {"Col 1", "Col 2", "Col 3"}, new Object[][] {
        {"Row 1", "Row A", 1},
        {"Row 2", "Row B", 4},
        {"Row 3", "Row C", 7.5},
        {"Row 4", "Row D", 2.24},
        });
    Table table = new Table(model);
    table.setSortSupported(true);
    hi.add(BorderLayout.CENTER, table);
    hi.add(NORTH, new Button("Button"));
    hi.show();

Notice this works with numbers, Strings and might work with dates but you can generally support any object type by overriding the method `protected Comparator createColumnSortComparator(int column)` which should return a comparator for your custom object type in the column.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — August 16, 2017 at 4:59 pm ([permalink](https://www.codenameone.com/blog/sortable-table.html#comment-23686))

> Francesco Galgani says:
>
> Thank you very much! 🙂
>



### **Francesco Galgani** — August 16, 2017 at 5:11 pm ([permalink](https://www.codenameone.com/blog/sortable-table.html#comment-21471))

> Francesco Galgani says:
>
> First question: I tested your code in a new Netbeans project, but it doesn’t compile because it «cannot find symbol table.setSortSupported(true);». I’ve seen that you updated the API, but… how do I update Netbeans?
>
> Second question: in your code, is the Table sorted by the third column? How can I choose which column to order by?
>
> Thank you for any help (and for your work in vacation time…)
>



### **Shai Almog** — August 17, 2017 at 4:39 am ([permalink](https://www.codenameone.com/blog/sortable-table.html#comment-23549))

> Shai Almog says:
>
> Make sure to update the libraries by opening Codename One Settings -> Basic -> Update Client Libs.
>



### **Francesco Galgani** — August 17, 2017 at 9:35 am ([permalink](https://www.codenameone.com/blog/sortable-table.html#comment-23645))

> Francesco Galgani says:
>
> Ok, now it compiles… but the table is not sorted (using exactly your code). How can I sort it according to the values in third column? Thank you
>



### **Shai Almog** — August 18, 2017 at 5:53 am ([permalink](https://www.codenameone.com/blog/sortable-table.html#comment-23573))

> Shai Almog says:
>
> Click the columns to sort them and again to flip ascending/descending direction.
>



### **salah Alhaddabi** — August 21, 2017 at 4:44 am ([permalink](https://www.codenameone.com/blog/sortable-table.html#comment-23520))

> salah Alhaddabi says:
>
> Very nice you guys are amazing!!!
>



### **SoAppMedia** — August 21, 2017 at 4:42 pm ([permalink](https://www.codenameone.com/blog/sortable-table.html#comment-21525))

> SoAppMedia says:
>
> Thanks for sharing! this is such a great help this is worth to share!
>



### **Francesco Galgani** — August 21, 2017 at 5:02 pm ([permalink](https://www.codenameone.com/blog/sortable-table.html#comment-21854))

> Francesco Galgani says:
>
> Ok. Is there any way to get a column already selected for ordering (in ascending or descending direction) before any user input, for example on [myForm.show](<http://myForm.show)(>) or myForm.revalidate()?
>



### **Shai Almog** — August 22, 2017 at 5:27 am ([permalink](https://www.codenameone.com/blog/sortable-table.html#comment-23613))

> Shai Almog says:
>
> I’ll add a sort(column, ascending) method to allow this.
>



### **Francesco Galgani** — August 22, 2017 at 11:13 am ([permalink](https://www.codenameone.com/blog/sortable-table.html#comment-23565))

> Francesco Galgani says:
>
> Thank you! 😀
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
