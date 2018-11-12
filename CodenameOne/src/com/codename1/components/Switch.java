package com.codename1.components;

import com.codename1.ui.Component;
import static com.codename1.ui.Component.CENTER;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;
import java.util.Collection;

/**
 * <p>
 * The on/off switch is a checkbox of sort (although it derives container) that
 * represents its state as a switch When using the Android native theme, this implementation follows the Material
 * Design Switch guidelines:
 * https://material.io/guidelines/components/selection-controls.html#selection-controls-radio-button
 * 
 * </p>
 *
 * <h3>Customizing Look and Feel</h3>
 * 
 * <p>You can customize the look and feel of a switch using styles, either directly
 * in the theme.res file or in CSS.  This component consists of two elements: the "thumb" the "track".
 * The "thumb" is the circular handle that can be toggled/dragged between an on and off position.  
 * The "track" is the background track along which the "thumb" appears to slide when it is dragged/toggled.
 * 
 * <p>The thumb will be rendered using Switch's {@link Style#getFgColor() }.  It will use the 
 * selected style, when in the "on" position, the unselected style when in the "off" position,
 * and the disabled style, when {@link #isEnabled() } is {@literal false}.</p>
 * 
 * <p>The track will be rendered using the Switch's {@link Style#getBgColor() }.  It will
 * use the selected style, when in the "on" position, and the unselected style when in the "off"
 * position.</p>
 * 
 * <p>You can also adjust the thumb and track sizes using the following theme constants:</p>
 * 
 * <table>
 * <tr><td>{@literal switchThumbScaleY}</td><td>A floating point value used to scale the thumb's diameter, relative to the font size.  In the android native theme, this value is set to 1.5.  On iOS, it is set to 1.4.</td></tr>
 * <tr><td>{@literal switchTrackScaleY}</td><td>A floating point value used to scale the track's height, relative the the font size.  In the android native theme, this value is set to 0.9.  On iOS, it is set to 1.5</td></tr>
 * <tr><td>{@literal switchTrackScaleX}</td><td>A floating point value used to scale the track's width relative to the font size.  In the Android native theme, this value is set to 3.0.  On iOS it is 2.5</td></tr>
 * <tr><td>{@literal switchTrackOffOutlineWidthMM}</td><td>A floating point value used to set the stroke/outline thickness of the track when the switch is in the "off" position.  In the Android native theme, this is set to 0.  On iOS it is 0.25</td></tr>
 * <tr><td>{@literal switchTrackOnOutlineWidthMM}</td><td>A floating point value used to set the stroke/outline thickness of the track when the switch is in the "on" position.  In both the Android native theme and the iOS native theme, this is set to 0.</td></tr>
 * <tr><td>{@literal switchTrackOffOutlineColor}</td><td>The color used to stroke the outline for the track when the switch is in the "off" position, expressed as a base-16 int.  In the iOS native theme, this is set to "cccccc".</td></tr>
 * <tr><td>{@literal switchTrackOnOutlineColor}</td><td>The color used to stroke the outline for the track when the switch is in the "on" position, expressed as a base-16 int.  This is currently not used in either Android or iOS.</td></tr>
 * <tr><td>{@literal switchThumbInsetMM}</td><td>An inset to use when rendering the thumb that will cause it to be inset from the edge of the track. In the iOS native theme, this is 0.25</td></tr>
 * </table>
 * 
 * <p><strong>CSS used in the Android native theme:</strong></p>
 * <pre>{@code
#Constants {
  ...
  switchThumbPaddingInt: 2;
  switchThumbScaleY: "1.5";
  switchTrackScaleY: "0.9";
  switchTrackScaleX: "3";
  switchTrackOffOutlineWidthMM: "0";
  switchTrackOnOutlineWidthMM: "0";
  switchTrackOffOutlineColor: "cccccc";
  switchThumbInsetMM: "0";
}
Switch {
  color: rgb(237, 237, 237);
  background-color: rgb(159, 158, 158);

}
Switch.selected {
    color: rgb(34,44,50);
    background-color: rgb(117, 126, 132);
}
}</pre>
* 
* <p><strong>CSS used in the iOS native theme:</strong></p>
* <pre>{@code 
#Constants {
  ...
  switchThumbPaddingInt: 2;
    switchThumbScaleY: "1.4";
    switchTrackScaleY: "1.5";
    switchTrackScaleX: "2.5";
    switchTrackOffOutlineWidthMM: "0.25";
    switchTrackOnOutlineWidthMM: "0";
    switchTrackOffOutlineColor: "cccccc";
    switchThumbInsetMM: "0.25";
}
Switch {
    color: white;
    background-color: white;
    
}
Switch.selected {
    color: white;
    background-color: rgb(61, 216, 76);
    
}
}
</pre>
* <h4>Using Custom Thumb and Track Images</h4>
* <p>You can optionally provide custom images for use as the track or thumb via the following
* theme constants</p>
* <ul>
*   <li>{@literal switchThumbOnImage}</li>
*   <li>{@literal switchThumbOffImage}</li>
*   <li>{@literal switchThumbDisabledImage}</li>
*   <li>{@literal switchOnTrackImage}</li>
*   <li>{@literal switchOffTrackImage}</li>
*   <li>{@literal switchDisabledTrackImage}</li>
* </ul>
 */
