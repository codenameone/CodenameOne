package com.codenameone.playground;

import bsh.cn1.CN1LambdaSupport;

public class PlaygroundLambdaBridge {
    public Object lambda(Object[] parameterNames, String bodySource) {
        if (parameterNames == null || parameterNames.length == 0) {
            return CN1LambdaSupport.lambda(new String[0], bodySource);
        }
        String[] names = new String[parameterNames.length];
        for (int i = 0; i < parameterNames.length; i++) {
            names[i] = parameterNames[i] == null ? "" : String.valueOf(parameterNames[i]);
        }
        return CN1LambdaSupport.lambda(names, bodySource);
    }

    public Object lambda(String[] parameterNames, String bodySource) {
        if (parameterNames == null || parameterNames.length == 0) {
            return CN1LambdaSupport.lambda(new String[0], bodySource);
        }
        return CN1LambdaSupport.lambda(parameterNames, bodySource);
    }
}
