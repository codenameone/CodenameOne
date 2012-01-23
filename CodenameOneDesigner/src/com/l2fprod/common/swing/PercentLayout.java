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
package com.l2fprod.common.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * PercentLayout. <BR>Constraint based layout which allow the space to be
 * splitted using percentages. The following are allowed when adding components
 * to container:
 * <ul>
 * <li>container.add(component); <br>in this case, the component will be
 * sized to its preferred size
 * <li>container.add(component, "100"); <br>in this case, the component will
 * have a width (or height) of 100
 * <li>container.add(component, "25%"); <br>in this case, the component will
 * have a width (or height) of 25 % of the container width (or height) <br>
 * <li>container.add(component, "*"); <br>in this case, the component will
 * take the remaining space. if several components use the "*" constraint the
 * space will be divided among the components.
 * </ul>
 * 
 * @javabean.class
 *          name="PercentLayout"
 *          shortDescription="A layout supports constraints expressed in percent."
 */
public class PercentLayout implements LayoutManager2 {

  /**
   * Useful constant to layout the components horizontally (from left to right).
   */
  public final static int HORIZONTAL = 0;

  /**
   * Useful constant to layout the components vertically (from top to bottom).
   */
  public final static int VERTICAL = 1;

  static class Constraint {
    protected Object value;
    private Constraint(Object value) {
      this.value = value;
    }
  }

  static class NumberConstraint extends Constraint {
    public NumberConstraint(int d) {
      this(new Integer(d));
    }
    public NumberConstraint(Integer d) {
      super(d);
    }
    public int intValue() {
      return ((Integer)value).intValue();
    }
  }

  static class PercentConstraint extends Constraint {
    public PercentConstraint(float d) {
      super(new Float(d));
    }
    public float floatValue() {
      return ((Float)value).floatValue();
    }
  }

  private final static Constraint REMAINING_SPACE = new Constraint("*");

  private final static Constraint PREFERRED_SIZE = new Constraint("");

  private int orientation;
  private int gap;

  private Hashtable m_ComponentToConstraint;

  /**
   * Creates a new HORIZONTAL PercentLayout with a gap of 0.
   */
  public PercentLayout() {
    this(HORIZONTAL, 0);
  }

  public PercentLayout(int orientation, int gap) {
    setOrientation(orientation);
    this.gap = gap;

    m_ComponentToConstraint = new Hashtable();
  }

  public void setGap(int gap) {
    this.gap = gap;   
  }
  
  /**
   * @javabean.property
   *          bound="true"
   *          preferred="true"
   */
  public int getGap() {
    return gap;
  }
  
  public void setOrientation(int orientation) {
    if (orientation != HORIZONTAL && orientation != VERTICAL) {
      throw new IllegalArgumentException("Orientation must be one of HORIZONTAL or VERTICAL");
    }
    this.orientation = orientation;
  }

  /**
   * @javabean.property
   *          bound="true"
   *          preferred="true"
   */
  public int getOrientation() {
    return orientation;
  }
  
  public Constraint getConstraint(Component component) {
    return (Constraint)m_ComponentToConstraint.get(component);
  }
  
  public void setConstraint(Component component, Object constraints) {
    if (constraints instanceof Constraint) {
      m_ComponentToConstraint.put(component, constraints);
    } else if (constraints instanceof Number) {
      setConstraint(
        component,
        new NumberConstraint(((Number)constraints).intValue()));
    } else if ("*".equals(constraints)) {
      setConstraint(component, REMAINING_SPACE);
    } else if ("".equals(constraints)) {
      setConstraint(component, PREFERRED_SIZE);
    } else if (constraints instanceof String) {
      String s = (String)constraints;
      if (s.endsWith("%")) {
        float value = Float.valueOf(s.substring(0, s.length() - 1))
          .floatValue() / 100;
        if (value > 1 || value < 0)
          throw new IllegalArgumentException("percent value must be >= 0 and <= 100");
        setConstraint(component, new PercentConstraint(value));
      } else {
        setConstraint(component, new NumberConstraint(Integer.valueOf(s)));
      }
    } else if (constraints == null) {
      // null constraint means preferred size
      setConstraint(component, PREFERRED_SIZE);
    } else {
      throw new IllegalArgumentException("Invalid Constraint");
    }    
  }
  
  public void addLayoutComponent(Component component, Object constraints) {
    setConstraint(component, constraints);    
  }

  /**
   * Returns the alignment along the x axis. This specifies how the component
   * would like to be aligned relative to other components. The value should be
   * a number between 0 and 1 where 0 represents alignment along the origin, 1
   * is aligned the furthest away from the origin, 0.5 is centered, etc.
   */
  public float getLayoutAlignmentX(Container target) {
    return 1.0f / 2.0f;
  }

  /**
   * Returns the alignment along the y axis. This specifies how the component
   * would like to be aligned relative to other components. The value should be
   * a number between 0 and 1 where 0 represents alignment along the origin, 1
   * is aligned the furthest away from the origin, 0.5 is centered, etc.
   */
  public float getLayoutAlignmentY(Container target) {
    return 1.0f / 2.0f;
  }

