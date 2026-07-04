#ifndef __CN1GLOBALS__
#define __CN1GLOBALS__

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include "cn1_class_method_index.h"
#ifdef _WIN32
#include "cn1_win_compat.h"
#else
#include <pthread.h>
#endif
#include <setjmp.h>
#include <math.h>
#include <stdatomic.h>
#include <stdint.h>

// PHASE 3b DEFAULT ON: conservative native-stack GC as a real root source, paired
// with object/instance frameless codegen (BytecodeMethod cn1.frameless.objects/
// .instance, also default on). Validated bit-identical + GC-safe + MtStress
// deterministic on arm64 macOS (the dev + iOS arch). Disable with
// -DCN1_DISABLE_CONSERVATIVE_GC_ROOTS (and run the translator with
// -Dcn1.frameless.objects=false -Dcn1.frameless.instance=false) to revert to the
// precise threadObjectStack GC.
#ifndef CN1_DISABLE_CONSERVATIVE_GC_ROOTS
#define CN1_CONSERVATIVE_GC_ROOTS
#endif

#ifdef CN1_CONSERVATIVE_GC_ROOTS
// PHASE 3b: conservative native-stack scanning as a REAL GC root source. Needs
// signal-based universal thread stopping (sig_atomic_t / sigaction / ucontext).
#include <signal.h>
#if !defined(_WIN32)
// macOS gates the ucontext routines behind _XOPEN_SOURCE; define it locally (only
// affects which symbols are exposed, never computation) before pulling the header.
#ifndef _XOPEN_SOURCE
#define _XOPEN_SOURCE 700
#endif
#include <ucontext.h>
#endif
#endif

//#define DEBUG_GC_ALLOCATIONS

#define NUMBER_OF_SUPPORTED_THREADS 1024
#define CN1_FINALIZER_QUEUE_SIZE 65536

//#define CN1_INCLUDE_NPE_CHECKS
#define CN1_INCLUDE_ARRAY_BOUND_CHECKS

// Uncommented by the translator (driven by the cn1.onDeviceDebug system
// property) when an on-device-debug build is requested. Enables per-frame
// locals-address tables, the cn1DebuggerActive hot-path check inside
// __CN1_DEBUG_INFO, and the proxy listener thread. Release builds leave
// this off and pay no overhead.
//#define CN1_ON_DEVICE_DEBUG

#ifdef DEBUG_GC_ALLOCATIONS
#define DEBUG_GC_VARIABLES int line; int className;
#define DEBUG_GC_INIT 0, 0,
#else
#define DEBUG_GC_VARIABLES
#define DEBUG_GC_INIT 
#endif

/**
 * header file containing global CN1 constants and structs
 */


typedef void               JAVA_VOID;
typedef int                JAVA_BOOLEAN;
typedef int                JAVA_CHAR;
typedef int                JAVA_BYTE;
typedef int                JAVA_SHORT;
typedef int                JAVA_INT;
typedef long long          JAVA_LONG;
typedef float              JAVA_FLOAT;
typedef double             JAVA_DOUBLE;

/* MUST be signed char, not plain char: Java bytes are signed, but bare char is
 * UNSIGNED in the aarch64/arm Linux ABI (it is signed on x86/x64 and on all
 * Apple targets, which is why this never bit the iOS builds). On the Linux
 * arm64 port the unsigned reads broke every negative-byte round-trip -- seen as
 * SimdApiTest's saturating byte add and the allocaByteFilled readback failing
 * deterministically on that leg only. */
typedef signed char       JAVA_ARRAY_BYTE;
typedef char              JAVA_ARRAY_BOOLEAN;
typedef unsigned short    JAVA_ARRAY_CHAR;
typedef short             JAVA_ARRAY_SHORT;
typedef int               JAVA_ARRAY_INT;
typedef long long         JAVA_ARRAY_LONG;
typedef float             JAVA_ARRAY_FLOAT;
typedef double            JAVA_ARRAY_DOUBLE;

typedef struct JavaArrayPrototype*               JAVA_ARRAY;
typedef struct JavaObjectPrototype*              JAVA_OBJECT;

typedef JAVA_OBJECT       JAVA_ARRAY_OBJECT;

#define cn1_array_1_id_JAVA_BOOLEAN (cn1_array_start_offset + 1)
#define cn1_array_2_id_JAVA_BOOLEAN (cn1_array_start_offset + 2)
#define cn1_array_3_id_JAVA_BOOLEAN (cn1_array_start_offset + 3)

#define cn1_array_1_id_JAVA_CHAR (cn1_array_start_offset + 5)
#define cn1_array_2_id_JAVA_CHAR (cn1_array_start_offset + 6)
#define cn1_array_3_id_JAVA_CHAR (cn1_array_start_offset + 7)

#define cn1_array_1_id_JAVA_BYTE (cn1_array_start_offset + 9)
#define cn1_array_2_id_JAVA_BYTE (cn1_array_start_offset + 10)
#define cn1_array_3_id_JAVA_BYTE (cn1_array_start_offset + 11)

#define cn1_array_1_id_JAVA_SHORT (cn1_array_start_offset + 13)
#define cn1_array_2_id_JAVA_SHORT (cn1_array_start_offset + 14)
#define cn1_array_3_id_JAVA_SHORT (cn1_array_start_offset + 15)

#define cn1_array_1_id_JAVA_INT (cn1_array_start_offset + 17)
#define cn1_array_2_id_JAVA_INT (cn1_array_start_offset + 18)
#define cn1_array_3_id_JAVA_INT (cn1_array_start_offset + 19)

#define cn1_array_1_id_JAVA_LONG (cn1_array_start_offset + 21)
#define cn1_array_2_id_JAVA_LONG (cn1_array_start_offset + 22)
#define cn1_array_3_id_JAVA_LONG (cn1_array_start_offset + 23)

#define cn1_array_1_id_JAVA_FLOAT (cn1_array_start_offset + 25)
#define cn1_array_2_id_JAVA_FLOAT (cn1_array_start_offset + 26)
#define cn1_array_3_id_JAVA_FLOAT (cn1_array_start_offset + 27)

#define cn1_array_1_id_JAVA_DOUBLE (cn1_array_start_offset + 29)
#define cn1_array_2_id_JAVA_DOUBLE (cn1_array_start_offset + 30)
#define cn1_array_3_id_JAVA_DOUBLE (cn1_array_start_offset + 31)

struct CN1ThreadData {
    pthread_mutex_t __codenameOneMutex;
    pthread_cond_t __codenameOneCondition;
    JAVA_LONG ownerThread;
    int counter;
};

struct clazz {
    DEBUG_GC_VARIABLES
    // these first  fields aren't really used but they allow us to treat a clazz as an object
    struct clazz *__codenameOneParentClsReference;
    int __codenameOneGcMark;
    int __heapPosition;

    void* finalizerFunction;
    void* releaseFieldsFunction;
    void* markFunction;
    
    JAVA_BOOLEAN initialized;
    int classId;
    const char* clsName;
    const JAVA_BOOLEAN isArray;
    
    // array type dimensions
    int dimensions;
    
    // array internal type
    struct clazz* arrayType;  // <---- The component type for an array class. 0 for scalars.
    JAVA_BOOLEAN primitiveType;
    
    const struct clazz* baseClass;
    const struct clazz** baseInterfaces;
    const int baseInterfaceCount;
    
    void* newInstanceFp;
    
    // virtual method table lookup
    void** vtable;
    
    void* enumValueOfFp;
    JAVA_BOOLEAN isSynthetic;
    JAVA_BOOLEAN isInterface;
    JAVA_BOOLEAN isAnonymous;
    JAVA_BOOLEAN isAnnotation;
    
    struct clazz* arrayClass;  // <----- The array type for a class.  if clazz=Object, then class->arrayClass=Object[]
};

#define EMPTY_INTERFACES ((const struct clazz**)0)

struct JavaObjectPrototype {
    DEBUG_GC_VARIABLES
    struct clazz *__codenameOneParentClsReference;
    int __codenameOneGcMark;
    int __heapPosition;
};

struct JavaArrayPrototype {
    DEBUG_GC_VARIABLES
    struct clazz *__codenameOneParentClsReference;
    int __codenameOneGcMark;
    int __heapPosition;
    int length;
    int dimensions;
    int primitiveSize;
    void* data;
};

typedef union {
    JAVA_OBJECT  o;
    JAVA_INT     i;
    JAVA_FLOAT   f;
    JAVA_DOUBLE  d;
    JAVA_LONG    l;
} elementUnion;

#define CODENAME_ONE_ASSERT(assertion) assert(assertion)

typedef enum {
    CN1_TYPE_INVALID, CN1_TYPE_OBJECT, CN1_TYPE_INT, CN1_TYPE_FLOAT, CN1_TYPE_DOUBLE, CN1_TYPE_LONG, CN1_TYPE_PRIMITIVE
} javaTypes;

// type must be first so memsetting will first reset the type then the data preventing the GC
// from mistakingly detecting an object
struct elementStruct {
    javaTypes type;
    elementUnion data;
};


typedef struct clazz*       JAVA_CLASS;

#define JAVA_NULL ((JAVA_OBJECT) 0)

#define JAVA_FALSE ((JAVA_BOOLEAN) 0)
#define JAVA_TRUE ((JAVA_BOOLEAN) 1)


#define BC_ILOAD(local) { \
    (*SP).type = CN1_TYPE_INT; \
    (*SP).data.i = ilocals_##local##_; \
    SP++; \
}

#define BC_LLOAD(local) { \
    (*SP).type = CN1_TYPE_LONG; \
    (*SP).data.l = llocals_##local##_; \
    SP++; \
}

#define BC_FLOAD(local) { \
    (*SP).type = CN1_TYPE_FLOAT; \
    (*SP).data.f = flocals_##local##_; \
    SP++; \
}

#define BC_DLOAD(local) { \
    (*SP).type = CN1_TYPE_DOUBLE; \
    (*SP).data.d = dlocals_##local##_; \
    SP++; \
}

#define BC_ALOAD(local) { \
    (*SP).type = CN1_TYPE_INVALID; \
    (*SP).data.o = locals[local].data.o; \
    (*SP).type = CN1_TYPE_OBJECT; \
    SP++; \
}


#define BC_ISTORE(local) { SP--; \
    ilocals_##local##_ = (*SP).data.i; \
    }

#define BC_LSTORE(local) { SP--; \
    llocals_##local##_ = (*SP).data.l; \
    }

#define BC_FSTORE(local) { SP--; \
    flocals_##local##_ = (*SP).data.f; \
    }

#define BC_DSTORE(local) { SP--; \
    dlocals_##local##_ = (*SP).data.d; \
    }

#define BC_ASTORE(local) { SP--; \
    locals[local].type = CN1_TYPE_INVALID; \
    locals[local].data.o = (*SP).data.o; \
    locals[local].type = CN1_TYPE_OBJECT; \
    }

// todo map instanceof and throw typecast exception
#define BC_CHECKCAST(type)

#define BC_SWAP() swapStack(SP)


#define POP_INT() (*pop(&SP)).data.i
#define POP_OBJ() (*pop(&SP)).data.o
#define POP_OBJ_NO_RELEASE() (*pop(&SP)).data.o
#define POP_LONG() (*pop(&SP)).data.l
#define POP_DOUBLE() (*pop(&SP)).data.d
#define POP_FLOAT() (*pop(&SP)).data.f

#define PEEK_INT(offset) SP[-offset].data.i
#define PEEK_OBJ(offset) SP[-offset].data.o
#define PEEK_LONG(offset) SP[-offset].data.l
#define PEEK_DOUBLE(offset) SP[-offset].data.d
#define PEEK_FLOAT(offset) SP[-offset].data.f

#define POP_MANY(offset) popMany(threadStateData, offset, &SP)

#define BC_IADD() { \
    SP--; \
    SP[-1].data.i = SP[-1].data.i + (*SP).data.i; \
}

#define BC_LADD() { \
    SP--; \
    SP[-1].data.l = SP[-1].data.l + (*SP).data.l; \
}

#define BC_FADD() { \
    SP--; \
    SP[-1].data.f = SP[-1].data.f + (*SP).data.f; \
}

#define BC_DADD() { \
    SP--; \
    SP[-1].data.d = SP[-1].data.d + (*SP).data.d; \
}

#define BC_IMUL() { \
    SP--; \
    SP[-1].data.i = SP[-1].data.i * (*SP).data.i; \
}

#define BC_LMUL() { \
    SP--; \
    SP[-1].data.l = SP[-1].data.l * (*SP).data.l; \
}

#define BC_FMUL() { \
    SP--; \
    SP[-1].data.f = SP[-1].data.f * (*SP).data.f; \
}

#define BC_DMUL() { \
    SP--; \
    SP[-1].data.d = SP[-1].data.d * (*SP).data.d; \
}

#define BC_INEG() SP[-1].data.i *= -1

#define BC_LNEG() SP[-1].data.l *= -1

#define BC_FNEG() SP[-1].data.f *= -1

#define BC_DNEG() SP[-1].data.d *= -1

#define BC_IAND() { \
    SP--; \
    SP[-1].data.i = SP[-1].data.i & (*SP).data.i; \
}

#define BC_LAND() { \
    SP--; \
    SP[-1].data.l = SP[-1].data.l & (*SP).data.l; \
}

#define BC_IOR() { \
    SP--; \
    SP[-1].data.i = SP[-1].data.i | (*SP).data.i; \
}

#define BC_LOR() { \
    SP--; \
    SP[-1].data.l = SP[-1].data.l | (*SP).data.l; \
}

#define BC_IXOR() { \
    SP--; \
    SP[-1].data.i = SP[-1].data.i ^ (*SP).data.i; \
}

#define BC_LXOR() { \
    SP--; \
    SP[-1].data.l = SP[-1].data.l ^ (*SP).data.l; \
}

// Conversion macros must rewrite the runtime type tag too. BC_DUP2_X1 /
// BC_DUP2_X2 / BC_DUP_X2 dispatch via IS_DOUBLE_WORD on the tag, so a stale
// tag corrupts the stack on chained assignments (issue #3108).
#define BC_I2L() do { SP[-1].data.l = SP[-1].data.i; SP[-1].type = CN1_TYPE_LONG; } while(0)

