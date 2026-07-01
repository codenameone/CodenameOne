// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::the-components-of-codename-one-java-001[]
Form currentForm = Display.getInstance().getCurrent();
// end::the-components-of-codename-one-java-001[]

// tag::the-components-of-codename-one-java-002[]
myForm.getContentPane().add(...);
// end::the-components-of-codename-one-java-002[]

// tag::the-components-of-codename-one-java-003[]
Form form = new Form(new BorderLayout());

Container bottomBar = new Container(BoxLayout.x());
bottomBar.setSafeArea(true);
bottomBar.addAll(new Button("Home"), new Button("Search"), new Button("Profile"));

form.add(BorderLayout.SOUTH, bottomBar);
form.show();
// end::the-components-of-codename-one-java-003[]

// tag::the-components-of-codename-one-java-004[]
Form form = Display.getInstance().getCurrent();
Rectangle safe = form.getSafeArea();

Graphics g = ...; // e.g. inside paint()
g.setClip(safe.getX(), safe.getY(), safe.getWidth(), safe.getHeight());
// Custom drawing code that should avoid the notch/gesture areas
// end::the-components-of-codename-one-java-004[]

// tag::the-components-of-codename-one-java-005[]
Container drawer = new Container(BoxLayout.y());
drawer.setSafeAreaRoot(true); // Ensure safe margins apply before the drawer is visible
drawer.setSafeArea(true);
// end::the-components-of-codename-one-java-005[]

// tag::the-components-of-codename-one-java-006[]
if(Dialog.show("Click Yes Or No", "Select one", "Yes", "No")) {
    // user clicked yes
} else {
    // user clicked no
}
// end::the-components-of-codename-one-java-006[]

// tag::the-components-of-codename-one-java-007[]
Dialog d = new Dialog("Title");
d.setLayout(new BorderLayout());
d.add(BorderLayout.CENTER, new SpanLabel("Dialog Body", "DialogBody"));
d.showPacked(BorderLayout.SOUTH, true);
// end::the-components-of-codename-one-java-007[]

// tag::the-components-of-codename-one-java-008[]
Dialog d = new Dialog("Title");
d.setLayout(new BorderLayout());
d.add(BorderLayout.CENTER, new SpanLabel("Dialog Body", "DialogBody"));
d.show(hi.getHeight() / 2, 0, 0, 0);
// end::the-components-of-codename-one-java-008[]

// tag::the-components-of-codename-one-java-009[]
Form hi = new Form("Tint Dialog", new BoxLayout(BoxLayout.Y_AXIS));
Button showDialog = new Button("Tint");
showDialog.addActionListener((e) -> Dialog.show("Tint", "Is On....", "OK", null));
hi.add(showDialog);
hi.show();
// end::the-components-of-codename-one-java-009[]

// tag::the-components-of-codename-one-java-010[]
Form hi = new Form("Tint Dialog", new BoxLayout(BoxLayout.Y_AXIS));
hi.setTintColor(0x7700ff00);
Button showDialog = new Button("Tint");
showDialog.addActionListener((e) -> Dialog.show("Tint", "Is On....", "OK", null));
hi.add(showDialog);
hi.show();
// end::the-components-of-codename-one-java-010[]

// tag::the-components-of-codename-one-java-011[]
Form hi = new Form("Blur Dialog", new BoxLayout(BoxLayout.Y_AXIS));
Dialog.setDefaultBlurBackgroundRadius(8);
Button showDialog = new Button("Blur");
showDialog.addActionListener((e) -> Dialog.show("Blur", "Is On....", "OK", null));
hi.add(showDialog);
hi.show();
// end::the-components-of-codename-one-java-011[]

// tag::the-components-of-codename-one-java-012[]
hi.setTintColor(0);
// end::the-components-of-codename-one-java-012[]

// tag::the-components-of-codename-one-java-013[]
Dialog d = new Dialog("Title");
d.setLayout(new BorderLayout());
d.add(BorderLayout.CENTER, new SpanLabel("Dialog Body", "DialogBody"));
d.showPopupDialog(showDialog);
// end::the-components-of-codename-one-java-013[]

// tag::the-components-of-codename-one-java-014[]
PopupDialogArrowBool=true
PopupDialogArrowTopImage=arrow up image
PopupDialogArrowBottomImage=arrow down image
PopupDialogArrowLeftImage=arrow left image
PopupDialogArrowRightImage=arrow right image
// end::the-components-of-codename-one-java-014[]

// tag::the-components-of-codename-one-java-015[]
InteractionDialog dlg = new InteractionDialog("Hello");
dlg.setLayout(new BorderLayout());
dlg.add(BorderLayout.CENTER, new Label("Hello Dialog"));
Button close = new Button("Close");
close.addActionListener((ee) -> dlg.dispose());
dlg.addComponent(BorderLayout.SOUTH, close);
Dimension pre = dlg.getContentPane().getPreferredSize();
dlg.show(0, 0, Display.getInstance().getDisplayWidth() - (pre.getWidth() + pre.getWidth() / 6), 0);
// end::the-components-of-codename-one-java-015[]

// tag::the-components-of-codename-one-java-016[]
Label left = new Label("Left", icon);
left.setTextPosition(Component.LEFT);
Label right = new Label("Right", icon);
right.setTextPosition(Component.RIGHT);
Label bottom = new Label("Bottom", icon);
bottom.setTextPosition(Component.BOTTOM);
Label top = new Label("Top", icon);
top.setTextPosition(Component.TOP);
hi.add(left).add(right).add(bottom).add(top);
// end::the-components-of-codename-one-java-016[]

// tag::the-components-of-codename-one-java-017[]
Form hi = new Form("AutoSize", BoxLayout.y());

Label a = new Label("Short Text");
a.setAutoSizeMode(true);
Label b = new Label("Much Longer Text than the previous line...");
b.setAutoSizeMode(true);
Label c = new Label("MUCH MUCH MUCH Much Longer Text than the previous line by a pretty big margin...");
c.setAutoSizeMode(true);

Label a1 = new Button("Short Text");
a1.setAutoSizeMode(true);
Label b1 = new Button("Much Longer Text than the previous line...");
b1.setAutoSizeMode(true);
Label c1 = new Button("MUCH MUCH MUCH Much Longer Text than the previous line by a pretty big margin...");
c1.setAutoSizeMode(true);
hi.addAll(a, b, c, a1, b1, c1);

hi.show();
// end::the-components-of-codename-one-java-017[]

// tag::the-components-of-codename-one-java-018[]
TableLayout tl;
int spanButton = 2;
if(Display.getInstance().isTablet()) {
    tl = new TableLayout(7, 2);
} else {
    tl = new TableLayout(14, 1);
    spanButton = 1;
}
tl.setGrowHorizontally(true);
hi.setLayout(tl);

TextField firstName = new TextField("", "First Name", 20, TextArea.ANY);
TextField surname = new TextField("", "Surname", 20, TextArea.ANY);
TextField email = new TextField("", "E-Mail", 20, TextArea.EMAILADDR);
TextField url = new TextField("", "URL", 20, TextArea.URL);
TextField phone = new TextField("", "Phone", 20, TextArea.PHONENUMBER);

TextField num1 = new TextField("", "1234", 4, TextArea.NUMERIC);
TextField num2 = new TextField("", "1234", 4, TextArea.NUMERIC);
TextField num3 = new TextField("", "1234", 4, TextArea.NUMERIC);
TextField num4 = new TextField("", "1234", 4, TextArea.NUMERIC);

Button submit = new Button("Submit");
TableLayout.Constraint cn = tl.createConstraint();
cn.setHorizontalSpan(spanButton);
cn.setHorizontalAlign(Component.RIGHT);
hi.add("First Name").add(firstName).
        add("Surname").add(surname).
        add("E-Mail").add(email).
        add("URL").add(url).
        add("Phone").add(phone).
        add("Credit Card").
                add(GridLayout.encloseIn(4, num1, num2, num3, num4)).
        add(cn, submit);
// end::the-components-of-codename-one-java-018[]

// tag::the-components-of-codename-one-java-019[]
automoveToNext(num1, num2);
automoveToNext(num2, num3);
automoveToNext(num3, num4);
// end::the-components-of-codename-one-java-019[]

// tag::the-components-of-codename-one-java-020[]
private void automoveToNext(final TextField current, final TextField next) {
    current.addDataChangedListener((type, index) -> {
        if(current.getText().length() == 5) {
            current.stopEditing();
            current.setText(val.substring(0, 4));
            next.setText(val.substring(4));
            next.startEditingAsync();
        }
    });
}
// end::the-components-of-codename-one-java-020[]

// tag::the-components-of-codename-one-java-021[]
searchTextField.putClientProperty("searchField", Boolean.TRUE);
sendTextField.putClientProperty("sendButton", Boolean.TRUE);
goTextField.putClientProperty("goButton", Boolean.TRUE);
// end::the-components-of-codename-one-java-021[]

// tag::the-components-of-codename-one-java-022[]
tf.putClientProperty("iosHideToolbar", Boolean.TRUE);
// end::the-components-of-codename-one-java-022[]

// tag::the-components-of-codename-one-java-023[]
cnt.add(myTextField);
// end::the-components-of-codename-one-java-023[]

// tag::the-components-of-codename-one-java-024[]
cnt.add(ClearableTextField.wrap(myTextField));
// end::the-components-of-codename-one-java-024[]

// tag::the-components-of-codename-one-java-025[]
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
// end::the-components-of-codename-one-java-025[]

// tag::the-components-of-codename-one-java-026[]
TextComponent t = new TextComponent().
    text("This appears in the text field").
    hint("This is the hint").
    label("This is the label").
    multiline(true);
// end::the-components-of-codename-one-java-026[]

// tag::the-components-of-codename-one-java-027[]
Validator val = new Validator();
val.addConstraint(title, new LengthConstraint(2));
val.addConstraint(price, new NumericConstraint(true));
// end::the-components-of-codename-one-java-027[]

// tag::the-components-of-codename-one-java-028[]
TextComponent tc = new TextComponent().
    label("Input Required").
    errorMessage("Input is essential in this field");
// end::the-components-of-codename-one-java-028[]

// tag::the-components-of-codename-one-java-029[]
TextModeLayout tl = new TextModeLayout(3, 2);
Form f = new Form("Pixel Perfect", tl);
TextComponent title = new TextComponent().label("Title");
TextComponent price = new TextComponent().label("Price");
TextComponent location = new TextComponent().label("Location");
PickerComponent date = PickerComponent.createDate(new Date()).label("Date");
TextComponent description = new TextComponent().label("Description").multiline(true);
Validator val = new Validator();
val.addConstraint(title, new LengthConstraint(2));
val.addConstraint(price, new NumericConstraint(true));
f.add(tl.createConstraint().widthPercentage(60), title);
f.add(tl.createConstraint().widthPercentage(40), date);
f.add(location);
f.add(price);
f.add(tl.createConstraint().horizontalSpan(2), description);
f.setEditOnShow(title.getField());
f.show();
// end::the-components-of-codename-one-java-029[]

