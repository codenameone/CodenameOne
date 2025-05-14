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

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.filter.ElementFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.maven.model.jdom.util.JDomUtils.insertNewElement;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteValue;

/**
 * JDOM implementation of POM plugins {@code configuration} element.
 *
 * @author Marc Rohlfs, CoreMedia AG
 */
public class JDomConfiguration extends Xpp3Dom implements JDomBacked {

  private final Element jdomElement;

  private List<JDomConfiguration> children;

  public JDomConfiguration(Element jdomElement) {
    super(jdomElement.getName());
    this.jdomElement = jdomElement;
    this.children = getChildren(jdomElement);
  }

  private static List<JDomConfiguration> getChildren(Element jdomElement) {
    List<Element> childElements = jdomElement.getContent(new ElementFilter());
    List<JDomConfiguration> children = new ArrayList<>(childElements.size());
    for (Element childElement : childElements) {
      children.add(new JDomConfiguration(childElement));
    }
    return children;
  }

  @Override
  public void addChild(Xpp3Dom xpp3Dom) {
    recAddChild(jdomElement, xpp3Dom);
    children = getChildren(jdomElement);
  }

  private void recAddChild(Element parent, Xpp3Dom child) {
    Element targetChild = insertNewElement(child.getName(), parent);
    targetChild.setContent(new Text(child.getValue()));
    for (String attrName : child.getAttributeNames()) {
      targetChild.setAttribute(attrName, child.getAttribute(attrName));
    }
    for (Xpp3Dom grandChild : child.getChildren()) {
      recAddChild(targetChild, grandChild);
    }
  }

  @Override
  public String getValue() {
    return getJDomElement().getContent().stream()
            .map(Content::getValue)
            .collect(Collectors.joining());
  }

  @Override
  public void setValue(String value) {
    rewriteValue(jdomElement, value);
  }

  @Override
  public String[] getAttributeNames() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getAttribute(String name) {
    return jdomElement.getAttributeValue(name);
  }

  @Override
  public void setAttribute(String name, String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Xpp3Dom getChild(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Xpp3Dom getChild(String name) {
    return children.stream().filter(c -> name.equals(c.getName())).findFirst().orElse(null);
  }

  public void setConfigurationProperty(String propertyName, String value) {
    Element propertyElement = null;
    for (Element element : jdomElement.getContent(new ElementFilter())) {
      if (propertyName.equals(element.getName())) {
        propertyElement = element;
        break;
      }
    }
    if (propertyElement == null) {
      propertyElement = insertNewElement(propertyName, jdomElement);
      children.add(new JDomConfiguration(propertyElement));
    }
    propertyElement.setText(value);
  }

  @Override
  public Xpp3Dom[] getChildren() {
    return children.toArray(new JDomConfiguration[]{});
  }

  @Override
  public Xpp3Dom[] getChildren(String name) {
    return children.stream()
            .filter(c -> name.equals(c.getName()))
            .collect(Collectors.toList())
            .toArray(new Xpp3Dom[]{});
  }

  @Override
  public int getChildCount() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeChild(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Xpp3Dom getParent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setParent(Xpp3Dom parent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeToSerializer(String namespace, XmlSerializer serializer) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toUnescapedString() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "JDomConfiguration{" +
            "jdomElement=" + jdomElement +
            '}';
  }

  /** {@inheritDoc} */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