#define BC_L2I() do { SP[-1].data.i = (JAVA_INT)SP[-1].data.l; SP[-1].type = CN1_TYPE_INT; } while(0)

#define BC_L2F() do { SP[-1].data.f = (JAVA_FLOAT)SP[-1].data.l; SP[-1].type = CN1_TYPE_FLOAT; } while(0)

#define BC_L2D() do { SP[-1].data.d = (JAVA_DOUBLE)SP[-1].data.l; SP[-1].type = CN1_TYPE_DOUBLE; } while(0)

#define BC_I2F() do { SP[-1].data.f = (JAVA_FLOAT)SP[-1].data.i; SP[-1].type = CN1_TYPE_FLOAT; } while(0)

#define BC_F2I() do { SP[-1].data.i = (JAVA_INT)SP[-1].data.f; SP[-1].type = CN1_TYPE_INT; } while(0)

#define BC_F2L() do { SP[-1].data.l = (JAVA_LONG)SP[-1].data.f; SP[-1].type = CN1_TYPE_LONG; } while(0)

#define BC_F2D() do { SP[-1].data.d = SP[-1].data.f; SP[-1].type = CN1_TYPE_DOUBLE; } while(0)

#define BC_D2I() do { SP[-1].data.i = (JAVA_INT)SP[-1].data.d; SP[-1].type = CN1_TYPE_INT; } while(0)

#define BC_D2L() do { SP[-1].data.l = (JAVA_LONG)SP[-1].data.d; SP[-1].type = CN1_TYPE_LONG; } while(0)

#define BC_I2D() do { SP[-1].data.d = SP[-1].data.i; SP[-1].type = CN1_TYPE_DOUBLE; } while(0)

#define BC_D2F() do { SP[-1].data.f = (JAVA_FLOAT)SP[-1].data.d; SP[-1].type = CN1_TYPE_FLOAT; } while(0)

#ifdef CN1_INCLUDE_NPE_CHECKS
#define BC_ARRAYLENGTH() { \
    if(SP[-1].data.o == JAVA_NULL) { \
        throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)); \
    }; \
    SP[-1].type = CN1_TYPE_INT; \
    SP[-1].data.i = (*((JAVA_ARRAY)SP[-1].data.o)).length; \
}
#else
#define BC_ARRAYLENGTH() { \
    SP[-1].type = CN1_TYPE_INT; \
    SP[-1].data.i = (*((JAVA_ARRAY)SP[-1].data.o)).length; \
}
#endif

#define BC_IF_ICMPEQ() SP-=2; if((*SP).data.i == SP[1].data.i)

#define BC_IF_ICMPNE() SP-=2; if((*SP).data.i != SP[1].data.i)

#define BC_IF_ICMPLT() SP-=2; if((*SP).data.i < SP[1].data.i)

#define BC_IF_ICMPGE() SP-=2; if((*SP).data.i >= SP[1].data.i)

#define BC_IF_ICMPGT() SP-=2; if((*SP).data.i > SP[1].data.i)

#define BC_IF_ICMPLE() SP-=2; if((*SP).data.i <= SP[1].data.i)

#define BC_IF_ACMPEQ() SP-=2; if((*SP).data.o == SP[1].data.o)

#define BC_IF_ACMPNE() SP-=2; if((*SP).data.o != SP[1].data.o)

//#define POP_TYPE(type) (*((type*)POP_OBJ()))

// we assign the value to trigger the expression in the macro
// then set the type to invalid first so we don't get a race condition where the value is
// incomplete and the GC goes crazy
#define PUSH_POINTER(value) { JAVA_OBJECT ppX = value; (*SP).type = CN1_TYPE_INVALID; \
    (*SP).data.o = ppX; (*SP).type = CN1_TYPE_OBJECT; \
    SP++; }

#define PUSH_OBJ(value)  { JAVA_OBJECT ppX = value; (*SP).type = CN1_TYPE_INVALID; \
    (*SP).data.o = ppX; (*SP).type = CN1_TYPE_OBJECT; \
    SP++; }

#define PUSH_INT(value) { JAVA_INT pInt = value; (*SP).type = CN1_TYPE_INT; \
    (*SP).data.i = pInt; \
    SP++; }

#define PUSH_LONG(value) { JAVA_LONG plong = value; (*SP).type = CN1_TYPE_LONG; \
    (*SP).data.l = plong; \
    SP++; }

#define PUSH_DOUBLE(value) { JAVA_DOUBLE pdob = value; (*SP).type = CN1_TYPE_DOUBLE; \
    (*SP).data.d = pdob; \
    SP++; }

#define PUSH_FLOAT(value) { JAVA_FLOAT pFlo = value; (*SP).type = CN1_TYPE_FLOAT; \
    (*SP).data.f = pFlo; \
    SP++; }

#define POP_MANY_AND_PUSH_OBJ(value, offset) {  \
    JAVA_OBJECT pObj = value; SP[-offset].type = CN1_TYPE_INVALID; \
    SP[-offset].data.o = pObj; SP[-offset].type = CN1_TYPE_OBJECT; \
    popMany(threadStateData, MAX(1, offset) - 1, &SP); }

#define POP_MANY_AND_PUSH_INT(value, offset) {  \
    JAVA_INT pInt = value; SP[-offset].type = CN1_TYPE_INT; \
    SP[-offset].data.i = pInt; \
    popMany(threadStateData, MAX(1, offset) - 1, &SP); }

#define POP_MANY_AND_PUSH_LONG(value, offset) {  \
    JAVA_LONG pLong = value; SP[-offset].type = CN1_TYPE_LONG; \
    SP[-offset].data.l = pLong; \
    popMany(threadStateData, MAX(1, offset) - 1, &SP); }

#define POP_MANY_AND_PUSH_DOUBLE(value, offset) {  \
    JAVA_DOUBLE pDob = value; SP[-offset].type = CN1_TYPE_DOUBLE; \
    SP[-offset].data.d = pDob; \
    popMany(threadStateData, MAX(1, offset) - 1, &SP); }

#define POP_MANY_AND_PUSH_FLOAT(value, offset) {  \
    JAVA_FLOAT pFlo = value; SP[-offset].type = CN1_TYPE_FLOAT; \
    SP[-offset].data.f = pFlo; \
    popMany(threadStateData, MAX(1, offset) - 1, &SP); }


#define BC_IDIV() SP--; SP[-1].data.i = SP[-1].data.i / (*SP).data.i

#define BC_LDIV() SP--; SP[-1].data.l = SP[-1].data.l / (*SP).data.l

#define BC_FDIV() SP--; SP[-1].data.f = SP[-1].data.f / (*SP).data.f

#define BC_DDIV() SP--; SP[-1].data.d = SP[-1].data.d / (*SP).data.d

#define BC_IREM() SP--; SP[-1].data.i = SP[-1].data.i % (*SP).data.i

#define BC_LREM() SP--; SP[-1].data.l = SP[-1].data.l % (*SP).data.l

#define BC_FREM() SP--; SP[-1].data.f = fmod(SP[-1].data.f, (*SP).data.f)

#define BC_DREM() SP--; SP[-1].data.d = fmod(SP[-1].data.d, (*SP).data.d)

#define BC_LCMP() SP--; if(SP[-1].data.l == (*SP).data.l) { \
        SP[-1].data.i = 0; \
    } else { \
        if(SP[-1].data.l > (*SP).data.l) { \
            SP[-1].data.i = 1; \
        } else { \
            SP[-1].data.i = -1; \
        } \
    } \
    SP[-1].type = CN1_TYPE_INT;

#define BC_FCMPL() SP--; if(SP[-1].data.f == (*SP).data.f) { \
        SP[-1].data.i = 0; \
    } else { \
        if(SP[-1].data.f > (*SP).data.f) { \
            SP[-1].data.i = 1; \
        } else { \
            SP[-1].data.i = -1; \
        } \
    } \
    SP[-1].type = CN1_TYPE_INT;

#define BC_DCMPL() SP--; if(SP[-1].data.d == (*SP).data.d) { \
        SP[-1].data.i = 0; \
    } else { \
        if(SP[-1].data.d > (*SP).data.d) { \
            SP[-1].data.i = 1; \
        } else { \
            SP[-1].data.i = -1; \
        } \
    } \
    SP[-1].type = CN1_TYPE_INT;

#define CN1_CMP_EXPR(val1, val2) ((val1 == val2) ? 0 : (val1 > val2) ? 1 :  -1)

#define BC_DUP()  { \
        JAVA_LONG plong = SP[-1].data.l; \
        (*SP).type = CN1_TYPE_INVALID; \
        (*SP).data.l = plong; (*SP).type = CN1_TYPE_LONG; \
        SP++; \
    } \
    SP[-1].type = SP[-2].type; 

#define BC_DUP2()  \
if(SP[-1].type == CN1_TYPE_LONG || SP[-1].type == CN1_TYPE_DOUBLE) {\
    BC_DUP(); \
} else {\
    { \
        JAVA_LONG plong = SP[-2].data.l; \
        JAVA_LONG plong2 = SP[-1].data.l; \
        (*SP).type = CN1_TYPE_INVALID; \
        SP[1].type = CN1_TYPE_INVALID; \
        (*SP).data.l = plong; \
        SP[1].data.l = plong2; \
        SP+=2; \
    } \
    SP[-1].type = SP[-3].type; \
    SP[-2].type = SP[-4].type; \
}

#define BC_DUP2_X1() {\
    if (IS_DOUBLE_WORD(-1)){\
        (*SP).data.l = SP[-1].data.l; \
        SP[-1].data.l = SP[-2].data.l; \
        SP[-2].data.l = (*SP).data.l; \
        (*SP).type = SP[-1].type; \
        SP[-1].type = SP[-2].type; \
        SP[-2].type = (*SP).type; \
        SP++; \
    } else {\
        SP[1].data.l = SP[-1].data.l; \
        (*SP).data.l = SP[-2].data.l; \
        SP[-1].data.l = SP[-3].data.l; \
        SP[-2].data.l = SP[1].data.l; \
        SP[-3].data.l = (*SP).data.l;\
        SP[1].type = SP[-1].type;\
        (*SP).type = SP[-2].type; \
        SP[-1].type = SP[-3].type; \
        SP[-2].type = SP[1].type; \
        SP[-3].type = (*SP).type;\
        SP+=2;\
    }\
}

#define BC_DUP_X1() {\
    (*SP).data.l = SP[-1].data.l; \
    SP[-1].data.l = SP[-2].data.l; \
    SP[-2].data.l = (*SP).data.l; \
    (*SP).type = SP[-1].type; \
    SP[-1].type = SP[-2].type; \
    SP[-2].type = (*SP).type; \
    SP++; \
}

struct elementStruct* BC_DUP2_X2_DD(struct elementStruct* SP);
struct elementStruct* BC_DUP2_X2_DSS(struct elementStruct* SP);
struct elementStruct* BC_DUP2_X2_SSD(struct elementStruct* SP);
struct elementStruct* BC_DUP2_X2_SSSS(struct elementStruct* SP);
struct elementStruct* BC_DUP_X2_SD(struct elementStruct* SP);
struct elementStruct* BC_DUP_X2_SSS(struct elementStruct* SP);

#define IS_DOUBLE_WORD(offset) (SP[offset].type == CN1_TYPE_LONG || SP[offset].type == CN1_TYPE_DOUBLE)

#define BC_DUP_X2() {\
    if (IS_DOUBLE_WORD(-2)) SP=BC_DUP_X2_SD(SP);\
    else SP=BC_DUP_X2_SSS(SP);\
}

#define BC_DUP2_X2() { \
    if (IS_DOUBLE_WORD(-2)) SP=BC_DUP2_X2_DD(SP);\
else if (IS_DOUBLE_WORD(-1)) SP=BC_DUP2_X2_DSS(SP);\
    else if (IS_DOUBLE_WORD(-3)) SP=BC_DUP2_X2_SSD(SP);\
    else SP=BC_DUP2_X2_SSSS(SP);\
}


#define BC_I2B() SP[-1].data.i = ((SP[-1].data.i << 24) >> 24)

#define BC_I2S() SP[-1].data.i = ((SP[-1].data.i << 16) >> 16)

#define BC_I2C() SP[-1].data.i = (SP[-1].data.i & 0xffff)

#define BC_ISHL() SP--; SP[-1].data.i = (SP[-1].data.i << (0x1f & (*SP).data.i))
#define BC_ISHL_EXPR(val1, val2) (val1 << (0x1f & val2))
#define BC_LSHL() SP--; SP[-1].data.l = (SP[-1].data.l << (0x3f & (*SP).data.l))
#define BC_LSHL_EXPR(val1, val2) (val1 << (0x3f & val2))

#define BC_ISHR() SP--; SP[-1].data.i = (SP[-1].data.i >> (0x1f & (*SP).data.i))
#define BC_ISHR_EXPR(val1, val2) (val1 >> (0x1f & val2))

#define BC_LSHR() SP--; SP[-1].data.l = (SP[-1].data.l >> (0x3f & (*SP).data.l))
#define BC_LSHR_EXPR(val1, val2) (val1 >> (0x3f & val2))

#define BC_IUSHL() SP--; SP[-1].data.i = (((unsigned int)SP[-1].data.i) << (0x1f & ((unsigned int)(*SP).data.i)))
#define BC_IUSHL_EXPR(val1, val2) (((unsigned int)val1) << (0x1f & ((unsigned int)val2)))

#define BC_LUSHL() SP--; SP[-1].data.l = (((unsigned long long)SP[-1].data.l) << (0x3f & ((unsigned long long)(*SP).data.l)))
#define BC_LUSHL_EXPR(val1, val2) (((unsigned long long)val1) << (0x3f & ((unsigned long long)val2)))

#define BC_IUSHR() SP--; SP[-1].data.i = (((unsigned int)SP[-1].data.i) >> (0x1f & ((unsigned int)(*SP).data.i)))
#define BC_IUSHR_EXPR(val1, val2) (((unsigned int)val1) >> (0x1f & ((unsigned int)val2)))

#define BC_LUSHR() SP--; SP[-1].data.l = (((unsigned long long)SP[-1].data.l) >> (0x3f & ((unsigned long long)(*SP).data.l)))
#define BC_LUSHR_EXPR(val1, val2) (((unsigned long long)val1) >> (0x3f & ((unsigned long long)val2)))

