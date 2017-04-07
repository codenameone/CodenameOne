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

package com.codename1.properties;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.Row;
import com.codename1.io.Log;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A simple ORM wrapper for property objects. This is a very poor mans ORM that doesn't handle relations
 * properly at this time. 
 *
 * @author Shai Almog
 */
public class SQLMap {
    private boolean verbose = true;
    public static enum SqlType {
        SQL_TEXT("TEXT"),
        SQL_INTEGER("INTEGER") {
            @Override
            protected Object getValue(Row row, int index) throws IOException {
                return row.getInteger(index);
            }
        },
        SQL_LONG("INTEGER") {
            @Override
            protected Object getValue(Row row, int index) throws IOException {
                return row.getLong(index);
            }
        },
        SQL_SHORT("INTEGER") {
            @Override
            protected Object getValue(Row row, int index) throws IOException {
                return row.getShort(index);
            }
        },
        SQL_FLOAT("REAL") {
            @Override
            protected Object getValue(Row row, int index) throws IOException {
                return row.getFloat(index);
            }
        },
        SQL_DOUBLE("REAL") {
            @Override
            protected Object getValue(Row row, int index) throws IOException {
                return row.getDouble(index);
            }
        };
        
        String dbType;
        
        SqlType(String dbType) {
            this.dbType = dbType;
        }
        
        protected Object getValue(Row row, int index) throws IOException{
            return row.getString(index);
        }
    }
    
    private Database db;
    
    private SQLMap() {}
    
    /**
     * Creates an SQL Map instance to the given database instance
     * @param db the database connection instance
     * @return an instance of the SQL mapping
     */
    public static SQLMap create(Database db) {
        SQLMap s = new SQLMap();
        s.db = db;
        return s;
    }
    
    /**
     * Sets the primary key for the component
     * @param cmp the business object
     * @param pk the primary key field
     */
    public void setPrimaryKey(PropertyBusinessObject cmp, Property pk) {
        cmp.getPropertyIndex().putMetaDataOfClass("cn1$pk", pk.getName());
    }


    /**
     * Sets the sql type for the column
     * @param p the property
     * @param type one of the enum values representing supported SQL data types
     */
    public void setSqlType(PropertyBase p, SqlType type) {
        p.putClientProperty("cn1$colType", type);
    }

    /**
     * Returns the SQL type for the given column
     * 
     * @param p the property 
     * @return the sql data type
     */
    public SqlType getSqlType(PropertyBase p) {
        SqlType s = (SqlType)p.getClientProperty("cn1$colType");
        if(s == null) {
            if(p instanceof Property) {
                Object val = ((Property)p).get();
                if(val != null) {
                    if(val instanceof Long) {
                        return SqlType.SQL_LONG;
                    }
                    if(val instanceof Integer) {
                        return SqlType.SQL_INTEGER;
                    }
                    if(val instanceof Short) {
                        return SqlType.SQL_SHORT;
                    }
                    if(val instanceof Float) {
                        return SqlType.SQL_FLOAT;
                    }
                    if(val instanceof Double) {
                        return SqlType.SQL_DOUBLE;
                    }
                }
            }
            return SqlType.SQL_TEXT;
        }
        return s;
    }
    
    /**
     * By default the table name matches the property index name unless explicitly modified with this method
     * @param cmp the properties business object
     * @param name the name of the table
     */
    public void setTableName(PropertyBusinessObject cmp, String name) {
        cmp.getPropertyIndex().putMetaDataOfClass("cn1$tableName", name);
    }

    /**
     * By default the table name matches the property index name unless explicitly modified with this method
     * @param cmp the properties business object
     * @return the name of the table
     */
    public String getTableName(PropertyBusinessObject cmp) {
        String s = (String)cmp.getPropertyIndex().getMetaDataOfClass("cn1$tableName");
        if(s != null) {
            return s;
        }
        return cmp.getPropertyIndex().getName();
    }
    
    /**
     * By default the column name matches the property name unless explicitly modified with this method
     * @param prop a property instance, this will apply to all the property instances for the type
     * @param name the name of the column
     */
    public void setColumnName(PropertyBase prop, String name) {
        prop.putClientProperty("cn1$sqlColumn", name);
    }
    
    /**
     * By default the column name matches the property name unless explicitly modified with this method
     * @param prop a property instance, this will apply to all the property instances for the type
     * @return the name of the property
     */
    public String getColumnName(PropertyBase prop) {
        String val = (String)prop.getClientProperty("cn1$sqlColumn");
        if(val == null) {
            return prop.getName();
        }
        return val;
    }
    
