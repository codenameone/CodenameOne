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
package com.codename1.cloud;

import com.codename1.cloud.BindTarget;
import com.codename1.io.Externalizable;
import com.codename1.io.Util;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A cloud object can be persisted to the cloud or locally
 * it is a set of key/value pairs that can be either strings
 * or numbers. There is a 512 character limit on string length!
 * Notice: keys starting with CN1 are reserved for internal usage!
 *
 * @author Shai Almog
 * @deprecated the cloud storage API is no longer supported, we recommend switching to a solution such as parse4cn1
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
     * A world visible object! Can only be modified by its creator.
     */
    public static final int ACCESS_PUBLIC_READ_ONLY = 2;

    /**
     * An application visible/modifiable object!
     */
    public static final int ACCESS_APPLICATION = 3;

    /**
     * An application scope readable object! Can only be modified by its creator
     */
    public static final int ACCESS_APPLICATION_READ_ONLY = 4;
    
    /**
     * An object that can only be viewed or modified by its creator
     */
    public static final int ACCESS_PRIVATE = 5;
    
    
    /**
     * Changes to the bound property won't be reflected into the bound cloud object until commit binding is invoked
     */
    public static final int BINDING_DEFERRED = 1;

    /**
     * Changes to the bound property will be reflected instantly into the cloud object
     */
    public static final int BINDING_IMMEDIATE = 2;

    /**
     * Changes to the bound property will be reflected instantly into the cloud object and the object would
     * be saved immediately (not committed!).
     */
    public static final int BINDING_AUTO_SAVE = 3;

    private static Hashtable<String, CustomProperty> custom = new Hashtable<String, CustomProperty>();
    private Hashtable values = new Hashtable();
    private Hashtable deferedValues;
    
    private String cloudId;
    private long lastModified;
    private int status;
    private boolean owner = true;
    private int accessPermissions = ACCESS_PRIVATE;
    
    /**
     * Default constructor for externalization purposes only!
     */
    public CloudObject() {}
    
    /**
     * Constructor
     * @param type the type of the object
     */
    public CloudObject(String type) {
        values.put(CloudStorage.TYPE_FIELD, type);
    }
    
    /**
     * Create an object with different permissions settings
     * 
     * @param type the type of the object
     * @param permissions one of the ACCESS_* values
     */
    public CloudObject(String type, int permissions) {
        accessPermissions = permissions;
        values.put(CloudStorage.TYPE_FIELD, type);
    }
    
    CloudObject(int permissions) {
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
     * Set the type of the object
     * @param type the type of the field
     */
    public void setType(String type) {
        setString(CloudStorage.TYPE_FIELD, type);
    }

    /**
     * Returns the type of the object
     * @return the type of the object
     */
    public String getType() {
        return getString(CloudStorage.TYPE_FIELD);
    }
    
    /**
     * Only indexed values can be queried and sorted
     * @param index the index which must be a value between 1 and 10.
     * @param value the value for the given index
     */
    public void setIndexString(int index, String value) {
        if(index > 10 || index < 1) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }
        setString(CloudStorage.INDEX_FIELD + index, value);
    }
    
    /**
     * Returns the index value for the given index number
     * 
     * @param index the index number
     * @return the value of this entry for that index as a String
     */
    public String getIndexString(int index) {
        return getString(CloudStorage.INDEX_FIELD + index);
    }
    
    /**
     * Only indexed values can be queried and sorted
     * @param index the index which must be a value between 1 and 10.
     * @param value the value for the given index
     */
    public void setIndexLong(int index, long value) {
        if(index > 10 || index < 1) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }
        setLong(CloudStorage.INDEX_FIELD + index, value);
    }
    
    /**
     * Returns the index value for the given index number
     * 
     * @param index the index number
     * @return the value of this entry for that index as a Long value
     */
    public Long getIndexLong(int index) {
        return getLong(CloudStorage.INDEX_FIELD + index);
    }

    
    /**
     * Only indexed values can be queried and sorted
     * @param index the index which must be a value between 1 and 10.
     * @param value the value for the given index
     */
    public void setIndexDouble(int index, double value) {
        if(index > 10 || index < 1) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }
        setDouble(CloudStorage.INDEX_FIELD + index, value);
    }
    
    /**
     * Returns the index value for the given index number
     * 
     * @param index the index number
     * @return the value of this entry for that index as a Double value
     */
    public Double getIndexDouble(int index) {
        return getDouble(CloudStorage.INDEX_FIELD + index);
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
     * Allows us to extract an object from the cloud object without knowing its type in advance
     * or whether it exists
     * @param key the key for the object
     * @return the value of the object
     */
    public Object getObject(String key) {
        Object o = values.get(key);
        if(o == null) {
            CustomProperty cp = custom.get(key);
            if(cp != null) {
                return cp.propertyValue(this, key);
            }
        }
        return o;
    }
    
    /**
     * Install a custom property on the given property name
     * 
     * @param key the key on which to install the custom property
     * @param cp the custom property implementation
     */
    public static void setCustomProperty(String key, CustomProperty cp) {
        if(cp == null) {
            custom.remove(key);
        } else {
            custom.put(key, cp);
        }
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
        return (String)getObject(key);
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
        Object o = getObject(key);
        if(o instanceof Integer) {
            return new Long(((Integer)o).intValue());
        }
        return (Long)o;
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
        Object o = getObject(key);
        if(o instanceof Long) {
            return new Integer((int)((Long)o).longValue());
        }
        return (Integer)o;
    }

    /**
     * Sets a value 
     * 
     * @param key the key for the given value
     * @param value the value
     */
    public void setDouble(String key, double value) {
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
        return (Double)getObject(key);
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
        Object o = getObject(key);
        if(o instanceof Double) {
            return new Float(((Double)o).floatValue());
        }
        return (Float)o;
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
        return (Boolean)getObject(key);
    }

    /**
     * {@inheritDoc}
     */
    public int getVersion() {
        return 1;
    }
    
    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public int hashCode() {
        if(cloudId != null) {
            return cloudId.hashCode();
        }
        return 0;
    }
    
    /**
     * Binds a UI tree to the cloud object so its values automatically update in the cloud object
     * 
     * @param ui the component tree to bind
     * @param defer bind settings whether to defer the binding which requires developers to explicitly commit
     * the binding to perform the changes
     * @param objectLead if set to true the UI property is initialized from values in the CloudObject, if false
     * the cloud object property is initialized from the UI
     */
    public void bindTree(Container ui, int defer, boolean objectLead) {
        int componentCount = ui.getComponentCount();
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component c = ui.getComponentAt(iter);
            if(c instanceof Container) {
                bindTree((Container)c, defer, objectLead);
                continue;
            }
            
            String bind = c.getCloudBoundProperty();
            if(bind != null && bind.length() > 0) {
                String attributeName = c.getCloudDestinationProperty();            
                if(attributeName != null) {
                    bindProperty(c, bind, attributeName, defer, objectLead);
                }
            }
        }
    }
    
    /**
     * Clears the binding to this component tree
     * @param ui the container whose children might be bound
     */
    public void unbindTree(Container ui) {
        int componentCount = ui.getComponentCount();
        for(int iter = 0 ; iter < componentCount ; iter++) {
            Component c = ui.getComponentAt(iter);
            if(c instanceof Container) {
                unbindTree((Container)c);
                continue;
            }
            
            String bind = c.getCloudBoundProperty();
            if(bind != null && bind.length() > 0) {
                String attributeName = c.getCloudDestinationProperty();            
                if(attributeName != null) {
                    unbindProperty(c, bind);
                }
            }
        }
    }
    
    /**
     * Binds a property value within the given component to this cloud object, this means that
     * when the component changes the cloud object changes unless deferred. If the defer flag is 
     * false all changes are stored in a temporary location and only "committed" once commitBindings()
     * is invoked.
     * @param cmp the component to bind
     * @param propertyName the name of the property in the bound component
     * @param attributeName the key within the cloud object
     * @param defer bind settings whether to defer the binding which requires developers to explicitly commit
     * the binding to perform the changes
     * @param objectLead if set to true the UI property is initialized from values in the CloudObject, if false
     * the cloud object property is initialized from the UI
     */
    public void bindProperty(Component cmp, final String propertyName, final String attributeName, final int defer, boolean objectLead) {
        if(objectLead) {
            Object val = values.get(attributeName);
            Object cmpVal = cmp.getBoundPropertyValue(propertyName);
            if(val == null) {
                if(cmpVal != null) {
                    cmp.setBoundPropertyValue(propertyName, null);
                }
            } else {
                if(cmpVal == null || !(val.equals(cmpVal))) {
                    cmp.setBoundPropertyValue(propertyName, val);
                }
            }
        } else {
            Object val = values.get(attributeName);
            Object cmpVal = cmp.getBoundPropertyValue(propertyName);
            if(cmpVal == null) {
                if(val != null) {
                    values.remove(attributeName);
                    status = STATUS_MODIFIED;
                }
            } else {
                if(val == null || !(val.equals(cmpVal))) {
                    values.put(attributeName, cmpVal);
                    status = STATUS_MODIFIED;
                }
            }
        }
        BindTarget target = new BindTarget() {
            public void propertyChanged(Component source, String propertyName, Object oldValue, Object newValue) {
                switch(defer) {
                    case BINDING_DEFERRED:
                        if(deferedValues == null) {
                            deferedValues = new Hashtable();
                        }
                        deferedValues.put(attributeName, newValue);
                        break;
                    case BINDING_IMMEDIATE:
                        values.put(attributeName, newValue);
                        status = STATUS_MODIFIED;
                        break;
                    case BINDING_AUTO_SAVE:
                        values.put(attributeName, newValue);
                        status = STATUS_MODIFIED;
                        CloudStorage.getInstance().save(CloudObject.this);
                        break;
                }
            }
        };
        cmp.bindProperty(propertyName, target);
        cmp.putClientProperty("CN1Bind" + propertyName, target);
    }
    
    /**
     * Releases the binding for the specific property name
     * @param cmp the component
     * @param propertyName the name of the property
     */
    public void unbindProperty(Component cmp, String propertyName) {
        BindTarget t = (BindTarget)cmp.getClientProperty("CN1Bind" + propertyName);
        cmp.unbindProperty(propertyName, t);;
    }
    
    /**
     * If deferred changes exist this method applies these changes to the data
     */
    public void commitBinding() {
        if(deferedValues != null && deferedValues.size() > 0) {
            Enumeration en = deferedValues.keys();
            while(en.hasMoreElements()) {
                Object k = en.nextElement();
                values.put(k, deferedValues.get(k));
            }
            deferedValues = null;
        }
    }
    
    /**
     * If deferred changes exist this method discards such values
     */
    public void cancelBinding() {
        deferedValues = null;
    }
}
