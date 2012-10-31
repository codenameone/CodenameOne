/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A cloud object can be persisted to the cloud or locally
 * it is a set of key/value pairs that can be either strings
 * or numbers. There is a 512 character limit on string length!
 *
 * @author Shai Almog
 */
public final class CloudObject implements Externalizable {
    /**
     * Indicates the state of the current object, a new object never persisted
     */
    public static final int STATUS_NEW = 0;

    /**
     * Indicates the state of the current object, an object that is in sync with the database as far as
     * the client code is aware (the client code doesn't check!)
     */
    public static final int STATUS_COMMITTED = 1;

    /**
     * Indicates the state of the current object, a locally modified object that wasn't committed yet
     */
    public static final int STATUS_MODIFIED = 2;

    /**
     * Indicates the state of the current object, an object in the process of committing
     */
    public static final int STATUS_COMMIT_IN_PROGRESS = 3;

    /**
     * Indicates the state of the current object, an object that is in the process of deletion
     */
    public static final int STATUS_DELETE_IN_PROGRESS = 4;

    /**
     * Indicates the state of the current object, a deleted object
     */
    public static final int STATUS_DELETED = 5;

    /**
     * Indicates the state of the current object, an object in the process of refresh
     */
    public static final int STATUS_REFRESH_IN_PROGRESS = 6;
    
    /**
     * A world visible/modifiable object!
     */
    public static final int ACCESS_PUBLIC = 1;

    /**
     * A world visible/modifiable object!
     */
    public static final int ACCESS_PUBLIC_READ_ONLY = 2;

    /**
     * A world visible/modifiable object!
     */
    public static final int ACCESS_APPLICATION = 3;

    /**
     * A world visible object! Can only be modified by its creator
     */
    public static final int ACCESS_APPLICATION_READ_ONLY = 4;
    
    /**
     * An object that can only be viewed or modified by its creator
     */
    public static final int ACCESS_PRIVATE = 5;

    
    private Hashtable values = new Hashtable();
    
    private String cloudId;
    private long lastModified;
    private int status;
    private boolean owner = true;
    private int accessPermissions = ACCESS_PRIVATE;
    
    /**
     * Default constructor
     */
    public CloudObject() {
    }
    
    /**
     * Create an object with different permissions settings
     * 
     * @param permissions one of the ACCESS_* values
     */
    public CloudObject(int permissions) {
        accessPermissions = permissions;
    }
    
    /**
     * Returns one of the status constants in this class
     * @return the status of the object against the cloud
     */
    public int getStatus() {
        return status;
    }
    
    void setStatus(int s) {
        status = s;
    }
    
    void setValues(Hashtable values) {
        this.values = values;
    }
    
    Hashtable getValues() {
        return values;
    }
        

    /**
     * Returns true if this object is owned by me or is world writeable
     * @return ownership status
     */
    public boolean isOwner() {
        return owner;
    }
    
    /**
     * The object id is a unique key that allows you to find an object that was persisted in the 
     * store (a primary key). When it is null the object is effectively unsynchronized!
     * @return the object id or null for an object that isn't fully persisted yet
     */
    public String getCloudId() {
        return cloudId;
    }
    
    void setCloudId(String cloudId) {
        this.cloudId = cloudId;
    }
    
    /**
     * Indicates the last modification date for the object
     * @return the last modification date
     */
    public long getLastModified() {
        return lastModified;
    }
    
    void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
    /**
     * Sets a value that can be no more than 512 characters
     * 
     * @param key the key for the given value
     * @param value the value
     */
    public void setString(String key, String value) {
        if(!owner) {
            throw new RuntimeException("Read only object, you are not the owner");
        }
        if(value == null) {
            values.remove(key);
            return;
        }
        if(value.length() > 512) {
            throw new IllegalArgumentException("String too long!");
        }
        status = STATUS_MODIFIED;
        values.put(key, value);
    }
    
    /**
     * Returns the value for the given key
     * @param key the key 
     * @return a string value
     */
    public String getString(String key) {
        return (String)values.get(key);
    }
    
    /**
     * Delete an entry within the object
     * @param key the key to remove from the object
     */
    public void remove(String key) {
        values.remove(key);
    }
    

    /**
     * Sets a value 
     * 
     * @param key the key for the given value
     * @param value the value
     */
    public void setLong(String key, long value) {
        if(!owner) {
            throw new RuntimeException("Read only object, you are not the owner");
        }
        status = STATUS_MODIFIED;
        values.put(key, new Long(value));
    }
    
    /**
     * Sets a value 
     * 
     * @param key the key for the given value
     * @param value the value
     */
    public void setLong(String key, Long value) {
        if(!owner) {
            throw new RuntimeException("Read only object, you are not the owner");
        }
        status = STATUS_MODIFIED;
        values.put(key, value);
    }
    
    /**
     * Returns the value for the given key
     * @param key the key 
     * @return a long value
     */
    public Long getLong(String key) {
        return (Long)values.get(key);
    }