#define BC_ISUB() SP--; SP[-1].data.i = (SP[-1].data.i - (*SP).data.i)

#define BC_LSUB() SP--; SP[-1].data.l = (SP[-1].data.l - (*SP).data.l)

#define BC_FSUB() SP--; SP[-1].data.f = (SP[-1].data.f - (*SP).data.f)

#define BC_DSUB() SP--; SP[-1].data.d = (SP[-1].data.d - (*SP).data.d)

extern JAVA_OBJECT* constantPoolObjects;

extern int classListSize;
extern struct clazz* classesList[];

// this needs to be fixed to actually return a JAVA_OBJECT...
#define STRING_FROM_CONSTANT_POOL_OFFSET(off) constantPoolObjects[off]

#define BC_IINC(val, num) ilocals_##val##_ += num;

extern int instanceofFunction(int sourceClass, int destId);

// Tagged small-integer ("poor man's Valhalla"): Integer.valueOf returns an immediate
// tagged pointer (low bit = 1, the int in the high bits) instead of allocating, and the
// GC ignores it while every class/dispatch lookup substitutes Integer's class. 64-bit
// POINTERS ONLY: on a 32-bit-pointer target a 32-bit int can't be tagged losslessly, so
// it must fall back to heap boxing. That includes armv7/armv7k AND arm64_32 (Apple Watch
// Series 4+, which is 64-bit hardware but uses 32-bit pointers) -- hence the gate is on
// __SIZEOF_POINTER__, not the architecture. DEFAULT ON for 64-bit-pointer
// targets (the shipping iOS/tv/desktop shape); opt out with
// -DCN1_DISABLE_TAGGED_INT. The gate below still auto-disables it wherever
// pointers are 32-bit, so no per-target configuration is needed.
#if !defined(CN1_DISABLE_TAGGED_INT) && !defined(CN1_TAGGED_INT)
#define CN1_TAGGED_INT
#endif
#if defined(CN1_TAGGED_INT) && !defined(CN1_DISABLE_TAGGED_INT) && defined(__SIZEOF_POINTER__) && (__SIZEOF_POINTER__ >= 8)
#define CN1_TAGGED_ACTIVE 1
#else
#define CN1_TAGGED_ACTIVE 0
#endif
extern struct clazz class__java_lang_Integer;
#if CN1_TAGGED_ACTIVE
struct JavaObjectPrototype;
// A static object-shaped proxy whose header is Integer's class. CN1_CLASS_OF selects a
// VALID object pointer (proxy for a tagged int, else the object itself) BEFORE the single
// header load, so clang's if-conversion can branchlessly select the pointer yet the load
// is always on a dereferenceable address -- a plain ternary lets clang speculate the
// faulting `tagged->header` load above the tag test (observed: a SIGSEGV in interface
// dispatch like Comparable.compareTo, where no inline fast path guards it first).
extern struct JavaObjectPrototype cn1TaggedProxy;
#define CN1_IS_TAGGED(o) (((uintptr_t)(o)) & 1)
#define CN1_TAG_INT(v) ((JAVA_OBJECT)((((uintptr_t)(intptr_t)(JAVA_INT)(v)) << 1) | 1))
#define CN1_UNTAG_INT(o) ((JAVA_INT)(((intptr_t)(o)) >> 1))
#define CN1_CLASS_OF(o) ((CN1_IS_TAGGED(o) ? &cn1TaggedProxy : (struct JavaObjectPrototype*)(o))->__codenameOneParentClsReference)
#else
#define CN1_IS_TAGGED(o) (0)
#define CN1_CLASS_OF(o) ((o)->__codenameOneParentClsReference)
#endif

#define GET_CLASS_ID(JavaObj) ((CN1_CLASS_OF(JavaObj))->classId)

#define BC_INSTANCEOF(typeOfInstanceOf) { \
    if(SP[-1].data.o != JAVA_NULL) { \
        int tmpInstanceOfId = GET_CLASS_ID(SP[-1].data.o); \
        SP[-1].type = CN1_TYPE_INVALID; \
        SP[-1].data.i = instanceofFunction( typeOfInstanceOf, tmpInstanceOfId ); \
    } \
    SP[-1].type = CN1_TYPE_INT; \
}

#define BC_IALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_INT; \
    SP[-1].data.i = ((JAVA_ARRAY_INT*) (*(JAVA_ARRAY)SP[-1].data.o).data)[(*SP).data.i]; \
    }

#define BC_LALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_LONG; \
    SP[-1].data.l = LONG_ARRAY_LOOKUP((JAVA_ARRAY)SP[-1].data.o, (*SP).data.i); \
    }

#define BC_FALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_FLOAT; \
    SP[-1].data.f = FLOAT_ARRAY_LOOKUP((JAVA_ARRAY)SP[-1].data.o, (*SP).data.i); \
    }

#define BC_DALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_DOUBLE; \
    SP[-1].data.d = DOUBLE_ARRAY_LOOKUP((JAVA_ARRAY)SP[-1].data.o, (*SP).data.i); \
    }

#define BC_AALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_INVALID; \
    SP[-1].data.o = ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)SP[-1].data.o).data)[(*SP).data.i]; \
    SP[-1].type = CN1_TYPE_OBJECT;  }

#define BC_BALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_INT; \
    SP[-1].data.i = ((JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY)SP[-1].data.o).data)[(*SP).data.i]; \
    }

#define BC_CALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_INT; \
    SP[-1].data.i = ((JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY)SP[-1].data.o).data)[(*SP).data.i]; \
    }

#define BC_SALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_INT; \
    SP[-1].data.i = ((JAVA_ARRAY_SHORT*) (*(JAVA_ARRAY)SP[-1].data.o).data)[(*SP).data.i]; \
    }


#define BC_BASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    ((JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY)SP[-3].data.o).data)[SP[-2].data.i] = SP[-1].data.i; SP-=3

#define BC_CASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    ((JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY)SP[-3].data.o).data)[SP[-2].data.i] = SP[-1].data.i; SP-=3

#define BC_SASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    ((JAVA_ARRAY_SHORT*) (*(JAVA_ARRAY)SP[-3].data.o).data)[SP[-2].data.i] = SP[-1].data.i; SP-=3

#define BC_IASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    ((JAVA_ARRAY_INT*) (*(JAVA_ARRAY)SP[-3].data.o).data)[SP[-2].data.i] = SP[-1].data.i; SP-=3

#define BC_LASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    LONG_ARRAY_LOOKUP((JAVA_ARRAY)SP[-3].data.o, SP[-2].data.i) = SP[-1].data.l; SP-=3

#define BC_FASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    FLOAT_ARRAY_LOOKUP((JAVA_ARRAY)SP[-3].data.o, SP[-2].data.i) = SP[-1].data.f; SP-=3

#define BC_DASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    DOUBLE_ARRAY_LOOKUP((JAVA_ARRAY)SP[-3].data.o, SP[-2].data.i) = SP[-1].data.d; SP-=3

#define BC_AASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); { \
    JAVA_OBJECT aastoreTmp = SP[-3].data.o; \
    CN1_WRITE_BARRIER(aastoreTmp, SP[-1].data.o); \
    ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)aastoreTmp).data)[SP[-2].data.i] = SP[-1].data.o; \
    SP-=3; \
}
#define BC_AASTORE_WITH_ARGS(array, index, value) CHECK_ARRAY_ACCESS(3, SP[-2].data.i); { \
    JAVA_OBJECT aastoreTmp = SP[-3].data.o; \
    CN1_WRITE_BARRIER(aastoreTmp, SP[-1].data.o); \
    ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)aastoreTmp).data)[SP[-2].data.i] = SP[-1].data.o; \
    SP-=3; \
}


//#define BYTE_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_BYTE*) (*array).data)[offset]
//#define SHORT_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_SHORT*) (*array).data)[offset]
//#define CHAR_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_CHAR*) (*array).data)[offset]
//#define INT_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_INT*) (*array).data)[offset]

#define LONG_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_LONG*) (*array).data)[offset]

#define FLOAT_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_FLOAT*) (*array).data)[offset]

#define DOUBLE_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_DOUBLE*) (*array).data)[offset]

//#define OBJECT_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_OBJECT*) (*array).data)[offset]

// indicates a try/catch block currently in frame
struct TryBlock {
    jmp_buf destination;
    
    // -1 for all exceptions
    JAVA_INT exceptionClass;

    // Synchronized methods will use a TryBlock for its monitor
    // so that the monitor will be exited when an exception is thrown.
    // This will be 0 for regular TryBlock.
    JAVA_OBJECT monitor;
};

#define CN1_MAX_STACK_CALL_DEPTH 1024
#define CN1_STACK_OVERFLOW_CALL_DEPTH_LIMIT CN1_MAX_STACK_CALL_DEPTH
#define CN1_MAX_OBJECT_STACK_DEPTH 16536

#define PER_THREAD_ALLOCATION_COUNT 4096

#ifdef CN1_NURSERY
// Tunables (override with -D). Block size and arena size trade footprint against
// how long churn lives before a minor collection (the bigger the nursery, the more
// short-lived garbage dies in-place instead of being promoted).
// 64 KB measured better than 256 KB across the allocation benchmarks (less waste when
// a block tenures at low density -> stringBuilding 0.92x -> 1.23x, objectAllocation
// slightly better, hashMapChurn ~neutral). Configurable for fragmentation tuning.
#ifndef CN1_NURSERY_BLOCK_SIZE
#define CN1_NURSERY_BLOCK_SIZE (64*1024)
#endif
#ifndef CN1_NURSERY_ARENA_SIZE
#define CN1_NURSERY_ARENA_SIZE (64*1024*1024)
#endif
#ifndef CN1_NURSERY_MAX_OBJECT
#define CN1_NURSERY_MAX_OBJECT 512
#endif
// Minor collection fires after this many bytes have been bump-allocated by a thread.
#ifndef CN1_NURSERY_MINOR_TRIGGER
#define CN1_NURSERY_MINOR_TRIGGER (8*1024*1024)
#endif
// Adaptive survival-based bypass. When a minor collection finds that at least
// CN1_NURSERY_BYPASS_SURVIVAL_PCT% of the objects allocated since the last collection
// survived (escaped/were promoted), the nursery is pure overhead for this phase: the
// thread bypasses it and allocates straight into the global heap for the next
// CN1_NURSERY_BYPASS_ALLOCS allocations, then re-probes by allocating in the nursery
// again. CN1_NURSERY_BYPASS_MIN_SAMPLE avoids deciding on a tiny sample.
#ifndef CN1_NURSERY_BYPASS_SURVIVAL_PCT
#define CN1_NURSERY_BYPASS_SURVIVAL_PCT 60
#endif
#ifndef CN1_NURSERY_BYPASS_ALLOCS
#define CN1_NURSERY_BYPASS_ALLOCS 200000
#endif
#ifndef CN1_NURSERY_BYPASS_MIN_SAMPLE
#define CN1_NURSERY_BYPASS_MIN_SAMPLE 1024
#endif
// When re-probing after a bypass, collect after this many bytes (a small sample)
// instead of the full minor trigger, so a still-escaping phase pays only a tiny
// re-measurement cost before bypassing again.
#ifndef CN1_NURSERY_REPROBE_BYTES
#define CN1_NURSERY_REPROBE_BYTES (512*1024)
#endif
extern char* cn1NurseryArenaStart;
extern char* cn1NurseryArenaEnd;
// Forward-declare at file scope so the prototype below refers to THIS tag, not a new
// prototype-scoped one. CODENAME_ONE_THREAD_STATE isn't defined this early either.
struct ThreadLocalData;
extern JAVA_OBJECT cn1NurseryAlloc(struct ThreadLocalData* threadStateData, int size, struct clazz* parent);
extern void cn1NurseryWriteBarrier(JAVA_OBJECT target, JAVA_OBJECT value);
static inline JAVA_BOOLEAN cn1InNursery(void* p) {
    return (char*)p >= cn1NurseryArenaStart && (char*)p < cn1NurseryArenaEnd;
}
// Emitted by the translator before an object-reference store into a heap location.
// Fast path is INLINE: only a value that actually lives in the nursery can escape, so
// the overwhelmingly common heap->heap / null store collapses to a two-compare range
// check with no call and no getThreadLocalData() TLS lookup. This matters enormously
// for store-heavy code (HashMap internals, etc.) and makes the barrier ~free whenever
// the nursery isn't holding the value (including while bypassed).
#define CN1_WRITE_BARRIER(target, value) \
    do { JAVA_OBJECT cn1__bv = (JAVA_OBJECT)(value); \
         if(cn1__bv != JAVA_NULL && cn1InNursery(cn1__bv)) { \
             cn1NurseryWriteBarrier((JAVA_OBJECT)(target), cn1__bv); } } while(0)
#else
#define CN1_WRITE_BARRIER(target, value)
#endif

extern const char* volatile cn1LastNamSetter; // diagnosis: last bracket toucher
#ifdef CN1_CONSERVATIVE_GC_ROOTS
// The bracket's purpose was to suppress GC interaction while native C code
// holds heap references in UNROOTED C locals. Under conservative roots the
// native stack IS scanned, so those locals are roots like any other -- the
// flag stores (two of them per native call, one a global write) were pure
// hot-path overhead on every String/StringBuilder/HashMap native.
#define enteringNativeAllocations() do { } while(0)
#define finishedNativeAllocations() do { } while(0)
#else
#define enteringNativeAllocations() do { threadStateData->nativeAllocationMode = JAVA_TRUE; cn1LastNamSetter = __FUNCTION__; } while(0)
#define finishedNativeAllocations() do { threadStateData->nativeAllocationMode = JAVA_FALSE; cn1LastNamSetter = 0; } while(0)
#endif

// handles the stack used for print stack trace and GC
struct ThreadLocalData {
    JAVA_LONG threadId;
    JAVA_OBJECT currentThreadObject;
    struct TryBlock* blocks;
    int tryBlockOffset;
    JAVA_OBJECT exception;
    
    JAVA_BOOLEAN lightweightThread;
    JAVA_BOOLEAN threadActive;
    JAVA_BOOLEAN threadBlockedByGC;
    JAVA_BOOLEAN nativeAllocationMode;
    JAVA_BOOLEAN threadRemoved;