// tag::the-components-of-codename-one-java-030[]
Form hi = new Form("Button");
Button b = new Button("My Button");
hi.add(b);
b.addActionListener((e) -> Log.p("Clicked"));
// end::the-components-of-codename-one-java-030[]

// tag::the-components-of-codename-one-java-031[]
Form hi = new Form("Button");
Button b = new Button("Link Button");
b.getAllStyles().setBorder(Border.createEmpty());
b.getAllStyles().setTextDecoration(Style.TEXT_DECORATION_UNDERLINE);
hi.add(b);
b.addActionListener((e) -> Log.p("Clicked"));
// end::the-components-of-codename-one-java-031[]

// tag::the-components-of-codename-one-java-032[]
if(UIManager.getInstance().isThemeConstant("hasRaisedButtonBool", false)) {
    // that means we can use a raised button
}
// end::the-components-of-codename-one-java-032[]

// tag::the-components-of-codename-one-java-033[]
Form f = new Form("Pixel Perfect", BoxLayout.y());
Button b = new Button("Raised Button", "RaisedButton");
Button r = new Button("Flat Button");
f.add(b);
f.add(r);
f.show();
// end::the-components-of-codename-one-java-033[]

// tag::the-components-of-codename-one-java-034[]
CheckBox cb1 = new CheckBox("CheckBox No Icon");
cb1.setSelected(true);
CheckBox cb2 = new CheckBox("CheckBox With Icon", icon);
CheckBox cb3 = new CheckBox("CheckBox Opposite True", icon);
CheckBox cb4 = new CheckBox("CheckBox Opposite False", icon);
cb3.setOppositeSide(true);
cb4.setOppositeSide(false);
RadioButton rb1 = new RadioButton("Radio 1");
RadioButton rb2 = new RadioButton("Radio 2");
RadioButton rb3 = new RadioButton("Radio 3", icon);
new ButtonGroup(rb1, rb2, rb3);
rb2.setSelected(true);
hi.add(cb1).add(cb2).add(cb3).add(cb4).add(rb1).add(rb2).add(rb3);
// end::the-components-of-codename-one-java-034[]

// tag::the-components-of-codename-one-java-035[]
CheckBox cb1 = CheckBox.createToggle("CheckBox No Icon");
cb1.setSelected(true);
CheckBox cb2 = CheckBox.createToggle("CheckBox With Icon", icon);
CheckBox cb3 = CheckBox.createToggle("CheckBox Opposite True", icon);
CheckBox cb4 = CheckBox.createToggle("CheckBox Opposite False", icon);
cb3.setOppositeSide(true);
cb4.setOppositeSide(false);
ButtonGroup bg = new ButtonGroup();
RadioButton rb1 = RadioButton.createToggle("Radio 1", bg);
RadioButton rb2 = RadioButton.createToggle("Radio 2", bg);
RadioButton rb3 = RadioButton.createToggle("Radio 3", icon, bg);
rb2.setSelected(true);
hi.add(cb1).add(cb2).add(cb3).add(cb4).add(rb1).add(rb2).add(rb3);
// end::the-components-of-codename-one-java-035[]

// tag::the-components-of-codename-one-java-036[]
hi.add(ComponentGroup.enclose(cb1, cb2, cb3, cb4)).
        add(ComponentGroup.encloseHorizontal(rb1, rb2, rb3));
// end::the-components-of-codename-one-java-036[]

// tag::the-components-of-codename-one-java-037[]
hi.add("Three Labels").
        add(ComponentGroup.enclose(new Label("GroupElementFirst UIID"), new Label("GroupElement UIID"), new Label("GroupElementLast UIID"))).
        add("One Label").
        add(ComponentGroup.enclose(new Label("GroupElementOnly UIID"))).
        add("Three Buttons").
        add(ComponentGroup.enclose(new Button("ButtonGroupFirst UIID"), new Button("ButtonGroup UIID"), new Button("ButtonGroupLast UIID"))).
        add("One Button").
        add(ComponentGroup.enclose(new Button("ButtonGroupOnly UIID")));
// end::the-components-of-codename-one-java-037[]

// tag::the-components-of-codename-one-java-038[]
MultiButton twoLinesNoIcon = new MultiButton("MultiButton");
twoLinesNoIcon.setTextLine2("Line 2");
MultiButton oneLineIconEmblem = new MultiButton("Icon + Emblem");
oneLineIconEmblem.setIcon(icon);
oneLineIconEmblem.setEmblem(emblem);
MultiButton twoLinesIconEmblem = new MultiButton("Icon + Emblem");
twoLinesIconEmblem.setIcon(icon);
twoLinesIconEmblem.setEmblem(emblem);
twoLinesIconEmblem.setTextLine2("Line 2");

MultiButton twoLinesIconEmblemHorizontal = new MultiButton("Icon + Emblem");
twoLinesIconEmblemHorizontal.setIcon(icon);
twoLinesIconEmblemHorizontal.setEmblem(emblem);
twoLinesIconEmblemHorizontal.setTextLine2("Line 2 Horizontal");
twoLinesIconEmblemHorizontal.setHorizontalLayout(true);

MultiButton twoLinesIconCheckBox = new MultiButton("CheckBox");
twoLinesIconCheckBox.setIcon(icon);
twoLinesIconCheckBox.setCheckBox(true);
twoLinesIconCheckBox.setTextLine2("Line 2");

MultiButton fourLinesIcon = new MultiButton("With Icon");
fourLinesIcon.setIcon(icon);
fourLinesIcon.setTextLine2("Line 2");
fourLinesIcon.setTextLine3("Line 3");
fourLinesIcon.setTextLine4("Line 4");

hi.add(oneLineIconEmblem).
        add(twoLinesNoIcon).
        add(twoLinesIconEmblem).
        add(twoLinesIconEmblemHorizontal).
        add(twoLinesIconCheckBox).
        add(fourLinesIcon);
// end::the-components-of-codename-one-java-038[]

// tag::the-components-of-codename-one-java-039[]
SpanButton sb = new SpanButton("SpanButton is a composite component (lead component) that looks/acts like a Button but can break lines rather than crop them when the text is very long.");
sb.setIcon(icon);
hi.add(sb);
// end::the-components-of-codename-one-java-039[]

// tag::the-components-of-codename-one-java-040[]
SpanLabel d = new SpanLabel("Default SpanLabel that can seamlessly line break when the text is really long.");
d.setIcon(icon);
SpanLabel l = new SpanLabel("NORTH Positioned Icon SpanLabel that can seamlessly line break when the text is really long.");
l.setIcon(icon);
l.setIconPosition(BorderLayout.NORTH);
SpanLabel r = new SpanLabel("SOUTH Positioned Icon SpanLabel that can seamlessly line break when the text is really long.");
r.setIcon(icon);
r.setIconPosition(BorderLayout.SOUTH);
SpanLabel c = new SpanLabel("EAST Positioned Icon SpanLabel that can seamlessly line break when the text is really long.");
c.setIcon(icon);
c.setIconPosition(BorderLayout.EAST);
hi.add(d).add(l).add(r).add(c);
// end::the-components-of-codename-one-java-040[]

// tag::the-components-of-codename-one-java-041[]
OnOffSwitch onOff = new OnOffSwitch();
hi.add(onOff);
// end::the-components-of-codename-one-java-041[]

// tag::the-components-of-codename-one-java-042[]
Validator v = new Validator();
v.addConstraint(firstName, new LengthConstraint(2)).
        addConstraint(surname, new LengthConstraint(2)).
        addConstraint(url, RegexConstraint.validURL()).
        addConstraint(email, RegexConstraint.validEmail()).
        addConstraint(phone, new RegexConstraint(phoneRegex, "Must be valid phone number")).
        addConstraint(num1, new LengthConstraint(4)).
        addConstraint(num2, new LengthConstraint(4)).
        addConstraint(num3, new LengthConstraint(4)).
        addConstraint(num4, new LengthConstraint(4));

v.addSubmitButtons(submit);
// end::the-components-of-codename-one-java-042[]

// tag::the-components-of-codename-one-java-043[]
myContainer.add(new InfiniteProgress());
// end::the-components-of-codename-one-java-043[]

// tag::the-components-of-codename-one-java-044[]
Dialog ip = new InfiniteProgress().showInifiniteBlocking();

// do some long operation here using invokeAndBlock or do something in a separate thread and callback later
// when you are done just call

ip.dispose();
// end::the-components-of-codename-one-java-044[]

// tag::the-components-of-codename-one-java-045[]
int pageNumber = 1;
java.util.List<Map<String, Object>> fetchPropertyData(String text) {
    try {
        ConnectionRequest r = new ConnectionRequest();
        r.setPost(false);
        r.setUrl("http://api.nestoria.co.uk/api");
        r.addArgument("pretty", "0");
        r.addArgument("action", "search_listings");
        r.addArgument("encoding", "json");
        r.addArgument("listing_type", "buy");
        r.addArgument("page", "" + pageNumber);
        pageNumber++;
        r.addArgument("country", "uk");
        r.addArgument("place_name", text);
        NetworkManager.getInstance().addToQueueAndWait(r);
        Map<String,Object> result = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
        Map<String, Object> response = (Map<String, Object>)result.get("response");
        return (java.util.List<Map<String, Object>>)response.get("listings");
    } catch(Exception err) {
        Log.e(err);
        return null;
    }
}
// end::the-components-of-codename-one-java-045[]

// tag::the-components-of-codename-one-java-046[]
Form hi = new Form("InfiniteScrollAdapter", new BoxLayout(BoxLayout.Y_AXIS));

Style s = UIManager.getInstance().getComponentStyle("MultiLine1");
FontImage p = FontImage.createMaterial(FontImage.MATERIAL_PORTRAIT, s);
EncodedImage placeholder = EncodedImage.createFromImage(p.scaled(p.getWidth() * 3, p.getHeight() * 3), false); // <1>

