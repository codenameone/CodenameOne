/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui.plaf;

import com.codename1.charts.util.ColorUtil;
import com.codename1.io.Log;
import com.codename1.io.Util;
import static com.codename1.ui.CN.convertToPixels;
import com.codename1.ui.Component;
import static com.codename1.ui.Component.BOTTOM;
import static com.codename1.ui.Component.LEFT;
import static com.codename1.ui.Component.RIGHT;
import static com.codename1.ui.Component.TOP;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle2D;
import com.codename1.ui.util.Resources;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.codename1.compat.java.util.Objects;

/**
 * <p>A border that can be configured using a limited subset of CSS directives.  This 
 * class is designed as a stop-gap to deal with common CSS style patterns that aren't 
 * well-covered by existing borders.  As time goes on this class will be enhanced to 
 * support more CSS styles.  At present, it is used by the CSS compiler for compound borders.
 * E.g. If one side has a different border style, color, or thickness than other sides.</p>
 * 
 * <p>The follow types of borders are well-supported with this class:</p>
 * 
 * <ul>
 * <li>border-radius - can support different x and y radii for each corner.</li>
 * <li>border-width - can support different widths for each side.</li>
 * <li>border-color - can support different colors for each side</li>
 * <li>background-color</li>
 * </ul>
 * 
 * <p>This class also supports background images and gradients, but these are not well-tested
 * and are not currently used by the CSS compiler.</p>
 * 
 * @since 7.0
 * @author shannah
 */
public class CSSBorder extends Border {
    /**
     * Constant indicating no-repeat for background images.
     */
    public static final byte REPEAT_NONE=0;
    
    /**
     * Constant indicating repeating on both x and y for background images.
     */
    public static final byte REPEAT_BOTH=1;
    
    /**
     * Constant indicating repeat-x for background images.
     */
    public static final byte REPEAT_X=2;
    
    /**
     * Constant indicating repeat-y for background images.
     */
    public static final byte REPEAT_Y=3;
    
    /**
     * Constant indicating background-position top.
     */
    public static final byte VPOSITION_TOP=0;
    
    /**
     * Constant indicating background-position bottom.
     */
    public static final byte VPOSITION_BOTTOM=1;
    
    /**
     * Constant indicating background-position center.
     */
    public static final byte VPOSITION_CENTER=2;
    public static final byte VPOSITION_OTHER=99;
    
    /**
     * Constant indicating background-position left.
     */
    public static final byte HPOSITION_LEFT=0;
    
    /**
     * Constant indicating background-position right.
     */
    public static final byte HPOSITION_RIGHT=1;
    
    /**
     * Constant indicating background-position center (horizontal).
     */
    public static final byte HPOSITION_CENTER=2;
    public static final byte HPOSITION_OTHER=99;
    
    
    
    public static final byte SIZE_AUTO=0;
    public static final byte SIZE_CONTAIN=1;
    public static final byte SIZE_COVER=2;
    public static final byte SIZE_OTHER=99;
    
    /**
     * Constant for border-style none
     */
    public static final byte STYLE_NONE=0;
    
    /**
     * Constant for border-style hidden
     */
    public static final byte STYLE_HIDDEN=1;
    
    /**
     * Constant for border-style dotted
     */
    public static final byte STYLE_DOTTED=2;
    
    
    /**
     * Constant for border-style dashed
     */
    public static final byte STYLE_DASHED=3;
    
    /**
     * Constant for border-style solid
     */
    public static final byte STYLE_SOLID=4;
    
    private static interface Decorator {
        public CSSBorder decorate(CSSBorder border, String cssProperty, String cssPropertyValue);
    }
    
    private static final Map<String,Decorator> decorators = new HashMap<String,Decorator>();
    static {
        decorators.put("background-color", new Decorator() {
           public CSSBorder decorate(CSSBorder border, String cssProperty, String cssPropertyValue) {
               return border.backgroundColor(cssPropertyValue);
           }
        });
        decorators.put("background-image", new Decorator() {
           public CSSBorder decorate(CSSBorder border, String cssProperty, String cssPropertyValue) {
               return border.backgroundImage(cssPropertyValue);
           }
        });
        decorators.put("background-position", new Decorator() {
           public CSSBorder decorate(CSSBorder border, String cssProperty, String cssPropertyValue) {
               return border.backgroundPosition(cssPropertyValue);
           }
        });
        decorators.put("background-repeat", new Decorator() {
           public CSSBorder decorate(CSSBorder border, String cssProperty, String cssPropertyValue) {
               return border.backgroundRepeat(cssPropertyValue);
           }
        });
        decorators.put("border-color", new Decorator() {
           public CSSBorder decorate(CSSBorder border, String cssProperty, String cssPropertyValue) {
               return border.borderColor(cssPropertyValue);
           }
        });
        decorators.put("border-radius", new Decorator() {
           public CSSBorder decorate(CSSBorder border, String cssProperty, String cssPropertyValue) {
               return border.borderRadius(cssPropertyValue);
           }
        });
        decorators.put("border-stroke", new Decorator() {
           public CSSBorder decorate(CSSBorder border, String cssProperty, String cssPropertyValue) {
               return border.borderStroke(cssPropertyValue);
           }
        });
        decorators.put("border-style", new Decorator() {
           public CSSBorder decorate(CSSBorder border, String cssProperty, String cssPropertyValue) {
               return border.borderStyle(cssPropertyValue);
           }
        });
        decorators.put("border-width", new Decorator() {
           public CSSBorder decorate(CSSBorder border, String cssProperty, String cssPropertyValue) {
               return border.borderWidth(cssPropertyValue);
           }
        });
        decorators.put("border-image", new Decorator() {
           public CSSBorder decorate(CSSBorder border, String cssProperty, String cssPropertyValue) {
               return border.borderImage(cssPropertyValue);
           }
        });
    }
    
    
    
    
    
    private Color backgroundColor;
    private BackgroundImage[] backgroundImages;
    private BorderImage borderImage;
    private BorderStroke[] stroke;
    private BoxShadow boxShadow;
    private BorderRadius borderRadius;
    private Resources res;
    
    /**
     * Constant for unit px
     */
    public static final byte UNIT_PIXELS=0;
    
    /**
     * Constant for unit mm
     */
    public static final byte UNIT_MM=2;
    
    /**
     * Constant for unit %
     */
    public static final byte UNIT_PERCENT=1;
    
    /**
     * Constant for unit em
     */
    public static final byte UNIT_EM=4;
    
    private void setAlpha(Graphics g, Color c) {
        g.setAlpha(c == null ? 0 : c.alpha);
    }
    
    /**
     * Creates a new empty CSS border.
     */
    public CSSBorder() {
        res = Resources.getGlobalResources();
    }
    
    /**
     * Creates an empty border.
     * @param res Theme resource file from which images can be referenced.
     */
    public CSSBorder(Resources res) {
        this.res = res;
    }
    
    
    /**
     * Creates a new CSS border with the provided CSS styles.  This currenlty only supports a subset of CSS.  The following
     * properties are currently supported:
     * 
     * <p>
     * <ul>
     * <li>background-color</li>
     * <li>background-image</li>
     * <li>background-position</li>
     * <li>background-repeat</li>
     * <li>border-color</li>
     * <li>border-radius</li>
     * <li>border-stroke</li>
     * <li>border-style</li>
     * <li>border-width</li>
     * <li>border-image</li>
     * </ul>
     * </p>
     * @param css CSS to parse.
     * @throws IllegalArgumentException If it fails to parse the style.
     */
    public CSSBorder(String css) {
        this(Resources.getGlobalResources(), css);
        
    }
    
