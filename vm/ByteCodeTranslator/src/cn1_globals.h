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
    
    void* enumValueOfFp;
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

#define BC_I2L() SP[-1].data.l = SP[-1].data.i

#define BC_L2I() SP[-1].data.i = (JAVA_INT)SP[-1].data.l

#define BC_L2F() SP[-1].data.f = (JAVA_FLOAT)SP[-1].data.l

#define BC_L2D() SP[-1].data.d = (JAVA_DOUBLE)SP[-1].data.l

#define BC_I2F() SP[-1].data.f = (JAVA_FLOAT)SP[-1].data.i 

#define BC_F2I() SP[-1].data.i = (JAVA_INT)SP[-1].data.f

#define BC_F2L() SP[-1].data.l = (JAVA_LONG)SP[-1].data.f

#define BC_F2D() SP[-1].data.d = SP[-1].data.f

#define BC_D2I() SP[-1].data.i = (JAVA_INT)SP[-1].data.d

#define BC_D2L() SP[-1].data.l = (JAVA_LONG)SP[-1].data.d

#define BC_I2D() SP[-1].data.d = SP[-1].data.i

#define BC_D2F() SP[-1].data.f = (JAVA_FLOAT)SP[-1].data.d

#define BC_ARRAYLENGTH() { \
    if(SP[-1].data.o == JAVA_NULL) { \
        throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)); \
    }; \
    SP[-1].type = CN1_TYPE_INT; \
    SP[-1].data.i = (*((JAVA_ARRAY)SP[-1].data.o)).length; \
}

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
    (*SP).data.l = SP[-1].data.l; \
    SP[-1].data.l = SP[-2].data.l; \
    SP[-2].data.l = (*SP).data.l; \
    (*SP).type = SP[-1].type; \
    SP[-1].type = SP[-2].type; \
    SP[-2].type = (*SP).type; \
    SP++; \
}

#define BC_DUP2_X2() { \
    if (SP[-2].type == CN1_TYPE_LONG || SP[-2].type == CN1_TYPE_DOUBLE) {\
        (*SP).data.l = SP[-1].data.l; \
        SP[-1].data.l = SP[-2].data.l; \
        SP[-2].data.l = (*SP).data.l; \
        (*SP).type = SP[-1].type; \
        SP[-1].type = SP[-2].type; \
        SP[-2].type = (*SP).type; \
    } else {\
        (*SP).data.l = SP[-1].data.l; \
        SP[-1].data.l = SP[-2].data.l; \
        SP[-2].data.l = SP[-3].data.l; \
        SP[-3].data.l = (*SP).data.l; \
        (*SP).type = SP[-1].type; \
        SP[-1].type = SP[-2].type; \
        SP[-2].type = SP[-3].type; \
        SP[-3].type = (*SP).type; \
    }\
    SP++; \
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

#define GET_CLASS_ID(JavaObj) (*(*JavaObj).__codenameOneParentClsReference).classId

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
    ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)aastoreTmp).data)[SP[-2].data.i] = SP[-1].data.o; \
    SP-=3; \
}
#define BC_AASTORE_WITH_ARGS(array, index, value) CHECK_ARRAY_ACCESS(3, SP[-2].data.i); { \
    JAVA_OBJECT aastoreTmp = SP[-3].data.o; \
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
    JAVA_BOOLEAN threadRemoved;

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
    JAVA_BOOLEAN threadKilled;      // we don't expect to see this in the GC
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

extern void releaseForReturn(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread);


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

#define END_TRY() threadStateData->tryBlockOffset--

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

#define CN1_ARRAY_LENGTH(array) ((array == JAVA_NULL) ? throwException_R_int(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)) : (*((JAVA_ARRAY)array)).length)

