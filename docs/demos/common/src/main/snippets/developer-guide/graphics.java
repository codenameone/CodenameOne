// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::graphics-java-001[]
// hide the title
Form hi = new Form("", new BorderLayout());
hi.add(BorderLayout.CENTER, new Component() {
    @Override
    public void paint(Graphics g) {
        // red color
        g.setColor(0xff0000);

        // paint the screen in red
        g.fillRect(getX(), getY(), getWidth(), getHeight());

        // draw hi world in white text in the top left corner of the screen
        g.setColor(0xffffff);
        g.drawString("Hi World", getX(), getY());
    }
});
hi.show();
// end::graphics-java-001[]

// tag::graphics-java-002[]
LinearGradientPaint gradient = new LinearGradientPaint(
        0, 0, getWidth(), 0,  // horizontal gradient
        new int[] {0xff4285f4, 0xff34a853, 0xfffbbc05},
        new float[] {0f, 0.5f, 1f}
);
g.setColor(gradient);
g.fillRect(getX(), getY(), getWidth(), getHeight());
// end::graphics-java-002[]

// tag::graphics-java-003[]
int[] colors  = { 0xffff0080, 0xffff8c00, 0xff40e0d0 };
float[] stops = { 0f, 0.5f, 1f };

g.fillGradient(new LinearGradient(45f, colors, stops),
        0, 0, getWidth(), getHeight());

RadialGradient circle = new RadialGradient(colors, stops);
circle.setShape(RadialGradient.SHAPE_CIRCLE)
      .setExtent(RadialGradient.EXTENT_FARTHEST_CORNER);
g.fillGradient(circle, 0, 0, getWidth(), getHeight());

g.fillGradient(new ConicGradient(colors, stops),
        0, 0, getWidth(), getHeight());
// end::graphics-java-003[]

// tag::graphics-java-004[]
hi.setGlassPane(new Painter() {
    @Override
    public void paint(Graphics g, Rectangle rect) {
    }
});
// end::graphics-java-004[]

// tag::graphics-java-005[]
hi.setGlassPane((g, rect) -> {
});
// end::graphics-java-005[]

// tag::graphics-java-006[]
Form hi = new Form("Glass Pane", new BoxLayout(BoxLayout.Y_AXIS));
Style s = UIManager.getInstance().getComponentStyle("Label");
s.setFgColor(0xff0000);
s.setBgTransparency(0);
Image warningImage = FontImage.createMaterial(FontImage.MATERIAL_WARNING, s).toImage();
TextField tf1 = new TextField("My Field");
tf1.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
tf1.getAllStyles().setMargin(5, 5, 5, 5);
hi.add(tf1);
hi.setGlassPane((g, rect) -> {
    int x = tf1.getAbsoluteX() + tf1.getWidth();
    int y = tf1.getAbsoluteY();
    x -= warningImage.getWidth() / 2;
    y += (tf1.getHeight() / 2 - warningImage.getHeight() / 2);
    g.drawImage(warningImage, x, y);
});
hi.show();
// end::graphics-java-006[]

// tag::graphics-java-007[]
public class DrawingCanvas extends Component {
    GeneralPath p = new GeneralPath();
    int strokeColor = 0x0000ff;
    int strokeWidth = 10;

    public void addPoint(float x, float y){
        // To be written
    }

    @Override
    protected void paintBackground(Graphics g) {
        super.paintBackground(g);
            Stroke stroke = new Stroke(
                strokeWidth,
                Stroke.CAP_BUTT,
                Stroke.JOIN_ROUND, 1f
            );
            g.setColor(strokeColor);

            // Draw the shape
            g.drawShape(p, stroke);

    }

    @Override
    public void pointerPressed(int x, int y) {
        addPoint(x-getParent().getAbsoluteX(), y-getParent().getAbsoluteY());
    }
}
// end::graphics-java-007[]

// tag::graphics-java-008[]
private float lastX = -1;
private float lastY = -1;

public void addPoint(float x, float y) {
    if (lastX == -1) {
        // this is the first point... Don't draw a line yet
        p.moveTo(x, y);
    } else {
        p.lineTo(x, y);
    }
    lastX = x;
    lastY = y;

    repaint();
}
// end::graphics-java-008[]

