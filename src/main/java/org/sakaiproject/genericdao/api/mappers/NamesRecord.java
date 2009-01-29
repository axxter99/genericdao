/**
 * $Id$
 * $URL$
 * NamesRecord.java - genericdao - Apr 25, 2008 10:31:17 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2
 * 
 * A copy of the Apache License, Version 2 has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski@gmail.com) (aaronz@vt.edu) (aaron@caret.cam.ac.uk)
 */

package org.sakaiproject.genericdao.api.mappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores the names mapping from persistent entity properties to database column names,
 * this is generated by reflection if not defined for the persistent entity
 * 
 * @author Aaron Zeckoski (azeckoski@gmail.com)
 */
public class NamesRecord {

    private Map<String, String> propertyToColumn = new ConcurrentHashMap<String, String>();
    private Map<String, String> columnToProperty = new ConcurrentHashMap<String, String>();
    private Map<String, String> foreignKeyPropertyToColumn = new ConcurrentHashMap<String, String>();
    /**
     * Allows for special handling of the types and will convert all data going into the DB into the
     * type indicated in this map for the column name stored here, by default this will be totally null, 
     * will also convert the data coming out from the
     * JDBC type to the persistent class type
     */
    private Map<String, Class<?>> columnToType = new ConcurrentHashMap<String, Class<?>>();

    /**
     * Store a mapping from property to column
     * @param property the property from a persistent object
     * @param column the db table column which maps to the property
     */
    public void setNameMapping(String property, String column) {
        setNameMapping(property, column, null);
    }

    /**
     * Store a mapping from property to column with a conversion type for the column
     * @param property the property from a persistent object
     * @param column the db table column which maps to the property
     * @param type the conversion type to convert this to in input into the DB
     */
    public void setNameMapping(String property, String column, Class<?> type) {
        if (property == null || column == null) {
            throw new IllegalArgumentException("property and column must not be null");
        }
        propertyToColumn.put(property, column);
        columnToProperty.put(column, property);
        if (columnToProperty.size() != propertyToColumn.size()) {
            propertyToColumn.remove(property);
            columnToProperty.remove(column);
            throw new IllegalArgumentException("Invalid state of mapping, there is an uneven set of properties to columns which " +
                    "indicates that this property ("+property+") and column ("+column+") combination has overwritten an existing value");
        }
        if (type != null) {
            columnToType.put(property, type);
        }
    }


    private String idProperty = DataMapper.DEFAULT_ID_PROPERTY;
    /**
     * @return the property name that represents the unique identifiers,
     * this will be the default of "id" if not set
     */
    public String getIdProperty() {
        return idProperty;
    }
    /**
     * Special handling for setting the unique identifier property for a class,
     * this is optional but recommended
     * @param property the property from a persistent object of the unique identifier
     * @param column the db table column which maps to the identifier property
     */
    public void setIdentifierProperty(String property) {
        if (! propertyToColumn.containsKey(property)) {
            throw new IllegalArgumentException("this property ("+property+") is not one of the mappings, " +
                    "you must specify an id property which is an existing mapping, i.e. this cannot be " +
            "set until after you have added the id property mapping");
        }
        idProperty = property;
    }

    /**
     * Special handling for setting the foreign key properties for a class,
     * this is optional but recommended
     * @param property the property from a persistent object which matches a foreign key,
     * normally this would be something like "thing.id" where thing is the property of the foreign key
     * and id is the unique identifier of the object type of the thing property
     * @param column the db table column which maps to the foreign key
     */
    public void setForeignKeyMapping(String property, String column) {
        if (property == null || column == null) {
            throw new IllegalArgumentException("property and column must not be null");
        }
        if (! columnToProperty.containsKey(column)) {
            throw new IllegalArgumentException("this column ("+column+") is not one of the mappings, " +
                    "you must specify a foreign key column which is an existing mapping, i.e. this cannot be " +
            "set until after you have added the column mapping");
        }
        foreignKeyPropertyToColumn.put(property, column);
        if (property.indexOf('.') != -1) {
            String prefix = property.substring(0, property.indexOf('.'));
            foreignKeyPropertyToColumn.put(prefix, column);         
        }
    }

    /**
     * @param property a property from a persistent object
     * @return true if this is a foreign key
     */
    public boolean isForeignKeyProperty(String property) {
        boolean fk = foreignKeyPropertyToColumn.containsKey(property);
        return fk;
    }