#define CN1_ARRAY_ELEMENT_INT(array, index) (CHECK_ARRAY_ACCESS_EXPR(array,index) ? ((JAVA_ARRAY_INT*) (*(JAVA_ARRAY)array).data)[index] : 0)
#define CN1_ARRAY_ELEMENT_BYTE(array, index) (CHECK_ARRAY_ACCESS_EXPR(array,index) ? ((JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY)array).data)[index] : 0)
#define CN1_ARRAY_ELEMENT_FLOAT(array, index) (CHECK_ARRAY_ACCESS_EXPR(array,index) ? ((JAVA_ARRAY_FLOAT*) (*(JAVA_ARRAY)array).data)[index] : 0)
#define CN1_ARRAY_ELEMENT_DOUBLE(array, index) (CHECK_ARRAY_ACCESS_EXPR(array,index) ? ((JAVA_ARRAY_DOUBLE*) (*(JAVA_ARRAY)array).data)[index] : 0)
#define CN1_ARRAY_ELEMENT_LONG(array, index) (CHECK_ARRAY_ACCESS_EXPR(array,index) ? ((JAVA_ARRAY_LONG*) (*(JAVA_ARRAY)array).data)[index] : 0)
#define CN1_ARRAY_ELEMENT_OBJECT(array, index) (CHECK_ARRAY_ACCESS_EXPR(array,index) ? ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)array).data)[index] : 0)
#define CN1_ARRAY_ELEMENT_SHORT(array, index) (CHECK_ARRAY_ACCESS_EXPR(array,index) ? ((JAVA_ARRAY_SHORT*) (*(JAVA_ARRAY)array).data)[index] : 0)
#define CN1_ARRAY_ELEMENT_CHAR(array, index) (CHECK_ARRAY_ACCESS_EXPR(array,index) ? ((JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY)array).data)[index] : 0)
#define CN1_SET_ARRAY_ELEMENT_INT(array, index, value) CHECK_ARRAY_ACCESS_WITH_ARGS(array, index); \
    ((JAVA_ARRAY_INT*) (*(JAVA_ARRAY)array).data)[index] = value;
#define CN1_SET_ARRAY_ELEMENT_BYTE(array, index, value) CHECK_ARRAY_ACCESS_WITH_ARGS(array, index); \
    ((JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY)array).data)[index] = value;
#define CN1_SET_ARRAY_ELEMENT_FLOAT(array, index, value) CHECK_ARRAY_ACCESS_WITH_ARGS(array, index); \
    ((JAVA_ARRAY_FLOAT*) (*(JAVA_ARRAY)array).data)[index] = value;
#define CN1_SET_ARRAY_ELEMENT_DOUBLE(array, index, value) CHECK_ARRAY_ACCESS_WITH_ARGS(array, index); \
    ((JAVA_ARRAY_DOUBLE*) (*(JAVA_ARRAY)array).data)[index] = value;
#define CN1_SET_ARRAY_ELEMENT_LONG(array, index, value) CHECK_ARRAY_ACCESS_WITH_ARGS(array, index); \
    ((JAVA_ARRAY_LONG*) (*(JAVA_ARRAY)array).data)[index] = value;
#define CN1_SET_ARRAY_ELEMENT_OBJECT(array, index, value) CHECK_ARRAY_ACCESS_WITH_ARGS(array, index); \
    ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)array).data)[index] = value;
#define CN1_SET_ARRAY_ELEMENT_SHORT(array, index, value) CHECK_ARRAY_ACCESS_WITH_ARGS(array, index); \
    ((JAVA_ARRAY_SHORT*) (*(JAVA_ARRAY)array).data)[index] = value;
#define CN1_SET_ARRAY_ELEMENT_CHAR(array, index, value) CHECK_ARRAY_ACCESS_WITH_ARGS(array, index); \
    ((JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY)array).data)[index] = value;

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
    struct elementStruct* SP = &stack[spPosition]; \
    initMethodStack(threadStateData, (JAVA_OBJECT)1, stackSize,localsStackSize, classNameId, methodNameId); \
    const int currentCodenameOneCallStackOffset = threadStateData->callStackOffset;

#define DEFINE_INSTANCE_METHOD_STACK(stackSize, localsStackSize, spPosition, classNameId, methodNameId) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    struct elementStruct* SP = &stack[spPosition]; \
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

extern struct elementStruct* pop(struct elementStruct**sp);
extern void popMany(CODENAME_ONE_THREAD_STATE, int count, struct elementStruct**sp);


#define swapStack(sp) { \
    struct elementStruct t = sp[-1]; \
    sp[-1] = sp[-2]; \
    sp[-2] = t; \
}

extern struct clazz class__java_lang_Class;

#endif //__CN1GLOBALS__
