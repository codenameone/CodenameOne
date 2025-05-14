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

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.jdom2.Element;

import java.util.List;
import java.util.Properties;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_ACTIVATION;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_ID;
import static org.apache.maven.model.jdom.util.JDomUtils.getChildElementTextTrim;
import static org.apache.maven.model.jdom.util.JDomUtils.insertNewElement;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteElement;

/**
 * JDom implementation of poms PROFILE element
 *
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 */
public class JDomProfile extends Profile implements JDomBacked {

  private final Element jdomElement;

  private final JDomModelBase modelBase;

  public JDomProfile(Element jdomElement) {
    this.jdomElement = jdomElement;
    this.modelBase = new JDomModelBase(jdomElement);
  }

  @Override
  public Activation getActivation() {
    Element elm = jdomElement.getChild(POM_ELEMENT_ACTIVATION, jdomElement.getNamespace());
    if (elm == null) {
      return null;
    } else {
      return new JDomActivation(elm);
    }
  }

  @Override
  public void setActivation(Activation activation) {
    if (activation == null) {
      rewriteElement(POM_ELEMENT_ACTIVATION, null, jdomElement);
    } else {
      Activation jdomActivation = getActivation();
      if (jdomActivation == null) {
        Element activationRoot = insertNewElement(POM_ELEMENT_ACTIVATION, jdomElement);
        jdomActivation = new JDomActivation(activationRoot);
      }

      jdomActivation.setActiveByDefault(activation.isActiveByDefault());

      ActivationFile file = activation.getFile();
      if (file != null) {
        jdomActivation.setFile(file);
      }

      String jdk = activation.getJdk();
      if (jdk != null) {
        jdomActivation.setJdk(jdk);
      }

      ActivationOS os = activation.getOs();
      if (os != null) {
        jdomActivation.setOs(os);
      }

      ActivationProperty property = activation.getProperty();
      if (property != null) {
        jdomActivation.setProperty(property);
      }
    }

    super.setActivation(activation);
  }

  @Override
  public BuildBase getBuild() {
    return modelBase.getBuild();
  }

  @Override
  public void setBuild(BuildBase build) {
    modelBase.setBuild(build);
  }

  @Override
  public List<Dependency> getDependencies() {
    return modelBase.getDependencies();
  }

  @Override
  public void setDependencies(List<Dependency> dependencies) {
    modelBase.setDependencies(dependencies);
  }

  @Override
  public DependencyManagement getDependencyManagement() {
    return modelBase.getDependencyManagement();
  }

  @Override
  public void setDependencyManagement(DependencyManagement dependencyManagement) {
    modelBase.setDependencyManagement(dependencyManagement);
  }

  @Override
  public DistributionManagement getDistributionManagement() {
    return modelBase.getDistributionManagement();
  }

  @Override
  public void setDistributionManagement(DistributionManagement distributionManagement) {
    modelBase.setDistributionManagement(distributionManagement);
  }

  @Override
  public String getId() {
    return getChildElementTextTrim(POM_ELEMENT_ID, jdomElement);
  }

  @Override
  public void setId(String id) {
    rewriteElement(POM_ELEMENT_ID, id, jdomElement);
  }

  @Override
  public List<String> getModules() {
    return modelBase.getModules();
  }

  @Override
  public void setModules(List<String> modules) {
    modelBase.setModules(modules);
  }

  @Override
  public List<Repository> getPluginRepositories() {
    return modelBase.getPluginRepositories();
  }

  @Override
  public void setPluginRepositories(List<Repository> pluginRepositories) {
    modelBase.setPluginRepositories(pluginRepositories);
  }

  @Override
  public Properties getProperties() {
    return modelBase.getProperties();
  }

  @Override
  public void setProperties(Properties properties) {
    modelBase.setProperties(properties);
  }

  @Override
  public Reporting getReporting() {
    return modelBase.getReporting();
  }

  @Override
  public void setReporting(Reporting reporting) {
    modelBase.setReporting(reporting);
  }

  @Override
  public List<Repository> getRepositories() {
    return modelBase.getRepositories();
  }

  @Override
  public void setRepositories(List<Repository> repositories) {
    modelBase.setRepositories(repositories);
  }

  /** {@inheritDoc} */
  @Override
  public Profile clone() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
