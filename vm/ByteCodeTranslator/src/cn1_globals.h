#ifndef __CN1GLOBALS__
#define __CN1GLOBALS__

#include <stdio.h>
#include <stdlib.h>
#include "cn1_class_method_index.h"
#include <pthread.h>
#include <setjmp.h>
#include <math.h>

//#define DEBUG_GC_ALLOCATIONS

#define NUMBER_OF_SUPPORTED_THREADS 1024
#define CN1_FINALIZER_QUEUE_SIZE 65536

#define CN1_INCLUDE_NPE_CHECKS
#define CN1_INCLUDE_ARRAY_BOUND_CHECKS

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
    struct clazz* arrayType;
    JAVA_BOOLEAN primitiveType;
    
    const struct clazz* baseClass;
    const struct clazz** baseInterfaces;
    const int baseInterfaceCount;
    
    void* newInstanceFp;
    
    // virtual method table lookup
    void** vtable;
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
    stack[stackPointer].type = CN1_TYPE_INT; \
    stack[stackPointer].data.i = locals[local].data.i; \
    stackPointer++; \
}

#define BC_LLOAD(local) { \
    stack[stackPointer].type = CN1_TYPE_LONG; \
    stack[stackPointer].data.l = locals[local].data.l; \
    stackPointer++; \
}

#define BC_FLOAD(local) { \
    stack[stackPointer].type = CN1_TYPE_FLOAT; \
    stack[stackPointer].data.f = locals[local].data.f; \
    stackPointer++; \
}

#define BC_DLOAD(local) { \
    stack[stackPointer].type = CN1_TYPE_DOUBLE; \
    stack[stackPointer].data.d = locals[local].data.d; \
    stackPointer++; \
}

#define BC_ALOAD(local) { \
    stack[stackPointer].type = CN1_TYPE_INVALID; \
    stack[stackPointer].data.o = locals[local].data.o; \
    stack[stackPointer].type = CN1_TYPE_OBJECT; \
    stackPointer++; \
}


#define BC_ISTORE(local) { stackPointer--; \
    locals[local].type = CN1_TYPE_INT; \
    locals[local].data.i = stack[stackPointer].data.i; \
    }

#define BC_LSTORE(local) { stackPointer--; \
    locals[local].type = CN1_TYPE_LONG; \
    locals[local].data.l = stack[stackPointer].data.l; \
    }

#define BC_FSTORE(local) { stackPointer--; \
    locals[local].type = CN1_TYPE_FLOAT; \
    locals[local].data.f = stack[stackPointer].data.f; \
    }

#define BC_DSTORE(local) { stackPointer--; \
    locals[local].type = CN1_TYPE_DOUBLE; \
    locals[local].data.d = stack[stackPointer].data.d; \
    }

#define BC_ASTORE(local) { stackPointer--; \
    locals[local].type = CN1_TYPE_INVALID; \
    locals[local].data.o = stack[stackPointer].data.o; \
    locals[local].type = CN1_TYPE_OBJECT; \
    }

// todo map instanceof and throw typecast exception
#define BC_CHECKCAST(type)

#define BC_SWAP() swapStack(stack, stackPointer)


#define POP_INT() (*pop(stack, &stackPointer)).data.i
#define POP_OBJ() (*pop(stack, &stackPointer)).data.o
#define POP_OBJ_NO_RELEASE() (*pop(stack, &stackPointer)).data.o
#define POP_LONG() (*pop(stack, &stackPointer)).data.l
#define POP_DOUBLE() (*pop(stack, &stackPointer)).data.d
#define POP_FLOAT() (*pop(stack, &stackPointer)).data.f

#define PEEK_INT(offset) stack[stackPointer - offset].data.i
#define PEEK_OBJ(offset) stack[stackPointer - offset].data.o
#define PEEK_LONG(offset) stack[stackPointer - offset].data.l
#define PEEK_DOUBLE(offset) stack[stackPointer - offset].data.d
#define PEEK_FLOAT(offset) stack[stackPointer - offset].data.f

#define POP_MANY(offset) popMany(threadStateData, offset, stack, &stackPointer)

#define BC_IADD() { \
    stackPointer--; \
    stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i + stack[stackPointer].data.i; \
}

#define BC_LADD() { \
    stackPointer--; \
    stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l + stack[stackPointer].data.l; \
}

