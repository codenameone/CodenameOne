package com.codenameone.playground;

import bsh.EvalError;
import bsh.cn1.CN1LambdaSupport;
import com.codename1.io.NetworkEvent;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.OnComplete;

public class PlaygroundListenerBridge {
    public ActionListener actionListener(Object lambdaValue) {
        final CN1LambdaSupport.LambdaValue lambda = requireLambda(lambdaValue);
        return new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                invoke(lambda, evt);
            }
        };
    }

    public ActionListener networkListener(Object lambdaValue) {
        final CN1LambdaSupport.LambdaValue lambda = requireLambda(lambdaValue);
        return new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                invoke(lambda, (NetworkEvent) evt);
            }
        };
    }

    public Runnable runnable(Object lambdaValue) {
        final CN1LambdaSupport.LambdaValue lambda = requireLambda(lambdaValue);
        return new Runnable() {
            public void run() {
                invoke(lambda);
            }
        };
    }

    public OnComplete onComplete(Object lambdaValue) {
        final CN1LambdaSupport.LambdaValue lambda = requireLambda(lambdaValue);
        return new OnComplete() {
            public void completed(Object value) {
                invoke(lambda, value);
            }
        };
    }

    private CN1LambdaSupport.LambdaValue requireLambda(Object lambdaValue) {
        if (lambdaValue instanceof CN1LambdaSupport.LambdaValue) {
            return (CN1LambdaSupport.LambdaValue) lambdaValue;
        }
        throw new IllegalArgumentException("Unsupported playground callback value: " + lambdaValue);
    }

    private void invoke(CN1LambdaSupport.LambdaValue lambda, Object... args) {
        try {
            lambda.invoke(args);
        } catch (EvalError ex) {
            throw new RuntimeException(ex);
        }
    }
}