    /**
     * Creates a table matching the given property component if one doesn't exist yet
     * @param cmp the business object
     * @return true if the table was created false if it already existed
     */
    public boolean createTable(PropertyBusinessObject cmp) throws IOException {
        String tableName = getTableName(cmp);
        Cursor cr = null;
        boolean has = false;
        try {
            cr = executeQuery("SELECT * FROM sqlite_master WHERE type='table' AND name='" + tableName +"'");
            has = cr.next();
        } finally {
            if(cr != null) {
                cr.close();
            } 
        }
        if(has) {
            return false;
        }
        StringBuilder createStatement = new StringBuilder("CREATE TABLE ");
        createStatement.append(tableName);
        createStatement.append(" (");

        String pkName = (String)cmp.getPropertyIndex().getMetaDataOfClass("cn1$pk");
        boolean first = true;
        for(PropertyBase p : cmp.getPropertyIndex()) {
            if(!first) {
                createStatement.append(",");
            }
            first = false;
            String columnName = getColumnName(p);
            createStatement.append(columnName);
            createStatement.append(" ");
            createStatement.append(getSqlType(p).dbType);
            if(columnName.equalsIgnoreCase(pkName)) {
                createStatement.append(" PRIMARY KEY");
            }
        }
        
        createStatement.append(")");
        
        execute(createStatement.toString());
        return true;
    }

    private void execute(String stmt) throws IOException {
        if(verbose) {
            Log.p(stmt);
        }
        db.execute(stmt);
    }

    private void execute(String stmt, Object[] args) throws IOException {
        if(verbose) {
            Log.p(stmt);
        }
        db.execute(stmt, args);
    }
    
    private Cursor executeQuery(String stmt, Object[] args) throws IOException {
        if(verbose) {
            Log.p(stmt);
        }
        return db.executeQuery(stmt, args);
    }

    
    private Cursor executeQuery(String stmt) throws IOException {
        if(verbose) {
            Log.p(stmt);
        }
        return db.executeQuery(stmt);
    }

    /**
     * Drop a table matching the given property component
     * @param cmp the business object
     */
    public void dropTable(PropertyBusinessObject cmp) throws IOException {
        String tableName = getTableName(cmp);
        execute("Drop table " + tableName);
    }

    /**
     * Adds a new business object into the database
     * @param cmp the business component
     */
    public void insert(PropertyBusinessObject cmp) throws IOException {
        String tableName = getTableName(cmp);
        StringBuilder createStatement = new StringBuilder("INSERT INTO ");
        createStatement.append(tableName);
        createStatement.append(" (");

        int count = 0;
        Object[] values = new Object[cmp.getPropertyIndex().getSize()];
        for(PropertyBase p : cmp.getPropertyIndex()) {
            if(count > 0) {
                createStatement.append(",");
            }
            if(p instanceof Property) {
                values[count] = ((Property)p).get();
            } else {
                // TODO
                values[count] = null;
            }
            count++;
            String columnName = getColumnName(p);
            createStatement.append(columnName);
        }
        
        createStatement.append(") VALUES (?");

        for(int iter = 1 ; iter < values.length; iter++) {
            createStatement.append(",?");
        }
        
        createStatement.append(")");
        
        execute(createStatement.toString(), values);
    }
    
    /**
     * The equivalent of an SQL update assumes that the object is already in the database
     * @param cmp the component
     * @throws IOException 
     */
    public void update(PropertyBusinessObject cmp) throws IOException {
        String pkName = (String)cmp.getPropertyIndex().getMetaDataOfClass("cn1$pk");
        if(pkName == null) {
            throw new IOException("Primary key required for update");
        }
        String tableName = getTableName(cmp);
        StringBuilder createStatement = new StringBuilder("UPDATE ");
        createStatement.append(tableName);
        createStatement.append(" SET ");

        int count = 0;
        Object[] values;
        values = new Object[cmp.getPropertyIndex().getSize() + 1];
        for(PropertyBase p : cmp.getPropertyIndex()) {
            if(count > 0) {
                createStatement.append(",");
            }
            if(p instanceof Property) {
                values[count] = ((Property)p).get();
            } else {
                // TODO
                values[count] = null;
            }
            count++;
            String columnName = getColumnName(p);
            createStatement.append(columnName);
            createStatement.append(" = ?");
        }
        
        createStatement.append(" WHERE ");

        createStatement.append(pkName);
        createStatement.append(" = ?");
        
        Property p = (Property)cmp.getPropertyIndex().getIgnoreCase(pkName);
        values[values.length - 1] = p.get();

        execute(createStatement.toString(), values);
    }
    
