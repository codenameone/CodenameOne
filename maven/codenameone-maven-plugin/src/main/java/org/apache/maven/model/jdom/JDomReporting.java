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

import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.jdom2.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_PLUGIN;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_PLUGINS;

/**
 * JDom implementation of poms REPORTING element
 *
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 */
public class JDomReporting extends Reporting implements JDomBacked {

  private final Element jdomElement;

  public JDomReporting(Element jdomElement) {
    this.jdomElement = jdomElement;
  }

  @Override
  public String getOutputDirectory() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<ReportPlugin> getPlugins() {
    Element pluginsElm = jdomElement.getChild(POM_ELEMENT_PLUGINS, jdomElement.getNamespace());
    if (pluginsElm == null) {
      return Collections.emptyList();
    } else {
      List<Element> pluginElms = pluginsElm.getChildren(POM_ELEMENT_PLUGIN, jdomElement.getNamespace());

      List<ReportPlugin> plugins = new ArrayList<>(pluginElms.size());

      for (Element pluginElm : pluginElms) {
        plugins.add(new JDomReportPlugin(pluginElm));
      }

      return plugins;
    }
  }

  @Override
  public void removePlugin(ReportPlugin reportPlugin) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setOutputDirectory(String outputDirectory) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setPlugins(List<ReportPlugin> plugins) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void flushReportPluginMap() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, ReportPlugin> getReportPluginsAsMap() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isExcludeDefaults() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setExcludeDefaults(boolean excludeDefaults) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Reporting clone() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
