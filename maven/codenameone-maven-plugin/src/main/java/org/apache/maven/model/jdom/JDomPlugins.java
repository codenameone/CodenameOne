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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import static org.apache.maven.model.jdom.util.JDomCfg.POM_ELEMENT_PLUGIN;
import static org.apache.maven.model.jdom.util.JDomUtils.addElement;
import static org.apache.maven.model.jdom.util.JDomUtils.insertNewElement;
import static org.apache.maven.model.jdom.util.JDomUtils.removeChildElement;

public class JDomPlugins extends ArrayList<Plugin> implements JDomBacked {

  private final Element jdomElement;

  public JDomPlugins(Element jdomElement) {
    super(transformToJDomPluginList(getPluginElements(jdomElement)));
    this.jdomElement = jdomElement;
  }

  private static List<Element> getPluginElements(Element plugins) {
    return plugins.getContent(new ElementFilter(POM_ELEMENT_PLUGIN, plugins.getNamespace()));
  }

  private static List<JDomPlugin> transformToJDomPluginList(List<Element> pluginElements) {
    List<JDomPlugin> jDomPluginList = new ArrayList<>(pluginElements.size());
    for (Element pluginElement : pluginElements) {
      jDomPluginList.add(new JDomPlugin(pluginElement));
    }
    return jDomPluginList;
  }

  @Override
  public void add(int index, Plugin plugin) {
    Element newElement;
    if (plugin instanceof JDomPlugin) {
      if (index == -1) {
        addElement(((JDomPlugin) plugin).getJDomElement().clone(), jdomElement);
      } else {
        addElement(((JDomPlugin) plugin).getJDomElement().clone(), jdomElement, index);
      }
    } else {
      newElement = index == -1
          ? insertNewElement(POM_ELEMENT_PLUGIN, jdomElement)
          : insertNewElement(POM_ELEMENT_PLUGIN, jdomElement, index);
      JDomPlugin jDomPlugin = new JDomPlugin(newElement);

      jDomPlugin.setGroupId(plugin.getGroupId());
      jDomPlugin.setArtifactId(plugin.getArtifactId());
      jDomPlugin.setVersion(plugin.getVersion());

      List<Dependency> dependencies = plugin.getDependencies();
      if (!dependencies.isEmpty()) {
        jDomPlugin.setDependencies(dependencies);
      }

      if (plugin.isExtensions()) {
        jDomPlugin.setExtensions(true);
      }

      List<PluginExecution> executions = plugin.getExecutions();
      if (!executions.isEmpty()) {
        jDomPlugin.setExecutions(executions);
      }

      if (!plugin.isInherited()) {
        jDomPlugin.setInherited(false);
      }
    }

    if (index == -1) {
      super.add(plugin);
    } else {
      super.add(index, plugin);
    }
  }

  @Override
  public boolean remove(final Object plugin) {
    Plugin removePlugin = (Plugin) plugin;
    for (Plugin candidate : this) {
      if (Objects.equals(candidate.getGroupId(), removePlugin.getGroupId())
              && Objects.equals(candidate.getArtifactId(), removePlugin.getArtifactId())) {
        removeChildElement(jdomElement, ((JDomPlugin) candidate).getJDomElement());
        return super.remove(removePlugin);
      }
    }
    return false;
  }

  @Override
  public boolean addAll(Collection<? extends Plugin> plugins) {
    boolean added = false;
    for (Plugin plugin : plugins) {
      added |= this.add(plugin);
    }
    return added;
  }

  @Override
  public boolean addAll(int index, Collection<? extends Plugin> plugins) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> plugins) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> plugins) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    while (size() > 0) {
      remove(0);
    }
  }

  @Override
  public Plugin set(int index, Plugin plugin) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(Plugin plugin) {
    add(-1, plugin);
    return true;
  }

  @Override
  public Plugin remove(int index) {
    Plugin plugin = get(index);
    remove(plugin);
    return plugin;
  }

  @Override
  public int lastIndexOf(Object plugin) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<Plugin> listIterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<Plugin> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Plugin> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Object clone() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Element getJDomElement() {
    return jdomElement;
  }
}