  /**
   * Invalidates the layout, indicating that if the layout manager has cached
   * information it should be discarded.
   */
  public void invalidateLayout(Container target) {
  }

  /**
   * Adds the specified component with the specified name to the layout.
   * 
   * @param name the component name
   * @param comp the component to be added
   */
  public void addLayoutComponent(String name, Component comp) {
  }

  /**
   * Removes the specified component from the layout.
   * 
   * @param comp the component ot be removed
   */
  public void removeLayoutComponent(Component comp) {
    m_ComponentToConstraint.remove(comp);
  }

  /**
   * Calculates the minimum size dimensions for the specified panel given the
   * components in the specified parent container.
   * 
   * @param parent the component to be laid out
   * @see #preferredLayoutSize
   */
  public Dimension minimumLayoutSize(Container parent) {
    return preferredLayoutSize(parent);
  }

  /**
   * Returns the maximum size of this component.
   * 
   * @see java.awt.Component#getMinimumSize()
   * @see java.awt.Component#getPreferredSize()
   * @see java.awt.LayoutManager
   */
  public Dimension maximumLayoutSize(Container parent) {
    return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  public Dimension preferredLayoutSize(Container parent) {
    Component[] components = parent.getComponents();
    Insets insets = parent.getInsets();
    int width = 0;
    int height = 0;
    Dimension componentPreferredSize;
    boolean firstVisibleComponent = true;
    for (int i = 0, c = components.length; i < c; i++) {
      if (components[i].isVisible()) {
        componentPreferredSize = components[i].getPreferredSize();
        if (orientation == HORIZONTAL) {
          height = Math.max(height, componentPreferredSize.height);
          width += componentPreferredSize.width;
          if (firstVisibleComponent) {
            firstVisibleComponent = false;
          } else {
            width += gap;
          }
        } else {
          height += componentPreferredSize.height;
          width = Math.max(width, componentPreferredSize.width);
          if (firstVisibleComponent) {
            firstVisibleComponent = false;
          } else {
            height += gap;
          }
        }
      }
    }
    return new Dimension(
      width + insets.right + insets.left,
      height + insets.top + insets.bottom);
  }

  public void layoutContainer(Container parent) {
    Insets insets = parent.getInsets();
    Dimension d = parent.getSize();

    // calculate the available sizes
    d.width = d.width - insets.left - insets.right;
    d.height = d.height - insets.top - insets.bottom;

    // pre-calculate the size of each components
    Component[] components = parent.getComponents();
    int[] sizes = new int[components.length];

    // calculate the available size
    int totalSize =
      (HORIZONTAL == orientation ? d.width : d.height)
        - (components.length - 1) * gap;
    int availableSize = totalSize;

    // PENDING(fred): the following code iterates 4 times on the component
    // array, need to find something more efficient!
       
    // give priority to components who want to use their preferred size or who
    // have a predefined size
    for (int i = 0, c = components.length; i < c; i++) {
      if (components[i].isVisible()) {
        Constraint constraint =
          (Constraint)m_ComponentToConstraint.get(components[i]);
        if (constraint == null || constraint == PREFERRED_SIZE) {
          sizes[i] =
            (HORIZONTAL == orientation
              ? components[i].getPreferredSize().width
              : components[i].getPreferredSize().height);
          availableSize -= sizes[i];
        } else if (constraint instanceof NumberConstraint) {
          sizes[i] = ((NumberConstraint)constraint).intValue();
          availableSize -= sizes[i];
        }
      }
    }
    
    // then work with the components who want a percentage of the remaining
    // space
    int remainingSize = availableSize;    
    for (int i = 0, c = components.length; i < c; i++) {
      if (components[i].isVisible()) {
        Constraint constraint =
          (Constraint)m_ComponentToConstraint.get(components[i]);
        if (constraint instanceof PercentConstraint) {
          sizes[i] = (int)(remainingSize * ((PercentConstraint)constraint)
            .floatValue());
          availableSize -= sizes[i];
        }
      }
    }
    
    // finally share the remaining space between the other components    
    ArrayList remaining = new ArrayList();
    for (int i = 0, c = components.length; i < c; i++) {
      if (components[i].isVisible()) {
        Constraint constraint =
          (Constraint)m_ComponentToConstraint.get(components[i]);
        if (constraint == REMAINING_SPACE) {
          remaining.add(new Integer(i));
          sizes[i] = 0;
        }
      }
    }

    if (remaining.size() > 0) {
      int rest = availableSize / remaining.size();
      for (Iterator iter = remaining.iterator(); iter.hasNext();) {
        sizes[((Integer)iter.next()).intValue()] = rest;
      }
    }

    // all calculations are done, apply the sizes
    int currentOffset = (HORIZONTAL == orientation ? insets.left : insets.top);

    for (int i = 0, c = components.length; i < c; i++) {
      if (components[i].isVisible()) {
        if (HORIZONTAL == orientation) {
          components[i].setBounds(
            currentOffset,
            insets.top,
            sizes[i],
            d.height);
        } else {
          components[i].setBounds(
            insets.left,
            currentOffset,
            d.width,
            sizes[i]);
        }
        currentOffset += gap + sizes[i];
      }
    }
  }

}
