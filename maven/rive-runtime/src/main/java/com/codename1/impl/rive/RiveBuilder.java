/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import com.codename1.util.Base64;

import java.util.ArrayList;

/// Assembles the objects of a `.riv` file, in the order the file holds
/// them. The build translates each `.riv` into a class that replays the
/// file's object stream through a builder: it creates every object, sets its
/// properties and passes it on, so nothing is parsed on the device and only
/// the classes a file uses are referenced.
///
/// The routing is rive-runtime's importer stack, flattened: every object
/// belongs to the most recent object of the kind that owns it.
public final class RiveBuilder {
    private final RiveFileContext context;
    private final ArrayList<Artboard> artboards = new ArrayList<Artboard>();
    private Core current;
    private Artboard artboard;
    private LinearAnimation animation;
    private KeyedObject keyedObject;
    private KeyedProperty keyedProperty;
    private StateMachine stateMachine;
    private FileAsset asset;
    /// The latest object that is not a data bind: what a data bind binds.
    private Core lastBindable;
    private ViewModel viewModel;
    private ViewModelInstance viewModelInstance;
    private ViewModelInstanceList viewModelList;
    private DataEnum dataEnum;
    private DataConverter converter;
    /// Whether a null object inside the current artboard is swallowed by an
    /// animation or state machine importer instead of taking an artboard slot.
    private boolean nullsSwallowed;

    /// A builder whose artboards share the assets of `context`.
    public RiveBuilder(RiveFileContext context) {
        this.context = context;
    }

    /// Starts `object`; the property calls that follow apply to it.
    public void begin(Core object) {
        current = object;
    }

    /// Sets a uint property of the current object.
    public void u(int key, int value) {
        current.deserializeUint(key, value);
    }

    /// Sets a float property of the current object.
    public void d(int key, float value) {
        current.deserializeDouble(key, value);
    }

    /// Sets a color property of the current object.
    public void c(int key, int value) {
        current.deserializeColor(key, value);
    }

    /// Sets a boolean property of the current object.
    public void b(int key, boolean value) {
        current.deserializeBool(key, value);
    }

    /// Sets a string property of the current object.
    public void s(int key, String value) {
        current.deserializeString(key, value);
    }

    /// Sets a bytes property of the current object, from base64 chunks.
    public void bytes(int key, String[] base64) {
        current.deserializeBytes(key, decode(base64));
    }

    /// Adds the current object to the file.
    public void end() {
        Core o = current;
        current = null;
        add(o);
    }

    /// An object of a type the runtime skips: it still takes its slot.
    public void skip() {
        if (artboard != null && !nullsSwallowed) {
            artboard.addObject(null);
        }
    }

    /// Initializes and answers the artboards built so far.
    public ArrayList<Artboard> finish() {
        for (int i = 0; i < artboards.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            Artboard a = artboards.get(i);
            a.setFile(context);
            a.initialize();
            ViewModelInstance vmi = context.createDefaultInstance(a);
            if (vmi != null) {
                a.bindViewModelInstance(vmi);
            }
        }
        return artboards;
    }

