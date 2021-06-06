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
import org.apache.maven.model.DependencyManagement;
import org.jdom2.Element;

import java.util.List;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_DEPENDENCIES;
import static org.apache.maven.model.jdom.util.JDomUtils.getChildElement;
import static org.apache.maven.model.jdom.util.JDomUtils.newDetachedElement;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteElement;

/**
 * JDom implementation of poms DEPENDENCYMANAGEMENT element
 *
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 */
public class JDomDependencyManagement extends DependencyManagement implements JDomBacked {

  private final Element jdomElement;

  private final JDomBacked parent;

  public JDomDependencyManagement(Element jdomElement, JDomBacked parent) {
    this.jdomElement = jdomElement;
    this.parent = parent;

    Element dependenciesElement = getChildElement(POM_ELEMENT_DEPENDENCIES, jdomElement);
    if (dependenciesElement == null) {
      dependenciesElement = newDetachedElement(POM_ELEMENT_DEPENDENCIES, jdomElement);
    }
    super.setDependencies(new JDomDependencies(dependenciesElement, this));
  }

  @Override
  public void setDependencies(List<Dependency> dependencies) {
    if (dependencies == null) {
      rewriteElement(POM_ELEMENT_DEPENDENCIES, null, jdomElement);
    } else {
      JDomDependencies jdomDependencies = (JDomDependencies) getDependencies();
      jdomDependencies.clear();
      jdomDependencies.addAll(dependencies);
    }
  }

  /** {@inheritDoc} */
  @Override
  public DependencyManagement clone() {
    return super.clone();
  }

  /** {@inheritDoc} */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }

  public JDomBacked getParent() {
    return parent;
  }
}