    // used by the GC to traverse the objects pointed to by this thread
    struct elementStruct* threadObjectStack;
    int threadObjectStackOffset;
    
    // allocations are stored here and then copied to the big memory pool during
    // the mark sweep
    void** pendingHeapAllocations;
    JAVA_INT heapAllocationSize;
    JAVA_INT threadHeapTotalSize;

#ifdef CN1_NURSERY
    // Thread-local young-generation ("nursery") bump allocator. Small objects are
    // bump-allocated here and NEVER enter allObjectsInHeap; a thread-local minor
    // collection promotes only the survivors and reclaims the rest in bulk, so the
    // global mark/sweep cost becomes O(survivors) instead of O(allocated).
    char* nurseryBump;                 // next free byte in the current block
    char* nurseryEnd;                  // end of the current block
    int   nurseryCurrentBlock;         // index of the current block (-1 = none)
    int*  nurseryYoungBlocks;          // block indices owned by this thread's young gen
    int   nurseryYoungCount;
    int   nurseryYoungCapacity;
    long  nurseryBytesSinceMinor;      // drives the minor-GC trigger
    // Per-thread promotion state. MUST be per-thread: the concurrent GC thread runs
    // gcMarkObject at the same time a mutator promotes, and a shared flag would make
    // the GC thread promote-instead-of-mark and corrupt the heap.
    JAVA_BOOLEAN nurseryPromoting;
    JAVA_OBJECT* nurseryPromoteWorklist;
    int   nurseryPromoteTop;
    int   nurseryPromoteCap;
    // Adaptive bypass: survival sampling + bypass countdown (see CN1_NURSERY_BYPASS_*).
    int   nurseryAllocSinceMinor;      // objects bump-allocated since the last minor
    int   nurseryPromotedSinceMinor;   // of those, how many survived (were promoted)
    JAVA_BOOLEAN nurseryBypass;        // true => allocate straight to the global heap
    int   nurseryBypassCountdown;      // allocations left before re-probing the nursery
    JAVA_BOOLEAN nurseryReprobing;     // just exited bypass: collect on a small sample
#endif

    // used to construct stack trace
    int* callStackClass;
    int* callStackLine;
    int* callStackMethod;
    int callStackOffset;

    // Native C-stack low-water mark used by frameless methods (see
    // CN1_FRAMELESS_SOE_GUARD). Frameless primitive-only methods don't bump
    // callStackOffset, so the call-depth limit can't catch their native C-stack
    // recursion; this per-thread limit lets the guard throw a catchable
    // StackOverflowError instead of overrunning the stack into a SIGSEGV.
    // 0 == not yet computed (lazily initialized once per thread on first use).
    JAVA_LONG nativeStackLimit;

#ifndef CN1_DISABLE_DEATOMIC_BYTES
    // LEVER A (perf-tier1): per-thread, plain-add accumulator for BiBOP allocation
    // volume. Replaces the per-object atomic_fetch_add on the global bibopBytesSinceGc
    // (which an uncontended single thread still pays as an arm64 exclusive-monitor RMW,
    // and which bounces a cache line across threads). Flushed to the global atomic once
    // per page-acquire (~64KB of allocation), so the GC-trigger cadence is unchanged to
    // within (nthreads * page) -- negligible vs the 24MB trigger, and the trigger is a
    // pure heuristic with NO correctness role (see CN1_BIBOP_FLUSH_BYTES).
    JAVA_LONG bibopBytesLocal;
#endif

#ifdef CN1_ON_DEVICE_DEBUG
    // Per-frame pointer to a stack-allocated array of void* addresses, one per
    // JVM local slot in the current method. Populated by translator-emitted
    // prologue code in debug builds; consulted by the debugger thread to read
    // primitive locals (the auto C variables are volatile so their address is
    // stable for the duration of the frame).
    void*** callStackLocalsAddresses;
    // Per-frame pointer to the static cn1_frame_info struct for the current
    // method. Carries the variable side-table the debugger uses to map source
    // lines to slot/type info.
    const struct cn1_frame_info** callStackFrameInfo;
#endif

    char* utf8Buffer;
    int utf8BufferSize;
    JAVA_BOOLEAN threadKilled;      // we don't expect to see this in the GC
    JAVA_BOOLEAN interrupted;

    // Dead-thread pending-migration queue (single-writer allObjectsInHeap):
    // markDeadThread queues the dying thread's TLD (critical section held)
    // instead of migrating pendingHeapAllocations itself; the GC thread drains
    // the queue at mark start. gcReleaseRequested defers the TLD free (Thread
    // finalizer) until after that drain. See cn1DeadPendingThreads in
    // cn1_globals.m for the invariant and the race this closes.
    struct ThreadLocalData* gcDeadNext;
    JAVA_BOOLEAN gcQueuedForDrain;
    JAVA_BOOLEAN gcReleaseRequested;
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    // PHASE 3b: state for conservatively scanning this thread's native C stack as a
    // GC root source (so object-bearing FRAMELESS methods, whose object roots live in
    // native C locals / the method-local operand array rather than threadObjectStack,
    // are kept alive). Two stop mechanisms feed these:
    //   (1) COOPERATIVE park: a lightweight thread that pauses at an allocation
    //       safepoint runs CN1_GC_PARK_CAPTURE just before publishing threadActive=0.
    //   (2) SIGNAL stop: the GC pthread_kills any thread it could not cooperatively
    //       park; the async-signal-safe handler captures SP+regs and spins here.
    pthread_t    gcPthread;              // pthread_self() of THIS thread (set at startup)
    JAVA_BOOLEAN gcPthreadValid;         // gcPthread has been filled in
    // cooperative-park capture
    jmp_buf      gcRegisterSnapshot;     // setjmp flushes callee-saved regs -> scanned
    void* volatile gcStackPointerAtPark; // SP-ish low bound captured at the park point
    volatile JAVA_BOOLEAN gcParkCaptured;// a fresh cooperative capture exists this cycle
    // signal-stop capture (async-signal-safe: handler only stores + spins).
    // GENERATION HANDSHAKE: request/stopped/release carry a per-thread generation
    // number (monotonic, GC-thread owned counter gcSigStopGen) instead of booleans,
    // so an abandoned stop (timeout) or a descheduled handler can never strand
    // spinning on a release that was reset -- see cn1GcSignalStopOne/ReleaseOne.
    volatile sig_atomic_t gcSigStopRequest; // GC sets to gen>0 to ask the handler to park
    volatile sig_atomic_t gcSigStopped;     // handler publishes the gen it parked for
    volatile sig_atomic_t gcSigRelease;     // GC publishes highest released gen (monotonic)
    volatile sig_atomic_t gcSigStopGen;     // generation counter (GC thread writes only)
    void* volatile gcSigStackPointer;        // SP captured inside the signal handler
    void* volatile gcSigStackBase;           // [sp,base) high bound (filled by GC/handler)
    char         gcSigRegs[4096];            // raw copy of the interrupted ucontext (GPRs)
    volatile sig_atomic_t gcSigRegsLen;      // valid bytes in gcSigRegs
#endif
};

//#define BLOCK_FOR_GC() while(threadStateData->threadBlockedByGC) { usleep(500); }

#ifdef CN1_ON_DEVICE_DEBUG
// One row of the variable side-table: a single (line, slot, typeCode) tuple.
// typeCode is the JVM type descriptor first char (I/J/F/D/Z/B/S/C/L/[) so the
// debugger thread knows how to dereference the void* held in
// callStackLocalsAddresses[offset][slot].
struct cn1_var_entry {
    int line;
    int slot;
    char typeCode;
};

// Per-method static metadata emitted once per translated method. Held alive
// for the life of the program. The translator emits an instance as
// "static const struct cn1_frame_info __cn1_finfo_<method> = { ... };" and
// passes &__cn1_finfo_<method> into the frame at method entry.
struct cn1_frame_info {
    int classId;
    int methodId;
    int numLocals;
    int varTableCount;
    const struct cn1_var_entry* varTable;
};

// Set to non-zero by the debugger proxy listener once a proxy has connected
// and is ready to receive events. Read on the hot path of __CN1_DEBUG_INFO,
// so kept as a plain volatile int (predictable branch when zero).
extern volatile int cn1DebuggerActive;

// Cold-path callee invoked by __CN1_DEBUG_INFO when cn1DebuggerActive is set.
// Defined in cn1_debugger.m (iOS port) / a no-op shim in release builds.
extern void cn1_debugger_check(struct ThreadLocalData* threadStateData, int line);

#define __CN1_DEBUG_INFO(line) \
    do { \
        threadStateData->callStackLine[threadStateData->callStackOffset - 1] = (line); \
        if (__builtin_expect(cn1DebuggerActive, 0)) { \
            cn1_debugger_check(threadStateData, (line)); \
        } \
    } while (0)
// Line-info store for a source line whose every instruction is provably
// non-throwing/non-calling (pure arithmetic, local load/store, constants,
// compares, branches, conversions). Such a line can NEVER be the line reported
// in a stack trace -- the trace line is always read at a call/throw/alloc site,
// which lives on a kept line -- so eliding its store is trace-identical. Kept
// fully under the on-device debugger (which steps line-by-line and needs every
// line); elided in release/device builds, where it removes the only per-line hot
// cost (lets clang keep tight loops in registers / vectorize).
#define __CN1_DEBUG_INFO_NT(line) __CN1_DEBUG_INFO(line)
#else
#define __CN1_DEBUG_INFO(line) threadStateData->callStackLine[threadStateData->callStackOffset - 1] = line;
#define __CN1_DEBUG_INFO_NT(line) do {} while(0)
#endif

// we need to throw stack overflow error but its unavailable here...
/*#define ENTERING_CODENAME_ONE_METHOD(classIdNumber, methodIdNumber) { \
    assert(threadStateData->callStackOffset < CN1_MAX_STACK_CALL_DEPTH - 1); \
    threadStateData->callStackClass[threadStateData->callStackOffset] = classIdNumber; \
    threadStateData->callStackMethod[threadStateData->callStackOffset] = methodIdNumber; \
    threadStateData->callStackOffset++; \
} \
const int currentCodenameOneCallStackOffset = threadStateData->callStackOffset; 
*/

#define CODENAME_ONE_THREAD_STATE struct ThreadLocalData* threadStateData

// =========================================================================
// LEVER 1: inlined BiBOP bump fast-path (alloc fast-path, perf-tier1)
//
// The escaping `new X()` path is, at codenameOneGcMalloc, an OUT-OF-LINE cross
// translation-unit call (per-class .c -> cn1_globals.c, no LTO in this build),
// which clang therefore cannot inline. This block exposes the minimal BiBOP
// bump surface to every TU so the common case (per-thread current page has a
// free bump slot for this object's size class) can be emitted INLINE at the
// allocation site -- pointer-bump + header stamp, no call -- exactly mirroring
// HotSpot's inlined-TLAB-bump + slow-path-call. The size-class index is a
// compile-time constant per type (sizeof(struct obj__X) is known to clang), so
// CN1_BIBOP_CIDX folds to a literal; an oversized type folds the fast path away
// entirely and falls straight to the slow path.
//
// The bump replicates cn1BibopAlloc's bump branch BIT-FOR-BIT (relaxed load of
// bumpIndex, init the slot with the mark published LAST via an atomic-release
// store, release-store the new cursor AFTER the slot is fully initialized,
// relaxed add to bibopBytesSinceGc). Same memory ordering => the concurrent-GC
// overflow-rescan / sweep correctness argument is unchanged. The free-list and
// page-acquire cases stay on the slow path (codenameOneGcMalloc). isAppSuspended
// handling stays on the slow path too; a suspended-app bibop alloc still
// succeeds and bibopBytesSinceGc still drives collection, so the only behavioural
// delta is a delayed GC-thread restart while suspended (no checksum/leak impact).
//
// Gated by -DCN1_INLINE_ALLOC. OFF => CN1_FAST_NEW(X) is exactly __NEW_X.
// =========================================================================
#ifndef CN1_BIBOP_PAGE_SIZE
#define CN1_BIBOP_PAGE_SIZE (64*1024)
#endif
#ifndef CN1_BIBOP_MAX_OBJECT
#define CN1_BIBOP_MAX_OBJECT 512
#endif
#ifndef CN1_BIBOP_HEAP_POS
#define CN1_BIBOP_HEAP_POS (-3)
#endif
// Slot sizes (16-aligned); a size maps to the smallest class >= size.
#define CN1_BIBOP_NUM_CLASSES 15
// Compile-time size -> class-index. With a constant `sz` (sizeof(...)) clang
// folds the whole chain to an int literal (or -1 for oversized => fast path
// dead-code-eliminated, slow path only).
#define CN1_BIBOP_CIDX(sz) ( \
  (sz)<=32?0:(sz)<=48?1:(sz)<=64?2:(sz)<=80?3:(sz)<=96?4:(sz)<=112?5: \
  (sz)<=128?6:(sz)<=160?7:(sz)<=192?8:(sz)<=224?9:(sz)<=256?10: \
  (sz)<=320?11:(sz)<=384?12:(sz)<=448?13:(sz)<=512?14:-1)

typedef struct CN1BibopPage {
    struct CN1BibopPage* _Atomic nextAll; // append-only global registry chain
    struct CN1BibopPage* nextPool;        // FREE/PARTIAL pool / SWEEP stack link
    int classIndex;
    int slotSize;
    int slotCount;
    int firstSlotOffset;                  // byte offset of slot 0 from page base
    _Atomic int bumpIndex;                // next slot to bump-allocate (published)
    void* freeList;                       // intrusive free-list head (slot ptr)
    int freeCount;
    JAVA_BOOLEAN owned;
    // ---- O(live-pages) sweep bookkeeping (perf-tier1, gated by CN1_BIBOP_NO_FASTSWEEP)
    // These let cn1BibopSweep reclaim an all-dead page or skip an all-live (in-grace)
    // page in O(1) -- without the per-slot walk -- whenever it can PROVE the page is
    // homogeneous. The fields are always present (so the struct layout is identical in
    // A/B builds); only the writes/reads are gated. See cn1BibopSweep for the proof.
    JAVA_BOOLEAN gcAllocedSinceSweep;     // any alloc into the page since last sweep/reset
                                          // (owner-thread single-writer; published to the
                                          //  GC via the sweep-stack release-push)
    JAVA_BOOLEAN gcNeedsReclaim;          // a survivor carries a finalizer or monitor ->
                                          //  dead slots must reach cn1BibopReclaimSlot
    JAVA_BOOLEAN gcHasMonitors;           // STICKY: a monitor was ever attached to an
                                          //  object in this page (set by
                                          //  cn1BibopNoteMonitorAttached, cleared only by
                                          //  cn1BibopFormatPage). Suppresses the O(1)
                                          //  all-dead reclaim for THIS page only, so a
                                          //  dead monitored slot always reaches
                                          //  cn1BibopReclaimSlot -- replacing the global
                                          //  monitor count that disabled the shortcut for
                                          //  EVERY page whenever any monitor existed
                                          //  (e.g. java.lang.System.LOCK, permanently).
                                          //  (recomputed at every full walk)
    _Atomic int gcLastMarkedEpoch;        // currentGcMarkValue stamped by gcMarkObject when
                                          //  a slot on this page is marked live (relaxed;
                                          //  idempotent across parallel markers)
    int gcGraceEpoch;                     // upper bound on survivor epochs as of the last
                                          //  full walk (GC-thread only)
} CN1BibopPage;