#define BC_FADD() { \
    stackPointer--; \
    stack[stackPointer - 1].data.f = stack[stackPointer - 1].data.f + stack[stackPointer].data.f; \
}

#define BC_DADD() { \
    stackPointer--; \
    stack[stackPointer - 1].data.d = stack[stackPointer - 1].data.d + stack[stackPointer].data.d; \
}

#define BC_IMUL() { \
    stackPointer--; \
    stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i * stack[stackPointer].data.i; \
}

#define BC_LMUL() { \
    stackPointer--; \
    stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l * stack[stackPointer].data.l; \
}

#define BC_FMUL() { \
    stackPointer--; \
    stack[stackPointer - 1].data.f = stack[stackPointer - 1].data.f * stack[stackPointer].data.f; \
}

#define BC_DMUL() { \
    stackPointer--; \
    stack[stackPointer - 1].data.d = stack[stackPointer - 1].data.d * stack[stackPointer].data.d; \
}

#define BC_INEG() stack[stackPointer - 1].data.i *= -1

#define BC_LNEG() stack[stackPointer - 1].data.l *= -1

#define BC_FNEG() stack[stackPointer - 1].data.f *= -1

#define BC_DNEG() stack[stackPointer - 1].data.d *= -1

#define BC_IAND() { \
    stackPointer--; \
    stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i & stack[stackPointer].data.i; \
}

#define BC_LAND() { \
    stackPointer--; \
    stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l & stack[stackPointer].data.l; \
}

#define BC_IOR() { \
    stackPointer--; \
    stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i | stack[stackPointer].data.i; \
}

#define BC_LOR() { \
    stackPointer--; \
    stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l | stack[stackPointer].data.l; \
}

#define BC_IXOR() { \
    stackPointer--; \
    stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i ^ stack[stackPointer].data.i; \
}

#define BC_LXOR() { \
    stackPointer--; \
    stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l ^ stack[stackPointer].data.l; \
}

#define BC_I2L() stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.i

#define BC_L2I() stack[stackPointer - 1].data.i = (JAVA_INT)stack[stackPointer - 1].data.l

#define BC_L2F() stack[stackPointer - 1].data.f = (JAVA_FLOAT)stack[stackPointer - 1].data.l

#define BC_L2D() stack[stackPointer - 1].data.d = (JAVA_DOUBLE)stack[stackPointer - 1].data.l

#define BC_I2F() stack[stackPointer - 1].data.f = (JAVA_FLOAT)stack[stackPointer - 1].data.i 

#define BC_F2I() stack[stackPointer - 1].data.i = (JAVA_INT)stack[stackPointer - 1].data.f

#define BC_F2L() stack[stackPointer - 1].data.l = (JAVA_LONG)stack[stackPointer - 1].data.f

#define BC_F2D() stack[stackPointer - 1].data.d = stack[stackPointer - 1].data.f

#define BC_D2I() stack[stackPointer - 1].data.i = (JAVA_INT)stack[stackPointer - 1].data.d

#define BC_D2L() stack[stackPointer - 1].data.l = (JAVA_LONG)stack[stackPointer - 1].data.d

#define BC_I2D() stack[stackPointer - 1].data.d = stack[stackPointer - 1].data.i

#define BC_D2F() stack[stackPointer - 1].data.f = (JAVA_FLOAT)stack[stackPointer - 1].data.d

#define BC_ARRAYLENGTH() { \
    if(stack[stackPointer - 1].data.o == JAVA_NULL) { \
        throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)); \
    }; \
    stack[stackPointer - 1].type = CN1_TYPE_INT; \
    stack[stackPointer - 1].data.i = (*((JAVA_ARRAY)stack[stackPointer - 1].data.o)).length; \
}

#define BC_IF_ICMPEQ() stackPointer -= 2; if(stack[stackPointer].data.i == stack[stackPointer + 1].data.i)

#define BC_IF_ICMPNE() stackPointer -= 2; if(stack[stackPointer].data.i != stack[stackPointer + 1].data.i)

#define BC_IF_ICMPLT() stackPointer -= 2; if(stack[stackPointer].data.i < stack[stackPointer + 1].data.i)

#define BC_IF_ICMPGE() stackPointer -= 2; if(stack[stackPointer].data.i >= stack[stackPointer + 1].data.i)

#define BC_IF_ICMPGT() stackPointer -= 2; if(stack[stackPointer].data.i > stack[stackPointer + 1].data.i)

