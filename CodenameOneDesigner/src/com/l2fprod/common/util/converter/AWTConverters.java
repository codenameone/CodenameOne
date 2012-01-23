/**
 * @PROJECT.FULLNAME@ @VERSION@ License.
 *
 * Copyright @YEAR@ L2FProd.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.l2fprod.common.util.converter;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.StringTokenizer;

import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;

/**
 * AWTConverters. <br>Converter commonly used AWT classes like Point,
 * Dimension, Rectangle, Insets to/from Strings and between each others when
 * possible.
 * 
 * The following convertions are supported:
 * 
 * <table>
 * <tr>
 * <th>From</th>
 * <th>To</th>
 * <th>Reverse</th>
 * </tr>
 * <tr>
 * <td>Dimension</td>
 * <td>String</td>
 * <td>yes</td>
 * </tr>
 * <tr>
 * <td>Font</td>
 * <td>String</td>
 * <td>no</td>
 * </tr>
 * <tr>
 * <td>Insets</td>
 * <td>String</td>
 * <td>yes</td>
 * </tr>
 * <tr>
 * <td>Point</td>
 * <td>String</td>
 * <td>yes</td>
 * </tr>
 * <tr>
 * <td>Rectangle</td>
 * <td>String</td>
 * <td>yes</td>
 * </tr>
 * </table>
 */
public class AWTConverters implements Converter {

  public AWTConverters() {
    super();
  }

  public void register(ConverterRegistry registry) {
    registry.addConverter(Dimension.class, String.class, this);
    registry.addConverter(String.class, Dimension.class, this);
    registry.addConverter(DimensionUIResource.class, String.class, this);

    registry.addConverter(Insets.class, String.class, this);
    registry.addConverter(String.class, Insets.class, this);
    registry.addConverter(InsetsUIResource.class, String.class, this);

    registry.addConverter(Point.class, String.class, this);
    registry.addConverter(String.class, Point.class, this);
    
    registry.addConverter(Rectangle.class, String.class, this);
    registry.addConverter(String.class, Rectangle.class, this);
    
    registry.addConverter(Font.class, String.class, this);
    registry.addConverter(FontUIResource.class, String.class, this);
  }

  public Object convert(Class type, Object value) {
    if (String.class.equals(type)) {
      if (value instanceof Rectangle) {
        return ((Rectangle)value).getX()
          + " "
          + ((Rectangle)value).getY()
          + " "
          + ((Rectangle)value).getWidth()
          + " "
          + ((Rectangle)value).getHeight();
      } else if (value instanceof Insets) {
        return ((Insets)value).top
          + " "
          + ((Insets)value).left
          + " "
          + ((Insets)value).bottom
          + " "
          + ((Insets)value).right;
      } else if (value instanceof Dimension) {
        return ((Dimension)value).getWidth()
          + " x "
          + ((Dimension)value).getHeight();
      } else if (Point.class.equals(value.getClass())) {
        return ((Point)value).getX() + " " + ((Point)value).getY();
      } else if (value instanceof Font) {
        return ((Font)value).getFontName()
          + ", "
          + ((Font)value).getStyle()
          + ", "
          + ((Font)value).getSize();
      }
    }

    if (value instanceof String) {
      if (Rectangle.class.equals(type)) {
        double[] values = convert((String)value, 4, " ");
        if (values == null) {
          throw new IllegalArgumentException("Invalid format");
        }
        Rectangle rect = new Rectangle();
        rect.setFrame(values[0], values[1], values[2], values[3]);
        return rect;
      } else if (Insets.class.equals(type)) {
        double[] values = convert((String)value, 4, " ");
        if (values == null) {
          throw new IllegalArgumentException("Invalid format");
        }
        return new Insets(
          (int)values[0],
          (int)values[1],
          (int)values[2],
          (int)values[3]);
      } else if (Dimension.class.equals(type)) {
        double[] values = convert((String)value, 2, "x");
        if (values == null) {
          throw new IllegalArgumentException("Invalid format");
        }
        Dimension dim = new Dimension();
        dim.setSize(values[0], values[1]);
        return dim;
      } else if (Point.class.equals(type)) {
        double[] values = convert((String)value, 2, " ");
        if (values == null) {
          throw new IllegalArgumentException("Invalid format");
        }
        Point p = new Point();
        p.setLocation(values[0], values[1]);
        return p;
      }
    }
    return null;
  }

  private double[] convert(String text, int tokenCount, String delimiters) {
    StringTokenizer tokenizer = new StringTokenizer(text, delimiters);
    if (tokenizer.countTokens() != tokenCount) {
      return null;
    }

    try {
      double[] values = new double[tokenCount];
      for (int i = 0; tokenizer.hasMoreTokens(); i++) {
        values[i] = Double.parseDouble(tokenizer.nextToken());
      }
      return values;
    } catch (Exception e) {
      return null;
    }
  }

}