    /**
     * Sets a value 
     * 
     * @param key the key for the given value
     * @param value the value
     */
    public void setInteger(String key, int value) {
        if(!owner) {
            throw new RuntimeException("Read only object, you are not the owner");
        }
        status = STATUS_MODIFIED;
        values.put(key, new Integer(value));
    }
    
    /**
     * Sets a value 
     * 
     * @param key the key for the given value
     * @param value the value
     */
    public void setInteger(String key, Integer value) {
        if(!owner) {
            throw new RuntimeException("Read only object, you are not the owner");
        }
        status = STATUS_MODIFIED;
        values.put(key, value);
    }
    
    /**
     * Returns the value for the given key
     * @param key the key 
     * @return a value
     */
    public Integer getInteger(String key) {
        return (Integer)values.get(key);
    }

    /**
     * Sets a value 
     * 
     * @param key the key for the given value
     * @param value the value
     */
    public void setDouble(String key, long value) {
        if(!owner) {
            throw new RuntimeException("Read only object, you are not the owner");
        }
        status = STATUS_MODIFIED;
        values.put(key, new Double(value));
    }
    
    /**
     * Sets a value 
     * 
     * @param key the key for the given value
     * @param value the value
     */
    public void setDouble(String key, Double value) {
        if(!owner) {
            throw new RuntimeException("Read only object, you are not the owner");
        }
        status = STATUS_MODIFIED;
        values.put(key, value);
    }
    
    /**
     * Returns the value for the given key
     * @param key the key 
     * @return a value
     */
    public Double getDouble(String key) {
        return (Double)values.get(key);
    }

    /**
     * Sets a value 
     * 
     * @param key the key for the given value
     * @param value the value
     */
    public void setFloat(String key, float value) {
        if(!owner) {
            throw new RuntimeException("Read only object, you are not the owner");
        }
        status = STATUS_MODIFIED;
        values.put(key, new Float(value));
    }
    
    /**
     * Sets a value 
     * 
     * @param key the key for the given value
     * @param value the value
     */
    public void setFloat(String key, Float value) {
        if(!owner) {
            throw new RuntimeException("Read only object, you are not the owner");
        }
        status = STATUS_MODIFIED;
        values.put(key, value);
    }
    
    /**
     * Returns the value for the given key
     * @param key the key 
     * @return a value
     */
    public Float getFloat(String key) {
        return (Float)values.get(key);
    }

    /**
     * Sets a value 
     * 
     * @param key the key for the given value
     * @param value the value
     */
    public void setBoolean(String key, boolean value) {
        if(!owner) {
            throw new RuntimeException("Read only object, you are not the owner");
        }
        status = STATUS_MODIFIED;
        values.put(key, new Boolean(value));
    }
    
    /**
     * Sets a value 
     * 
     * @param key the key for the given value
     * @param value the value
     */
    public void setBoolean(String key, Boolean value) {
        if(!owner) {
            throw new RuntimeException("Read only object, you are not the owner");
        }
        status = STATUS_MODIFIED;
        values.put(key, value);
    }
    
    /**
     * Returns the value for the given key
     * @param key the key 
     * @return a value
     */
    public Boolean getBoolean(String key) {
        return (Boolean)values.get(key);
    }

    /**
     * @inheritDoc
     */
    public int getVersion() {
        return 1;
    }
    
    /**
     * @inheritDoc
     */
    public void externalize(DataOutputStream out) throws IOException {
        Util.writeUTF(cloudId, out);
        out.writeBoolean(owner);
        out.writeByte(getAccessPermissions());
        out.writeLong(lastModified);
        out.writeInt(status);
        Util.writeObject(values, out);
    }

    
    public String getObjectId() {
        return "CloudObject";
    }

    /**
     * @inheritDoc
     */
    public void internalize(int version, DataInputStream in) throws IOException {
        cloudId = Util.readUTF(in);
        owner = in.readBoolean();
        accessPermissions = in.readByte();
        lastModified = in.readLong();
        status = in.readInt();
        values = (Hashtable)Util.readObject(in);
    }

    /**
     * The access permissions for an object can only be changed for an object in which
     * the current user is the owner
     * @return the accessPermissions
     */
    public int getAccessPermissions() {
        return accessPermissions;
    }
    
    /**
     * @inheritDoc
     */
    public boolean equals(Object o) {
        if(!(o instanceof CloudObject)) {
            return false;
        }
        CloudObject cl = (CloudObject)o;
        if(cloudId == null && cl.cloudId == null) {
            return values.equals(cl.values);
        }
        if(cloudId == null || cl.cloudId == null) {
            return false;
        }
        return cloudId.equals(cl.cloudId);
    }

    /**
     * @inheritDoc
     */
    public int hashCode() {
        if(cloudId != null) {
            return cloudId.hashCode();
        }
        return 0;
    }
}