#define BC_IF_ICMPLE() stackPointer -= 2; if(stack[stackPointer].data.i <= stack[stackPointer + 1].data.i)

#define BC_IF_ACMPEQ() stackPointer -= 2; if(stack[stackPointer].data.o == stack[stackPointer + 1].data.o)

#define BC_IF_ACMPNE() stackPointer -= 2; if(stack[stackPointer].data.o != stack[stackPointer + 1].data.o)

//#define POP_TYPE(type) (*((type*)POP_OBJ()))

// we assign the value to trigger the expression in the macro
// then set the type to invalid first so we don't get a race condition where the value is
// incomplete and the GC goes crazy
#define PUSH_POINTER(value) { JAVA_OBJECT ppX = value; stack[stackPointer].type = CN1_TYPE_INVALID; \
    stack[stackPointer].data.o = ppX; stack[stackPointer].type = CN1_TYPE_OBJECT; \
    stackPointer++; }

#define PUSH_OBJ(value)  { JAVA_OBJECT ppX = value; stack[stackPointer].type = CN1_TYPE_INVALID; \
    stack[stackPointer].data.o = ppX; stack[stackPointer].type = CN1_TYPE_OBJECT; \
    stackPointer++; }

#define PUSH_INT(value) { JAVA_INT pInt = value; stack[stackPointer].type = CN1_TYPE_INT; \
    stack[stackPointer].data.i = pInt; \
    stackPointer++; }

#define PUSH_LONG(value) { JAVA_LONG plong = value; stack[stackPointer].type = CN1_TYPE_LONG; \
    stack[stackPointer].data.l = plong; \
    stackPointer++; }

#define PUSH_DOUBLE(value) { JAVA_DOUBLE pdob = value; stack[stackPointer].type = CN1_TYPE_DOUBLE; \
    stack[stackPointer].data.d = pdob; \
    stackPointer++; }

#define PUSH_FLOAT(value) { JAVA_FLOAT pFlo = value; stack[stackPointer].type = CN1_TYPE_FLOAT; \
    stack[stackPointer].data.f = pFlo; \
    stackPointer++; }

#define POP_MANY_AND_PUSH_OBJ(value, offset) { int acOff = stackPointer - offset; \
    JAVA_OBJECT pObj = value; stack[stackPointer - offset].type = CN1_TYPE_INVALID; \
    stack[stackPointer - offset].data.o = pObj; stack[stackPointer - offset].type = CN1_TYPE_OBJECT; \
    popMany(threadStateData, MAX(1, offset) - 1, stack, &stackPointer); }

#define POP_MANY_AND_PUSH_INT(value, offset) { int acOff = stackPointer - offset; \
    JAVA_INT pInt = value; stack[stackPointer - offset].type = CN1_TYPE_INT; \
    stack[stackPointer - offset].data.i = pInt; \
    popMany(threadStateData, MAX(1, offset) - 1, stack, &stackPointer); }

#define POP_MANY_AND_PUSH_LONG(value, offset) { int acOff = stackPointer - offset; \
    JAVA_LONG pLong = value; stack[stackPointer - offset].type = CN1_TYPE_LONG; \
    stack[stackPointer - offset].data.l = pLong; \
    popMany(threadStateData, MAX(1, offset) - 1, stack, &stackPointer); }

#define POP_MANY_AND_PUSH_DOUBLE(value, offset) { int acOff = stackPointer - offset; \
    JAVA_DOUBLE pDob = value; stack[stackPointer - offset].type = CN1_TYPE_DOUBLE; \
    stack[stackPointer - offset].data.d = pDob; \
    popMany(threadStateData, MAX(1, offset) - 1, stack, &stackPointer); }

#define POP_MANY_AND_PUSH_FLOAT(value, offset) { int acOff = stackPointer - offset; \
    JAVA_FLOAT pFlo = value; stack[stackPointer - offset].type = CN1_TYPE_FLOAT; \
    stack[stackPointer - offset].data.f = pFlo; \
    popMany(threadStateData, MAX(1, offset) - 1, stack, &stackPointer); }


#define BC_IDIV() stackPointer--; stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i / stack[stackPointer].data.i

#define BC_LDIV() stackPointer--; stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l / stack[stackPointer].data.l

#define BC_FDIV() stackPointer--; stack[stackPointer - 1].data.f = stack[stackPointer - 1].data.f / stack[stackPointer].data.f

