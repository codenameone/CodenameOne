package org.apache.maven.model.jdom;

/*
 * Copyright 2018 CoreMedia AG, Hamburg
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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.jdom.util.JDomUtils;
import org.codehaus.plexus.util.StringUtils;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_DEPENDENCY;
import static org.apache.maven.model.jdom.util.JDomUtils.addElement;
import static org.apache.maven.model.jdom.util.JDomUtils.getElementIndex;
import static org.apache.maven.model.jdom.util.JDomUtils.insertNewElement;
import static org.codehaus.plexus.util.StringUtils.defaultString;

/**
 * JDOM implementation of POMs {@code dependencies} element.
 *
 * @author Marc Rohlfs, CoreMedia AG
 */
public class JDomDependencies extends ArrayList<Dependency> implements JDomBacked {

  private final Element jdomElement;

  private final JDomBacked parent;

  public JDomDependencies(Element jdomElement, JDomBacked parent) {
    super(transformDependencyElementsToJDomDependencyList(jdomElement));
    this.jdomElement = jdomElement;
    this.parent = parent;
  }

  private static List<JDomDependency> transformDependencyElementsToJDomDependencyList(Element jdomElement) {
    Filter<Element> dependencyElementFilter = new ElementFilter(POM_ELEMENT_DEPENDENCY, jdomElement.getNamespace());
    List<Element> dependencyElements = jdomElement.getContent(dependencyElementFilter);

    List<JDomDependency> jDomDependencyList = new ArrayList<>(dependencyElements.size());
    for (Element dependencyElement : dependencyElements) {
      jDomDependencyList.add(new JDomDependency(dependencyElement));
    }
    return jDomDependencyList;
  }

  @Override
  public boolean add(Dependency dependency) {
    add(size(), dependency);
    return true;
  }

  @Override
  public boolean remove(final Object dependency) {
    Dependency removeDependency = (Dependency) dependency;
    for (Dependency candidate : this) {
      if (StringUtils.equals(candidate.getGroupId(), removeDependency.getGroupId())
              && StringUtils.equals(candidate.getArtifactId(), removeDependency.getArtifactId())
              && defaultString(candidate.getType(), "jar").equals(defaultString(removeDependency.getType(), "jar"))) {
        JDomUtils.removeChildAndItsCommentFromContent(jdomElement, ((JDomDependency) candidate).getJDomElement());

        boolean remove = super.remove(candidate);
        if (super.isEmpty()) {
          if (parent instanceof JDomDependencyManagement) {
            JDomBacked parentOfDependencyManagement = ((JDomDependencyManagement) parent).getParent();
            JDomUtils.removeChildAndItsCommentFromContent(parentOfDependencyManagement.getJDomElement(), this.parent.getJDomElement());
          } else {
            JDomUtils.removeChildAndItsCommentFromContent(parent.getJDomElement(), jdomElement);
          }
        }
        return remove;
      }
    }
    return false;
  }

  @Override
  public boolean addAll(Collection<? extends Dependency> dependencies) {
    boolean added = false;
    for (Dependency dependency : dependencies) {
      added |= this.add(dependency);
    }
    return added;
  }

  @Override
  public boolean addAll(int index, Collection<? extends Dependency> dependencies) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> dependencies) {
    boolean result = false;
    for (Object dependency : dependencies) {
      result |= remove(dependency);
    }
    return result;
  }

  @Override
  public boolean retainAll(Collection<?> dependencies) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    while (size() > 0) {
      remove(0);
    }
  }

  @Override
  public Dependency set(int index, Dependency dependency) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(int index, Dependency dependency) {
    if (index > size() || index < 0) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
    }

    int elementIndex = index;
    if (index > 0) {
      Element previousElement = ((JDomDependency) get(index - 1)).getJDomElement();
      elementIndex = 1 + getElementIndex(previousElement, jdomElement);
    }

    if (jdomElement.getParent() == null) {
      addElement(jdomElement, parent.getJDomElement());
    }
    if (parent instanceof JDomDependencyManagement) {
      if (parent.getJDomElement().getParent() == null) {
        addElement(this.parent.getJDomElement(), ((JDomDependencyManagement) parent).getParent().getJDomElement());
      }
    }

    JDomDependency jdomDependency;
    if (dependency instanceof JDomDependency) {
      jdomDependency = (JDomDependency) dependency;
      addElement(jdomDependency.getJDomElement().clone(), jdomElement, elementIndex);
    } else {
      Element newElement = insertNewElement(POM_ELEMENT_DEPENDENCY, jdomElement, elementIndex);
      jdomDependency = new JDomDependency(newElement, dependency);
    }

    super.add(index, jdomDependency);
  }

  @Override
  public Dependency remove(int index) {
    Dependency dependency = get(index);
    remove(dependency);
    return dependency;
  }

  @Override
  public int lastIndexOf(Object dependency) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<Dependency> listIterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<Dependency> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Dependency> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object clone() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
