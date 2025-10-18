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

import com.codename1.io.Util;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextComponent;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.table.AbstractTableModel;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.validation.Constraint;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The binding framework can implicitly bind UI elements to properties, this allow seamless model to
 * UI mapping. Most cases allow simple binding by just using the {@link
 * #bind(com.codename1.properties.Property, com.codename1.ui.Component)} method to seamlessly update
 * a property/component based on changes.
 *
 * <p>It contains the following base concepts:
 *
 * <p>1. {@link com.codename1.properties.UiBinding.ObjectConverter} - a converter converts from one
 * type to another. E.g. if we want a {@link com.codename1.ui.TextArea} to map to an {@code Integer}
 * property we'd use an {@link com.codename1.properties.UiBinding.IntegerConverter} to indicate the
 * desired destination value.
 *
 * <p>2. {@link com.codename1.properties.UiBinding.ComponentAdapter} - takes two {@link
 * com.codename1.properties.UiBinding.ObjectConverter} to convert to/from the component and
 * property. It provides the API for event binding and value extraction/setting on the component.
 *
 * <p>3. {@link com.codename1.properties.UiBinding.Binding} - the commit mode.
 *
 * <p>4. {@link #bind(com.codename1.properties.Property, com.codename1.ui.Component)} - the {@code
 * bind} helper methods allow us to bind a component easily without exposure to these complexities.
 *
 * @author Shai Almog
 */
public class UiBinding {
    private boolean autoCommit = true;

    /**
     * Allows us to unbind the property from binding, this is equivalent to calling {@link
     * com.codename1.properties.UiBinding.Binding#disconnect()} on all bindings...
     *
     * @param prop the property
     */
    public static void unbind(PropertyBase prop) {
        if (prop.getListeners() != null) {
            for (Object l : prop.getListeners()) {
                if (l instanceof Binding) {
                    ((Binding) l).disconnect();

                    // prevent a concurrent modification exception by returning and recursing
                    unbind(prop);
                    return;
                }
            }
        }
    }

    /**
     * Unbinds all the properties within the business object
     *
     * @param po the business object
     */
    public static void unbind(PropertyBusinessObject po) {
        for (PropertyBase pb : po.getPropertyIndex()) {
            unbind(pb);
        }
    }

    private static ObjectConverter pickerTypeToConverter(int type) {
        switch (type) {
            case Display.PICKER_TYPE_DATE:
            case Display.PICKER_TYPE_DATE_AND_TIME:
                return new DateConverter();
            case Display.PICKER_TYPE_TIME:
                return new IntegerConverter();
            case Display.PICKER_TYPE_STRINGS:
                return new StringConverter();
        }
        throw new IllegalArgumentException("Unsupported picker type: " + type);
    }

    /**
     * Is auto-commit mode on by default see {@link #setAutoCommit(boolean)}
     *
     * @return true if auto-commit is on
     */
    public boolean isAutoCommit() {
        return autoCommit;
    }

    /**
     * Default value for auto-commit mode, in auto-commit mode changes to the component/property are
     * instantly reflected otherwise {@link com.codename1.properties.UiBinding.CommitMode#commit()}
     * should be invoked explicitly
     *
     * @param b true to enable auto commit mode
     */
    public void setAutoCommit(boolean b) {
        autoCommit = b;
    }

    private ObjectConverter getPropertyConverter(PropertyBase prop) {
        Class gt = prop.getGenericType();
        if (gt == null || gt == String.class) {
            return new StringConverter();
        }
        if (gt == Integer.class) {
            return new IntegerConverter();
        }
        if (gt == Long.class) {
            return new LongConverter();
        }
        if (gt == Double.class) {
            return new DoubleConverter();
        }
        if (gt == Float.class) {
            return new FloatConverter();
        }
        if (gt == Boolean.class) {
            return new BooleanConverter();
        }
        if (gt == Date.class) {
            return new DateConverter();
        }
        throw new RuntimeException("Unsupported property converter: " + gt.getName());
    }

    GroupBinding createGroupBinding(List<Binding> allBindings) {
        return new GroupBinding(allBindings);
    }

    /**
     * Binds a hierarchy of Components to a business object by searching the tree and collecting the
     * bindings. Components are associated with properties based on their name attribute
     *
     * @param obj the business object with the properties to bind
     * @param cnt a container that will be recursed for binding
     * @return a Binding object that manipulates all of the individual bindings at once
     */
    public Binding bind(final PropertyBusinessObject obj, final Container cnt) {
        ArrayList<Binding> allBindings = new ArrayList<Binding>();
        bind(obj, cnt, allBindings);

        return new GroupBinding(allBindings);
    }

    private void bind(
            final PropertyBusinessObject obj, final Container cnt, ArrayList<Binding> allBindings) {
        for (Component cmp : cnt) {
            if (cmp instanceof Container && ((Container) cmp).getLeadComponent() == null) {
                bind(obj, ((Container) cmp), allBindings);
                continue;
            }
            String n = cmp.getName();
            if (n != null) {
                PropertyBase b = obj.getPropertyIndex().get(n);
                if (b != null) {
                    allBindings.add(bind(b, cmp));
                }
            }
        }
    }

    /**
     * Binds the given property to the selected value from the set based on the multiple components.
     * This is useful for binding multiple radio buttons to a single property value based on selection
     *
     * @param prop   the property
     * @param values the values that can be used
     * @param cmps   the components
     * @return a binding object that allows us to toggle auto commit mode, commit/rollback and unbind
     */
    public Binding bindGroup(
            final PropertyBase prop, final Object[] values, final Component... cmps) {
        ObjectConverter cnv = getPropertyConverter(prop);
        if (cmps[0] instanceof RadioButton) {
            RadioButton[] rb = new RadioButton[cmps.length];
            System.arraycopy(cmps, 0, rb, 0, cmps.length);
            return bindImpl(prop, rb, new RadioListAdapter(cnv, values));
        }
        throw new RuntimeException("Unsupported binding type: " + cmps[0].getClass().getName());
    }

    /**
     * Binds the given property to the component using default adapters
     *
     * @param prop the property
     * @param cmp  the component
     * @return a binding object that allows us to toggle auto commit mode, commit/rollback and unbind
     */
    public Binding bind(final PropertyBase prop, final Component cmp) {
        ObjectConverter cnv = getPropertyConverter(prop);
        if (cmp instanceof TextArea) {
            return bind(prop, cmp, new TextAreaAdapter(cnv));
        }
        if (cmp instanceof TextComponent) {
            return bind(prop, cmp, new TextComponentAdapter(cnv));
        }
        if (cmp instanceof CheckBox) {
            return bind(prop, cmp, new CheckBoxRadioSelectionAdapter(cnv));
        }
        if (cmp instanceof RadioButton) {
            return bind(prop, cmp, new CheckBoxRadioSelectionAdapter(cnv));
        }
        if (cmp instanceof Picker) {
            return bind(prop, cmp, new PickerAdapter(cnv, ((Picker) cmp).getType()));
        }
        throw new RuntimeException("Unsupported binding type: " + cmp.getClass().getName());
    }

    /**
     * Binds the given property to the component using a custom adapter class
     *
     * @param prop  the property
     * @param cmp   the component
     * @param adapt an implementation of {@link com.codename1.properties.UiBinding.ComponentAdapter}
     *              that allows us to define the way the component maps to/from the property
     * @return a binding object that allows us to toggle auto commit mode, commit/rollback and unbind
     */
    public Binding bind(final PropertyBase prop, final Component cmp, final ComponentAdapter adapt) {
        return bindImpl(prop, cmp, adapt);
    }

    /**
     * Creates a table model which is implicitly bound to the properties
     *
     * @param objects   list of business objects
     * @param prototype the type by which we determine the structure of the table
     * @return a bound table model that can be used in the {@code Table} class
     */
    public BoundTableModel createTableModel(
            List<? extends PropertyBusinessObject> objects, PropertyBusinessObject prototype) {
        return new BoundTableModel(objects, prototype);
    }

    /**
     * Creates a table model which is implicitly bound to the properties
     *
     * @param objects   list of business objects
     * @param prototype the type by which we determine the structure of the table
     * @return a bound table model that can be used in the {@code Table} class
     */
    public BoundTableModel createTableModel(
            CollectionProperty<? extends PropertyBusinessObject, ? extends Object> objects,
            PropertyBusinessObject prototype) {
        return new BoundTableModel(objects, prototype);
    }

    private Binding bindImpl(
            final PropertyBase prop, final Object cmp, final ComponentAdapter adapt) {
        adapt.assignTo(prop.get(), cmp);

        class BindingImpl extends Binding
                implements PropertyChangeListener, ActionListener<ActionEvent> {
            private boolean lock;

            public void actionPerformed(ActionEvent evt) {
                if (isAutoCommit()) {
                    if (lock) {
                        return;
                    }
                    lock = true;
                    ((Property) prop).set(adapt.getFrom(cmp));
                    lock = false;
                }
            }

            public void propertyChanged(PropertyBase p) {
                if (isAutoCommit()) {
                    if (lock) {
                        return;
                    }
                    lock = true;
                    adapt.assignTo(prop.get(), cmp);
                    lock = false;
                }
            }

            @Override
            public void commit() {
                if (isAutoCommit()) {
                    throw new RuntimeException("Can't commit in autocommit mode");
                }
                ((Property) prop).set(adapt.getFrom(cmp));
            }

            @Override
            public void rollback() {
                if (isAutoCommit()) {
                    throw new RuntimeException("Can't rollback in autocommit mode");
                }
                adapt.assignTo(prop.get(), cmp);
            }

            @Override
            public void disconnect() {
                adapt.removeListener(cmp, this);
                prop.removeChangeListener(this);
            }
        }
        BindingImpl b = new BindingImpl();
        adapt.bindListener(cmp, b);
        prop.addChangeListener(b);
        return b;
    }

    /**
     * Changes to the text area are automatically reflected to the given property and visa versa
     *
     * @param prop the property value
     * @param ta   the text area
     * @deprecated this code was experimental we will use the more generic Adapter/bind framework
     */
    public void bindString(Property<String, ? extends Object> prop, TextArea ta) {
        bind(prop, ta);
    }

    /**
     * Changes to the text area are automatically reflected to the given property and visa versa
     *
     * @param prop the property value
     * @param ta   the text area
     * @deprecated this code was experimental we will use the more generic Adapter/bind framework
     */
    public void bindInteger(Property<Integer, ? extends Object> prop, TextArea ta) {
        bind(prop, ta);
    }

    /**
     * Object converter can convert an object from one type to another e.g. a String to an integer an
     * array to a list model. Use this object converter to keep source/values the same e.g. when
     * converting using a {@link com.codename1.properties.UiBinding.TextAreaAdapter} to a String
     * property.
     */
    public static class ObjectConverter {
        /**
         * Converts an object of source type to the type matching this class, the default implementation
         * does nothing and can be used as a stand-in
         *
         * @param source an object or null
         * @return null or a new object instance
         */
        public Object convert(Object source) {
            return source;
        }
    }

    /**
     * Converts the source value to a String.
     */
    public static class StringConverter extends ObjectConverter {
        @Override
        public Object convert(Object source) {
            if (source == null) {
                return null;
            }
            return source.toString();
        }
    }

    /**
     * Converts the source value to an Integer.
     */
    public static class IntegerConverter extends ObjectConverter {
        @Override
        public Object convert(Object source) {
            if (source == null) {
                return null;
            }
            return Util.toIntValue(source);
        }
    }

    /**
     * Converts the source value to a Date.
     */
    public static class DateConverter extends ObjectConverter {
        @Override
        public Object convert(Object source) {
            if (source == null) {
                return null;
            }
            if (source instanceof Date) {
                return source;
            }
            return new Date(Util.toLongValue(source));
        }
    }

    /**
     * Converts the source value to a Long.
     */
    public static class LongConverter extends ObjectConverter {
        @Override
        public Object convert(Object source) {
            if (source == null) {
                return null;
            }
            return Util.toLongValue(source);
        }
    }

    /**
     * Converts the source value to a Float.
     */
    public static class FloatConverter extends ObjectConverter {
        @Override
        public Object convert(Object source) {
            if (source == null) {
                return null;
            }
            return Util.toFloatValue(source);
        }
    }

    /**
     * Converts the source value to a Double.
     */
    public static class DoubleConverter extends ObjectConverter {
        @Override
        public Object convert(Object source) {
            if (source == null) {
                return null;
            }
            return Util.toDoubleValue(source);
        }
    }

    /**
     * Converts the source value to a Boolean.
     */
    public static class BooleanConverter extends ObjectConverter {
        @Override
        public Object convert(Object source) {
            if (source == null) {
                return null;
            }
            if (source instanceof Boolean) {
                return source;
            }
            if (source instanceof String) {
                String s = ((String) source).toLowerCase();
                return s.indexOf("true") > 0 || s.indexOf("yes") > 0 || s.indexOf("1") > 0;
            }
            return Util.toIntValue(source) > 0;
        }
    }

    /**
     * Maps values to other values for conversion in a similar way to a Map this is pretty useful for
     * API's like picker where we have a list of Strings and we might want a list of other objects to
     * match every string
     */
    public static class MappingConverter extends ObjectConverter {
        private final Map<Object, Object> m;

        public MappingConverter(Map<Object, Object> m) {
            this.m = m;
        }

        @Override
        public Object convert(Object source) {
            if (source == null) {
                return null;
            }
            return m.get(source);
        }
    }

    /**
     * Adapters can be extended to allow any component to bind to a property via a converter
     */
    public abstract static class ComponentAdapter<PropertyType, ComponentType> {
        /**
         * Used by the subclass to convert values from the component to the property
         */
        protected final ObjectConverter toPropertyType;

        /**
         * Used by the subclass to convert values from the property to the component
         */
        protected final ObjectConverter toComponentType;

        /**
         * Subclasses usually provide the toComponentType and allow their callers to define the
         * toPropertyType
         *
         * @param toPropertyType  Used by the subclass to convert values from the component to the
         *                        property
         * @param toComponentType Used by the subclass to convert values from the property to the
         *                        component
         */
        public ComponentAdapter(ObjectConverter toPropertyType, ObjectConverter toComponentType) {
            this.toPropertyType = toPropertyType;
            this.toComponentType = toComponentType;
        }

        /**
         * Assigns the value from the property into the component
         *
         * @param value the value that was returned from the property get method
         * @param cmp   the component instance
         */
        public abstract void assignTo(PropertyType value, ComponentType cmp);

        /**
         * Returns the value for the set method of the property from the given component
         *
         * @param cmp the component
         * @return the value we can place into the set method
         */
        public abstract PropertyType getFrom(ComponentType cmp);

        /**
         * Binds an action listener to changes in the component
         *
         * @param cmp the component
         * @param l   listener
         */
        public abstract void bindListener(ComponentType cmp, ActionListener<ActionEvent> l);

        /**
         * Removes the action listener from changes in the component
         *
         * @param cmp the component
         * @param l   listener
         */
        public abstract void removeListener(ComponentType cmp, ActionListener<ActionEvent> l);
    }

    /**
     * Adapts a {@link com.codename1.ui.TextArea} (and it's subclass {@link
     * com.codename1.ui.TextField} to binding
     *
     * @param <PropertyType> the type of the property generic
     */
    public static class TextAreaAdapter<PropertyType>
            extends ComponentAdapter<PropertyType, TextArea> {
        /**
         * Constructs a new binding
         *
         * @param toPropertyType the conversion logic to the property
         */
        public TextAreaAdapter(ObjectConverter toPropertyType) {
            super(toPropertyType, new StringConverter());
        }

        /**
         * Constructs a new binding assuming a String property
         */
        public TextAreaAdapter() {
            super(new ObjectConverter(), new ObjectConverter());
        }

        @Override
        public void assignTo(PropertyType value, TextArea cmp) {
            cmp.setText((String) toComponentType.convert(value));
        }

        @Override
        public PropertyType getFrom(TextArea cmp) {
            return (PropertyType) toPropertyType.convert(cmp.getText());
        }

        @Override
        public void bindListener(TextArea cmp, ActionListener<ActionEvent> l) {
            cmp.addActionListener(l);
        }

        @Override
        public void removeListener(TextArea cmp, ActionListener<ActionEvent> l) {
            cmp.removeActionListener(l);
        }
    }

    /**
     * Adapts a {@link com.codename1.ui.TextComponent} to binding
     *
     * @param <PropertyType> the type of the property generic
     */
    public static class TextComponentAdapter<PropertyType>
            extends ComponentAdapter<PropertyType, TextComponent> {
        /**
         * Constructs a new binding
         *
         * @param toPropertyType the conversion logic to the property
         */
        public TextComponentAdapter(ObjectConverter toPropertyType) {
            super(toPropertyType, new StringConverter());
        }

        /**
         * Constructs a new binding assuming a String property
         */
        public TextComponentAdapter() {
            super(new ObjectConverter(), new ObjectConverter());
        }

        @Override
        public void assignTo(PropertyType value, TextComponent cmp) {
            cmp.getField().setText((String) toComponentType.convert(value));
        }

        @Override
        public PropertyType getFrom(TextComponent cmp) {
            return (PropertyType) toPropertyType.convert(cmp.getText());
        }

        @Override
        public void bindListener(TextComponent cmp, ActionListener<ActionEvent> l) {
            cmp.getField().addActionListener(l);
        }

        @Override
        public void removeListener(TextComponent cmp, ActionListener<ActionEvent> l) {
            cmp.getField().removeActionListener(l);
        }
    }

    /**
     * Adapts a {@link com.codename1.ui.CheckBox} or {@link com.codename1.ui.RadioButton} to binding
     *
     * @param <PropertyType> the type of the property generic
     */
    public static class CheckBoxRadioSelectionAdapter<PropertyType>
            extends ComponentAdapter<PropertyType, Button> {
        /**
         * Constructs a new binding
         *
         * @param toPropertyType the conversion logic to the property
         */
        public CheckBoxRadioSelectionAdapter(ObjectConverter toPropertyType) {
            super(toPropertyType, new BooleanConverter());
        }

        @Override
        public void assignTo(PropertyType value, Button cmp) {
            if (cmp instanceof CheckBox) {
                ((CheckBox) cmp).setSelected((Boolean) toComponentType.convert(value));
            } else {
                ((RadioButton) cmp).setSelected((Boolean) toComponentType.convert(value));
            }
        }

        @Override
        public PropertyType getFrom(Button cmp) {
            return (PropertyType) toPropertyType.convert(cmp.isSelected());
        }

        @Override
        public void bindListener(Button cmp, ActionListener<ActionEvent> l) {
            cmp.addActionListener(l);
        }

        @Override
        public void removeListener(Button cmp, ActionListener<ActionEvent> l) {
            cmp.removeActionListener(l);
        }
    }

    /**
     * Adapts a set of {@link com.codename1.ui.RadioButton} to a selection within a list of values
     *
     * @param <PropertyType> the type of the property generic
     */
    public static class RadioListAdapter<PropertyType>
            extends ComponentAdapter<PropertyType, RadioButton[]> {
        private final PropertyType[] values;

        /**
         * Constructs a new binding
         *
         * @param toPropertyType the conversion logic to the property
         * @param values         potential values for the selection
         */
        public RadioListAdapter(ObjectConverter toPropertyType, PropertyType... values) {
            super(toPropertyType, null);
            this.values = values;
        }

        @Override
        public void assignTo(PropertyType value, RadioButton[] cmp) {
            for (int iter = 0; iter < values.length; iter++) {
                if (values[iter].equals(value)) {
                    cmp[iter].setSelected(true);
                    return;
                }
            }
        }

        @Override
        public PropertyType getFrom(RadioButton[] cmp) {
            for (int iter = 0; iter < values.length; iter++) {
                if (cmp[iter].isSelected()) {
                    return values[iter];
                }
            }
            return null;
        }

        @Override
        public void bindListener(RadioButton[] cmp, ActionListener<ActionEvent> l) {
            for (RadioButton r : cmp) {
                r.addActionListener(l);
            }
        }

        @Override
        public void removeListener(RadioButton[] cmp, ActionListener<ActionEvent> l) {
            for (RadioButton r : cmp) {
                r.removeActionListener(l);
            }
        }
    }

    /**
     * Adapts a {@link com.codename1.ui.spinner.Picker} to binding
     *
     * @param <PropertyType> the type of the property generic
     */
    public static class PickerAdapter<PropertyType> extends ComponentAdapter<PropertyType, Picker> {
        /**
         * Constructs a new binding
         *
         * @param toPropertyType the conversion logic to the property
         * @param pickerType     the type of the picker
         */
        public PickerAdapter(ObjectConverter toPropertyType, int pickerType) {
            super(toPropertyType, pickerTypeToConverter(pickerType));
        }

        /**
         * Constructs a new binding for mapping back and forth of a String Picker
         *
         * @param toPropertyType  map to convert objects forth
         * @param toComponentType map to convert objects back
         */
        public PickerAdapter(MappingConverter toPropertyType, MappingConverter toComponentType) {
            super(toPropertyType, toComponentType);
        }

        @Override
        public void assignTo(PropertyType value, Picker cmp) {
            switch (cmp.getType()) {
                case Display.PICKER_TYPE_DATE:
                case Display.PICKER_TYPE_DATE_AND_TIME:
                    cmp.setDate((Date) toComponentType.convert(value));
                    break;
                case Display.PICKER_TYPE_TIME:
                    cmp.setTime((Integer) toComponentType.convert(value));
                    break;
                case Display.PICKER_TYPE_STRINGS:
                    if (value instanceof Integer) {
                        cmp.setSelectedStringIndex((Integer) toComponentType.convert(value));
                    } else {
                        cmp.setSelectedString((String) toComponentType.convert(value));
                    }
                    break;
            }
        }

        @Override
        public PropertyType getFrom(Picker cmp) {
            switch (cmp.getType()) {
                case Display.PICKER_TYPE_DATE:
                case Display.PICKER_TYPE_DATE_AND_TIME:
                    return (PropertyType) toPropertyType.convert(cmp.getDate());
                case Display.PICKER_TYPE_TIME:
                    return (PropertyType) toPropertyType.convert(cmp.getTime());
                case Display.PICKER_TYPE_STRINGS:
                    if (toPropertyType instanceof IntegerConverter) {
                        return (PropertyType) Integer.valueOf(cmp.getSelectedStringIndex());
                    }
                    return (PropertyType) toPropertyType.convert(cmp.getSelectedString());
            }
            throw new RuntimeException("Illegal state for picker binding");
        }

        @Override
        public void bindListener(Picker cmp, ActionListener<ActionEvent> l) {
            cmp.addActionListener(l);
        }

        @Override
        public void removeListener(Picker cmp, ActionListener<ActionEvent> l) {
            cmp.removeActionListener(l);
        }
    }

    /**
     * Implements table model binding, this is implemented as a class to allow additional features
     * such as adding/removing rows
     */
    public static class BoundTableModel extends AbstractTableModel {
        private List<? extends PropertyBusinessObject> objects;
        private CollectionProperty objectProperty;
        private final PropertyBusinessObject prototype;
        private final Set<String> exclude = new HashSet<String>();
        private List<String> included;
        private PropertyBase[] columnOrder;
        private final Set<String> uneditable = new HashSet<String>();
        private final EventDispatcher listeners = new EventDispatcher();

        /**
         * Creates a table model with the business objects
         *
         * @param objects   the objects of the model
         * @param prototype the type by which we determine the structure of the table
         */
        public BoundTableModel(
                List<? extends PropertyBusinessObject> objects, PropertyBusinessObject prototype) {
            this.objects = objects;
            this.prototype = prototype;
        }

        /**
         * Creates a table model with the business objects
         *
         * @param objectProperty the objects of the model
         * @param prototype      the type by which we determine the structure of the table
         */
        public BoundTableModel(CollectionProperty objectProperty, PropertyBusinessObject prototype) {
            this.objectProperty = objectProperty;
            this.prototype = prototype;
        }

        /**
         * The properties that are ignored
         *
         * @param b the property to ignore
         */
        public void excludeProperty(PropertyBase b) {
            exclude.add(b.getName());
            if (included == null) {
                included = new ArrayList<String>();
                for (PropertyBase pb : prototype.getPropertyIndex()) {
                    included.add(pb.getName());
                }
            }
            included.remove(b.getName());
        }

        /**
         * Sets the order of the columns explicitly
         *
         * @param columnOrder the order of the columns based on the prototype
         */
        public void setColumnOrder(PropertyBase... columnOrder) {
            this.columnOrder = columnOrder;
        }

        /**
         * Makes the property editable or uneditable
         *
         * @param pb       the property base
         * @param editable true for editable (the default)
         */
        public void setEditable(PropertyBase pb, boolean editable) {
            if (editable) {
                uneditable.remove(pb.getName());
            } else {
                uneditable.add(pb.getName());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            if (objects != null) {
                return objects.size();
            }
            return objectProperty.size();
        }

        /**
         * Adds a new business object to the table
         *
         * @param index the index of the addition
         * @param b     the business object
         */
        public void addRow(int index, PropertyBusinessObject b) {
            if (objects != null) {
                ((List) objects).add(index, b);
            } else {
                if (objectProperty instanceof ListProperty) {
                    ((ListProperty) objectProperty).add(index, b);
                } else {
                    objectProperty.add(b);
                }
            }
            for (int col = 0; col < getColumnCount(); col++) {
                listeners.fireDataChangeEvent(col, index);
            }
        }

        /**
         * Removes the row at the given index
         *
         * @param index the position in the table
         */
        public void removeRow(int index) {
            if (objects != null) {
                objects.remove(index);
            } else {
                if (objectProperty instanceof ListProperty) {
                    objectProperty.remove(index);
                } else {
                    Iterator i = objectProperty.iterator();
                    for (int iter = 0; iter < index - 1; iter++) {
                        i.next();
                    }
                    i.remove();
                }
            }
            listeners.fireDataChangeEvent(Integer.MIN_VALUE, Integer.MIN_VALUE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getColumnCount() {
            if (columnOrder != null) {
                return columnOrder.length;
            }
            return prototype.getPropertyIndex().getSize() - exclude.size();
        }

        private PropertyBase getProperty(int index) {
            if (included != null) {
                return prototype.getPropertyIndex().get(included.get(index));
            }
            return prototype.getPropertyIndex().get(index);
        }

        @Override
        public String getColumnName(int i) {
            if (columnOrder != null) {
                return columnOrder[i].getLabel();
            }
            return getProperty(i).getLabel();
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            if (columnOrder != null && columnOrder.length > 0) {
                return !uneditable.contains(columnOrder[column].getName());
            }
            return !uneditable.contains(getProperty(column).getName());
        }

        private PropertyBusinessObject getRow(int row) {
            if (objects != null) {
                return objects.get(row);
            } else {
                if (objectProperty instanceof ListProperty) {
                    return (PropertyBusinessObject) ((ListProperty) objectProperty).get(row);
                } else {
                    Iterator i = objectProperty.iterator();
                    for (int iter = 0; iter < row - 1; iter++) {
                        i.next();
                    }
                    return (PropertyBusinessObject) i.next();
                }
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            PropertyBusinessObject pb = getRow(row);
            String n;
            if (columnOrder != null) {
                n = columnOrder[column].getName();
            } else {
                n = getProperty(column).getName();
            }
            return pb.getPropertyIndex().get(n).get();
        }

        @Override
        public Class getCellType(int row, int column) {
            PropertyBusinessObject pb = getRow(row);
            String n;
            if (columnOrder != null) {
                n = columnOrder[column].getName();
            } else {
                n = getProperty(column).getName();
            }
            return pb.getPropertyIndex().get(n).getGenericType();
        }

        private PropertyBase getPropertyAt(int row, int column) {
            PropertyBusinessObject pb = getRow(row);
            String n;
            if (columnOrder != null) {
                n = columnOrder[column].getName();
            } else {
                n = getProperty(column).getName();
            }
            return pb.getPropertyIndex().get(n);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[] getMultipleChoiceOptions(int row, int column) {
            return (String[]) getPropertyAt(row, column).getClientProperty("multipleChoiceOptions");
        }

        /**
         * Sets the multiple choice option for a string entry, this is useful if a string property can
         * have one of several values. It will be rendered as a picker in the table
         *
         * @param prop   the property
         * @param values the supported values
         */
        public void setMultipleChoiceOptions(PropertyBase prop, String... values) {
            prop.putClientProperty("multipleChoiceOptions", values);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Constraint getValidationConstraint(int row, int column) {
            return (Constraint) getPropertyAt(row, column).getClientProperty("validationConstraint");
        }

        /**
         * Sets a validator constraint on the table
         *
         * @param prop the property matching the constraint
         * @param c    the constraint value
         */
        public void setValidationConstraint(PropertyBase prop, Constraint c) {
            prop.putClientProperty("validationConstraint", c);
        }

        @Override
        public void setValueAt(int row, int column, Object o) {
            PropertyBusinessObject pb = getRow(row);
            String n;
            if (columnOrder != null) {
                n = columnOrder[column].getName();
            } else {
                n = getProperty(column).getName();
            }
            pb.getPropertyIndex().get(n).setImpl(o);
            listeners.fireDataChangeEvent(column, row);
        }

        @Override
        public void addDataChangeListener(DataChangedListener d) {
            listeners.addListener(d);
        }

        @Override
        public void removeDataChangeListener(DataChangedListener d) {
            listeners.removeListener(d);
        }
    }

    /**
     * Binding allows us to have commit/auto-commit mode. This allows changes to properties to reflect
     * immediately or only when committed, e.g. if a {@code Form} has "OK" &amp; "Cancel" buttons you
     * might want to do a commit on OK. We also provide a "rollback" method to reset to the original
     * property values.
     *
     * <p>{@code UiBinding} has a boolean auto commit flag that can be toggled to set the default for
     * new bindings.
     *
     * <p>Binding also provides the ability to disengage a "binding" between a property and a UI
     * component
     */
    public abstract class Binding {
        private boolean autoCommit = UiBinding.this.autoCommit;

        /**
         * Gets the autocommit value see {@link #setAutoCommit(boolean)}
         *
         * @return true if autocommit is on
         */
        public boolean isAutoCommit() {
            return autoCommit;
        }

        /**
         * Toggles auto-commit mode and overrides the {@code UiBinding} autocommit default. Autocommit
         * instantly reflects changes to the property or component values.
         *
         * @param b true to enable auto-commit
         */
        public void setAutoCommit(boolean b) {
            autoCommit = b;
        }

        /**
         * Set the value from the component into the property, note that this will throw an exception if
         * autocommit is on
         */
        public abstract void commit();

        /**
         * Sets the value from the property into the component, note that this will throw an exception
         * if autocommit is on
         */
        public abstract void rollback();

        /**
         * Clears the listeners and disengages binding, this can be important for GC as binding can keep
         * object references in RAM
         */
        public abstract void disconnect();
    }

    class GroupBinding extends Binding {
        private final List<Binding> allBindings;

        public GroupBinding(List<Binding> allBindings) {
            this.allBindings = allBindings;
        }

        @Override
        public void setAutoCommit(boolean b) {
            super.setAutoCommit(b);
            for (Binding bb : allBindings) {
                bb.setAutoCommit(b);
            }
        }

        @Override
        public void commit() {
            for (Binding bb : allBindings) {
                bb.commit();
            }
        }

        @Override
        public void rollback() {
            for (Binding bb : allBindings) {
                bb.rollback();
            }
        }

        @Override
        public void disconnect() {
            for (Binding bb : allBindings) {
                bb.disconnect();
            }
        }
    }
}
