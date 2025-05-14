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
import org.jdom2.Text;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_ARTIFACT_ID;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_GROUP_ID;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_VERSION;
import static org.apache.maven.model.jdom.util.JDomUtils.detectIndentation;
import static org.apache.maven.model.jdom.util.JDomUtils.getChildElement;
import static org.apache.maven.model.jdom.util.JDomUtils.getChildElementTextTrim;
import static org.apache.maven.model.jdom.util.JDomUtils.removeChildElement;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteElement;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteValue;

/**
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 */
public class JDomMavenCoordinate implements JDomBacked, MavenCoordinate {

  private final Element jdomElement;

  JDomMavenCoordinate(Element jdomElement) {
    this.jdomElement = jdomElement;
  }

  @Override
  public String getArtifactId() {
    return getChildElementTextTrim(POM_ELEMENT_ARTIFACT_ID, jdomElement);
  }

  @Override
  public void setArtifactId(String artifactId) {
    rewriteElement(POM_ELEMENT_ARTIFACT_ID, artifactId, jdomElement);
  }

  @Override
  public String getGroupId() {
    return getChildElementTextTrim(POM_ELEMENT_GROUP_ID, jdomElement);
  }

  @Override
  public void setGroupId(String groupId) {
    rewriteElement(POM_ELEMENT_GROUP_ID, groupId, jdomElement);
  }

  @Override
  public String getVersion() {
    return getChildElementTextTrim(POM_ELEMENT_VERSION, jdomElement);
  }

  @Override
  public void setVersion(String version) {
    Element versionElement = getChildElement(POM_ELEMENT_VERSION, jdomElement);
    if (versionElement != null) {
      if (version == null) {
        removeChildElement(jdomElement, versionElement);
      } else {
        rewriteValue(versionElement, version);
      }
    } else if (version != null) {
      // This 'if' branch should only be executed when the project version is inherited from the parent but now
      // is changed without having changed the parent version. In this case, the version cannot be inherited
      // anymore and thus the project version element must be added.

      versionElement = new Element(POM_ELEMENT_VERSION, jdomElement.getNamespace());
      versionElement.setText(version);

      // Add the new version element after the artifactId.
      int indexArtifactId = jdomElement.indexOf(jdomElement.getChild(POM_ELEMENT_ARTIFACT_ID, jdomElement.getNamespace()));

      // Linebreak and indentation are (tried to be copied) from the existing XML structure.
      String indent = detectIndentation(jdomElement);
      if (indent != null) {
        jdomElement.addContent(++indexArtifactId, new Text("\n" + indent));
      }

      jdomElement.addContent(++indexArtifactId, versionElement);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
