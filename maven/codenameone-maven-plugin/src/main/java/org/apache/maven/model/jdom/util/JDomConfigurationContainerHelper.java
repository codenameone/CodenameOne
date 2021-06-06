package org.apache.maven.model.jdom.util;

import org.apache.maven.model.jdom.JDomConfiguration;
import org.jdom2.Element;
import org.jdom2.Text;

import static java.util.Arrays.asList;
import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_CONFIGURATION;
import static org.apache.maven.model.jdom.util.JDomUtils.detectIndentation;
import static org.apache.maven.model.jdom.util.JDomUtils.getChildElement;
import static org.apache.maven.model.jdom.util.JDomUtils.resetIndentations;
import static org.apache.maven.model.jdom.util.JDomUtils.rewriteElement;

public class JDomConfigurationContainerHelper {
  public static JDomConfiguration getConfiguration(Element jdomElement) {
    Element elm = getChildElement(POM_ELEMENT_CONFIGURATION, jdomElement);
    if (elm == null) {
      return null;
    } else {
      return new JDomConfiguration(elm);
    }
  }

  public static void setConfiguration(Element jdomElement, Object configuration) {
    if (configuration == null) {
      rewriteElement(POM_ELEMENT_CONFIGURATION, null, jdomElement);
    } else if (configuration instanceof JDomConfiguration) {
      Element newJDomConfigurationElement = ((JDomConfiguration) configuration).getJDomElement().clone();

      JDomConfiguration oldJDomConfiguration = getConfiguration(jdomElement);
      if (oldJDomConfiguration == null) {
        jdomElement.addContent(
                jdomElement.getContentSize() - 1,
                asList(new Text("\n" + detectIndentation(jdomElement)), newJDomConfigurationElement));
      } else {
        int replaceIndex = jdomElement.indexOf(oldJDomConfiguration.getJDomElement());
        jdomElement.removeContent(replaceIndex);
        jdomElement.addContent(replaceIndex, newJDomConfigurationElement);
      }

      resetIndentations(jdomElement, detectIndentation(jdomElement));
      resetIndentations(newJDomConfigurationElement, detectIndentation(jdomElement) + "  ");
    }
  }
}
