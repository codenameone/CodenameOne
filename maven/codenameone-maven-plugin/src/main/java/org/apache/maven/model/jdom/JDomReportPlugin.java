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
import org.apache.maven.model.ReportSet;
import org.jdom2.Element;

import java.util.List;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_INHERITED;
import static org.apache.maven.model.jdom.util.JDomUtils.getChildElementTextTrim;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteElement;

/**
 * JDom implementation of poms reports PLUGIN element
 *
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 */
public class JDomReportPlugin extends ReportPlugin implements JDomBacked, MavenCoordinate {

  private final Element jdomElement;

  private final MavenCoordinate coordinate;

  public JDomReportPlugin(Element jdomElement) {
    this.jdomElement = jdomElement;
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
  public Object getConfiguration() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setConfiguration(Object configuration) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getGroupId() {
    return coordinate.getGroupId();
  }

  @Override
  public void setGroupId(String groupId) {
    coordinate.setGroupId(groupId);
  }

  @Override
  public String getInherited() {
    return getChildElementTextTrim(POM_ELEMENT_INHERITED, jdomElement);
  }

  @Override
  public void setInherited(String inherited) {
    rewriteElement(POM_ELEMENT_INHERITED, inherited, jdomElement);
  }

  @Override
  public boolean isInherited() {
    return Boolean.parseBoolean(getInherited());
  }

  @Override
  public void setInherited(boolean inherited) {
    setInherited(Boolean.toString(inherited));
  }

  @Override
  public List<ReportSet> getReportSets() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setReportSets(List<ReportSet> reportSets) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getVersion() {
    return coordinate.getVersion();
  }

  @Override
  public void setVersion(String version) {
    coordinate.setVersion(version);
  }

  @Override
  public String getKey() {
    return constructKey(getGroupId(), getArtifactId());
  }

  /** {@inheritDoc} */
  @Override
  public ReportPlugin clone() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
