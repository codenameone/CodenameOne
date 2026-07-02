// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::component-selector-java-001[]
import static com.codename1.ui.ComponentSelector.$;

// ...

Button slideUp = $(new Button("Slide Up")) // <1>
    .setIcon(FontImage.MATERIAL_EXPAND_LESS) // <2>
    .addActionListener(e->{ // <3>
        $(e) // <4>
            .getParent() // <5>
            .find(">*") // <6>
            .slideUpAndWait(1000) // <7>
            .slideDownAndWait(1000); // <8>
    })
    .asComponent(Button.class); // <9>
// end::component-selector-java-001[]

// tag::component-selector-java-002[]
TableLayout tl = new TableLayout(numRows, numCols);
Container table = new Container(tl);
int rowNum = 0;
int colNum = 0;
for (String[] row : data) {
    colNum = 0;
    for (String cell : row) {
        table.add(
            tl.createConstraint(rowNum, colNum),
            $(new Button(cell))
                .setUIID("Label")
                .addTags(rowNum % 2 == 0 ? "even":"odd")
                .asComponent()
        );
        colNum++;
    }
    rowNum++;
}
$(".even", table)
    .setBgColor(0xcccccc)
    .setBgTransparency(255);
// end::component-selector-java-002[]

// tag::component-selector-java-003[]
$(".even", table)
    .setBgColor(0xcccccc)
    .setBgTransparency(255);
// end::component-selector-java-003[]

// tag::component-selector-java-004[]
for (Component c : evenComponents) {
    c.getStyle().setBgColor(0xcccccc);
}
// end::component-selector-java-004[]

// tag::component-selector-java-005[]
$(".even", table)
    .selectPressedStyle()
    .setBgColor(0xcccccc)
    .setBgTransparency(255);
// end::component-selector-java-005[]

// tag::component-selector-java-006[]
$(".even:pressed", table)
    .setBgColor(0xcccccc)
    .setBgTransparency(255);
// end::component-selector-java-006[]

// tag::component-selector-java-007[]
Button replace = $(new Button("Replace Fade/Slide"))
    .setIcon(FontImage.MATERIAL_REDEEM)
    .addActionListener(e->{
        $(e).getParent()
            .find(">*")  // <1>
            .replaceAndWait(c->{ // <2>
                return $(new Label("Replacement")) // <3>
                    .putClientProperty("origComponent", c) // <4>
                    .asComponent();
            }, CommonTransitions.createFade(1000)) // <5>
            .replaceAndWait(c->{
                Component orig = (Component)c.getClientProperty("origComponent");
                if (orig != null) {
                    c.putClientProperty("origComponent", null);
                    return orig; // <6>
                }
                return c;

            }, CommonTransitions.createCover(CommonTransitions.SLIDE_HORIZONTAL, false, 1000)); // <7>


    })
    .asComponent(Button.class);
// end::component-selector-java-007[]