public class Switch extends Component {

    private boolean value;
    private Image thumbOnImage;
    private Image thumbOffImage;
    private Image thumbDisabledImage;
    private Image trackOnImage;
    private Image trackOffImage;
    private Image trackDisabledImage;
    private boolean dragged;
    private int pressX;
    private int deltaX; //pressX - currentdragX
    private final EventDispatcher dispatcher = new EventDispatcher();
    private final EventDispatcher changeDispatcher = new EventDispatcher();
    private boolean animationLock;

    private int valign = CENTER;
    /**
     * Default constructor
     */
    public Switch() {
        setUIID("Switch");
        setOpaque(false);
        initialize();
    }

    private int getFontSize() {
        Font f = getUnselectedStyle().getFont();
        if (f == null) {
            f = Font.getDefaultFont();
        }
        return f.getHeight();
    }
    
    private Image getThumbOnImage() {
        if (thumbOnImage == null) {
            thumbOnImage = createPlatformThumbImage((int) (getFontSize() * getThumbScaleY()), getSelectedStyle().getFgColor(), 2, getThumbInset());
        }
        return thumbOnImage;
    }
    
    private Image getThumbOffImage() {
        if (thumbOffImage == null) {
            thumbOffImage = createPlatformThumbImage((int) (getFontSize() * getThumbScaleY()), getUnselectedStyle().getFgColor(), 2, getThumbInset()); //getUnselectedStyle().getFgColor(), true);
        }
        return thumbOffImage;
    }
    
    private Image getThumbDisabledImage() {
        if (thumbDisabledImage == null) {
            thumbDisabledImage = createPlatformThumbImage((int) (getFontSize() * getThumbScaleY()), getDisabledStyle().getFgColor(), 2, getThumbInset()); //getDisabledStyle().getFgColor(), true);
        }
        return thumbDisabledImage;
    }

    private Image getCurrentThumbImage() {
        if (isEnabled()) {
            if (value) {
                return getThumbOnImage();
            } else {
                return getThumbOffImage();
            }
        } else {
            return getThumbDisabledImage();
        }
    }

    private double getTrackScaleY() {
        return Double.parseDouble(getUIManager().getThemeConstant("switchTrackScaleY", "0.9"));
    }
    
    private double getThumbScaleY() {
        return Double.parseDouble(getUIManager().getThemeConstant("switchThumbScaleY", "1.5"));
    }
    
    private double getTrackScaleX() {
        return Double.parseDouble(getUIManager().getThemeConstant("switchTrackScaleX", "3"));
    }
    
