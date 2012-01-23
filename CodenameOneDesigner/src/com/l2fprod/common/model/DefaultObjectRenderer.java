/**
 * @PROJECT.FULLNAME@ @VERSION@ License.
 *
 * Copyright @YEAR@ L2FProd.com
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
package com.l2fprod.common.model;

import com.l2fprod.common.util.ResourceManager;
import com.l2fprod.common.util.converter.ConverterRegistry;

import java.io.File;

/**
 * DefaultObjectRenderer. <br>
 *  
 */
public class DefaultObjectRenderer implements ObjectRenderer {

  private boolean idVisible = false;

  public void setIdVisible(boolean b) {
    idVisible = b;
  }

  public String getText(Object object) {
    if (object == null) {
      return null;
    }

    // lookup the shared ConverterRegistry
    try {
      return (String)ConverterRegistry.instance().convert(String.class, object);
    } catch (IllegalArgumentException e) {
    }

    if (object instanceof Boolean) {
      return Boolean.TRUE.equals(object)
        ? ResourceManager.common().getString("true")
        : ResourceManager.common().getString("false");
    }

    if (object instanceof File) {
      return ((File)object).getAbsolutePath();
    }

    StringBuffer buffer = new StringBuffer();
    if (idVisible && object instanceof HasId) {
      buffer.append(((HasId)object).getId());
    }
    if (object instanceof TitledObject) {
      buffer.append(((TitledObject)object).getTitle());
    }
    if (!(object instanceof HasId || object instanceof TitledObject)) {
      buffer.append(String.valueOf(object));
    }
    return buffer.toString();
  }

}
