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

import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.jdom.util.JDomConfigurationContainerHelper;
import org.jdom2.Element;

public class JDomPluginExecution extends PluginExecution implements JDomBacked {
  private static final long serialVersionUID = -6973299112773078102L;

  private final Element jdomElement;

  JDomPluginExecution(Element jdomElement) {
    this.jdomElement = jdomElement;
  }

  @Override
  public Element getJDomElement() {
    return jdomElement;
  }

  @Override
  public Object getConfiguration() {
    return JDomConfigurationContainerHelper.getConfiguration(jdomElement);
  }

  @Override
  public void setConfiguration(Object configuration) {
    JDomConfigurationContainerHelper.setConfiguration(jdomElement, configuration);
  }
}
