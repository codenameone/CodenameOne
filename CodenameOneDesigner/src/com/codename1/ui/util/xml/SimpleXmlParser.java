package com.codename1.ui.util.xml;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Lightweight XML-to-bean parser used to avoid JAXB runtime dependencies.
 */
public final class SimpleXmlParser {
    private SimpleXmlParser() {
    }

    public static <T> T parse(File xml, Class<T> type) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document doc = factory.newDocumentBuilder().parse(xml);
            Element root = doc.getDocumentElement();
            return parseElement(root, type);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse XML file " + xml, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T parseElement(Element element, Class<T> type) throws Exception {
        T instance = type.getDeclaredConstructor().newInstance();
        BeanInfo beanInfo = Introspector.getBeanInfo(type, Object.class);
        Map<String, PropertyDescriptor> properties = new HashMap<String, PropertyDescriptor>();
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            if (pd.getWriteMethod() != null) {
                properties.put(pd.getName(), pd);
            }
        }

        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            PropertyDescriptor pd = properties.get(attr.getNodeName());
            if (pd == null) {
                continue;
            }
            invokeSetter(instance, pd.getWriteMethod(), convert(attr.getNodeValue(), pd.getPropertyType()));
        }

        Map<String, List<Element>> childrenByTag = new HashMap<String, List<Element>>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String tag = child.getNodeName();
            List<Element> list = childrenByTag.get(tag);
            if (list == null) {
                list = new ArrayList<Element>();
                childrenByTag.put(tag, list);
            }
            list.add((Element) child);
        }

        for (PropertyDescriptor pd : properties.values()) {
            Method setter = pd.getWriteMethod();
            Class<?> propertyType = pd.getPropertyType();
            List<Element> propertyChildren = childrenByTag.get(pd.getName());

            if (propertyType.isArray()) {
                if (propertyChildren == null) {
                    continue;
                }
                Class<?> componentType = propertyType.getComponentType();
                Object arr = Array.newInstance(componentType, propertyChildren.size());
                for (int i = 0; i < propertyChildren.size(); i++) {
                    Element child = propertyChildren.get(i);
                    Object value = isSimple(componentType)
                            ? convert(child.getTextContent(), componentType)
                            : parseElement(child, componentType);
                    Array.set(arr, i, value);
                }
                invokeSetter(instance, setter, arr);
                continue;
            }

            if (propertyChildren != null && !propertyChildren.isEmpty()) {
                Element child = propertyChildren.get(0);
                Object value = isSimple(propertyType)
                        ? convert(child.getTextContent(), propertyType)
                        : parseElement(child, propertyType);
                invokeSetter(instance, setter, value);
                continue;
            }

            if ("value".equals(pd.getName()) && isSimple(propertyType)) {
                String text = element.getTextContent();
                if (text != null) {
                    text = text.trim();
                }
                if (text != null && text.length() > 0) {
                    invokeSetter(instance, setter, convert(text, propertyType));
                }
            }
        }
        return instance;
    }

    private static void invokeSetter(Object target, Method setter, Object value) throws Exception {
        if (value == null && setter.getParameterTypes()[0].isPrimitive()) {
            return;
        }
        setter.invoke(target, value);
    }

    private static boolean isSimple(Class<?> type) {
        return type == String.class
                || type == Integer.class || type == Integer.TYPE
                || type == Long.class || type == Long.TYPE
                || type == Boolean.class || type == Boolean.TYPE
                || type == Float.class || type == Float.TYPE
                || type == Double.class || type == Double.TYPE
                || type == Short.class || type == Short.TYPE
                || type == Byte.class || type == Byte.TYPE;
    }

    private static Object convert(String value, Class<?> type) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (type == String.class) {
            return value;
        }
        if (trimmed.length() == 0) {
            return null;
        }
        if (type == Integer.class || type == Integer.TYPE) {
            return Integer.valueOf(trimmed);
        }
        if (type == Long.class || type == Long.TYPE) {
            return Long.valueOf(trimmed);
        }
        if (type == Boolean.class || type == Boolean.TYPE) {
            return Boolean.valueOf(trimmed);
        }
        if (type == Float.class || type == Float.TYPE) {
            return Float.valueOf(trimmed);
        }
        if (type == Double.class || type == Double.TYPE) {
            return Double.valueOf(trimmed);
        }
        if (type == Short.class || type == Short.TYPE) {
            return Short.valueOf(trimmed);
        }
        if (type == Byte.class || type == Byte.TYPE) {
            return Byte.valueOf(trimmed);
        }
        return value;
    }
}
