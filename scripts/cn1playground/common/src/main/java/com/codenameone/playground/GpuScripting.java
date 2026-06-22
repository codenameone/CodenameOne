package com.codenameone.playground;

import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Renderer;

import java.util.function.Consumer;

/// Bridges the multi-method {@link com.codename1.gpu.Renderer} interface to the
/// single-method lambdas the Playground's BeanShell runner can produce.
///
/// On the JavaScript port the runner cannot instantiate a user-defined class or
/// anonymous interface implementation (ParparVM is ahead-of-time compiled, so
/// BeanShell's runtime class/`Proxy` generation has nothing to bind to). Single
/// abstract method lambdas DO work -- the runner rewrites them into
/// `CN1LambdaSupport.LambdaValue`s that implement the common functional
/// interfaces (`Runnable`, `Consumer`, ...). This compiled adapter lets a script
/// drive a GPU `RenderView` with two such lambdas:
///
/// ```java
/// RenderView view = new RenderView(GpuScripting.renderer(
///     device -> { /* onInit: build meshes/materials */ },
///     device -> { /* onFrame: clear, set camera, draw */ }));
/// ```
///
/// `onResize` defaults to setting the full viewport; `onDispose` is a no-op.
public final class GpuScripting {
    private GpuScripting() {
    }

    /// Builds a {@link Renderer} that forwards `onInit` / `onFrame` to the given
    /// lambdas (each receives the {@link GraphicsDevice}). Either may be null.
    public static Renderer renderer(final Consumer onInit, final Consumer onFrame) {
        return new Renderer() {
            @Override
            public void onInit(GraphicsDevice device) {
                if (onInit != null) {
                    onInit.accept(device);
                }
            }

            @Override
            public void onResize(GraphicsDevice device, int width, int height) {
                device.setViewport(0, 0, width, height);
            }

            @Override
            public void onFrame(GraphicsDevice device) {
                if (onFrame != null) {
                    onFrame.accept(device);
                }
            }

            @Override
            public void onDispose(GraphicsDevice device) {
            }
        };
    }
}