#define BC_DDIV() stackPointer--; stack[stackPointer - 1].data.d = stack[stackPointer - 1].data.d / stack[stackPointer].data.d

#define BC_IREM() stackPointer--; stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i % stack[stackPointer].data.i

#define BC_LREM() stackPointer--; stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l % stack[stackPointer].data.l

#define BC_FREM() stackPointer--; stack[stackPointer - 1].data.f = fmod(stack[stackPointer - 1].data.f, stack[stackPointer].data.f)

#define BC_DREM() stackPointer--; stack[stackPointer - 1].data.d = fmod(stack[stackPointer - 1].data.d, stack[stackPointer].data.d)

#define BC_LCMP() stackPointer--; if(stack[stackPointer - 1].data.l == stack[stackPointer].data.l) { \
        stack[stackPointer - 1].data.i = 0; \
    } else { \
        if(stack[stackPointer - 1].data.l > stack[stackPointer].data.l) { \
            stack[stackPointer - 1].data.i = 1; \
        } else { \
            stack[stackPointer - 1].data.i = -1; \
        } \
    } \
    stack[stackPointer - 1].type = CN1_TYPE_INT;

#define BC_FCMPL() stackPointer--; if(stack[stackPointer - 1].data.f == stack[stackPointer].data.f) { \
        stack[stackPointer - 1].data.i = 0; \
    } else { \
        if(stack[stackPointer - 1].data.f > stack[stackPointer].data.f) { \
            stack[stackPointer - 1].data.i = 1; \
        } else { \
            stack[stackPointer - 1].data.i = -1; \
        } \
    } \
    stack[stackPointer - 1].type = CN1_TYPE_INT;

#define BC_DCMPL() stackPointer--; if(stack[stackPointer - 1].data.d == stack[stackPointer].data.d) { \
        stack[stackPointer - 1].data.i = 0; \
    } else { \
        if(stack[stackPointer - 1].data.d > stack[stackPointer].data.d) { \
            stack[stackPointer - 1].data.i = 1; \
        } else { \
            stack[stackPointer - 1].data.i = -1; \
        } \
    } \
    stack[stackPointer - 1].type = CN1_TYPE_INT;

#define BC_DUP()  { \
        JAVA_LONG plong = stack[stackPointer - 1].data.l; \
        stack[stackPointer].type = CN1_TYPE_INVALID; \
        stack[stackPointer].data.l = plong; stack[stackPointer].type = CN1_TYPE_LONG; \
        stackPointer++; \
    } \
    stack[stackPointer - 1].type = stack[stackPointer - 2].type; 

#define BC_DUP2()  \
if(stack[stackPointer - 1].type == CN1_TYPE_LONG || stack[stackPointer - 1].type == CN1_TYPE_DOUBLE) {\
    BC_DUP(); \
} else {\
    { \
        JAVA_LONG plong = stack[stackPointer - 2].data.l; \
        JAVA_LONG plong2 = stack[stackPointer - 1].data.l; \
        stack[stackPointer].type = CN1_TYPE_INVALID; \
        stack[stackPointer + 1].type = CN1_TYPE_INVALID; \
        stack[stackPointer].data.l = plong; \
        stack[stackPointer + 1].data.l = plong2; \
        stackPointer+=2; \
    } \
    stack[stackPointer - 1].type = stack[stackPointer - 3].type; \
    stack[stackPointer - 2].type = stack[stackPointer - 4].type; \
}

#define BC_DUP2_X1() {\
    stack[stackPointer].data.l = stack[stackPointer - 1].data.l; \
    stack[stackPointer - 1].data.l = stack[stackPointer - 2].data.l; \
    stack[stackPointer - 2].data.l = stack[stackPointer].data.l; \
    stack[stackPointer].type = stack[stackPointer - 1].type; \
    stack[stackPointer - 1].type = stack[stackPointer - 2].type; \
    stack[stackPointer - 2].type = stack[stackPointer].type; \
    stackPointer++; \
}