    /**
     * Deletes a table row matching the component
     * @param cmp the component
     */
    public void delete(PropertyBusinessObject cmp) throws IOException {
        String pkName = (String)cmp.getPropertyIndex().getMetaDataOfClass("cn1$pk");
        String tableName = getTableName(cmp);
        StringBuilder createStatement = new StringBuilder("DELETE FROM ");
        createStatement.append(tableName);
        createStatement.append(" WHERE ");


        if(pkName != null) {
            createStatement.append(pkName);
            createStatement.append(" = ?");
            Property p = (Property)cmp.getPropertyIndex().getIgnoreCase(pkName);
            execute(createStatement.toString(), new Object[]{ p.get() });
        } else {
            int count = 0;
            Object[] values = new Object[cmp.getPropertyIndex().getSize()];
            for(PropertyBase p : cmp.getPropertyIndex()) {
                if(count == 0) {
                    createStatement.append(",");
                }
                if(p instanceof Property) {
                    values[count] = ((Property)p).get();
                } else {
                    // TODO
                    values[count] = null;
                }
                count++;
                String columnName = getColumnName(p);
                createStatement.append(columnName);
                createStatement.append(" = ?");
            }
            execute(createStatement.toString(), values);
        }        
    }
    
    /**
     * Fetches the components from the database matching the given cmp description, the fields that aren't
     * null within the cmp will match the where clause
     * @param cmp the component to match
     * @param orderBy the column to order by, can be null to ignore order
     * @param ascending true to indicate ascending order
     * @param maxElements the maximum number of elements returned can be 0 or lower to ignore
     * @param page  the page within the query to match the max elements value
     * @return the result of the query 
     */
    public java.util.List<PropertyBusinessObject> select(PropertyBusinessObject cmp, Property orderBy, boolean ascending, int maxElements, int page) throws IOException, InstantiationException {
        String tableName = getTableName(cmp);
        StringBuilder createStatement = new StringBuilder("SELECT * FROM ");
        createStatement.append(tableName);
        ArrayList<Object> params = new ArrayList<Object>();
        
        createStatement.append(" WHERE ");
        boolean found = false;
        for(PropertyBase p : cmp.getPropertyIndex()) {
            if(p instanceof Property) {
                if(((Property)p).get() != null) {
                    if(found) {
                        createStatement.append(", ");
                    }
                    found = true;
                    params.add(((Property)p).get());
                    createStatement.append(getColumnName(p));
                    createStatement.append(" = ?");
                }
            }
        }

        // all properties are null undo the where append
        if(!found) {
            createStatement = new StringBuilder("SELECT * FROM ");
            createStatement.append(tableName);
        }
        
        if(orderBy != null) {
            createStatement.append(" ORDER BY ");
            createStatement.append(getColumnName(orderBy));
            if(!ascending) {
                createStatement.append(" DESC");
            } 
        }
        
        if(maxElements > 0) {
            createStatement.append(" LIMIT ");
            createStatement.append(maxElements);
            if(page > 0) {
                createStatement.append(" OFFSET ");
                createStatement.append(page * maxElements);
            }
        }
        
        Cursor c = null;
        try {
            ArrayList<PropertyBusinessObject> response = new ArrayList<PropertyBusinessObject>();
            c = executeQuery(createStatement.toString(), params.toArray());
            while(c.next()) {
                PropertyBusinessObject pb = (PropertyBusinessObject)cmp.getClass().newInstance();
                for(PropertyBase p : pb.getPropertyIndex()) {
                    Row currentRow = c.getRow();
                    SqlType t = getSqlType(p);
                    Object value = t.getValue(currentRow, c.getColumnIndex(getColumnName(p)));
                    if(p instanceof Property) {
                        ((Property)p).set(value);
                    } 
                }
                response.add(pb);
            }
            c.close();
            return response;
        } catch(Throwable t) {
            Log.e(t);
            if(c != null) {
                c.close();
            }
            if(t instanceof IOException) {
                throw ((IOException)t);
            } else {
                throw new IOException(t.toString());
            }
        }
    }
    
    /**
     * Toggle verbose mode 
     * @param verbose 
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
