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

import org.apache.maven.artifact.Artifact;

/**
 * <p>
 * ExecutableDependency class.
 * </p>
 */
public class ExecutableDependency
{
    private String groupId;

    private String artifactId;

    /**
     * <p>
     * Getter for the field <code>groupId</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getGroupId()
    {
        return this.groupId;
    }

    /**
     * <p>
     * Setter for the field <code>groupId</code>.
     * </p>
     * 
     * @param groupId a {@link java.lang.String} object.
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    /**
     * <p>
     * Getter for the field <code>artifactId</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getArtifactId()
    {
        return this.artifactId;
    }

    /**
     * <p>
     * Setter for the field <code>artifactId</code>.
     * </p>
     * 
     * @param artifactId a {@link java.lang.String} object.
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /**
     * <p>
     * Matches the groupId and artifactId.
     * </p>
     * 
     * @param artifact a {@link org.apache.maven.artifact.Artifact} object.
     * @return <code>true</code> if both math, <code>false</code> otherwise.
     */
    public boolean matches( Artifact artifact )
    {
        return artifact.getGroupId().equals( this.getGroupId() )
            && artifact.getArtifactId().equals( this.getArtifactId() );
    }

    /** {@inheritDoc} */
    public String toString()
    {
        return this.groupId + ":" + this.artifactId;
    }

    /** {@inheritDoc} */
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof ExecutableDependency ) )
        {
            return false;
        }

        final ExecutableDependency that = (ExecutableDependency) o;

        if ( artifactId != null ? !artifactId.equals( that.artifactId ) : that.artifactId != null )
        {
            return false;
        }
        if ( groupId != null ? !groupId.equals( that.groupId ) : that.groupId != null )
        {
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    public int hashCode()
    {
        int result;
        result = ( groupId != null ? groupId.hashCode() : 0 );
        result = 29 * result + ( artifactId != null ? artifactId.hashCode() : 0 );
        return result;
    }
}