#define BC_DUP2_X2() { \
    if (stack[stackPointer-2].type == CN1_TYPE_LONG || stack[stackPointer-2].type == CN1_TYPE_DOUBLE) {\
        stack[stackPointer].data.l = stack[stackPointer - 1].data.l; \
        stack[stackPointer - 1].data.l = stack[stackPointer - 2].data.l; \
        stack[stackPointer - 2].data.l = stack[stackPointer].data.l; \
        stack[stackPointer].type = stack[stackPointer - 1].type; \
        stack[stackPointer - 1].type = stack[stackPointer - 2].type; \
        stack[stackPointer - 2].type = stack[stackPointer].type; \
    } else {\
        stack[stackPointer].data.l = stack[stackPointer - 1].data.l; \
        stack[stackPointer - 1].data.l = stack[stackPointer - 2].data.l; \
        stack[stackPointer - 2].data.l = stack[stackPointer - 3].data.l; \
        stack[stackPointer - 3].data.l = stack[stackPointer].data.l; \
        stack[stackPointer].type = stack[stackPointer - 1].type; \
        stack[stackPointer - 1].type = stack[stackPointer - 2].type; \
        stack[stackPointer - 2].type = stack[stackPointer - 3].type; \
        stack[stackPointer - 3].type = stack[stackPointer].type; \
    }\
    stackPointer++; \
}

#define BC_I2B() stack[stackPointer - 1].data.i = ((stack[stackPointer - 1].data.i << 24) >> 24)

#define BC_I2S() stack[stackPointer - 1].data.i = ((stack[stackPointer - 1].data.i << 16) >> 16)

#define BC_I2C() stack[stackPointer - 1].data.i = (stack[stackPointer - 1].data.i & 0xffff)

#define BC_ISHL() stackPointer--; stack[stackPointer - 1].data.i = (stack[stackPointer - 1].data.i << (0x1f & stack[stackPointer].data.i))

#define BC_LSHL() stackPointer--; stack[stackPointer - 1].data.l = (stack[stackPointer - 1].data.l << (0x3f & stack[stackPointer].data.l))

#define BC_ISHR() stackPointer--; stack[stackPointer - 1].data.i = (stack[stackPointer - 1].data.i >> (0x1f & stack[stackPointer].data.i))

#define BC_LSHR() stackPointer--; stack[stackPointer - 1].data.l = (stack[stackPointer - 1].data.l >> (0x3f & stack[stackPointer].data.l))

#define BC_IUSHL() stackPointer--; stack[stackPointer - 1].data.i = (((unsigned int)stack[stackPointer - 1]).data.i << (0x1f & ((unsigned int)stack[stackPointer].data.i)))

#define BC_LUSHL() stackPointer--; stack[stackPointer - 1].data.l = (((unsigned long long)stack[stackPointer - 1].data.l) << (0x3f & ((unsigned long long)stack[stackPointer].data.l)))

#define BC_IUSHR() stackPointer--; stack[stackPointer - 1].data.i = (((unsigned int)stack[stackPointer - 1].data.i) >> (0x1f & ((unsigned int)stack[stackPointer].data.i)))

#define BC_LUSHR() stackPointer--; stack[stackPointer - 1].data.l = (((unsigned long long)stack[stackPointer - 1].data.l) >> (0x3f & ((unsigned long long)stack[stackPointer].data.l)))

#define BC_ISUB() stackPointer--; stack[stackPointer - 1].data.i = (stack[stackPointer - 1].data.i - stack[stackPointer].data.i)

#define BC_LSUB() stackPointer--; stack[stackPointer - 1].data.l = (stack[stackPointer - 1].data.l - stack[stackPointer].data.l)

#define BC_FSUB() stackPointer--; stack[stackPointer - 1].data.f = (stack[stackPointer - 1].data.f - stack[stackPointer].data.f)

#define BC_DSUB() stackPointer--; stack[stackPointer - 1].data.d = (stack[stackPointer - 1].data.d - stack[stackPointer].data.d)

extern JAVA_OBJECT* constantPoolObjects;

extern int classListSize;
extern struct clazz* classesList[];

// this needs to be fixed to actually return a JAVA_OBJECT...
#define STRING_FROM_CONSTANT_POOL_OFFSET(off) constantPoolObjects[off]

#define BC_IINC(val, num) locals[val].data.i += num;

extern int instanceofFunction(int sourceClass, int destId);

#define GET_CLASS_ID(JavaObj) (*(*JavaObj).__codenameOneParentClsReference).classId

#define BC_INSTANCEOF(typeOfInstanceOf) { \
    if(stack[stackPointer - 1].data.o != JAVA_NULL) { \
        int tmpInstanceOfId = GET_CLASS_ID(stack[stackPointer - 1].data.o); \
        stack[stackPointer - 1].type = CN1_TYPE_INVALID; \
        stack[stackPointer - 1].data.i = instanceofFunction( typeOfInstanceOf, tmpInstanceOfId ); \
    } \
    stack[stackPointer - 1].type = CN1_TYPE_INT; \
}

