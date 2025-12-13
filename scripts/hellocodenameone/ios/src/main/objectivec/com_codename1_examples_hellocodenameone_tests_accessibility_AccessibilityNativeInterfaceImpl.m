#import "CodenameOne_GLViewController.h"
#include "com_codename1_examples_hellocodenameone_tests_accessibility_AccessibilityNativeInterface.h"

extern NSString* CN1LastAccessibilityAnnouncement;

JAVA_OBJECT Java_com_codename1_examples_hellocodenameone_tests_accessibility_AccessibilityNativeInterfaceImpl_getLastAccessibilityAnnouncement(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    if (CN1LastAccessibilityAnnouncement == nil) {
        return JAVA_NULL;
    }
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG CN1LastAccessibilityAnnouncement);
}

JAVA_BOOLEAN Java_com_codename1_examples_hellocodenameone_tests_accessibility_AccessibilityNativeInterfaceImpl_isSupported(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_TRUE;
}