// tag::graphics-java-009[]
private boolean odd=true;
public void addPoint(float x, float y){
    if ( lastX == -1 ){
        p.moveTo(x, y);

    } else {
        float controlX = odd ? lastX : x;
        float controlY = odd ? y : lastY;
        p.quadTo(controlX, controlY, x, y);
    }
    odd = !odd;
    lastX = x;
    lastY = y;
    repaint();
}
// end::graphics-java-009[]

// tag::graphics-java-010[]
@Override
protected void paintBackground(Graphics g) {
    super.paintBackground(g);
    if ( g.isShapeSupported() ){
       // do my shape drawing code here
    } else {
        // draw an alternate representation for device
        // that doesn't support shapes.
        // E.g. You could defer to the Pisces
        // library in this case
    }

}
// end::graphics-java-010[]

// tag::graphics-java-011[]
public void paint(Graphics g){
    if ( g.isAffineSupported() ){
        // Do something that requires rotation and scaling

    } else {
        // Fallback behavior here
    }
}
// end::graphics-java-011[]

// tag::graphics-java-012[]
public class AnalogClock extends Component {
    Date currentTime = new Date();

    @Override
    public void paintBackground(Graphics g) {
        // Draw the clock in this method
    }
}
// end::graphics-java-012[]

// tag::graphics-java-013[]
// Hard code the padding at 10 pixels for now
double padding = 10;

// Clock radius
double r = Math.min(getWidth(), getHeight())/2-padding;

// Center point.
double cX = getX()+getWidth()/2;
double cY = getY()+getHeight()/2;

//Tick Styles
int tickLen = 10;  // short tick
int medTickLen = 30;  // at 5-minute intervals
int longTickLen = 50; // at the quarters
int tickColor = 0xCCCCCC;
Stroke tickStroke = new Stroke(2f, Stroke.CAP_BUTT, Stroke.JOIN_ROUND, 1f);
// end::graphics-java-013[]

// tag::graphics-java-014[]
// Draw a tick for each "second" (1 through 60)
for ( int i=1; i<= 60; i++){
    // default tick length is short
    int len = tickLen;
    if ( i % 15 == 0 ){
        // Longest tick on quarters (every 15 ticks)
        len = longTickLen;
    } else if ( i % 5 == 0 ){
        // Medium ticks on the '5's (every 5 ticks)
        len = medTickLen;
    }

    double di = (double)i; // tick num as double for easier math

    // Get the angle from 12 O'Clock to this tick (radians)
    double angleFrom12 = di/60.0*2.0*Math.PI;

    // Get the angle from 3 O'Clock to this tick
        // Note: 3 O'Clock corresponds with zero angle in unit circle
        // Makes it easier to do the math.
    double angleFrom3 = Math.PI/2.0-angleFrom12;

    // Move to the outer edge of the circle at correct position
    // for this tick.
    ticksPath.moveTo(
            (float)(cX+Math.cos(angleFrom3)*r),
            (float)(cY-Math.sin(angleFrom3)*r)
    );

    // Draw line inward along radius for length of tick mark
    ticksPath.lineTo(
            (float)(cX+Math.cos(angleFrom3)*(r-len)),
            (float)(cY-Math.sin(angleFrom3)*(r-len))
    );
}

// Draw the full shape onto the graphics context.
g.setColor(tickColor);
g.drawShape(ticksPath, tickStroke);
// end::graphics-java-014[]

