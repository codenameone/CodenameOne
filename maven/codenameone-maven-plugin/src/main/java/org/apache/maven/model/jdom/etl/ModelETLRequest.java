package org.apache.maven.model.jdom.etl;

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

/**
 * @author Robert Scholte (for <a href="https://github.com/apache/maven-release/">Maven Release projct</a>, version 3.0)
 */
public class ModelETLRequest {

  /**
   * The Unix line separator
   */
  public static final String UNIX_LS = "\n";

  /**
   * The Windows line separator
   */
  public static final String WINDOWS_LS = "\r\n";

  /**
   * The Classic Mac line separator
   */
  public static final String CLASSIC_MAC_LS = "\r";

  private boolean addSchema;
  private String lineSeparator = System.getProperty("line.separator");

  public boolean isAddSchema() {
    return addSchema;
  }

  public void setAddSchema(boolean addSchema) {
    this.addSchema = addSchema;
  }

  public String getLineSeparator() {
    return lineSeparator;
  }

  public void setLineSeparator(String lineSeparator) {
    this.lineSeparator = lineSeparator;
  }
}