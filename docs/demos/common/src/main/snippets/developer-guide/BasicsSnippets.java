// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::basics-java-001[]
Component.setSameWidth(cmp1, cmp2, cmp3, cmp4);
Component.setSameHeight(cmp5, cmp6, cmp7);
// end::basics-java-001[]

// tag::basics-java-002[]
Container cnt = new Container(BoxLayout.y());
cnt.add(new Label("Just Added"));
// end::basics-java-002[]

// tag::basics-java-003[]
cnt.add(NORTH, new Label("Just Added"));
// end::basics-java-003[]

// tag::basics-java-004[]
import static com.codename1.ui.CN.*;
// end::basics-java-004[]

// tag::basics-java-005[]
callSerially(() -> runThisOnTheEDT());
// end::basics-java-005[]

// tag::basics-java-006[]
Display.getInstance().callSerially(() -> runThisOnTheEDT());
// end::basics-java-006[]

// tag::basics-java-007[]
addToQueue(myConnectionRequest);
// end::basics-java-007[]

// tag::basics-java-008[]
NetworkManager.getInstance().addToQueue(myConnectionRequest);
// end::basics-java-008[]

// tag::basics-java-009[]
log("my log message");
log(myException);
// end::basics-java-009[]

// tag::basics-java-010[]
Container cnt = new Container(BoxLayout.y());
cnt.add(new Label("Just Added")); // <1>
cnt.addAll(new Label("Adding Multiple"), // <2>
    new Label("Second One"));

cnt.add(new Label("Chaining")). // <3>
    add(new Label("Value"));
// end::basics-java-010[]

// tag::basics-java-011[]
Container boxY = BoxLayout.encloseY(cmp1, cmp2); // <1>
Container boxX = BoxLayout.encloseX(cmp3, cmp4);
Container flowCenter = FlowLayout. // <2>
    encloseCenter(cmp5, cmp6);
// end::basics-java-011[]

// tag::basics-java-012[]
Form hi = new Form("Flow Layout", new FlowLayout());
hi.add(new Label("First")).
    add(new Label("Second")).
    add(new Label("Third")).
    add(new Label("Fourth")).
    add(new Label("Fifth"));
hi.show();
// end::basics-java-012[]

// tag::basics-java-013[]
Container flowLayout = FlowLayout.encloseIn(
        new Label("First"),
        new Label("Second"),
        new Label("Third"),
        new Label("Fourth"),
        new Label("Fifth"));
// end::basics-java-013[]

// tag::basics-java-014[]
Form hi = new Form("Box Y Layout", new BoxLayout(BoxLayout.Y_AXIS));
hi.add(new Label("First")).
    add(new Label("Second")).
    add(new Label("Third")).
    add(new Label("Fourth")).
    add(new Label("Fifth"));
// end::basics-java-014[]

// tag::basics-java-015[]
Container box = BoxLayout.encloseX(new Label("First"),
        new Label("Second"),
        new Label("Third"),
        new Label("Fourth"),
        new Label("Fifth"));
// end::basics-java-015[]

// tag::basics-java-016[]
Form hi = new Form("Border Layout", new BorderLayout());
hi.add(BorderLayout.CENTER, new Label("Center")).
    add(BorderLayout.SOUTH, new Label("South")).
    add(BorderLayout.NORTH, new Label("North")).
    add(BorderLayout.EAST, new Label("East")).
    add(BorderLayout.WEST, new Label("West"));
hi.show();
// end::basics-java-016[]

// tag::basics-java-017[]
Form hi = new Form("Border Layout", new BorderLayout());
((BorderLayout)hi.getLayout()).setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER);
hi.add(BorderLayout.CENTER, new Label("Center")).
    add(BorderLayout.SOUTH, new Label("South")).
    add(BorderLayout.NORTH, new Label("North")).
    add(BorderLayout.EAST, new Label("East")).
    add(BorderLayout.WEST, new Label("West"));
hi.show();
// end::basics-java-017[]