#define BC_IALOAD() { CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); \
    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_INT; \
    stack[stackPointer - 1].data.i = ((JAVA_ARRAY_INT*) (*(JAVA_ARRAY)stack[stackPointer - 1].data.o).data)[stack[stackPointer].data.i]; \
    }

#define BC_LALOAD() { CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); \
    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_LONG; \
    stack[stackPointer - 1].data.l = LONG_ARRAY_LOOKUP((JAVA_ARRAY)stack[stackPointer - 1].data.o, stack[stackPointer].data.i); \
    }

#define BC_FALOAD() { CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); \
    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_FLOAT; \
    stack[stackPointer - 1].data.f = FLOAT_ARRAY_LOOKUP((JAVA_ARRAY)stack[stackPointer - 1].data.o, stack[stackPointer].data.i); \
    }

#define BC_DALOAD() { CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); \
    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_DOUBLE; \
    stack[stackPointer - 1].data.d = DOUBLE_ARRAY_LOOKUP((JAVA_ARRAY)stack[stackPointer - 1].data.o, stack[stackPointer].data.i); \
    }

#define BC_AALOAD() { CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); \
    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_INVALID; \
    stack[stackPointer - 1].data.o = ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)stack[stackPointer - 1].data.o).data)[stack[stackPointer].data.i]; \
    stack[stackPointer - 1].type = CN1_TYPE_OBJECT;  }

#define BC_BALOAD() { CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); \
    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_INT; \
    stack[stackPointer - 1].data.i = ((JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY)stack[stackPointer - 1].data.o).data)[stack[stackPointer].data.i]; \
    }

#define BC_CALOAD() { CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); \
    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_INT; \
    stack[stackPointer - 1].data.i = ((JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY)stack[stackPointer - 1].data.o).data)[stack[stackPointer].data.i]; \
    }

#define BC_SALOAD() { CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); \
    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_INT; \
    stack[stackPointer - 1].data.i = ((JAVA_ARRAY_SHORT*) (*(JAVA_ARRAY)stack[stackPointer - 1].data.o).data)[stack[stackPointer].data.i]; \
    }


#define BC_BASTORE() CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); \
    ((JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY)stack[stackPointer - 3].data.o).data)[stack[stackPointer - 2].data.i] = stack[stackPointer - 1].data.i; stackPointer -= 3

#define BC_CASTORE() CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); \
    ((JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY)stack[stackPointer - 3].data.o).data)[stack[stackPointer - 2].data.i] = stack[stackPointer - 1].data.i; stackPointer -= 3

#define BC_SASTORE() CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); \
    ((JAVA_ARRAY_SHORT*) (*(JAVA_ARRAY)stack[stackPointer - 3].data.o).data)[stack[stackPointer - 2].data.i] = stack[stackPointer - 1].data.i; stackPointer -= 3

#define BC_IASTORE() CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); \
    ((JAVA_ARRAY_INT*) (*(JAVA_ARRAY)stack[stackPointer - 3].data.o).data)[stack[stackPointer - 2].data.i] = stack[stackPointer - 1].data.i; stackPointer -= 3

#define BC_LASTORE() CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); \
    LONG_ARRAY_LOOKUP((JAVA_ARRAY)stack[stackPointer - 3].data.o, stack[stackPointer - 2].data.i) = stack[stackPointer - 1].data.l; stackPointer -= 3

#define BC_FASTORE() CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); \
    FLOAT_ARRAY_LOOKUP((JAVA_ARRAY)stack[stackPointer - 3].data.o, stack[stackPointer - 2].data.i) = stack[stackPointer - 1].data.f; stackPointer -= 3

#define BC_DASTORE() CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); \
    DOUBLE_ARRAY_LOOKUP((JAVA_ARRAY)stack[stackPointer - 3].data.o, stack[stackPointer - 2].data.i) = stack[stackPointer - 1].data.d; stackPointer -= 3

#define BC_AASTORE() CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); { \
    JAVA_OBJECT aastoreTmp = stack[stackPointer - 3].data.o; \
    ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)aastoreTmp).data)[stack[stackPointer - 2].data.i] = stack[stackPointer - 1].data.o; \
    stackPointer -= 3; \
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
};