// tag::graphics-java-015[]
for ( int i=1; i<=12; i++){
    // Calculate the string width and height so we can center it properly
    String numStr = ""+i;
    int charWidth = g.getFont().stringWidth(numStr);
    int charHeight = g.getFont().getHeight();

    double di = (double)i;  // number as double for easier math

    // Calculate the position along the edge of the clock where the number should
    // be drawn
     // Get the angle from 12 O'Clock to this tick (radians)
    double angleFrom12 = di/12.0*2.0*Math.PI;

    // Get the angle from 3 O'Clock to this tick
        // Note: 3 O'Clock corresponds with zero angle in unit circle
        // Makes it easier to do the math.
    double angleFrom3 = Math.PI/2.0-angleFrom12;

    // Get diff between number position and clock center
    int tx = (int)(Math.cos(angleFrom3)*(r-longTickLen));
    int ty = (int)(-Math.sin(angleFrom3)*(r-longTickLen));

    // For 6 and 12 we will shift number slightly so they're more even
    if ( i == 6 ){
        ty -= charHeight/2;
    } else if ( i == 12 ){
        ty += charHeight/2;
    }

    // Translate the graphics context by delta between clock center and
    // number position
    g.translate(
            tx,
            ty
    );


    // Draw number at clock center.
    g.drawString(numStr, (int)cX-charWidth/2, (int)cY-charHeight/2);

    // Undo translation
    g.translate(-tx, -ty);

}
// end::graphics-java-015[]

// tag::graphics-java-016[]
GeneralPath secondHand = new GeneralPath();
secondHand.moveTo((float)cX, (float)cY);
secondHand.lineTo((float)cX, (float)(cY-(r-medTickLen)));
// end::graphics-java-016[]

// tag::graphics-java-017[]
Shape translatedSecondHand = secondHand.createTransformedShape(
    Transform.makeTranslation(0f, 5)
);
// end::graphics-java-017[]

// tag::graphics-java-018[]
// Calculate the angle of the second hand
Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
double second = (double)(calendar.get(Calendar.SECOND));
double secondAngle = second/60.0*2.0*Math.PI;

// Get absolute center position of the clock
double absCX = getAbsoluteX()+cX-getX();
double absCY = getAbsoluteY()+cY-getY();

g.rotate((float)secondAngle, (int)absCX, (int)absCY);
g.setColor(0xff0000);
g.drawShape(
        translatedSecondHand,
        new Stroke(2f, Stroke.CAP_BUTT, Stroke.JOIN_BEVEL, 1f)
);
g.resetAffine();
// end::graphics-java-018[]

// tag::graphics-java-019[]
// Draw the minute hand
GeneralPath minuteHand = new GeneralPath();
minuteHand.moveTo((float)cX, (float)cY);
minuteHand.lineTo((float)cX+6, (float)cY);
minuteHand.lineTo((float)cX+2, (float)(cY-(r-tickLen)));
minuteHand.lineTo((float)cX-2, (float)(cY-(r-tickLen)));
minuteHand.lineTo((float)cX-6, (float)cY);
minuteHand.closePath();

// Translate the minute hand slightly down so it overlaps the center
Shape translatedMinuteHand = minuteHand.createTransformedShape(
    Transform.makeTranslation(0f, 5)
);

double minute = (double)(calendar.get(Calendar.MINUTE)) +
        (double)(calendar.get(Calendar.SECOND))/60.0;

double minuteAngle = minute/60.0*2.0*Math.PI;

// Rotate and draw the minute hand
g.rotate((float)minuteAngle, (int)absCX, (int)absCY);
g.setColor(0×000000);
g.fillShape(translatedMinuteHand);
g.resetAffine();


// Draw the hour hand
GeneralPath hourHand = new GeneralPath();
hourHand.moveTo((float)cX, (float)cY);
hourHand.lineTo((float)cX+4, (float)cY);
hourHand.lineTo((float)cX+1, (float)(cY-(r-longTickLen)*0.75));
hourHand.lineTo((float)cX-1, (float)(cY-(r-longTickLen)*0.75));
hourHand.lineTo((float)cX-4, (float)cY);
hourHand.closePath();

Shape translatedHourHand = hourHand.createTransformedShape(
    Transform.makeTranslation(0f, 5)
);

//Calendar cal = Calendar.getInstance().get
double hour = (double)(calendar.get(Calendar.HOUR_OF_DAY)%12) +
        (double)(calendar.get(Calendar.MINUTE))/60.0;

double angle = hour/12.0*2.0*Math.PI;
g.rotate((float)angle, (int)absCX, (int)absCY);
g.setColor(0×000000);
g.fillShape(translatedHourHand);
g.resetAffine();
// end::graphics-java-019[]

// tag::graphics-java-020[]
Date currentTime = new Date();
long lastRenderedTime = 0;

