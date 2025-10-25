#if 0
// tag::nativeCallbackIncludes[]
#include "com_mycompany_NativeCallback.h"
#include "CodenameOne_GLViewController.h"
// end::nativeCallbackIncludes[]

// tag::nativeCallbackInvoke[]
com_mycompany_NativeCallback_callback__(CN1_THREAD_STATE_PASS_SINGLE_ARG);
// end::nativeCallbackInvoke[]

// tag::nativeCallbackInvokeInt[]
com_mycompany_NativeCallback_callback___int(CN1_THREAD_GET_STATE_PASS_ARG intValue);
// end::nativeCallbackInvokeInt[]

// tag::nativeCallbackInvokeString[]
com_mycompany_NativeCallback_callback___java_lang_String(CN1_THREAD_GET_STATE_PASS_ARG fromNSString(CN1_THREAD_GET_STATE_PASS_ARG nsStringValue));
// end::nativeCallbackInvokeString[]

// tag::nativeCallbackInvokeReturn[]
com_mycompany_NativeCallback_callback___int_R_int(intValue);
// end::nativeCallbackInvokeReturn[]
#endif