InfiniteScrollAdapter.createInfiniteScroll(hi.getContentPane(), () -> { // <2>
    java.util.List<Map<String, Object>> data = fetchPropertyData("Leeds"); // <3>
    MultiButton[] cmps = new MultiButton[data.size()];
    for(int iter = 0 ; iter < cmps.length ; iter++) {
        Map<String, Object> currentListing = data.get(iter);
        if(currentListing == null) { // <4>
            InfiniteScrollAdapter.addMoreComponents(hi.getContentPane(), new Component[0], false);
            return;
        }
        String thumb_url = (String)currentListing.get("thumb_url");
        String guid = (String)currentListing.get("guid");
        String summary = (String)currentListing.get("summary");
        cmps[iter] = new MultiButton(summary);
        cmps[iter].setIcon(URLImage.createToStorage(placeholder, guid, thumb_url));
    }
    InfiniteScrollAdapter.addMoreComponents(hi.getContentPane(), cmps, true); // <5>
}, true); // <6>
// end::the-components-of-codename-one-java-046[]

// tag::the-components-of-codename-one-java-047[]
Form hi = new Form("InfiniteContainer", new BorderLayout());

Style s = UIManager.getInstance().getComponentStyle("MultiLine1");
FontImage p = FontImage.createMaterial(FontImage.MATERIAL_PORTRAIT, s);
EncodedImage placeholder = EncodedImage.createFromImage(p.scaled(p.getWidth() * 3, p.getHeight() * 3), false);

InfiniteContainer ic = new InfiniteContainer() {
    @Override
    public Component[] fetchComponents(int index, int amount) {
        java.util.List<Map<String, Object>> data = fetchPropertyData("Leeds");
        MultiButton[] cmps = new MultiButton[data.size()];
        for(int iter = 0 ; iter < cmps.length ; iter++) {
            Map<String, Object> currentListing = data.get(iter);
            if(currentListing == null) {
                return null;
            }
            String thumb_url = (String)currentListing.get("thumb_url");
            String guid = (String)currentListing.get("guid");
            String summary = (String)currentListing.get("summary");
            cmps[iter] = new MultiButton(summary);
            cmps[iter].setIcon(URLImage.createToStorage(placeholder, guid, thumb_url));
        }
        return cmps;
    }
};
hi.add(BorderLayout.CENTER, ic);
// end::the-components-of-codename-one-java-047[]

// tag::the-components-of-codename-one-java-048[]
form.setScrollable(false);
form.setLayout(new BorderLayout());
form.add(BorderLayout.CENTER, myList);
// end::the-components-of-codename-one-java-048[]

// tag::the-components-of-codename-one-java-049[]
Form hi = new Form("MultiList", new BorderLayout());

ArrayList<Map<String, Object>> data = new ArrayList<>();

data.add(createListEntry("A Game of Thrones", "1996"));
data.add(createListEntry("A Clash Of Kings", "1998"));
data.add(createListEntry("A Storm Of Swords", "2000"));
data.add(createListEntry("A Feast For Crows", "2005"));
data.add(createListEntry("A Dance With Dragons", "2011"));
data.add(createListEntry("The Winds of Winter", "2016 (please, please, please)"));
data.add(createListEntry("A Dream of Spring", "Ugh"));

DefaultListModel<Map<String, Object>> model = new DefaultListModel<>(data);
MultiList ml = new MultiList(model);
hi.add(BorderLayout.CENTER, ml);
// end::the-components-of-codename-one-java-049[]

// tag::the-components-of-codename-one-java-050[]
private Map<String, Object> createListEntry(String name, String date) {
    Map<String, Object> entry = new HashMap<>();
    entry.put("Line1", name);
    entry.put("Line2", date);
    return entry;
}
// end::the-components-of-codename-one-java-050[]

// tag::the-components-of-codename-one-java-051[]
private Map<String, Object> createListEntry(String name, String date, Image cover) {
    Map<String, Object> entry = new HashMap<>();
    entry.put("Line1", name);
    entry.put("Line2", date);
    entry.put("icon", cover);
    return entry;
}
// end::the-components-of-codename-one-java-051[]

// tag::the-components-of-codename-one-java-052[]
class GRMMModel implements ListModel<Map<String,Object>> {
    @Override
    public Map<String, Object> getItemAt(int index) {
        int idx = index % 7;
        switch(idx) {
            case 0:
                return createListEntry("A Game of Thrones " + index, "1996");
            case 1:
                return createListEntry("A Clash Of Kings " + index, "1998");
            case 2:
                return createListEntry("A Storm Of Swords " + index, "2000");
            case 3:
                return createListEntry("A Feast For Crows " + index, "2005");
            case 4:
                return createListEntry("A Dance With Dragons " + index, "2011");
            case 5:
                return createListEntry("The Winds of Winter " + index, "2016 (please, please, please)");
            default:
                return createListEntry("A Dream of Spring " + index, "Ugh");
        }
    }

    @Override
    public int getSize() {
        return 1000000;
    }

    @Override
    public int getSelectedIndex() {
        return 0;
    }

    @Override
    public void setSelectedIndex(int index) {
    }

    @Override
    public void addDataChangedListener(DataChangedListener l) {
    }

    @Override
    public void removeDataChangedListener(DataChangedListener l) {
    }

    @Override
    public void addSelectionListener(SelectionListener l) {
    }

    @Override
    public void removeSelectionListener(SelectionListener l) {
    }

    @Override
    public void addItem(Map<String, Object> item) {
    }

    @Override
    public void removeItem(int index) {
    }
}
// end::the-components-of-codename-one-java-052[]

// tag::the-components-of-codename-one-java-053[]
MultiList ml = new MultiList(new GRMMModel());
// end::the-components-of-codename-one-java-053[]

// tag::the-components-of-codename-one-java-054[]
public interface ListCellRenderer {
   //This method is called by the List for each item, when the List paints itself.
   public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected);

   //This method returns the List animated focus which is animated when list selection changes
   public Component getListFocusComponent(List list);
}
// end::the-components-of-codename-one-java-054[]

// tag::the-components-of-codename-one-java-055[]
public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected){
     return new Label(value.toString());
}

public Component getListFocusComponent(List list){
     return null;
}
// end::the-components-of-codename-one-java-055[]

// tag::the-components-of-codename-one-java-056[]
public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected){
      Label l = new Label(value.toString());
if (isSelected) {
             l.setFocus(true);
             l.getAllStyles().setBgTransparency(100);
         } else {
             l.setFocus(false);
             l.getAllStyles().setBgTransparency(0);
        }
        return l;
}   public Component getListFocusComponent(List list){
      return null;
}
// end::the-components-of-codename-one-java-056[]

// tag::the-components-of-codename-one-java-057[]
class MyRenderer extends Label implements ListCellRenderer {
    public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected){
        setText(value.toString());
        if (isSelected) {
            setFocus(true);
            getAllStyles().setBgTransparency(100);
        } else {
            setFocus(false);
            getAllStyles().setBgTransparency(0);
        }
        return this;
        }
    }
}
// end::the-components-of-codename-one-java-057[]

// tag::the-components-of-codename-one-java-058[]
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
// end::the-components-of-codename-one-java-058[]

// tag::the-components-of-codename-one-java-059[]
focus.getAllStyles().setBgTransparency(100);
try {
    focus.setIcon(Image.createImage("/duke.png"));
    focus.setAlignment(Component.RIGHT);
} catch (IOException ex) {
    ex.printStackTrace();
}
// end::the-components-of-codename-one-java-059[]

// tag::the-components-of-codename-one-java-060[]
com.codename1.ui.List list = new com.codename1.ui.List(createGenericListCellRendererModelData());
list.setRenderer(new GenericListCellRenderer(createGenericRendererContainer(), createGenericRendererContainer()));


private Container createGenericRendererContainer() {
    Label name = new Label();
    name.setFocusable(true);
    name.setName("Name");
    Label surname = new Label();
    surname.setFocusable(true);
    surname.setName("Surname");
    CheckBox selected = new CheckBox();
    selected.setName("Selected");
    selected.setFocusable(true);
    Container c = BorderLayout.center(name).
            add(BorderLayout.SOUTH, surname).
            add(BorderLayout.WEST, selected);
    c.setUIID("ListRenderer");
    return c;
}

private Object[] createGenericListCellRendererModelData() {
    Map<String,Object>[] data = new HashMap[5];
    data[0] = new HashMap<>();
    data[0].put("Name", "Shai");
    data[0].put("Surname", "Almog");
    data[0].put("Selected", Boolean.TRUE);
    data[1] = new HashMap<>();
    data[1].put("Name", "Chen");
    data[1].put("Surname", "Fishbein");
    data[1].put("Selected", Boolean.TRUE);
    data[2] = new HashMap<>();
    data[2].put("Name", "Ofir");
    data[2].put("Surname", "Leitner");
    data[3] = new HashMap<>();
    data[3].put("Name", "Yaniv");
    data[3].put("Surname", "Vakarat");
    data[4] = new HashMap<>();
    data[4].put("Name", "Meirav");
    data[4].put("Surname", "Nachmanovitch");
    return data;
}
// end::the-components-of-codename-one-java-060[]

// tag::the-components-of-codename-one-java-061[]
map.put("componentName", "Component Value");
// end::the-components-of-codename-one-java-061[]

// tag::the-components-of-codename-one-java-062[]
map.put("componentName_uiid", "red");
// end::the-components-of-codename-one-java-062[]

// tag::the-components-of-codename-one-java-063[]
map.put("componentName_uiid", "blue");
// end::the-components-of-codename-one-java-063[]

// tag::the-components-of-codename-one-java-064[]
Map<String, Object> proto = new HashMap<>();
map.put("Line1", "WWWWWWWWWWWWWWWWWWWW");
map.put("Line2", "WWWWWWWWWWWWWWWWWWWW");
int mm5 = Display.getInstance().convertToPixels(5, true);
map.put("icon", Image.create(mm5, mm5));
myMultiList.setRenderingPrototype(map);
// end::the-components-of-codename-one-java-064[]

// tag::the-components-of-codename-one-java-065[]
Form hi = new Form("ComboBox", new BoxLayout(BoxLayout.Y_AXIS));
ComboBox<Map<String, Object>> combo = new ComboBox<> (
        createListEntry("A Game of Thrones", "1996"),
        createListEntry("A Clash Of Kings", "1998"),
        createListEntry("A Storm Of Swords", "2000"),
        createListEntry("A Feast For Crows", "2005"),
        createListEntry("A Dance With Dragons", "2011"),
        createListEntry("The Winds of Winter", "2016 (please, please, please)"),
        createListEntry("A Dream of Spring", "Ugh"));

combo.setRenderer(new GenericListCellRenderer<>(new MultiButton(), new MultiButton()));
// end::the-components-of-codename-one-java-065[]

