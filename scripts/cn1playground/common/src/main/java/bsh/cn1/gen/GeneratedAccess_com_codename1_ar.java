package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ar {
    private GeneratedAccess_com_codename1_ar() {
    }

    public static Class<?> findClass(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        int dollar = name.lastIndexOf('$');
        int sep = dot > dollar ? dot : dollar;
        if (sep < 0 || sep == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(sep + 1));
    }

    public static Class<?> findClassBySimpleName(String simpleName) {
        Class<?> found0 = findClassChunk0(simpleName);
        if (found0 != null) {
            return found0;
        }
        return null;
    }


    private static Class<?> findClassChunk0(String simpleName) {
        if ("AR".equals(simpleName)) {
            return com.codename1.ar.AR.class;
        }
        if ("ARAnchor".equals(simpleName)) {
            return com.codename1.ar.ARAnchor.class;
        }
        if ("ARAnchorEvent".equals(simpleName)) {
            return com.codename1.ar.ARAnchorEvent.class;
        }
        if ("Kind".equals(simpleName)) {
            return com.codename1.ar.ARAnchorEvent.Kind.class;
        }
        if ("ARAnchorListener".equals(simpleName)) {
            return com.codename1.ar.ARAnchorListener.class;
        }
        if ("ARCapabilities".equals(simpleName)) {
            return com.codename1.ar.ARCapabilities.class;
        }
        if ("ARFaceAnchor".equals(simpleName)) {
            return com.codename1.ar.ARFaceAnchor.class;
        }
        if ("ARFaceRegion".equals(simpleName)) {
            return com.codename1.ar.ARFaceRegion.class;
        }
        if ("ARHitResult".equals(simpleName)) {
            return com.codename1.ar.ARHitResult.class;
        }
        if ("Type".equals(simpleName)) {
            return com.codename1.ar.ARHitResult.Type.class;
        }
        if ("ARImageAnchor".equals(simpleName)) {
            return com.codename1.ar.ARImageAnchor.class;
        }
        if ("ARLightEstimate".equals(simpleName)) {
            return com.codename1.ar.ARLightEstimate.class;
        }
        if ("ARModel".equals(simpleName)) {
            return com.codename1.ar.ARModel.class;
        }
        if ("ARNode".equals(simpleName)) {
            return com.codename1.ar.ARNode.class;
        }
        if ("ARPlane".equals(simpleName)) {
            return com.codename1.ar.ARPlane.class;
        }
        if ("Type".equals(simpleName)) {
            return com.codename1.ar.ARPlane.Type.class;
        }
        if ("ARPlaneDetection".equals(simpleName)) {
            return com.codename1.ar.ARPlaneDetection.class;
        }
        if ("ARPlaneEvent".equals(simpleName)) {
            return com.codename1.ar.ARPlaneEvent.class;
        }
        if ("Kind".equals(simpleName)) {
            return com.codename1.ar.ARPlaneEvent.Kind.class;
        }
        if ("ARPlaneListener".equals(simpleName)) {
            return com.codename1.ar.ARPlaneListener.class;
        }
        if ("ARPose".equals(simpleName)) {
            return com.codename1.ar.ARPose.class;
        }
        if ("ARReferenceImage".equals(simpleName)) {
            return com.codename1.ar.ARReferenceImage.class;
        }
        if ("ARSession".equals(simpleName)) {
            return com.codename1.ar.ARSession.class;
        }
        if ("ARSessionOptions".equals(simpleName)) {
            return com.codename1.ar.ARSessionOptions.class;
        }
        if ("ARTrackingFailureReason".equals(simpleName)) {
            return com.codename1.ar.ARTrackingFailureReason.class;
        }
        if ("ARTrackingListener".equals(simpleName)) {
            return com.codename1.ar.ARTrackingListener.class;
        }
        if ("ARTrackingMode".equals(simpleName)) {
            return com.codename1.ar.ARTrackingMode.class;
        }
        if ("ARTrackingState".equals(simpleName)) {
            return com.codename1.ar.ARTrackingState.class;
        }
        if ("ARView".equals(simpleName)) {
            return com.codename1.ar.ARView.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ar.ARAnchor.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ar.ARPose.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ar.ARPose.class}, false);
                return new com.codename1.ar.ARAnchor((java.lang.String) adaptedArgs[0], (com.codename1.ar.ARPose) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.ar.ARCapabilities.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class}, false);
                return new com.codename1.ar.ARCapabilities(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue(), ((Boolean) adaptedArgs[2]).booleanValue(), ((Boolean) adaptedArgs[3]).booleanValue(), ((Boolean) adaptedArgs[4]).booleanValue());
            }
        }
        if (type == com.codename1.ar.ARFaceAnchor.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ar.ARPose.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ar.ARPose.class}, false);
                return new com.codename1.ar.ARFaceAnchor((java.lang.String) adaptedArgs[0], (com.codename1.ar.ARPose) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.ar.ARHitResult.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARPose.class, java.lang.Float.class, com.codename1.ar.ARHitResult.Type.class, com.codename1.ar.ARPlane.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARPose.class, java.lang.Float.class, com.codename1.ar.ARHitResult.Type.class, com.codename1.ar.ARPlane.class, java.lang.Object.class}, false);
                return new com.codename1.ar.ARHitResult((com.codename1.ar.ARPose) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), (com.codename1.ar.ARHitResult.Type) adaptedArgs[2], (com.codename1.ar.ARPlane) adaptedArgs[3], (java.lang.Object) adaptedArgs[4]);
            }
        }
        if (type == com.codename1.ar.ARImageAnchor.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ar.ARPose.class, java.lang.String.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ar.ARPose.class, java.lang.String.class, java.lang.Float.class}, false);
                return new com.codename1.ar.ARImageAnchor((java.lang.String) adaptedArgs[0], (com.codename1.ar.ARPose) adaptedArgs[1], (java.lang.String) adaptedArgs[2], ((Number) adaptedArgs[3]).floatValue());
            }
        }
        if (type == com.codename1.ar.ARLightEstimate.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return new com.codename1.ar.ARLightEstimate(((Boolean) adaptedArgs[0]).booleanValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue());
            }
        }
        if (type == com.codename1.ar.ARNode.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ar.ARNode();
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARModel.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARModel.class}, false);
                return new com.codename1.ar.ARNode((com.codename1.ar.ARModel) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ar.ARPlane.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ar.ARPlane.Type.class, com.codename1.ar.ARPose.class, java.lang.Float.class, java.lang.Float.class, float[].class, com.codename1.ar.ARTrackingState.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ar.ARPlane.Type.class, com.codename1.ar.ARPose.class, java.lang.Float.class, java.lang.Float.class, float[].class, com.codename1.ar.ARTrackingState.class}, false);
                return new com.codename1.ar.ARPlane((java.lang.String) adaptedArgs[0], (com.codename1.ar.ARPlane.Type) adaptedArgs[1], (com.codename1.ar.ARPose) adaptedArgs[2], ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), (float[]) adaptedArgs[5], (com.codename1.ar.ARTrackingState) adaptedArgs[6]);
            }
        }
        if (type == com.codename1.ar.ARPose.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return new com.codename1.ar.ARPose(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue(), ((Number) adaptedArgs[6]).floatValue());
            }
        }
        if (type == com.codename1.ar.ARReferenceImage.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, byte[].class, java.lang.Float.class}, false);
                return new com.codename1.ar.ARReferenceImage((java.lang.String) adaptedArgs[0], (byte[]) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Image.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.Image.class, java.lang.Float.class}, false);
                return new com.codename1.ar.ARReferenceImage((java.lang.String) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1], ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if (type == com.codename1.ar.ARSessionOptions.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.ar.ARSessionOptions();
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ar.AR.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.ar.ARModel.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.ar.ARPose.class) return invokeStatic2(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("getCapabilities".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ar.AR.getCapabilities();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ar.AR.isSupported();
            }
        }
        if ("open".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARSessionOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARSessionOptions.class}, false);
                return com.codename1.ar.AR.open((com.codename1.ar.ARSessionOptions) adaptedArgs[0]);
            }
        }
        if ("requestPermissions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false);
                com.codename1.ar.AR.requestPermissions((com.codename1.util.SuccessCallback) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ar.AR.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("fromGltf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                return com.codename1.ar.ARModel.fromGltf((byte[]) adaptedArgs[0]);
            }
        }
        if ("fromMesh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.gpu.Mesh.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gpu.Mesh.class, java.lang.Integer.class}, false);
                return com.codename1.ar.ARModel.fromMesh((com.codename1.gpu.Mesh) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.gpu.Mesh.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.gpu.Mesh.class, com.codename1.ui.Image.class}, false);
                return com.codename1.ar.ARModel.fromMesh((com.codename1.gpu.Mesh) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.ar.ARModel.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("fromMatrix".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class}, false);
                return com.codename1.ar.ARPose.fromMatrix((float[]) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ar.ARPose.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.ar.ARFaceAnchor) {
            try {
                return invoke0((com.codename1.ar.ARFaceAnchor) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARImageAnchor) {
            try {
                return invoke1((com.codename1.ar.ARImageAnchor) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARAnchor) {
            try {
                return invoke2((com.codename1.ar.ARAnchor) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARAnchorEvent) {
            try {
                return invoke3((com.codename1.ar.ARAnchorEvent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARCapabilities) {
            try {
                return invoke4((com.codename1.ar.ARCapabilities) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARHitResult) {
            try {
                return invoke5((com.codename1.ar.ARHitResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARLightEstimate) {
            try {
                return invoke6((com.codename1.ar.ARLightEstimate) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARModel) {
            try {
                return invoke7((com.codename1.ar.ARModel) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARNode) {
            try {
                return invoke8((com.codename1.ar.ARNode) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARPlane) {
            try {
                return invoke9((com.codename1.ar.ARPlane) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARPlaneDetection) {
            try {
                return invoke10((com.codename1.ar.ARPlaneDetection) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARPlaneEvent) {
            try {
                return invoke11((com.codename1.ar.ARPlaneEvent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARPose) {
            try {
                return invoke12((com.codename1.ar.ARPose) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARReferenceImage) {
            try {
                return invoke13((com.codename1.ar.ARReferenceImage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARSession) {
            try {
                return invoke14((com.codename1.ar.ARSession) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARSessionOptions) {
            try {
                return invoke15((com.codename1.ar.ARSessionOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARView) {
            try {
                return invoke16((com.codename1.ar.ARView) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARAnchorListener) {
            try {
                return invoke17((com.codename1.ar.ARAnchorListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARPlaneListener) {
            try {
                return invoke18((com.codename1.ar.ARPlaneListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ar.ARTrackingListener) {
            try {
                return invoke19((com.codename1.ar.ARTrackingListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ar.ARFaceAnchor typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("detach".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.detach(); return null;
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMeshTriangles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMeshTriangles();
            }
        }
        if ("getMeshVertices".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMeshVertices();
            }
        }
        if ("getNode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNode();
            }
        }
        if ("getPose".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPose();
            }
        }
        if ("getRegionPose".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARFaceRegion.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARFaceRegion.class}, false);
                return typedTarget.getRegionPose((com.codename1.ar.ARFaceRegion) adaptedArgs[0]);
            }
        }
        if ("getTrackingState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrackingState();
            }
        }
        if ("isDetached".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDetached();
            }
        }
        if ("setNode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARNode.class}, false);
                typedTarget.setNode((com.codename1.ar.ARNode) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ar.ARImageAnchor typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("detach".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.detach(); return null;
            }
        }
        if ("getEstimatedPhysicalWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEstimatedPhysicalWidth();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getNode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNode();
            }
        }
        if ("getPose".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPose();
            }
        }
        if ("getReferenceImageName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReferenceImageName();
            }
        }
        if ("getTrackingState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrackingState();
            }
        }
        if ("isDetached".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDetached();
            }
        }
        if ("setNode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARNode.class}, false);
                typedTarget.setNode((com.codename1.ar.ARNode) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ar.ARAnchor typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("detach".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.detach(); return null;
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getNode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNode();
            }
        }
        if ("getPose".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPose();
            }
        }
        if ("getTrackingState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrackingState();
            }
        }
        if ("isDetached".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDetached();
            }
        }
        if ("setNode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARNode.class}, false);
                typedTarget.setNode((com.codename1.ar.ARNode) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ar.ARAnchorEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAnchor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnchor();
            }
        }
        if ("getKind".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKind();
            }
        }
        if ("getSession".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSession();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ar.ARCapabilities typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("isFaceTrackingSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFaceTrackingSupported();
            }
        }
        if ("isImageTrackingSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isImageTrackingSupported();
            }
        }
        if ("isLightEstimationSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLightEstimationSupported();
            }
        }
        if ("isPlaneDetectionSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPlaneDetectionSupported();
            }
        }
        if ("isWorldTrackingSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isWorldTrackingSupported();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ar.ARHitResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("createAnchor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createAnchor();
            }
        }
        if ("getDistance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDistance();
            }
        }
        if ("getNativeHandle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeHandle();
            }
        }
        if ("getPlane".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPlane();
            }
        }
        if ("getPose".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPose();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ar.ARLightEstimate typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAmbientIntensity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAmbientIntensity();
            }
        }
        if ("getColorCorrection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColorCorrection();
            }
        }
        if ("isValid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isValid();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ar.ARModel typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getBaseColorImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaseColorImage();
            }
        }
        if ("getColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getColor();
            }
        }
        if ("getGltfBytes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGltfBytes();
            }
        }
        if ("getMesh".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMesh();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.ar.ARNode typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addChild".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARNode.class}, false);
                typedTarget.addChild((com.codename1.ar.ARNode) adaptedArgs[0]); return null;
            }
        }
        if ("getChildAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getChildAt(toIntValue(adaptedArgs[0]));
            }
        }
        if ("getChildCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChildCount();
            }
        }
        if ("getLocalQw".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalQw();
            }
        }
        if ("getLocalQx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalQx();
            }
        }
        if ("getLocalQy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalQy();
            }
        }
        if ("getLocalQz".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalQz();
            }
        }
        if ("getLocalScale".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalScale();
            }
        }
        if ("getLocalX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalX();
            }
        }
        if ("getLocalY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalY();
            }
        }
        if ("getLocalZ".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalZ();
            }
        }
        if ("getModel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getModel();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("removeChild".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARNode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARNode.class}, false);
                typedTarget.removeChild((com.codename1.ar.ARNode) adaptedArgs[0]); return null;
            }
        }
        if ("setLocalPosition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.setLocalPosition(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("setLocalRotation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.setLocalRotation(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue());
            }
        }
        if ("setLocalScale".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.setLocalScale(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.ar.ARPlane typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCenterPose".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCenterPose();
            }
        }
        if ("getExtentX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExtentX();
            }
        }
        if ("getExtentZ".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExtentZ();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getPolygon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPolygon();
            }
        }
        if ("getTrackingState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrackingState();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.ar.ARPlaneDetection typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("includesHorizontal".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.includesHorizontal();
            }
        }
        if ("includesVertical".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.includesVertical();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.ar.ARPlaneEvent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getKind".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKind();
            }
        }
        if ("getPlane".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPlane();
            }
        }
        if ("getSession".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSession();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.ar.ARPose typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getQw".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getQw();
            }
        }
        if ("getQx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getQx();
            }
        }
        if ("getQy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getQy();
            }
        }
        if ("getQz".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getQz();
            }
        }
        if ("getTx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTx();
            }
        }
        if ("getTy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTy();
            }
        }
        if ("getTz".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTz();
            }
        }
        if ("toMatrix".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toMatrix();
            }
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class}, false);
                typedTarget.toMatrix((float[]) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("transform".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARPose.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARPose.class}, false);
                return typedTarget.transform((com.codename1.ar.ARPose) adaptedArgs[0]);
            }
        }
        if ("transformPoint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class}, false);
                typedTarget.transformPoint((float[]) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.ar.ARReferenceImage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getEncodedImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEncodedImage();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getPhysicalWidthMeters".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPhysicalWidthMeters();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.ar.ARSession typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAnchorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARAnchorListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARAnchorListener.class}, false);
                typedTarget.addAnchorListener((com.codename1.ar.ARAnchorListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPlaneListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARPlaneListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARPlaneListener.class}, false);
                typedTarget.addPlaneListener((com.codename1.ar.ARPlaneListener) adaptedArgs[0]); return null;
            }
        }
        if ("addTrackingListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARTrackingListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARTrackingListener.class}, false);
                typedTarget.addTrackingListener((com.codename1.ar.ARTrackingListener) adaptedArgs[0]); return null;
            }
        }
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("createAnchor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARPose.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARPose.class}, false);
                return typedTarget.createAnchor((com.codename1.ar.ARPose) adaptedArgs[0]);
            }
        }
        if ("createView".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.createView();
            }
        }
        if ("getAnchors".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnchors();
            }
        }
        if ("getCameraPose".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCameraPose();
            }
        }
        if ("getLightEstimate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLightEstimate();
            }
        }
        if ("getOptions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOptions();
            }
        }
        if ("getPlanes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPlanes();
            }
        }
        if ("getTrackingFailureReason".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrackingFailureReason();
            }
        }
        if ("getTrackingState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrackingState();
            }
        }
        if ("hitTest".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                return typedTarget.hitTest(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("isClosed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isClosed();
            }
        }
        if ("pause".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.pause(); return null;
            }
        }
        if ("removeAnchorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARAnchorListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARAnchorListener.class}, false);
                typedTarget.removeAnchorListener((com.codename1.ar.ARAnchorListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePlaneListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARPlaneListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARPlaneListener.class}, false);
                typedTarget.removePlaneListener((com.codename1.ar.ARPlaneListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeTrackingListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARTrackingListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARTrackingListener.class}, false);
                typedTarget.removeTrackingListener((com.codename1.ar.ARTrackingListener) adaptedArgs[0]); return null;
            }
        }
        if ("resume".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.resume(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.ar.ARSessionOptions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getPlaneDetection".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPlaneDetection();
            }
        }
        if ("getReferenceImages".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReferenceImages();
            }
        }
        if ("getTrackingMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrackingMode();
            }
        }
        if ("isLightEstimation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLightEstimation();
            }
        }
        if ("lightEstimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.lightEstimation(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("planeDetection".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARPlaneDetection.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARPlaneDetection.class}, false);
                return typedTarget.planeDetection((com.codename1.ar.ARPlaneDetection) adaptedArgs[0]);
            }
        }
        if ("referenceImages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARReferenceImage[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARReferenceImage[].class}, false);
                return typedTarget.referenceImages((com.codename1.ar.ARReferenceImage[]) adaptedArgs[0]);
            }
        }
        if ("trackingMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARTrackingMode.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARTrackingMode.class}, false);
                return typedTarget.trackingMode((com.codename1.ar.ARTrackingMode) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.ar.ARView typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("accessibilityChanged".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.accessibilityChanged(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.accessibilityChanged(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("add".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.add((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                return typedTarget.add((com.codename1.ui.Image) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.add((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Component.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.String.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, com.codename1.ui.Image.class}, false);
                return typedTarget.add((java.lang.Object) adaptedArgs[0], (com.codename1.ui.Image) adaptedArgs[1]);
            }
        }
        if ("addAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component[].class}, true);
                com.codename1.ui.Component[] varArgs = new com.codename1.ui.Component[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.ui.Component) adaptedArgs[i];
                }
                return typedTarget.addAll(varArgs);
            }
        }
        if ("addComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.addComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(toIntValue(adaptedArgs[0]), (com.codename1.ui.Component) adaptedArgs[1]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Object.class, com.codename1.ui.Component.class}, false);
                typedTarget.addComponent(toIntValue(adaptedArgs[0]), (java.lang.Object) adaptedArgs[1], (com.codename1.ui.Component) adaptedArgs[2]); return null;
            }
        }
        if ("addContextMenuListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addContextMenuListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.addFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("addLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addMouseWheelListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addMouseWheelListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addPointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.addScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addStylusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addStylusListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("animate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.animate();
            }
        }
        if ("animateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchy(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateHierarchyAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateHierarchyAndWait(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateHierarchyFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateHierarchyFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayout(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateLayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.animateLayoutAndWait(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("animateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateLayoutFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("animateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.animateUnlayout(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.Runnable) adaptedArgs[2]); return null;
            }
        }
        if ("animateUnlayoutAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.animateUnlayoutAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("announceForAccessibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.announceForAccessibility((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("applyRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.applyRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("bindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.bindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("blocksSideSwipe".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.blocksSideSwipe();
            }
        }
        if ("clearClientProperties".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clearClientProperties(); return null;
            }
        }
        if ("contains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.contains((com.codename1.ui.Component) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.contains(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("containsOrOwns".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.containsOrOwns(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateHierarchy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchy(toIntValue(adaptedArgs[0]));
            }
        }
        if ("createAnimateHierarchyFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateHierarchyFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayout(toIntValue(adaptedArgs[0]));
            }
        }
        if ("createAnimateLayoutFade".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFade(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateLayoutFadeAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.createAnimateLayoutFadeAndWait(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("createAnimateUnlayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                return typedTarget.createAnimateUnlayout(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createReplaceTransition".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                return typedTarget.createReplaceTransition((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]);
            }
        }
        if ("createStyleAnimation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false);
                return typedTarget.createStyleAnimation((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("drop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.drop((com.codename1.ui.Component) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("findDropTargetAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.findDropTargetAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("findFirstFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.findFirstFocusable();
            }
        }
        if ("flushReplace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.flushReplace(); return null;
            }
        }
        if ("forceRevalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.forceRevalidate(); return null;
            }
        }
        if ("getAbsoluteX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteX();
            }
        }
        if ("getAbsoluteY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAbsoluteY();
            }
        }
        if ("getAccessibilityNode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityNode();
            }
        }
        if ("getAccessibilityText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAccessibilityText();
            }
        }
        if ("getAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAllStyles();
            }
        }
        if ("getAnimationManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAnimationManager();
            }
        }
        if ("getBaseline".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getBaseline(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getBaselineResizeBehavior".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaselineResizeBehavior();
            }
        }
        if ("getBindablePropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyNames();
            }
        }
        if ("getBindablePropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBindablePropertyTypes();
            }
        }
        if ("getBottomGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBottomGap();
            }
        }
        if ("getBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getBoundPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getChildrenAsList".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.getChildrenAsList(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getClientProperty((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getClosestComponentTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getClosestComponentTo(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getCloudBoundProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudBoundProperty();
            }
        }
        if ("getCloudDestinationProperty".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCloudDestinationProperty();
            }
        }
        if ("getComponentAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(toIntValue(adaptedArgs[0]));
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getComponentAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getComponentCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentCount();
            }
        }
        if ("getComponentForm".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentForm();
            }
        }
        if ("getComponentIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.getComponentIndex((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("getComponentState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getComponentState();
            }
        }
        if ("getCursor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCursor();
            }
        }
        if ("getDirtyRegion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDirtyRegion();
            }
        }
        if ("getDisabledStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisabledStyle();
            }
        }
        if ("getDragTransparency".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDragTransparency();
            }
        }
        if ("getDraggedx".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedx();
            }
        }
        if ("getDraggedy".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDraggedy();
            }
        }
        if ("getEditingDelegate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEditingDelegate();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getInlineAllStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineAllStyles();
            }
        }
        if ("getInlineDisabledStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineDisabledStyles();
            }
        }
        if ("getInlinePressedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlinePressedStyles();
            }
        }
        if ("getInlineSelectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineSelectedStyles();
            }
        }
        if ("getInlineStylesTheme".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineStylesTheme();
            }
        }
        if ("getInlineUnselectedStyles".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInlineUnselectedStyles();
            }
        }
        if ("getInnerHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerHeight();
            }
        }
        if ("getInnerPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredH();
            }
        }
        if ("getInnerPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerPreferredW();
            }
        }
        if ("getInnerWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerWidth();
            }
        }
        if ("getInnerX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerX();
            }
        }
        if ("getInnerY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInnerY();
            }
        }
        if ("getLabelForComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLabelForComponent();
            }
        }
        if ("getLayout".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayout();
            }
        }
        if ("getLayoutHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutHeight();
            }
        }
        if ("getLayoutWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLayoutWidth();
            }
        }
        if ("getLeadComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadComponent();
            }
        }
        if ("getLeadParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLeadParent();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getNativeOverlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNativeOverlay();
            }
        }
        if ("getNextFocusDown".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusDown();
            }
        }
        if ("getNextFocusLeft".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusLeft();
            }
        }
        if ("getNextFocusRight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusRight();
            }
        }
        if ("getNextFocusUp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNextFocusUp();
            }
        }
        if ("getOuterHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterHeight();
            }
        }
        if ("getOuterPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredH();
            }
        }
        if ("getOuterPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterPreferredW();
            }
        }
        if ("getOuterWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterWidth();
            }
        }
        if ("getOuterX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterX();
            }
        }
        if ("getOuterY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOuterY();
            }
        }
        if ("getOwner".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOwner();
            }
        }
        if ("getParent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParent();
            }
        }
        if ("getPreferredH".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredH();
            }
        }
        if ("getPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSize();
            }
        }
        if ("getPreferredSizeStr".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredSizeStr();
            }
        }
        if ("getPreferredTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredTabIndex();
            }
        }
        if ("getPreferredW".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPreferredW();
            }
        }
        if ("getPressedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPressedStyle();
            }
        }
        if ("getPropertyNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyNames();
            }
        }
        if ("getPropertyTypeNames".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypeNames();
            }
        }
        if ("getPropertyTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPropertyTypes();
            }
        }
        if ("getPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getPropertyValue((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getResponderAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.getResponderAt(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("getSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSafeAreaRoot();
            }
        }
        if ("getSameHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameHeight();
            }
        }
        if ("getSameWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSameWidth();
            }
        }
        if ("getScrollAnimationSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollAnimationSpeed();
            }
        }
        if ("getScrollDimension".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollDimension();
            }
        }
        if ("getScrollIncrement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollIncrement();
            }
        }
        if ("getScrollOpacity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacity();
            }
        }
        if ("getScrollOpacityChangeSpeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollOpacityChangeSpeed();
            }
        }
        if ("getScrollX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollX();
            }
        }
        if ("getScrollY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollY();
            }
        }
        if ("getScrollable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getScrollable();
            }
        }
        if ("getSelectCommandText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectCommandText();
            }
        }
        if ("getSelectedRect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedRect();
            }
        }
        if ("getSelectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelectedStyle();
            }
        }
        if ("getSemantics".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSemantics();
            }
        }
        if ("getSession".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSession();
            }
        }
        if ("getSideGap".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSideGap();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("getTabIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTabIndex();
            }
        }
        if ("getTensileLength".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTensileLength();
            }
        }
        if ("getTextSelectionSupport".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextSelectionSupport();
            }
        }
        if ("getTooltip".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTooltip();
            }
        }
        if ("getUIID".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIID();
            }
        }
        if ("getUIManager".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUIManager();
            }
        }
        if ("getUnselectedStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUnselectedStyle();
            }
        }
        if ("getViewPeer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getViewPeer();
            }
        }
        if ("getVisibleBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                return typedTarget.getVisibleBounds((com.codename1.ui.geom.Rectangle) adaptedArgs[0]);
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("getX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getX();
            }
        }
        if ("getY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getY();
            }
        }
        if ("growShrink".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.growShrink(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("handlesInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.handlesInput();
            }
        }
        if ("hasFixedPreferredSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFixedPreferredSize();
            }
        }
        if ("hasFocus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasFocus();
            }
        }
        if ("invalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.invalidate(); return null;
            }
        }
        if ("isAlwaysTensile".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlwaysTensile();
            }
        }
        if ("isBlockLead".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBlockLead();
            }
        }
        if ("isCellRenderer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCellRenderer();
            }
        }
        if ("isChildOf".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Container.class}, false);
                return typedTarget.isChildOf((com.codename1.ui.Container) adaptedArgs[0]);
            }
        }
        if ("isDraggable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDraggable();
            }
        }
        if ("isDropTarget".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDropTarget();
            }
        }
        if ("isEditable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditable();
            }
        }
        if ("isEditing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEditing();
            }
        }
        if ("isEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEnabled();
            }
        }
        if ("isFlatten".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFlatten();
            }
        }
        if ("isFocusable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFocusable();
            }
        }
        if ("isGrabsPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGrabsPointerEvents();
            }
        }
        if ("isHScrollThumbGrabbed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHScrollThumbGrabbed();
            }
        }
        if ("isHScrollThumbHover".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHScrollThumbHover();
            }
        }
        if ("isHidden".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHidden();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.isHidden(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isHideInLandscape".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInLandscape();
            }
        }
        if ("isHideInPortrait".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHideInPortrait();
            }
        }
        if ("isIgnorePointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isIgnorePointerEvents();
            }
        }
        if ("isOpaque".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOpaque();
            }
        }
        if ("isOwnedBy".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                return typedTarget.isOwnedBy((com.codename1.ui.Component) adaptedArgs[0]);
            }
        }
        if ("isPinchBlocksDragAndDrop".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPinchBlocksDragAndDrop();
            }
        }
        if ("isRTL".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRTL();
            }
        }
        if ("isRippleEffect".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRippleEffect();
            }
        }
        if ("isSafeArea".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeArea();
            }
        }
        if ("isSafeAreaRoot".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSafeAreaRoot();
            }
        }
        if ("isScrollVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollVisible();
            }
        }
        if ("isScrollableX".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableX();
            }
        }
        if ("isScrollableY".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isScrollableY();
            }
        }
        if ("isSmoothScrolling".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSmoothScrolling();
            }
        }
        if ("isSnapToGrid".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSnapToGrid();
            }
        }
        if ("isSurface".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSurface();
            }
        }
        if ("isTactileTouch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTactileTouch();
            }
        }
        if ("isTensileDragEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTensileDragEnabled();
            }
        }
        if ("isTraversable".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTraversable();
            }
        }
        if ("isVScrollThumbGrabbed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVScrollThumbGrabbed();
            }
        }
        if ("isVScrollThumbHover".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVScrollThumbHover();
            }
        }
        if ("isVisible".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVisible();
            }
        }
        if ("iterator".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.iterator();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.iterator(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("keyPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyPressed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("keyReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyReleased(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("keyRepeated".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.keyRepeated(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("layoutContainer".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.layoutContainer(); return null;
            }
        }
        if ("longPointerPress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.longPointerPress(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("morph".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class, java.lang.Runnable.class}, false);
                typedTarget.morph((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], toIntValue(adaptedArgs[2]), (java.lang.Runnable) adaptedArgs[3]); return null;
            }
        }
        if ("morphAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, java.lang.Integer.class}, false);
                typedTarget.morphAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("paint".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paint((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintBackgrounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintBackgrounds((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Boolean.class}, false);
                typedTarget.paintComponent((com.codename1.ui.Graphics) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("paintComponentBackground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintComponentBackground((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintIntersectingComponentsAbove".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class}, false);
                typedTarget.paintIntersectingComponentsAbove((com.codename1.ui.Graphics) adaptedArgs[0]); return null;
            }
        }
        if ("paintLock".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.paintLock(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("paintLockRelease".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.paintLockRelease(); return null;
            }
        }
        if ("paintRippleOverlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintRippleOverlay((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("paintShadows".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Graphics.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.paintShadows((com.codename1.ui.Graphics) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        if ("pointerDragged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerDragged(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerDragged((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHover".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHover((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerHoverReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerHoverReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerPressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerPressed(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerPressed((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("pointerReleased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.pointerReleased(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, int[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, int[].class}, false);
                typedTarget.pointerReleased((int[]) adaptedArgs[0], (int[]) adaptedArgs[1]); return null;
            }
        }
        if ("putClientProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.putClientProperty((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("refreshTheme".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.refreshTheme(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.refreshTheme(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("remove".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.remove(); return null;
            }
        }
        if ("removeAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.removeAll(); return null;
            }
        }
        if ("removeComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.removeComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("removeContextMenuListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeContextMenuListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragFinishedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragFinishedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDragOverListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDragOverListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeDropListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeDropListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeFocusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.FocusListener.class}, false);
                typedTarget.removeFocusListener((com.codename1.ui.events.FocusListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeLongPressListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeLongPressListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeMouseWheelListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeMouseWheelListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerDraggedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerDraggedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerPressedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerPressedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removePointerReleasedListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removePointerReleasedListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeScrollListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ScrollListener.class}, false);
                typedTarget.removeScrollListener((com.codename1.ui.events.ScrollListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeStylusListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeStylusListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("repaint".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.repaint(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.repaint(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3])); return null;
            }
        }
        if ("replace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Runnable.class, java.lang.Integer.class}, false);
                typedTarget.replace((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], (java.lang.Runnable) adaptedArgs[3], toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("replaceAndWait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Integer.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], toIntValue(adaptedArgs[3])); return null;
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class, com.codename1.ui.Component.class, com.codename1.ui.animations.Transition.class, java.lang.Boolean.class}, false);
                typedTarget.replaceAndWait((com.codename1.ui.Component) adaptedArgs[0], (com.codename1.ui.Component) adaptedArgs[1], (com.codename1.ui.animations.Transition) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue()); return null;
            }
        }
        if ("requestFocus".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.requestFocus(); return null;
            }
        }
        if ("respondsToPointerEvents".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.respondsToPointerEvents();
            }
        }
        if ("revalidate".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidate(); return null;
            }
        }
        if ("revalidateLater".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateLater(); return null;
            }
        }
        if ("revalidateWithAnimationSafety".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.revalidateWithAnimationSafety(); return null;
            }
        }
        if ("scrollComponentToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.scrollComponentToVisible((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("scrollRectToVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, com.codename1.ui.Component.class}, false);
                typedTarget.scrollRectToVisible(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), (com.codename1.ui.Component) adaptedArgs[4]); return null;
            }
        }
        if ("setAccessibilityText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAccessibilityText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlwaysTensile".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setAlwaysTensile(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBlockLead".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setBlockLead(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setBoundPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setBoundPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setCellRenderer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setCellRenderer(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setCloudBoundProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudBoundProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setCloudDestinationProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setCloudDestinationProperty((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setComponentState".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.setComponentState((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        if ("setCursor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setCursor(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDirtyRegion".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Rectangle.class}, false);
                typedTarget.setDirtyRegion((com.codename1.ui.geom.Rectangle) adaptedArgs[0]); return null;
            }
        }
        if ("setDisabledStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setDisabledStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setDragTransparency".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Byte.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Byte.class}, false);
                typedTarget.setDragTransparency((byte) toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setDraggable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDraggable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setDropTarget".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setDropTarget(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setEditingDelegate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Editable.class}, false);
                typedTarget.setEditingDelegate((com.codename1.ui.Editable) adaptedArgs[0]); return null;
            }
        }
        if ("setEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFlatten".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFlatten(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocus".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocus(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFocusable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFocusable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setGrabsPointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setGrabsPointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHandlesInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHandlesInput(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHeight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHeight(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setHidden".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class, java.lang.Boolean.class}, false);
                typedTarget.setHidden(((Boolean) adaptedArgs[0]).booleanValue(), ((Boolean) adaptedArgs[1]).booleanValue()); return null;
            }
        }
        if ("setHideInLandscape".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInLandscape(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHideInPortrait".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setHideInPortrait(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setHorizontalScrollBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setHorizontalScrollBounds(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6]), toIntValue(adaptedArgs[7])); return null;
            }
        }
        if ("setIgnorePointerEvents".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIgnorePointerEvents(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setInlineAllStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineAllStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineDisabledStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineDisabledStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlinePressedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlinePressedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineSelectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineSelectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineStylesTheme".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.util.Resources.class}, false);
                typedTarget.setInlineStylesTheme((com.codename1.ui.util.Resources) adaptedArgs[0]); return null;
            }
        }
        if ("setInlineUnselectedStyles".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInlineUnselectedStyles((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIsScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setIsScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setLabelForComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Label.class}, false);
                typedTarget.setLabelForComponent((com.codename1.ui.Label) adaptedArgs[0]); return null;
            }
        }
        if ("setLayout".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.layouts.Layout.class}, false);
                typedTarget.setLayout((com.codename1.ui.layouts.Layout) adaptedArgs[0]); return null;
            }
        }
        if ("setLeadComponent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setLeadComponent((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusDown".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusDown((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusLeft".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusLeft((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusRight".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusRight((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setNextFocusUp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setNextFocusUp((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setOpaque".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setOpaque(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setOwner".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Component.class}, false);
                typedTarget.setOwner((com.codename1.ui.Component) adaptedArgs[0]); return null;
            }
        }
        if ("setPinchBlocksDragAndDrop".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setPinchBlocksDragAndDrop(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setPreferredH".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredH(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setPreferredSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredSizeStr".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setPreferredSizeStr((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPreferredTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredTabIndex(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPreferredW".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setPreferredW(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPressedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setPressedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setPropertyValue".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                return typedTarget.setPropertyValue((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]);
            }
        }
        if ("setPullToRefresh".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.setPullToRefresh((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("setRTL".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRTL(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setRippleEffect".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setRippleEffect(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeArea".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeArea(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSafeAreaRoot".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSafeAreaRoot(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollAnimationSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollAnimationSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollIncrement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollIncrement(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollOpacityChangeSpeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setScrollOpacityChangeSpeed(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setScrollSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setScrollSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setScrollVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableX(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setScrollableY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setScrollableY(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSelectCommandText".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSelectCommandText((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSelectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setSelectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setShouldCalcPreferredSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setShouldCalcPreferredSize(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.geom.Dimension.class}, false);
                typedTarget.setSize((com.codename1.ui.geom.Dimension) adaptedArgs[0]); return null;
            }
        }
        if ("setSmoothScrolling".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSmoothScrolling(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setSnapToGrid".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setSnapToGrid(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTabIndex".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTabIndex(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTactileTouch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTactileTouch(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileDragEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTensileDragEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTensileLength".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTensileLength(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setTooltip".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTooltip((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTraversable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setTraversable(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setUIID".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.setUIID((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("setUIManager".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.UIManager.class}, false);
                typedTarget.setUIManager((com.codename1.ui.plaf.UIManager) adaptedArgs[0]); return null;
            }
        }
        if ("setUnselectedStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.plaf.Style.class}, false);
                typedTarget.setUnselectedStyle((com.codename1.ui.plaf.Style) adaptedArgs[0]); return null;
            }
        }
        if ("setVerticalScrollBounds".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.setVerticalScrollBounds(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4]), toIntValue(adaptedArgs[5]), toIntValue(adaptedArgs[6]), toIntValue(adaptedArgs[7])); return null;
            }
        }
        if ("setVisible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setVisible(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setWidth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setWidth(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setX".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setX(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setY".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setY(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("startEditingAsync".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.startEditingAsync(); return null;
            }
        }
        if ("stopEditing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.stopEditing((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("stripMarginAndPadding".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.stripMarginAndPadding();
            }
        }
        if ("styleChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ui.plaf.Style.class}, false);
                typedTarget.styleChanged((java.lang.String) adaptedArgs[0], (com.codename1.ui.plaf.Style) adaptedArgs[1]); return null;
            }
        }
        if ("toImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toImage();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        if ("unbindProperty".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.cloud.BindTarget.class}, false);
                typedTarget.unbindProperty((java.lang.String) adaptedArgs[0], (com.codename1.cloud.BindTarget) adaptedArgs[1]); return null;
            }
        }
        if ("updateTabIndices".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.updateTabIndices(toIntValue(adaptedArgs[0]));
            }
        }
        if ("visibleBoundsContains".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.visibleBoundsContains(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.ar.ARAnchorListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("anchorChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARAnchorEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARAnchorEvent.class}, false);
                typedTarget.anchorChanged((com.codename1.ar.ARAnchorEvent) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.ar.ARPlaneListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("planeChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARPlaneEvent.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARPlaneEvent.class}, false);
                typedTarget.planeChanged((com.codename1.ar.ARPlaneEvent) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.ar.ARTrackingListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("trackingStateChanged".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ar.ARSession.class, com.codename1.ar.ARTrackingState.class, com.codename1.ar.ARTrackingFailureReason.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ar.ARSession.class, com.codename1.ar.ARTrackingState.class, com.codename1.ar.ARTrackingFailureReason.class}, false);
                typedTarget.trackingStateChanged((com.codename1.ar.ARSession) adaptedArgs[0], (com.codename1.ar.ARTrackingState) adaptedArgs[1], (com.codename1.ar.ARTrackingFailureReason) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ar.ARAnchorEvent.Kind.class) return getStaticField0(name);
        if (type == com.codename1.ar.ARCapabilities.class) return getStaticField1(name);
        if (type == com.codename1.ar.ARFaceRegion.class) return getStaticField2(name);
        if (type == com.codename1.ar.ARHitResult.Type.class) return getStaticField3(name);
        if (type == com.codename1.ar.ARLightEstimate.class) return getStaticField4(name);
        if (type == com.codename1.ar.ARPlane.Type.class) return getStaticField5(name);
        if (type == com.codename1.ar.ARPlaneDetection.class) return getStaticField6(name);
        if (type == com.codename1.ar.ARPlaneEvent.Kind.class) return getStaticField7(name);
        if (type == com.codename1.ar.ARPose.class) return getStaticField8(name);
        if (type == com.codename1.ar.ARTrackingFailureReason.class) return getStaticField9(name);
        if (type == com.codename1.ar.ARTrackingMode.class) return getStaticField10(name);
        if (type == com.codename1.ar.ARTrackingState.class) return getStaticField11(name);
        if (type == com.codename1.ar.ARView.class) return getStaticField12(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("ADDED".equals(name)) return com.codename1.ar.ARAnchorEvent.Kind.ADDED;
        if ("REMOVED".equals(name)) return com.codename1.ar.ARAnchorEvent.Kind.REMOVED;
        if ("UPDATED".equals(name)) return com.codename1.ar.ARAnchorEvent.Kind.UPDATED;
        throw unsupportedStaticField(com.codename1.ar.ARAnchorEvent.Kind.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("UNSUPPORTED".equals(name)) return com.codename1.ar.ARCapabilities.UNSUPPORTED;
        throw unsupportedStaticField(com.codename1.ar.ARCapabilities.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("FOREHEAD_LEFT".equals(name)) return com.codename1.ar.ARFaceRegion.FOREHEAD_LEFT;
        if ("FOREHEAD_RIGHT".equals(name)) return com.codename1.ar.ARFaceRegion.FOREHEAD_RIGHT;
        if ("LEFT_EYE".equals(name)) return com.codename1.ar.ARFaceRegion.LEFT_EYE;
        if ("NOSE_TIP".equals(name)) return com.codename1.ar.ARFaceRegion.NOSE_TIP;
        if ("RIGHT_EYE".equals(name)) return com.codename1.ar.ARFaceRegion.RIGHT_EYE;
        throw unsupportedStaticField(com.codename1.ar.ARFaceRegion.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("ESTIMATED_PLANE".equals(name)) return com.codename1.ar.ARHitResult.Type.ESTIMATED_PLANE;
        if ("FEATURE_POINT".equals(name)) return com.codename1.ar.ARHitResult.Type.FEATURE_POINT;
        if ("PLANE".equals(name)) return com.codename1.ar.ARHitResult.Type.PLANE;
        throw unsupportedStaticField(com.codename1.ar.ARHitResult.Type.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("INVALID".equals(name)) return com.codename1.ar.ARLightEstimate.INVALID;
        throw unsupportedStaticField(com.codename1.ar.ARLightEstimate.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("HORIZONTAL_DOWN".equals(name)) return com.codename1.ar.ARPlane.Type.HORIZONTAL_DOWN;
        if ("HORIZONTAL_UP".equals(name)) return com.codename1.ar.ARPlane.Type.HORIZONTAL_UP;
        if ("VERTICAL".equals(name)) return com.codename1.ar.ARPlane.Type.VERTICAL;
        throw unsupportedStaticField(com.codename1.ar.ARPlane.Type.class, name);
    }

    private static Object getStaticField6(String name) throws Exception {
        if ("HORIZONTAL".equals(name)) return com.codename1.ar.ARPlaneDetection.HORIZONTAL;
        if ("HORIZONTAL_AND_VERTICAL".equals(name)) return com.codename1.ar.ARPlaneDetection.HORIZONTAL_AND_VERTICAL;
        if ("NONE".equals(name)) return com.codename1.ar.ARPlaneDetection.NONE;
        if ("VERTICAL".equals(name)) return com.codename1.ar.ARPlaneDetection.VERTICAL;
        throw unsupportedStaticField(com.codename1.ar.ARPlaneDetection.class, name);
    }

    private static Object getStaticField7(String name) throws Exception {
        if ("ADDED".equals(name)) return com.codename1.ar.ARPlaneEvent.Kind.ADDED;
        if ("REMOVED".equals(name)) return com.codename1.ar.ARPlaneEvent.Kind.REMOVED;
        if ("UPDATED".equals(name)) return com.codename1.ar.ARPlaneEvent.Kind.UPDATED;
        throw unsupportedStaticField(com.codename1.ar.ARPlaneEvent.Kind.class, name);
    }

    private static Object getStaticField8(String name) throws Exception {
        if ("IDENTITY".equals(name)) return com.codename1.ar.ARPose.IDENTITY;
        throw unsupportedStaticField(com.codename1.ar.ARPose.class, name);
    }

    private static Object getStaticField9(String name) throws Exception {
        if ("EXCESSIVE_MOTION".equals(name)) return com.codename1.ar.ARTrackingFailureReason.EXCESSIVE_MOTION;
        if ("INITIALIZING".equals(name)) return com.codename1.ar.ARTrackingFailureReason.INITIALIZING;
        if ("INSUFFICIENT_FEATURES".equals(name)) return com.codename1.ar.ARTrackingFailureReason.INSUFFICIENT_FEATURES;
        if ("INSUFFICIENT_LIGHT".equals(name)) return com.codename1.ar.ARTrackingFailureReason.INSUFFICIENT_LIGHT;
        if ("NONE".equals(name)) return com.codename1.ar.ARTrackingFailureReason.NONE;
        throw unsupportedStaticField(com.codename1.ar.ARTrackingFailureReason.class, name);
    }

    private static Object getStaticField10(String name) throws Exception {
        if ("FACE".equals(name)) return com.codename1.ar.ARTrackingMode.FACE;
        if ("WORLD".equals(name)) return com.codename1.ar.ARTrackingMode.WORLD;
        throw unsupportedStaticField(com.codename1.ar.ARTrackingMode.class, name);
    }

    private static Object getStaticField11(String name) throws Exception {
        if ("LIMITED".equals(name)) return com.codename1.ar.ARTrackingState.LIMITED;
        if ("NOT_TRACKING".equals(name)) return com.codename1.ar.ARTrackingState.NOT_TRACKING;
        if ("TRACKING".equals(name)) return com.codename1.ar.ARTrackingState.TRACKING;
        throw unsupportedStaticField(com.codename1.ar.ARTrackingState.class, name);
    }

    private static Object getStaticField12(String name) throws Exception {
        if ("BASELINE".equals(name)) return com.codename1.ar.ARView.BASELINE;
        if ("BOTTOM".equals(name)) return com.codename1.ar.ARView.BOTTOM;
        if ("BRB_CENTER_OFFSET".equals(name)) return com.codename1.ar.ARView.BRB_CENTER_OFFSET;
        if ("BRB_CONSTANT_ASCENT".equals(name)) return com.codename1.ar.ARView.BRB_CONSTANT_ASCENT;
        if ("BRB_CONSTANT_DESCENT".equals(name)) return com.codename1.ar.ARView.BRB_CONSTANT_DESCENT;
        if ("BRB_OTHER".equals(name)) return com.codename1.ar.ARView.BRB_OTHER;
        if ("CENTER".equals(name)) return com.codename1.ar.ARView.CENTER;
        if ("CROSSHAIR_CURSOR".equals(name)) return com.codename1.ar.ARView.CROSSHAIR_CURSOR;
        if ("DEFAULT_CURSOR".equals(name)) return com.codename1.ar.ARView.DEFAULT_CURSOR;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_X".equals(name)) return com.codename1.ar.ARView.DRAG_REGION_IMMEDIATELY_DRAG_X;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_XY".equals(name)) return com.codename1.ar.ARView.DRAG_REGION_IMMEDIATELY_DRAG_XY;
        if ("DRAG_REGION_IMMEDIATELY_DRAG_Y".equals(name)) return com.codename1.ar.ARView.DRAG_REGION_IMMEDIATELY_DRAG_Y;
        if ("DRAG_REGION_LIKELY_DRAG_X".equals(name)) return com.codename1.ar.ARView.DRAG_REGION_LIKELY_DRAG_X;
        if ("DRAG_REGION_LIKELY_DRAG_XY".equals(name)) return com.codename1.ar.ARView.DRAG_REGION_LIKELY_DRAG_XY;
        if ("DRAG_REGION_LIKELY_DRAG_Y".equals(name)) return com.codename1.ar.ARView.DRAG_REGION_LIKELY_DRAG_Y;
        if ("DRAG_REGION_NOT_DRAGGABLE".equals(name)) return com.codename1.ar.ARView.DRAG_REGION_NOT_DRAGGABLE;
        if ("DRAG_REGION_POSSIBLE_DRAG_X".equals(name)) return com.codename1.ar.ARView.DRAG_REGION_POSSIBLE_DRAG_X;
        if ("DRAG_REGION_POSSIBLE_DRAG_XY".equals(name)) return com.codename1.ar.ARView.DRAG_REGION_POSSIBLE_DRAG_XY;
        if ("DRAG_REGION_POSSIBLE_DRAG_Y".equals(name)) return com.codename1.ar.ARView.DRAG_REGION_POSSIBLE_DRAG_Y;
        if ("E_RESIZE_CURSOR".equals(name)) return com.codename1.ar.ARView.E_RESIZE_CURSOR;
        if ("HAND_CURSOR".equals(name)) return com.codename1.ar.ARView.HAND_CURSOR;
        if ("LEFT".equals(name)) return com.codename1.ar.ARView.LEFT;
        if ("MOVE_CURSOR".equals(name)) return com.codename1.ar.ARView.MOVE_CURSOR;
        if ("NE_RESIZE_CURSOR".equals(name)) return com.codename1.ar.ARView.NE_RESIZE_CURSOR;
        if ("NW_RESIZE_CURSOR".equals(name)) return com.codename1.ar.ARView.NW_RESIZE_CURSOR;
        if ("N_RESIZE_CURSOR".equals(name)) return com.codename1.ar.ARView.N_RESIZE_CURSOR;
        if ("RIGHT".equals(name)) return com.codename1.ar.ARView.RIGHT;
        if ("SE_RESIZE_CURSOR".equals(name)) return com.codename1.ar.ARView.SE_RESIZE_CURSOR;
        if ("SW_RESIZE_CURSOR".equals(name)) return com.codename1.ar.ARView.SW_RESIZE_CURSOR;
        if ("S_RESIZE_CURSOR".equals(name)) return com.codename1.ar.ARView.S_RESIZE_CURSOR;
        if ("TEXT_CURSOR".equals(name)) return com.codename1.ar.ARView.TEXT_CURSOR;
        if ("TOP".equals(name)) return com.codename1.ar.ARView.TOP;
        if ("WAIT_CURSOR".equals(name)) return com.codename1.ar.ARView.WAIT_CURSOR;
        if ("W_RESIZE_CURSOR".equals(name)) return com.codename1.ar.ARView.W_RESIZE_CURSOR;
        throw unsupportedStaticField(com.codename1.ar.ARView.class, name);
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
        if (type == com.codename1.printing.PrintResultListener.class) {
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
        if (type == com.codename1.printing.PrintResultListener.class) {
            return new com.codename1.printing.PrintResultListener() {
                public void onResult(com.codename1.printing.PrintResult arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
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