// Per-thread current page per size class; defined in cn1_globals.m. Touched only
// by the owning thread (alloc) and by that same thread on death.
extern __thread CN1BibopPage* bibopCurrent[CN1_BIBOP_NUM_CLASSES];
extern _Atomic long bibopBytesSinceGc;
#ifndef CN1_BIBOP_NO_FASTSWEEP
// Called from monitorEnter (any thread) when a monitor (CN1ThreadData) is freshly
// attached to a heap object. If the object is a BiBOP slot it bumps a global live-monitor
// count so the O(1) all-dead page shortcut is suppressed until every BiBOP monitor has
// been freed by cn1BibopReclaimSlot. No-op for non-BiBOP objects.
extern void cn1BibopNoteMonitorAttached(JAVA_OBJECT obj);
#endif
// Monitor side table (relocated __codenameOneThreadData out of the per-object header).
extern void* cn1MonitorDataGet(JAVA_OBJECT o);
extern void cn1MonitorDataSet(JAVA_OBJECT o, void* data);
extern void* cn1MonitorDataRemove(JAVA_OBJECT o);
extern long long allocationsSinceLastGC;
extern long long totalAllocations;

// LEVER A (perf-tier1, -DCN1_DEATOMIC_BYTES): per-object BiBOP byte accounting.
// CN1_BIBOP_ACCOUNT_BYTES is called once per allocation (inline fast path AND the
// .m slow path); CN1_BIBOP_FLUSH_BYTES is called once per page-acquire (slow path)
// and at thread death. The global bibopBytesSinceGc is read only by the GC-trigger
// heuristic (cn1BibopMaybeGc) and reset to 0 by the sweep -- it has NO liveness/
// correctness role -- so deferring the per-thread total into it via plain adds and
// flushing it in bulk is safe; only the trigger cadence shifts (by < nthreads*page,
// negligible vs the 24MB trigger, and already racy today). The bump cursor / mark
// publication ordering is UNCHANGED (those are the GC-visible fields; see report).
#ifndef CN1_DISABLE_DEATOMIC_BYTES
#define CN1_BIBOP_ACCOUNT_BYTES(ts, n) do { (ts)->bibopBytesLocal += (JAVA_LONG)(n); } while(0)
// Flush the per-thread byte accumulator AND, in the same bulk step, the
// isHighFrequencyGC heuristic counters (allocationsSinceLastGC/totalAllocations) --
// which used to be two global stores per object on the hot path. Coarsening them to
// once-per-page-acquire is fine: both are pure heuristics with no correctness role.
#define CN1_BIBOP_FLUSH_BYTES(ts) do { \
    JAVA_LONG __bl = (ts)->bibopBytesLocal; \
    if(__bl) { \
        atomic_fetch_add_explicit(&bibopBytesSinceGc, __bl, memory_order_relaxed); \
        allocationsSinceLastGC += __bl; \
        totalAllocations += __bl; \
        (ts)->bibopBytesLocal = 0; } } while(0)
#else
#define CN1_BIBOP_ACCOUNT_BYTES(ts, n) do { \
    atomic_fetch_add_explicit(&bibopBytesSinceGc, (n), memory_order_relaxed); \
    allocationsSinceLastGC += (n); totalAllocations += (n); } while(0)
#define CN1_BIBOP_FLUSH_BYTES(ts) do {} while(0)
#endif

// Inlined bump fast path. Returns 0 (slow path: page full / free-list present /
// ineligible / oversized) -> caller falls back to __NEW_X / codenameOneGcMalloc.
static inline JAVA_OBJECT cn1BibopFastAlloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent, int ci) {
    if(ci < 0) return (JAVA_OBJECT)0; // oversized: folded away for big types
    CN1BibopPage* p = bibopCurrent[ci];
    if(__builtin_expect(p != (CN1BibopPage*)0 && p->freeList == (void*)0 &&
                        constantPoolObjects != (JAVA_OBJECT*)0
#ifndef CN1_CONSERVATIVE_GC_ROOTS
                        && !threadStateData->nativeAllocationMode
#endif
                        , 1)) {
        int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
        if(__builtin_expect(bi < p->slotCount, 1)) {
            JAVA_OBJECT o = (JAVA_OBJECT)((char*)p + p->firstSlotOffset + (long)bi * p->slotSize);
#ifdef CN1_BIBOP_VALIDATE
            // INVARIANT: the per-thread current page must be OWNED and match this
            // size class, and the bumped slot must lie inside the page. A violation
            // means bibopCurrent[ci] points at a retired/recycled/reformatted page
            // (the intermittent x64 cn1BibopFastAlloc crash). Abort AT the source,
            // in a normal (non-ASan) build so ASan's layout changes can't mask it.
            if(p->classIndex != ci || p->owned != JAVA_TRUE ||
               (char*)o < (char*)p + p->firstSlotOffset ||
               (char*)o + p->slotSize > (char*)p + CN1_BIBOP_PAGE_SIZE) {
                fprintf(stderr, "CN1BIBOP FASTALLOC CORRUPT: ci=%d p=%p classIndex=%d owned=%d "
                        "bi=%d slotSize=%d slotCount=%d firstSlotOffset=%d o=%p pageEnd=%p\n",
                        ci, (void*)p, p->classIndex, (int)p->owned, bi, p->slotSize,
                        p->slotCount, p->firstSlotOffset, (void*)o,
                        (void*)((char*)p + CN1_BIBOP_PAGE_SIZE));
                fflush(stderr);
                abort();
            }
#endif
            int hdr = (int)sizeof(struct JavaObjectPrototype);
            if(size > hdr) {
                // NOT removable: skipping this is ~2x SLOWER -- uninitialized ref
                // fields get scanned during the mark==-1 grace window and retain
                // floating garbage. The body zero is load-bearing, not overhead.
                memset((char*)o + hdr, 0, size - hdr);
            }
            o->__codenameOneParentClsReference = parent;
            // __codenameOneReferenceCount + __codenameOneThreadData relocated out of the
            // header (force-visited / monitor side tables); no per-object stores.
            o->__heapPosition = CN1_BIBOP_HEAP_POS;
#ifdef DEBUG_GC_ALLOCATIONS
            o->className = threadStateData->callStackClass[threadStateData->callStackOffset - 1];
            o->line = threadStateData->callStackLine[threadStateData->callStackOffset - 1];
#endif
            __atomic_store_n(&o->__codenameOneGcMark, -1, __ATOMIC_RELEASE);
            atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_release);
#ifndef CN1_BIBOP_NO_FASTSWEEP
            // Mark the page dirty so the O(1) sweep never treats a page that still has
            // fresh mark==-1 (grace-candidate) slots as homogeneous. Single plain store
            // to the already-hot page header; published to the GC by the eventual
            // retire release-push.
            p->gcAllocedSinceSweep = JAVA_TRUE;
#endif
            CN1_BIBOP_ACCOUNT_BYTES(threadStateData, p->slotSize);
            // allocationsSinceLastGC / totalAllocations (the isHighFrequencyGC heuristic)
            // are now bumped in bulk by CN1_BIBOP_FLUSH_BYTES once per page-acquire, not
            // per object -- removing two global-counter stores from the hot path.
            return o;
        }
    }
    return (JAVA_OBJECT)0;
}

// -------------------------------------------------------------------------
// PER-OBJECT MEMSET ELIMINATION (perf-tier1, init-before-publish).
// cn1BibopFastAllocNoZero is bit-for-bit cn1BibopFastAlloc WITHOUT the body
// memset. Its use is ONLY sound under the init-before-publish discipline the
// translator emits for it (BytecodeMethod.markInitBeforePublish): the object is
// built in a C temp and every field is either written by the inlined constructor
// or explicitly zeroed BEFORE the object is published as a GC root (written to
// an operand-stack slot). Between this alloc and that publish the emitted C is
// straight-line loads/stores ONLY -- no calls, no throws, no safepoint (ctor
// args that may call or throw are hoisted into temps BEFORE this alloc, see
// InlinableConstructor.appendArgTemps) -- and the object is not reachable from
// any root, so neither the precise nor the conservative-native-stack collector
// can trace its (garbage) body: conservative scans only run on threads stopped
// at a safepoint, and this window contains none. The mark==-1 grace window
// additionally keeps the object alive across a sweep (see the "load-bearing
// memset" note in cn1BibopFastAlloc and OVERFLOW RESCAN in cn1_globals.m). The
// header (parentCls / mark / heapPosition) is still initialized here; ONLY the
// body zero is elided.
static inline JAVA_OBJECT cn1BibopFastAllocNoZero(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent, int ci) {
    if(ci < 0) return (JAVA_OBJECT)0; // oversized: folded away for big types
    CN1BibopPage* p = bibopCurrent[ci];
    if(__builtin_expect(p != (CN1BibopPage*)0 && p->freeList == (void*)0 &&
                        constantPoolObjects != (JAVA_OBJECT*)0
#ifndef CN1_CONSERVATIVE_GC_ROOTS
                        && !threadStateData->nativeAllocationMode
#endif
                        , 1)) {
        int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
        if(__builtin_expect(bi < p->slotCount, 1)) {
            JAVA_OBJECT o = (JAVA_OBJECT)((char*)p + p->firstSlotOffset + (long)bi * p->slotSize);
#ifdef CN1_BIBOP_VALIDATE
            if(p->classIndex != ci || p->owned != JAVA_TRUE ||
               (char*)o < (char*)p + p->firstSlotOffset ||
               (char*)o + p->slotSize > (char*)p + CN1_BIBOP_PAGE_SIZE) {
                fprintf(stderr, "CN1BIBOP NOZERO CORRUPT: ci=%d p=%p classIndex=%d owned=%d "
                        "bi=%d slotSize=%d slotCount=%d firstSlotOffset=%d o=%p\n",
                        ci, (void*)p, p->classIndex, (int)p->owned, bi, p->slotSize,
                        p->slotCount, p->firstSlotOffset, (void*)o);
                fflush(stderr);
                abort();
            }
#endif
            // BODY MEMSET ELIDED (init-before-publish -- see comment above).
            // parentCls is deliberately left 0 UNTIL THE PUBLISH: a thread can be
            // SIGNAL-STOPPED at an arbitrary instruction inside the construction
            // window, and the conservative scan then resolves this slot (heapPosition
            // is already CN1_BIBOP_HEAP_POS) and calls gcMarkObject on it -- whose
            // parentCls==0 guard is the ONLY thing preventing it from tracing the
            // garbage body. The translator stores &class__X right before publishing
            // the fully-built object (InlinableConstructor.appendInitBeforePublish);
            // the mark==-1 grace keeps the object alive through the skipped cycle.
            // The explicit 0 store matters: a bump slot recycled by the O(1)
            // homogeneous page reclaim still holds the DEAD previous occupant's
            // class pointer.
            o->__codenameOneParentClsReference = (struct clazz*)0;
            o->__heapPosition = CN1_BIBOP_HEAP_POS;
#ifdef DEBUG_GC_ALLOCATIONS
            o->className = threadStateData->callStackClass[threadStateData->callStackOffset - 1];
            o->line = threadStateData->callStackLine[threadStateData->callStackOffset - 1];
#endif
            __atomic_store_n(&o->__codenameOneGcMark, -1, __ATOMIC_RELEASE);
            atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_release);
#ifndef CN1_BIBOP_NO_FASTSWEEP
            p->gcAllocedSinceSweep = JAVA_TRUE;
#endif
            CN1_BIBOP_ACCOUNT_BYTES(threadStateData, p->slotSize);
            return o;
        }
    }
    return (JAVA_OBJECT)0;
}