// tag::the-components-of-codename-one-java-066[]
Form hi = new Form("Star Slider", new BoxLayout(BoxLayout.Y_AXIS));
hi.add(FlowLayout.encloseCenter(createStarRankSlider()));
hi.show();
// end::the-components-of-codename-one-java-066[]

// tag::the-components-of-codename-one-java-067[]
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
private void showStarPickingForm() {
  Form hi = new Form("Star Slider", new BoxLayout(BoxLayout.Y_AXIS));
  hi.add(FlowLayout.encloseCenter(createStarRankSlider()));
  hi.show();
}
// end::the-components-of-codename-one-java-067[]

// tag::the-components-of-codename-one-java-068[]
Form hi = new Form("Table", new BorderLayout());
TableModel model = new DefaultTableModel(new String[] {"Col 1", "Col 2", "Col 3"}, new Object[][] {
    {"Row 1", "Row A", "Row X"},
    {"Row 2", "Row B", "Row Y"},
    {"Row 3", "Row C", "Row Z"},
    {"Row 4", "Row D", "Row K"},
    }) {
        public boolean isCellEditable(int row, int col) {
            return col != 0;
        }
    };
Table table = new Table(model);
hi.add(BorderLayout.CENTER, table);
hi.show();
// end::the-components-of-codename-one-java-068[]

// tag::the-components-of-codename-one-java-069[]
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
// end::the-components-of-codename-one-java-069[]

// tag::the-components-of-codename-one-java-070[]
Table table = new Table(model) {
    @Override
    protected Component createCell(Object value, int row, int column, boolean editable) { // <1>
        Component cell;
        if(row == 1 && column == 1) {  // <2>
            Picker p = new Picker();
            p.setType(Display.PICKER_TYPE_STRINGS);
            p.setStrings("Row B can now stretch", "This is a good value", "So Is This", "Better than text field");
            p.setSelectedString((String)value);  // <3>
            p.setUIID("TableCell");
            p.addActionListener((e) -> getModel().setValueAt(row, column, p.getSelectedString()));  // <4>
            cell = p;
        } else {
            cell = super.createCell(value, row, column, editable);
        }
        if(row > -1 && row % 2 == 0) {  // <5>
            // pinstripe effect
            cell.getAllStyles().setBgColor(0xeeeeee);
            cell.getAllStyles().setBgTransparency(255);
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
// end::the-components-of-codename-one-java-070[]

// tag::the-components-of-codename-one-java-071[]
Form hi = new Form("Table", new BorderLayout());
TableModel model = new DefaultTableModel(new String[] {"Col 1", "Col 2", "Col 3"}, new Object[][] {
    {"Row 1", "Row A", "Row X"},
    {"Row 2", "Row B can now stretch very long line that should span multiple rows as much as possible", "Row Y"},
    {"Row 3", "Row C", "Row Z"},
    {"Row 4", "Row D", "Row K"},
    }) {
        public boolean isCellEditable(int row, int col) {
            return col != 0;
        }
    };
Table table = new Table(model) {
    @Override
    protected Component createCell(Object value, int row, int column, boolean editable) {
        TextArea ta = new TextArea((String)value);
        ta.setUIID("TableCell");
        return ta;
    }

    @Override
    protected TableLayout.Constraint createCellConstraint(Object value, int row, int column) {
        TableLayout.Constraint con =  super.createCellConstraint(value, row, column);
        con.setWidthPercentage(33);
        return con;
    }
};
hi.add(BorderLayout.CENTER, table);
hi.show();
// end::the-components-of-codename-one-java-071[]

// tag::the-components-of-codename-one-java-072[]
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
// end::the-components-of-codename-one-java-072[]

// tag::the-components-of-codename-one-java-073[]
class StringArrayTreeModel implements TreeModel {
    String[][] arr = new String[][] {
            {"Colors", "Letters", "Numbers"},
            {"Red", "Green", "Blue"},
            {"A", "B", "C"},
            {"1", "2", "3"}
        };

    public Vector getChildren(Object parent) {
        if(parent == null) {
            Vector v = new Vector();
            for(int iter = 0 ; iter < arr[0].length ; iter++) {
                v.addElement(arr[0][iter]);
            }
            return v;
        }
        Vector v = new Vector();
        for(int iter = 0 ; iter < arr[0].length ; iter++) {
            if(parent == arr[0][iter]) {
                if(arr.length > iter + 1 && arr[iter + 1] != null) {
                    for(int i = 0 ; i < arr[iter + 1].length ; i++) {
                        v.addElement(arr[iter + 1][i]);
                    }
                }
            }
        }
        return v;
    }

    public boolean isLeaf(Object node) {
        Vector v = getChildren(node);
        return v == null || v.size() == 0;
    }
}

Tree dt = new Tree(new StringArrayTreeModel());
// end::the-components-of-codename-one-java-073[]

// tag::the-components-of-codename-one-java-074[]
Form hi = new Form("XML Tree", new BorderLayout());
InputStream is = Display.getInstance().getResourceAsStream(getClass(), "/build.xml");
try(Reader r = new InputStreamReader(is, "UTF-8");) {
    Element e = new XMLParser().parse(r);
    Tree xmlTree = new Tree(new XMLTreeModel(e)) {
        @Override
        protected String childToDisplayLabel(Object child) {
            if(child instanceof Element) {
                return ((Element)child).getTagName();
            }
            return child.toString();
        }
    };
    hi.add(BorderLayout.CENTER, xmlTree);
} catch(IOException err) {
    Log.e(err);
}
// end::the-components-of-codename-one-java-074[]

// tag::the-components-of-codename-one-java-075[]
class XMLTreeModel implements TreeModel {
    private Element root;
    public XMLTreeModel(Element e) {
        root = e;
    }

    public Vector getChildren(Object parent) {
        if(parent == null) {
            Vector c = new Vector();
            c.addElement(root);
            return c;
        }
        Vector result = new Vector();
        Element e = (Element)parent;
        for(int iter = 0 ; iter < e.getNumChildren() ; iter++) {
            result.addElement(e.getChildAt(iter));
        }
        return result;
    }

    public boolean isLeaf(Object node) {
        Element e = (Element)node;
        return e.getNumChildren() == 0;
    }
}
// end::the-components-of-codename-one-java-075[]

// tag::the-components-of-codename-one-java-076[]
Form hi = new Form("ShareButton");
ShareButton sb = new ShareButton();
sb.setText("Share Screenshot");
hi.add(sb);

Image screenshot = Image.createImage(hi.getWidth(), hi.getHeight());
hi.revalidate();
hi.setVisible(true);
hi.paintComponent(screenshot.getGraphics(), true);

String imageFile = FileSystemStorage.getInstance().getAppHomePath() + "screenshot.png";
try(OutputStream os = FileSystemStorage.getInstance().openOutputStream(imageFile);) {
    ImageIO.getImageIO().save(screenshot, os, ImageIO.FORMAT_PNG, 1);
} catch(IOException err) {
    Log.e(err);
}
sb.setImageToShare(imageFile, "image/png");
// end::the-components-of-codename-one-java-076[]

// tag::the-components-of-codename-one-java-077[]
ShareButton sb = new ShareButton();
sb.setTextToShare("Check this out!");
sb.setShareResultListener(result -> {
    if (result.isSharedTo()) {
        Log.p("Shared to " + result.getPackageName());
    } else if (result.isDismissed()) {
        Log.p("User dismissed the share sheet");
    } else if (result.isFailed()) {
        Log.p("Share failed: " + result.getError());
    }
});
form.add(sb);
// end::the-components-of-codename-one-java-077[]

// tag::the-components-of-codename-one-java-078[]
Display.getInstance().share(
        "Check this out!", imagePath, "image/png", sourceRect,
        result -> handleResult(result));
// end::the-components-of-codename-one-java-078[]

// tag::the-components-of-codename-one-java-079[]
import com.codename1.util.IOSShareExtensionBuilder;

new IOSShareExtensionBuilder()
        .setExtensionName("MyShareExtension")
        .setDisplayName("Share to MyApp")
        .setHostBundleId("com.example.myapp")
        .setAppGroupId("group.com.example.myapp.shared")
        .acceptText(true)
        .acceptURLs(true)
        .acceptImages(true)
        .writeAppext(new File("src/main/resources/MyShareExtension.ios.appext"));
// end::the-components-of-codename-one-java-079[]

// tag::the-components-of-codename-one-java-080[]
Form hi = new Form("Tabs", new BorderLayout());

Tabs t = new Tabs();
Style s = UIManager.getInstance().getComponentStyle("Tab");
FontImage icon1 = FontImage.createMaterial(FontImage.MATERIAL_QUESTION_ANSWER, s);

Container container1 = BoxLayout.encloseY(new Label("Label1"), new Label("Label2"));
t.addTab("Tab1", icon1, container1);
t.addTab("Tab2", new SpanLabel("Some text directly in the tab"));

hi.add(BorderLayout.CENTER, t);
// end::the-components-of-codename-one-java-080[]

// tag::the-components-of-codename-one-java-081[]
Form hi = new Form("Swipe Tabs", new LayeredLayout());
Tabs t = new Tabs();
t.hideTabs();

Style s = UIManager.getInstance().getComponentStyle("Button");
FontImage radioEmptyImage = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_UNCHECKED, s);
FontImage radioFullImage = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_CHECKED, s);
((DefaultLookAndFeel)UIManager.getInstance().getLookAndFeel()).setRadioButtonImages(radioFullImage, radioEmptyImage, radioFullImage, radioEmptyImage);

Container container1 = BoxLayout.encloseY(new Label("Swipe the tab to see more"),
        new Label("You can put anything here"));
t.addTab("Tab1", container1);
t.addTab("Tab2", new SpanLabel("Some text directly in the tab"));

RadioButton firstTab = new RadioButton("");
RadioButton secondTab = new RadioButton("");
firstTab.setUIID("Container");
secondTab.setUIID("Container");
new ButtonGroup(firstTab, secondTab);
firstTab.setSelected(true);
Container tabsFlow = FlowLayout.encloseCenter(firstTab, secondTab);

hi.add(t);
hi.add(BorderLayout.south(tabsFlow));

t.addSelectionListener((i1, i2) -> {
    switch(i2) {
        case 0:
            if(!firstTab.isSelected()) {
                firstTab.setSelected(true);
            }
            break;
        case 1:
            if(!secondTab.isSelected()) {
                secondTab.setSelected(true);
            }
            break;
     }
});
// end::the-components-of-codename-one-java-081[]

// tag::the-components-of-codename-one-java-082[]
Tabs tabs = new Tabs();
tabs.setAnimatedIndicator(true);
// end::the-components-of-codename-one-java-082[]

// tag::the-components-of-codename-one-java-083[]
final Form hi = new Form("MediaPlayer", new BorderLayout());
hi.setToolbar(new Toolbar());
Style s = UIManager.getInstance().getComponentStyle("Title");
FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_VIDEO_LIBRARY, s);
hi.getToolbar().addCommandToRightBar("", icon, (evt) -> {
    Display.getInstance().openGallery((e) -> {
        if(e != null && e.getSource() != null) {
            String file = (String)e.getSource();
            try {
                Media video = MediaManager.createMedia(file, true);
                hi.removeAll();
                hi.add(BorderLayout.CENTER, new MediaPlayer(video));
                hi.revalidate();
            } catch(IOException err) {
                Log.e(err);
            }
        }
    }, Display.GALLERY_VIDEO);
});
hi.show();
// end::the-components-of-codename-one-java-083[]

