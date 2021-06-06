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

import org.apache.maven.model.Scm;
import org.jdom2.Element;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_CONNECTION;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_DEVELOPER_CONNECTION;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_TAG;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_URL;
import static org.apache.maven.model.jdom.util.JDomUtils.getChildElementTextTrim;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteElement;

/**
 * JDom implementation of poms SCM element
 *
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 */
public class JDomScm extends Scm implements JDomBacked {

  private Element jdomElement;

  public JDomScm(Element jdomElement) {
    this.jdomElement = jdomElement;
  }

  @Override
  public String getConnection() {
    return getChildElementTextTrim(POM_ELEMENT_CONNECTION, jdomElement);
  }

  @Override
  public void setConnection(String connection) {
    rewriteElement(POM_ELEMENT_CONNECTION, connection, jdomElement);
  }

  @Override
  public String getDeveloperConnection() {
    return getChildElementTextTrim(POM_ELEMENT_DEVELOPER_CONNECTION, jdomElement);
  }

  @Override
  public void setDeveloperConnection(String developerConnection) {
    rewriteElement(POM_ELEMENT_DEVELOPER_CONNECTION, developerConnection, jdomElement);
  }

  @Override
  public String getTag() {
    return getChildElementTextTrim(POM_ELEMENT_TAG, jdomElement);
  }

  @Override
  public void setTag(String tag) {
    rewriteElement(POM_ELEMENT_TAG, tag, jdomElement);
  }

  @Override
  public String getUrl() {
    return getChildElementTextTrim(POM_ELEMENT_URL, jdomElement);
  }

  @Override
  public void setUrl(String url) {
    rewriteElement(POM_ELEMENT_URL, url, jdomElement);
  }

  /** {@inheritDoc} */
  @Override
  public Scm clone() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