// CN1_FAST_NEW(X): inlined alloc + static-init guard for a NEW of concrete type
// X. The static initializer is invoked only when the class isn't initialised yet
// (the bump fast path can be reached for a class whose <clinit> hasn't run,
// because bibopCurrent[] is shared across all classes of the same size class).
#ifndef CN1_DISABLE_INLINE_ALLOC
#define CN1_FAST_NEW(X) ({ \
    if(__builtin_expect(!class__##X.initialized, 0)) __STATIC_INITIALIZER_##X(threadStateData); \
    JAVA_OBJECT __cn1fo = cn1BibopFastAlloc(threadStateData, sizeof(struct obj__##X), &class__##X, CN1_BIBOP_CIDX(sizeof(struct obj__##X))); \
    if(__builtin_expect(__cn1fo == (JAVA_OBJECT)0, 0)) __cn1fo = __NEW_##X(threadStateData); \
    __cn1fo; })
// No-body-zero variant (init-before-publish). The slow-path fallback __NEW_X
// still fully zeroes (calloc) -- correct, just un-elided on the rare page-full
// path.
#define CN1_FAST_NEW_NOZERO(X) ({ \
    if(__builtin_expect(!class__##X.initialized, 0)) __STATIC_INITIALIZER_##X(threadStateData); \
    JAVA_OBJECT __cn1fo = cn1BibopFastAllocNoZero(threadStateData, sizeof(struct obj__##X), &class__##X, CN1_BIBOP_CIDX(sizeof(struct obj__##X))); \
    if(__builtin_expect(__cn1fo == (JAVA_OBJECT)0, 0)) __cn1fo = __NEW_##X(threadStateData); \
    __cn1fo; })
#else
#define CN1_FAST_NEW(X) __NEW_##X(threadStateData)
#define CN1_FAST_NEW_NOZERO(X) __NEW_##X(threadStateData)
#endif

#define CN1_THREAD_STATE_SINGLE_ARG CODENAME_ONE_THREAD_STATE
#define CN1_THREAD_STATE_MULTI_ARG CODENAME_ONE_THREAD_STATE,
#define CN1_THREAD_STATE_PASS_ARG threadStateData,
#define CN1_THREAD_STATE_PASS_SINGLE_ARG threadStateData
#define CN1_THREAD_GET_STATE_PASS_ARG getThreadLocalData(),
#define CN1_THREAD_GET_STATE_PASS_SINGLE_ARG getThreadLocalData()
#define CN1_YIELD_THREAD getThreadLocalData()->threadActive = JAVA_FALSE;
#define CN1_RESUME_THREAD while (getThreadLocalData()->threadBlockedByGC){ usleep((JAVA_INT)1000);} getThreadLocalData()->threadActive = JAVA_TRUE;

extern struct ThreadLocalData* getThreadLocalData();

/* Monitor-ownership identity for the reentrancy check in monitorEnter/wait.
 * This MUST identify the executing pthread, not the ThreadLocalData struct:
 * one pthread can run under two states (the main-thread-hosted EDT executes
 * with an explicitly-passed CodenameOneThread state, while natives calling
 * getThreadLocalData() get the pthread's own TLS struct), and an id-based
 * check then misses the reentrant case and self-deadlocks. pthread_t is a
 * pointer on Apple/glibc targets; the Windows compat shim's pthread_t is a
 * struct, so there GetCurrentThreadId() supplies the per-thread identity. */
#if defined(_WIN32)
/* The compat shim's pthread_t is {handle, id}; id is GetCurrentThreadId(),
 * unique per thread. Using the shim keeps windows.h out of this header. */
#define CN1_MONITOR_SELF() ((JAVA_LONG)pthread_self().id)
#else
#define CN1_MONITOR_SELF() ((JAVA_LONG)(uintptr_t)pthread_self())
#endif


#define BEGIN_TRY(classId, destinationJump) {\
        threadStateData->blocks[threadStateData->tryBlockOffset].monitor = 0; \
        threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass = classId; \
        memcpy(threadStateData->blocks[threadStateData->tryBlockOffset].destination, destinationJump, sizeof(jmp_buf)); \
        threadStateData->tryBlockOffset++; \
    }

#define JUMP_TO(labelToJumpTo, blockOffsetLevel) {\
        threadStateData->tryBlockOffset = methodBlockOffset + blockOffsetLevel; \
        goto labelToJumpTo; \
    }

static inline void releaseForReturn(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread) {
    threadStateData->threadObjectStackOffset = cn1LocalsBeginInThread;
    threadStateData->callStackOffset--;
}


#define RETURN_AND_RELEASE_FROM_METHOD(returnVal, cn1SizeOfLocals) { \
        releaseForReturn(threadStateData, cn1LocalsBeginInThread); \
        return returnVal; \
    }

#define RETURN_AND_RELEASE_FROM_VOID(cn1SizeOfLocals) { \
        releaseForReturn(threadStateData, cn1LocalsBeginInThread); \
        return; \
    }

extern void releaseForReturnInException(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread, int methodBlockOffset);

#define RETURN_FROM_METHOD(returnVal, cn1SizeOfLocals) releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); \
        return returnVal; \

#define RETURN_FROM_VOID(cn1SizeOfLocals) releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); \
        return; \

#define END_TRY(offset) threadStateData->tryBlockOffset = methodBlockOffset + offset - 1

#define DEFINE_CATCH_BLOCK(destinationJump, labelName, restoreToCn1LocalsBeginInThread) jmp_buf destinationJump; \
{ \
    int currentOffset = threadStateData->tryBlockOffset; \
    if(setjmp(destinationJump)) { \
        threadStateData->callStackOffset = currentCodenameOneCallStackOffset; \
        threadStateData->threadObjectStackOffset = restoreToCn1LocalsBeginInThread; \
        SP = &stack[1]; \
        stack[0].data.o = threadStateData->exception; \
        stack[0].type = CN1_TYPE_OBJECT; \
        goto labelName; \
    } \
}

extern JAVA_VOID java_lang_Throwable_fillInStack__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT ex);


extern void throwException(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exceptionArg);
extern JAVA_INT  throwException_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exceptionArg);
extern JAVA_BOOLEAN  throwException_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exceptionArg);
extern JAVA_OBJECT __NEW_java_lang_NullPointerException(CODENAME_ONE_THREAD_STATE);
extern JAVA_OBJECT __NEW_INSTANCE_java_lang_NullPointerException(CODENAME_ONE_THREAD_STATE);
extern JAVA_OBJECT __NEW_INSTANCE_java_lang_StackOverflowError(CODENAME_ONE_THREAD_STATE);
// Throws the PREALLOCATED StackOverflowError (pre-filled trace, no allocation,
// no trace building) -- safe to call at stack exhaustion. See cn1_globals.m.
extern void cn1ThrowStackOverflow(CODENAME_ONE_THREAD_STATE);
extern JAVA_OBJECT __NEW_java_lang_ArrayIndexOutOfBoundsException(CODENAME_ONE_THREAD_STATE);
extern JAVA_VOID java_lang_ArrayIndexOutOfBoundsException___INIT_____int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT __cn1Arg1);
extern void throwArrayIndexOutOfBoundsException(CODENAME_ONE_THREAD_STATE, int index);
extern JAVA_BOOLEAN throwArrayIndexOutOfBoundsException_R_boolean(CODENAME_ONE_THREAD_STATE, int index);
#define THROW_NULL_POINTER_EXCEPTION()    throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData))

#define THROW_ARRAY_INDEX_EXCEPTION(index)    throwArrayIndexOutOfBoundsException(threadStateData, index)

#ifdef CN1_INCLUDE_NPE_CHECKS
    #define CHECK_NPE_TOP_OF_STACK() if(SP[-1].data.o == JAVA_NULL) { NSLog(@"Throwing NullPointerException!"); throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)); }
    #define CHECK_NPE_AT_STACK(pos) if(SP[-pos].data.o == JAVA_NULL) { NSLog(@"Throwing NullPointerException!"); throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)); }

    #ifdef CN1_INCLUDE_ARRAY_BOUND_CHECKS
        #define CHECK_ARRAY_ACCESS(array_pos, bounds) if(SP[- array_pos].data.o == JAVA_NULL) { NSLog(@"Throwing NullPointerException!"); throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)); } \
            if(bounds < 0 || bounds >= ((JAVA_ARRAY)SP[- array_pos].data.o)->length) { THROW_ARRAY_INDEX_EXCEPTION(bounds); }
        #define CHECK_ARRAY_ACCESS_EXPR(array, bounds) ((array == JAVA_NULL) ? throwException_R_boolean(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)) : (bounds < 0 || bounds >= ((JAVA_ARRAY)array)->length) ? throwArrayIndexOutOfBoundsException_R_boolean(threadStateData, bounds) : JAVA_TRUE)
        #define CHECK_ARRAY_ACCESS_WITH_ARGS(array, bounds) if(array == JAVA_NULL) { NSLog(@"Throwing NullPointerException!"); throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)); } \
            if(bounds < 0 || bounds >= ((JAVA_ARRAY)array)->length) { THROW_ARRAY_INDEX_EXCEPTION(bounds); }
        // DIVERGING check for FRAMELESS methods only: the failure path throws and
        // RETURNS from the (frame-free) method instead of falling through. That
        // keeps the throwException CALL out of every loop cycle, so clang can
        // hoist the array header loads (data/length) that the merging accessors
        // (cn1_array_element_*) force it to reload each iteration. Mirrors the
        // CN1_FRAMELESS_SOE_GUARD throw-and-return pattern.
        #define CN1_ARRAY_CHECK_DIVERGE(array, bounds, retval) \
            if(__builtin_expect(array == JAVA_NULL, 0)) { THROW_NULL_POINTER_EXCEPTION(); return retval; } \
            if(__builtin_expect(((unsigned int)(bounds)) >= (unsigned int)(((JAVA_ARRAY)array)->length), 0)) { THROW_ARRAY_INDEX_EXCEPTION(bounds); return retval; }
    #else
        #define CHECK_ARRAY_ACCESS(array_pos, bounds) if(SP[-array_pos].data.o == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); }
        #define CHECK_ARRAY_ACCESS_EXPR(array, bounds) ((array == JAVA_NULL) ? throwException_R_boolean(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)) : JAVA_TRUE)
        #define CHECK_ARRAY_ACCESS_WITH_ARGS(array, bounds) if(array == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); }
        #define CN1_ARRAY_CHECK_DIVERGE(array, bounds, retval) \
            if(__builtin_expect(array == JAVA_NULL, 0)) { THROW_NULL_POINTER_EXCEPTION(); return retval; }
    #endif
#else
    #define CHECK_NPE_TOP_OF_STACK()
    #define CHECK_NPE_AT_STACK(pos)
    #define CHECK_ARRAY_ACCESS(array_pos, bounds)
    #define CHECK_ARRAY_ACCESS_EXPR(array, bounds) JAVA_TRUE
    #define CHECK_ARRAY_ACCESS_WITH_ARGS(array, bounds)
    #define CN1_ARRAY_CHECK_DIVERGE(array, bounds, retval)
#endif

#ifdef CN1_INCLUDE_ARRAY_BOUND_CHECKS
    #define CHECK_ARRAY_BOUNDS_AT_STACK(pos, bounds) if(bounds < 0 || bounds >= ((JAVA_ARRAY)PEEK_OBJ(pos))->length) { THROW_ARRAY_INDEX_EXCEPTION(bounds); }
#else
    #define CHECK_ARRAY_BOUNDS_AT_STACK(pos, bounds)
#endif

#ifdef CN1_INCLUDE_NPE_CHECKS
#define CN1_ARRAY_LENGTH(array) ((array == JAVA_NULL) ? throwException_R_int(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)) : (*((JAVA_ARRAY)array)).length)
#else
#define CN1_ARRAY_LENGTH(array) ((*((JAVA_ARRAY)array)).length)
#endif

static inline JAVA_BOOLEAN cn1_array_access_in_bounds(JAVA_OBJECT array, JAVA_INT index) {
    return array != JAVA_NULL && index >= 0 && index < ((JAVA_ARRAY)array)->length;
}

static inline JAVA_BOOLEAN cn1_array_access_validate(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (array == JAVA_NULL) {
        throwException(threadStateData, __NEW_java_lang_NullPointerException(threadStateData));
        return JAVA_FALSE;
    }
    if (index < 0 || index >= ((JAVA_ARRAY)array)->length) {
        throwArrayIndexOutOfBoundsException(threadStateData, index);
        return JAVA_FALSE;
    }
    return JAVA_TRUE;
}

static inline JAVA_INT cn1_array_element_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_INT*) (*(JAVA_ARRAY)array).data)[index];
}

static inline JAVA_BYTE cn1_array_element_byte(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY)array).data)[index];
}

static inline JAVA_FLOAT cn1_array_element_float(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_FLOAT*) (*(JAVA_ARRAY)array).data)[index];
}

static inline JAVA_DOUBLE cn1_array_element_double(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_DOUBLE*) (*(JAVA_ARRAY)array).data)[index];
}

static inline JAVA_LONG cn1_array_element_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_LONG*) (*(JAVA_ARRAY)array).data)[index];
}

static inline JAVA_OBJECT cn1_array_element_object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return JAVA_NULL;
    }
    return ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)array).data)[index];
}

static inline JAVA_SHORT cn1_array_element_short(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_SHORT*) (*(JAVA_ARRAY)array).data)[index];
}

static inline JAVA_CHAR cn1_array_element_char(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY)array).data)[index];
}

static inline JAVA_VOID cn1_set_array_element_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_INT value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_INT*) (*(JAVA_ARRAY)array).data)[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_byte(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_BYTE value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY)array).data)[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_float(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_FLOAT value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_FLOAT*) (*(JAVA_ARRAY)array).data)[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_double(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_DOUBLE value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_DOUBLE*) (*(JAVA_ARRAY)array).data)[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_LONG value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_LONG*) (*(JAVA_ARRAY)array).data)[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_OBJECT value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    CN1_WRITE_BARRIER(array, value); // nursery: storing a ref into a (heap) array escapes
    ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)array).data)[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_short(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_SHORT value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_SHORT*) (*(JAVA_ARRAY)array).data)[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_char(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_CHAR value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY)array).data)[index] = value;
}

#define CN1_ARRAY_ELEMENT_INT(array, index) cn1_array_element_int(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_BYTE(array, index) cn1_array_element_byte(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_FLOAT(array, index) cn1_array_element_float(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_DOUBLE(array, index) cn1_array_element_double(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_LONG(array, index) cn1_array_element_long(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_OBJECT(array, index) cn1_array_element_object(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_SHORT(array, index) cn1_array_element_short(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_CHAR(array, index) cn1_array_element_char(threadStateData, array, index)

// Unchecked array element reads. Emitted by the translator ONLY for accesses the
// prove-safe bounds-check-elimination pass proved are always in range and on a
// non-null array (canonical counted loops indexed by their own induction var,
// bounded by arr.length). No null/bounds branch -> the C compiler is free to keep
// the load in registers and auto-vectorize. If the proof is ever wrong this reads
// out of bounds, so the pass is deliberately conservative and fail-closed.
#define CN1_ARRAY_ELEMENT_INT_NOCHK(array, index) (((JAVA_ARRAY_INT*) (*(JAVA_ARRAY)(array)).data)[(index)])
#define CN1_ARRAY_ELEMENT_BYTE_NOCHK(array, index) (((JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY)(array)).data)[(index)])
#define CN1_ARRAY_ELEMENT_FLOAT_NOCHK(array, index) (((JAVA_ARRAY_FLOAT*) (*(JAVA_ARRAY)(array)).data)[(index)])
#define CN1_ARRAY_ELEMENT_DOUBLE_NOCHK(array, index) (((JAVA_ARRAY_DOUBLE*) (*(JAVA_ARRAY)(array)).data)[(index)])
#define CN1_ARRAY_ELEMENT_LONG_NOCHK(array, index) (((JAVA_ARRAY_LONG*) (*(JAVA_ARRAY)(array)).data)[(index)])
#define CN1_ARRAY_ELEMENT_OBJECT_NOCHK(array, index) (((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)(array)).data)[(index)])
#define CN1_ARRAY_ELEMENT_SHORT_NOCHK(array, index) (((JAVA_ARRAY_SHORT*) (*(JAVA_ARRAY)(array)).data)[(index)])
#define CN1_ARRAY_ELEMENT_CHAR_NOCHK(array, index) (((JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY)(array)).data)[(index)])