// tag::the-components-of-codename-one-java-084[]
Form hi = new Form("ImageViewer", new BorderLayout());
ImageViewer iv = new ImageViewer(duke);
hi.add(BorderLayout.CENTER, iv);
// end::the-components-of-codename-one-java-084[]

// tag::the-components-of-codename-one-java-085[]
Form hi = new Form("ImageViewer", new BorderLayout());

Image red = Image.createImage(100, 100, 0xffff0000);
Image green = Image.createImage(100, 100, 0xff00ff00);
Image blue = Image.createImage(100, 100, 0xff0000ff);
Image gray = Image.createImage(100, 100, 0xffcccccc);

ImageViewer iv = new ImageViewer(red);
iv.setImageList(new DefaultListModel<>(red, green, blue, gray));
hi.add(BorderLayout.CENTER, iv);
// end::the-components-of-codename-one-java-085[]

// tag::the-components-of-codename-one-java-086[]
ImageViewer iv = new ImageViewer(red);
iv.setImageList(new DefaultListModel<>(red, green, blue, gray));
iv.setNavigationArrowsVisible(true);
iv.setThumbnailsVisible(true);
iv.setThumbnailBarHeight(6f); // Optional, defaults to 6mm
// end::the-components-of-codename-one-java-086[]

// tag::the-components-of-codename-one-java-087[]
Form hi = new Form("ImageViewer", new BorderLayout());
final EncodedImage placeholder = EncodedImage.createFromImage(
        FontImage.createMaterial(FontImage.MATERIAL_SYNC, s).
                scaled(300, 300), false);

class ImageList implements ListModel<Image> {
    private int selection;
    private String[] imageURLs = {
        "http://awoiaf.westeros.org/images/thumb/9/93/AGameOfThrones.jpg/300px-AGameOfThrones.jpg",
        "http://awoiaf.westeros.org/images/thumb/3/39/AClashOfKings.jpg/300px-AClashOfKings.jpg",
        "http://awoiaf.westeros.org/images/thumb/2/24/AStormOfSwords.jpg/300px-AStormOfSwords.jpg",
        "http://awoiaf.westeros.org/images/thumb/a/a3/AFeastForCrows.jpg/300px-AFeastForCrows.jpg",
        "http://awoiaf.westeros.org/images/7/79/ADanceWithDragons.jpg"
    };
    private Image[] images;
    private EventDispatcher listeners = new EventDispatcher();

    public ImageList() {
        this.images = new EncodedImage[imageURLs.length];
    }

    public Image getItemAt(final int index) {
        if(images[index] == null) {
            images[index] = placeholder;
            Util.downloadUrlToStorageInBackground(imageURLs[index], "list" + index, (e) -> {
                    try {
                        images[index] = EncodedImage.create(Storage.getInstance().createInputStream("list" + index));
                        listeners.fireDataChangeEvent(index, DataChangedListener.CHANGED);
                    } catch(IOException err) {
                        err.printStackTrace();
                    }
            });
        }
        return images[index];
    }

    public int getSize() {
        return imageURLs.length;
    }

    public int getSelectedIndex() {
        return selection;
    }

    public void setSelectedIndex(int index) {
        selection = index;
    }

    public void addDataChangedListener(DataChangedListener l) {
        listeners.addListener(l);
    }

    public void removeDataChangedListener(DataChangedListener l) {
        listeners.removeListener(l);
    }

    public void addSelectionListener(SelectionListener l) {
    }

    public void removeSelectionListener(SelectionListener l) {
    }

    public void addItem(Image item) {
    }

    public void removeItem(int index) {
    }
};

ImageList imodel = new ImageList();

ImageViewer iv = new ImageViewer(imodel.getItemAt(0));
iv.setImageList(imodel);
hi.add(BorderLayout.CENTER, iv);
// end::the-components-of-codename-one-java-087[]

// tag::the-components-of-codename-one-java-088[]
TableLayout tl = new TableLayout(2, 2);
Form hi = new Form("ScaleImageButton/Label", tl);
Style s = UIManager.getInstance().getComponentStyle("Button");
Image icon = FontImage.createMaterial(FontImage.MATERIAL_WARNING, s);
ScaleImageLabel fillLabel = new ScaleImageLabel(icon);
fillLabel.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FILL);
ScaleImageButton fillButton = new ScaleImageButton(icon);
fillButton.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FILL);
hi.add(tl.createConstraint().widthPercentage(20), new ScaleImageButton(icon)).
        add(tl.createConstraint().widthPercentage(80), new ScaleImageLabel(icon)).
        add(fillLabel).
        add(fillButton);
hi.show();
// end::the-components-of-codename-one-java-088[]

// tag::the-components-of-codename-one-java-089[]
Toolbar.setGlobalToolbar(true);

Form hi = new Form("Toolbar", new BoxLayout(BoxLayout.Y_AXIS));
hi.getToolbar().addCommandToLeftBar("Left", icon, (e) -> Log.p("Clicked"));
hi.getToolbar().addCommandToRightBar("Right", icon, (e) -> Log.p("Clicked"));
hi.getToolbar().addCommandToOverflowMenu("Overflow", icon, (e) -> Log.p("Clicked"));
hi.getToolbar().addCommandToSideMenu("Sidemenu", icon, (e) -> Log.p("Clicked"));
hi.show();
// end::the-components-of-codename-one-java-089[]

// tag::the-components-of-codename-one-java-090[]
Toolbar.setGlobalToolbar(true);
Style s = UIManager.getInstance().getComponentStyle("Title");

Form hi = new Form("Toolbar", new BoxLayout(BoxLayout.Y_AXIS));
TextField searchField = new TextField("", "Toolbar Search"); <1>
searchField.getHintLabel().setUIID("Title");
searchField.setUIID("Title");
searchField.getAllStyles().setAlignment(Component.LEFT);
hi.getToolbar().setTitleComponent(searchField);
FontImage searchIcon = FontImage.createMaterial(FontImage.MATERIAL_SEARCH, s);
searchField.addDataChangeListener((i1, i2) -> { <2>
    String t = searchField.getText();
    if(t.length() < 1) {
        for(Component cmp : hi.getContentPane()) {
            cmp.setHidden(false);
            cmp.setVisible(true);
        }
    } else {
        t = t.toLowerCase();
        for(Component cmp : hi.getContentPane()) {
            String val = null;
            if(cmp instanceof Label) {
                val = ((Label)cmp).getText();
            } else {
                if(cmp instanceof TextArea) {
                    val = ((TextArea)cmp).getText();
                } else {
                    val = (String)cmp.getPropertyValue("text");
                }
            }
            boolean show = val != null && val.toLowerCase().indexOf(t) > -1;
            cmp.setHidden(!show); <3>
            cmp.setVisible(show);
        }
    }
    hi.getContentPane().animateLayout(250);
});
hi.getToolbar().addCommandToRightBar("", searchIcon, (e) -> {
    searchField.startEditingAsync(); <4>
});

hi.add("A Game of Thrones").
        add("A Clash Of Kings").
        add("A Storm Of Swords").
        add("A Feast For Crows").
        add("A Dance With Dragons").
        add("The Winds of Winter").
        add("A Dream of Spring");
hi.show();
// end::the-components-of-codename-one-java-090[]

// tag::the-components-of-codename-one-java-091[]
Image duke = null;
try {
    duke = Image.createImage("/duke.png");
} catch(IOException err) {
    Log.e(err);
}
int fiveMM = Display.getInstance().convertToPixels(5);
final Image finalDuke = duke.scaledWidth(fiveMM);
Toolbar.setGlobalToolbar(true);
Form hi = new Form("Search", BoxLayout.y());
hi.add(new InfiniteProgress());
Display.getInstance().scheduleBackgroundTask(()-> {
    // this will take a while...
    Contact[] cnts = Display.getInstance().getAllContacts(true, true, true, true, false, false);
    Display.getInstance().callSerially(() -> {
        hi.removeAll();
        for(Contact c : cnts) {
            MultiButton m = new MultiButton();
            m.setTextLine1(c.getDisplayName());
            m.setTextLine2(c.getPrimaryPhoneNumber());
            Image pic = c.getPhoto();
            if(pic != null) {
                m.setIcon(fill(pic, finalDuke.getWidth(), finalDuke.getHeight()));
            } else {
                m.setIcon(finalDuke);
            }
            hi.add(m);
        }
        hi.revalidate();
    });
});

hi.getToolbar().addSearchCommand(e -> {
    String text = (String)e.getSource();
    if(text == null || text.length() == 0) {
        // clear search
        for(Component cmp : hi.getContentPane()) {
            cmp.setHidden(false);
            cmp.setVisible(true);
        }
        hi.getContentPane().animateLayout(150);
    } else {
        text = text.toLowerCase();
        for(Component cmp : hi.getContentPane()) {
            MultiButton mb = (MultiButton)cmp;
            String line1 = mb.getTextLine1();
            String line2 = mb.getTextLine2();
            boolean show = line1 != null && line1.toLowerCase().indexOf(text) > -1 ||
                    line2 != null && line2.toLowerCase().indexOf(text) > -1;
            mb.setHidden(!show);
            mb.setVisible(show);
        }
        hi.getContentPane().animateLayout(150);
    }
}, 4);

hi.show();
// end::the-components-of-codename-one-java-091[]

// tag::the-components-of-codename-one-java-092[]
toolbar.setComponentToSideMenuSouth(myComponent);
// end::the-components-of-codename-one-java-092[]

// tag::the-components-of-codename-one-java-093[]
Toolbar.setGlobalToolbar(true);

Form hi = new Form("Toolbar", new BoxLayout(BoxLayout.Y_AXIS));
EncodedImage placeholder = EncodedImage.createFromImage(Image.createImage(hi.getWidth(), hi.getWidth() / 5, 0xffff0000), true);
URLImage background = URLImage.createToStorage(placeholder, "400px-AGameOfThrones.jpg",
        "http://awoiaf.westeros.org/images/thumb/9/93/AGameOfThrones.jpg/400px-AGameOfThrones.jpg");