    /**
     * Creates a new CSS border with the provided CSS styles.  This currenlty only supports a subset of CSS.  The following
     * properties are currently supported:
     * 
     * <p>
     * <ul>
     * <li>background-color</li>
     * <li>background-image</li>
     * <li>background-position</li>
     * <li>background-repeat</li>
     * <li>border-color</li>
     * <li>border-radius</li>
     * <li>border-stroke</li>
     * <li>border-style</li>
     * <li>border-width</li>
     * <li>border-image</li>
     * </ul>
     * </p>
     * @param res Theme resource file from which images can be loaded.
     * @param css CSS to parse.
     * @throws IllegalArgumentException If it fails to parse the style.
     */
    public CSSBorder(Resources res, String css) {
        this.res = res;
        String[] parts = Util.split(css, ";");
        for (String part : parts) {
            int colonPos = part.indexOf(":");
            if (colonPos == -1) {
                continue;
            }
            String key = part.substring(0, colonPos).trim().toLowerCase();
            String value = part.substring(colonPos+1).trim();
            Decorator decorator = decorators.get(key);
            if (decorator == null) {
                throw new IllegalArgumentException("Unsupported CSS property: "+key);
            }
            decorator.decorate(this, key, value);
            
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    
    /**
     * Converts this border to a CSS string.
     * @return CSS string for this border.
     */
    public String toCSSString() {
        StringBuilder sb = new StringBuilder();
        if (backgroundColor != null) {
            sb.append("background-color:").append(backgroundColor.toCSSString()).append(";");
        }
        if (backgroundImages != null) {
            sb.append("background-image:");
            boolean first = true;
            for (BackgroundImage img : backgroundImages) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(img.toCSSString());
            }
            sb.append(";");
            sb.append("background-position:");
            first = true;
            for (BackgroundImage img : backgroundImages) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(img.getBackgroundPositionCSSString());
            }
            sb.append(";");
        }
        
        if (borderRadius != null) {
            sb.append("border-radius:");
            sb.append(borderRadius.toCSSString());
            sb.append(";");
        }
        if (stroke != null) {
            sb.append("border-width:");
            sb.append(stroke[TOP].toBorderWidthCSSString()).append(" ")
                    .append(stroke[RIGHT].toBorderWidthCSSString()).append(" ")
                    .append(stroke[BOTTOM].toBorderWidthCSSString()).append(" ")
                    .append(stroke[LEFT].toBorderWidthCSSString());
            sb.append(";");
            sb.append("border-style:")
                    .append(stroke[TOP].toBorderStyleCSSString()).append(" ")
                    .append(stroke[RIGHT].toBorderStyleCSSString()).append(" ")
                    .append(stroke[BOTTOM].toBorderStyleCSSString()).append(" ")
                    .append(stroke[LEFT].toBorderStyleCSSString());
            sb.append(";");
            sb.append("border-color:")
                    .append(stroke[TOP].toBorderColorCSSString()).append(" ")
                    .append(stroke[RIGHT].toBorderColorCSSString()).append(" ")
                    .append(stroke[BOTTOM].toBorderColorCSSString()).append(" ")
                    .append(stroke[LEFT].toBorderColorCSSString());
            sb.append(";");
        }
        if (boxShadow != null) {
            sb.append("box-shadow:").append(boxShadow.toCSSString()).append(";");
                    
        }
        if (borderImage != null) {
            sb.append("border-image:").append(borderImage.toCSSString()).append(";");
        }
        
        return sb.toString();
    }
    
    private static class ScalarUnit {
        float value;
        byte type;
        
        ScalarUnit copy() {
            return new ScalarUnit(this);
        }
        
        ScalarUnit(String unit) {
            if("0".equals(unit) || "0.0".equals(unit)) {
                this.value = 0;
                this.type = UNIT_PIXELS;
            } else if (unit.endsWith("mm")) {
                this.value = Float.parseFloat(unit.substring(0, unit.length()-2));
                this.type = UNIT_MM;
            } else if (unit.endsWith("px")) {
                this.value = Integer.parseInt(unit.substring(0, unit.length()-2));
                this.type = UNIT_PIXELS;
            } else if (unit.endsWith("em")) {
                this.value = Float.parseFloat(unit.substring(0, unit.length()-2));
                this.type = UNIT_EM;
            } else if (unit.endsWith("%")) {
                this.value = Float.parseFloat(unit.substring(0, unit.length()-1));
                this.type = UNIT_PERCENT;
            } else if (unit.endsWith("pt")) {
                this.value = Float.parseFloat(unit.substring(0, unit.length()-2)) / 72f * 25.4f;
                this.type = UNIT_MM;
            } else if (unit.endsWith("in")) {
                this.value = Float.parseFloat(unit.substring(0, unit.length()-2)) * 25.4f;
                this.type = UNIT_MM;
            } else {
                throw new IllegalArgumentException("Illegal unit "+unit);
            }
            
        }
        
        ScalarUnit(ScalarUnit u) {
            this.value = u.value;
            this.type = u.type;
        }
        
        ScalarUnit(float value, byte type) {
            this.value = value;
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ScalarUnit) {
                ScalarUnit u = (ScalarUnit)obj;
                return u.value == 0 && value == 0 || u.value == value && u.type == type;
            }
            return false;
        }
        
        
        
        static boolean validate(String val) {
            val = val.trim();
            int len = val.length();
            if ("0".equals(val) || "0.0".equals(val)) {
                return true;
            }
            if (val.endsWith("px") && isInt(val.substring(0, len-2))) {
                return true;
            }
            if ((val.endsWith("em") || val.endsWith("mm") || val.endsWith("pt") || val.endsWith("in")) && isFloat(val.substring(0, len-2))) {
                return true;
            }
            if (val.endsWith("%") && isFloat(val.substring(0, len-1))) {
                return true;
            }
            return false;
        }
        
        private static boolean isInt(String val) {
            try {
                Integer.parseInt(val);
                return true;
            } catch (Throwable t) {
                return false;
            }
        }
        
        private static boolean isFloat(String val) {
            try {
                Float.parseFloat(val);
                return true;
            } catch (Throwable t) {
                return false;
            }
        }
        
        boolean isZero() {
            return value == 0;
        }
        
        int px() {
            switch (type) {
                case UNIT_PIXELS:
                    return (int)value;
                case UNIT_MM:
                    return convertToPixels(value);
                default:
                    throw new IllegalArgumentException("Can't get px() units for type "+type+" without providing content rect");
            }
        }
        
        float floatPx() {
            switch (type) {
                case UNIT_PIXELS:
                    return value;
                case UNIT_MM:
                    return convertToPixels(value*1000f)/1000f;
                default:
                    throw new IllegalArgumentException("Can't get px() units for type "+type+" without providing content rect");
                    
            }
        }
        
        float floatPx(Component c, Rectangle2D contentRect, boolean horizontal) {
            switch (type) {
                case UNIT_PIXELS:
                    return value;
                case UNIT_MM:
                    return convertToPixels(value*1000f)/1000f;
                case UNIT_PERCENT:
                    return (float)(horizontal ? contentRect.getWidth() * value/100.0 : contentRect.getHeight() * value/100.0);
                case UNIT_EM:
                    Font f = c.getStyle().getFont();
                    return f != null ? c.getStyle().getFont().getPixelSize() : Font.getDefaultFont().getPixelSize();
                default:
                    throw new IllegalArgumentException("Can't get px() units for type "+type+" without providing content rect");    
            }
        }