    /**
     * @param column the column name in the DB table storing data for this entity (e.g SOME_STRING)
     * @return the property from the persistent object which maps to this db column OR null if not found
     */
    public String getPropertyForColumn(String column) {
        String name = columnToProperty.get(column);
        if (name == null && column != null) {
            name = columnToProperty.get(column.toLowerCase());
        }
        if (name == null && column != null) {
            name = columnToProperty.get(column.toUpperCase());
        }
        // this is bad but we need to do a case-insensitive search for the name if we still failed to find it
        for (String key : columnToProperty.keySet()) {
            if (key.equalsIgnoreCase(column)) {
                name = columnToProperty.get(key);
                break;
            }
        }
        return name;
    }

    /**
     * @param property a property in a persistent object
     * @return the db column name which maps to this property OR null if not found
     */
    public String getColumnForProperty(String property) {
        String name = foreignKeyPropertyToColumn.get(property);
        if (name == null) {
            name = propertyToColumn.get(property);
        }
        return name;
    }

    /**
     * @param property a property in a persistent object
     * @return the type which maps to this property OR null if the property name is not found or there is no conversion needed
     */
    public Class<?> getTypeForProperty(String property) {
        Class<?> type = null;
        if (property != null) {
            String column = getColumnForProperty(property);
            if (column != null) {
                type = columnToType.get(column);
            }
        }
        return type;
    }

    /**
     * @param column the column name in the DB table storing data for this entity (e.g SOME_STRING)
     * @return the type which maps to this column OR null if the column name is not found or there is no conversion needed
     */
    public Class<?> getTypeForColumn(String column) {
        Class<?> type = null;
        if (column != null) {
            type = columnToType.get(column);
        }
        return type;
    }

    /**
     * Allows for special handling of the types and will convert all data going into the DB into the
     * type indicated in this map for the property, by default this returns null for all columns
     * indicating no conversion is needed, will also convert the data coming out from the
     * JDBC type to the persistent class type
     * 
     * @param property a property in a persistent object, there must be column to property
     * mapping for this or it will fail
     * @param type the type which maps to this property,
     * if null this will clear the current setting
     * @throws IllegalArgumentException if there is no mapping for this property OR the property is null
     */
    public void setTypeForProperty(String property, Class<?> type) {
        if (property == null) {
            throw new IllegalArgumentException("property must be set");
        }
        String column = getColumnForProperty(property);
        if (column == null) {
            throw new IllegalArgumentException("No column found to match this property: " + property);
        }
        setTypeForColumn(column, type);
    }

    /**
     * Allows for special handling of the types and will convert all data going into the DB into the
     * type indicated in this map for the column, by default this returns null for all columns
     * indicating no conversion is needed, will also convert the data coming out from the
     * JDBC type to the persistent class type
     * 
     * @param column the column name in the DB table storing data for this entity (e.g SOME_STRING)
     * @param type the type which maps to this property,
     * if null this will clear the current setting
     * @throws IllegalArgumentException if the column is null
     */
    public void setTypeForColumn(String column, Class<?> type) {
        if (column == null) {
            throw new IllegalArgumentException("column must be set");
        }
        if (type == null) {
            columnToType.remove(column);
        } else {
            columnToType.put(column, type);
        }
    }

    /**
     * @return a list of all property names which are mapped,
     * sorted in alpha order
     */
    public List<String> getPropertyNames() {
        List<String> names = new ArrayList<String>();
        if (propertyToColumn != null) {
            names.addAll( propertyToColumn.keySet() );
        }
        Collections.sort(names);
        return names;
    }

    /**
     * @return a list of all column names which are mapped,
     * sorted in alpha order
     */
    public List<String> getColumnNames() {
        List<String> names = new ArrayList<String>();
        if (columnToProperty != null) {
            names.addAll( columnToProperty.keySet() );
        }
        Collections.sort(names);
        return names;
    }

    /**
     * @return a list of all properties which are foreign keys,
     * sorted in alpha order
     */
    public List<String> getForeignKeyPropertyNames() {
        List<String> names = new ArrayList<String>();
        if (foreignKeyPropertyToColumn != null) {
            for (String fkProp : foreignKeyPropertyToColumn.keySet()) {
                if (fkProp.indexOf('.') == -1) {
                    names.add( fkProp );
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    /**
     * @return a list of all column names which are foreign keys,
     * sorted in alpha order
     */
    public List<String> getForeignKeyColumnNames() {
        List<String> names = new ArrayList<String>();
        if (foreignKeyPropertyToColumn != null) {
            for (String fkProp : foreignKeyPropertyToColumn.keySet()) {
                if (fkProp.indexOf('.') == -1) {
                    names.add( foreignKeyPropertyToColumn.get(fkProp) );
                }
            }
        }
        Collections.sort(names);
        return names;
    }

}
