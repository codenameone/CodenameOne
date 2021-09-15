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
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Scm;
import org.jdom2.Document;
import org.jdom2.Element;

import java.util.List;
import java.util.Properties;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_DESCRIPTION;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_INCEPTION_YEAR;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_MODEL_VERSION;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_NAME;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_PACKAGING;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_PARENT;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_PROFILES;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_SCM;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_URL;
import static org.apache.maven.model.jdom.util.JDomUtils.getChildElement;
import static org.apache.maven.model.jdom.util.JDomUtils.getChildElementTextTrim;
import static org.apache.maven.model.jdom.util.JDomUtils.insertNewElement;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteElement;

/**
 * JDom implementation of poms PROJECT element
 *
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 */
public class JDomModel extends Model implements JDomBacked, MavenCoordinate {

  private final Element jdomElement;

  private final JDomModelBase modelBase;
  private final JDomMavenCoordinate coordinate;

  public JDomModel(Document document) {
    this(document.getRootElement());
  }

  public JDomModel(Element jdomElement) {
    this.jdomElement = jdomElement;
    this.modelBase = new JDomModelBase(jdomElement);
    this.coordinate = new JDomMavenCoordinate(jdomElement);
  }

  @Override
  public String getArtifactId() {
    return coordinate.getArtifactId();
  }

  @Override
  public void setArtifactId(String artifactId) {
    coordinate.setArtifactId(artifactId);
  }

  @Override
  public Build getBuild() {
    return modelBase.getBuild();
  }

  @Override
  public void setBuild(Build build) {
    modelBase.setBuild(build);
  }