    private int getTrackOnOutlineWidth() {
        return Display.getInstance().convertToPixels(Float.parseFloat(getUIManager().getThemeConstant("switchTrackOnOutlineWidthMM", "0")));
    }
    
    private int getTrackOnOutlineColor() {
        return Integer.parseInt(getUIManager().getThemeConstant("switchTrackOnOutlineColor", "0"), 16);
    }
    
    private int getTrackOffOutlineWidth() {
        return Display.getInstance().convertToPixels(Float.parseFloat(getUIManager().getThemeConstant("switchTrackOffOutlineWidthMM", "0")));
    }
    
    private int getTrackOffOutlineColor() {
        return Integer.parseInt(getUIManager().getThemeConstant("switchTrackOffOutlineColor", "0"), 16);
    }
    
    private int getThumbInset() {
        return Display.getInstance().convertToPixels(Float.parseFloat(getUIManager().getThemeConstant("switchThumbInsetMM", "0")));
    }
    
    private Image getTrackOnImage() {
        if (trackOnImage == null) {
            trackOnImage = createPlatformTrackImage((int) (getFontSize() * getTrackScaleX()), (int) (getFontSize() * getTrackScaleY()), getSelectedStyle().getBgColor(), 255, 2, getTrackOnOutlineColor(), getTrackOnOutlineWidth());
        }
        return trackOnImage;
    }
    
    private Image getTrackDisabledImage() {
        if (trackDisabledImage == null) {
            trackDisabledImage = createPlatformTrackImage((int) (getFontSize() * getTrackScaleX()), (int) (getFontSize() * getTrackScaleY()), getDisabledStyle().getBgColor(), 255, 2, getTrackOffOutlineColor(), getTrackOffOutlineWidth());
        }
        return trackDisabledImage;
    }
    
    private Image getCurrentTrackOnImage() {
        if (isEnabled()) {
            return getTrackOnImage();
        } else {
            return getTrackDisabledImage();
        }
    }
    
    private Image getTrackOffImage() {
        if (trackOffImage == null) {
            trackOffImage = createPlatformTrackImage((int) (getFontSize() * getTrackScaleX()), (int) (getFontSize() * getTrackScaleY()), getUnselectedStyle().getBgColor(), 255, 2, getTrackOffOutlineColor(), getTrackOffOutlineWidth());
        }
        return trackOffImage;
    }

