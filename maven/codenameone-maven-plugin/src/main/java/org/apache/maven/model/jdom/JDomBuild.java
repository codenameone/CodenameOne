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

import org.apache.maven.model.Build;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Resource;
import org.jdom2.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_DEFAULT_GOAL;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_DIRECTORY;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_EXTENSION;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_EXTENSIONS;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_FINAL_NAME;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_OUTPUT_DIRECTORY;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_PLUGINS;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_PLUGIN_MANAGEMENT;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_SCRIPT_SOURCE_DIRECTORY;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_SOURCE_DIRECTORY;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_TEST_OUTPUT_DIRECTORY;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_TEST_SOURCE_DIRECTORY;
import static org.apache.maven.model.jdom.util.JDomUtils.getChildElementTextTrim;
import static org.apache.maven.model.jdom.util.JDomUtils.insertNewElement;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteElement;

/**
 * JDom implementation of poms BUILD element
 *
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 */
public class JDomBuild extends Build implements JDomBacked {

  private final Element jdomElement;

  public JDomBuild(Element jdomElement) {
    this.jdomElement = jdomElement;
  }

  @Override
  public String getDefaultGoal() {
    return getChildElementTextTrim(POM_ELEMENT_DEFAULT_GOAL, jdomElement);
  }

  @Override
  public void setDefaultGoal(String defaultGoal) {
    rewriteElement(POM_ELEMENT_DEFAULT_GOAL, defaultGoal, jdomElement);
  }

  @Override
  public String getDirectory() {
    return getChildElementTextTrim(POM_ELEMENT_DIRECTORY, jdomElement);
  }

  @Override
  public void setDirectory(String directory) {
    rewriteElement(POM_ELEMENT_DIRECTORY, directory, jdomElement);
  }

  @Override
  public List<Extension> getExtensions() {
    Element extensionsElm = jdomElement.getChild(POM_ELEMENT_EXTENSIONS, jdomElement.getNamespace());
    if (extensionsElm == null) {
      return Collections.emptyList();
    } else {
      List<Element> extensionElms = extensionsElm.getChildren(POM_ELEMENT_EXTENSION, jdomElement.getNamespace());
      List<Extension> extensions = new ArrayList<>(extensionElms.size());
      for (Element extensionElm : extensionElms) {
        extensions.add(new JDomExtension(extensionElm));
      }
      return extensions;
    }
  }

  @Override
  public void setExtensions(List<Extension> extensions) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> getFilters() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setFilters(List<String> filters) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getFinalName() {
    return getChildElementTextTrim(POM_ELEMENT_FINAL_NAME, jdomElement);
  }

  @Override
  public void setFinalName(String finalName) {
    rewriteElement(POM_ELEMENT_FINAL_NAME, finalName, jdomElement);
  }

  @Override
  public String getOutputDirectory() {
    return getChildElementTextTrim(POM_ELEMENT_OUTPUT_DIRECTORY, jdomElement);
  }

  @Override
  public void setOutputDirectory(String outputDirectory) {
    rewriteElement(POM_ELEMENT_OUTPUT_DIRECTORY, outputDirectory, jdomElement);
  }

  @Override
  public PluginManagement getPluginManagement() {
    Element pluginManagementElm = jdomElement.getChild(POM_ELEMENT_PLUGIN_MANAGEMENT, jdomElement.getNamespace());
    if (pluginManagementElm == null) {
      return null;
    } else {
      return new JDomPluginManagement(pluginManagementElm);
    }
  }

  @Override
  public void setPluginManagement(PluginManagement pluginManagement) {
    throw new UnsupportedOperationException();
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
      List<Plugin> jdomPlugins = getPlugins();
      if (!(jdomPlugins instanceof JDomPlugins)) {
        jdomPlugins = new JDomPlugins(insertNewElement(POM_ELEMENT_PLUGINS, jdomElement));
      }
      jdomPlugins.addAll(plugins);
    }
  }

  @Override
  public List<Resource> getResources() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setResources(List<Resource> resources) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getScriptSourceDirectory() {
    return getChildElementTextTrim(POM_ELEMENT_SCRIPT_SOURCE_DIRECTORY, jdomElement);
  }

  @Override
  public void setScriptSourceDirectory(String scriptSourceDirectory) {
    rewriteElement(POM_ELEMENT_SCRIPT_SOURCE_DIRECTORY, scriptSourceDirectory, jdomElement);
  }

  @Override
  public String getSourceDirectory() {
    return getChildElementTextTrim(POM_ELEMENT_SOURCE_DIRECTORY, jdomElement);
  }

  @Override
  public void setSourceDirectory(String sourceDirectory) {
    rewriteElement(POM_ELEMENT_SOURCE_DIRECTORY, sourceDirectory, jdomElement);
  }

  @Override
  public String getTestOutputDirectory() {
    return getChildElementTextTrim(POM_ELEMENT_TEST_OUTPUT_DIRECTORY, jdomElement);
  }

  @Override
  public void setTestOutputDirectory(String testOutputDirectory) {
    rewriteElement(POM_ELEMENT_TEST_OUTPUT_DIRECTORY, testOutputDirectory, jdomElement);
  }

  @Override
  public List<Resource> getTestResources() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setTestResources(List<Resource> testResources) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getTestSourceDirectory() {
    return getChildElementTextTrim(POM_ELEMENT_TEST_SOURCE_DIRECTORY, jdomElement);
  }

  @Override
  public void setTestSourceDirectory(String testSourceDirectory) {
    rewriteElement(POM_ELEMENT_TEST_SOURCE_DIRECTORY, testSourceDirectory, jdomElement);
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
  public Build clone() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
