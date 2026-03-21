package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_contacts {
    private GeneratedAccess_com_codename1_contacts() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codename1.contacts.Address".equals(name)) return com.codename1.contacts.Address.class;
        if ("com.codename1.contacts.Contact".equals(name)) return com.codename1.contacts.Contact.class;
        if ("com.codename1.contacts.ContactsManager".equals(name)) return com.codename1.contacts.ContactsManager.class;
        if ("com.codename1.contacts.ContactsModel".equals(name)) return com.codename1.contacts.ContactsModel.class;
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.contacts.ContactsModel.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) safeArgs[i];
                }
                return new com.codename1.contacts.ContactsModel(varArgs);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.contacts.ContactsManager.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("createContact".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.contacts.ContactsManager.createContact((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5]);
            }
        }
        if ("deleteContact".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.contacts.ContactsManager.deleteContact((java.lang.String) safeArgs[0]);
            }
        }
        if ("getAllContacts".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.contacts.ContactsManager.getAllContacts();
            }
        }
        if ("getAllContactsWithNumbers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.contacts.ContactsManager.getAllContactsWithNumbers();
            }
        }
        if ("getContactById".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return com.codename1.contacts.ContactsManager.getContactById((java.lang.String) safeArgs[0]);
            }
        }
        if ("getContactById".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                return com.codename1.contacts.ContactsManager.getContactById((java.lang.String) safeArgs[0], ((Boolean) safeArgs[1]).booleanValue(), ((Boolean) safeArgs[2]).booleanValue(), ((Boolean) safeArgs[3]).booleanValue(), ((Boolean) safeArgs[4]).booleanValue(), ((Boolean) safeArgs[5]).booleanValue());
            }
        }
        if ("getContacts".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                return com.codename1.contacts.ContactsManager.getContacts(((Boolean) safeArgs[0]).booleanValue(), ((Boolean) safeArgs[1]).booleanValue(), ((Boolean) safeArgs[2]).booleanValue(), ((Boolean) safeArgs[3]).booleanValue(), ((Boolean) safeArgs[4]).booleanValue(), ((Boolean) safeArgs[5]).booleanValue());
            }
        }
        if ("isAllContactsFast".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.contacts.ContactsManager.isAllContactsFast();
            }
        }
        if ("refresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                com.codename1.contacts.ContactsManager.refresh(); return null;
            }
        }
        throw unsupportedStatic(com.codename1.contacts.ContactsManager.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.contacts.Address) {
            try {
                return invoke0((com.codename1.contacts.Address) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.contacts.Contact) {
            try {
                return invoke1((com.codename1.contacts.Contact) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.contacts.ContactsManager) {
            try {
                return invoke2((com.codename1.contacts.ContactsManager) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.contacts.ContactsModel) {
            try {
                return invoke3((com.codename1.contacts.ContactsModel) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.contacts.Address typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCountry".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCountry();
            }
        }
        if ("getLocality".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLocality();
            }
        }
        if ("getPostalCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPostalCode();
            }
        }
        if ("getRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getRegion();
            }
        }
        if ("getStreetAddress".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStreetAddress();
            }
        }
        if ("setCountry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setCountry((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setLocality".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setLocality((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setPostalCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setPostalCode((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setRegion((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setStreetAddress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setStreetAddress((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.contacts.Contact typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAddresses".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getAddresses();
            }
        }
        if ("getBirthday".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBirthday();
            }
        }
        if ("getDisplayName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisplayName();
            }
        }
        if ("getEmails".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getEmails();
            }
        }
        if ("getFamilyName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFamilyName();
            }
        }
        if ("getFirstName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getFirstName();
            }
        }
        if ("getId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getId();
            }
        }
        if ("getLinkedContactIds".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLinkedContactIds();
            }
        }
        if ("getNote".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNote();
            }
        }
        if ("getPhoneNumbers".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPhoneNumbers();
            }
        }
        if ("getPhoto".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPhoto();
            }
        }
        if ("getPrimaryEmail".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPrimaryEmail();
            }
        }
        if ("getPrimaryPhoneNumber".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPrimaryPhoneNumber();
            }
        }
        if ("getUrls".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getUrls();
            }
        }
        if ("setAddresses".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                typedTarget.setAddresses((java.util.Hashtable) safeArgs[0]); return null;
            }
        }
        if ("setBirthday".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.setBirthday(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        if ("setDisplayName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDisplayName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setEmails".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                typedTarget.setEmails((java.util.Hashtable) safeArgs[0]); return null;
            }
        }
        if ("setFamilyName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setFamilyName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setFirstName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setFirstName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setId((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setNote".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setNote((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setPhoneNumbers".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                typedTarget.setPhoneNumbers((java.util.Hashtable) safeArgs[0]); return null;
            }
        }
        if ("setPhoto".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setPhoto((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setPrimaryEmail".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setPrimaryEmail((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setPrimaryPhoneNumber".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setPrimaryPhoneNumber((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setUrls".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                typedTarget.setUrls((java.lang.String[]) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.contacts.ContactsManager typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAllContacts".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                return typedTarget.getAllContacts(((Boolean) safeArgs[0]).booleanValue(), ((Boolean) safeArgs[1]).booleanValue(), ((Boolean) safeArgs[2]).booleanValue(), ((Boolean) safeArgs[3]).booleanValue(), ((Boolean) safeArgs[4]).booleanValue(), ((Boolean) safeArgs[5]).booleanValue());
            }
        }
        if ("isGetAllContactsFast".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isGetAllContactsFast();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.contacts.ContactsModel typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addDataChangedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.DataChangedListener.class}, false)) {
                typedTarget.addDataChangedListener((com.codename1.ui.events.DataChangedListener) safeArgs[0]); return null;
            }
        }
        if ("addItem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                typedTarget.addItem((java.lang.Object) safeArgs[0]); return null;
            }
        }
        if ("addSelectedIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, true)) {
                int[] varArgs = new int[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = ((Number) safeArgs[i]).intValue();
                }
                typedTarget.addSelectedIndices(varArgs); return null;
            }
        }
        if ("addSelectionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.SelectionListener.class}, false)) {
                typedTarget.addSelectionListener((com.codename1.ui.events.SelectionListener) safeArgs[0]); return null;
            }
        }
        if ("getItemAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.getItemAt(((Number) safeArgs[0]).intValue());
            }
        }
        if ("getList".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getList();
            }
        }
        if ("getSelectedIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSelectedIndex();
            }
        }
        if ("getSelectedIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSelectedIndices();
            }
        }
        if ("getSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSize();
            }
        }
        if ("isMultiSelectionMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isMultiSelectionMode();
            }
        }
        if ("removeAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeDataChangedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.DataChangedListener.class}, false)) {
                typedTarget.removeDataChangedListener((com.codename1.ui.events.DataChangedListener) safeArgs[0]); return null;
            }
        }
        if ("removeItem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.removeItem(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("removeSelectedIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, true)) {
                int[] varArgs = new int[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = ((Number) safeArgs[i]).intValue();
                }
                typedTarget.removeSelectedIndices(varArgs); return null;
            }
        }
        if ("removeSelectionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.SelectionListener.class}, false)) {
                typedTarget.removeSelectionListener((com.codename1.ui.events.SelectionListener) safeArgs[0]); return null;
            }
        }
        if ("setMultiSelectionMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                typedTarget.setMultiSelectionMode(((Boolean) safeArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPlaceHolderImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                typedTarget.setPlaceHolderImage((com.codename1.ui.Image) safeArgs[0]); return null;
            }
        }
        if ("setSelectedIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setSelectedIndex(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setSelectedIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, true)) {
                int[] varArgs = new int[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = ((Number) safeArgs[i]).intValue();
                }
                typedTarget.setSelectedIndices(varArgs); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        throw unsupportedStaticField(type, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        throw unsupportedFieldWrite(target, name, value);
    }

    private static Object[] safeArgs(Object[] args) {
        return args == null ? new Object[0] : args;
    }

    private static boolean matches(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (!varArgs) {
            if (args.length != paramTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!matchesType(args[i], paramTypes[i])) {
                    return false;
                }
            }
            return true;
        }
        if (paramTypes.length == 0) {
            return true;
        }
        int fixedCount = paramTypes.length - 1;
        if (args.length < fixedCount) {
            return false;
        }
        for (int i = 0; i < fixedCount; i++) {
            if (!matchesType(args[i], paramTypes[i])) {
                return false;
            }
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < args.length; i++) {
            if (!matchesType(args[i], componentType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesType(Object value, Class<?> type) {
        if (type == Object.class) {
            return true;
        }
        if (value == null) {
            return !type.isPrimitive();
        }
        if (type.isArray()) {
            return type.isInstance(value);
        }
        if ("boolean".equals(type.getName()) || type == Boolean.class) {
            return value instanceof Boolean;
        }
        if ("char".equals(type.getName()) || type == Character.class) {
            return value instanceof Character;
        }
        if ("byte".equals(type.getName()) || type == Byte.class || "short".equals(type.getName()) || type == Short.class
                || "int".equals(type.getName()) || type == Integer.class || "long".equals(type.getName()) || type == Long.class
                || "float".equals(type.getName()) || type == Float.class || "double".equals(type.getName()) || type == Double.class) {
            return value instanceof Number;
        }
        return type.isInstance(value);
    }

    private static CN1AccessException unsupportedConstruct(Class<?> type, Object[] args) {
        return new CN1AccessException("Generated constructor dispatch not implemented for " + type.getName() + describeArgs(args));
    }

    private static CN1AccessException unsupportedStatic(Class<?> type, String name, Object[] args) {
        return new CN1AccessException("Generated static dispatch not implemented for " + type.getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedInstance(Object target, String name, Object[] args) {
        return new CN1AccessException("Generated instance dispatch not implemented for " + target.getClass().getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedStaticField(Class<?> type, String name) {
        return new CN1AccessException("Generated static field access not implemented for " + type.getName() + "." + name);
    }

    private static CN1AccessException unsupportedField(Object target, String name) {
        return new CN1AccessException("Generated field access not implemented for " + target.getClass().getName() + "." + name);
    }

    private static CN1AccessException unsupportedStaticFieldWrite(Class<?> type, String name, Object value) {
        return new CN1AccessException("Generated static field write not implemented for " + type.getName() + "." + name + " value=" + describeValue(value));
    }

    private static CN1AccessException unsupportedFieldWrite(Object target, String name, Object value) {
        return new CN1AccessException("Generated field write not implemented for " + target.getClass().getName() + "." + name + " value=" + describeValue(value));
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(describeValue(args[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String describeValue(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
