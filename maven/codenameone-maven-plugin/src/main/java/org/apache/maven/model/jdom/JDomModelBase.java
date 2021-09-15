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
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.jdom2.Element;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_ARTIFACT_ID;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_BUILD;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_DEPENDENCIES;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_DEPENDENCY_MANAGEMENT;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_GROUP_ID;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_MODULES;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_PLUGIN;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_PLUGINS;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_PLUGIN_MANAGEMENT;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_PROPERTIES;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_REPORTING;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_VERSION;
import static org.apache.maven.model.jdom.util.JDomUtils.getChildElement;
import static org.apache.maven.model.jdom.util.JDomUtils.insertContentElement;
import static org.apache.maven.model.jdom.util.JDomUtils.insertNewElement;
import static org.apache.maven.model.jdom.util.JDomUtils.insertNewNestedElements;
import static org.apache.maven.model.jdom.util.JDomUtils.newDetachedElement;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteElement;

/**
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 */
public class JDomModelBase extends ModelBase implements JDomBacked {

  private final Element jdomElement;

  public JDomModelBase(Element jdomElement) {
    this.jdomElement = jdomElement;

    Element dependenciesElement = getChildElement(POM_ELEMENT_DEPENDENCIES, jdomElement);
    if (dependenciesElement == null) {
      dependenciesElement = newDetachedElement(POM_ELEMENT_DEPENDENCIES, jdomElement);
    }
    super.setDependencies(new JDomDependencies(dependenciesElement, this));

    Element dependencyManagementElement = getChildElement(POM_ELEMENT_DEPENDENCY_MANAGEMENT, jdomElement);
    if (dependencyManagementElement == null) {
      dependencyManagementElement = newDetachedElement(POM_ELEMENT_DEPENDENCY_MANAGEMENT, jdomElement);
      insertNewElement(POM_ELEMENT_DEPENDENCIES, dependencyManagementElement);
    }
    super.setDependencyManagement(new JDomDependencyManagement(dependencyManagementElement, this));
  }

  public Build getBuild() {
    Element elm = jdomElement.getChild(POM_ELEMENT_BUILD, jdomElement.getNamespace());
    if (elm == null) {
      return null;
    } else {
      // this way build setters change DOM tree immediately
      return new JDomBuild(elm);
    }
  }

  public void setBuild(BuildBase build) {
    if (build == null) {
      rewriteElement(POM_ELEMENT_BUILD, null, jdomElement);
    } else {
      Element jdomBuild = insertNewElement(POM_ELEMENT_BUILD, this.jdomElement);
      insertPluginManagement(jdomBuild, build.getPluginManagement());
    }
  }

  public DistributionManagement getDistributionManagement() {
    throw new UnsupportedOperationException();
  }

  public void setDistributionManagement(DistributionManagement distributionManagement) {
    throw new UnsupportedOperationException();
  }

  public void setDependencies(List<Dependency> dependencies) {
    if (dependencies == null) {
      rewriteElement(POM_ELEMENT_DEPENDENCIES, null, jdomElement);
    } else {
      new JDomDependencies(insertNewElement(POM_ELEMENT_DEPENDENCIES, jdomElement), this).addAll(dependencies);
    }
  }

  public void setDependencyManagement(DependencyManagement dependencyManagement) {
    if (dependencyManagement == null) {
      rewriteElement(POM_ELEMENT_DEPENDENCY_MANAGEMENT, null, jdomElement);
    } else {
      DependencyManagement jdomDependencyManagement = getDependencyManagement();
      if (jdomDependencyManagement == null) {
        Element dependencyManagementRoot = insertNewElement(POM_ELEMENT_DEPENDENCY_MANAGEMENT, jdomElement);
        jdomDependencyManagement = new JDomDependencyManagement(dependencyManagementRoot, this);
      }

      jdomDependencyManagement.setDependencies(dependencyManagement.getDependencies());
    }
  }

  public List<String> getModules() {
    Element modulesElement = getChildElement(POM_ELEMENT_MODULES, jdomElement);
    if (modulesElement == null) {
      return emptyList();
    } else {
      return new JDomModules(modulesElement);
    }
  }

  public void setModules(List<String> modules) {
    if (modules == null) {
      rewriteElement(POM_ELEMENT_MODULES, null, jdomElement);
    } else {
      List<String> jDomModules = getModules();
      if (jDomModules instanceof JDomModules) {
        jDomModules.clear();
      } else {
        jDomModules = new JDomModules(insertNewElement(POM_ELEMENT_MODULES, jdomElement));
      }
      jDomModules.addAll(modules);
    }
  }

  public List<Repository> getPluginRepositories() {
    throw new UnsupportedOperationException();
  }

  public void setPluginRepositories(List<Repository> pluginRepositories) {
    throw new UnsupportedOperationException();
  }

  public Properties getProperties() {
    Element properties = jdomElement.getChild(POM_ELEMENT_PROPERTIES, jdomElement.getNamespace());

    if (properties == null) {
      return null;
    } else {
      return new JDomProperties(properties);
    }
  }

  public void setProperties(Properties properties) {
    if (properties == null) {
      rewriteElement(POM_ELEMENT_PROPERTIES, null, jdomElement);
    } else {
      Properties jDomProperties = getProperties();
      if (jDomProperties != null) {
        jDomProperties.clear();
      } else {
        jDomProperties = new JDomProperties(insertNewElement(POM_ELEMENT_PROPERTIES, jdomElement));
      }
      for (Map.Entry<Object, Object> entry : properties.entrySet()) {
        jDomProperties.setProperty((String) entry.getKey(), (String) entry.getValue());
      }
    }
  }

  public Reporting getReporting() {
    Element reporting = jdomElement.getChild(POM_ELEMENT_REPORTING, jdomElement.getNamespace());

    if (reporting == null) {
      return null;
    } else {
      return new JDomReporting(reporting);
    }
  }

  public void setReporting(Reporting reporting) {
    throw new UnsupportedOperationException();
  }

  public List<Repository> getRepositories() {
    throw new UnsupportedOperationException();
  }

  public void setRepositories(List<Repository> repositories) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public ModelBase clone() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }


  // --- internal ---------------------------------------------------

  private static void insertPluginManagement(Element jdomParent, PluginManagement pluginManagement) {
    if (pluginManagement != null) {
      Element jdomPlugins = insertNewNestedElements(jdomParent, POM_ELEMENT_PLUGIN_MANAGEMENT, POM_ELEMENT_PLUGINS);
      insertPlugins(jdomPlugins, pluginManagement.getPlugins());
    }
  }

  private static void insertPlugins(Element jdomPlugins, List<Plugin> plugins) {
    for (Plugin plugin : plugins) {
      Element jdomPlugin = insertNewElement(POM_ELEMENT_PLUGIN, jdomPlugins);
      insertGAV(jdomPlugin, plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion());
    }
  }

  private static void insertGAV(Element jdomParent, String groupId, String artifactId, String version) {
    insertContentElement(jdomParent, POM_ELEMENT_GROUP_ID, groupId);
    insertContentElement(jdomParent, POM_ELEMENT_ARTIFACT_ID, artifactId);
    insertContentElement(jdomParent, POM_ELEMENT_VERSION, version);
  }
}
