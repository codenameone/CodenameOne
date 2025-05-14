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

import org.apache.maven.model.jdom.util.JDomUtils;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.filter.ElementFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import static java.util.Arrays.asList;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_MODULE;
import static org.apache.maven.model.jdom.util.JDomUtils.detectIndentation;

/**
 * JDOM implementation of POMs {@code modules} element.
 *
 * @author Marc Rohlfs, CoreMedia AG
 */
public class JDomModules extends ArrayList<String> implements JDomBacked {

  private final Element jdomElement;

  public JDomModules(Element jdomElement) {
    super(transformToElementTextList(getModuleElements(jdomElement)));
    this.jdomElement = jdomElement;
  }

  private static List<Element> getModuleElements(Element modules) {
    return modules.getContent(new ElementFilter(modules.getNamespace()));
  }

  private static List<String> transformToElementTextList(List<Element> elements) {
    List<String> elementTextList = new ArrayList<>(elements.size());
    for (Element element : elements) {
      elementTextList.add(element.getTextTrim());
    }
    return elementTextList;
  }

  @Override
  public boolean add(String module) {
    Element newModule = new Element(POM_ELEMENT_MODULE, jdomElement.getNamespace());
    newModule.setText(module);

    jdomElement.addContent(
            jdomElement.getContentSize() - 1,
            asList(
                    new Text("\n" + detectIndentation(jdomElement)),
                    newModule));
    return super.add(module);
  }

  @Override
  public boolean remove(final Object module) {
    List<Element> removeElements = jdomElement.getContent(new ElementFilter() {
      @Override
      public Element filter(Object content) {
        Element element = super.filter(content);
        return element == null || !module.equals(element.getTextTrim()) ? null : element;
      }
    });

    for (Element removeElement : removeElements) {
      JDomUtils.removeChildAndItsCommentFromContent(jdomElement, removeElement);
    }

    return super.remove(module);
  }

  @Override
  public boolean addAll(Collection<? extends String> modules) {
    boolean added = false;
    for (String module : modules) {
      added |= this.add(module);
    }
    return added;
  }

  @Override
  public boolean addAll(int index, Collection<? extends String> modules) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> modules) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> modules) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    while (size() > 0) {
      remove(0);
    }
  }

  @Override
  public String set(int index, String module) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(int index, String module) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String remove(int index) {
    String module = get(index);
    remove(module);
    return module;
  }

  @Override
  public int lastIndexOf(Object module) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<String> listIterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<String> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object clone() {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