    private static byte[] decode(String[] chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.length; i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            sb.append(chunks[i]);
        }
        int n = sb.length();
        byte[] ascii = new byte[n];
        for (int i = 0; i < n; i++) {
            ascii[i] = (byte) sb.charAt(i);
        }
        return Base64.decode(ascii);
    }

    private void add(Core object) {
        if (object instanceof DataBind) {
            addDataBind((DataBind) object);
            return;
        }
        lastBindable = object;
        if (object instanceof Artboard) {
            artboard = (Artboard) object;
            artboards.add(artboard);
            artboard.addObject(artboard);
            animation = null;
            keyedObject = null;
            keyedProperty = null;
            stateMachine = null;
            nullsSwallowed = false;
            return;
        }
        if (object instanceof FileAsset) {
            asset = (FileAsset) object;
            context.addAsset(asset);
            return;
        }
        if (object instanceof FileAssetContents) {
            if (asset != null) {
                asset.decode(((FileAssetContents) object).getBytes());
            }
            return;
        }
        if (artboard == null) {
            addFileObject(object);
            return;
        }
        // The objects whose claimsArtboardSlot() answers true upstream: they
        // take the next artboard index, which ids in the file refer to.
        // Scripted inputs claim one conditionally; scripting is not
        // supported, so they arrive as null objects and take one like any
        // other unknown object.
        if (object instanceof Component || object instanceof KeyFrameInterpolator
                || object instanceof UserInput || object instanceof ScrollPhysics) {
            artboard.addObject(object);
            return;
        }
        if (object instanceof LinearAnimation) {
            animation = (LinearAnimation) object;
            artboard.addAnimation(animation);
            keyedObject = null;
            keyedProperty = null;
            return;
        }
        if (object instanceof KeyedObject) {
            keyedObject = (KeyedObject) object;
            keyedProperty = null;
            if (animation != null) {
                animation.addKeyedObject(keyedObject);
            }
            return;
        }
        if (object instanceof KeyedProperty) {
            keyedProperty = (KeyedProperty) object;
            nullsSwallowed = true;
            if (keyedObject != null) {
                keyedObject.addKeyedProperty(keyedProperty);
            }
            return;
        }
        if (object instanceof KeyFrame) {
            if (keyedProperty != null && animation != null) {
                KeyFrame kf = (KeyFrame) object;
                kf.computeSeconds(animation.getFps());
                keyedProperty.addKeyFrame(kf);
            }
            return;
        }
        if (object instanceof StateMachine) {
            stateMachine = (StateMachine) object;
            artboard.addStateMachine(stateMachine);
            nullsSwallowed = true;
            return;
        }
        if (stateMachine != null) {
            stateMachine.importObject(object);
        }
    }

    /// Routes a data bind as upstream's `DataBind::import` does: to the
    /// converter, formula token or template argument it targets, to the state
    /// machine for bindable properties and transitions, else to the artboard.
    private void addDataBind(DataBind db) {
        Core t = lastBindable;
        db.setTarget(t);
        db.setFile(context);
        if (t instanceof DataConverter) {
            ((DataConverter) t).addDataBind(db);
        } else if (t instanceof FormulaToken) {
            ((FormulaToken) t).addDataBind(db);
        } else if (t instanceof StringTemplateArgument) {
            ((StringTemplateArgument) t).addDataBind(db);
        } else if ((t instanceof BindableProperty || t instanceof StateTransition) && stateMachine != null) {
            stateMachine.addDataBind(db);
        } else if (t != null && artboard != null) {
            // A bind on a keyframe writes the keyframe itself. Upstream clones
            // it onto a holder per animation instance
            // (LinearAnimationInstance::keyFrameValueHolder); here every
            // artboard instance is built with its own keyframes, so only two
            // instances of one animation on one artboard share the value.
            artboard.addDataBind(db);
        }
    }

    /// The objects before the first artboard: view models, their instances,
    /// enums, converters and interpolators belong to the whole file
    /// (upstream's BackboardImporter and the importers it stacks).
    private void addFileObject(Core object) {
        if (object instanceof ViewModel) {
            viewModel = (ViewModel) object;
            context.addViewModel(viewModel);
        } else if (object instanceof ViewModelProperty) {
            if (viewModel != null) {
                viewModel.addProperty((ViewModelProperty) object);
            }
        } else if (object instanceof DataEnum) {
            dataEnum = (DataEnum) object;
            context.addEnum(dataEnum);
        } else if (object instanceof DataEnumValue) {
            if (dataEnum != null) {
                dataEnum.addValue((DataEnumValue) object);
            }
        } else if (object instanceof ViewModelInstance) {
            viewModelInstance = (ViewModelInstance) object;
            viewModelList = null;
            ViewModel vm = context.getViewModel(viewModelInstance.getViewModelId());
            viewModelInstance.setViewModel(vm);
            if (vm != null) {
                vm.addInstance(viewModelInstance);
            }
        } else if (object instanceof ViewModelInstanceValue) {
            if (viewModelInstance != null) {
                viewModelInstance.addValue((ViewModelInstanceValue) object);
            }
            viewModelList = object instanceof ViewModelInstanceList ? (ViewModelInstanceList) object : null;
        } else if (object instanceof ViewModelInstanceListItem) {
            if (viewModelList != null) {
                viewModelList.addItem((ViewModelInstanceListItem) object);
            }
        } else if (object instanceof DataConverter) {
            converter = (DataConverter) object;
            context.addConverter(converter);
        } else if (object instanceof DataConverterGroupItem) {
            if (converter instanceof DataConverterGroup) {
                ((DataConverterGroup) converter).addItem((DataConverterGroupItem) object);
            }
        } else if (object instanceof FormulaToken) {
            if (converter instanceof DataConverterFormula) {
                ((DataConverterFormula) converter).addToken((FormulaToken) object);
            }
        } else if (object instanceof StringTemplateArgument) {
            if (converter instanceof DataConverterStringTemplate) {
                ((DataConverterStringTemplate) converter).addArgument((StringTemplateArgument) object);
            }
        } else if (object instanceof KeyFrameInterpolator) {
            context.addInterpolator(object);
        }
    }
}
