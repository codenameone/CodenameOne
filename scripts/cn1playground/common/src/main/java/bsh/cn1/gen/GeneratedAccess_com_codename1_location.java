package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_location {
    private GeneratedAccess_com_codename1_location() {
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
        if ("Geofence".equals(simpleName)) {
            return com.codename1.location.Geofence.class;
        }
        if ("GeofenceListener".equals(simpleName)) {
            return com.codename1.location.GeofenceListener.class;
        }
        if ("GeofenceManager".equals(simpleName)) {
            return com.codename1.location.GeofenceManager.class;
        }
        if ("Location".equals(simpleName)) {
            return com.codename1.location.Location.class;
        }
        if ("LocationListener".equals(simpleName)) {
            return com.codename1.location.LocationListener.class;
        }
        if ("LocationManager".equals(simpleName)) {
            return com.codename1.location.LocationManager.class;
        }
        if ("LocationRequest".equals(simpleName)) {
            return com.codename1.location.LocationRequest.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.location.Geofence.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.location.Location.class, java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.location.Location.class, java.lang.Integer.class, java.lang.Long.class}, false);
                return new com.codename1.location.Geofence((java.lang.String) adaptedArgs[0], (com.codename1.location.Location) adaptedArgs[1], ((Number) adaptedArgs[2]).intValue(), ((Number) adaptedArgs[3]).longValue());
            }
        }
        if (type == com.codename1.location.Location.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.location.Location();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class}, false);
                return new com.codename1.location.Location(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Float.class}, false);
                return new com.codename1.location.Location(((Number) adaptedArgs[0]).doubleValue(), ((Number) adaptedArgs[1]).doubleValue(), ((Number) adaptedArgs[2]).doubleValue(), ((Number) adaptedArgs[3]).floatValue());
            }
        }
        if (type == com.codename1.location.LocationRequest.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.location.LocationRequest();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class}, false);
                return new com.codename1.location.LocationRequest(((Number) adaptedArgs[0]).intValue(), ((Number) adaptedArgs[1]).longValue());
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.location.Geofence.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.location.GeofenceManager.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.location.LocationManager.class) return invokeStatic2(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("createDistanceComparator".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.location.Geofence.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.location.Geofence.class}, false);
                return com.codename1.location.Geofence.createDistanceComparator((com.codename1.location.Geofence) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.location.Location.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.location.Location.class}, false);
                return com.codename1.location.Geofence.createDistanceComparator((com.codename1.location.Location) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.location.Geofence.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getInstance".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.location.GeofenceManager.getInstance();
            }
        }
        throw unsupportedStatic(com.codename1.location.GeofenceManager.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("getLocationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.location.LocationManager.getLocationManager();
            }
        }
        throw unsupportedStatic(com.codename1.location.LocationManager.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.location.Geofence) {
            try {
                return invoke0((com.codename1.location.Geofence) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.location.GeofenceManager) {
            try {
                return invoke1((com.codename1.location.GeofenceManager) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.location.Location) {
            try {
                return invoke2((com.codename1.location.Location) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.location.LocationManager) {
            try {
                return invoke3((com.codename1.location.LocationManager) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.location.LocationRequest) {
            try {
                return invoke4((com.codename1.location.LocationRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.location.GeofenceListener) {
            try {
                return invoke5((com.codename1.location.GeofenceListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.location.LocationListener) {
            try {
                return invoke6((com.codename1.location.LocationListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.location.Geofence typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("equals".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                return typedTarget.equals((java.lang.Object) adaptedArgs[0]);
            }
        }
        if ("getDistanceTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.location.Geofence.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.location.Geofence.class}, false);
                return typedTarget.getDistanceTo((com.codename1.location.Geofence) adaptedArgs[0]);
            }
        }
        if ("getExpiration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExpiration();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getLoc".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLoc();
            }
        }
        if ("getRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRadius();
            }
        }
        if ("hashCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hashCode();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.location.GeofenceManager typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                typedTarget.add((java.util.Collection) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.location.Geofence[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.location.Geofence[].class}, true);
                com.codename1.location.Geofence[] varArgs = new com.codename1.location.Geofence[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.location.Geofence) adaptedArgs[i];
                }
                typedTarget.add(varArgs); return null;
            }
        }
        if ("asList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asList();
            }
        }
        if ("asMap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asMap();
            }
        }
        if ("asSortedList".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asSortedList();
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("getBubbleExpiration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBubbleExpiration();
            }
        }
        if ("getBubbleRadius".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBubbleRadius();
            }
        }
        if ("getListenerClass".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getListenerClass();
            }
        }
        if ("isBubble".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.isBubble((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isCurrentlyActive".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.isCurrentlyActive((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
        }
        if ("refresh".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refresh(); return null;
            }
        }
        if ("remove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Collection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Collection.class}, false);
                typedTarget.remove((java.util.Collection) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                typedTarget.remove(varArgs); return null;
            }
        }
        if ("setBubbleExpiration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setBubbleExpiration(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("setBubbleRadius".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setBubbleRadius(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setListenerClass".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class}, false);
                typedTarget.setListenerClass((java.lang.Class) adaptedArgs[0]); return null;
            }
        }
        if ("size".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.size();
            }
        }
        if ("update".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.update(((Number) adaptedArgs[0]).intValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Boolean.class}, false);
                typedTarget.update(((Number) adaptedArgs[0]).intValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.location.Location typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("createDistanceCompartor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createDistanceCompartor();
            }
        }
        if ("getAccuracy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccuracy();
            }
        }
        if ("getAltitude".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAltitude();
            }
        }
        if ("getDirection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirection();
            }
        }
        if ("getDistanceTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.location.Location.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.location.Location.class}, false);
                return typedTarget.getDistanceTo((com.codename1.location.Location) adaptedArgs[0]);
            }
        }
        if ("getLatitude".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLatitude();
            }
        }
        if ("getLongitude".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLongitude();
            }
        }
        if ("getLongtitude".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLongtitude();
            }
        }
        if ("getStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStatus();
            }
        }
        if ("getTimeStamp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimeStamp();
            }
        }
        if ("getVelocity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVelocity();
            }
        }
        if ("setAccuracy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setAccuracy(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setAltitude".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setAltitude(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setDirection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setDirection(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("setLatitude".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setLatitude(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setLongitude".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setLongitude(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setLongtitude".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class}, false);
                typedTarget.setLongtitude(((Number) adaptedArgs[0]).doubleValue()); return null;
            }
        }
        if ("setStatus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setStatus(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setTimeStamp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setTimeStamp(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("setVelocity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                typedTarget.setVelocity(((Number) adaptedArgs[0]).floatValue()); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.location.LocationManager typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addGeoFencing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class, com.codename1.location.Geofence.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class, com.codename1.location.Geofence.class}, false);
                typedTarget.addGeoFencing((java.lang.Class) adaptedArgs[0], (com.codename1.location.Geofence) adaptedArgs[1]); return null;
            }
        }
        if ("getCurrentLocation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentLocation();
            }
        }
        if ("getCurrentLocationSync".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCurrentLocationSync();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.getCurrentLocationSync(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("getLastKnownLocation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLastKnownLocation();
            }
        }
        if ("getStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStatus();
            }
        }
        if ("isBackgroundLocationSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBackgroundLocationSupported();
            }
        }
        if ("isGPSDetectionSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGPSDetectionSupported();
            }
        }
        if ("isGPSEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGPSEnabled();
            }
        }
        if ("isGeofenceSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGeofenceSupported();
            }
        }
        if ("removeGeoFencing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.removeGeoFencing((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setBackgroundLocationListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Class.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Class.class}, false);
                typedTarget.setBackgroundLocationListener((java.lang.Class) adaptedArgs[0]); return null;
            }
        }
        if ("setLocationListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.location.LocationListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.location.LocationListener.class}, false);
                typedTarget.setLocationListener((com.codename1.location.LocationListener) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.location.LocationListener.class, com.codename1.location.LocationRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.location.LocationListener.class, com.codename1.location.LocationRequest.class}, false);
                typedTarget.setLocationListener((com.codename1.location.LocationListener) adaptedArgs[0], (com.codename1.location.LocationRequest) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.location.LocationRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getInterval".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInterval();
            }
        }
        if ("getPriority".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPriority();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.location.GeofenceListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onEntered".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.onEntered((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("onExit".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.onExit((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.location.LocationListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("locationUpdated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.location.Location.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.location.Location.class}, false);
                typedTarget.locationUpdated((com.codename1.location.Location) adaptedArgs[0]); return null;
            }
        }
        if ("providerStateChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.providerStateChanged(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.location.LocationManager.class) {
            if ("AVAILABLE".equals(name)) return com.codename1.location.LocationManager.AVAILABLE;
            if ("OUT_OF_SERVICE".equals(name)) return com.codename1.location.LocationManager.OUT_OF_SERVICE;
            if ("TEMPORARILY_UNAVAILABLE".equals(name)) return com.codename1.location.LocationManager.TEMPORARILY_UNAVAILABLE;
        }
        if (type == com.codename1.location.LocationRequest.class) {
            if ("PRIORITY_HIGH_ACCUARCY".equals(name)) return com.codename1.location.LocationRequest.PRIORITY_HIGH_ACCUARCY;
            if ("PRIORITY_LOW_ACCUARCY".equals(name)) return com.codename1.location.LocationRequest.PRIORITY_LOW_ACCUARCY;
            if ("PRIORITY_MEDIUM_ACCUARCY".equals(name)) return com.codename1.location.LocationRequest.PRIORITY_MEDIUM_ACCUARCY;
        }
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
