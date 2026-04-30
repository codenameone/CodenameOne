package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_contacts {
    private GeneratedAccess_com_codename1_contacts() {
    }

    public static Class<?> findClass(String name) {
        int lastDot = name == null ? -1 : name.lastIndexOf('.');
        if (lastDot < 0 || lastDot == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(lastDot + 1));
    }

    public static Class<?> findClassBySimpleName(String simpleName) {
        Class<?> found0 = findClassChunk0(simpleName);
        if (found0 != null) {
            return found0;
        }
        return null;
    }


    private static Class<?> findClassChunk0(String simpleName) {
        if ("Address".equals(simpleName)) {
            return com.codename1.contacts.Address.class;
        }
        if ("Contact".equals(simpleName)) {
            return com.codename1.contacts.Contact.class;
        }
        if ("ContactsManager".equals(simpleName)) {
            return com.codename1.contacts.ContactsManager.class;
        }
        if ("ContactsModel".equals(simpleName)) {
            return com.codename1.contacts.ContactsModel.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.contacts.ContactsModel.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
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
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.contacts.ContactsManager.createContact((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4], (java.lang.String) adaptedArgs[5]);
            }
        }
        if ("deleteContact".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.contacts.ContactsManager.deleteContact((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getAllContacts".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.contacts.ContactsManager.getAllContacts();
            }
        }
        if ("getAllContactsWithNumbers".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.contacts.ContactsManager.getAllContactsWithNumbers();
            }
        }
        if ("getContactById".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.contacts.ContactsManager.getContactById((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false);
                return com.codename1.contacts.ContactsManager.getContactById((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), ((Boolean) adaptedArgs[2]).booleanValue(), ((Boolean) adaptedArgs[3]).booleanValue(), ((Boolean) adaptedArgs[4]).booleanValue(), ((Boolean) adaptedArgs[5]).booleanValue());
            }
        }
        if ("getContacts".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false);
                return com.codename1.contacts.ContactsManager.getContacts(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue(), ((Boolean) adaptedArgs[2]).booleanValue(), ((Boolean) adaptedArgs[3]).booleanValue(), ((Boolean) adaptedArgs[4]).booleanValue(), ((Boolean) adaptedArgs[5]).booleanValue());
            }
        }
        if ("isAllContactsFast".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.contacts.ContactsManager.isAllContactsFast();
            }
        }
        if ("refresh".equals(name)) {
            if (safeArgs.length == 0) {
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
            if (safeArgs.length == 0) {
                return typedTarget.getCountry();
            }
        }
        if ("getLocality".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocality();
            }
        }
        if ("getPostalCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPostalCode();
            }
        }
        if ("getRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRegion();
            }
        }
        if ("getStreetAddress".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStreetAddress();
            }
        }
        if ("setCountry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCountry((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setLocality".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLocality((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPostalCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPostalCode((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setRegion((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setStreetAddress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setStreetAddress((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.contacts.Contact typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAddresses".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAddresses();
            }
        }
        if ("getBirthday".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBirthday();
            }
        }
        if ("getDisplayName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayName();
            }
        }
        if ("getEmails".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEmails();
            }
        }
        if ("getFamilyName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFamilyName();
            }
        }
        if ("getFirstName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFirstName();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getLinkedContactIds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLinkedContactIds();
            }
        }
        if ("getNote".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNote();
            }
        }
        if ("getPhoneNumbers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPhoneNumbers();
            }
        }
        if ("getPhoto".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPhoto();
            }
        }
        if ("getPrimaryEmail".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPrimaryEmail();
            }
        }
        if ("getPrimaryPhoneNumber".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPrimaryPhoneNumber();
            }
        }
        if ("getUrls".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUrls();
            }
        }
        if ("setAddresses".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                typedTarget.setAddresses((java.util.Hashtable) adaptedArgs[0]); return null;
            }
        }
        if ("setBirthday".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setBirthday(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("setDisplayName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDisplayName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setEmails".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                typedTarget.setEmails((java.util.Hashtable) adaptedArgs[0]); return null;
            }
        }
        if ("setFamilyName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setFamilyName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setFirstName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setFirstName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNote".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setNote((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPhoneNumbers".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                typedTarget.setPhoneNumbers((java.util.Hashtable) adaptedArgs[0]); return null;
            }
        }
        if ("setPhoto".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setPhoto((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setPrimaryEmail".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPrimaryEmail((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPrimaryPhoneNumber".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPrimaryPhoneNumber((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setUrls".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, false);
                typedTarget.setUrls((java.lang.String[]) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.contacts.ContactsManager typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAllContacts".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false);
                return typedTarget.getAllContacts(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue(), ((Boolean) adaptedArgs[2]).booleanValue(), ((Boolean) adaptedArgs[3]).booleanValue(), ((Boolean) adaptedArgs[4]).booleanValue(), ((Boolean) adaptedArgs[5]).booleanValue());
            }
        }
        if ("isGetAllContactsFast".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGetAllContactsFast();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.contacts.ContactsModel typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addDataChangedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.DataChangedListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.DataChangedListener.class}, false);
                typedTarget.addDataChangedListener((com.codename1.ui.events.DataChangedListener) adaptedArgs[0]); return null;
            }
        }
        if ("addItem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.addItem((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("addSelectedIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = toIntValue(adaptedArgs[i]);
                }
                typedTarget.addSelectedIndices(varArgs); return null;
            }
        }
        if ("addSelectionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.SelectionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.SelectionListener.class}, false);
                typedTarget.addSelectionListener((com.codename1.ui.events.SelectionListener) adaptedArgs[0]); return null;
            }
        }
        if ("getItemAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getItemAt(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getList();
            }
        }
        if ("getSelectedIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedIndex();
            }
        }
        if ("getSelectedIndices".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedIndices();
            }
        }
        if ("getSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSize();
            }
        }
        if ("isMultiSelectionMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isMultiSelectionMode();
            }
        }
        if ("removeAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeDataChangedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.DataChangedListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.DataChangedListener.class}, false);
                typedTarget.removeDataChangedListener((com.codename1.ui.events.DataChangedListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeItem".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.removeItem(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("removeSelectedIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = toIntValue(adaptedArgs[i]);
                }
                typedTarget.removeSelectedIndices(varArgs); return null;
            }
        }
        if ("removeSelectionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.SelectionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.SelectionListener.class}, false);
                typedTarget.removeSelectionListener((com.codename1.ui.events.SelectionListener) adaptedArgs[0]); return null;
            }
        }
        if ("setMultiSelectionMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setMultiSelectionMode(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPlaceHolderImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setPlaceHolderImage((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setSelectedIndex(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setSelectedIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class}, true);
                int[] varArgs = new int[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = toIntValue(adaptedArgs[i]);
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

    private static Object[] adaptArgs(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (args == null || args.length == 0) {
            return args == null ? new Object[0] : args;
        }
        Object[] adapted = args.clone();
        if (!varArgs) {
            for (int i = 0; i < Math.min(adapted.length, paramTypes.length); i++) {
                adapted[i] = adaptValue(adapted[i], paramTypes[i]);
            }
            return adapted;
        }
        if (paramTypes.length == 0) {
            return adapted;
        }
        int fixedCount = paramTypes.length - 1;
        for (int i = 0; i < Math.min(fixedCount, adapted.length); i++) {
            adapted[i] = adaptValue(adapted[i], paramTypes[i]);
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < adapted.length; i++) {
            adapted[i] = adaptValue(adapted[i], componentType);
        }
        return adapted;
    }

    private static boolean isSamInterface(Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return true;
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return true;
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return true;
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return true;
        }
        if (type == java.lang.Runnable.class) {
            return true;
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return true;
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return true;
        }
        return false;
    }

    private static Object adaptLambdaValue(final bsh.cn1.CN1LambdaSupport.LambdaValue lambda, Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return new com.codename1.util.OnComplete() {
                public void completed(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return new com.codename1.util.SuccessCallback() {
                public void onSucess(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return new com.codename1.util.FailureCallback() {
                public void onError(java.lang.Object arg0, java.lang.Throwable arg1, int arg2, java.lang.String arg3) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1, arg2, arg3});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(com.codename1.ui.events.ActionEvent arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == java.lang.Runnable.class) {
            return new java.lang.Runnable() {
                public void run() {
                    try {
                        lambda.invoke(new Object[0]);
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return new com.codename1.ui.events.DataChangedListener() {
                public void dataChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return new com.codename1.ui.events.SelectionListener() {
                public void selectionChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        return lambda;
    }

    private static Object adaptValue(Object value, Class<?> type) {
        if (!(value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue)) {
            return value;
        }
        // Direct fit when LambdaValue already implements the target SAM
        // (Runnable, Function, Comparator, ...).
        if (type.isInstance(value)) {
            return value;
        }
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
    }

    private static int toIntValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Character) return (int) ((Character) value).charValue();
        throw new ClassCastException("Cannot coerce "
            + (value == null ? "null" : value.getClass().getName()) + " to int");
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
            // Java widens char to int implicitly, so accept Character
            // for any int-or-larger numeric slot.
            return value instanceof Number || value instanceof Character;
        }
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            // LambdaValue implements common SAMs directly (Runnable,
            // Function, Predicate, Comparator, ...). Also accept any
            // CN1 SAM the listener-bridge knows how to wrap.
            return type.isInstance(value) || isSamInterface(type);
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