// tag::basics-java-018[]
Form hi = new Form("Grid Layout 2×2", new GridLayout(2, 2));
hi.add(new Label("First")).
    add(new Label("Second")).
    add(new Label("Third")).
    add(new Label("Fourth")).
    add(new Label("Fifth"));
// end::basics-java-018[]

// tag::basics-java-019[]
GridLayout.encloseIn(new Label("First"),
    new Label("Second"),
    new Label("Third"),
    new Label("Fourth"),
    new Label("Fifth"));
// end::basics-java-019[]

// tag::basics-java-020[]
Form hi = new Form("Table Layout 2×2", new TableLayout(2, 2));
hi.add(new Label("First")).
    add(new Label("Second")).
    add(new Label("Third")).
    add(new Label("Fourth")).
    add(new Label("Fifth"));
hi.show();
// end::basics-java-020[]

// tag::basics-java-021[]
Container tl = TableLayout.encloseIn(2, new Label("First"),
                new Label("Second"),
                new Label("Third"),
                new Label("Fourth"),
                new Label("Fifth"));
// end::basics-java-021[]

// tag::basics-java-022[]
TableLayout tl = new TableLayout(2, 3); // <1>
Form hi = new Form("Table Layout Cons", tl);
hi.setScrollable(false); // <2>
hi.add(tl.createConstraint(). // <3>
            widthPercentage(20),
                new Label("AAA")).

        add(tl.createConstraint(). // <4>
            horizontalSpan(2).
            heightPercentage(80).
            verticalAlign(Component.CENTER).
            horizontalAlign(Component.CENTER),
                new Label("Span H")).

        add(new Label("BBB")).

        add(tl.createConstraint().
            widthPercentage(60).
            heightPercentage(20),
                new Label("CCC")).

        add(tl.createConstraint().
            widthPercentage(20),
                new Label("DDD"));
// end::basics-java-022[]

// tag::basics-java-023[]
TableLayout.Constraint cn = tl.createConstraint();
cn.setWidthPercentage(20);
hi.add(cn, new Label("AAA"));
// end::basics-java-023[]

// tag::basics-java-024[]
TextModeLayout tl = new TextModeLayout(3, 2);
Form f = new Form("Pixel Perfect", tl);
TextComponent title = new TextComponent().label("Title");
TextComponent price = new TextComponent().label("Price");
TextComponent location = new TextComponent().label("Location");
TextComponent description = new TextComponent().label("Description").multiline(true);

f.add(tl.createConstraint().horizontalSpan(2), title);
f.add(tl.createConstraint().widthPercentage(30), price);
f.add(tl.createConstraint().widthPercentage(70), location);
f.add(tl.createConstraint().horizontalSpan(2), description);
f.setEditOnShow(title.getField());
f.show();
// end::basics-java-024[]

// tag::basics-java-025[]
hi.add(LayeredLayout.encloseIn(settingsLabel,
        FlowLayout.encloseRight(close)));
// end::basics-java-025[]

// tag::basics-java-026[]
Form hi = new Form("Layered Layout");
int w = Math.min(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());
Button settingsLabel = new Button("");
Style settingsStyle = settingsLabel.getAllStyles();
settingsStyle.setFgColor(0xff);
settingsStyle.setBorder(null);
settingsStyle.setBgColor(0xff00);
settingsStyle.setBgTransparency(255);
settingsStyle.setFont(settingsLabel.getUnselectedStyle().getFont().derive(w / 3, Font.STYLE_PLAIN));
FontImage.setMaterialIcon(settingsLabel, FontImage.MATERIAL_SETTINGS);
Button close = new Button("");
close.setUIID("Container");
close.getAllStyles().setFgColor(0xff0000);
FontImage.setMaterialIcon(close, FontImage.MATERIAL_CLOSE);
hi.add(LayeredLayout.encloseIn(settingsLabel,
        FlowLayout.encloseRight(close)));
// end::basics-java-026[]

// tag::basics-java-027[]
Container cnt = new Container(new LayeredLayout());
Button btn = new Button("Submit");
LayeredLayout ll = (LayeredLayout)cnt.getLayout();
cnt.add(btn);
ll.setInsets(btn, "auto 0 0 auto");
// end::basics-java-027[]