background.fetch();
Style stitle = hi.getToolbar().getTitleComponent().getUnselectedStyle();
stitle.setBgImage(background);
stitle.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FILL);
stitle.setPaddingUnit(Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS);
stitle.setPaddingTop(15);
SpanButton credit = new SpanButton("This excerpt is from A Wiki Of Ice And Fire. Please check it out by clicking here!");
credit.addActionListener((e) -> Display.getInstance().execute("http://awoiaf.westeros.org/index.php/A_Game_of_Thrones"));
hi.add(new SpanLabel("A Game of Thrones is the first of seven planned novels in A Song of Ice and Fire, an epic fantasy series by American author George R. R. Martin. It was first published on 6 August 1996. The novel was nominated for the 1998 Nebula Award and the 1997 World Fantasy Award,[1] and won the 1997 Locus Award.[2] The novella Blood of the Dragon, comprising the Daenerys Targaryen chapters from the novel, won the 1997 Hugo Award for Best Novella. ")).
        add(new Label("Plot introduction", "Heading")).
        add(new SpanLabel("A Game of Thrones is set in the Seven Kingdoms of Westeros, a land reminiscent of Medieval Europe. In Westeros the seasons last for years, sometimes decades, at a time.\n\n" +
            "Fifteen years prior to the novel, the Seven Kingdoms were torn apart by a civil war, known alternately as \"Robert's Rebellion\" and the \"War of the Usurper.\" Prince Rhaegar Targaryen kidnapped Lyanna Stark, arousing the ire of her family and of her betrothed, Lord Robert Baratheon (the war's titular rebel). The Mad King, Aerys II Targaryen, had Lyanna's father and eldest brother executed when they demanded her safe return. Her second brother, Eddard, joined his boyhood friend Robert Baratheon and Jon Arryn, with whom they had been fostered as children, in declaring war against the ruling Targaryen dynasty, securing the allegiances of House Tully and House Arryn through a network of dynastic marriages (Lord Eddard to Catelyn Tully and Lord Arryn to Lysa Tully). The powerful House Tyrell continued to support the King, but House Lannister and House Martell both stalled due to insults against their houses by the Targaryens. The civil war climaxed with the Battle of the Trident, when Prince Rhaegar was killed in battle by Robert Baratheon. The Lannisters finally agreed to support King Aerys, but then brutally... ")).
        add(credit);

ComponentAnimation title = hi.getToolbar().getTitleComponent().createStyleAnimation("Title", 200);
hi.getAnimationManager().onTitleScrollAnimation(title);
hi.show();
// end::the-components-of-codename-one-java-093[]

// tag::the-components-of-codename-one-java-094[]
ComponentAnimation title = hi.getToolbar().getTitleComponent().createStyleAnimation("Title", 200);
hi.getAnimationManager().onTitleScrollAnimation(title);
// end::the-components-of-codename-one-java-094[]

// tag::the-components-of-codename-one-java-095[]
Form hi = new Form("Browser", new BorderLayout());
BrowserComponent browser = new BrowserComponent();
browser.setURL("https://www.codenameone.com/");
hi.add(BorderLayout.CENTER, browser);
// end::the-components-of-codename-one-java-095[]

// tag::the-components-of-codename-one-java-096[]
BrowserComponent wb = new BrowserComponent();
wb.setURL("jar:///Page.html");
// end::the-components-of-codename-one-java-096[]

// tag::the-components-of-codename-one-java-097[]
try {
    browserComponent.setURLHierarchy("/htmlFile.html");
} catch(IOException err) {
    ...
}
// end::the-components-of-codename-one-java-097[]

// tag::the-components-of-codename-one-java-098[]
Form hi = new Form("BrowserComponent", new BorderLayout());
BrowserComponent bc = new BrowserComponent();
bc.setPage( "<html lang=\"en\">\n" +
            "    <head>\n" +
            "        <meta charset=\"utf-8\">\n" +
            "        <script>\n" +
            "          function  fnc(message) {\n" +
            "         document.write(message);\n" +
            "            };\n" +
            "        </script>\n" +
            "    </head>\n" +
            "    <body >\n" +
            "        <p><a href=\"http://click\">Demo</a></p>\n" +
            "    </body>\n" +
            "</html>", null);
hi.add(BorderLayout.CENTER, bc);
bc.setBrowserNavigationCallback((url) -> {
    if(url.startsWith("http://click")) {
        Display.getInstance().callSerially(() -> bc.execute("fnc('<p>You clicked!</p>')"));
        return false;
    }
    return true;
});
// end::the-components-of-codename-one-java-098[]

// tag::the-components-of-codename-one-java-099[]
JSObject window = ctx.get("window");
// end::the-components-of-codename-one-java-099[]

// tag::the-components-of-codename-one-java-100[]
bc.execute(
    "callback.onSuccess(3+4)",
    res -> Log.p("The result was "+res.getInt())
);
// end::the-components-of-codename-one-java-100[]

// tag::the-components-of-codename-one-java-101[]
public void execute(String js, SuccessCallback<JSRef> callback)
// end::the-components-of-codename-one-java-101[]

// tag::the-components-of-codename-one-java-102[]
JSRef res = bc.executeAndWait("callback.onSuccess(3+4)");
Log.p("The result was "+res.Int());
// end::the-components-of-codename-one-java-102[]

// tag::the-components-of-codename-one-java-103[]
bc.execute(
    "$('#somebutton').click(function(){callback.onSuccess('Button was clicked')})",
    res -> Log.p(res.toString())
);
// end::the-components-of-codename-one-java-103[]

// tag::the-components-of-codename-one-java-104[]
bc.addJSCallback(
    "$('#somebutton').click(function(){callback.onSuccess('Button was clicked')})",
    res -> Log.p(res.toString())
);
// end::the-components-of-codename-one-java-104[]

// tag::the-components-of-codename-one-java-105[]
bc.execute(
    "jQuery('#bio').text(${0}); jQuery('#age').text(${1})",
    new Object[]{
       "A multi-line\n string with \"quotes\"",
       27
    }
);
// end::the-components-of-codename-one-java-105[]

// tag::the-components-of-codename-one-java-106[]
JSProxy location = bc.createJSProxy("window.location");
// end::the-components-of-codename-one-java-106[]

// tag::the-components-of-codename-one-java-107[]
location.get("href", res -> Log.p("location.href="+res));
// end::the-components-of-codename-one-java-107[]

// tag::the-components-of-codename-one-java-108[]
JSRef href = location.getAndWait("href");
Log.p("location.href="+href);
// end::the-components-of-codename-one-java-108[]

// tag::the-components-of-codename-one-java-109[]
location.set("href", "http://www.google.com");
// end::the-components-of-codename-one-java-109[]

// tag::the-components-of-codename-one-java-110[]
location.call("replace", new Object[]{"http://www.google.com"},
    res -> Log.p("Return value was "+res)
);
// end::the-components-of-codename-one-java-110[]

// tag::the-components-of-codename-one-java-111[]
Form hi = new Form("BrowserComponent", new BorderLayout());
BrowserComponent bc = new BrowserComponent();
bc.setPage( "<html lang=\"en\">\n" +
            "    <head>\n" +
            "        <meta charset=\"utf-8\">\n" +
            "        <script>\n" +
            "          function  fnc(message) {\n" +
            "         document.write(message);\n" +
            "            };\n" +
            "        </script>\n" +
            "    </head>\n" +
            "    <body >\n" +
            "        <p>Demo</p>\n" +
            "    </body>\n" +
            "</html>", null);
TextField tf = new TextField();
hi.add(BorderLayout.CENTER, bc).
        add(BorderLayout.SOUTH, tf);
bc.addWebEventListener("onLoad", (e) ->  bc.execute("fnc('<p>Hello World</p>')"));
tf.addActionListener((e) -> bc.execute("fnc('<p>" + tf.getText() +"</p>')"));
hi.show();
// end::the-components-of-codename-one-java-111[]

// tag::the-components-of-codename-one-java-112[]
Form hi = new Form("BrowserComponent", new BorderLayout());
BrowserComponent bc = new BrowserComponent();
bc.setPage( "<html lang=\"en\">\n" +
            "    <head>\n" +
            "        <meta charset=\"utf-8\">\n" +
            "    </head>\n" +
            "    <body >\n" +
            "        <p>This will appear twice...</p>\n" +
            "    </body>\n" +
            "</html>", null);
hi.add(BorderLayout.CENTER, bc);
bc.addWebEventListener("onLoad", (e) -> {
    // Create a JavaScript context for this BrowserComponent
    JavascriptContext ctx = new JavascriptContext(bc);

    String pageContent = (String)ctx.get("document.body.innerHTML");
    hi.add(BorderLayout.SOUTH, pageContent);
    hi.revalidate();
});
hi.show();
// end::the-components-of-codename-one-java-112[]

// tag::the-components-of-codename-one-java-113[]
Double outerWidth = (Double)ctx.get("window.outerWidth");
// end::the-components-of-codename-one-java-113[]

// tag::the-components-of-codename-one-java-114[]
Form hi = new Form("BrowserComponent", new BorderLayout());
BrowserComponent bc = new BrowserComponent();
bc.setPage( "<html lang=\"en\">\n" +
            "    <head>\n" +
            "        <meta charset=\"utf-8\">\n" +
            "    </head>\n" +
            "    <body >\n" +
            "        <p>Please Wait...</p>\n" +
            "    </body>\n" +
            "</html>", null);
hi.add(BorderLayout.CENTER, bc);
bc.addWebEventListener("onLoad", (e) -> {
    // Create a JavaScript context for this BrowserComponent
    JavascriptContext ctx = new JavascriptContext(bc);

    JSObject jo = (JSObject)ctx.get("window");
    jo.set("location", "https://www.codenameone.com/");
});
// end::the-components-of-codename-one-java-114[]

// tag::the-components-of-codename-one-java-115[]
Form hi = new Form("Compose", new BorderLayout());
RichTextArea editor = new RichTextArea();
editor.setPlaceholder("Write something...");
editor.setHtml("<h2>Trip itinerary</h2><p>Meet at the <b>main lobby</b>.</p>");

Toolbar tb = hi.getToolbar();
tb.addCommandToRightBar("B", null, e -> editor.bold());
tb.addCommandToRightBar("I", null, e -> editor.italic());
tb.addCommandToRightBar("List", null, e -> editor.insertUnorderedList());
tb.addCommandToRightBar("Save", null, e ->
        editor.getHtml(html -> Log.p("User wrote: " + html)));