#define CN1_SET_ARRAY_ELEMENT_INT(array, index, value) cn1_set_array_element_int(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_BYTE(array, index, value) cn1_set_array_element_byte(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_FLOAT(array, index, value) cn1_set_array_element_float(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_DOUBLE(array, index, value) cn1_set_array_element_double(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_LONG(array, index, value) cn1_set_array_element_long(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_OBJECT(array, index, value) cn1_set_array_element_object(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_SHORT(array, index, value) cn1_set_array_element_short(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_CHAR(array, index, value) cn1_set_array_element_char(threadStateData, array, index, value)

extern JAVA_VOID monitorEnter(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);
extern JAVA_VOID monitorExit(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);
extern JAVA_VOID monitorEnterBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);
extern JAVA_VOID monitorExitBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);

extern void arrayFinalizerFunction(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array);


#define MONITOR_ENTER() monitorEnter(threadStateData, POP_OBJ())
#define MONITOR_EXIT() monitorExit(threadStateData, POP_OBJ())

extern void gcReleaseObj(JAVA_OBJECT o);

extern JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim);
extern JAVA_OBJECT allocArrayAligned(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim, int alignment);
// Fused-object block allocator (owner + encapsulated child in ONE BiBOP slot;
// see the FUSED OBJECTS comment in cn1_globals.m). NULL => caller must fall
// back to ordinary separate allocations (oversize / BiBOP unavailable).
extern JAVA_OBJECT cn1AllocFused(CODENAME_ONE_THREAD_STATE, int totalSize, struct clazz* cls);

// Bytes a fused primitive-array child occupies inside the owner block: array
// header + the data-pointer skip allocArray uses + elements, 8-aligned so a
// following child's header is aligned.
#define CN1_FUSED_ARR_BYTES(len, esz) \
    ((int)((sizeof(struct JavaArrayPrototype) + sizeof(void*) + (size_t)(len) * (esz) + 7) & ~(size_t)7))

// Lay out a fused child array INSIDE an owner block freshly returned by
// cn1AllocFused (zeroed, owner parentCls set). The child gets a full ordinary
// array header -- every reader sees a normal array -- but no independent GC
// identity: the page sweep walks slot boundaries only, and the conservative
// resolver maps any pointer into the block to the OWNER (slot base), so the
// child lives and dies with it. heapPosition -1 = never registered; the
// remove/free paths no-op on it. Element placement mirrors allocArray.
static inline JAVA_OBJECT cn1FusedInstallPrimArray(JAVA_OBJECT owner, int off, struct clazz* acls, int esz, int len) {
    struct JavaArrayPrototype* a = (struct JavaArrayPrototype*)((char*)owner + off);
    a->__codenameOneParentClsReference = acls;
    a->__codenameOneGcMark = -1;
    a->__heapPosition = -1;
    a->length = len;
    a->dimensions = 1;
    a->primitiveSize = esz;
    if(len > 0) {
        void* p = (void*)&(a->data);
        p = (char*)p + sizeof(void*);
        a->data = p;
    } else {
        a->data = 0;
    }
    return (JAVA_OBJECT)a;
}
// Register an object referenced only from C globals as a permanent GC root.
extern void cn1AddImmortalRoot(JAVA_OBJECT o);
// Flag a BiBOP object's page as holding a native peer (cached NSString etc.):
// its dead slots then always reach cn1BibopReclaimSlot, which releases the
// peer -- instead of the O(1) all-dead page reclaim, which would leak it.
extern void cn1BibopNoteNativePeer(JAVA_OBJECT o);
extern JAVA_OBJECT allocMultiArray(int* lengths, struct clazz* type, int primitiveSize, int dim);
#define CN1_SIMD_ALIGNMENT 16
/* Maximum payload size we are willing to alloca() on the per-thread stack
 * before falling back to a regular GC-tracked heap allocation. iOS secondary
 * threads default to a 512 KB stack, so any allocation that scales with image
 * dimensions (e.g. createMask / applyMask) can blow the stack at modest sizes
 * (a 410x410 ARGB image needs ~656 KB of int scratch). The cap is intentionally
 * conservative: the fallback path costs a normal heap allocation (cheap
 * relative to the SIMD work that follows it), while a stack overflow is fatal
 * with no chance to recover. */
#define CN1_SIMD_STACK_HEAP_THRESHOLD (32 * 1024)
#define CN1_SIMD_STACK_PRIMITIVE_ARRAY(length, arrayClass, primitiveSize) \
    __extension__ ({ \
        int __cn1StackLength = (length); \
        const int __cn1Alignment = CN1_SIMD_ALIGNMENT; \
        int __cn1ActualSize = __cn1StackLength * (primitiveSize); \
        JAVA_OBJECT __cn1Result; \
        if (__cn1StackLength < 0 || __cn1ActualSize > CN1_SIMD_STACK_HEAP_THRESHOLD) { \
            /* Too large to safely place on the stack - fall back to a regular */ \
            /* aligned heap allocation. The returned array still satisfies the */ \
            /* SIMD alignment contract; only the lifetime widens (GC-managed */ \
            /* instead of method-local), which is harmless for callers. */ \
            __cn1Result = allocArrayAligned(threadStateData, __cn1StackLength, (arrayClass), (primitiveSize), 1, __cn1Alignment); \
        } else { \
            /* header + embedded data pointer slot + payload + alignment slack for the payload start */ \
            char* __cn1StackMem = (char*)__builtin_alloca(sizeof(struct JavaArrayPrototype) + sizeof(void*) + __cn1ActualSize + __cn1Alignment - 1); \
            JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)__cn1StackMem; \
            *__cn1StackArray = (struct JavaArrayPrototype){DEBUG_GC_INIT (arrayClass), 0, 0, __cn1StackLength, 1, (primitiveSize), 0}; \
            if (__cn1ActualSize > 0) { \
                char* __cn1Data = (char*)(&(__cn1StackArray->data)); \
                __cn1Data += sizeof(void*); \
                /* round the payload start up by adding alignment-1 then masking off the low bits */ \
                uintptr_t __cn1Aligned = (((uintptr_t)__cn1Data) + ((uintptr_t)__cn1Alignment - 1)) & ~((uintptr_t)__cn1Alignment - 1); \
                __cn1StackArray->data = (void*)__cn1Aligned; \
            } else { \
                __cn1StackArray->data = 0; \
            } \
            __cn1Result = (JAVA_OBJECT)__cn1StackArray; \
        } \
        __cn1Result; \
    })
#define CN1_SIMD_ALLOCA_BYTE(length) CN1_SIMD_STACK_PRIMITIVE_ARRAY((length), &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE))
#define CN1_SIMD_ALLOCA_INT(length) CN1_SIMD_STACK_PRIMITIVE_ARRAY((length), &class_array1__JAVA_INT, sizeof(JAVA_ARRAY_INT))
#define CN1_SIMD_ALLOCA_FLOAT(length) CN1_SIMD_STACK_PRIMITIVE_ARRAY((length), &class_array1__JAVA_FLOAT, sizeof(JAVA_ARRAY_FLOAT))
#define CN1_SIMD_ALLOCA_BYTE_ZEROED(length) \
    __extension__ ({ \
        int __cn1InitLength = (length); \
        JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)CN1_SIMD_ALLOCA_BYTE(__cn1InitLength); \
        if (__cn1InitLength > 0) { \
            memset(__cn1StackArray->data, 0, (size_t)__cn1InitLength); \
        } \
        (JAVA_OBJECT)__cn1StackArray; \
    })
#define CN1_SIMD_ALLOCA_INT_ZEROED(length) \
    __extension__ ({ \
        int __cn1InitLength = (length); \
        JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)CN1_SIMD_ALLOCA_INT(__cn1InitLength); \
        if (__cn1InitLength > 0) { \
            memset(__cn1StackArray->data, 0, (size_t)__cn1InitLength * sizeof(JAVA_ARRAY_INT)); \
        } \
        (JAVA_OBJECT)__cn1StackArray; \
    })
#define CN1_SIMD_ALLOCA_FLOAT_ZEROED(length) \
    __extension__ ({ \
        int __cn1InitLength = (length); \
        JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)CN1_SIMD_ALLOCA_FLOAT(__cn1InitLength); \
        if (__cn1InitLength > 0) { \
            memset(__cn1StackArray->data, 0, (size_t)__cn1InitLength * sizeof(JAVA_ARRAY_FLOAT)); \
        } \
        (JAVA_OBJECT)__cn1StackArray; \
    })
#define CN1_SIMD_ALLOCA_BYTE_FILLED(length, value) \
    __extension__ ({ \
        int __cn1InitLength = (length); \
        JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)CN1_SIMD_ALLOCA_BYTE(__cn1InitLength); \
        if (__cn1InitLength > 0) { \
            memset(__cn1StackArray->data, (value), (size_t)__cn1InitLength); \
        } \
        (JAVA_OBJECT)__cn1StackArray; \
    })
#define CN1_SIMD_ALLOCA_INT_FILLED(length, value) \
    __extension__ ({ \
        int __cn1InitLength = (length); \
        JAVA_ARRAY_INT __cn1InitValue = (value); \
        JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)CN1_SIMD_ALLOCA_INT(__cn1InitLength); \
        JAVA_ARRAY_INT* __cn1Data = (JAVA_ARRAY_INT*)__cn1StackArray->data; \
        if (__cn1InitValue == 0 && __cn1InitLength > 0) { \
            memset(__cn1StackArray->data, 0, (size_t)__cn1InitLength * sizeof(JAVA_ARRAY_INT)); \
        } else { \
            for (int __cn1FillIndex = 0; __cn1FillIndex < __cn1InitLength; __cn1FillIndex++) { \
                __cn1Data[__cn1FillIndex] = __cn1InitValue; \
            } \
        } \
        (JAVA_OBJECT)__cn1StackArray; \
    })
#define CN1_SIMD_ALLOCA_FLOAT_FILLED(length, value) \
    __extension__ ({ \
        int __cn1InitLength = (length); \
        JAVA_ARRAY_FLOAT __cn1InitValue = (value); \
        JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)CN1_SIMD_ALLOCA_FLOAT(__cn1InitLength); \
        JAVA_ARRAY_FLOAT* __cn1Data = (JAVA_ARRAY_FLOAT*)__cn1StackArray->data; \
        if (__cn1InitValue == 0.0f && __cn1InitLength > 0) { \
            memset(__cn1StackArray->data, 0, (size_t)__cn1InitLength * sizeof(JAVA_ARRAY_FLOAT)); \
        } else { \
            for (int __cn1FillIndex = 0; __cn1FillIndex < __cn1InitLength; __cn1FillIndex++) { \
                __cn1Data[__cn1FillIndex] = __cn1InitValue; \
            } \
        } \
        (JAVA_OBJECT)__cn1StackArray; \
    })
extern JAVA_OBJECT alloc2DArray(CODENAME_ONE_THREAD_STATE, int length1, int length2, struct clazz* parentType, struct clazz* childType, int primitiveSize);
extern JAVA_OBJECT alloc3DArray(CODENAME_ONE_THREAD_STATE, int length1, int length2, int length3, struct clazz* parentType, struct clazz* childType, struct clazz* grandChildType, int primitiveSize);

extern void lockCriticalSection();
extern void unlockCriticalSection();
extern void lockThreadHeapMutex();
extern void unlockThreadHeapMutex();

extern struct clazz class_array1__JAVA_BOOLEAN;
extern struct clazz class_array2__JAVA_BOOLEAN;
extern struct clazz class_array3__JAVA_BOOLEAN;

extern struct clazz class_array1__JAVA_CHAR;
extern struct clazz class_array2__JAVA_CHAR;
extern struct clazz class_array3__JAVA_CHAR;

extern struct clazz class_array1__JAVA_BYTE;
extern struct clazz class_array2__JAVA_BYTE;
extern struct clazz class_array3__JAVA_BYTE;

extern struct clazz class_array1__JAVA_SHORT;
extern struct clazz class_array2__JAVA_SHORT;
extern struct clazz class_array3__JAVA_SHORT;

extern struct clazz class_array1__JAVA_INT;
extern struct clazz class_array2__JAVA_INT;
extern struct clazz class_array3__JAVA_INT;

extern struct clazz class_array1__JAVA_LONG;
extern struct clazz class_array2__JAVA_LONG;
extern struct clazz class_array3__JAVA_LONG;

extern struct clazz class_array1__JAVA_FLOAT;
extern struct clazz class_array2__JAVA_FLOAT;
extern struct clazz class_array3__JAVA_FLOAT;

extern struct clazz class_array1__JAVA_DOUBLE;
extern struct clazz class_array2__JAVA_DOUBLE;
extern struct clazz class_array3__JAVA_DOUBLE;

extern JAVA_OBJECT newString(CODENAME_ONE_THREAD_STATE, int length, JAVA_CHAR data[]);
extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char *str);
extern void initConstantPool();