#define CN1_MAX_STACK_CALL_DEPTH 1024
#define CN1_MAX_OBJECT_STACK_DEPTH 16536

#define PER_THREAD_ALLOCATION_COUNT 4096

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

    // used by the GC to traverse the objects pointed to by this thread
    struct elementStruct* threadObjectStack;
    int threadObjectStackOffset;
    
    // allocations are stored here and then copied to the big memory pool during
    // the mark sweep
    void** pendingHeapAllocations;
    JAVA_INT heapAllocationSize;
    JAVA_INT threadHeapTotalSize;

    // used to construct stack trace
    int* callStackClass;
    int* callStackLine;
    int* callStackMethod;
    int callStackOffset;
    
    char* utf8Buffer;
    int utf8BufferSize;
};

//#define BLOCK_FOR_GC() while(threadStateData->threadBlockedByGC) { usleep(500); }

#define __CN1_DEBUG_INFO(line) threadStateData->callStackLine[threadStateData->callStackOffset - 1] = line;

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

#define DEFINE_EXCEPTION_HANDLING_CONSTANTS() int methodBlockOffset = threadStateData->tryBlockOffset

#define BEGIN_TRY(classId, destinationJump) {\
        threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass = classId; \
        memcpy(threadStateData->blocks[threadStateData->tryBlockOffset].destination, destinationJump, sizeof(jmp_buf)); \
        threadStateData->tryBlockOffset++; \
    }

#define JUMP_TO(labelToJumpTo, blockOffsetLevel) {\
        threadStateData->tryBlockOffset = methodBlockOffset + blockOffsetLevel; \
        goto labelToJumpTo; \
    }

extern void releaseForReturn(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread, int stackPointer, int cn1SizeOfLocals, struct elementStruct* stack, struct elementStruct* locals);


#define RETURN_AND_RELEASE_FROM_METHOD(returnVal, cn1SizeOfLocals) { \
        releaseForReturn(threadStateData, cn1LocalsBeginInThread, stackPointer - 1, cn1SizeOfLocals, stack, locals); \
        return returnVal; \
    }

#define RETURN_AND_RELEASE_FROM_VOID(cn1SizeOfLocals) { \
        releaseForReturn(threadStateData, cn1LocalsBeginInThread, stackPointer, cn1SizeOfLocals, stack, locals); \
        return; \
    }

extern void releaseForReturnInException(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread, int stackPointer, int cn1SizeOfLocals, struct elementStruct* stack, struct elementStruct* locals, int methodBlockOffset);

#define RETURN_FROM_METHOD(returnVal, cn1SizeOfLocals) releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, stackPointer, cn1SizeOfLocals, stack, locals, methodBlockOffset); \
        return returnVal; \

#define RETURN_FROM_VOID(cn1SizeOfLocals) releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, stackPointer, cn1SizeOfLocals, stack, locals, methodBlockOffset); \
        return; \

#define END_TRY() threadStateData->tryBlockOffset--

#define DEFINE_CATCH_BLOCK(destinationJump, labelName, restoreToCn1LocalsBeginInThread) jmp_buf destinationJump; \
{ \
    int currentOffset = threadStateData->tryBlockOffset; \
    if(setjmp(destinationJump)) { \
        threadStateData->callStackOffset = currentCodenameOneCallStackOffset; \
        threadStateData->threadObjectStackOffset = restoreToCn1LocalsBeginInThread; \
        stackPointer = 1; \
        stack[0].data.o = threadStateData->exception; \
        stack[0].type = CN1_TYPE_OBJECT; \
        goto labelName; \
    } \
}

extern JAVA_VOID java_lang_Throwable_fillInStack__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT ex);


extern void throwException(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exceptionArg);

extern JAVA_OBJECT __NEW_java_lang_NullPointerException(CODENAME_ONE_THREAD_STATE);
extern JAVA_OBJECT __NEW_java_lang_ArrayIndexOutOfBoundsException(CODENAME_ONE_THREAD_STATE);
extern JAVA_VOID java_lang_ArrayIndexOutOfBoundsException___INIT_____int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT __cn1Arg1);
extern void throwArrayIndexOutOfBoundsException(CODENAME_ONE_THREAD_STATE, int index);

#define THROW_NULL_POINTER_EXCEPTION()    throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData))

#define THROW_ARRAY_INDEX_EXCEPTION(index)    throwArrayIndexOutOfBoundsException(threadStateData, index)