@Override
public boolean animate() {
    if ( System.currentTimeMillis()/1000 != lastRenderedTime/1000){
        currentTime.setTime(System.currentTimeMillis());
        return true;
    }
    return false;
}
// end::graphics-java-020[]

// tag::graphics-java-021[]
public void start(){
    getComponentForm().registerAnimated(this);
}

public void stop(){
    getComponentForm().deregisterAnimated(this);
}
// end::graphics-java-021[]

// tag::graphics-java-022[]
AnalogClock clock = new AnalogClock();
parent.addComponent(clock);
clock.start();
// end::graphics-java-022[]

// tag::graphics-java-023[]
Image duke = null;
try {
    // duke.png is just the default Codename One icon copied into place
    duke = Image.createImage("/duke.png");
} catch(IOException err) {
    Log.e(err);
}
final Image finalDuke = duke;

Form hi = new Form("Shape Clip");

// We create a 50 x 100 shape, this is arbitrary since we can scale it easily
GeneralPath path = new GeneralPath();
path.moveTo(20,0);
path.lineTo(30, 0);
path.lineTo(30, 100);
path.lineTo(20, 100);
path.lineTo(20, 15);
path.lineTo(5, 40);
path.lineTo(5, 25);
path.lineTo(20,0);

Stroke stroke = new Stroke(0.5f, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 4);
hi.getContentPane().getUnselectedStyle().setBgPainter((Graphics g, Rectangle rect) -> {
    g.setColor(0xff);
    float widthRatio = ((float)rect.getWidth()) / 50f;
    float heightRatio = ((float)rect.getHeight()) / 100f;
    g.scale(widthRatio, heightRatio);
    g.translate((int)(((float)rect.getX()) / widthRatio), (int)(((float)rect.getY()) / heightRatio));
    g.setClip(path);
    g.setAntiAliased(true);
    g.drawImage(finalDuke, 0, 0, 50, 100);
    g.setClip(path.getBounds());
    g.drawShape(path, stroke);
    g.translate(-(int)(((float)rect.getX()) / widthRatio), -(int)(((float)rect.getY()) / heightRatio));
    g.resetAffine();
});

hi.show();
// end::graphics-java-023[]

// tag::graphics-java-024[]
g.drawRect(10,10, 100, 100);
// end::graphics-java-024[]

// tag::graphics-java-025[]
// Find out the current translation
int currX = g.getTranslateX();
int currY = g.getTranslateY();

// Reset the translation to zeroes
g.translate(-currX, -currY);

// Now we are working in absolute screen coordinates
g.drawRect(10, 10, 100, 100);

// This rectangle should now be drawn at the exact screen
// coordinates (10,10).

//Restore the translation
g.translate(currX, currY);
// end::graphics-java-025[]

// tag::graphics-java-026[]
class RectangleComponent extends Component {
    public void paint(Graphics g){
        g.setColor(0x0000ff);
        g.drawRect(getX()+5, getY()+5, getWidth()-10, getHeight()-10);
    }
}
// end::graphics-java-026[]

// tag::graphics-java-027[]
    class RectangleComponent extends Component {

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(250,250);
        }

        public void paint(Graphics g) {
            g.setColor(0x0000ff);
            g.rotate((float) (Math.PI / 4.0));
            g.drawRect(getX() + 5, getY() + 5, getWidth() - 10, getHeight() - 10);
            g.rotate(-(float) (Math.PI / 4.0));
        }
    }


// end::graphics-java-027[]

// tag::graphics-java-028[]
    class MyForm extends Form {

        public MyForm() {
            super("Rectangle Rotations");
            for ( int i=0; i< 10; i++ ){
                this.addComponent(new RectangleComponent());
            }
        }
    }
// end::graphics-java-028[]

// tag::graphics-java-029[]
        public void paint(Graphics g) {
            g.setColor(0x0000ff);
            g.rotate((float)(Math.PI/4.0), getAbsoluteX(), getAbsoluteY());
            g.drawRect(getX() + 5, getY() + 5, getWidth() - 10, getHeight() - 10);
            g.rotate(-(float) (Math.PI / 4.0), getAbsoluteX(), getAbsoluteY());
        }
// end::graphics-java-029[]