extern void initMethodStack(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, int stackSize, int localsStackSize, int classNameId, int methodNameId);
static inline void cn1_init_method_stack_fast(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, int stackSize, int localsStackSize, JAVA_BOOLEAN fullClear) {
#ifdef CN1_INCLUDE_NPE_CHECKS
    if(__cn1ThisObject == JAVA_NULL) {
        THROW_NULL_POINTER_EXCEPTION();
    }
#endif
    if (threadStateData->callStackOffset >= CN1_STACK_OVERFLOW_CALL_DEPTH_LIMIT - 1) {
        cn1ThrowStackOverflow(threadStateData);
        return;
    }
    /* The call-depth guard above does not protect the operand/locals stack: a
     * deep recursion of methods with large frames can exhaust threadObjectStack
     * before the call-depth limit, and without this check initMethodStack would
     * memset/write past the buffer end -> access violation instead of a catchable
     * StackOverflowError. The 1024-slot margin leaves room to build+throw it. */
    if (threadStateData->threadObjectStackOffset + localsStackSize + stackSize >= CN1_MAX_OBJECT_STACK_DEPTH - 1024) {
        cn1ThrowStackOverflow(threadStateData);
        return;
    }
    if (fullClear) {
        memset(&threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset], 0,
                sizeof(struct elementStruct) * (localsStackSize + stackSize));
    } else {
        /*
         * Primitive-only fast frames intentionally use the same memset strategy.
         * A per-slot type-only loop was measurably slower in benchmarks and did
         * not improve generated-code performance.
         */
        memset(&threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset], 0,
                sizeof(struct elementStruct) * (localsStackSize + stackSize));
    }
    threadStateData->threadObjectStackOffset += localsStackSize + stackSize;
    threadStateData->callStackOffset++;
}

// Inline frame setup WITH stack-trace name recording. Methods that make calls can't use
// the fast leaf frame (the trace must keep their frame), but they were paying a non-inline
// initMethodStack() call per invocation -- brutal for hot recursive methods (fib: ~30M
// calls, two extern calls each with releaseForReturn). This inlines it so the C compiler
// folds the offset arithmetic and the call overhead disappears.
static inline void cn1InitMethodStackInline(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, int stackSize, int localsStackSize, int classNameId, int methodNameId) {
#ifdef CN1_INCLUDE_NPE_CHECKS
    if(__cn1ThisObject == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); }
#endif
    if (threadStateData->callStackOffset >= CN1_STACK_OVERFLOW_CALL_DEPTH_LIMIT - 1) {
        cn1ThrowStackOverflow(threadStateData);
        return;
    }
    if (threadStateData->threadObjectStackOffset + localsStackSize + stackSize >= CN1_MAX_OBJECT_STACK_DEPTH - 1024) {
        cn1ThrowStackOverflow(threadStateData);
        return;
    }
    memset(&threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset], 0, sizeof(struct elementStruct) * (localsStackSize + stackSize));
    threadStateData->threadObjectStackOffset += localsStackSize + stackSize;
    threadStateData->callStackClass[threadStateData->callStackOffset] = classNameId;
    threadStateData->callStackMethod[threadStateData->callStackOffset] = methodNameId;
    threadStateData->callStackOffset++;
}

// we need to zero out the values with memset otherwise we will run into a problem
// when invoking release on pre-existing object which might be garbage
#define DEFINE_METHOD_STACK(stackSize, localsStackSize, spPosition, classNameId, methodNameId) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    struct elementStruct* SP = &stack[spPosition]; \
    cn1InitMethodStackInline(threadStateData, (JAVA_OBJECT)1, stackSize, localsStackSize, classNameId, methodNameId); \
    const int currentCodenameOneCallStackOffset = threadStateData->callStackOffset;\
    int methodBlockOffset = threadStateData->tryBlockOffset;

#define DEFINE_INSTANCE_METHOD_STACK(stackSize, localsStackSize, spPosition, classNameId, methodNameId) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    struct elementStruct* SP = &stack[spPosition]; \
    cn1InitMethodStackInline(threadStateData, __cn1ThisObject, stackSize, localsStackSize, classNameId, methodNameId); \
    const int currentCodenameOneCallStackOffset = threadStateData->callStackOffset;\
    int methodBlockOffset = threadStateData->tryBlockOffset;

#define DEFINE_METHOD_STACK_FAST_REF(stackSize, localsStackSize, spPosition) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    struct elementStruct* SP = &stack[spPosition]; \
    cn1_init_method_stack_fast(threadStateData, (JAVA_OBJECT)1, stackSize, localsStackSize, JAVA_TRUE); \
    const int currentCodenameOneCallStackOffset = threadStateData->callStackOffset;\
    int methodBlockOffset = threadStateData->tryBlockOffset;

#define DEFINE_INSTANCE_METHOD_STACK_FAST_REF(stackSize, localsStackSize, spPosition) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    struct elementStruct* SP = &stack[spPosition]; \
    cn1_init_method_stack_fast(threadStateData, __cn1ThisObject, stackSize, localsStackSize, JAVA_TRUE); \
    const int currentCodenameOneCallStackOffset = threadStateData->callStackOffset;\
    int methodBlockOffset = threadStateData->tryBlockOffset;

#define DEFINE_METHOD_STACK_FAST_PRIMITIVE(stackSize, localsStackSize, spPosition) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    struct elementStruct* SP = &stack[spPosition]; \
    cn1_init_method_stack_fast(threadStateData, (JAVA_OBJECT)1, stackSize, localsStackSize, JAVA_FALSE); \
    const int currentCodenameOneCallStackOffset = threadStateData->callStackOffset;\
    int methodBlockOffset = threadStateData->tryBlockOffset;

#define DEFINE_INSTANCE_METHOD_STACK_FAST_PRIMITIVE(stackSize, localsStackSize, spPosition) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    struct elementStruct* SP = &stack[spPosition]; \
    cn1_init_method_stack_fast(threadStateData, __cn1ThisObject, stackSize, localsStackSize, JAVA_FALSE); \
    const int currentCodenameOneCallStackOffset = threadStateData->callStackOffset;\
    int methodBlockOffset = threadStateData->tryBlockOffset;

#define CN1_FAST_RETURN_RELEASE() \
    threadStateData->threadObjectStackOffset = cn1LocalsBeginInThread; \
    threadStateData->callStackOffset--;

// === Frameless frame (primitive-only static methods) ========================
// A method whose frame holds ZERO object references contributes no GC roots, so
// the precise collector has nothing to scan there and the per-call frame can be
// eliminated. The operand stack + locals live in a method-LOCAL C-stack array --
// NOT a slice of the global threadObjectStack -- so there is no per-call memset,
// no threadObjectStack offset bump/restore, no callStack class/method push, and
// no callStackOffset bump. The method body (PUSH/POP/SP ops, arithmetic, calls)
// is emitted byte-for-byte unchanged; it just operates on this local SP. Frame
// elimination is GC-trivial here -- it changes nothing the collector sees.
#define DEFINE_METHOD_STACK_FRAMELESS(stackSize, localsStackSize, spPosition) \
    struct elementStruct cn1_frameless_frame[(localsStackSize) + (stackSize)]; \
    struct elementStruct* locals = &cn1_frameless_frame[0]; \
    struct elementStruct* stack = &cn1_frameless_frame[localsStackSize]; \
    struct elementStruct* SP = &stack[spPosition];

// Headroom (bytes) kept below the end of the native C stack: enough to detect the
// overflow and still build + throw the StackOverflowError without overrunning.
#define CN1_FRAMELESS_STACK_GUARD_BAND (256 * 1024)

// Computes (lazily, once per thread) ThreadLocalData.nativeStackLimit. Defined in
// cn1_globals.m so the hot header stays free of pthread stack-introspection.
extern void cn1ComputeNativeStackLimit(CODENAME_ONE_THREAD_STATE);

// Stack-overflow guard emitted at the top of every frameless method. Frameless
// frames don't bump callStackOffset, so the 1024-depth call-limit can't protect
// them; deep non-tail recursion (e.g. fib) would otherwise blow the native C
// stack into a SIGSEGV. Compare the current frame address against the per-thread
// low-water mark and throw a catchable StackOverflowError before that happens.
// Cost on the hot path: one load + a predicted-not-taken branch. `retval` is the
// method's default return ('' for void, 0 for primitives); throwException normally
// longjmps, so the return is just the unreachable fall-through the compiler needs.
// The trip test is TWO-SIDED: only an address inside the guard band
// [limit - BAND, limit) throws. A one-sided (addr < limit) test misfires when
// this thread-state executes on a FOREIGN stack -- iOS natives dispatch_sync
// blocks onto the main queue and call Java helpers with the EDT's captured
// threadStateData, so the main thread's frame addresses were compared against
// the EDT's stack bounds and getBytes/toNSString spuriously threw
// StackOverflowError the first time RichTextArea set a browser page (observed
// as build-ios/metal/mac-native dying at exactly 78 screenshots). A foreign
// stack essentially never maps into the 256KB band of another stack, while a
// genuinely overflowing stack must descend THROUGH the band (no single
// frameless frame approaches 256KB), so overflow detection is preserved.
#define CN1_FRAMELESS_SOE_GUARD(retval) \
    do { \
        if (__builtin_expect(threadStateData->nativeStackLimit == 0, 0)) { cn1ComputeNativeStackLimit(threadStateData); } \
        JAVA_LONG __cn1FrameAddr = (JAVA_LONG)(intptr_t)__builtin_frame_address(0); \
        if (__builtin_expect(__cn1FrameAddr < threadStateData->nativeStackLimit \
                && __cn1FrameAddr >= threadStateData->nativeStackLimit - (JAVA_LONG)CN1_FRAMELESS_STACK_GUARD_BAND, 0)) { \
            cn1ThrowStackOverflow(threadStateData); \
            return retval; \
        } \
    } while(0)


#if defined(__APPLE__) && defined(__OBJC__)
@class NSString;
extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str);
extern NSString* toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o);
#else
#define NSLog(...) printf(__VA_ARGS__); printf("\n")
typedef int BOOL;
#define YES 1
#define NO 0
#endif

extern JAVA_OBJECT __NEW_ARRAY_JAVA_BOOLEAN(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_CHAR(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_BYTE(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_SHORT(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_INT(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_LONG(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_FLOAT(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_DOUBLE(CODENAME_ONE_THREAD_STATE, JAVA_INT size);

extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);

JAVA_OBJECT codenameOneGcMalloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent);
void codenameOneGcFree(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);

extern int currentGcMarkValue;
extern void gcMarkObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force);
extern void gcMarkArrayObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force);
extern JAVA_BOOLEAN removeObjectFromHeapCollection(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o);

extern void codenameOneGCMark();
extern void codenameOneGCSweep();

#ifdef CN1_CONSERVATIVE_GC_ROOTS
// PHASE 3b production conservative-root API. cn1ConservativeResolve maps an
// arbitrary machine word to the base of the live heap object it points into
// (interior pointers included) or JAVA_NULL, dereferencing nothing unproven.
// cn1ConservativeMarkRange reads every aligned word in [lo,hi) and gcMarkObject's
// what it resolves to -- a REAL root source (marks a superset of the precise set,
// so nothing live is ever freed). cn1GcBuildRootSnapshots rebuilds the resolver's
// page/extent index once per GC. cn1GcInstallSignalHandler installs the SIGUSR-based
// universal thread-stop handler (idempotent). See the big block in cn1_globals.m.
extern JAVA_OBJECT cn1ConservativeResolve(void* w);
extern void cn1ConservativeMarkRange(CODENAME_ONE_THREAD_STATE, char* lo, char* hi);
extern void cn1GcBuildRootSnapshots(void);
extern void cn1GcInstallSignalHandler(void);
// Per-thread self pointer, set at thread registration; read async-signal-safely by the
// universal-stop handler.
extern __thread struct ThreadLocalData* cn1TlsSelf;

// Capture a parking mutator's native register file + native-stack low bound so the
// concurrent GC can conservatively scan [sp, stackBase) for native-stack-held roots.
// MUST be a macro so setjmp + the SP marker live in the PARKING frame itself: that
// frame -- and the entire live mutator call chain above it (including any frameless
// object frame whose roots are native-C locals) -- stays resident while the thread
// spins in the GC-wait loop and the GC walks it. gcParkCaptured is published LAST,
// and the GC only scans a thread after observing threadActive==FALSE (set right after
// this macro), so it always reads a complete capture.
#define CN1_GC_PARK_CAPTURE(ts) do { \
        (void)setjmp((ts)->gcRegisterSnapshot); \
        volatile void* cn1__sp = (void*)&cn1__sp; \
        (ts)->gcStackPointerAtPark = (void*)cn1__sp; \
        __atomic_thread_fence(__ATOMIC_RELEASE); \
        (ts)->gcParkCaptured = JAVA_TRUE; \
    } while(0)
#else
#define CN1_GC_PARK_CAPTURE(ts) do {} while(0)
#endif

typedef JAVA_OBJECT (*newInstanceFunctionPointer)(CODENAME_ONE_THREAD_STATE);
typedef JAVA_OBJECT (*enumValueOfFunctionPointer)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);

extern void** initVtableForInterface();

extern JAVA_OBJECT cloneArray(JAVA_OBJECT array);
extern int byteSizeForArray(struct clazz* cls);
extern void markStatics(CODENAME_ONE_THREAD_STATE);

/*#define safeRelease(threadStateData, es) { \
    if(es != 0 && (es)->type == CN1_TYPE_OBJECT) { releaseObj(threadStateData, (es)->data.o); } \
}

static inline struct elementStruct* pop(struct elementStruct* array, int* sp) {
    --(*sp);
    struct elementStruct* retVal = &array[*sp];
    return retVal;
}

static inline struct elementStruct* popAndRelease(CODENAME_ONE_THREAD_STATE, struct elementStruct* array, int* sp) {
    --(*sp);
    struct elementStruct* retVal = &array[*sp];
    releaseObj(threadStateData, retVal->data.o);
    retVal->type = CN1_TYPE_INVALID;
    return retVal;
}

#define popMany(threadStateData, count, array, sp) { \
    int countVal = count; \
    while(countVal > 0) { \
        --sp; \
        struct elementStruct* ddd = &array[sp]; \
        if(ddd != 0 && (ddd)->type == CN1_TYPE_OBJECT) { releaseObj(threadStateData, (ddd)->data.o); } \
        countVal--; \
    } \
}
*/

// Inlined: POP_INT/POP_LONG/POP_OBJ hit this on every pop, including hot return paths
// (return POP_LONG()). It was a non-inline call -- pure overhead for a pointer decrement.
static inline struct elementStruct* pop(struct elementStruct**sp) {
    --(*sp);
    return *sp;
}
extern void popMany(CODENAME_ONE_THREAD_STATE, int count, struct elementStruct**sp);


#define swapStack(sp) { \
    struct elementStruct t = sp[-1]; \
    sp[-1] = sp[-2]; \
    sp[-2] = t; \
}

extern struct clazz class__java_lang_Class;

#endif //__CN1GLOBALS__