// tag::basics-java-028[]
ll.setInsets(btn, "auto 0 0 auto");
// end::basics-java-028[]

// tag::basics-java-029[]
ll.setInsets(btn, "auto auto auto 5mm");
// end::basics-java-029[]

// tag::basics-java-030[]
ll.setInsets(btn, "auto 5mm auto auto");
// end::basics-java-030[]

// tag::basics-java-031[]
Container cnt = new Container(new LayeredLayout());
LayeredLayout ll = (LayeredLayout)cnt.getLayout();
Button btn = new Button("Submit");
TextField tf = new TextField();
cnt.add(tf).add(btn);
ll.setInsets(tf, "auto")
  .setInsets(btn, "auto auto auto 0")
  .setReferenceComponentLeft(btn, tf, 1f);
// end::basics-java-031[]

// tag::basics-java-032[]
  TableLayout.encloseIn(1, btn)
  .setInsets(btn, "auto auto auto 0") //<1>
  .setReferenceComponentLeft(btn, tf, 1f); //<2>
// end::basics-java-032[]

// tag::basics-java-033[]
Button button;
hi.setLayout(new GridBagLayout());
GridBagConstraints c = new GridBagConstraints();
//natural height, maximum width
c.fill = GridBagConstraints.HORIZONTAL;
button = new Button("Button 1");
c.weightx = 0.5;
c.fill = GridBagConstraints.HORIZONTAL;
c.gridx = 0;
c.gridy = 0;
hi.addComponent(c, button);

button = new Button("Button 2");
c.fill = GridBagConstraints.HORIZONTAL;
c.weightx = 0.5;
c.gridx = 1;
c.gridy = 0;
hi.addComponent(c, button);

button = new Button("Button 3");
c.fill = GridBagConstraints.HORIZONTAL;
c.weightx = 0.5;
c.gridx = 2;
c.gridy = 0;
hi.addComponent(c, button);

button = new Button("Long-Named Button 4");
c.fill = GridBagConstraints.HORIZONTAL;
c.ipady = 40;      //make this component tall
c.weightx = 0.0;
c.gridwidth = 3;
c.gridx = 0;
c.gridy = 1;
hi.addComponent(c, button);

button = new Button("5");
c.fill = GridBagConstraints.HORIZONTAL;
c.ipady = 0;       //reset to default
c.weighty = 1.0;   //request any extra vertical space
c.anchor = GridBagConstraints.PAGE_END; //bottom of space
c.insets = new Insets(10,0,0,0);  //top padding
c.gridx = 1;       //aligned with button 2
c.gridwidth = 2;   //2 columns wide
c.gridy = 2;       //third row
hi.addComponent(c, button);
// end::basics-java-033[]

// tag::basics-java-034[]
Form hi = new Form("GroupLayout");

Label label1 = new Label();
Label label2 = new Label();
Label label3 = new Label();
Label label4 = new Label();
Label label5 = new Label();
Label label6 = new Label();
Label label7 = new Label();

label1.setText("label1");

label2.setText("label2");

label3.setText("label3");

label4.setText("label4");

label5.setText("label5");

label6.setText("label6");

label7.setText("label7");