// tag::graphics-java-030[]
public void paint(Graphics g) {
    g.setColor(0x0000ff);
    g.rotate(
        (float)(Math.PI/4.0),
        getAbsoluteX()+getWidth()/2,
        getAbsoluteY()+getHeight()/2
    );
    g.drawRect(getX() + 5, getY() + 5, getWidth() - 10, getHeight() - 10);
    g.rotate(
        -(float)(Math.PI/4.0),
        getAbsoluteX()+getWidth()/2,
        getAbsoluteY()+getHeight()/2
    );
}
// end::graphics-java-030[]

// tag::graphics-java-031[]
public void pointerPressed(int x, int y) {
    addPoint(x-getParent().getAbsoluteX(), y-getParent().getAbsoluteY());
}
// end::graphics-java-031[]

// tag::graphics-java-032[]
Form hi = new Form("Icon Font");
Font materialFont = FontImage.getMaterialDesignFont();
int w = Display.getInstance().getDisplayWidth();
FontImage fntImage = FontImage.createFixed("\uE161", materialFont, 0xff0000, w, w);
hi.add(fntImage);
hi.show();
// end::graphics-java-032[]

// tag::graphics-java-033[]
Form hi = new Form("Icon Font");
Font materialFont = FontImage.getMaterialDesignFont();
int size = Display.getInstance().convertToPixels(6, true);
materialFont = materialFont.derive(size, Font.STYLE_PLAIN);
Button myButton = new Button("Save");
myButton.setIcon(FontImage.create("\uE161", myButton.getUnselectedStyle(), materialFont));
hi.add(myButton);
hi.show();
// end::graphics-java-033[]

// tag::graphics-java-034[]
Form hi = new Form("Icon Font");
Button myButton = new Button("Save");
myButton.setIcon(FontImage.createMaterial(FontImage.MATERIAL_SAVE, myButton.getUnselectedStyle()));
hi.add(myButton);
// end::graphics-java-034[]

// tag::graphics-java-035[]
Form hi = new Form("Icon Font");
Button myButton = new Button("Save");
FontImage.setMaterialIcon(myButton, FontImage.MATERIAL_SAVE);
hi.add(myButton);
// end::graphics-java-035[]

// tag::graphics-java-036[]
Toolbar.setGlobalToolbar(true);
Form hi = new Form("Rounder", new BorderLayout());
Label picture = new Label("", "Container");
hi.add(BorderLayout.CENTER, picture);
hi.getUnselectedStyle().setBgColor(0xff0000);
hi.getUnselectedStyle().setBgTransparency(255);
Style s = UIManager.getInstance().getComponentStyle("TitleCommand");
Image camera = FontImage.createMaterial(FontImage.MATERIAL_CAMERA, s);
hi.getToolbar().addCommandToRightBar("", camera, (ev) -> {
    try {
        int width = Display.getInstance().getDisplayWidth();
        Image capturedImage = Image.createImage(Capture.capturePhoto(width, -1));
        Image roundMask = Image.createImage(width, capturedImage.getHeight(), 0xff000000);
        Graphics gr = roundMask.getGraphics();
        gr.setColor(0xffffff);
        gr.fillArc(0, 0, width, width, 0, 360);
        Object mask = roundMask.createMask();
        capturedImage = capturedImage.applyMask(mask);
        picture.setIcon(capturedImage);
        hi.revalidate();
    } catch(IOException err) {
        Log.e(err);
    }
});
// end::graphics-java-036[]

// tag::graphics-java-037[]
Image i = URLImage.createToStorage(placeholder, "fileNameInStorage", "http://xxx/myurl.jpg", URLImage.RESIZE_SCALE);
// end::graphics-java-037[]

// tag::graphics-java-038[]
public EncodedImage adaptImage(EncodedImage downloadedImage, Image placeholderImage)
// end::graphics-java-038[]

// tag::graphics-java-039[]
// Global default -- applied to every URLImage download from this point on.
// The most common case is "all our images sit behind the same bearer
// token", which has its own shorthand:
URLImage.setDefaultBearerToken(Preferences.get("auth.token", null));

