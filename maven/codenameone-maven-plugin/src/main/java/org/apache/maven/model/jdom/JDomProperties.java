package org.apache.maven.model.jdom;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.maven.model.jdom.util.JDomUtils.getChildElementTextTrim;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteElement;

/**
 * JDom implementation of poms PROPERTIES element
 *
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 */
public class JDomProperties extends Properties implements JDomBacked {

  private final Element jdomElement;

  public JDomProperties(Element jdomElement) {
    this.jdomElement = jdomElement;
  }

  @Override
  public Set<Map.Entry<Object, Object>> entrySet() {
    JDomPropertiesSet entrySet = new JDomPropertiesSet();

    for (Element property : jdomElement.getContent(new ElementFilter(jdomElement.getNamespace()))) {
      entrySet.addProperty(new JDomProperty(property));
    }

    return entrySet;
  }

  @Override
  public synchronized Object put(Object key, Object value) {
    String previousValue = getChildElementTextTrim((String) key, jdomElement);
    rewriteElement((String) key, (String) value, jdomElement);
    return previousValue;
  }

  @Override
  public synchronized Object remove(Object key) {
    String previousValue = getChildElementTextTrim((String) key, jdomElement);
    rewriteElement((String) key, null, jdomElement);
    return previousValue;
  }

  @Override
  public synchronized void load(Reader reader)
          throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized void load(InputStream inStream)
          throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void save(OutputStream out, String comments) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void store(Writer writer, String comments)
          throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void store(OutputStream out, String comments)
          throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized void loadFromXML(InputStream in)
          throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void storeToXML(OutputStream os, String comment)
          throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void storeToXML(OutputStream os, String comment, String encoding)
          throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object get(Object key) {
    return key instanceof String ? getProperty((String)key) : null;
  }

  @Override
  public int size() {
    return jdomElement.getChildren().size();
  }

  @Override
  public String toString() {
    String kvs = jdomElement.getChildren().stream()
            .map(j -> j.getName()+"="+j.getTextTrim())
            .collect(Collectors.joining("\n"));
    return "JDomProperties{\n" + kvs + "\n}";
  }

  @Override
  public String getProperty(String key) {
    Element property = jdomElement.getChild(key, jdomElement.getNamespace());

    if (property == null) {
      return null;
    } else {
      return property.getTextTrim();
    }
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Enumeration<?> propertyNames() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> stringPropertyNames() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void list(PrintStream out) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void list(PrintWriter out) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }

  private class JDomPropertiesSet extends HashSet<Map.Entry<Object, Object>> {
    private void addProperty(JDomProperty jDomProperty) {
      // The 'add' method can only be called internally.
      // Adding a property from the outside can currently only be supported using the JDomProperties.put() method.
      super.add(jDomProperty);
    }

    @Override
    public boolean add(Map.Entry<Object, Object> objectObjectEntry) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }


    @Override
    public boolean addAll(Collection<? extends Map.Entry<Object, Object>> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }
  }

  private static class JDomProperty implements JDomBacked, Map.Entry<Object, Object> {

    private final Element jdElement;

    private JDomProperty(Element jdElement) {
      this.jdElement = jdElement;
    }

    @Override
    public Object getKey() {
      return jdElement.getName();
    }

    @Override
    public Object getValue() {
      return jdElement.getTextTrim();
    }

    @Override
    public Object setValue(Object value) {
      String previousValue = jdElement.getTextTrim();
      jdElement.setText((String) value);
      return previousValue;
    }

    /** {@inheritDoc} */
    @Override
    protected Object clone() throws CloneNotSupportedException {
      throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public Element getJDomElement() {
      return jdElement;
    }
  }
}