GroupLayout layout = new GroupLayout(hi.getContentPane());
hi.setLayout(layout);
layout.setHorizontalGroup(
    layout.createParallelGroup(GroupLayout.LEADING)
    .add(layout.createSequentialGroup()
        .addContainerGap()
        .add(layout.createParallelGroup(GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(label1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.RELATED)
                .add(layout.createParallelGroup(GroupLayout.LEADING)
                    .add(label4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .add(label3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .add(label2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
            .add(label5, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .add(layout.createSequentialGroup()
                .add(label6, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.RELATED)
                .add(label7, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
        .addContainerGap(296, Short.MAX_VALUE))
);
layout.setVerticalGroup(
    layout.createParallelGroup(GroupLayout.LEADING)
    .add(layout.createSequentialGroup()
        .addContainerGap()
        .add(layout.createParallelGroup(GroupLayout.TRAILING)
            .add(label2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .add(label1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(LayoutStyle.RELATED)
        .add(label3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(LayoutStyle.RELATED)
        .add(label4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(LayoutStyle.RELATED)
        .add(label5, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(LayoutStyle.RELATED)
        .add(layout.createParallelGroup(GroupLayout.LEADING)
            .add(label6, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .add(label7, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addContainerGap(150, Short.MAX_VALUE))
);
// end::basics-java-034[]

// tag::basics-java-035[]
Form hi = new Form("MigLayout", new MigLayout("fillx,insets 0"));

hi.add(new Label("First")).
    add("span 2 2", new Label("Second")).  // The component will span 2×2 cells.
    add("wrap", new Label("Third")).      // Wrap to next row
    add(new Label("Forth")).
    add("wrap", new Label("Fifth")).    // Note that it "jumps over" the occupied cells.
    add(new Label("Sixth")).
    add(new Label("Seventh"));
hi.show();
// end::basics-java-035[]

// tag::basics-java-036[]
nameText.setUIID("Label");
// end::basics-java-036[]

// tag::basics-java-037[]
nameText.getAllStyles().setFgColor(0xff0000);
// end::basics-java-037[]

// tag::basics-java-038[]
theme = UIManager.initFirstTheme("/theme");
// end::basics-java-038[]

// tag::basics-java-039[]
public void onButton_1ActionEvent(com.codename1.ui.events.ActionEvent ev) {
}
// end::basics-java-039[]

// tag::basics-java-040[]
private com.codename1.ui.Button gui_Button_1 = new com.codename1.ui.Button();
// end::basics-java-040[]

// tag::basics-java-041[]
package com.mycompany.myapp;

/**
 * GUI builder created Form
 *
 * @author shai
 */
public class MyForm extends com.codename1.ui.Form {

    public MyForm() {
        this(com.codename1.ui.util.Resources.getGlobalResources());
    }

    public MyForm(com.codename1.ui.util.Resources resourceObjectInstance) {
        initGuiBuilderComponents(resourceObjectInstance);
    }

//-- DON'T EDIT BELOW THIS LINE!!!
    private com.codename1.ui.Label gui_Label_1 = new com.codename1.ui.Label();
    private com.codename1.ui.Button gui_Button_1 = new com.codename1.ui.Button();


// <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void guiBuilderBindComponentListeners() {
        EventCallbackClass callback = new EventCallbackClass();
        gui_Button_1.addActionListener(callback);
    }

    class EventCallbackClass implements com.codename1.ui.events.ActionListener, com.codename1.ui.events.DataChangedListener {
        private com.codename1.ui.Component cmp;
        public EventCallbackClass(com.codename1.ui.Component cmp) {
            this.cmp = cmp;
        }

        public EventCallbackClass() {
        }

        public void actionPerformed(com.codename1.ui.events.ActionEvent ev) {
            com.codename1.ui.Component sourceComponent = ev.getComponent();
            if(sourceComponent.getParent().getLeadParent() != null) {
                sourceComponent = sourceComponent.getParent().getLeadParent();
            }

            if(sourceComponent == gui_Button_1) {
                onButton_1ActionEvent(ev);
            }
        }

        public void dataChanged(int type, int index) {
        }
    }
    private void initGuiBuilderComponents(com.codename1.ui.util.Resources resourceObjectInstance) {
        guiBuilderBindComponentListeners();
        setLayout(new com.codename1.ui.layouts.FlowLayout());
        setTitle("My new title");
        setName("MyForm");
        addComponent(gui_Label_1);
        addComponent(gui_Button_1);
        gui_Label_1.setText("Hi World");
        gui_Label_1.setName("Label_1");
        gui_Button_1.setText("Click Me");
        gui_Button_1.setName("Button_1");
    }// </editor-fold>

//-- DON'T EDIT ABOVE THIS LINE!!!
    public void onButton_1ActionEvent(com.codename1.ui.events.ActionEvent ev) {
    }

}
// end::basics-java-041[]