  @Override
  public CiManagement getCiManagement() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setCiManagement(CiManagement ciManagement) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Contributor> getContributors() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setContributors(List<Contributor> contributors) {
    throw new UnsupportedOperationException();
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
  public String getDescription() {
    return getChildElementTextTrim(POM_ELEMENT_DESCRIPTION, jdomElement);
  }

  @Override
  public void setDescription(String description) {
    rewriteElement(POM_ELEMENT_DESCRIPTION, description, jdomElement);
  }

  @Override
  public List<Developer> getDevelopers() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setDevelopers(List<Developer> developers) {
    throw new UnsupportedOperationException();
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
  public String getGroupId() {
    return coordinate.getGroupId();
  }

  @Override
  public void setGroupId(String groupId) {
    String projectGroupId = coordinate.getGroupId();
    if (projectGroupId != null) {
      coordinate.setGroupId(groupId);
    } else {
      Parent parent = getParent();
      if (parent == null || !groupId.equals(parent.getGroupId())) {
        coordinate.setGroupId(groupId);
      }
    }
  }

  @Override
  public String getInceptionYear() {
    return getChildElementTextTrim(POM_ELEMENT_INCEPTION_YEAR, jdomElement);
  }

  @Override
  public void setInceptionYear(String inceptionYear) {
    rewriteElement(POM_ELEMENT_INCEPTION_YEAR, inceptionYear, jdomElement);
  }

  @Override
  public IssueManagement getIssueManagement() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setIssueManagement(IssueManagement issueManagement) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<License> getLicenses() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setLicenses(List<License> licenses) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<MailingList> getMailingLists() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMailingLists(List<MailingList> mailingLists) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getModelVersion() {
    return getChildElementTextTrim(POM_ELEMENT_MODEL_VERSION, jdomElement);
  }

  @Override
  public void setModelVersion(String modelVersion) {
    rewriteElement(POM_ELEMENT_MODEL_VERSION, modelVersion, jdomElement);
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
  public String getName() {
    return getChildElementTextTrim(POM_ELEMENT_NAME, jdomElement);
  }

  @Override
  public void setName(String name) {
    rewriteElement(POM_ELEMENT_NAME, name, jdomElement);
  }

  @Override
  public Organization getOrganization() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setOrganization(Organization organization) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getPackaging() {
    return getChildElementTextTrim(POM_ELEMENT_PACKAGING, jdomElement);
  }

  @Override
  public void setPackaging(String packaging) {
    rewriteElement(POM_ELEMENT_PACKAGING, packaging, jdomElement);
  }

  @Override
  public Parent getParent() {
    Element elm = getChildElement(POM_ELEMENT_PARENT, jdomElement);
    if (elm == null) {
      return null;
    } else {
      // this way parent setters change DOM tree immediately
      return new JDomParent(elm);
    }
  }

  @Override
  public void setParent(Parent parent) {
    if (parent == null) {
      Parent removedParent = getParent();

      rewriteElement(POM_ELEMENT_PARENT, null, jdomElement);

      if (getGroupId() == null) {
        setGroupId(removedParent.getGroupId());
      }
      if (getVersion() == null) {
        setVersion(removedParent.getVersion());
      }
    } else {
      boolean containsRelativePath = false;

      Parent jdomParent = getParent();
      if (jdomParent == null) {
        Element parentRoot = insertNewElement(POM_ELEMENT_PARENT, jdomElement);
        jdomParent = new JDomParent(parentRoot);
      } else {
        containsRelativePath = jdomParent.getRelativePath() != null;
      }

      // Write current values to JDom tree
      jdomParent.setGroupId(parent.getGroupId());
      jdomParent.setArtifactId(parent.getArtifactId());
      jdomParent.setVersion(parent.getVersion());

      String relativePath = parent.getRelativePath();
      if (relativePath != null && !parent.getRelativePath().equals("../pom.xml") || containsRelativePath) {
        jdomParent.setRelativePath(relativePath);
      }

    }
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
  public Prerequisites getPrerequisites() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setPrerequisites(Prerequisites prerequisites) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Profile> getProfiles() {
    return new JDomProfiles(jdomElement.getChild(POM_ELEMENT_PROFILES, jdomElement.getNamespace()), this);
  }

  @Override
  public void setProfiles(List<Profile> profiles) {
    if (profiles == null) {
      rewriteElement(POM_ELEMENT_PROFILES, null, jdomElement);
    } else {
      new JDomProfiles(insertNewElement(POM_ELEMENT_PROFILES, jdomElement), this).addAll(profiles);
    }
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

  @Override
  public Scm getScm() {
    Element elm = jdomElement.getChild(POM_ELEMENT_SCM, jdomElement.getNamespace());
    if (elm == null) {
      return null;
    } else {
      // this way scm setters change DOM tree immediately
      return new JDomScm(elm);
    }
  }

  @Override
  public void setScm(Scm scm) {
    if (scm == null) {
      rewriteElement(POM_ELEMENT_SCM, null, jdomElement);
    } else {
      Scm jdomScm = getScm();
      if (jdomScm == null) {
        Element scmRoot = insertNewElement(POM_ELEMENT_SCM, jdomElement);
        jdomScm = new JDomScm(scmRoot);
      }

      // Write current values to JDom tree
      jdomScm.setConnection(scm.getConnection());
      jdomScm.setDeveloperConnection(scm.getDeveloperConnection());
      jdomScm.setTag(scm.getTag());
      jdomScm.setUrl(scm.getUrl());
    }
  }

  @Override
  public String getUrl() {
    return getChildElementTextTrim(POM_ELEMENT_URL, jdomElement);
  }

  @Override
  public void setUrl(String url) {
    rewriteElement(POM_ELEMENT_URL, url, jdomElement);
  }

  @Override
  public String getVersion() {
    return coordinate.getVersion();
  }

  @Override
  public void setVersion(String version) {
    String projectVersion = coordinate.getVersion();
    if (projectVersion != null) {
      coordinate.setVersion(version);
    } else {
      Parent parent = getParent();
      if (parent == null || !version.equals(parent.getVersion())) {
        coordinate.setVersion(version);
      }
    }
  }

  @Override
  public Model clone() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
