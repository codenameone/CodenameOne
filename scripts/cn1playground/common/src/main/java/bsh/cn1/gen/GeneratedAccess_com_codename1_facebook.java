package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_facebook {
    private GeneratedAccess_com_codename1_facebook() {
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
        if ("Album".equals(simpleName)) {
            return com.codename1.facebook.Album.class;
        }
        if ("FBObject".equals(simpleName)) {
            return com.codename1.facebook.FBObject.class;
        }
        if ("FaceBookAccess".equals(simpleName)) {
            return com.codename1.facebook.FaceBookAccess.class;
        }
        if ("Page".equals(simpleName)) {
            return com.codename1.facebook.Page.class;
        }
        if ("Photo".equals(simpleName)) {
            return com.codename1.facebook.Photo.class;
        }
        if ("Post".equals(simpleName)) {
            return com.codename1.facebook.Post.class;
        }
        if ("User".equals(simpleName)) {
            return com.codename1.facebook.User.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.facebook.Album.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.facebook.Album();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                return new com.codename1.facebook.Album((java.util.Hashtable) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.facebook.FBObject.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.facebook.FBObject();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                return new com.codename1.facebook.FBObject((java.util.Hashtable) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.facebook.Page.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.facebook.Page();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                return new com.codename1.facebook.Page((java.util.Hashtable) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.facebook.Photo.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.facebook.Photo();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                return new com.codename1.facebook.Photo((java.util.Hashtable) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.facebook.Post.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.facebook.Post();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                return new com.codename1.facebook.Post((java.util.Hashtable) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.facebook.User.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.facebook.User();
            }
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                return new com.codename1.facebook.User((java.util.Hashtable) adaptedArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.facebook.FaceBookAccess.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("anonymousLogin".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                com.codename1.facebook.FaceBookAccess.anonymousLogin((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("createObjectsModel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.list.DefaultListModel.class, java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.list.DefaultListModel.class, java.lang.Class.class}, false);
                return com.codename1.facebook.FaceBookAccess.createObjectsModel((com.codename1.ui.list.DefaultListModel) adaptedArgs[0], (java.lang.Class) adaptedArgs[1]);
            }
        }
        if ("getApiVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.facebook.FaceBookAccess.getApiVersion();
            }
        }
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.facebook.FaceBookAccess.getInstance();
            }
        }
        if ("getToken".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.facebook.FaceBookAccess.getToken();
            }
        }
        if ("logOut".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.facebook.FaceBookAccess.logOut(); return null;
            }
        }
        if ("setApiVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.facebook.FaceBookAccess.setApiVersion((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setClientId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.facebook.FaceBookAccess.setClientId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setClientSecret".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.facebook.FaceBookAccess.setClientSecret((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPermissions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, false);
                com.codename1.facebook.FaceBookAccess.setPermissions((java.lang.String[]) adaptedArgs[0]); return null;
            }
        }
        if ("setRedirectURI".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.facebook.FaceBookAccess.setRedirectURI((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setToken".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.facebook.FaceBookAccess.setToken((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.facebook.FaceBookAccess.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.facebook.Album) {
            try {
                return invoke0((com.codename1.facebook.Album) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.facebook.Page) {
            try {
                return invoke1((com.codename1.facebook.Page) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.facebook.Photo) {
            try {
                return invoke2((com.codename1.facebook.Photo) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.facebook.Post) {
            try {
                return invoke3((com.codename1.facebook.Post) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.facebook.User) {
            try {
                return invoke4((com.codename1.facebook.User) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.facebook.FBObject) {
            try {
                return invoke5((com.codename1.facebook.FBObject) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.facebook.FaceBookAccess) {
            try {
                return invoke6((com.codename1.facebook.FaceBookAccess) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.facebook.Album typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                typedTarget.copy((java.util.Hashtable) adaptedArgs[0]); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCount();
            }
        }
        if ("getCoverPhoto".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCoverPhoto();
            }
        }
        if ("getCover_photo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCover_photo();
            }
        }
        if ("getCreatedTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCreatedTime();
            }
        }
        if ("getCreated_time".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCreated_time();
            }
        }
        if ("getDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescription();
            }
        }
        if ("getFrom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFrom();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getLink".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLink();
            }
        }
        if ("getLocation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocation();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getPrivacy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPrivacy();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("getUpdatedTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUpdatedTime();
            }
        }
        if ("getUpdated_time".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUpdated_time();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.facebook.Page typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                typedTarget.copy((java.util.Hashtable) adaptedArgs[0]); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getAbout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbout();
            }
        }
        if ("getCategory".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCategory();
            }
        }
        if ("getCoverId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCoverId();
            }
        }
        if ("getCoverLink".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCoverLink();
            }
        }
        if ("getFounded".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFounded();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getLikesCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLikesCount();
            }
        }
        if ("getLink".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLink();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getUsername".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUsername();
            }
        }
        if ("getWebsite".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWebsite();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.facebook.Photo typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                typedTarget.copy((java.util.Hashtable) adaptedArgs[0]); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getComments".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComments();
            }
        }
        if ("getCreatedTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCreatedTime();
            }
        }
        if ("getCreated_time".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCreated_time();
            }
        }
        if ("getFrom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFrom();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getIconUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIconUrl();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getImages".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getImages();
            }
        }
        if ("getLink".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLink();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getPictureUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPictureUrl();
            }
        }
        if ("getPosition".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPosition();
            }
        }
        if ("getSourceUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSourceUrl();
            }
        }
        if ("getUpdatedTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUpdatedTime();
            }
        }
        if ("getUpdated_time".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUpdated_time();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.facebook.Post typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                typedTarget.copy((java.util.Hashtable) adaptedArgs[0]); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getAttribution".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAttribution();
            }
        }
        if ("getCommentsCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCommentsCount();
            }
        }
        if ("getCreatedTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCreatedTime();
            }
        }
        if ("getFrom".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFrom();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getLikes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLikes();
            }
        }
        if ("getLinkDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLinkDescription();
            }
        }
        if ("getLinkName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLinkName();
            }
        }
        if ("getLinkUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLinkUrl();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getPicture".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPicture();
            }
        }
        if ("getTo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTo();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.facebook.User typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                typedTarget.copy((java.util.Hashtable) adaptedArgs[0]); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getAbout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbout();
            }
        }
        if ("getBio".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBio();
            }
        }
        if ("getBirthday".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBirthday();
            }
        }
        if ("getEmail".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEmail();
            }
        }
        if ("getFirstName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFirstName();
            }
        }
        if ("getFirst_name".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFirst_name();
            }
        }
        if ("getGender".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGender();
            }
        }
        if ("getHometown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHometown();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getLastName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLastName();
            }
        }
        if ("getLastUpdated".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLastUpdated();
            }
        }
        if ("getLast_name".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLast_name();
            }
        }
        if ("getLast_updated".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLast_updated();
            }
        }
        if ("getLink".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLink();
            }
        }
        if ("getLocale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocale();
            }
        }
        if ("getLocation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocation();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getQuotes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getQuotes();
            }
        }
        if ("getRelationshipStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRelationshipStatus();
            }
        }
        if ("getRelationship_status".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRelationship_status();
            }
        }
        if ("getTimezone".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimezone();
            }
        }
        if ("getUsername".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUsername();
            }
        }
        if ("getWebsite".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWebsite();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.facebook.FBObject typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("copy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Hashtable.class}, false);
                typedTarget.copy((java.util.Hashtable) adaptedArgs[0]); return null;
            }
        }
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.facebook.FaceBookAccess typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addResponseCodeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("cleanTempStorage".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.cleanTempStorage(); return null;
            }
        }
        if ("createAuthComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                return typedTarget.createAuthComponent((com.codename1.ui.events.ActionListener) adaptedArgs[0]);
            }
        }
        if ("createNote".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                typedTarget.createNote((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.createNote((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.ui.events.ActionListener) adaptedArgs[3]); return null;
            }
        }
        if ("createOAuth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createOAuth();
            }
        }
        if ("getAlbum".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.getAlbum((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.facebook.Album.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.facebook.Album.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getAlbum((java.lang.String) adaptedArgs[0], (com.codename1.facebook.Album) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
        }
        if ("getAlbumPhotos".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getAlbumPhotos((java.lang.String) adaptedArgs[0], (com.codename1.ui.list.DefaultListModel) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).intValue(), (com.codename1.ui.events.ActionListener) adaptedArgs[4]); return null;
            }
        }
        if ("getFaceBookObject".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getFaceBookObject((java.lang.String) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.getFaceBookObject((java.lang.String) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue(), ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("getFaceBookObjectItems".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, java.util.Hashtable.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, java.util.Hashtable.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getFaceBookObjectItems((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.ui.list.DefaultListModel) adaptedArgs[2], (java.util.Hashtable) adaptedArgs[3], (com.codename1.ui.events.ActionListener) adaptedArgs[4]); return null;
            }
        }
        if ("getImageURL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.geom.Dimension.class}, false);
                return typedTarget.getImageURL((java.lang.String) adaptedArgs[0], (com.codename1.ui.geom.Dimension) adaptedArgs[1]);
            }
        }
        if ("getNewsFeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getNewsFeed((java.lang.String) adaptedArgs[0], (com.codename1.ui.list.DefaultListModel) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, java.lang.Integer.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, java.lang.Integer.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getNewsFeed((java.lang.String) adaptedArgs[0], (com.codename1.ui.list.DefaultListModel) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (com.codename1.ui.events.ActionListener) adaptedArgs[3]); return null;
            }
        }
        if ("getPage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPage((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getPhoto".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.getPhoto((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.facebook.Photo.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.facebook.Photo.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getPhoto((java.lang.String) adaptedArgs[0], (com.codename1.facebook.Photo) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
        }
        if ("getPhotoThumbnail".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, java.lang.Boolean.class}, false);
                typedTarget.getPhotoThumbnail((java.lang.String) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class, com.codename1.ui.geom.Dimension.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class, com.codename1.ui.geom.Dimension.class, java.lang.Boolean.class}, false);
                typedTarget.getPhotoThumbnail((java.lang.String) adaptedArgs[0], (com.codename1.ui.Label) adaptedArgs[1], (com.codename1.ui.geom.Dimension) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("getPicture".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class, com.codename1.ui.geom.Dimension.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Label.class, com.codename1.ui.geom.Dimension.class, java.lang.Boolean.class}, false);
                typedTarget.getPicture((java.lang.String) adaptedArgs[0], (com.codename1.ui.Label) adaptedArgs[1], (com.codename1.ui.geom.Dimension) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, com.codename1.ui.geom.Dimension.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class, com.codename1.ui.geom.Dimension.class, java.lang.Boolean.class}, false);
                typedTarget.getPicture((java.lang.String) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1], (com.codename1.ui.geom.Dimension) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.String.class, com.codename1.ui.geom.Dimension.class, java.lang.Boolean.class}, false);
                typedTarget.getPicture((java.lang.String) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (java.lang.String) adaptedArgs[3], (com.codename1.ui.geom.Dimension) adaptedArgs[4], ((Boolean) adaptedArgs[5]).booleanValue()); return null;
            }
        }
        if ("getPictureAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.geom.Dimension.class}, false);
                return typedTarget.getPictureAndWait((java.lang.String) adaptedArgs[0], (com.codename1.ui.geom.Dimension) adaptedArgs[1]);
            }
        }
        if ("getPost".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return typedTarget.getPost((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.facebook.Post.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.facebook.Post.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getPost((java.lang.String) adaptedArgs[0], (com.codename1.facebook.Post) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
        }
        if ("getPostComments".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getPostComments((java.lang.String) adaptedArgs[0], (com.codename1.ui.list.DefaultListModel) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
        }
        if ("getUser".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getUser((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.facebook.User.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.facebook.User.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getUser((java.lang.String) adaptedArgs[0], (com.codename1.facebook.User) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
        }
        if ("getUserAlbums".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getUserAlbums((java.lang.String) adaptedArgs[0], (com.codename1.ui.list.DefaultListModel) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
        }
        if ("getUserEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getUserEvents((java.lang.String) adaptedArgs[0], (com.codename1.ui.list.DefaultListModel) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
        }
        if ("getUserFriends".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getUserFriends((java.lang.String) adaptedArgs[0], (com.codename1.ui.list.DefaultListModel) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
        }
        if ("getUserInboxThreads".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, java.lang.Integer.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, java.lang.Integer.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getUserInboxThreads((java.lang.String) adaptedArgs[0], (com.codename1.ui.list.DefaultListModel) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (com.codename1.ui.events.ActionListener) adaptedArgs[3]); return null;
            }
        }
        if ("getUserNotifications".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getUserNotifications((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], ((Boolean) adaptedArgs[2]).booleanValue(), (com.codename1.ui.list.DefaultListModel) adaptedArgs[3], (com.codename1.ui.events.ActionListener) adaptedArgs[4]); return null;
            }
        }
        if ("getUsersDetails".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.String[].class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class, java.lang.String[].class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getUsersDetails((java.lang.String[]) adaptedArgs[0], (java.lang.String[]) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
        }
        if ("getWallFeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getWallFeed((java.lang.String) adaptedArgs[0], (com.codename1.ui.list.DefaultListModel) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, java.lang.Integer.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, java.lang.Integer.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getWallFeed((java.lang.String) adaptedArgs[0], (com.codename1.ui.list.DefaultListModel) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (com.codename1.ui.events.ActionListener) adaptedArgs[3]); return null;
            }
        }
        if ("getWallPosts".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getWallPosts((java.lang.String) adaptedArgs[0], (com.codename1.ui.list.DefaultListModel) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, java.lang.Integer.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, java.lang.Integer.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.getWallPosts((java.lang.String) adaptedArgs[0], (com.codename1.ui.list.DefaultListModel) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), (com.codename1.ui.events.ActionListener) adaptedArgs[3]); return null;
            }
        }
        if ("isAuthenticated".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAuthenticated();
            }
        }
        if ("killCurrentRequest".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.killCurrentRequest(); return null;
            }
        }
        if ("postComment".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.postComment((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.postComment((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
        }
        if ("postLike".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.postLike((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.postLike((java.lang.String) adaptedArgs[0], (com.codename1.ui.events.ActionListener) adaptedArgs[1]); return null;
            }
        }
        if ("postOnWall".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.postOnWall((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.postOnWall((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.ui.events.ActionListener) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.postOnWall((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4], (java.lang.String) adaptedArgs[5], (java.lang.String) adaptedArgs[6], (com.codename1.ui.events.ActionListener) adaptedArgs[7]); return null;
            }
        }
        if ("removeResponseCodeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeResponseCodeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("search".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, com.codename1.ui.list.DefaultListModel.class, com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.search((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (com.codename1.ui.list.DefaultListModel) adaptedArgs[2], (com.codename1.ui.events.ActionListener) adaptedArgs[3]); return null;
            }
        }
        if ("setProgress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Slider.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Slider.class}, false);
                typedTarget.setProgress((com.codename1.ui.Slider) adaptedArgs[0]); return null;
            }
        }
        if ("showAuthentication".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.showAuthentication((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
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
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
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
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            return isSamInterface(type);
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
