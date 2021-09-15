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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.jdom2.Element;

import java.util.List;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_ARTIFACT_ID;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_CLASSIFIER;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_GROUP_ID;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_OPTIONAL;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_SCOPE;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_SYSTEM_PATH;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_TYPE;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_VERSION;
import static org.apache.maven.model.jdom.util.JDomUtils.getChildElementTextTrim;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteElement;
import static org.codehaus.plexus.util.StringUtils.defaultString;
import static org.codehaus.plexus.util.StringUtils.trim;

/**
 * JDOM implementation of the {@link Dependency} class. It holds the child elements of the Maven POMs {@code dependency}
 * element.
 *
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 * @author Marc Rohlfs, CoreMedia AG
 */
public class JDomDependency extends Dependency implements JDomBacked {

  private static final long serialVersionUID = -6973299112773078102L;

  private final Element jdomElement;

  JDomDependency(Element jdomElement) {
    this.jdomElement = jdomElement;

    super.setArtifactId(getChildElementTextTrim(POM_ELEMENT_ARTIFACT_ID, jdomElement));
    super.setClassifier(getChildElementTextTrim(POM_ELEMENT_CLASSIFIER, jdomElement));
    super.setGroupId(getChildElementTextTrim(POM_ELEMENT_GROUP_ID, jdomElement));
    super.setOptional(getChildElementTextTrim(POM_ELEMENT_OPTIONAL, jdomElement));
    super.setScope(getChildElementTextTrim(POM_ELEMENT_SCOPE, jdomElement));
    super.setSystemPath(getChildElementTextTrim(POM_ELEMENT_SYSTEM_PATH, jdomElement));
    super.setVersion(getChildElementTextTrim(POM_ELEMENT_VERSION, jdomElement));

    String type = getChildElementTextTrim(POM_ELEMENT_TYPE, jdomElement);
    if (type != null) {
      super.setType(type);
    }
  }

  JDomDependency(Element jdomElement, Dependency dependency) {
    this.jdomElement = jdomElement;

    setArtifactId(dependency.getArtifactId());
    setClassifier(dependency.getClassifier());
    setGroupId(dependency.getGroupId());
    setOptional(dependency.getOptional());
    setScope(dependency.getScope());
    setSystemPath(dependency.getSystemPath());
    setVersion(dependency.getVersion());

    String type = dependency.getType();
    if (!"jar".equals(type)) {
      setType(type);
    }

    List<Exclusion> exclusions = dependency.getExclusions();
    if (!exclusions.isEmpty()) {
      setExclusions(exclusions);
    }
  }

  @Override
  public void setArtifactId(String artifactId) {
    rewriteElement(POM_ELEMENT_ARTIFACT_ID, artifactId, jdomElement);
    super.setArtifactId(trim(artifactId));
  }

  @Override
  public void setClassifier(String classifier) {
    rewriteElement(POM_ELEMENT_CLASSIFIER, classifier, jdomElement);
    super.setClassifier(trim(classifier));
  }

  @Override
  public List<Exclusion> getExclusions() {
    // Remove this method override when Dependency#exclusions is properly set in constructors and #setExclusions.
    throw new UnsupportedOperationException();
  }

  @Override
  public void setExclusions(List<Exclusion> exclusions) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setGroupId(String groupId) {
    rewriteElement(POM_ELEMENT_GROUP_ID, groupId, jdomElement);
    super.setGroupId(trim(groupId));
  }

  @Override
  public void setOptional(String optional) {
    rewriteElement(POM_ELEMENT_OPTIONAL, optional, jdomElement);
    super.setOptional(trim(optional));
  }

  @Override
  public void setOptional(boolean optional) {
    setOptional(String.valueOf(optional));
  }

  @Override
  public void setScope(String scope) {
    rewriteElement(POM_ELEMENT_SCOPE, scope, jdomElement);
    super.setScope(trim(scope));
  }

  @Override
  public void setSystemPath(String systemPath) {
    rewriteElement(POM_ELEMENT_SYSTEM_PATH, systemPath, jdomElement);
    super.setSystemPath(trim(systemPath));
  }

  @Override
  public void setType(String type) {
    rewriteElement(POM_ELEMENT_TYPE, type, jdomElement);
    super.setType(defaultString(trim(type), "jar"));
  }

  @Override
  public void setVersion(String version) {
    rewriteElement(POM_ELEMENT_VERSION, version, jdomElement);
    super.setVersion(trim(version));
  }

  @Override
  public Dependency clone() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