        private String toCSSString() {
            StringBuilder sb = new StringBuilder();
            if (Math.ceil(value) == Math.floor(value)) {
                sb.append((int)value);
            } else {
                sb.append(value);
            }
            switch (type) {
                case UNIT_PIXELS:
                    sb.append("px");
                    break;
                case UNIT_MM:
                    sb.append("mm");
                    break;
                case UNIT_PERCENT:
                    sb.append("%");
                    break;
                case UNIT_EM:
                    sb.append("em");
                    break;
                default:
                    throw new IllegalStateException("Unsupported unit type "+type);
                    
            }
            
            return sb.toString();
        }
    }
    
    
    private float floatPx(ScalarUnit u) {
        return u == null ? 0 : u.floatPx();
    }
    
    private float  floatPx(ScalarUnit u, Component c, Rectangle2D contentRect, boolean horizontal) {
        return u == null ? 0 : u.floatPx(c, contentRect, horizontal);
    }
    
    private class BoxShadow {
        ScalarUnit hOffset, vOffset, blurRadius, spread;
        boolean inset;
        Color color;
        
        int spreadPx() {
            return spread != null ? spread.px() : 0;
        }
        
        int blurPx() {
            return blurRadius != null ? blurRadius.px() : 0;
        }
        
        int vOffsetPx() {
            return vOffset != null ? vOffset.px() : 0;
        }
        
        int hOffsetPx() {
            return hOffset != null ? hOffset.px() : 0;
        }
       
        
        void paint(Graphics g, Component c, Rectangle2D contentRect) {
            int alpha = g.getAlpha();
            int color = g.getColor();
            boolean antialiased = g.isAntiAliased();
            setColor(g, this.color);
            GeneralPath p = GeneralPath.createFromPool();
            try {
                createShape(p, contentRect.getX(), contentRect.getY(), contentRect.getWidth(), contentRect.getHeight());
                p.transform(Transform.makeTranslation(hOffset.floatPx(c, contentRect, true), vOffset.floatPx(c, contentRect, false)));
                g.fillShape(p);
                
            } finally {
                GeneralPath.recycle(p);
                g.setAlpha(alpha);
                g.setColor(color);
                g.setAntiAliased(antialiased);
            }
        }

        private String toCSSString() {
            throw new RuntimeException("Box-shadow not fully supported yet");
        }
                
    }
    
    private static class Context {
        Component component;
        Rectangle2D contentRect;
        Context(Component comp, Rectangle2D contentRect) {
            component = comp;
            this.contentRect = contentRect;
        }
        
    }
    
    private static Context context;
    
    private class BorderRadius {
        private ScalarUnit topLeftX, topRightX, bottomLeftX, bottomRightX;
        private ScalarUnit topLeftY, topRightY, bottomLeftY, bottomRightY;
        
        ScalarUnit[] all() {
            return new ScalarUnit[]{topLeftX, topLeftY, topRightX, topRightY, bottomRightX, bottomRightY, bottomLeftX, bottomLeftY};
        }
        
        ScalarUnit[] horizontal() {
            return new ScalarUnit[]{topLeftX, topRightX, bottomRightX, bottomLeftX};
        }
        
        ScalarUnit[] vertical() {
            return new ScalarUnit[]{topLeftY, topRightY, bottomRightY, bottomLeftY};
        }
        
        ScalarUnit[] topLeft() {
            return new ScalarUnit[]{topLeftX, topLeftY};
        }
        
        ScalarUnit[] topRight() {
            return new ScalarUnit[]{topRightX, topRightY};
        }
        
        ScalarUnit[] bottomRight() {
            return new ScalarUnit[]{bottomRightX, bottomRightY};
        }
        
        ScalarUnit[] bottomLeft() {
            return new ScalarUnit[]{bottomLeftX, bottomLeftY};
        }
        
        BorderRadius(String value) {
            if (value.indexOf("/") > 0) {
                String[] parts = Util.split(value, "/");
                String[] hVals = Util.split(parts[0].trim(), " ");
                String[] vVals = Util.split(parts[1].trim(), " ");
                if (hVals.length == 1) {
                    topLeftX = new ScalarUnit(hVals[0]);
                    topRightX = new ScalarUnit(topLeftX);
                    bottomLeftX = new ScalarUnit(topLeftX);
                    bottomRightX = new ScalarUnit(topLeftX);
                } else if (hVals.length == 2) {
                    topLeftX = new ScalarUnit(hVals[0]);
                    bottomRightX = new ScalarUnit(topLeftX);
                    topRightX = new ScalarUnit(hVals[1]);
                    bottomLeftX = new ScalarUnit(topRightX);
                } else if (hVals.length == 3) {
                    topLeftX = new ScalarUnit(hVals[0]);
                    topRightX = new ScalarUnit(hVals[1]);
                    bottomLeftX = new ScalarUnit(topRightX);
                    bottomRightX = new ScalarUnit(hVals[2]);
                } else if (hVals.length == 4) {
                    topLeftX = new ScalarUnit(hVals[0]);
                    topRightX = new ScalarUnit(hVals[1]);
                    bottomRightX = new ScalarUnit(hVals[2]);
                    bottomLeftX = new ScalarUnit(hVals[3]);
                } else {
                    throw new IllegalArgumentException("Border radius should include 1, 2, 3, of 4 params only");
                }
                
                if (vVals.length == 1) {
                    topLeftY = new ScalarUnit(vVals[0]);
                    topRightY = new ScalarUnit(topLeftY);
                    bottomLeftY = new ScalarUnit(topLeftY);
                    bottomRightY = new ScalarUnit(topLeftY);
                } else if (vVals.length == 2) {
                    topLeftY = new ScalarUnit(vVals[0]);
                    bottomRightY = new ScalarUnit(topLeftY);
                    topRightY = new ScalarUnit(hVals[1]);
                    bottomLeftY = new ScalarUnit(topRightY);
                } else if (vVals.length == 3) {
                    topLeftY = new ScalarUnit(hVals[0]);
                    topRightY = new ScalarUnit(hVals[1]);
                    bottomLeftY = new ScalarUnit(topRightY);
                    bottomRightY = new ScalarUnit(hVals[2]);
                } else if (vVals.length == 4) {
                    topLeftY = new ScalarUnit(vVals[0]);
                    topRightY = new ScalarUnit(vVals[1]);
                    bottomRightY = new ScalarUnit(vVals[2]);
                    bottomLeftY = new ScalarUnit(vVals[3]);
                } else {
                    throw new IllegalArgumentException("Border radius should include 1, 2, 3, of 4 params only: "+Arrays.toString(vVals));
                }
                
            } else {
                String[] vals = Util.split(value, " ");
                switch (vals.length) {
                    case 1 :
                        topLeftX = new ScalarUnit(vals[0]);
                        topLeftY = topLeftX.copy();
                        topRightX = topLeftX.copy();
                        topRightY = topLeftX.copy();
                        bottomRightX = topLeftX.copy();
                        bottomRightY = topLeftX.copy();
                        bottomLeftX = topLeftX.copy();
                        bottomLeftY = topLeftX.copy();
                        break;
                    case 2:
                        topLeftX = new ScalarUnit(vals[0]);
                        topLeftY = topLeftX.copy();
                        bottomRightX = topLeftX.copy();
                        bottomRightY = topLeftX.copy();
                        
                        topRightX = new ScalarUnit(vals[1]);
                        topRightY = topRightX.copy();
                        bottomLeftX = topRightX.copy();
                        bottomLeftY = topRightX.copy();
                        break;
                    case 3:
                        topLeftX = new ScalarUnit(vals[0]);
                        topLeftY = topLeftX.copy();
                        topRightX = new ScalarUnit(vals[1]);
                        topRightY = topRightX.copy();
                        bottomLeftX = topRightX.copy();
                        bottomLeftY = topRightX.copy();
                        bottomRightX = new ScalarUnit(vals[2]);
                        bottomRightY = bottomRightX.copy();
                        break;
                        
                    case 4:
                        topLeftX = new ScalarUnit(vals[0]);
                        topLeftY = topLeftX.copy();
                        topRightX = new ScalarUnit(vals[1]);
                        topRightY = topRightX.copy();
                        bottomRightX = new ScalarUnit(vals[2]);
                        bottomRightY =bottomRightX.copy();
                        bottomLeftX = new ScalarUnit(vals[3]);
                        bottomLeftY = bottomLeftX.copy();
                        break;
                    default:
                        throw new IllegalArgumentException("Illegal input for border radius: "+value);
                }
            }
        }
        
        boolean hasNonZeroRadius() {
            return topLeftX != null && !topLeftX.isZero() || 
                    topRightX != null && !topRightX.isZero() || 
                    bottomLeftX != null && !bottomLeftX.isZero() ||
                    bottomRightX != null && !bottomRightX.isZero() ||
                    topLeftY != null && !topLeftY.isZero() || 
                    topRightY != null && !topRightY.isZero() || 
                    bottomLeftY != null && !bottomLeftY.isZero() ||
                    bottomRightY != null && !bottomRightY.isZero();
        }
        
        float topLeftRadiusX() {
            if (context != null) {
                return floatPx(topLeftX, context.component, context.contentRect, true);
            }
            return floatPx(topLeftX);
        }
        
        float topLeftRadiusY() {
            if (context != null) {
                return floatPx(topLeftY, context.component, context.contentRect, false);
            }
            return floatPx(topLeftY);          
        }
        
        float topRightRadiusX() {
            if (context != null) {
                return floatPx(topRightX, context.component, context.contentRect, true);
            }
            return floatPx(topRightX);
        }
        
        float topRightRadiusY() {
            if (context != null) {
                return floatPx(topRightY, context.component, context.contentRect, false);
            }
            return floatPx(topRightY);
        }
        
        float bottomLeftX() {
            if (context != null) {
                return floatPx(bottomLeftX, context.component, context.contentRect, true);
            }
            return floatPx(bottomLeftX);
        }
        
        float bottomLeftY() {
            if (context != null) {
                return floatPx(bottomLeftY, context.component, context.contentRect, false);
            }
            return floatPx(bottomLeftY);
        }
        
        float bottomRightX() {
            if (context != null) {
                return floatPx(bottomRightX, context.component, context.contentRect, true);
            }
            return floatPx(bottomRightX);
        }
        
        float bottomRightY() {
            if (context != null) {
                return floatPx(bottomRightY, context.component, context.contentRect, false);
            }
            return floatPx(bottomRightY);
        }

        private String toCSSString() {
            StringBuilder sb = new StringBuilder();
            sb.append(topLeftX.toCSSString()).append(" ")
                    .append(topRightX.toCSSString()).append(" ")
                    .append(bottomRightX.toCSSString()).append(" ")
                    .append(bottomLeftX.toCSSString()).append(" / ")
                    .append(topLeftY.toCSSString()).append(" ")
                    .append(topRightY.toCSSString()).append(" ")
                    .append(bottomRightY.toCSSString()).append(" ")
                    .append(bottomLeftY.toCSSString());
            return sb.toString();
        }
        
        
        
    }
    
    private static class Color {
        int color;
        int alpha;
        
        static final int CACHE_SIZE=100;
        static Map<String,Color> cache;
        static Map<String,Color> cache() {
            if (cache == null) {
                cache = new HashMap<String,Color>();
            }
            return cache;
        }
        
        private String padLeft(String str, int len) {
            while (str.length() < len) {
                str = "0" + str;
            }
            return str;
        }
        
        public String toCSSString() {
            StringBuilder sb = new StringBuilder();
            sb.append("#");
            sb.append(padLeft(Integer.toHexString(color), 6));
            sb.append(padLeft(Integer.toHexString(alpha), 2));
            return sb.toString();
        }
        
        static Color parse(String value) {
            value = value.trim();
            if (!cache().containsKey(value)) {
                if (cache.size() > CACHE_SIZE) {
                    cache.clear();
                }
                cache.put(value, new Color(value));
                
            }
            return cache.get(value);
        }
        
        static boolean validate(String value) {
            return value.startsWith("#") || value.startsWith("rgb(") || value.startsWith("rbga(") || "transparent".equals(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Color) {
                Color c = (Color)obj;
                return c.alpha == alpha && c.color == color;
            }
            return false;
        }
        
        
        
        Color(String value) {
            
            if (value.startsWith("#")) {
                if (value.length() == 9) {
                    alpha = Integer.parseInt(value.substring(7, 9), 16);
                    color = Integer.parseInt(value.substring(1, 7), 16);
                } else if (value.length() == 7) {
                    alpha = 0xff;
                    color = Integer.parseInt(value.substring(1, 7), 16);
                    
                } else if (value.length() == 5) {
                    String rStr = value.substring(1,2);
                    String gStr = value.substring(2,3);
                    String bStr = value.substring(3,4);
                    String aStr = value.substring(4,5);
                    alpha = Integer.parseInt(aStr+aStr, 16);
                    color = 0xffffff & ColorUtil.rgb( 
                            Integer.parseInt(rStr+rStr, 16),
                            Integer.parseInt(gStr+gStr, 16),
                            Integer.parseInt(bStr+bStr, 16)
                    );
                } else if (value.length() == 4) {
                    alpha = 0xff;
                    String rStr = value.substring(1,2);
                    String gStr = value.substring(2,3);
                    String bStr = value.substring(3,4);
                    
                    
                    color = 0xffffff & ColorUtil.rgb( 
                            Integer.parseInt(rStr+rStr, 16),
                            Integer.parseInt(gStr+gStr, 16),
                            Integer.parseInt(bStr+bStr, 16)
                    );
                    
                } else {
                    throw new IllegalArgumentException("Illegal color value "+value);
                }
            } else if (value.startsWith("rgb(")) {
                throw new IllegalArgumentException("rgb() color values not supported yet: "+value);
            } else if ("transparent".equals(value)) {
                alpha = 0;
                color = 0x0;
            } else {
                throw new IllegalArgumentException("Unsuppored color value: "+value);
            }
                
        }
        
        boolean isTransparent() {
            return alpha == 0;
        }
    }
    
    private static boolean isTransparent(Color color) {
        return color == null || color.isTransparent();
    }
    
    private class ColorStop {
        Color color;
        int position;
        
        
        public String toCSSString() {
            StringBuilder sb = new StringBuilder();
            sb.append(color.toCSSString());
            if (position > 0) {
                sb.append(" ").append(position).append("%");
            }
            return sb.toString();
        }
        
    }
    
    
    
    private class LinearGradient {
        float angle;
        ColorStop[] colors;
        
        double directionRadian() {
            return angle * Math.PI/180.0;
        }

        private String toCSSString() {
            StringBuilder sb = new StringBuilder();
            sb.append("linear-gradient(");
            sb.append(angle).append("deg");
            sb.append(",");
            boolean first = true;
            
            for (ColorStop cs : colors) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(cs.toCSSString());
            }
            sb.append(")");
            return sb.toString();
        }
    }
    
    private class RadialGradient {
        byte shape;
        byte size;
        float xPos, yPos;
        ColorStop[] colors;

        private String toCSSString() {
            throw new RuntimeException("RadialGradlient toCSSString() not implemented yet");
        }
    }
    
    private static Map<String,Byte> styleMap;
    private static Map<String,Byte> styleMap() {
        if (styleMap == null) {
            styleMap = new HashMap<String,Byte>();
            styleMap.put("none", STYLE_NONE);
            styleMap.put("hidden", STYLE_HIDDEN);
            styleMap.put("dotted", STYLE_DOTTED);
            styleMap.put("dashed", STYLE_DASHED);
            styleMap.put("solid", STYLE_SOLID);
        }
        return styleMap;
    }
    private static byte getBorderStyle(String style) {
        style = style.trim();
        styleMap();
        Byte b = styleMap.get(style);
        if (b == null) {
            throw new IllegalArgumentException("Unsupported border style "+style);
            
        }
        return b;
    }
    
    private static boolean validateBorderStyle(String style) {
        return styleMap().containsKey(style);
    }
    
    private static class BorderStroke {
        byte type;
        ScalarUnit thickness;
        Color color;
        
        public String toBorderWidthCSSString() {
            return thickness.toCSSString();
        }
        
        public String toBorderColorCSSString() {
            return color.toCSSString();
        }
        
        public String toBorderStyleCSSString() {
            switch (type) {
                case STYLE_SOLID:
                    return "solid";
                case STYLE_DOTTED:
                    return "dotted";
                case STYLE_DASHED:
                    return "dashed";
                case STYLE_HIDDEN:
                    return "hidden";
                case STYLE_NONE:
                    return "none";
                
            }
            return "none";
        }
        
        BorderStroke(String value) {
            String[] parts = Util.split(value, " ");
            if (parts.length == 3) {
                thickness = parseThickness(parts[0]);
                type = getBorderStyle(parts[1]);
                color = Color.parse(parts[2]);
            } else if (parts.length == 2) {
                int index = 0;
                if (validateThickness(parts[index])) {
                    thickness = parseThickness(parts[index]);
                    index++;
                } else {
                    thickness = parseThickness("medium");
                }
                
                if (validateBorderStyle(parts[index])) {
                    type = getBorderStyle(parts[index]);
                    index++;
                } else {
                    type = STYLE_NONE;
                }
                
                if (index < 2) {
                    color = Color.parse(parts[index]);
                    index++;
                } else {
                    color = Color.parse("transparent");
                }
                
                if (index < 2) {
                    throw new IllegalArgumentException("Illegal border stroke parameter "+value);
                }
                
            } else if (parts.length == 1) {
                boolean used = false;
                if (validateThickness(value)) {
                    thickness = parseThickness(value);
                    used = true;
                } else {
                    thickness = parseThickness("medium");
                }
                if (!used && validateBorderStyle(value)) {
                    type = getBorderStyle(value);
                    used = true;
                } else {
                    type = STYLE_NONE;
                }
                if (!used && Color.validate(value)) {
                    color = Color.parse(value);
                    used = true;
                } else {
                    color = Color.parse("transparent");
                }
                if (!used) {
                    throw new IllegalArgumentException("Illegal border stroke parameter "+value);
                }
            } else {
                throw new IllegalArgumentException("Illegal border stroke parameter "+value);
            }
            
            
        }
        
        BorderStroke(BorderStroke stroke) {
            this.type = stroke.type;
            this.thickness = stroke.thickness.copy();
            this.color = stroke.color;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BorderStroke) {
                BorderStroke s = (BorderStroke)obj;
                return s.type == type && s.thickness.equals(thickness) && s.color.equals(color);
            }
            return false;
        }
        
        boolean isVisible() {
            return color != null && !isTransparent(color) && type != STYLE_NONE && type != STYLE_HIDDEN;
        }
        
        static boolean validateThickness(String val) {
            return ScalarUnit.validate(val) || "thin".equals(val) || "medium".equals(val) || "thick".equals(val);
        }
        
        static ScalarUnit parseThickness(String val) {
            val = val.trim();
            if (ScalarUnit.validate(val)) {
                return new ScalarUnit(val);
            } else if ("thin".equals(val)) {
                return new ScalarUnit("1px");
            } else if ("medium".equals(val)) {
                return new ScalarUnit("0.75mm");
            } else if ("thick".equals(val)) {
                return new ScalarUnit("1.4mm");
            }
            throw new IllegalArgumentException("Illegal thickness value "+val);
        }
        
        
        
        Stroke getStroke(Component c, Rectangle2D contentRect, boolean horizontal) {
            return new Stroke(thickness.floatPx(c, contentRect, horizontal), Stroke.CAP_BUTT, Stroke.JOIN_MITER, 100f);
            
        }
        
    }
    
    private class BorderImage {
        Image image;
        double[] slices;
        Border internal;
        String imageName;
        
        BorderImage(String imageName, double... slces) {
            this.imageName = imageName;
            slices = new double[4];
            if (slces.length == 4) {
                System.arraycopy(slces, 0, slices, 0, 4);
            } else if (slces.length == 3) {
                slices[0] = slces[0];
                slices[1] = slices[3] = slces[1];
                slices[2] = slces[2];
            } else if (slces.length == 2) {
                slices[0] = slices[2] = slces[0];
                slices[1] = slices[3] = slces[1];
            } else if (slces.length == 1) {
                slices[0] = slices[1] = slices[2] = slices[3] = slces[0];
            } else {
                throw new IllegalArgumentException("Slices expected to be length 1 to 4, but found size "+slces.length+": "+Arrays.toString(slces));
            }
        }
        
        BorderImage(Image img, double... slces) {
            image = img;
            slices = new double[4];
            if (slces.length == 4) {
                System.arraycopy(slces, 0, slices, 0, 4);
            } else if (slces.length == 3) {
                slices[0] = slces[0];
                slices[1] = slices[3] = slces[1];
                slices[2] = slces[2];
            } else if (slces.length == 2) {
                slices[0] = slices[2] = slces[0];
                slices[1] = slices[3] = slces[1];
            } else if (slces.length == 1) {
                slices[0] = slices[1] = slices[2] = slices[3] = slces[0];
            } else {
                throw new IllegalArgumentException("Slices expected to be length 1 to 4, but found size "+slces.length+": "+Arrays.toString(slces));
            }
            internal = Border.createImageSplicedBorder(img, slices[0], slices[1], slices[2], slices[3]);
        }
        
        void paint(Graphics g, Component c, Rectangle2D contentRect) {
            internal().paint(g, (int)contentRect.getX(), (int)contentRect.getY(), (int)contentRect.getWidth(), (int)contentRect.getHeight(), c);
        }
        
        Image image() {
            if (image == null) {
                image = res.getImage(imageName);
                if (image == null) {
                    try {
                        image = EncodedImage.create("/"+imageName);
                    } catch (IOException ex) {
                        Log.p("Failed to load image named "+imageName+" for CSSBorder");
                        throw new IllegalStateException("Failed to load image "+imageName);
                    }
                }
            }
            return image;
        }
        
        Border internal() {
            if (internal == null) {
                internal = Border.createImageSplicedBorder(image(), slices[0], slices[1], slices[2], slices[3]);
            }
            return internal;
        }
        
        String toCSSString() {
            String imgName = imageName;
            if (imgName == null && image != null) {
                imgName = image.getImageName();
            }
            return Util.encodeUrl(imgName)+" "+slices[0]+" "+slices[1]+" "+slices[2]+" "+slices[3];
        }
    }
    
    private class BackgroundImage {
        LinearGradient linearGradient;
        RadialGradient radialGradient;
        byte verticalPositionType, horizontalPositionType, verticalSizeType, horizontalSizeType;
        ScalarUnit verticalPosition;
        ScalarUnit horizontalPosition;
        ScalarUnit verticalSize;
        ScalarUnit horizontalSize;
        Image image;
        byte repeat;
        
        
        BackgroundImage() {
            repeat = REPEAT_NONE;
            verticalPositionType = VPOSITION_TOP;
            horizontalPositionType = HPOSITION_LEFT;
        }
        
        BackgroundImage(Image image) {
            this.image = image;
            repeat = REPEAT_NONE;
            verticalPositionType = VPOSITION_TOP;
            horizontalPositionType = HPOSITION_LEFT;
            
        }
        
        public String toCSSString() {
            if (linearGradient != null) {
                return linearGradient.toCSSString();
            }
            if (radialGradient != null) {
                return radialGradient.toCSSString();
            }
            if (image != null && image.getImageName() != null) {
                return "url(\"" + image.getImageName()+"\"";
            }
            
            return "none";
                
        }
        
        private void setPosition(String pos) {
            
        }
        
        private String getBackgroundPositionCSSString() {
            StringBuilder sb = new StringBuilder();
            switch (verticalPositionType) {
                case VPOSITION_TOP:
                    sb.append("top").append(" ");
                    break;
                case VPOSITION_BOTTOM:
                    sb.append("bottom").append(" ");
                    break;
                case VPOSITION_CENTER:
                    sb.append("center").append(" ");
                    break;
            }
            if (verticalPosition != null) {
                sb.append(verticalPosition.toCSSString()).append(" ");
                
            }
            
            switch (horizontalPositionType) {
                case HPOSITION_LEFT:
                    sb.append("left").append(" ");
                    break;
                case HPOSITION_RIGHT:
                    sb.append("right").append(" ");
                    break;
                case HPOSITION_CENTER:
                    sb.append("center").append(" ");
                    break;
            }
            
            if (horizontalPosition != null) {
                sb.append(horizontalPosition.toCSSString()).append(" ");
            }
            return sb.toString().trim();
        }
        
        
        private Rectangle2D getTargetRect(Component c, Rectangle2D out, Rectangle2D contentRect) {
            if (image != null) {
                double w = image.getWidth();
                double h = image.getHeight();
                
                switch (verticalSizeType) {
                    case SIZE_CONTAIN: 
                        if (w > contentRect.getWidth()) {
                            h = h * contentRect.getWidth()/w;
                            w = contentRect.getWidth();
                        }
                        if (h > contentRect.getHeight()) {
                            w = contentRect.getHeight()/h;
                            h = contentRect.getHeight();
                        }
                    break;
                    case SIZE_COVER:
                        double aspect = w/h;
                        w = image.getWidth();
                        h = w/aspect;
                        if (h < image.getHeight()) {
                            h = image.getHeight();
                            w = h * aspect;
                        }
                    break;
                    case SIZE_OTHER:
                        w = floatPx(horizontalSize, c, contentRect, true);
                        h = floatPx(verticalSize, c, contentRect, false);
                        break;
 
                }
                
                double x = contentRect.getX();
                double y = contentRect.getY();
                
                switch (verticalPositionType) {
                    case VPOSITION_BOTTOM: 
                        y = contentRect.getY() + contentRect.getHeight()-h;
                        break;
                    case VPOSITION_CENTER:
                        y = contentRect.getY() + contentRect.getHeight()/2 - h/2;
                        break;
                    case VPOSITION_OTHER:
                        y = contentRect.getY() + floatPx(verticalPosition, c, contentRect, false);
                        break;
                }
                
                switch (horizontalPositionType) {
                    case HPOSITION_RIGHT:
                        x = contentRect.getX() + contentRect.getWidth() - w;
                        break;
                    case HPOSITION_CENTER:
                        x = contentRect.getX() + contentRect.getWidth()/2 - w/2;
                        break;
                    case HPOSITION_OTHER:
                        x = contentRect.getX() + floatPx(horizontalPosition, c, contentRect, true);
                        break;
                }
                
                out.setBounds(x, y, w, h);
                return out;
                
            } else {
                out.setBounds(contentRect.getX(), contentRect.getY(), contentRect.getWidth(), contentRect.getHeight());
                return out;
            }
            
        }
        
        void paint(Graphics g, Component c, Rectangle2D contentRect) {
            // Note:  This assumes that a shape clip has already happened f
        
            if (image != null) {
                switch (repeat) {
                    case REPEAT_NONE: {
                        Rectangle2D targetRect = getTargetRect(c, new Rectangle2D(), contentRect);
                        g.drawImage(image, (int)targetRect.getX(), (int)targetRect.getY(), (int)targetRect.getWidth(), (int)targetRect.getHeight());
                        break;
                    }
                    case REPEAT_X: {
                       Rectangle2D targetRect = getTargetRect(c, new Rectangle2D(), contentRect);
                       Image scaled = image.scaled((int)targetRect.getWidth(), (int)targetRect.getHeight());
                       double offX = targetRect.getX() - contentRect.getX();
                       while (offX > 0) {
                           offX -= targetRect.getWidth();
                       }
                       // offX should be non-positive
                       
                       g.tileImage(scaled, (int)(contentRect.getX() + offX), (int)targetRect.getY(), (int)(contentRect.getWidth() - offX), (int)targetRect.getHeight());
                       break;
                    }
                    case REPEAT_Y: {
                       Rectangle2D targetRect = getTargetRect(c, new Rectangle2D(), contentRect);
                       Image scaled = image.scaled((int)targetRect.getWidth(), (int)targetRect.getHeight());
                       double offY = targetRect.getY() - contentRect.getY();
                       while (offY > 0) {
                           offY -= targetRect.getHeight();
                       }
                       // offX should be non-positive
                       
                       g.tileImage(scaled, (int)targetRect.getX(), (int)(contentRect.getY() + offY), (int)targetRect.getWidth(), (int)(contentRect.getHeight()-offY));
                       break;
                    }
                    
                    case REPEAT_BOTH: {
                        Rectangle2D targetRect = getTargetRect(c, new Rectangle2D(), contentRect);
                        Image scaled = image.scaled((int)targetRect.getWidth(), (int)targetRect.getHeight());
                        double offY = targetRect.getY() - contentRect.getY();
                        while (offY > 0) {
                            offY -= targetRect.getHeight();
                        }
                        double offX = targetRect.getX() - contentRect.getX();
                        while (offX > 0) {
                            offX -= targetRect.getWidth();
                        }
                        // offX should be non-positive
                       
                        g.tileImage(scaled, (int)(contentRect.getX() + offX), (int)(contentRect.getY() + offY), (int)(contentRect.getWidth() - offX), (int)(contentRect.getHeight()-offY));
                        break;
                    }
                        

                }
            } else {
                if (linearGradient != null) {
                    
                    ColorStop prevColor = null;
                    
                    // Figure out the width that will be required
                    
                    double contentWidth = contentRect.getWidth() * Math.cos(linearGradient.directionRadian()) + contentRect.getHeight() * Math.sin(linearGradient.directionRadian());
                    double contentHeight = contentRect.getHeight() + Math.cos(linearGradient.directionRadian()) + contentRect.getWidth() * Math.sin(linearGradient.directionRadian());
                    
                    double contentX = contentRect.getX() + contentRect.getWidth()/2 - contentWidth/2;
                    double contentY = contentRect.getY() + contentRect.getHeight()/2 - contentHeight/2;
                    double x = contentX;
                    
                    Transform existingT = null;
                    if (linearGradient.directionRadian() != 0) {
                        existingT = Transform.makeIdentity(); 
                        g.getTransform(existingT);

                        Transform newT = existingT.copy();
                        newT.rotate((float)linearGradient.directionRadian(), (float)(contentX + contentWidth/2), (int)(contentY + contentHeight/2));
                        g.setTransform(newT);
                    }
                    
                    
                    
                    
                    for (ColorStop colorStop : linearGradient.colors) {
                        if (prevColor == null) {
                            prevColor = colorStop;
                            continue;
                        }
                        int alpha = g.getAlpha();
                        setAlpha(g, colorStop.color);
                        double nextX = x + contentWidth * colorStop.position /100.0;
                        g.fillLinearGradient(prevColor.color.color, colorStop.color.color, (int)x, (int)contentY, (int)(nextX-x), (int)contentHeight, true);
                        g.setAlpha(alpha);
                        x = nextX;
                        
                    }
                    
                    if (existingT != null) {
                        g.setTransform(existingT);
                    }
                }
                
                if (radialGradient != null) {
                    
                }
            }
        }

        
        
    }

    private boolean hasBorderRadius() {
        return borderRadius != null && borderRadius.hasNonZeroRadius();
    }
    
    /**
     * Since borders are drawn inside the bounds of components - this differs from HTML.  We 
     * need to be able to find the inner content bounds of the component so that we have room to 
     * draw shadows, etc..
     * @param outerWidth
     * @param outerHeight
     * @param rect Out param
     */
    private void calculateContentRect(int outerWidth, int outerHeight, Rectangle2D rect) {
        int paddingLeft = 0;
        int paddingRight = 0;
        int paddingBottom = 0;
        int paddingTop = 0;
        
        if (stroke != null) {
            if (stroke[TOP] != null) {
                paddingTop += Math.ceil(stroke[TOP].thickness.floatPx()/2);
            }
            if (stroke[LEFT] != null) {
                paddingLeft += Math.ceil(stroke[LEFT].thickness.floatPx()/2);
            }
            if (stroke[RIGHT] != null) {
                paddingRight += Math.ceil(stroke[RIGHT].thickness.floatPx()/2);
            }
            if (stroke[BOTTOM] != null) {
                paddingBottom += Math.ceil(stroke[BOTTOM].thickness.floatPx()/2);
            }
        }
        
        if (boxShadow != null && !boxShadow.inset) {
            paddingTop += -boxShadow.vOffsetPx() + boxShadow.blurPx() + boxShadow.spreadPx();
            paddingBottom += boxShadow.vOffsetPx() + boxShadow.blurPx() + boxShadow.spreadPx();
            paddingLeft += -boxShadow.hOffsetPx() + boxShadow.blurPx() + boxShadow.spreadPx();
            paddingRight += boxShadow.hOffsetPx() + boxShadow.blurPx() + boxShadow.spreadPx();
        }
        
        rect.setX(paddingLeft);
        rect.setY(paddingTop);
        rect.setWidth(outerWidth-paddingLeft-paddingRight);
        rect.setHeight(outerHeight-paddingTop-paddingBottom);
    }
    
    private GeneralPath createShape(GeneralPath out, double x, double y, double width, double height) {
        if (hasBorderRadius()) {
            out.reset();
            out.moveTo(borderRadius.topLeftRadiusX(), 0);
            out.lineTo(width - borderRadius.topRightRadiusX(), 0);
            //out.arcTo(width-borderRadius.topRightRadiusX(), borderRadius.topRightRadiusY(), width, borderRadius.topRightRadiusY(), true);
            out.quadTo(width, 0, width, borderRadius.topRightRadiusY());
            out.lineTo(width, height - borderRadius.bottomRightY());
            //out.arcTo(width-borderRadius.bottomRightX(), height-borderRadius.bottomRightY(), width-borderRadius.bottomRightX(), height, true);
            out.quadTo(width, height, width-borderRadius.bottomRightX(), height);
            out.lineTo(borderRadius.bottomLeftX(), height);
            //out.arcTo(borderRadius.bottomLeftX(), height-borderRadius.bottomLeftY(), 0, height-borderRadius.bottomLeftY(), true);
            out.quadTo(0, height, 0, height - borderRadius.bottomLeftY());
            out.lineTo(0, borderRadius.topLeftRadiusY());
            //out.arcTo(borderRadius.topLeftRadiusX(), borderRadius.topLeftRadiusY(), borderRadius.topLeftRadiusX(), 0, true);
            out.quadTo(0, 0, borderRadius.topLeftRadiusX(), 0);
            out.closePath();
            
        } else {
            out.reset();
            out.moveTo(0,0);
            out.lineTo(width, 0);
            out.lineTo(width, height);
            out.lineTo(0, height);
            out.closePath();
        }
        out.transform(Transform.makeTranslation((float)x, (float)y));
        return out;
    }
    
    private Rectangle2D contentRect;
    
    private boolean hasBackgroundImages() {
        return backgroundImages != null && backgroundImages.length > 0;
    }
    
   
    private void setColor(Graphics g, Color c) {
        if (c != null) {
            g.setAlpha(c.alpha);
            g.setColor(c.color);
        }
    }
    
    
    boolean allSidesHaveSameStroke() {
        if (stroke == null) return true;
        
        return stroke[TOP].equals(stroke[BOTTOM]) && stroke[LEFT].equals(stroke[RIGHT]) && stroke[TOP].equals(stroke[LEFT]);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isBackgroundPainter() {
        return true;
    }
    
    
    
    /**
     * {@inheritDoc }
     */
    @Override
    public void paintBorderBackground(Graphics g, Component c) {
        if (borderImage != null) {
            // A border image overrides everything!
            borderImage.internal().paintBorderBackground(g, c);
            return;
        }
        int alpha = g.getAlpha();
        int color = g.getColor();
        boolean antialias = g.isAntiAliased();
        g.setAntiAliased(true);
        Style s = c.getStyle();
        try {
            if (contentRect == null) contentRect = new Rectangle2D();
            calculateContentRect(c.getWidth(), c.getHeight(), contentRect);
            contentRect.setX(contentRect.getX() + c.getX());
            contentRect.setY(contentRect.getY() + c.getY());
            context = new Context(c, contentRect);
            GeneralPath p = GeneralPath.createFromPool();
            try {
                createShape(p, contentRect.getX(), contentRect.getY(), contentRect.getWidth(), contentRect.getHeight());
                
                if (boxShadow != null) {
                    boxShadow.paint(g, c, contentRect);
                }
                if (s.getBgTransparency() != 0) {
                    g.setColor(s.getBgColor());
                    int tp = s.getBgTransparency() & 0xff;
                    int al = (int)Math.round(alpha * tp/255.0);
                    g.setAlpha(al);
                    g.fillShape(p);
                    g.setColor(color);
                    g.setAlpha(alpha);
                    
                }

                if (hasBackgroundImages()) {
                    int[] oldClip = g.getClip();
                    g.setClip(p);
                    g.clipRect(oldClip[0], oldClip[1], oldClip[2], oldClip[3]);
                    for (BackgroundImage img : backgroundImages) {
                        img.paint(g, c, contentRect);
                    }
                    g.setClip(oldClip);
                }
                if (stroke != null) {
                    if (allSidesHaveSameStroke() && stroke[TOP].isVisible()) {
                        setColor(g,stroke[TOP].color);
                        g.drawShape(p, stroke[TOP].getStroke(c, contentRect, true));
                    } else {
                        p.reset();
                        double x = contentRect.getX();
                        double y = contentRect.getY();
                        double w = contentRect.getWidth();
                        double h = contentRect.getHeight();
                        if (hasBorderRadius()) {
                            
                            if (stroke[TOP].isVisible()) {
                                p.moveTo(x, y + borderRadius.topLeftRadiusY());
                                //p.arcTo(contentRect.getX() + borderRadius.topLeftRadiusX(), contentRect.getY() + borderRadius.topLeftRadiusY(), contentRect.getX() + borderRadius.topLeftRadiusX(), contentRect.getY());
                                p.quadTo(x, y, x + borderRadius.topLeftRadiusX(), y);
                                p.lineTo(x + w - borderRadius.topRightRadiusX(), y);
                                //p.arcTo(contentRect.getX() + contentRect.getWidth() - borderRadius.topRightRadiusX(), contentRect.getY() + borderRadius.topRightRadiusY(), contentRect.getX() + contentRect.getWidth(), contentRect.getY() + borderRadius.topRightRadiusY());
                                p.quadTo(x + w, y, x+w, y + borderRadius.topRightRadiusY());
                                setColor(g, stroke[TOP].color);
                                g.drawShape(p, stroke[TOP].getStroke(c, contentRect, true));
                            }
                            if (stroke[BOTTOM].isVisible()) {
                                p.reset();
                                p.moveTo(x, y + h - borderRadius.bottomLeftY());
                                //p.arcTo(contentRect.getX() + borderRadius.bottomLeftX(), contentRect.getY() + contentRect.getHeight() - borderRadius.bottomLeftY(), contentRect.getX() + borderRadius.bottomLeftX(), contentRect.getY() + contentRect.getHeight());
                                p.quadTo(x, y+h, x+borderRadius.bottomLeftX(), y+h);
                                p.lineTo(x + w - borderRadius.bottomRightX(), y + h);
                                //p.arcTo(contentRect.getX() + contentRect.getWidth() - borderRadius.bottomRightX(), contentRect.getY() + contentRect.getHeight() - borderRadius.bottomRightY(), contentRect.getX() + contentRect.getWidth(), contentRect.getY() + contentRect.getHeight() - borderRadius.bottomRightY());
                                p.quadTo(x+w, y+h, x+w, y+h-borderRadius.bottomLeftY());
                                setColor(g, stroke[BOTTOM].color);
                                g.drawShape(p, stroke[BOTTOM].getStroke(c, contentRect, true));
                            }
                            if (stroke[LEFT].isVisible()) {
                                p.reset();
                                p.moveTo(x, y + borderRadius.topLeftRadiusY());
                                p.lineTo(x, y + h - borderRadius.bottomLeftY());
                                setColor(g, stroke[LEFT].color);
                                g.drawShape(p, stroke[LEFT].getStroke(c, contentRect, false));
                            }
                            if (stroke[RIGHT].isVisible()) {
                                p.reset();
                                p.moveTo(x + w, y + borderRadius.topRightRadiusY());
                                p.lineTo(x + w, y + h - borderRadius.bottomRightY());
                                setColor(g, stroke[RIGHT].color);
                                g.drawShape(p, stroke[RIGHT].getStroke(c, contentRect, false));
                            }
                            
                            
                        } else {
                            
                            if (stroke[TOP].isVisible()) {
                                
                                p.reset();
                                p.moveTo(x, y);
                                p.lineTo(x + w, y);
                                
                                setColor(g, stroke[TOP].color);
                               
                                Stroke st = stroke[TOP].getStroke(c, contentRect, true);

                                g.drawShape(p, st);
                            }
                            if (stroke[BOTTOM].isVisible()) {
                                p.reset();
                                p.moveTo(x, y + h);
                                p.lineTo(x + w,y + h);
                                setColor(g, stroke[BOTTOM].color);
                                g.drawShape(p, stroke[BOTTOM].getStroke(c, contentRect, true));
                            }
                            if (stroke[LEFT].isVisible()) {
                                p.reset();
                                p.moveTo(x, y);
                                p.lineTo(x, y + h);
                                setColor(g, stroke[LEFT].color);
                                g.drawShape(p, stroke[LEFT].getStroke(c, contentRect, false));
                            }
                            if (stroke[RIGHT].isVisible()) {
                                p.reset();
                                p.moveTo(x + w, y);
                                p.lineTo(x + w, y + h);
                                setColor(g, stroke[RIGHT].color);
                                g.drawShape(p, stroke[RIGHT].getStroke(c, contentRect, false));
                            }
                        }
                    }
                }

            } finally {
                GeneralPath.recycle(p);
            }
        } finally {
            g.setAlpha(alpha);
            g.setColor(color);
            g.setAntiAliased(antialias);
        }
                       
       
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getMinimumHeight() {
        if (borderImage != null) {
            return borderImage.internal().getMinimumHeight();
        }
        
        return super.getMinimumHeight(); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getMinimumWidth() {
        if (borderImage != null) {
            return borderImage.internal().getMinimumWidth();
        }
        return super.getMinimumWidth();
    }
    
    
    
    
    /**
     * Creates a 9-piece image border.
     * 
     * <p>Insets are all given in a (u,v) coordinate space where (0,0) is the top-left corner of the image, and (1.0, 1.0) is the bottom-right corner of the image.</p>
     * <p>If a border image is set for the CSS border, it will override all other border types, and will result in only the 9-piece
     * border being rendered.</p>
     * 
     * @param borderImage The border image.
     * @param slicePoints The slice points.  Accepts 1 - 4 values:
     * <ul>
     *    <li>1 value = all sides</li>
     *    <li>2 values = vertical horizontal</li>
     *    <li>3 values = top horizontal bottom</li>
     *    <li>4 values = top right bottom left</li>
     * </ul>
     * @return Self for chaining.
     * @since 7.0
     * @see #borderImageWithName(java.lang.String, double...) 
     */
    public CSSBorder borderImage(Image borderImage, double... slicePoints) {
        this.borderImage = new BorderImage(borderImage, slicePoints);
        return this;
    }
    
    /**
     * Adds a 9-piece image border using the provided image name, which should exist in the
     * theme resource file.
     * <p>Insets are all given in a (u,v) coordinate space where (0,0) is the top-left corner of the image, and (1.0, 1.0) is the bottom-right corner of the image.</p>
     * <p>If a border image is set for the CSS border, it will override all other border types, and will result in only the 9-piece
     * border being rendered.</p>
     * @param borderImageName The image name.
     * @param slicePoints The slice points.  Accepts 1 - 4 values:
     * <ul>
     *    <li>1 value = all sides</li>
     *    <li>2 values = vertical horizontal</li>
     *    <li>3 values = top horizontal bottom</li>
     *    <li>4 values = top right bottom left</li>
     * </ul>
     * @return Self for chaining.
     * @since 7.0
     * @see #borderImage(com.codename1.ui.Image, double...) 
     */
    public CSSBorder borderImageWithName(String borderImageName, double... slicePoints) {
        this.borderImage = new BorderImage(borderImageName, slicePoints);
        return this;
    }
    
    
    private CSSBorder borderImage(String cssProperty) {
        String[] parts = Util.split(cssProperty, " ");
        parts[0] = Util.decode(parts[0], "UTF-8", false);
        
        int len = parts.length;
        double[] splices = new double[len-1];
        for (int i=1; i<len; i++) {
            splices[i-1] = Double.parseDouble(parts[i]);
        }
        
        return borderImageWithName(parts[0], splices);
    }
    
    /**
     * Sets the border radius for rounded corners.
     * @param radius
     * @return 
     */
    public CSSBorder borderRadius(String radius) {
        borderRadius = new BorderRadius(radius);
        return this;
    }
    
    /**
     * Sets the border stroke.
     * @param strokeStrs
     * @return 
     */
    public CSSBorder borderStroke(String... strokeStrs) {
        this.stroke = new BorderStroke[4];
        int len = strokeStrs.length;
        if (len == 4) {
            this.stroke[TOP] = new BorderStroke(strokeStrs[0]);
            this.stroke[RIGHT] = new BorderStroke(strokeStrs[1]);
            this.stroke[BOTTOM] = new BorderStroke(strokeStrs[2]);
            this.stroke[LEFT] = new BorderStroke(strokeStrs[3]);
        } else if (len == 2) {
            this.stroke[TOP] = new BorderStroke(strokeStrs[0]);
            this.stroke[BOTTOM] = new BorderStroke(this.stroke[TOP]);
            this.stroke[LEFT] = new BorderStroke(strokeStrs[1]);
            this.stroke[RIGHT] = new BorderStroke(this.stroke[LEFT]);
        } else if (len == 3) {
            this.stroke[TOP] = new BorderStroke(strokeStrs[0]);
            this.stroke[LEFT] = new BorderStroke(strokeStrs[1]);
            this.stroke[RIGHT] = new BorderStroke(this.stroke[LEFT]);
            this.stroke[BOTTOM] = new BorderStroke(strokeStrs[2]);
        } else if (len == 1) {
            this.stroke[TOP] = new BorderStroke(strokeStrs[0]);
            this.stroke[RIGHT] = new BorderStroke(this.stroke[TOP]);
            this.stroke[BOTTOM] = new BorderStroke(this.stroke[TOP]);
            this.stroke[LEFT] = new BorderStroke(this.stroke[TOP]);
        } else {
            throw new IllegalArgumentException("Border stroke expects 1 to 4 parameters for top, right, bottom, left");
        }
        return this;
        
    }
    
    /**
     * Sets the border colors.
     * @param colors The colors.  1 value sets all borders.  2 sets top/bottom, left/right.  3 sets top, left/right, bottom.  4 sets top, right, bottom, left.
     * @return Self for chaining.
     */
    public CSSBorder borderColor(String... colors) {
        if (this.stroke == null) {
            return this.borderStroke("solid").borderColor(colors);
        } else {
            int len = colors.length;
            if (len == 1 && colors[0].indexOf(" ") != -1) {
                return borderColor(Util.split(colors[0], " "));
            }
            if (len == 4) {
                stroke[TOP].color = Color.parse(colors[0]);
                stroke[RIGHT].color = Color.parse(colors[1]);
                stroke[BOTTOM].color = Color.parse(colors[2]);
                stroke[LEFT].color = Color.parse(colors[3]);
            } else if (len == 3) {
                stroke[TOP].color = Color.parse(colors[0]);
                stroke[RIGHT].color = Color.parse(colors[1]);
                stroke[LEFT].color = Color.parse(colors[1]);
                stroke[BOTTOM].color = Color.parse(colors[2]);
            } else if (len == 2) {
                stroke[TOP].color = stroke[BOTTOM].color = Color.parse(colors[0]);
                stroke[LEFT].color = stroke[RIGHT].color = Color.parse(colors[1]);
            } else if (len == 1) {
                stroke[TOP].color = stroke[BOTTOM].color = stroke[LEFT].color = stroke[RIGHT].color = Color.parse(colors[0]);
            } else {
                throw new IllegalArgumentException("borderColor expects 1-4 parameters");
            }
        }
        return this;
    }
    
    /**
     * Sets the border widths.
     * @param widths The widths. 1 value sets all borders.  2 sets top/bottom, left/right.  3 sets top, left/right, bottom.  4 sets top, right, bottom, left.
     * @return Self for chaining.
     */
    public CSSBorder borderWidth(String... widths) {
        if (this.stroke == null) {
            return this.borderStroke("solid").borderWidth(widths);
        } else {
            int len = widths.length;
            if (len == 4) {
                stroke[TOP].thickness = BorderStroke.parseThickness(widths[0]);
                stroke[RIGHT].thickness = BorderStroke.parseThickness(widths[1]);
                stroke[BOTTOM].thickness = BorderStroke.parseThickness(widths[2]);
                stroke[LEFT].thickness = BorderStroke.parseThickness(widths[3]);
            } else if (len == 3) {
                stroke[TOP].thickness = BorderStroke.parseThickness(widths[0]);
                stroke[LEFT].thickness = BorderStroke.parseThickness(widths[1]);
                stroke[RIGHT].thickness = stroke[LEFT].thickness.copy();
                stroke[BOTTOM].thickness = BorderStroke.parseThickness(widths[2]);
            } else if (len == 2) {
                stroke[TOP].thickness = BorderStroke.parseThickness(widths[0]);
                stroke[BOTTOM].thickness = stroke[TOP].thickness.copy();
                stroke[LEFT].thickness = BorderStroke.parseThickness(widths[1]);
                stroke[RIGHT].thickness = stroke[LEFT].thickness.copy();
            } else if (len == 1) {
                if (widths[0].indexOf(" ") != -1) {
                    return borderWidth(Util.split(widths[0], " "));
                }
                stroke[TOP].thickness = BorderStroke.parseThickness(widths[0]);
                stroke[RIGHT].thickness = BorderStroke.parseThickness(widths[0]);
                stroke[BOTTOM].thickness = BorderStroke.parseThickness(widths[0]);
                stroke[LEFT].thickness = BorderStroke.parseThickness(widths[0]);
            } else {
                throw new IllegalArgumentException("Border width expects 1 to 4 parameters");
            }
            
        }
        return this;
    }
    
    /**
     * Sets the border styles.  Supported styles: none, hidden, dotted, dashed, solid.
     * @param styles The border styles.  1 value sets all borders.  2 sets top/bottom, left/right.  3 sets top, left/right, bottom.  4 sets top, right, bottom, left.
     * @return Self for chaining.
     */
    public CSSBorder borderStyle(String... styles) {
        try {
            if (stroke == null) {
                return borderStroke("solid").borderStyle(styles);
            } else {
                int len = styles.length;
                switch (len) {
                    case 4:
                        stroke[TOP].type = getBorderStyle(styles[0]);
                        stroke[RIGHT].type= getBorderStyle(styles[1]);
                        stroke[BOTTOM].type = getBorderStyle(styles[2]);
                        stroke[LEFT].type = getBorderStyle(styles[3]);
                        break;
                    case 3:
                        stroke[TOP].type = getBorderStyle(styles[0]);
                        stroke[RIGHT].type= getBorderStyle(styles[1]);
                        stroke[BOTTOM].type = getBorderStyle(styles[2]);
                        stroke[LEFT].type = getBorderStyle(styles[1]);
                        break;
                    case 2:
                        stroke[TOP].type = getBorderStyle(styles[0]);
                        stroke[RIGHT].type= getBorderStyle(styles[1]);
                        stroke[BOTTOM].type = getBorderStyle(styles[0]);
                        stroke[LEFT].type = getBorderStyle(styles[1]);
                        break;
                    case 1:
                        if (styles[0].indexOf(" ") != -1) {
                            return borderStyle(Util.split(styles[0], " "));
                        }
                        stroke[TOP].type = getBorderStyle(styles[0]);
                        stroke[RIGHT].type= getBorderStyle(styles[0]);
                        stroke[BOTTOM].type = getBorderStyle(styles[0]);
                        stroke[LEFT].type = getBorderStyle(styles[0]);
                        break;
                    default:
                        throw new IllegalArgumentException("borderSTyle expects 1 to 4 arguments");
                }
            }
        } catch (Throwable t) {
            Log.e(t);
            throw new RuntimeException("Failed parsing border style: "+Arrays.toString(styles));
        }
        return this;
    }
    
    /**
     * Sets the background color of the border.
     * @param color A color string.
     * @return Self for chaining.
     */
    public CSSBorder backgroundColor(String color) {
        backgroundColor = Color.parse(color);
        return this;
    }
    
    /**
     * Adds one or more background images from a CSS background-image property.
     * @param cssDirective The value of the background-image property.
     * @return Self for chaining.
     */
    public CSSBorder backgroundImage(String cssDirective) {
        String[] parts = Util.split(cssDirective, ",");
        List<Image> imgs = new ArrayList<Image>();
        for (String part : parts) {
            part = part.trim();
            if (part.indexOf("url(") == 0) {
                part = part.substring(4, part.length()-1);
            }
            if (part.charAt(0) == '"' || part.charAt(0) == '"') {
                part = part.substring(1, part.length()-1);
            }
            if (part.indexOf("/") != -1) {
                part = part.substring(part.lastIndexOf("/")+1);
            }
            Image im = res.getImage(part);
            if (im == null) {
                try {
                    im = EncodedImage.create("/"+part);
                    im.setImageName(part);
                    
                } catch (IOException ex) {
                    Log.e(ex);
                    throw new IllegalArgumentException("Failed to parse image: "+part);
                }
            }
            imgs.add(im);
            
        }
        return backgroundImage(imgs.toArray(new Image[imgs.size()]));
        
    }
    
    /**
     * Sets the background image of the border.
     * @param images Images to use as background images.
     * @return Self for chaining.
     */
    public CSSBorder backgroundImage(Image... images) {
        int len = images.length;
        if (backgroundImages == null) {
            backgroundImages = new BackgroundImage[len];
            
        } else {
            if (backgroundImages.length < len) {
                BackgroundImage[] tmp = new BackgroundImage[len];
                System.arraycopy(backgroundImages, 0, tmp, 0, backgroundImages.length);
                backgroundImages = tmp;
                
            }
        }
        
        for (int i=0; i<len; i++) {
            if (backgroundImages[i] == null) {
                backgroundImages[i] = new BackgroundImage(images[i]);
            } else {
                backgroundImages[i].image = images[i];
            }
        }
        
        return this;
    }
    
    static byte parseRepeat(String repeat) {
        if ("repeat-x".equals(repeat)) {
            return REPEAT_X;
        } else if ("repeat-y".equals(repeat)) {
            return REPEAT_Y;
        } else if ("repeat".equals(repeat)) {
            return REPEAT_BOTH;
        } else if ("repeat-none".equals(repeat)) {
            return REPEAT_NONE;
        } else {
            throw new IllegalArgumentException("Unrecognized option for background-repeat");
        }
            
        
    }
    
    /**
     * Sets the background-repeat for the background images.
     * @param repeat Repeat options for respective background images.
     * @return Self for chaining.
     */
    public CSSBorder backgroundRepeat(String... repeat) {
        int len = repeat.length;
        if (len == 1 && repeat[0].indexOf(",") != -1) {
            return backgroundRepeat(Util.split(repeat[0], ","));
        }
        if (backgroundImages == null) {
            backgroundImages = new BackgroundImage[len];
        } else {
            if (backgroundImages.length < len) {
                BackgroundImage[] tmp = new BackgroundImage[len];
                System.arraycopy(backgroundImages, 0, tmp, 0, backgroundImages.length);
                backgroundImages = tmp;
            }
        }
        for (int i=0; i<len; i++) {
            if (backgroundImages[i] == null) {
                backgroundImages[i] = new BackgroundImage();
                
            }
            backgroundImages[i].repeat = parseRepeat(repeat[i].trim());
        }
        return this;
    }
    
    /**
     * Sets the background position.
     * @param pos The background positions of background images.
     * @return Self for chaining.
     */
    public CSSBorder backgroundPosition(String... pos) {
        
        int len = pos.length;
        if (len == 1 && pos[0].indexOf(",") != -1) {
            return backgroundPosition(Util.split(pos[0], ","));
        }
        if (backgroundImages == null) {
            backgroundImages = new BackgroundImage[len];
        } else {
            if (backgroundImages.length < len) {
                BackgroundImage[] tmp = new BackgroundImage[len];
                System.arraycopy(backgroundImages, 0, tmp, 0, backgroundImages.length);
                backgroundImages = tmp;
            }
        }
        for (int i=0; i<len; i++) {
            if (backgroundImages[i] == null) {
                backgroundImages[i] = new BackgroundImage();
                
            }
            backgroundImages[i].setPosition(pos[i].trim());
        }
        return this;
    }
    
    
    
}
