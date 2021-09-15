package org.apache.maven.model.jdom;

/*
 * Copyright 2018 CoreMedia AG, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.model.ActivationProperty;
import org.jdom2.Element;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_NAME;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_VALUE;
import static org.apache.maven.model.jdom.util.JDomUtils.getChildElementTextTrim;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteElement;

/**
 * JDOM implementation of POMs {@code property} element.
 *
 * @author Marc Rohlfs, CoreMedia AG
 */
public class JDomActivationProperty extends ActivationProperty implements JDomBacked {

  private final Element jdomElement;

  public JDomActivationProperty(Element jdomElement) {
    this.jdomElement = jdomElement;

    super.setName(getChildElementTextTrim(POM_ELEMENT_NAME, this.jdomElement));
    super.setValue(getChildElementTextTrim(POM_ELEMENT_VALUE, this.jdomElement));
  }

  @Override
  public void setName(String name) {
    rewriteElement(POM_ELEMENT_NAME, name, jdomElement);
    super.setName(name);
  }

  @Override
  public void setValue(String value) {
    rewriteElement(POM_ELEMENT_VALUE, value, jdomElement);
    super.setValue(value);
  }

  /** {@inheritDoc} */
  @Override
  public ActivationProperty clone() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