    private Image getCurrentTrackOffImage() {
        if (isEnabled()) {
            return getTrackOffImage();
        } else {
            return getTrackDisabledImage();
        }
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void styleChanged(String propertyName, Style source) {
        if (Style.FG_COLOR.equals(propertyName) || Style.BG_COLOR.equals(propertyName) || Style.FONT.equals(propertyName)) {
            initTheme();
        }
        super.styleChanged(propertyName, source);
    }

    
    
    /**
     * {@inheritDoc}
     */
    protected Dimension calcPreferredSize() {
        return new Dimension(
                    getStyle().getHorizontalPadding() +Math.max(getCurrentThumbImage().getWidth(), Math.max(getCurrentTrackOnImage().getWidth(), getCurrentTrackOffImage().getWidth())),
                    getStyle().getVerticalPadding() + Math.max(getCurrentThumbImage().getHeight(), Math.max(getCurrentTrackOnImage().getHeight(), getCurrentTrackOffImage().getHeight())));
        
    }

    /**
     * {@inheritDoc}
     */
    protected void resetFocusable() {
        setFocusable(true);
    }

    private void initialize() {
        setFocusable(true);
        initTheme();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        initTheme();
        /*
        if(sliderFull != null) {
            deinitializeCustomStyle(sliderFull);
            deinitializeCustomStyle(sliderFullSelected);
            initStyles();
        }
         */
    }

    private void initTheme() {
        thumbOffImage = null;
        thumbOnImage = null;
        thumbDisabledImage = null;
        trackOnImage = null;
        trackOffImage = null;
        trackDisabledImage = null;
        setThumbOnImage(UIManager.getInstance().getThemeImageConstant("switchThumbOnImage"));
        setThumbOffImage(UIManager.getInstance().getThemeImageConstant("switchThumbOffImage"));
        setThumbDisabledImage(UIManager.getInstance().getThemeImageConstant("switchThumbDisabledImage"));
        setTrackOnImage(UIManager.getInstance().getThemeImageConstant("switchOnTrackImage"));
        setTrackOffImage(UIManager.getInstance().getThemeImageConstant("switchOffTrackImage"));
        setTrackDisabledImage(UIManager.getInstance().getThemeImageConstant("switchDisabledTrackImage"));
    }

    private static Image createRoundThumbImage(int pxDim, int color, int shadowSpread, int thumbInset) {
        Image img = Image.createImage(pxDim + 2 * shadowSpread, pxDim + 2 * shadowSpread, 0x0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);

        int shadowOpacity = 200;
        float shadowBlur = 10;

        if (shadowSpread > 0) {
            // draw a gradient of sort for the shadow
            for (int iter = shadowSpread - 1; iter >= 0; iter--) {
                g.translate(iter, iter);
                g.setColor(0);
                g.setAlpha(shadowOpacity / shadowSpread);
                g.fillArc(
                        Math.max(1, thumbInset+shadowSpread + shadowSpread / 2 - iter), 
                        Math.max(1, thumbInset + 2 * shadowSpread - iter), 
                        Math.max(1, pxDim - (iter * 2) - 2*thumbInset), 
                        Math.max(1, pxDim - (iter * 2) - 2*thumbInset), 0, 360);
                g.translate(-iter, -iter);
            }
            if (Display.getInstance().isGaussianBlurSupported()) {
                Image blured = Display.getInstance().gaussianBlurImage(img, shadowBlur / 2);
                //img = Image.createImage(pxDim+2*shadowSpread, pxDim+2*shadowSpread, 0);
                img = blured;
                g = img.getGraphics();
                //g.drawImage(blured, 0, 0);
                g.setAntiAliased(true);
            }
        }

        //g.translate(shadowSpread, shadowSpread);
        g.setAlpha(255);
        g.setColor(color);
        g.fillArc(shadowSpread+thumbInset, shadowSpread+thumbInset, Math.max(1, pxDim-2*thumbInset), Math.max(1, pxDim-2*thumbInset), 0, 360);
        //g.setColor(outlinecolor);
        //g.drawArc(shadowSize, shadowSize, pxDim-1, pxDim-1, 0, 360);
        return img;
    }
    
    private static Image createPlatformThumbImage(int pxDim, int color, int shadowSpread, int thumbInset) {
        return createRoundThumbImage(pxDim, color, shadowSpread, thumbInset);
    }

    private static Image createRoundRectTrackImage(int width, int height, int color, int alpha, int thumbPadding, int outlineColor, int outlineWidth) {
        Image img = Image.createImage(width + 2 * thumbPadding, height, 0x0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setAlpha(alpha);
        int topPadding=0;
        if (outlineWidth > 0) {
            g.setColor(outlineColor);
            g.fillRoundRect(thumbPadding, 0, width, height, height, height);
            thumbPadding += outlineWidth;
            topPadding += outlineWidth;
            width -= 2*outlineWidth;
            height -= 2*outlineWidth;
        }
        thumbPadding = Math.max(0, thumbPadding);
        topPadding = Math.max(0, topPadding);
        width = Math.max(2, width);
        height = Math.max(2, height);
        g.setColor(color);
        g.fillRoundRect(thumbPadding, topPadding, width, height, height, height);
        return img;
    }
    
    private static Image createPlatformTrackImage(int width, int height, int color, int alpha, int thumbPadding, int outlineColor, int outlineWidth) {
        return createRoundRectTrackImage(width, height, color, alpha, thumbPadding, outlineColor, outlineWidth);
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isStickyDrag() {
        return true;
    }

    /**
     * Adds a listener to the switch which will cause an event to dispatch on
     * click
     *
     * @param l implementation of the action listener interface
     */
    public void addActionListener(ActionListener l) {
        dispatcher.addListener(l);
    }

    /**
     * Removes the given action listener from the switch
     *
     * @param l implementation of the action listener interface
     */
    public void removeActionListener(ActionListener l) {
        dispatcher.removeListener(l);
    }
    
    /**
     * Adds a listener to the switch which will cause an event on change
     *
     * @param l implementation of the action listener interface
     */
    public void addChangeListener(ActionListener l) {
        changeDispatcher.addListener(l);
    }

    /**
     * Removes the given change listener from the switch
     *
     * @param l implementation of the action listener interface
     */
    public void removeChangeListener(ActionListener l) {
        changeDispatcher.removeListener(l);
    }

    /**
     * Returns a collection containing the action listeners for this button
     *
     * @return the action listeners
     * @deprecated This will be removed in a future version.
     */
    public Collection getListeners() {
        return dispatcher.getListenerCollection();
    }

    void fireActionEvent() {
        dispatcher.fireActionEvent(new ActionEvent(this, ActionEvent.Type.PointerPressed));
        Display d = Display.getInstance();
        if (d.isBuiltinSoundsEnabled()) {
            d.playBuiltinSound(Display.SOUND_TYPE_BUTTON_PRESS);
        }
    }
    
    void fireChangeEvent() {
        changeDispatcher.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Data));
    }

    /**
     * {@inheritDoc}
     */
    public void paint(Graphics g) {

        Image cthumbImage = getCurrentThumbImage();
        Image cTrackImage = value ? getCurrentTrackOnImage() : getCurrentTrackOffImage();
        //Image ctrackOnImage = getCurrentTrackOnImage();
        //Image ctrackOffImage = getCurrentTrackOffImage();
        int strackLength = Math.max(cTrackImage.getWidth(), cTrackImage.getWidth());
        int sheight = Math.max(cthumbImage.getHeight(), Math.max(cTrackImage.getHeight(), cTrackImage.getHeight()));

        int vdeltaX = -deltaX; //virtual increase in slider "value" where OFF state = 0 and ON state = itrackLength
        if (isRTL()) {
            vdeltaX = deltaX;
        }

        Style s = getStyle();
        int padLeft = s.getPaddingLeft(isRTL());//s.getPaddingLeftNoRTL();
        int padRight = s.getPaddingRight(isRTL());
        int padTop = s.getPaddingTop();
        int padBot = s.getPaddingBottom();
        int innerHeight = getHeight() - padTop - padBot; //cache it to avoid calling getPadding multiple times, which costs CPU as it make the unit conversion at each call...
        int innerWidth = getWidth() - padLeft - padRight;
        int halign = s.getAlignment(); //TODO: swap left and right if RTL

        int thumbrX = 0; //X position of the thumb relative to the start of the track
        if (isRTL()) {
            if (!value) {
                thumbrX = cTrackImage.getWidth() - cthumbImage.getWidth();
            }
        } else {
            if (value) {
                thumbrX = cTrackImage.getWidth() - cthumbImage.getWidth();
            }
        }

        Image nextThumbImage = null;
        Image nextTrackImage = null;
        double nextImageProgress = 0.0;
        
        if (value) { //switch is ON so only drag movements going to the OFF position are relevant, meaning a vdelta < 0
            if (vdeltaX > 0) {
                dragged = false;
            } else {
                nextThumbImage = getThumbOffImage();
                nextTrackImage = getTrackOffImage();
                int trackMLength = cTrackImage.getWidth() - cthumbImage.getWidth();
                if (vdeltaX < -trackMLength) {
                    vdeltaX = -trackMLength;
                }
                nextImageProgress = Math.abs(vdeltaX)/(double)Math.abs(trackMLength);
                
                
            }
        } else { //switch is OFF so only consider drag movements toward the ON position, meaning a vdelta > 0
            if (vdeltaX < 0) {
                dragged = false;
            } else {
                nextThumbImage = getThumbOnImage();
                nextTrackImage = getTrackOnImage();
                int trackMLength = cTrackImage.getWidth() - cthumbImage.getWidth();
                if (vdeltaX > trackMLength) {
                    vdeltaX = trackMLength;
                }
                nextImageProgress = 1-Math.abs(trackMLength-vdeltaX)/(double)Math.abs(trackMLength);
            }
        }
        if (dragged) {
            int thumbCenterrX;
            if (value) { //vdelta is negative
                if (isRTL()) {
                    thumbCenterrX = -vdeltaX + cthumbImage.getWidth() / 2;
                } else {
                    thumbCenterrX = cTrackImage.getWidth() + vdeltaX - cthumbImage.getWidth() / 2;
                }
            } else { //positive vdelta	
                if (isRTL()) {
                    thumbCenterrX = cTrackImage.getWidth() - vdeltaX - cthumbImage.getWidth() / 2;
                } else {
                    thumbCenterrX = vdeltaX + cthumbImage.getWidth() / 2;
                }
            }

            thumbrX = thumbCenterrX - cthumbImage.getWidth() / 2;

            
            int imgY = getY() + padTop + getAlignedCoord((sheight / 2 - cTrackImage.getHeight() / 2), innerHeight, sheight, valign);
            int imgX = getX() + padLeft + getAlignedCoord(0, innerWidth, strackLength, halign);

            int alph = g.getAlpha();
            if (nextImageProgress > 0 && nextTrackImage != null) {
                g.setAlpha((int)Math.round((1-nextImageProgress)*255));
            }
            g.drawImage(cTrackImage, imgX, imgY);
            if (nextImageProgress > 0 && nextTrackImage != null) {
                g.setAlpha((int)Math.round(nextImageProgress*255));
                g.drawImage(nextTrackImage, imgX, imgY);
                g.setAlpha(alph);
            }

        } else {
            int imgX = getX() + padLeft + getAlignedCoord(0, innerWidth, strackLength, halign);
            int imgY = getY() + padTop + getAlignedCoord((sheight / 2 - cTrackImage.getHeight() / 2), innerHeight, sheight, valign);
            g.drawImage(cTrackImage, imgX, imgY);
            
        }

        //draw the thumb image
        int alph = g.getAlpha();
        if (nextImageProgress > 0 && nextThumbImage != null) {
            g.setAlpha((int)Math.round((1-nextImageProgress)*255));
        }
        g.drawImage(cthumbImage,
                getX() + padLeft + getAlignedCoord(thumbrX, innerWidth, strackLength, halign),
                getY() + padTop + getAlignedCoord((sheight / 2 - cthumbImage.getHeight() / 2), innerHeight, sheight, valign));
        if (nextImageProgress > 0 && nextThumbImage != null) {
            g.setAlpha((int)Math.round(nextImageProgress*255));
            g.drawImage(nextThumbImage,
                getX() + padLeft + getAlignedCoord(thumbrX, innerWidth, strackLength, halign),
                getY() + padTop + getAlignedCoord((sheight / 2 - cthumbImage.getHeight() / 2), innerHeight, sheight, valign));
            g.setAlpha(alph);
        }

    }

    private static int getAlignedCoord(int coord, int parentDim, int elemDim, int alignment) {
        switch (alignment) {
            case Component.CENTER:
                return coord + parentDim / 2 - elemDim / 2;
            case Component.RIGHT:
            case Component.BOTTOM:
                return coord + parentDim - elemDim;
            //case Component.LEFT: case Component.TOP:
            //	return coord;
            default:
                return coord;
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void initComponent() {
        super.initComponent();
    }

    /**
     * {@inheritDoc}
     */
    protected void deinitialize() {
        super.deinitialize();
    }

    /**
     * {@inheritDoc}
     */
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        pressX = x;
    }

    /**
     * {@inheritDoc}
     */
    public void pointerDragged(int x, int y) {
        dragged = true;
        deltaX = pressX - x;
    }

    /**
     * {@inheritDoc}
     */
    public void pointerReleased(int x, int y) {
        if (animationLock) {
            return;
        }
        animationLock = true;

        if (dragged) {
            if (deltaX > 0) { //dragged from RtL
                int trackLength = 0;
                if (isRTL()) {
                    trackLength = getCurrentTrackOffImage().getWidth() - getCurrentThumbImage().getWidth();
                } else {
                    trackLength = getCurrentTrackOnImage().getWidth() - getCurrentThumbImage().getWidth();
                }
                if (deltaX > trackLength / 2) {
                    animateTo(isRTL(), deltaX, trackLength, trackLength);
                } else { //Not moved enaugh, go back to the state where we come from (i.e. ON for LtR switch and OFF for RtL ones)
                    animateTo(!isRTL(), deltaX, 0, trackLength);
                }
            } else { //dragged from LtR
                int trackLength = 0;
                if (isRTL()) {
                    trackLength = getCurrentTrackOnImage().getWidth() - getCurrentThumbImage().getWidth();
                } else {
                    trackLength = getCurrentTrackOffImage().getWidth() - getCurrentThumbImage().getWidth();
                }
                if (deltaX * -1 > trackLength / 2) {
                    animateTo(!isRTL(), deltaX, -trackLength, trackLength);
                } else {
                    animateTo(isRTL(), deltaX, 0, trackLength);
                }
            }
        } else {
            if (value) {
                int trackLength = 0;
                if (isRTL()) {
                    trackLength = getCurrentTrackOnImage().getWidth() - getCurrentThumbImage().getWidth();
                } else {
                    trackLength = getCurrentTrackOffImage().getWidth() - getCurrentThumbImage().getWidth();
                }
                animateTo(false, 0, trackLength, trackLength);
            } else {
                int trackLength = 0;
                if (isRTL()) {
                    trackLength = getCurrentTrackOffImage().getWidth() - getCurrentThumbImage().getWidth();
                } else {
                    trackLength = getCurrentTrackOnImage().getWidth() - getCurrentThumbImage().getWidth();
                }
                animateTo(true, 0, -trackLength, trackLength);
            }

        }
        animationLock = false;
        return;
    }

    private void animateTo(final boolean value, final int deltaStart, final int deltaEnd, final int maxMoveDist) {
        int anim_duration = (int) Math.abs((deltaEnd - deltaStart) / (double) maxMoveDist * 100.0);
        final Motion current = Motion.createEaseInOutMotion(deltaStart, deltaEnd, anim_duration);
        if (anim_duration > 0) {
            current.start();
            deltaX = deltaStart;
            getComponentForm().registerAnimated(new Animation() {
                public boolean animate() {
                    deltaX = current.getValue();
                    dragged = true;
                    if (current.isFinished()) {
                        dragged = false;
                        Form f = getComponentForm();
                        if (f != null) {
                            f.deregisterAnimated(this);
                        }
                        Switch.this.setValue(value, true);
                    }
                    repaint();
                    return false;
                }

                public void paint(Graphics g) {
                }
            });
        } else {
            deltaX = deltaEnd;
            setValue(value, true);
        }
        dragged = true;
    }

    /**
     * The value of the switch
     *
     * @return the value
     */
    public boolean isValue() {
        return value;
    }

    /**
     * The value of the switch
     *
     * @param value the value to set
     */
    public void setValue(boolean value) {
        setValue(value, false);
    }
    
    private void setValue(boolean value, boolean fireEvent) {
        boolean orig = animationLock;
        animationLock = true;
        boolean oldValue = this.value;
        
        this.value = value;
        if (oldValue != value) {
            fireChangeEvent();
        }
        if (fireEvent && oldValue != value) {
            fireActionEvent();
        }
        repaint();
        animationLock = orig;
    }

    private void flip() {
        setValue(!value);
    }

    /**
     * Checks if switch is in the "on" position.
     * @return True if switch is on.
     */
    public boolean isOn() {
        return value;
    }

    /**
     * Sets the switch to the "on" position.
     */
    public void setOn() {
        setValue(true);
    }

    /**
     * Checks if the switch is in the "off" position.
     * @return True of switch is currently off.
     */
    public boolean isOff() {
        return !value;
    }

    /**
     * Switches the switch to the "off" position.
     */
    public void setOff() {
        setValue(false);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[]{
            "value",};
    }

    /**
     * Some components may optionally generate a state which can then be
     * restored using setCompnentState(). This method is used by the UIBuilder.
     *
     * @return the component state or null for undefined state.
     */
    public Object getComponentState() {
        if (value) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * If getComponentState returned a value the setter can update the value and
     * restore the prior state.
     *
     * @param state the non-null state
     */
    public void setComponentState(Object state) {
        value = ((Boolean) state).booleanValue();
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
        return new Class[]{
            Boolean.class,};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if (name.equals("value")) {
            if (value) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if (name.equals("value")) {
            setValue(((Boolean) value).booleanValue());
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    

    private void setThumbOnImage(Image image) {
        if (image != null) {
            this.thumbOnImage = image;
            //also set other thumb images if we don't have images for them yet
            if (this.thumbOffImage == null) {
                this.thumbOffImage = image;
            }
            if (this.thumbDisabledImage == null) {
                this.thumbDisabledImage = image;
            }
            setShouldCalcPreferredSize(true);
        }
    }

    

    private void setThumbOffImage(Image image) {
        if (image != null) {
            this.thumbOffImage = image;
            //also set other thumb images if we don't have images for them yet
            if (this.thumbOnImage == null) {
                this.thumbOnImage = image;
            }
            if (this.thumbDisabledImage == null) {
                this.thumbDisabledImage = image;
            }
            setShouldCalcPreferredSize(true);
        }
    }


    private void setThumbDisabledImage(Image image) {
        if (image != null) {
            this.thumbDisabledImage = image;
            //also set other thumb images if we don't have images for them yet
            if (this.thumbOnImage == null) {
                this.thumbOnImage = image;
            }
            if (this.thumbDisabledImage == null) {
                this.thumbDisabledImage = image;
            }
            setShouldCalcPreferredSize(true);
        }
    }

   

    private void setTrackOnImage(Image image) {
        if (image != null) {
            this.trackOnImage = image;
            if (this.trackOffImage == null) {
                this.trackOffImage = image;
            }
            if (this.trackDisabledImage == null) {
                this.trackDisabledImage = image;
            }
            setShouldCalcPreferredSize(true);
        }
    }

    

    /**
     * Sets the image to use for the track when the switch is set to "off".
     * Use the "switchOffTrackImage" theme constant as a default value.
     * @param image The image to use.
     */
    private void setTrackOffImage(Image image) {
        if (image != null) {
            this.trackOffImage = image;
            if (this.trackOnImage == null) {
                this.trackOnImage = image;
            }
            if (this.trackDisabledImage == null) {
                this.trackDisabledImage = image;
            }
            setShouldCalcPreferredSize(true);
        }
    }


    /**
     * Sets the image to be used for track when the component is disabled.
     * Use the "switchDisabledTrackImage" theme constant as a default value.
     * @param image 
     */
    private void setTrackDisabledImage(Image image) {
        if (image != null) {
            this.trackDisabledImage = image;
            if (this.trackOnImage == null) {
                this.trackOnImage = image;
            }
            if (this.trackOffImage == null) {
                this.trackOffImage = image;
            }
            setShouldCalcPreferredSize(true);
        }
    }

}
