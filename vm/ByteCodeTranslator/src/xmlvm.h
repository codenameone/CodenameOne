#ifndef __XMLVM_HH__
#define __XMLVM_HH__

/*
 * Compatibility header for XMLVM allowing us to port existing code
 */
#include "cn1_globals.h"

#define NEW_CODENAME_ONE_VM

extern JAVA_OBJECT xmlvm_create_java_string(CODENAME_ONE_THREAD_STATE, const char *chr);

//#define CN1_USE_ARC

extern JAVA_OBJECT com_codename1_ui_Display_getInstance__();
typedef struct obj__java_lang_String java_lang_String;

#endif //__XMLVM_HH__