hi.add(BorderLayout.CENTER, editor);
hi.show();
// end::the-components-of-codename-one-java-115[]

// tag::the-components-of-codename-one-java-116[]
editor.getHtml(html -> storage.save(html));   // full HTML markup
editor.getText(text -> index(text));          // plain text, markup stripped
// end::the-components-of-codename-one-java-116[]

// tag::the-components-of-codename-one-java-117[]
Form hi = new Form("Editor", new BorderLayout());
CodeEditor editor = new CodeEditor();
editor.setLanguage("java");
editor.setTheme("light");          // or "dark"
editor.setShowLineNumbers(true);
editor.setText("public class Main {\n\n}");
hi.add(BorderLayout.CENTER, editor);
hi.show();
// end::the-components-of-codename-one-java-117[]

// tag::the-components-of-codename-one-java-118[]
editor.setCompletionProvider((ed, code, cursor, results) -> {
    String prefix = currentWord(code, cursor);
    List<CodeCompletion> out = new ArrayList<>();
    for (String member : new String[] {"println(", "print(", "printf(", "flush()"}) {
        if (member.startsWith(prefix)) {
            out.add(new CodeCompletion(member).setType("method"));
        }
    }
    results.onSucess(out);   // an empty list hides the popup
});
// end::the-components-of-codename-one-java-118[]

// tag::the-components-of-codename-one-java-119[]
Form hi = new Form("Auto Complete", new BoxLayout(BoxLayout.Y_AXIS));
AutoCompleteTextField ac = new AutoCompleteTextField("Short", "Shock", "Sholder", "Shrek");
ac.setMinimumElementsShownInPopup(5);
hi.add(ac);
// end::the-components-of-codename-one-java-119[]

// tag::the-components-of-codename-one-java-120[]
public void showForm() {
  final DefaultListModel<String> options = new DefaultListModel<>();
  AutoCompleteTextField ac = new AutoCompleteTextField(options) {
      @Override
      protected boolean filter(String text) {
          if(text.length() == 0) {
              return false;
          }
          String[] l = searchLocations(text);
          if(l == null || l.length == 0) {
              return false;
          }

          options.removeAll();
          for(String s : l) {
              options.addItem(s);
          }
          return true;
      }

  };
  ac.setMinimumElementsShownInPopup(5);
  hi.add(ac);
  hi.add(new SpanLabel("This demo requires a valid google API key to be set below "
           + "you can get this key for the webservice (not the native key) by following the instructions here: "
           + "https://developers.google.com/places/web-service/get-api-key"));
  hi.add(apiKey);
  hi.getToolbar().addCommandToRightBar("Get Key", null, e -> Display.getInstance().execute("https://developers.google.com/places/web-service/get-api-key"));
  hi.show();
}

TextField apiKey = new TextField();

String[] searchLocations(String text) {
    try {
        if(text.length() > 0) {
            ConnectionRequest r = new ConnectionRequest();
            r.setPost(false);
            r.setUrl("https://maps.googleapis.com/maps/api/place/autocomplete/json");
            r.addArgument("key", apiKey.getText());
            r.addArgument("input", text);
            NetworkManager.getInstance().addToQueueAndWait(r);
            Map<String,Object> result = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
            String[] res = Result.fromContent(result).getAsStringArray("//description");
            return res;
        }
    } catch(Exception err) {
        Log.e(err);
    }
    return null;
}
// end::the-components-of-codename-one-java-120[]

// tag::the-components-of-codename-one-java-121[]
final String[] characters = { "Tyrion Lannister", "Jaime Lannister", "Cersei Lannister", "Daenerys Targaryen",
    "Jon Snow", "Petyr Baelish", "Jorah Mormont", "Sansa Stark", "Arya Stark", "Theon Greyjoy"
    // snipped the rest for clarity
};

Form current = new Form("AutoComplete", BoxLayout.y());

AutoCompleteTextField ac = new AutoCompleteTextField(characters);

final int size = Display.getInstance().convertToPixels(7);
final EncodedImage placeholder = EncodedImage.createFromImage(Image.createImage(size, size, 0xffcccccc), true);

final String[] actors = { "Peter Dinklage", "Nikolaj Coster-Waldau", "Lena Headey"}; // <1>
final Image[] pictures = {
    URLImage.createToStorage(placeholder, "tyrion","http://i.lv3.hbo.com/assets/images/series/game-of-thrones/character/s5/tyrion-lannister-512x512.jpg"),
    URLImage.createToStorage(placeholder, "jaime","http://i.lv3.hbo.com/assets/images/series/game-of-thrones/character/s5/jamie-lannister-512x512.jpg"),
    URLImage.createToStorage(placeholder, "cersei","http://i.lv3.hbo.com/assets/images/series/game-of-thrones/character/s5/cersei-lannister-512x512.jpg")
};

ac.setCompletionRenderer(new ListCellRenderer() {
    private final Label focus = new Label(); // <2>
    private final Label line1 = new Label(characters[0]);
    private final Label line2 = new Label(actors[0]);
    private final Label icon = new Label(pictures[0]);
    private final Container selection = BorderLayout.center(
            BoxLayout.encloseY(line1, line2)).add(BorderLayout.EAST, icon);

    @Override
    public Component getListCellRendererComponent(com.codename1.ui.List list, Object value, int index, boolean isSelected) {
        for(int iter = 0 ; iter < characters.length ; iter++) {
            if(characters[iter].equals(value)) {
                line1.setText(characters[iter]);
                if(actors.length > iter) {
                    line2.setText(actors[iter]);
                    icon.setIcon(pictures[iter]);
                } else {
                    line2.setText(""); // <3>
                    icon.setIcon(placeholder);
                }
                break;
            }
        }
        return selection;
    }

    @Override
    public Component getListFocusComponent(com.codename1.ui.List list) {
        return focus;
    }
});
current.add(ac);

current.show();
// end::the-components-of-codename-one-java-121[]

// tag::the-components-of-codename-one-java-122[]
Form hi = new Form("Picker", new BoxLayout(BoxLayout.Y_AXIS));
Picker datePicker = new Picker();
datePicker.setType(Display.PICKER_TYPE_DATE);
Picker dateTimePicker = new Picker();
dateTimePicker.setType(Display.PICKER_TYPE_DATE_AND_TIME);
Picker timePicker = new Picker();
timePicker.setType(Display.PICKER_TYPE_TIME);
Picker stringPicker = new Picker();
stringPicker.setType(Display.PICKER_TYPE_STRINGS);
Picker durationPicker = new Picker();
durationPicker.setType(Display.PICKER_TYPE_DURATION);
Picker minuteDurationPicker = new Picker();
minuteDurationPicker.setType(Display.PICKER_TYPE_DURATION_MINUTES);
Picker hourDurationPicker = new Picker();
hourDurationPicker.setType(Display.PICKER_TYPE_DURATION_HOURS);

datePicker.setDate(new Date());
dateTimePicker.setDate(new Date());
timePicker.setTime(10 * 60); // 10:00AM = Minutes since midnight
stringPicker.setStrings("A Game of Thrones", "A Clash Of Kings", "A Storm Of Swords", "A Feast For Crows",
        "A Dance With Dragons", "The Winds of Winter", "A Dream of Spring");
stringPicker.setSelectedString("A Game of Thrones");

hi.add(datePicker).add(dateTimePicker).add(timePicker)
  .add(stringPicker).add(durationPicker)
  .add(minuteDurationPicker).add(hourDurationPicker);
hi.show();
// end::the-components-of-codename-one-java-122[]

// tag::the-components-of-codename-one-java-123[]
Picker picker = new Picker();
picker.setType(Display.PICKER_TYPE_DATE);
picker.setUseLightweightPopup(true);
picker.setDate(new Date());

picker.addLightweightPopupButton("Today", () -> picker.setDate(new Date()));

picker.addLightweightPopupButton("+7 Days", () -> {
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, 7);
    picker.setDate(cal.getTime());
}, Picker.LightweightPopupButtonPlacement.BELOW_SPINNER);
// end::the-components-of-codename-one-java-123[]

// tag::the-components-of-codename-one-java-124[]
SwipeableContainer swip = new SwipeableContainer(bottom, top);
// end::the-components-of-codename-one-java-124[]

// tag::the-components-of-codename-one-java-125[]
Form hi = new Form("Swipe", new BoxLayout(BoxLayout.Y_AXIS));
hi.add(createRankWidget("A Game of Thrones", "1996")).
    add(createRankWidget("A Clash Of Kings", "1998")).
    add(createRankWidget("A Storm Of Swords", "2000")).
    add(createRankWidget("A Feast For Crows", "2005")).
    add(createRankWidget("A Dance With Dragons", "2011")).
    add(createRankWidget("The Winds of Winter", "TBD")).
    add(createRankWidget("A Dream of Spring", "TBD"));
hi.show();

public SwipeableContainer createRankWidget(String title, String year) {
    MultiButton button = new MultiButton(title);
    button.setTextLine2(year);
    return new SwipeableContainer(FlowLayout.encloseCenterMiddle(createStarRankSlider()),
            button);
}
// end::the-components-of-codename-one-java-125[]

// tag::the-components-of-codename-one-java-126[]
Form map = new Form("Map");
map.setLayout(new BorderLayout());
map.setScrollable(false);
final MapComponent mc = new MapComponent();

try {
   //get the current location from the Location API
   Location loc = LocationManager.getLocationManager().getCurrentLocation();

  Coord lastLocation = new Coord(loc.getLatitude(), loc.getLongtitude());
   Image i = Image.createImage("/blue_pin.png");
   PointsLayer pl = new PointsLayer();
   pl.setPointIcon(i);
   PointLayer p = new PointLayer(lastLocation, "You Are Here", i);
   p.setDisplayName(true);
   pl.addPoint(p);
   mc.addLayer(pl);
} catch (IOException ex) {
   ex.printStackTrace();
}
mc.zoomToLayers();

map.addComponent(BorderLayout.CENTER, mc);
map.addCommand(new BackCommand());
map.setBackCommand(new BackCommand());
map.show();
// end::the-components-of-codename-one-java-126[]