#ifdef CN1_INCLUDE_NPE_CHECKS
    #define CHECK_NPE_TOP_OF_STACK() if(stack[stackPointer - 1].data.o == JAVA_NULL) { NSLog(@"Throwing NullPointerException!"); throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)); }
    #define CHECK_NPE_AT_STACK(pos) if(stack[stackPointer - pos].data.o == JAVA_NULL) { NSLog(@"Throwing NullPointerException!"); throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)); }

    #ifdef CN1_INCLUDE_ARRAY_BOUND_CHECKS
        #define CHECK_ARRAY_ACCESS(array_pos, bounds) if(stack[stackPointer - array_pos].data.o == JAVA_NULL) { NSLog(@"Throwing NullPointerException!"); throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)); } \
            if(bounds < 0 || bounds >= ((JAVA_ARRAY)stack[stackPointer - array_pos].data.o)->length) { THROW_ARRAY_INDEX_EXCEPTION(bounds); }
    #else 
        #define CHECK_ARRAY_ACCESS(array_pos, bounds) if(stack[stackPointer - array_pos].data.o == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); } 
    #endif
#else
    #define CHECK_NPE_TOP_OF_STACK()
    #define CHECK_NPE_AT_STACK(pos)
    #define CHECK_ARRAY_ACCESS(array_pos, bounds) 
#endif

#ifdef CN1_INCLUDE_ARRAY_BOUND_CHECKS
    #define CHECK_ARRAY_BOUNDS_AT_STACK(pos, bounds) if(bounds < 0 || bounds >= ((JAVA_ARRAY)PEEK_OBJ(pos))->length) { THROW_ARRAY_INDEX_EXCEPTION(bounds); }
#else
    #define CHECK_ARRAY_BOUNDS_AT_STACK(pos, bounds)
#endif

extern JAVA_VOID monitorEnter(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);
extern JAVA_VOID monitorExit(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);

extern void arrayFinalizerFunction(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array);


#define MONITOR_ENTER() monitorEnter(threadStateData, POP_OBJ())
#define MONITOR_EXIT() monitorExit(threadStateData, POP_OBJ())

extern void gcReleaseObj(JAVA_OBJECT o);

extern JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim);
extern JAVA_OBJECT allocMultiArray(int* lengths, struct clazz* type, int primitiveSize, int dim);
extern JAVA_OBJECT alloc2DArray(CODENAME_ONE_THREAD_STATE, int length1, int length2, struct clazz* parentType, struct clazz* childType, int primitiveSize);
extern JAVA_OBJECT alloc3DArray(CODENAME_ONE_THREAD_STATE, int length1, int length2, int length3, struct clazz* parentType, struct clazz* childType, struct clazz* grandChildType, int primitiveSize);

extern void lockCriticalSection();
extern void unlockCriticalSection();

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

// we need to zero out the values with memset otherwise we will run into a problem
// when invoking release on pre-existing object which might be garbage
#define DEFINE_METHOD_STACK(stackSize, localsStackSize, spPosition, classNameId, methodNameId) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    int stackPointer = spPosition; \
    initMethodStack(threadStateData, (JAVA_OBJECT)1, stackSize,localsStackSize, classNameId, methodNameId); \
    const int currentCodenameOneCallStackOffset = threadStateData->callStackOffset;

#define DEFINE_INSTANCE_METHOD_STACK(stackSize, localsStackSize, spPosition, classNameId, methodNameId) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    int stackPointer = spPosition; \
    initMethodStack(threadStateData, __cn1ThisObject, stackSize,localsStackSize, classNameId, methodNameId); \
    const int currentCodenameOneCallStackOffset = threadStateData->callStackOffset;


#ifdef __OBJC__
extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str);
extern NSString* toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o);
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

typedef JAVA_OBJECT (*newInstanceFunctionPointer)(CODENAME_ONE_THREAD_STATE);

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

extern struct elementStruct* pop(struct elementStruct* array, int* sp);
extern void popMany(CODENAME_ONE_THREAD_STATE, int count, struct elementStruct* array, int* sp);


#define swapStack(array, sp) { \
    struct elementStruct t = array[sp-1]; \
    array[sp-1] = array[sp - 2]; \
    array[sp - 2] = t; \
}

extern struct clazz class__java_lang_Class;

#endif //__CN1GLOBALS__