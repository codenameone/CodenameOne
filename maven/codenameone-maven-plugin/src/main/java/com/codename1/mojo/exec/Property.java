package com.codename1.mojo.exec;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Wrapper class for the systemProperty argument type.
 * 
 * @author Kaare Nilsen (kaare.nilsen@gmail.com)
 */
public class Property
{
    private String key;

    private String value;

    /**
     * <p>
     * Getter for the field <code>key</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getKey()
    {
        return key;
    }

    /**
     * <p>
     * Setter for the field <code>key</code>.
     * </p>
     * 
     * @param key a {@link java.lang.String} object.
     */
    public void setKey( String key )
    {
        this.key = key;
    }

    /**
     * <p>
     * Getter for the field <code>value</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getValue()
    {
        return value;
    }

    /**
     * <p>
     * Setter for the field <code>value</code>.
     * </p>
     * 
     * @param value a {@link java.lang.String} object.
     */
    public void setValue( String value )
    {
        this.value = value;
    }
}
