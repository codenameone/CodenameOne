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

typedef char              JAVA_ARRAY_BYTE;
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
    int __codenameOneReferenceCount;
    
    void* __codenameOneThreadData;
    int __codenameOneGcMark;
    void* __ownerThread;
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
    int __codenameOneReferenceCount;
    void* __codenameOneThreadData;
    int __codenameOneGcMark;
    void* __ownerThread;
    int __heapPosition;
};

struct JavaArrayPrototype {
    DEBUG_GC_VARIABLES
    struct clazz *__codenameOneParentClsReference;
    int __codenameOneReferenceCount;
    void* __codenameOneThreadData;
    int __codenameOneGcMark;
    void* __ownerThread;
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
// __SIZEOF_POINTER__, not the architecture. Opt in with -DCN1_TAGGED_INT.
#if defined(CN1_TAGGED_INT) && defined(__SIZEOF_POINTER__) && (__SIZEOF_POINTER__ >= 8)
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

#define enteringNativeAllocations() threadStateData->nativeAllocationMode = JAVA_TRUE
#define finishedNativeAllocations() threadStateData->nativeAllocationMode = JAVA_FALSE

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
    // signal-stop capture (async-signal-safe: handler only stores + spins)
    volatile sig_atomic_t gcSigStopRequest; // GC sets 1 to ask the handler to park
    volatile sig_atomic_t gcSigStopped;     // handler sets 1 once parked + captured
    volatile sig_atomic_t gcSigRelease;      // GC sets 1 to release the spinning handler
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

#define CN1_THREAD_STATE_SINGLE_ARG CODENAME_ONE_THREAD_STATE
#define CN1_THREAD_STATE_MULTI_ARG CODENAME_ONE_THREAD_STATE,
#define CN1_THREAD_STATE_PASS_ARG threadStateData,
#define CN1_THREAD_STATE_PASS_SINGLE_ARG threadStateData
#define CN1_THREAD_GET_STATE_PASS_ARG getThreadLocalData(),
#define CN1_THREAD_GET_STATE_PASS_SINGLE_ARG getThreadLocalData()
#define CN1_YIELD_THREAD getThreadLocalData()->threadActive = JAVA_FALSE;
#define CN1_RESUME_THREAD while (getThreadLocalData()->threadBlockedByGC){ usleep((JAVA_INT)1000);} getThreadLocalData()->threadActive = JAVA_TRUE;

extern struct ThreadLocalData* getThreadLocalData();


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
           
    #else 
        #define CHECK_ARRAY_ACCESS(array_pos, bounds) if(SP[-array_pos].data.o == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); }
        #define CHECK_ARRAY_ACCESS_EXPR(array, bounds) ((array == JAVA_NULL) ? throwException_R_boolean(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)) : JAVA_TRUE)
        #define CHECK_ARRAY_ACCESS_WITH_ARGS(array, bounds) if(array == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); }
    #endif
#else
    #define CHECK_NPE_TOP_OF_STACK()
    #define CHECK_NPE_AT_STACK(pos)
    #define CHECK_ARRAY_ACCESS(array_pos, bounds) 
    #define CHECK_ARRAY_ACCESS_EXPR(array, bounds) JAVA_TRUE
    #define CHECK_ARRAY_ACCESS_WITH_ARGS(array, bounds) 
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
            *__cn1StackArray = (struct JavaArrayPrototype){DEBUG_GC_INIT (arrayClass), 0, 0, 0, 0, 0, __cn1StackLength, 1, (primitiveSize), 0}; \
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
        throwException(threadStateData, __NEW_INSTANCE_java_lang_StackOverflowError(threadStateData));
        return;
    }
    /* The call-depth guard above does not protect the operand/locals stack: a
     * deep recursion of methods with large frames can exhaust threadObjectStack
     * before the call-depth limit, and without this check initMethodStack would
     * memset/write past the buffer end -> access violation instead of a catchable
     * StackOverflowError. The 1024-slot margin leaves room to build+throw it. */
    if (threadStateData->threadObjectStackOffset + localsStackSize + stackSize >= CN1_MAX_OBJECT_STACK_DEPTH - 1024) {
        throwException(threadStateData, __NEW_INSTANCE_java_lang_StackOverflowError(threadStateData));
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
        throwException(threadStateData, __NEW_INSTANCE_java_lang_StackOverflowError(threadStateData));
        return;
    }
    if (threadStateData->threadObjectStackOffset + localsStackSize + stackSize >= CN1_MAX_OBJECT_STACK_DEPTH - 1024) {
        throwException(threadStateData, __NEW_INSTANCE_java_lang_StackOverflowError(threadStateData));
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
#define CN1_FRAMELESS_SOE_GUARD(retval) \
    do { \
        if (__builtin_expect(threadStateData->nativeStackLimit == 0, 0)) { cn1ComputeNativeStackLimit(threadStateData); } \
        if (__builtin_expect((JAVA_LONG)(intptr_t)__builtin_frame_address(0) < threadStateData->nativeStackLimit, 0)) { \
            throwException(threadStateData, __NEW_INSTANCE_java_lang_StackOverflowError(threadStateData)); \
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
