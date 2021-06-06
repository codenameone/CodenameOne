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

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.jdom2.Element;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_PLUGINS;
import static org.apache.maven.model.jdom.util.JDomUtils.insertNewElement;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteElement;

/**
 * JDom implementation of poms PLUGINMANAGEMENT element
 *
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 */
public class JDomPluginManagement extends PluginManagement implements JDomBacked {

  private final Element jdomElement;

  public JDomPluginManagement(Element jdomElement) {
    this.jdomElement = jdomElement;
  }

  @Override
  public List<Plugin> getPlugins() {
    Element pluginsElm = jdomElement.getChild(POM_ELEMENT_PLUGINS, jdomElement.getNamespace());
    if (pluginsElm == null) {
      return Collections.emptyList();
    } else {
      return new JDomPlugins(pluginsElm);
    }
  }

  @Override
  public void setPlugins(List<Plugin> plugins) {
    if (plugins == null) {
      rewriteElement(POM_ELEMENT_PLUGINS, null, jdomElement);
    } else {
      new JDomPlugins(insertNewElement(POM_ELEMENT_PLUGINS, jdomElement)).addAll(plugins);
    }
  }

  @Override
  public void flushPluginMap() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Plugin> getPluginsAsMap() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public PluginManagement clone() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
