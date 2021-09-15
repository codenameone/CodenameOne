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

import org.apache.maven.model.PluginExecution;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_EXECUTION;

/**
 * JDOM implementation of POMs {@code executions} element.
 */
public class JDomExecutions extends ArrayList<PluginExecution> implements JDomBacked {

  private final Element jdomElement;
  private final JDomBacked parent;

  public JDomExecutions(Element jdomElement, JDomBacked parent) {
    super(transformExecutionElementsToJDomExecutionList(jdomElement));
    this.jdomElement = jdomElement;
    this.parent = parent;
  }

  private static List<PluginExecution> transformExecutionElementsToJDomExecutionList(Element jdomElement) {
    Filter<Element> filter = new ElementFilter(POM_ELEMENT_EXECUTION, jdomElement.getNamespace());
    return jdomElement.getContent(filter).stream().map(JDomPluginExecution::new).collect(Collectors.toList());
  }

  @Override
  public boolean add(PluginExecution pluginExecution) {
    throw new UnsupportedOperationException("To be implemented if needed.");
  }

  @Override
  public boolean remove(Object execution) {
    throw new UnsupportedOperationException("How would executions be comparable?");
  }

  @Override
  public boolean addAll(Collection<? extends PluginExecution> executions) {
    throw new UnsupportedOperationException("To be implemented if needed.");
  }

  @Override
  public boolean addAll(int index, Collection<? extends PluginExecution> executions) {
    throw new UnsupportedOperationException("To be implemented if needed.");
  }

  @Override
  public boolean removeAll(Collection<?> executions) {
    throw new UnsupportedOperationException("How would executions be comparable?");
  }

  @Override
  public boolean retainAll(Collection<?> executions) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("To be implemented if needed.");
  }

  @Override
  public PluginExecution set(int index, PluginExecution execution) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(int index, PluginExecution execution) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PluginExecution remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int lastIndexOf(Object dependency) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<PluginExecution> listIterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<PluginExecution> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<PluginExecution> subList(int fromIndex, int toIndex) {
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