// tag::the-components-of-codename-one-java-127[]

          final Form map = new Form("Map");
           map.setLayout(new BorderLayout());
           map.setScrollable(false);
           final MapComponent mc = new MapComponent();
           Location loc = LocationManager.getLocationManager().getCurrentLocation();
           //use the code from above to show you on the map
           putMeOnMap(mc);
           map.addComponent(BorderLayout.CENTER, mc);
           map.addCommand(new BackCommand());
           map.setBackCommand(new BackCommand());

           ConnectionRequest req = new ConnectionRequest() {

               protected void readResponse(InputStream input) throws IOException {
                   JSONParser p = new JSONParser();
                   Hashtable h = p.parse(new InputStreamReader(input));
                   // "status" : "REQUEST_DENIED"
                   String response = (String)h.get("status");
                   if(response.equals("REQUEST_DENIED")){
                       System.out.println("make sure to obtain a key from "
                               + "https://developers.google.com/maps/documentation/places/");
                       progress.dispose();
                       Dialog.show("Info", "make sure to obtain an application key from "
                               + "google places api's"
                               , "Ok", null);
                       return;
                   }

                   final Vector v = (Vector) h.get("results");

                   Image im = Image.createImage("/red_pin.png");
                   PointsLayer pl = new PointsLayer();
                   pl.setPointIcon(im);
                   pl.addActionListener(new ActionListener() {

                       public void actionPerformed(ActionEvent evt) {
                           PointLayer p = (PointLayer) evt.getSource();
                           System.out.println("pressed " + p);

                           Dialog.show("Details", "" + p.getName(), "Ok", null);
                       }
                   });

                   for (int i = 0; i < v.size(); i++) {
                       Hashtable entry = (Hashtable) v.elementAt(i);
                       Hashtable geo = (Hashtable) entry.get("geometry");
                       Hashtable loc = (Hashtable) geo.get("location");
                       Double lat = (Double) loc.get("lat");
                       Double lng = (Double) loc.get("lng");
                       PointLayer point = new PointLayer(new Coord(lat.doubleValue(), lng.doubleValue()),
                               (String) entry.get("name"), null);
                       pl.addPoint(point);
                   }
                   progress.dispose();

                   mc.addLayer(pl);
                   map.show();
                   mc.zoomToLayers();

               }
           };
           req.setUrl("https://maps.googleapis.com/maps/api/place/search/json");
           req.setPost(false);
           req.addArgument("location", "" + loc.getLatitude() + "," + loc.getLongtitude());
           req.addArgument("radius", "500");
           req.addArgument("types", "food");
           req.addArgument("sensor", "false");

           //get your own key from https://developers.google.com/maps/documentation/places/
           //and replace it here.
           String key = "yourAPIKey";

           req.addArgument("key", key);

           NetworkManager.getInstance().addToQueue(req);
       }
       catch (IOException ex) {
           ex.printStackTrace();
       }
   }
// end::the-components-of-codename-one-java-127[]

// tag::the-components-of-codename-one-java-128[]
/**
 * Creates a renderer for the specified colors.
 */
private DefaultRenderer buildCategoryRenderer(int[] colors) {
    DefaultRenderer renderer = new DefaultRenderer();
    renderer.setLabelsTextSize(15);
    renderer.setLegendTextSize(15);
    renderer.setMargins(new int[]{20, 30, 15, 0});
    for (int color : colors) {
        SimpleSeriesRenderer r = new SimpleSeriesRenderer();
        r.setColor(color);
        renderer.addSeriesRenderer(r);
    }
    return renderer;
}

/**
 * Builds a category series using the provided values.
 *
 * @param titles the series titles
 * @param values the values
 * @return the category series
 */
protected CategorySeries buildCategoryDataset(String title, double[] values) {
    CategorySeries series = new CategorySeries(title);
    int k = 0;
    for (double value : values) {
        series.add("Project " + ++k, value);
    }

    return series;
}

public Form createPieChartForm() {

    // Generate the values
    double[] values = new double[]{12, 14, 11, 10, 19};

    // Set up the renderer
    int[] colors = new int[]{ColorUtil.BLUE, ColorUtil.GREEN, ColorUtil.MAGENTA, ColorUtil.YELLOW, ColorUtil.CYAN};
    DefaultRenderer renderer = buildCategoryRenderer(colors);
    renderer.setZoomButtonsVisible(true);
    renderer.setZoomEnabled(true);
    renderer.setChartTitleTextSize(20);
    renderer.setDisplayValues(true);
    renderer.setShowLabels(true);
    SimpleSeriesRenderer r = renderer.getSeriesRendererAt(0);
    r.setGradientEnabled(true);
    r.setGradientStart(0, ColorUtil.BLUE);
    r.setGradientStop(0, ColorUtil.GREEN);
    r.setHighlighted(true);

    // Create the chart ... pass the values and renderer to the chart object.
    PieChart chart = new PieChart(buildCategoryDataset("Project budget", values), renderer);

    // Wrap the chart in a Component so we can add it to a form
    ChartComponent c = new ChartComponent(chart);

    // Create a form and show it.
    Form f = new Form("Budget");
    f.setLayout(new BorderLayout());
    f.addComponent(BorderLayout.CENTER, c);
    return f;

}
// end::the-components-of-codename-one-java-128[]

// tag::the-components-of-codename-one-java-129[]
Form hi = new Form("Calendar", new BorderLayout());
Calendar cld = new Calendar();
cld.addActionListener((e) -> Log.p("You picked: " + new Date(cld.getSelectedDay())));
hi.add(BorderLayout.CENTER, cld);
// end::the-components-of-codename-one-java-129[]

// tag::the-components-of-codename-one-java-130[]
Status status = ToastBar.getInstance().createStatus();
status.setMessage("Downloading your file...");
status.show();

//  ... Later on when download completes
status.clear();
// end::the-components-of-codename-one-java-130[]

// tag::the-components-of-codename-one-java-131[]
Status status = ToastBar.getInstance().createStatus();
status.setMessage("Hello world");
status.setShowProgressIndicator(true);
status.show();
// end::the-components-of-codename-one-java-131[]

// tag::the-components-of-codename-one-java-132[]
Status status = ToastBar.getInstance().createStatus();
status.setMessage("Hello world");
status.setExpires(3000);  // only show the status for 3 seconds, then have it automatically clear
status.show();
// end::the-components-of-codename-one-java-132[]

// tag::the-components-of-codename-one-java-133[]
Status status = ToastBar.getInstance().createStatus();
status.setMessage("Hello world");
status.showDelayed(300); // Wait 300 ms to show the status

// ... Some time later, clear the status... This may be before it shows at all
status.clear();
// end::the-components-of-codename-one-java-133[]

// tag::the-components-of-codename-one-java-134[]
Form hi = new Form("Undo", BoxLayout.y());
Button add = new Button("Add");

add.addActionListener(e -> {
    Label l = new Label("Added this");
    hi.add(l);
    hi.revalidate();
    ToastBar.showMessage("Added, click here to undo...", FontImage.MATERIAL_UNDO,
            ee -> {
                l.remove();
                hi.revalidate();
            });
});
hi.add(add);
hi.show();
// end::the-components-of-codename-one-java-134[]

// tag::the-components-of-codename-one-java-135[]
Form hi = new Form("Signature Component");
hi.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
hi.add("Enter Your Name:");
hi.add(new TextField());
hi.add("Signature:");
SignatureComponent sig = new SignatureComponent();
sig.addActionListener((evt)-> {
    System.out.println("The signature was changed");
    Image img = sig.getSignatureImage();
    // Now we can do whatever we want with the image of this signature.
});
hi.addComponent(sig);
hi.show();
// end::the-components-of-codename-one-java-135[]

// tag::the-components-of-codename-one-java-136[]
Form f = new Form("Accordion", new BoxLayout(BoxLayout.Y_AXIS));
f.setScrollableY(true);
Accordion accr = new Accordion();
accr.addContent("Item1", new SpanLabel("The quick brown fox jumps over the lazy dog\n"
        + "The quick brown fox jumps over the lazy dog"));
accr.addContent("Item2", new SpanLabel("The quick brown fox jumps over the lazy dog\n"
        + "The quick brown fox jumps over the lazy dog\n "
        + "The quick brown fox jumps over the lazy dog\n "
        + "The quick brown fox jumps over the lazy dog\n "
        + ""));

accr.addContent("Item3", BoxLayout.encloseY(new Label("Label"), new TextField(), new Button("Button"), new CheckBox("CheckBox")));

f.add(accr);
f.show();
// end::the-components-of-codename-one-java-136[]

// tag::the-components-of-codename-one-java-137[]
Form hi = new Form("Floating Hint", BoxLayout.y());
TextField first = new TextField("", "First Field");
TextField second = new TextField("", "Second Field");
hi.add(new FloatingHint(first)).
        add(new FloatingHint(second)).
        add(new Button("Go"));
hi.show();
// end::the-components-of-codename-one-java-137[]

// tag::the-components-of-codename-one-java-138[]
FloatingActionButton fab = FloatingActionButton.createFAB(FontImage.MATERIAL_ADD);
fab.addActionListener(e -> ToastBar.showErrorMessage("Not implemented yet..."));
fab.bindFabToContainer(form.getContentPane());
// end::the-components-of-codename-one-java-138[]

// tag::the-components-of-codename-one-java-139[]
FloatingActionButton fab = FloatingActionButton.createFAB(FontImage.MATERIAL_ADD);
fab.createSubFAB(FontImage.MATERIAL_PEOPLE, "");
fab.createSubFAB(FontImage.MATERIAL_IMPORT_CONTACTS, "");
fab.bindFabToContainer(form.getContentPane());
// end::the-components-of-codename-one-java-139[]

// tag::the-components-of-codename-one-java-140[]
Form hi = new Form("Badge");

Button chat = new Button("");
FontImage.setMaterialIcon(chat, FontImage.MATERIAL_CHAT, 7);

FloatingActionButton badge = FloatingActionButton.createBadge("33");
hi.add(badge.bindFabToContainer(chat, Component.RIGHT, Component.TOP));

TextField changeBadgeValue = new TextField("33");
changeBadgeValue.addDataChangedListener((i, ii) -> {
    badge.setText(changeBadgeValue.getText());
    badge.getParent().revalidate();
});
hi.add(changeBadgeValue);

hi.show();
// end::the-components-of-codename-one-java-140[]

// tag::the-components-of-codename-one-java-141[]
private Container encloseInMaximizableGrid(Component cmp1, Component cmp2) {
    GridLayout gl = new GridLayout(2, 1);
    Container grid = new Container(gl);
    gl.setHideZeroSized(true);

    grid.add(encloseInMaximize(grid, cmp1)).
            add(encloseInMaximize(grid, cmp2));
    return grid;
}
// end::the-components-of-codename-one-java-141[]

// tag::the-components-of-codename-one-java-142[]
private Container encloseInMaximizableGrid(Component cmp1, Component cmp2) {
    return new SplitPane(SplitPane.VERTICAL_SPLIT, cmp1, cmp2, "25%", "50%", "75%");
}
// end::the-components-of-codename-one-java-142[]
