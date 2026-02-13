---
title: 'TIP: Select Table Row'
slug: tip-select-table-row
url: /blog/tip-select-table-row/
original_url: https://www.codenameone.com/blog/tip-select-table-row.html
aliases:
- /blog/tip-select-table-row.html
date: '2017-02-26'
author: Shai Almog
---

![Header Image](/blog/tip-select-table-row/tip.jpg)

Before we go into the tip I’d like to start with an apology to all the people I didn’t get back to. Earlier today I sent an email asking for help with the upcoming Codename One bootcamp. The anticipation and resulting email flood took me totally off-guard as I expected a couple of dozen responses in the best case scenario and got much more…​  
I started answering as fast as I could and getting back to people I already answered in the back and forth but quickly this became untenable and I’m seeing my backlog pile growing. Chen and others around here tried to help but I prefer to do this personally as I’d like the feedback to sink into my brain not as an afterthought as it is **very valuable**!

I promise that by tomorrow I’ll answer each and every one of you, I’m not doing it in order of arrival so some might wait while others get instant feedback (that’s mostly an oddity of intercom that makes it hard to work sensibly).

Anyway I read everything and agree with pretty much all of your ideas so a huge thank you, to all of you who took the time to write.

If you didn’t get a chance to write to me **please answer that e-mail now** , help shape our upcoming bootcamp & effectively make it yours. The thing that drives Codename One is the community and this is where it’s most obvious.

And now, something completely different…​

### Table Row Selection

When we designed the Codename One `Table` class we tried to keep it as far as possible from the Swing `JTable`. I loved that class but during my years as a consultant I was struck by the amount of bad `JTable` code that was floating around. Developers just couldn’t grasp so many of the nuances related to the delicate balance of renderer/editor…​

I hope we did a better job with our `Table` class, I know it’s not easy but having gone thru tables in many OS’s (don’t get me started on iOS) I would say our approach is MUCH easier.

One of the tricks we do in `Table` is a “forced refresh” which we trigger thru `tbl.setModel(tbl.getModel())` this effectively saves us from the need to derive the model and fire the proper model changed event. It allows us to store state in a different location than the model which we normally shouldn’t do but it’s a hack that’s useful as a shortcut.

E.g. [a recent stackoverflow question](http://stackoverflow.com/questions/42303713/how-can-i-select-and-hightlight-a-row-in-a-table-in-codename-one/42311881) asked about highlighting a table row. The asker eventually just meant a checkbox mode (I guess he didn’t think about the fact that `ctrl-click` doesn’t work in touch UI’s) but this is the perfect example for this use case.

We keep the selection in a variable and putting it into the model seems redundant. So when we need to refresh the other cells in the row we can just use that trick as a workaround.
    
    
    Form hi = new Form("Table", new BorderLayout());
    TableModel model = new DefaultTableModel(new String[] {"Col 1", "Col 2", "Col 3"}, new Object[][] {
        {"Row 1", "Row A", "Row X"},
        {"Row 2", "Row B can now stretch", null},
        {"Row 3", "Row C", "Row Z"},
        {"Row 4", "Row D", "Row K"},
        }) {
            public boolean isCellEditable(int row, int col) {
                return col != 0;
            }
        };
    Table table = new Table(model) {
        private int selectedRow = -1;
        @Override
        protected Component createCell(Object value, int row, int column, boolean editable) {
            Component cell;
            if(row < 0) {
                cell = super.createCell(value, row, column, editable);
            } else {
                cell = new Button(value.toString());
                cell.setUIID("TableCell");
                ((Button)cell).addActionListener(e -> {
                    selectedRow = row;
                    setModel(getModel());
                });
            }
            if(selectedRow > -1 && selectedRow == row) {
                cell.getAllStyles().setBgColor(0xff0000);
                cell.getAllStyles().setBgTransparency(100);
            }
            return cell;
        }
    
        @Override
        protected TableLayout.Constraint createCellConstraint(Object value, int row, int column) {
            TableLayout.Constraint con =  super.createCellConstraint(value, row, column);
            if(row == 1 && column == 1) {
                con.setHorizontalSpan(2);
            }
            con.setWidthPercentage(33);
            return con;
        }
    };
    hi.add(BorderLayout.CENTER, table);
    hi.show();

![Selected table row from the code above](/blog/tip-select-table-row/select-table-row.png)

Figure 1. Selected table row from the code above

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