// Or the explicit form, which can attach any header / cookie / timeout:
URLImage.setDefaultRequestDecorator(req ->
        req.addRequestHeader("Authorization", "Bearer " + token));
// end::graphics-java-039[]

// tag::graphics-java-040[]
URLImage profilePic = URLImage.createToStorage(
        placeholder,
        "profile-" + userId,
        baseUrl + "/users/" + userId + "/picture",
        URLImage.RESIZE_SCALE_TO_FILL,
        req -> req.addRequestHeader("X-API-Version", "2"));
// end::graphics-java-040[]

// tag::graphics-java-041[]
Image roundMask = Image.createImage(placeholder.getWidth(), placeholder.getHeight(), 0xff000000);
Graphics gr = roundMask.getGraphics();
gr.setColor(0xffffff);
gr.fillArc(0, 0, placeholder.getWidth(), placeholder.getHeight(), 0, 360);

URLImage.ImageAdapter ada = URLImage.createMaskAdapter(roundMask);
Image i = URLImage.createToStorage(placeholder, "fileNameInStorage", "http://xxx/myurl.jpg", ada);
// end::graphics-java-041[]

// tag::graphics-java-042[]
map.put("icon_URLImage", urlToActualImage);
// end::graphics-java-042[]

// tag::graphics-java-043[]
Style s = UIManager.getInstance().getComponentStyle("Button");
FontImage p = FontImage.createMaterial(FontImage.MATERIAL_PORTRAIT, s);
EncodedImage placeholder = EncodedImage.createFromImage(p.scaled(p.getWidth() * 3, p.getHeight() * 4), false);

Form hi = new Form("MultiList", new BorderLayout());

ArrayList<Map<String, Object>> data = new ArrayList<>();

data.add(createListEntry("A Game of Thrones", "1996", "http://www.georgerrmartin.com/wp-content/uploads/2013/03/GOTMTI2.jpg"));
data.add(createListEntry("A Clash Of Kings", "1998", "http://www.georgerrmartin.com/wp-content/uploads/2012/08/clashofkings.jpg"));
data.add(createListEntry("A Storm Of Swords", "2000", "http://www.georgerrmartin.com/wp-content/uploads/2013/03/stormswordsMTI.jpg"));
data.add(createListEntry("A Feast For Crows", "2005", "http://www.georgerrmartin.com/wp-content/uploads/2012/08/feastforcrows.jpg"));
data.add(createListEntry("A Dance With Dragons", "2011", "http://georgerrmartin.com/gallery/art/dragons05.jpg"));
data.add(createListEntry("The Winds of Winter", "2016 (please, please, please)", "http://www.georgerrmartin.com/wp-content/uploads/2013/03/GOTMTI2.jpg"));
data.add(createListEntry("A Dream of Spring", "Ugh", "http://www.georgerrmartin.com/wp-content/uploads/2013/03/GOTMTI2.jpg"));

DefaultListModel<Map<String, Object>> model = new DefaultListModel<>(data);
MultiList ml = new MultiList(model);
ml.getUnselectedButton().setIconName("icon_URLImage");
ml.getSelectedButton().setIconName("icon_URLImage");
ml.getUnselectedButton().setIcon(placeholder);
ml.getSelectedButton().setIcon(placeholder);
hi.add(BorderLayout.CENTER, ml);
// end::graphics-java-043[]

// tag::graphics-java-044[]
private Map<String, Object> createListEntry(String name, String date, String coverURL) {
    Map<String, Object> entry = new HashMap<>();
    entry.put("Line1", name);
    entry.put("Line2", date);
    entry.put("icon_URLImage", coverURL);
    entry.put("icon_URLImageName", name);
    return entry;
}
// end::graphics-java-044[]

// tag::graphics-java-045[]
XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
seriesRenderer.setColor(0xff0000);

XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
renderer.addSeriesRenderer(seriesRenderer);

XYSeries series = new XYSeries("Sales");
series.add(1, 42);
series.add(2, 57);

XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
dataset.addSeries(series);

BarChart chart = new BarChart(dataset, renderer, BarChart.Type.DEFAULT);
Form form = new Form(new BorderLayout());
form.add(BorderLayout.CENTER, new ChartComponent(chart));
form.show();
// end::graphics-java-045[]
