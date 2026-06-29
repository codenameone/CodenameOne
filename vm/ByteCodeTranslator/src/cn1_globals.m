#include "cn1_globals.h"
#include <assert.h>
#include "java_lang_Class.h"
#include "java_lang_Object.h"
#include "java_lang_Boolean.h"
#include "java_lang_String.h"
#include "java_lang_Integer.h"
#include "java_lang_Byte.h"
#include "java_lang_Short.h"
#include "java_lang_Character.h"
#include "java_lang_Thread.h"
#include "java_lang_Long.h"
#include "java_lang_Double.h"
#include "java_lang_Float.h"
#include "java_lang_Runnable.h"
#include "java_lang_System.h"
#include "java_lang_ArrayIndexOutOfBoundsException.h"
#if defined(__APPLE__) && defined(__OBJC__)
#import <TargetConditionals.h>
#import <mach/mach.h>
#import <mach/mach_host.h>
#else
#include <time.h>
#ifndef _WIN32
#include <unistd.h>
#endif
#define NSLog(...) printf(__VA_ARGS__); printf("\n")
#endif

#if defined(__APPLE__) && defined(__OBJC__)
#if TARGET_OS_SIMULATOR
#define CN1_GC_ASSERT(condition, message) \
    do { \
        if (!(condition)) { \
            __assert_rtn(__func__, __FILE__, __LINE__, message); \
        } \
    } while (0)
#else
#define CN1_GC_ASSERT(condition, message) \
    do { \
        (void)(condition); \
        (void)(message); \
    } while (0)
#endif
#else
#define CN1_GC_ASSERT(condition, message) CODENAME_ONE_ASSERT(condition)
#endif

// The amount of memory allocated between GC cycle checks (generally 30 seconds)
// that triggers "High-frequency" GC mode.  When "High-frequency" mode is triggered,
// it will only wait 200ms before triggering another GC cycle after completing the
// previous one.  Normally it's 30 seconds.
// This value is in bytes
long CN1_HIGH_FREQUENCY_ALLOCATION_THRESHOLD = 1024 * 1024;

// "High frequency" GC mode won't be enabled until the "total" allocated memory
// in the app reaches this threshold
// This value is in bytes
long CN1_HIGH_FREQUENCY_ALLOCATION_ACTIVATED_THRESHOLD = 10 * 1024 * 1024;


// The number of allocations (not measured in bytes, but actual allocation count) made on
// a thread that will result in the thread being treated as an aggressive allocator.
// If, during GC, it hits a thread that is an aggressive allocator, GC will lock that thread
// until the sweep is complete for all threads.  Normally, the thread is only locked while
// its objects are being marked.
// If the EDT is hitting this threshold, we'll have problems
long CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD = 5000;

long CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD_EDT = 10000;

// The max number of allocations (not bytes, but number) on a thread before 
// it will refuse to increase its size.  This is checked when allocating objects.
// If the thread is at its max size during allocation, it will aggressively call
// the GC and wait until the GC is complete before the allocation can occur.
// On the EDT, this has usability consequences.
// If the allocation array is maxed out, but hasn't reached this max size,
// it will double the size of the allocation array and trigger a GC (but not wait
// for the GC to complete).  
long CN1_MAX_HEAP_SIZE = 10000;

// Special value for the EDT to possibly allow for a larger allocation stack on the 
// EDT.
long CN1_MAX_HEAP_SIZE_EDT = 10000;

// THE THREAD ID OF THE EDT.  We'll treat the EDT specially.
long CN1_EDT_THREAD_ID = -1;

// A flag to indicate if the GC thresholds are initialized yet
// @see init_gc_thresholds
static JAVA_BOOLEAN GC_THRESHOLDS_INITIALIZED = JAVA_FALSE;

int currentGcMarkValue = 1;
#if defined(__APPLE__) && defined(__OBJC__)
extern JAVA_BOOLEAN lowMemoryMode;
#else
JAVA_BOOLEAN lowMemoryMode = JAVA_FALSE;
#endif

static JAVA_BOOLEAN isEdt(long threadId) {
    return (CN1_EDT_THREAD_ID == threadId);
}

// Gets the amount of free memory in the system.
 static long get_free_memory(void)
 {
#if defined(__APPLE__) && defined(__OBJC__)
   mach_port_t host_port;
   mach_msg_type_number_t host_size;
   vm_size_t pagesize;
   host_port = mach_host_self();
   host_size = sizeof(vm_statistics_data_t) / sizeof(integer_t);
   host_page_size(host_port, &pagesize);
   vm_statistics_data_t vm_stat;
   if (host_statistics(host_port, HOST_VM_INFO, (host_info_t)&vm_stat, &host_size) != KERN_SUCCESS)
   {
     #if defined(__OBJC__)
     NSLog(@"Failed to fetch vm statistics");
     #endif
     return 0;
   }
   /* Stats in bytes */
   long mem_free = vm_stat.free_count * pagesize;
   return mem_free;
#else
   return 1024 * 1024 * 100; // Stub: 100MB
#endif
 }

// Initializes the GC thresholds based on the free memory on the device.
// This is run inside the gc mark method.
// Previously we had been hardcoding this stuff, but that causes us to miss out
// on the greater capacity of newer devices.
static void init_gc_thresholds() {
    if (!GC_THRESHOLDS_INITIALIZED) {
        GC_THRESHOLDS_INITIALIZED = JAVA_TRUE;
        
        // On iPhone X, this generally starts with a figure like 388317184 (i.e. ~380 MB)
        long freemem = get_free_memory();
        
        // com.codename1.ui.Container is approx 900 bytes
        // Most allocations are 32 bytes though... so we're making an estimate
        // of the average size of an allocation.  This is based on experimentation and it is crude
        // This is used for trying to estimate how many allocations can be made on a thread
        // before we need to worry.
        long avgAllocSize = 128;
        
        // Estimate the number of allocation slots available in all of memory
        // On iPhone X, this will generally give around 38000 allocation slots
        long maxAllocationSlots = freemem / avgAllocSize;
        
        
        // Set the number of allocations allowed on a thread before it is considered
        // an aggressive allocator.  Aggressive allocator status will cause
        // the thread to lock until the sweep is complete, whereas other threads
        // are only locked during the mark() method.
        // The EDT is treated specially here
        CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD = maxAllocationSlots / 3;
        if (CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD < 5000) {
            CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD = 5000;
        }
        
        // For the EDT, experimenting with never declaring it aggressive (we don't want to block it)
        // unless we've received a low memory warning
        CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD_EDT = maxAllocationSlots * 10;
        
        // Set the high frequency allocation threshold.  If the app has allocated more
        // than the given threshold (in bytes) between GC cycles, it will issue an additional
        // GC cycle immediately after the last one (200ms) (sort of like doubling up.
        // Kind of picking numbers out of the air here.  one fifth of free memory
        // seems alright.
        //CN1_HIGH_FREQUENCY_ALLOCATION_THRESHOLD = freemem / 5;
        //if (CN1_HIGH_FREQUENCY_ALLOCATION_THRESHOLD < 1024 * 1024) {
        //    CN1_HIGH_FREQUENCY_ALLOCATION_THRESHOLD = 1024 * 1024;
        //}
        
        // Set the threshold of total allocated memory before the high-frequency GC cycles
        // are started.
        //CN1_HIGH_FREQUENCY_ALLOCATION_ACTIVATED_THRESHOLD = freemem/2;
        //if (CN1_HIGH_FREQUENCY_ALLOCATION_ACTIVATED_THRESHOLD < 10 * 1024 * 1024) {
        //    CN1_HIGH_FREQUENCY_ALLOCATION_ACTIVATED_THRESHOLD = 10 * 1024 * 1024;
        //}
        
        // GC will be triggered if the the number of allocations on any thread
        // reaches this threshold.  It is checked during malloc, so that
        // if we try to allocate and the number of allocations exceeds this threshold
        // then the thread is stopped until a GC cycle is completed.
        CN1_MAX_HEAP_SIZE = maxAllocationSlots / 3;
        if (CN1_MAX_HEAP_SIZE < 10000) {
            CN1_MAX_HEAP_SIZE = 10000;
        }

        // This might be a bit permissive (allowing the EDT to grow) to the total 
        // max allocation slots - but there are other safeguards in place that should
        // mitigate the harm done.
        CN1_MAX_HEAP_SIZE_EDT = maxAllocationSlots;
        if (CN1_MAX_HEAP_SIZE_EDT < 10000) {
            CN1_MAX_HEAP_SIZE_EDT = 10000;
        }
    }
}

//#define DEBUG_GC_OBJECTS_IN_HEAP

struct clazz class_array1__JAVA_BOOLEAN = {
    DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_BOOLEAN, "boolean[]", JAVA_TRUE, 1, &class__java_lang_Boolean, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_BOOLEAN = {
    DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_BOOLEAN, "boolean[]", JAVA_TRUE, 2, &class__java_lang_Boolean, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_BOOLEAN = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_BOOLEAN, "boolean[]", JAVA_TRUE, 3, &class__java_lang_Boolean, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_CHAR = {
    DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_CHAR, "char[]", JAVA_TRUE, 1, &class__java_lang_Character, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_CHAR = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_CHAR, "char[]", JAVA_TRUE, 2, &class__java_lang_Character, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_CHAR = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_CHAR, "char[]", JAVA_TRUE, 3, &class__java_lang_Character, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_BYTE = {
    DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_BYTE, "byte[]", JAVA_TRUE, 1, &class__java_lang_Byte, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_BYTE = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_BYTE, "byte[]", JAVA_TRUE, 2, &class__java_lang_Byte, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_BYTE = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_BYTE, "byte[]", JAVA_TRUE, 3, &class__java_lang_Byte, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_SHORT = {
    DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_SHORT, "short[]", JAVA_TRUE, 1, &class__java_lang_Short, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_SHORT = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_SHORT, "short[]", JAVA_TRUE, 2, &class__java_lang_Short, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_SHORT = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_SHORT, "short[]", JAVA_TRUE, 3, &class__java_lang_Short, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_INT = {
    DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_INT, "int[]", JAVA_TRUE, 1, &class__java_lang_Integer, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_INT = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_INT, "int[]", JAVA_TRUE, 2, &class__java_lang_Integer, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_INT = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_INT, "int[]", JAVA_TRUE, 3, &class__java_lang_Integer, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_LONG = {
    DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_LONG, "long[]", JAVA_TRUE, 1, &class__java_lang_Long, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_LONG = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_LONG, "long[]", JAVA_TRUE, 2, &class__java_lang_Long, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_LONG = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_LONG, "long[]", JAVA_TRUE, 3, &class__java_lang_Long, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_FLOAT = {
    DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_FLOAT, "float[]", JAVA_TRUE, 1, &class__java_lang_Float, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_FLOAT = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_FLOAT, "float[]", JAVA_TRUE, 2, &class__java_lang_Float, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_FLOAT = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_FLOAT, "float[]", JAVA_TRUE, 3, &class__java_lang_Float, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_DOUBLE = {
    DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_DOUBLE, "double[]", JAVA_TRUE, 1, &class__java_lang_Double, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_DOUBLE = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_DOUBLE, "double[]", JAVA_TRUE, 2, &class__java_lang_Double, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_DOUBLE = {
   DEBUG_GC_INIT 0, 999999, 0, 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_DOUBLE, "double[]", JAVA_TRUE, 3, &class__java_lang_Double, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};


void popMany(CODENAME_ONE_THREAD_STATE, int count, struct elementStruct** SP) {
    while(count > 0) {
        --(*SP);
        javaTypes t = (*SP)->type;
        if(t == CN1_TYPE_DOUBLE || t == CN1_TYPE_LONG) {
            count -= 2;
        } else {
            count--;
        }
    }
}


JAVA_OBJECT* constantPoolObjects = 0;

struct elementStruct* BC_DUP2_X2_DD(struct elementStruct* SP) {
    (*SP).data.l = SP[-1].data.l;
    SP[-1].data.l = SP[-2].data.l;
    SP[-2].data.l = (*SP).data.l;
    (*SP).type = SP[-1].type;
    SP[-1].type = SP[-2].type;
    SP[-2].type = (*SP).type;
    return (struct elementStruct*)(SP+1);
}
struct elementStruct* BC_DUP2_X2_DSS(struct elementStruct* SP) {
    SP[0].data.l = SP[-1].data.l;
    SP[-1].data.l = SP[-2].data.l;
    SP[-2].data.l = SP[-3].data.l;
    SP[-3].data.l = SP[0].data.l;
    SP[0].type = SP[-1].type;
    SP[-1].type = SP[-2].type;
    SP[-2].type = SP[-3].type;
    SP[-3].type = SP[0].type;
    return SP+1;
}
struct elementStruct* BC_DUP2_X2_SSD(struct elementStruct* SP) {
    SP[1].data.l = SP[-1].data.l;
    SP[0].data.l = SP[-2].data.l;
    SP[-1].data.l = SP[-3].data.l;
    SP[-2].data.l = SP[1].data.l;
    SP[-3].data.l = SP[0].data.l;
    SP[1].type = SP[-1].type;
    SP[0].type = SP[-2].type;
    SP[-1].type = SP[-3].type;
    SP[-2].type = SP[1].type;
    SP[-3].type = SP[0].type;
    return SP+2;
}
struct elementStruct* BC_DUP2_X2_SSSS(struct elementStruct* SP) {
    SP[1].data.l = SP[-1].data.l;
    SP[0].data.l = SP[-2].data.l;
    SP[-1].data.l = SP[-3].data.l;
    SP[-2].data.l = SP[-4].data.l;
    SP[-3].data.l = SP[1].data.l;
    SP[-4].data.l = SP[0].data.l;
    SP[1].type = SP[-1].type;
    SP[0].type = SP[-2].type;
    SP[-1].type = SP[-3].type;
    SP[-2].type = SP[-4].type;
    SP[-3].type = SP[1].type;
    SP[-4].type = SP[0].type;
    return SP+2;
}

struct elementStruct* BC_DUP_X2_SD(struct elementStruct* SP) {
    SP[0].data.l = SP[-1].data.l;
    SP[-1].data.l = SP[-2].data.l;
    SP[-2].data.l = SP[0].data.l;
    SP[0].type = SP[-1].type;
    SP[-1].type = SP[-2].type;
    SP[-2].type = SP[0].type;
    return SP+1;
}

struct elementStruct* BC_DUP_X2_SSS(struct elementStruct* SP) {
    SP[0].data.l = SP[-1].data.l;
    SP[-1].data.l = SP[-2].data.l;
    SP[-2].data.l = SP[-3].data.l;
    SP[-3].data.l = SP[0].data.l;
    SP[0].type = SP[-1].type;
    SP[-1].type = SP[-2].type;
    SP[-2].type = SP[-3].type;
    SP[-3].type = SP[0].type;
    return SP+1;
}


int instanceofFunction(int sourceClass, int destId) {
    if(sourceClass == destId) {
        return JAVA_TRUE;
    }
    if (sourceClass == cn1_array_1_id_JAVA_INT && destId == cn1_class_id_java_lang_Object) {
        int foo = 1;
    }
    if (destId == cn1_array_1_id_JAVA_INT && sourceClass == cn1_class_id_java_lang_Object) {
        int foo = 1;
    }
    if(sourceClass >= cn1_array_start_offset || destId >= cn1_array_start_offset) {
        
        // (destId instanceof sourceClass)
        // E.g. (new int[0] instanceof Object) ===> sourceClass==Object and destId=int[]
        
        if (sourceClass < cn1_array_start_offset) {
            return sourceClass == cn1_class_id_java_lang_Object;
        }  else if (destId < cn1_array_start_offset) {
            return JAVA_FALSE;
        }
        
        // At this point we know that both sourceClass and destId are array types
        
        // The start offset for reference array types
        int refArrayStartOffset = cn1_array_start_offset+100;
        if (sourceClass < refArrayStartOffset || destId < refArrayStartOffset) {
            if (sourceClass >= refArrayStartOffset) {
                // We need to deal with things like (int[][] instanceof Object[])
                int srcDim = (sourceClass - refArrayStartOffset)%3+1;
                int destDim = (destId - cn1_array_start_offset)%4;
                
                if (srcDim < destDim) {
                    if (srcDim > 1) {
                        sourceClass = sourceClass-1;
                    } else {
                        sourceClass =(sourceClass - refArrayStartOffset)/3;
                    }
                    return instanceofFunction(sourceClass, destId-1);
                }
            }
            // if either is primitive, then they must be the same type.
            return sourceClass == destId;
        }
        int srcDimension = (sourceClass - refArrayStartOffset)%3+1;
        int destDimension = (destId - refArrayStartOffset)%3+1;
        
        int sourceClassComponentTypeId = srcDimension > 1 ? sourceClass-1 : (sourceClass - refArrayStartOffset)/3;
        int destClassComponentTypeId = destDimension > 1 ? destId-1 : (destId - refArrayStartOffset)/3;
        return instanceofFunction(sourceClassComponentTypeId, destClassComponentTypeId);
    }
    
    int* i = classInstanceOf[destId];
    int counter = 0;
    while(i[counter] > -1) {
        if(i[counter] == sourceClass) {
            return JAVA_TRUE;
        }
        i++;
    }
    return JAVA_FALSE;
}



JAVA_OBJECT* releaseQueue = 0;
JAVA_INT releaseQueueSize = 0;
typedef void (*finalizerFunctionPointer)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);

// invokes finalizers and iterates over the release queue
void flushReleaseQueue() {
}

void freeAndFinalize(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    finalizerFunctionPointer ptr = (finalizerFunctionPointer)obj->__codenameOneParentClsReference->finalizerFunction;
    if(ptr != 0) {
        ptr(threadStateData, obj);
    }
    codenameOneGcFree(threadStateData, obj);
}

/**
 * Invoked to destroy an array and release all the objects within it
 */
void arrayFinalizerFunction(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array) {
}

BOOL invokedGC = NO;
extern int findPointerPosInHeap(JAVA_OBJECT obj);
extern pthread_mutex_t* getMemoryAccessMutex();
extern long gcThreadId;

void gcReleaseObj(JAVA_OBJECT o) {
}

// Lazily computes the per-thread native C-stack low-water mark consulted by
// CN1_FRAMELESS_SOE_GUARD. On Apple/macOS/iOS pthread exposes the stack base and
// size directly; elsewhere we fall back to anchoring off the current frame with a
// generous (8MB) assumed stack. The guard band is subtracted so there is room to
// build + throw the StackOverflowError once the limit is crossed.
void cn1ComputeNativeStackLimit(CODENAME_ONE_THREAD_STATE) {
#if defined(__APPLE__) || defined(__MACH__)
    void* stackBase = pthread_get_stackaddr_np(pthread_self());
    size_t stackSize = pthread_get_stacksize_np(pthread_self());
    threadStateData->nativeStackLimit = (JAVA_LONG)(intptr_t)stackBase
            - (JAVA_LONG)stackSize
            + (JAVA_LONG)CN1_FRAMELESS_STACK_GUARD_BAND;
#else
    // Portable fallback: pthread stack introspection is unavailable, so anchor off
    // the current frame and assume an 8MB stack below it.
    threadStateData->nativeStackLimit = (JAVA_LONG)(intptr_t)__builtin_frame_address(0)
            - (JAVA_LONG)(8L * 1024L * 1024L)
            + (JAVA_LONG)CN1_FRAMELESS_STACK_GUARD_BAND;
#endif
    // Guard against a degenerate (0) result, which would re-trigger computation
    // every call; if introspection yielded nothing usable, disable the limit.
    if (threadStateData->nativeStackLimit == 0) {
        threadStateData->nativeStackLimit = 1;
    }
}

// memory map of all the heap objects which we can walk over to delete/deallocate
// unused objects
JAVA_OBJECT* allObjectsInHeap = 0; 
JAVA_OBJECT* oldAllObjectsInHeap = 0;
int sizeOfAllObjectsInHeap = 30000;
int currentSizeOfAllObjectsInHeap = 0;
pthread_mutex_t* memoryAccessMutex = NULL;

pthread_mutex_t* getMemoryAccessMutex() {
    if(memoryAccessMutex == NULL) {
        memoryAccessMutex = malloc(sizeof(pthread_mutex_t));
        pthread_mutex_init(memoryAccessMutex, NULL);
    }
    return memoryAccessMutex;
}

int findPointerPosInHeap(JAVA_OBJECT obj) {
    if(obj == 0) {
        return -1;
    }
    return obj->__heapPosition;
}

// this is an optimization allowing us to continue searching for available space in RAM from the previous position
// that way we avoid looping over elements that we already probably checked
int lastOffsetInRam = 0;
void placeObjectInHeapCollection(JAVA_OBJECT obj) {
    if(allObjectsInHeap == 0) {
        allObjectsInHeap = malloc(sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
        memset(allObjectsInHeap, 0, sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
    }
    if(currentSizeOfAllObjectsInHeap < sizeOfAllObjectsInHeap) {
        allObjectsInHeap[currentSizeOfAllObjectsInHeap] = obj;
        obj->__heapPosition = currentSizeOfAllObjectsInHeap;
        currentSizeOfAllObjectsInHeap++;
    } else {
        int pos = -1;
        JAVA_OBJECT* currentAllObjectsInHeap = allObjectsInHeap;
        int currentSize = currentSizeOfAllObjectsInHeap;
        for(int iter = lastOffsetInRam ; iter < currentSize ; iter++) {
            if(currentAllObjectsInHeap[iter] == JAVA_NULL) {
                pos = iter;
                lastOffsetInRam = pos;
                break;
            }
        }
        if(pos < 0 && lastOffsetInRam > 0) {
            // just make sure there is nothing at the start
            for(int iter = 0 ; iter < lastOffsetInRam ; iter++) {
                if(currentAllObjectsInHeap[iter] == JAVA_NULL) {
                    pos = iter;
                    lastOffsetInRam = pos;
                    break;
                }
            }
        }
        if(pos < 0) {
            // we need to enlarge the block
            JAVA_OBJECT* tmpAllObjectsInHeap = malloc(sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap * 2);
            memset(tmpAllObjectsInHeap + sizeOfAllObjectsInHeap, 0, sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
            memcpy(tmpAllObjectsInHeap, allObjectsInHeap, sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
            sizeOfAllObjectsInHeap *= 2;
            // Defer freeing the replaced array by one growth cycle: the sweep and the
            // reference-counting removal path read allObjectsInHeap without taking the
            // critical section, so an immediate free can pull the array out from under
            // an in-flight read. Growths double the capacity so they are rare, and at
            // most one stale array is retained.
            if(oldAllObjectsInHeap != 0) {
                free(oldAllObjectsInHeap);
            }
            oldAllObjectsInHeap = allObjectsInHeap;
            allObjectsInHeap = tmpAllObjectsInHeap;
            // record the real slot -- leaving pos at -1 here left the object's
            // __heapPosition unset, so a later reference-counted free could not null
            // its slot and the sweep would dereference the dangling pointer.
            pos = currentSizeOfAllObjectsInHeap;
            allObjectsInHeap[pos] = obj;
            currentSizeOfAllObjectsInHeap++;
        } else {
            allObjectsInHeap[pos] = obj;
        }
        obj->__heapPosition = pos;
    }
}

extern struct ThreadLocalData** allThreads; 
extern int nThreadsToKill;

JAVA_BOOLEAN hasAgressiveAllocator;

#ifndef CN1_DISABLE_BIBOP
extern void cn1BibopRetireThreadPages();
#endif

// the thread just died, mark its remaining resources
void collectThreadResources(struct ThreadLocalData *current)
{
#ifndef CN1_DISABLE_BIBOP
    // Retire this (dying) thread's current BiBOP pages so their slots become
    // collectable. Runs on the dying thread, so its __thread current pages are
    // reachable here.
    cn1BibopRetireThreadPages();
#endif
    if(current->utf8Buffer != 0) {
        free(current->utf8Buffer);
        current->utf8Buffer = 0;
    }
    for(int heapTrav = 0 ; heapTrav < current->heapAllocationSize ; heapTrav++) {
        JAVA_OBJECT obj = (JAVA_OBJECT)current->pendingHeapAllocations[heapTrav];
        if(obj) {
            current->pendingHeapAllocations[heapTrav] = 0;
            placeObjectInHeapCollection(obj);
        }
    }
}
static void gcMarkDrain(CODENAME_ONE_THREAD_STATE);
// Parallel variant of gcMarkDrain: fans the transitive mark-drain out across a small
// pool of worker threads. Falls back to the serial gcMarkDrain when only one marker
// is configured. Defined further down (after gcMarkDrain). See the big comment block
// at the worklist declarations for the design and the invariants it preserves.
static void gcMarkDrainParallel(CODENAME_ONE_THREAD_STATE);

/**
 * A simple concurrent mark algorithm that traverses the currently running threads
 */
void codenameOneGCMark() {
    currentGcMarkValue++;
    init_gc_thresholds();
    hasAgressiveAllocator = JAVA_FALSE;
    struct ThreadLocalData* d = getThreadLocalData();
    //int marked = 0;
    
    // copy the allocated objects from already deleted threads so we can delete that data
    #if defined(__OBJC__)
    //NSLog(@"GC mark, %d dead processes pending",nThreadsToKill);
    #endif
    
    for(int iter = 0 ; iter < NUMBER_OF_SUPPORTED_THREADS ; iter++) {
        lockCriticalSection();
        struct ThreadLocalData* t = allThreads[iter];
        unlockCriticalSection();
        if(t != 0) {
            if(t->currentThreadObject != JAVA_NULL) {
                gcMarkObject(t, t->currentThreadObject, JAVA_FALSE);
            }
            if(t != d) {
                struct elementStruct* objects = t->threadObjectStack;
                
                // wait for the thread to pause so we can traverse its stack but not for native threads where
                // we don't have much control and who barely call into Java anyway
                if(t->lightweightThread) {
                    t->threadBlockedByGC = JAVA_TRUE;
                    int totalwait = 0;
                    long now = time(0);
                    while(t->threadActive) {
                        usleep(500);
                        totalwait += 500;
                        if((totalwait%10000)==0)
                        {   long later = time(0)-now;
                            if(later>10000)
                            {
#if defined(__OBJC__)
                            NSLog(@"GC trapped for %d seconds waiting for thread %d in slot %d (%d)",
                                  (int)(later/1000),(int)t->threadId,iter,t->threadKilled);
#endif
                            }
                        }
                    }
                }
                
                // place allocations from the local thread into the global heap list.
                // The critical section serializes this migration against
                // markDeadThread/collectThreadResources: the pause-wait above ends when
                // threadActive drops, but a thread that finishes runImpl drops
                // threadActive through markDeadThread, so without the lock both sides
                // migrate the same pendingHeapAllocations concurrently -- double-placing
                // objects and racing placeObjectInHeapCollection's grow-and-free of
                // allObjectsInHeap (double free / use-after-free, observed as random
                // SIGSEGV or a libmalloc abort that wedges the VM). If the slot no
                // longer holds this thread it died and markDeadThread already migrated
                // everything under this same lock; skip.
                lockCriticalSection();
                if(allThreads[iter] == t) {
                    if (!t->lightweightThread) {
                        // For native threads, we need to actually lock them while we traverse the
                        // heap allocations because we can't use the usual locking mechanisms on
                        // them.
                        lockThreadHeapMutex();
                    }
                    for(int heapTrav = 0 ; heapTrav < t->heapAllocationSize ; heapTrav++) {
                        JAVA_OBJECT obj = (JAVA_OBJECT)t->pendingHeapAllocations[heapTrav];
                        if(obj) {
                            t->pendingHeapAllocations[heapTrav] = 0;
                            placeObjectInHeapCollection(obj);
                        }
                    }
                    if (!t->lightweightThread) {
                        unlockThreadHeapMutex();
                    }
                }
                unlockCriticalSection();
                
                // this is a thread that allocates a lot and might demolish RAM. We will hold it until the sweep is finished...
                
                JAVA_INT allocSize = t->heapAllocationSize;
                JAVA_BOOLEAN agressiveAllocator = JAVA_FALSE;
                if (isEdt(t->threadId) && !lowMemoryMode) {
                    agressiveAllocator = allocSize > CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD_EDT;
                } else {
                    agressiveAllocator = allocSize > CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD;
                }
                if (CN1_EDT_THREAD_ID == t->threadId && agressiveAllocator) {
                    long freeMemory = get_free_memory();
                    #if defined(__OBJC__)
                    NSLog(@"[GC] Blocking EDT as aggressive allocator, free memory=%lld", freeMemory);
                    #endif
                    
                }
                
                t->heapAllocationSize = 0;
                
                int stackSize = t->threadObjectStackOffset;
                for(int stackIter = 0 ; stackIter < stackSize ; stackIter++) {
                    struct elementStruct* current = &t->threadObjectStack[stackIter];
                    if (current->type < CN1_TYPE_INVALID || current->type > CN1_TYPE_PRIMITIVE) {
#if defined(__APPLE__) && defined(__OBJC__)
#if TARGET_OS_SIMULATOR
                        CN1_GC_ASSERT(current->type >= CN1_TYPE_INVALID && current->type <= CN1_TYPE_PRIMITIVE,
                            "CN1_GC_STACK_ENTRY_TYPE");
#else
                        #if defined(__OBJC__)
                        NSLog(@"[GC] Invalid stack entry type %d at index %d; skipping entry", current->type, stackIter);
                        #endif
                        continue;
#endif
#else
                        CN1_GC_ASSERT(current->type >= CN1_TYPE_INVALID && current->type <= CN1_TYPE_PRIMITIVE,
                            "CN1_GC_STACK_ENTRY_TYPE");
#endif
                    }
                    if(current != 0 && current->type == CN1_TYPE_OBJECT && current->data.o != JAVA_NULL) {
                        gcMarkObject(t, current->data.o, JAVA_FALSE);
                        //marked++;
                    }
                }
                markStatics(d);
                // Drain the worklist before unblocking the thread so that every object
                // transitively reachable from this thread's roots is fully marked while the
                // thread is still paused -- matching the snapshot-at-the-beginning property
                // the recursive implementation had. Without this drain, an unblocked mutator
                // can read a still-grey field reference into a new local and null the field;
                // the captured object would never be visited by the final drain, sweep would
                // reclaim it, and a later monitorEnter on its freed pthread_mutex_t would
                // silently deadlock. Earlier attempts at this drain hung at app startup
                // because the overflow rescan path had a cursor-reset bug; with that fixed
                // below, the drain runs to completion in O(reachable) time.
                //
                // The drain is fanned out across a worker pool (gcMarkDrainParallel).
                // This still satisfies snapshot-at-the-beginning: the roots were already
                // pushed onto the worklist serially above (while this thread is paused),
                // and gcMarkDrainParallel does not return until the entire reachable set
                // is marked -- it just marks it faster. With a single configured marker
                // it degrades to the serial gcMarkDrain and is byte-for-byte identical.
                gcMarkDrainParallel(d);
                if(!agressiveAllocator) {
                    t->threadBlockedByGC = JAVA_FALSE;
                } else {
                    hasAgressiveAllocator = JAVA_TRUE;
                }
            }
        }
    }
    #if defined(__OBJC__)
    //NSLog(@"Mark set %i objects to %i", marked, currentGcMarkValue);
    #endif
    // since they are immutable this probably doesn't need as much sync as the statics...
    for(int iter = 0 ; iter < CN1_CONSTANT_POOL_SIZE ; iter++) {
        gcMarkObject(d, (JAVA_OBJECT)constantPoolObjects[iter], JAVA_TRUE);
    }

    // Drain the worklist that the calls above populated. gcMarkObject no longer recurses
    // through reference fields, so we need an explicit drain pass before sweep runs.
    gcMarkDrain(d);
}

#ifdef DEBUG_GC_OBJECTS_IN_HEAP
int totalAllocatedHeap = 0;
int getObjectSize(JAVA_OBJECT o) {
    int* ptr = (int*)o;
    ptr--;
    return *ptr;
}

int classTypeCountPreSweep[cn1_array_3_id_java_util_Vector + 1];
int sizeInHeapForTypePreSweep[cn1_array_3_id_java_util_Vector + 1];
int nullSpacesPreSweep = 0;
int preSweepRam;
void preSweepCount(CODENAME_ONE_THREAD_STATE) {
    preSweepRam = totalAllocatedHeap;
    memset(classTypeCountPreSweep, 0, sizeof(int) * cn1_array_3_id_java_util_Vector + 1);
    memset(sizeInHeapForTypePreSweep, 0, sizeof(int) * cn1_array_3_id_java_util_Vector + 1);
    int t = currentSizeOfAllObjectsInHeap;
    int nullSpacesPreSweep = 0;
    for(int iter = 0 ; iter < t ; iter++) {
        JAVA_OBJECT o = allObjectsInHeap[iter];
        if(o != JAVA_NULL) {
            classTypeCountPreSweep[o->__codenameOneParentClsReference->classId]++;
            sizeInHeapForTypePreSweep[o->__codenameOneParentClsReference->classId] += getObjectSize(o);
        } else {
            nullSpacesPreSweep++;
        }
    }
}

void printObjectsPostSweep(CODENAME_ONE_THREAD_STATE) {
#if defined(__APPLE__) && defined(__OBJC__)
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
#endif
    
    // this should be the last class used
    int classTypeCount[cn1_array_3_id_java_util_Vector + 1];
    int sizeInHeapForType[cn1_array_3_id_java_util_Vector + 1];
    memset(classTypeCount, 0, sizeof(int) * cn1_array_3_id_java_util_Vector + 1);
    memset(sizeInHeapForType, 0, sizeof(int) * cn1_array_3_id_java_util_Vector + 1);
    int nullSpaces = 0;
    const char** arrayOfNames = malloc(sizeof(char*) * cn1_array_3_id_java_util_Vector + 1);
    memset(arrayOfNames, 0, sizeof(char*) * cn1_array_3_id_java_util_Vector + 1);
    
    int t = currentSizeOfAllObjectsInHeap;
    for(int iter = 0 ; iter < t ; iter++) {
        JAVA_OBJECT o = allObjectsInHeap[iter];
        if(o != JAVA_NULL) {
            classTypeCount[o->__codenameOneParentClsReference->classId]++;
            sizeInHeapForType[o->__codenameOneParentClsReference->classId] += getObjectSize(o);
            if(o->__codenameOneParentClsReference->classId > cn1_array_start_offset) {
                if(arrayOfNames[o->__codenameOneParentClsReference->classId] == 0) {
                    arrayOfNames[o->__codenameOneParentClsReference->classId] = o->__codenameOneParentClsReference->clsName;
                }
            }
        } else {
            nullSpaces++;
        }
    }
    int actualTotalMemory = 0;
    #if defined(__OBJC__)
    NSLog(@"\n\n**** There are %i - %i = %i nulls available entries out of %i objects in heap which take up %i, sweep saved %i ****", nullSpaces, nullSpacesPreSweep, nullSpaces - nullSpacesPreSweep, t, totalAllocatedHeap, preSweepRam - totalAllocatedHeap);
    #endif
    for(int iter = 0 ; iter < cn1_array_3_id_java_util_Vector ; iter++) {
        if(classTypeCount[iter] > 0) {
            if(classTypeCountPreSweep[iter] - classTypeCount[iter] > 0) {
                if(iter > cn1_array_start_offset) {
#if defined(__APPLE__) && defined(__OBJC__)
                    #if defined(__OBJC__)
                    NSLog(@"There are %i instances of %@ taking up %i bytes, %i were cleaned which saved %i bytes", classTypeCount[iter], [NSString stringWithUTF8String:arrayOfNames[iter]], sizeInHeapForType[iter], classTypeCountPreSweep[iter] - classTypeCount[iter], sizeInHeapForTypePreSweep[iter] - sizeInHeapForType[iter]);
                    #endif
#endif
                } else {
                    JAVA_OBJECT str = STRING_FROM_CONSTANT_POOL_OFFSET(classNameLookup[iter]);
#if defined(__APPLE__) && defined(__OBJC__)
                    #if defined(__OBJC__)
                    NSLog(@"There are %i instances of %@ taking up %i bytes, %i were cleaned which saved %i bytes", classTypeCount[iter], toNSString(threadStateData, str), sizeInHeapForType[iter], classTypeCountPreSweep[iter] - classTypeCount[iter], sizeInHeapForTypePreSweep[iter] - sizeInHeapForType[iter]);
                    #endif
#endif
                }
            }
            actualTotalMemory += sizeInHeapForType[iter];
        }
    }
    #if defined(__OBJC__)
    //NSLog(@"Actual ram = %i vs total mallocs = %i", actualTotalMemory, totalAllocatedHeap);
    #endif
    #if defined(__OBJC__)
    NSLog(@"**** GC cycle complete ****");
    #endif
    
    free(arrayOfNames);
#if defined(__APPLE__) && defined(__OBJC__)
    [pool release];
#endif
}

void printObjectTypesInHeap(CODENAME_ONE_THREAD_STATE) {
#if defined(__APPLE__) && defined(__OBJC__)
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
#endif
    
    // this should be the last class used
    int classTypeCount[cn1_array_3_id_java_util_Vector + 1];
    int sizeInHeapForType[cn1_array_3_id_java_util_Vector + 1];
    memset(classTypeCount, 0, sizeof(int) * cn1_array_3_id_java_util_Vector + 1);
    memset(sizeInHeapForType, 0, sizeof(int) * cn1_array_3_id_java_util_Vector + 1);
    int nullSpaces = 0;
    const char** arrayOfNames = malloc(sizeof(char*) * cn1_array_3_id_java_util_Vector + 1);
    memset(arrayOfNames, 0, sizeof(char*) * cn1_array_3_id_java_util_Vector + 1);
    
    int t = currentSizeOfAllObjectsInHeap;
    for(int iter = 0 ; iter < t ; iter++) {
        JAVA_OBJECT o = allObjectsInHeap[iter];
        if(o != JAVA_NULL) {
            classTypeCount[o->__codenameOneParentClsReference->classId]++;
            sizeInHeapForType[o->__codenameOneParentClsReference->classId] += getObjectSize(o);
            if(o->__codenameOneParentClsReference->classId > cn1_array_start_offset) {
                if(arrayOfNames[o->__codenameOneParentClsReference->classId] == 0) {
                    arrayOfNames[o->__codenameOneParentClsReference->classId] = o->__codenameOneParentClsReference->clsName;
                }
            }
        } else {
            nullSpaces++;
        }
    }
    int actualTotalMemory = 0;
    #if defined(__OBJC__)
    NSLog(@"There are %i null available entries out of %i objects in heap which take up %i", nullSpaces, t, totalAllocatedHeap);
    #endif
    for(int iter = 0 ; iter < cn1_array_3_id_java_util_Vector ; iter++) {
        if(classTypeCount[iter] > 0) {
            float f = ((float)classTypeCount[iter]) / ((float)t) * 100.0f;
            float f2 = ((float)sizeInHeapForType[iter]) / ((float)totalAllocatedHeap) * 100.0f;
            if(iter > cn1_array_start_offset) {
#if defined(__APPLE__) && defined(__OBJC__)
                #if defined(__OBJC__)
                NSLog(@"There are %i instances of %@ which is %i percent its %i bytes which is %i mem percent", classTypeCount[iter], [NSString stringWithUTF8String:arrayOfNames[iter]], (int)f, sizeInHeapForType[iter], (int)f2);
                #endif
#endif
            } else {
                JAVA_OBJECT str = STRING_FROM_CONSTANT_POOL_OFFSET(classNameLookup[iter]);
#if defined(__APPLE__) && defined(__OBJC__)
                #if defined(__OBJC__)
                NSLog(@"There are %i instances of %@ which is %i percent its %i bytes which is %i mem percent", classTypeCount[iter], toNSString(threadStateData, str), (int)f, sizeInHeapForType[iter], (int)f2);
                #endif
#endif
            }
            actualTotalMemory += sizeInHeapForType[iter];
        }
    }
    #if defined(__OBJC__)
    NSLog(@"Actual ram = %i vs total mallocs = %i", actualTotalMemory, totalAllocatedHeap);
    #endif
    
    free(arrayOfNames);
#if defined(__APPLE__) && defined(__OBJC__)
    [pool release];
#endif
}
#endif

/**
 * The sweep GC phase iterates the memory block and deletes unmarked memory
 * since it always runs from the same thread and concurrent work doesn't matter
 * it can just delete everything it finds
 */
#ifndef CN1_DISABLE_BIBOP
static void cn1BibopSweep(CODENAME_ONE_THREAD_STATE);
#endif
void codenameOneGCSweep() {
    struct ThreadLocalData* threadStateData = getThreadLocalData();
#ifndef CN1_DISABLE_BIBOP
    // Reclaim dead slots on retired BiBOP pages (rebuild per-page free-lists from
    // the header epoch marks). Runs first, on the GC thread, with no marking in
    // flight and no mutator owning these pages.
    cn1BibopSweep(threadStateData);
#endif
#ifdef DEBUG_GC_OBJECTS_IN_HEAP
    preSweepCount(threadStateData);
#endif
    //int counter = 0;
    int t = currentSizeOfAllObjectsInHeap;
    for(int iter = 0 ; iter < t ; iter++) {
        JAVA_OBJECT o = allObjectsInHeap[iter];
        if(o != JAVA_NULL) {
            if(o->__codenameOneGcMark != -1) {
                if(o->__codenameOneGcMark < currentGcMarkValue - 1) {
                    if (o->__codenameOneGcMark <= 0) {
#if defined(__APPLE__) && defined(__OBJC__)
#if TARGET_OS_SIMULATOR
                        CN1_GC_ASSERT(o->__codenameOneGcMark > 0, "CN1_GC_INVALID_MARK");
#else
                        #if defined(__OBJC__)
                        NSLog(@"[GC] Invalid GC mark %d for object %p; skipping sweep", o->__codenameOneGcMark, o);
                        #endif
                        continue;
#endif
#else
                        CN1_GC_ASSERT(o->__codenameOneGcMark > 0, "CN1_GC_INVALID_MARK");
#endif
                    }
                    allObjectsInHeap[iter] = JAVA_NULL;
                    //if(o->__codenameOneReferenceCount > 0) {
                    #if defined(__OBJC__)
                    //    NSLog(@"Sweped %X", (int)o);
                    #endif
                    //}
                    
#ifdef DEBUG_GC_ALLOCATIONS
                    int classId = o->className;
#if defined(__APPLE__) && defined(__OBJC__)
                    NSString* whereIs;
                    if(classId > 0) {
                        whereIs = (NSString*)((struct obj__java_lang_String*)STRING_FROM_CONSTANT_POOL_OFFSET(classId))->java_lang_String_nsString;
                    } else {
                        whereIs = @"unknown";
                    }
                    
                    if(o->__codenameOneParentClsReference->isArray) {
                        JAVA_ARRAY arr = (JAVA_ARRAY)o;
                        if(arr->__codenameOneParentClsReference == &class_array1__JAVA_CHAR) {
                            JAVA_ARRAY_CHAR* ch = (JAVA_ARRAY_CHAR*)arr->data;
                            char data[arr->length + 1];
                            for(int iter = 0 ; iter < arr->length ; iter++) {
                                data[iter] = ch[iter];
                            }
                            data[arr->length] = 0;
                            #if defined(__OBJC__)
                            NSLog(@"Sweeping: %X, Mark: %i, Allocated: %@ %i type: %@, which is: '%@'", (int)o, o->__codenameOneGcMark, whereIs, o->line, [NSString stringWithUTF8String:o->__codenameOneParentClsReference->clsName], [NSString stringWithUTF8String:data]);
                            #endif
                        } else {
                            #if defined(__OBJC__)
                            NSLog(@"Sweeping: %X, Mark: %i, Allocated: %@ %i , type: %@", (int)o, o->__codenameOneGcMark, whereIs, o->line, [NSString stringWithUTF8String:o->__codenameOneParentClsReference->clsName]);
                            #endif
                        }
                    } else {
                        JAVA_OBJECT str = java_lang_Object_toString___R_java_lang_String(threadStateData, o);
                        NSString* ns = toNSString(threadStateData, str);
                        if(ns == nil) {
                            ns = @"[NULL]";
                        }
                        #if defined(__OBJC__)
                        NSLog(@"Sweeping: %X, Mark: %i, Allocated: %@ %i , type: %@, toString: '%@'", (int)o, o->__codenameOneGcMark, whereIs, o->line, [NSString stringWithUTF8String:o->__codenameOneParentClsReference->clsName], ns);
                        #endif
                    }
#endif
#endif
                    
                    removeObjectFromHeapCollection(threadStateData, o);
                    freeAndFinalize(threadStateData, o);
                    //counter++;
                }
            } else {
                o->__codenameOneGcMark = currentGcMarkValue;
            }
        }
    }
    
    // we had a thread that really ripped into the GC so we only release that thread now after cleaning RAM
    if(hasAgressiveAllocator) {
        for(int iter = 0 ; iter < NUMBER_OF_SUPPORTED_THREADS ; iter++) {
            lockCriticalSection();
            struct ThreadLocalData* t = allThreads[iter];
            unlockCriticalSection();
            if(t != 0) {
                t->threadBlockedByGC = JAVA_FALSE;
            }
        }
    }
    
#ifdef DEBUG_GC_OBJECTS_IN_HEAP
    //printObjectTypesInHeap(threadStateData);
    printObjectsPostSweep(threadStateData);
#endif
}

JAVA_BOOLEAN removeObjectFromHeapCollection(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    // Initialize allObjectsInHeap if it hasn't been initialized yet
    // This can happen if GC runs before any objects are allocated
    if(allObjectsInHeap == 0) {
        allObjectsInHeap = malloc(sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
        memset(allObjectsInHeap, 0, sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
    }

    int pos = findPointerPosInHeap(o);

    // double deletion might occur when the GC and the reference counting collide
    if(pos < 0) {
        // check the local thread heap
        for(int heapTrav = 0 ; heapTrav < threadStateData->heapAllocationSize ; heapTrav++) {
            JAVA_OBJECT obj = (JAVA_OBJECT)threadStateData->pendingHeapAllocations[heapTrav];
            if(obj == o) {
                threadStateData->pendingHeapAllocations[heapTrav] = JAVA_NULL;
                return JAVA_TRUE;
            }
        }
        return JAVA_FALSE;
    }
    o->__heapPosition = -1;

    allObjectsInHeap[pos] = JAVA_NULL;
    
    return JAVA_TRUE;
}

extern JAVA_BOOLEAN gcCurrentlyRunning;
int allocationsSinceLastGC = 0;
long long totalAllocations = 0;
long long cn1_instr_allocCount = 0;

JAVA_BOOLEAN java_lang_System_isHighFrequencyGC___R_boolean(CODENAME_ONE_THREAD_STATE) {
    int alloc = allocationsSinceLastGC;
    allocationsSinceLastGC = 0;
    return alloc > CN1_HIGH_FREQUENCY_ALLOCATION_THRESHOLD && totalAllocations > CN1_HIGH_FREQUENCY_ALLOCATION_ACTIVATED_THRESHOLD;
}

JAVA_INT java_lang_System_identityHashCode___java_lang_Object_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    return (JAVA_INT)__cn1Arg1;
}

#if defined(__APPLE__) && defined(__OBJC__)
extern int mallocWhileSuspended;
extern BOOL isAppSuspended;
#else
int mallocWhileSuspended = 0;
BOOL isAppSuspended = 0;
#endif

#ifndef CN1_DISABLE_BIBOP
// =========================================================================
// BiBOP: non-moving segregated-fits page heap + mark-sweep for SMALL non-array
// objects. Objects NEVER move (stable addresses, real pointers, real array
// offsets / SIMD alignment preserved). Arrays and objects larger than
// CN1_BIBOP_MAX_OBJECT keep the legacy calloc + allObjectsInHeap + table-sweep
// path verbatim. Gate the whole thing off with -DCN1_DISABLE_BIBOP for A/B.
//
// LIVENESS SOURCE OF TRUTH = the per-object header epoch mark
// (__codenameOneGcMark), exactly as the legacy collector. gcMarkObject is
// therefore UNCHANGED and works uniformly on page slots and table objects: a
// page slot has the same header layout as any object. We deliberately did NOT
// introduce a separate per-page mark bitmap; reusing the proven epoch + grace
// semantics (mark==-1 => grace; mark < cur-1 => dead) eliminates an entire
// class of mark-path races (no new claim path, no bitmap/epoch skew) and is
// what makes the "bit-identical + TSan-clean" gates reachable. The win is the
// dropped per-object registration (no placeObjectInHeapCollection, small
// objects absent from the giant allObjectsInHeap table) + word-free-list sweep.
//
// PAGE LIFECYCLE (a page is in exactly ONE role at a time):
//   FREE pool      empty page, reusable for any size class
//   PARTIAL pool   swept page w/ free slots AND some live objects (per class)
//   OWNED          a single thread bump/free-list allocates from it (NOT swept)
//   SWEEP stack    retired page (full, or from a dead thread) awaiting sweep
// Transitions: alloc pulls PARTIAL|FREE -> OWNED (under bibopMutex); a full
// OWNED page is retired -> SWEEP stack (under bibopMutex); the sweep snapshots
// the SWEEP stack via an atomic head-swap and routes each page to FREE/PARTIAL.
// A page is NEVER simultaneously allocated-into and swept (hard point #2).
//
// HARD POINT #1 (allocate-during-GC / new objects survive): a freshly
// allocated slot gets header mark = -1, and the sweep gives mark==-1 the same
// one-cycle grace the legacy table sweep does (sets it to currentGcMarkValue).
// New objects also live on the thread's OWNED current page, which the
// concurrent sweep never touches (only retired pages are swept) -- mirroring
// how legacy new objects sit in pendingHeapAllocations until a paused mark.
//
// HARD POINT #2 (sweep vs mutator alloc on same page): the sweep only ever
// processes pages it took off the SWEEP stack (retired, owner==0). The owning
// thread's current page is never on that stack, so its free-list / bump cursor
// are never touched by the sweep. No data race on a page's free-list/cursor.
//
// HARD POINT #3 (no page-table lookup race): there is NO page table. Address ->
// page is never needed on a hot path: gcMarkObject uses the header (no lookup),
// the sweep walks pages it already holds, and free() of a page slot never
// happens (slots are recycled into the page free-list, identified by the
// __heapPosition==-3 sentinel). The only cross-thread page structures are the
// pools/stack (bibopMutex) and the append-only all-pages registry used by the
// overflow rescan (atomic head, release/acquire) -- both lock/atomic safe.
//
// OVERFLOW RESCAN (invariant #3): if the mark worklist overflows, a marked
// object whose mark-function has not yet run must be re-discovered. Legacy
// rescans allObjectsInHeap; we additionally rescan every page slot via the
// all-pages registry. Concurrent reads are race-free because (a) page bump
// cursors are published with release / read with acquire, and (b) a slot's
// header fields are only dereferenced when its (atomically read) mark equals
// the current cycle -- which is impossible for a slot a mutator is mid-
// initializing (its mark transitions oldDead/FREE -> -1, never through cur).
// =========================================================================

#include <stdatomic.h>

#ifndef CN1_BIBOP_PAGE_SIZE
#define CN1_BIBOP_PAGE_SIZE (64*1024)
#endif
#ifndef CN1_BIBOP_MAX_OBJECT
#define CN1_BIBOP_MAX_OBJECT 512
#endif
// Bytes bump/free-list-allocated through BiBOP since the last GC that force a
// collection so RSS stays bounded even for an all-small-object workload (these
// objects bypass the legacy per-thread heapAllocationSize GC trigger).
#ifndef CN1_BIBOP_GC_TRIGGER_BYTES
#define CN1_BIBOP_GC_TRIGGER_BYTES (24*1024*1024)
#endif
// Header mark sentinel for a slot sitting on a page free-list (distinct from
// -1 "fresh", and from any real epoch >= 1). The free-list link is stored in
// the slot's first pointer word (the __codenameOneParentClsReference slot),
// which a free slot does not otherwise use.
#define CN1_BIBOP_FREE_MARK (-7)
#define CN1_BIBOP_HEAP_POS   (-3)

// Size classes (slot sizes, 16-aligned). size <= CN1_BIBOP_MAX_OBJECT maps to
// the smallest class >= size; everything else takes the legacy path.
static const int cn1BibopClassSize[] = {
    32, 48, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, 448, 512
};
#define CN1_BIBOP_NUM_CLASSES ((int)(sizeof(cn1BibopClassSize)/sizeof(int)))
static signed char cn1BibopSizeToClass[CN1_BIBOP_MAX_OBJECT + 1];

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
} CN1BibopPage;

static CN1BibopPage* _Atomic bibopAllPages = 0;   // registry head (atomic)
static CN1BibopPage* bibopFreePool = 0;           // bibopMutex
static CN1BibopPage* bibopPartialPool[CN1_BIBOP_NUM_CLASSES]; // bibopMutex
static CN1BibopPage* _Atomic bibopSweepStack = 0; // Treiber-ish (push CAS / swap)
static pthread_mutex_t bibopMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_once_t  bibopOnce  = PTHREAD_ONCE_INIT;
static _Atomic long bibopBytesSinceGc = 0;

// Per-thread current page per size class. Only ever touched by the owning
// thread (allocation) and by that same thread on death (collectThreadResources
// runs on the dying thread), so __thread is correct and the GC never needs to
// reach into it -- retired pages are handed to the GC via the global stack.
static __thread CN1BibopPage* bibopCurrent[CN1_BIBOP_NUM_CLASSES];

static void cn1BibopDoInit() {
    int ci = 0;
    for(int s = 0 ; s <= CN1_BIBOP_MAX_OBJECT ; s++) {
        while(ci < CN1_BIBOP_NUM_CLASSES && cn1BibopClassSize[ci] < s) {
            ci++;
        }
        cn1BibopSizeToClass[s] = (signed char)(ci < CN1_BIBOP_NUM_CLASSES ? ci : -1);
    }
    for(int i = 0 ; i < CN1_BIBOP_NUM_CLASSES ; i++) {
        bibopPartialPool[i] = 0;
    }
}

static void cn1BibopFormatPage(CN1BibopPage* p, int ci) {
    int slotSize = cn1BibopClassSize[ci];
    // slot 0 starts after the page header, rounded up to 16-byte alignment so
    // every slot is at least 16-aligned (matches/exceeds calloc's guarantee).
    int hdr = (int)((sizeof(CN1BibopPage) + 15) & ~((size_t)15));
    p->classIndex = ci;
    p->slotSize = slotSize;
    p->firstSlotOffset = hdr;
    p->slotCount = (CN1_BIBOP_PAGE_SIZE - hdr) / slotSize;
    atomic_store_explicit(&p->bumpIndex, 0, memory_order_relaxed);
    p->freeList = 0;
    p->freeCount = 0;
    p->owned = JAVA_FALSE;
}

static CN1BibopPage* cn1BibopNewPage(int ci) {
    void* mem = 0;
    if(posix_memalign(&mem, CN1_BIBOP_PAGE_SIZE, CN1_BIBOP_PAGE_SIZE) != 0 || mem == 0) {
        return 0;
    }
    CN1BibopPage* p = (CN1BibopPage*)mem;
    cn1BibopFormatPage(p, ci);
    // Publish into the append-only registry: set nextAll (release) BEFORE the
    // head CAS so a concurrent rescan that reads the new head (acquire) sees a
    // fully-linked node.
    CN1BibopPage* head = atomic_load_explicit(&bibopAllPages, memory_order_relaxed);
    do {
        atomic_store_explicit(&p->nextAll, head, memory_order_relaxed);
    } while(!atomic_compare_exchange_weak_explicit(&bibopAllPages, &head, p,
                memory_order_release, memory_order_relaxed));
    return p;
}

static inline JAVA_OBJECT cn1BibopSlot(CN1BibopPage* p, int i) {
    return (JAVA_OBJECT)((char*)p + p->firstSlotOffset + (long)i * p->slotSize);
}

// Trigger a full GC if BiBOP allocation volume since the last collection has
// crossed the threshold (these objects don't feed the legacy heapAllocationSize
// trigger). Mirrors codenameOneGcMalloc's simple self-triggering branch.
static void cn1BibopMaybeGc(CODENAME_ONE_THREAD_STATE) {
    if(gcCurrentlyRunning || constantPoolObjects == 0 || threadStateData->nativeAllocationMode) {
        return;
    }
    if(atomic_load_explicit(&bibopBytesSinceGc, memory_order_relaxed) > CN1_BIBOP_GC_TRIGGER_BYTES) {
        threadStateData->nativeAllocationMode = JAVA_TRUE;
        java_lang_System_gc__(threadStateData);
        threadStateData->nativeAllocationMode = JAVA_FALSE;
    }
}

// Retire the thread's current page for class ci (if any) onto the global SWEEP
// stack and adopt a PARTIAL (preferred) or FREE page, formatting a fresh one
// only as a last resort. Runs on the owning thread.
static CN1BibopPage* cn1BibopAcquirePage(int ci) {
    pthread_mutex_lock(&bibopMutex);
    CN1BibopPage* old = bibopCurrent[ci];
    if(old != 0) {
        old->owned = JAVA_FALSE;
        // push onto the SWEEP stack (single producer here holds bibopMutex, but
        // the sweep swaps the head atomically, so use an atomic CAS push).
        CN1BibopPage* sh = atomic_load_explicit(&bibopSweepStack, memory_order_relaxed);
        do {
            old->nextPool = sh;
        } while(!atomic_compare_exchange_weak_explicit(&bibopSweepStack, &sh, old,
                    memory_order_release, memory_order_relaxed));
        bibopCurrent[ci] = 0;
    }
    CN1BibopPage* np = bibopPartialPool[ci];
    if(np != 0) {
        bibopPartialPool[ci] = np->nextPool;
    } else if(bibopFreePool != 0) {
        np = bibopFreePool;
        bibopFreePool = np->nextPool;
        cn1BibopFormatPage(np, ci);
    }
    pthread_mutex_unlock(&bibopMutex);
    if(np == 0) {
        np = cn1BibopNewPage(ci);
        if(np == 0) {
            return 0;
        }
    }
    np->owned = JAVA_TRUE;
    np->nextPool = 0;
    bibopCurrent[ci] = np;
    return np;
}

// Initialize a freshly-claimed slot's header EXACTLY like codenameOneGcMalloc,
// publishing the mark field LAST with an atomic release store so a concurrent
// overflow-rescan never observes a half-initialized object as live (its mark
// goes oldDead/FREE -> -1, never through the current epoch). Only the object
// body (after the fixed header) is zeroed, never the mark word, so there is no
// plain-write-vs-atomic-read race on the mark.
static inline void cn1BibopInitSlot(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o, int size, struct clazz* parent) {
    int hdr = (int)sizeof(struct JavaObjectPrototype);
    if(size > hdr) {
        memset((char*)o + hdr, 0, size - hdr);
    }
    o->__codenameOneParentClsReference = parent;
    o->__codenameOneReferenceCount = 1;
    o->__codenameOneThreadData = 0;
    o->__ownerThread = threadStateData;
    o->__heapPosition = CN1_BIBOP_HEAP_POS;
#ifdef DEBUG_GC_ALLOCATIONS
    o->className = threadStateData->callStackClass[threadStateData->callStackOffset - 1];
    o->line = threadStateData->callStackLine[threadStateData->callStackOffset - 1];
#endif
    __atomic_store_n(&o->__codenameOneGcMark, -1, __ATOMIC_RELEASE);
}

// Allocate a small non-array object from the per-thread page for its size class.
// Returns 0 only if pages cannot be obtained (caller falls back to the heap).
static JAVA_OBJECT cn1BibopAlloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent) {
    pthread_once(&bibopOnce, cn1BibopDoInit);
    int ci = cn1BibopSizeToClass[size];
    if(ci < 0) {
        return 0;
    }
    CN1BibopPage* p = bibopCurrent[ci];
    JAVA_OBJECT o = 0;
    for(;;) {
        if(p != 0) {
            if(p->freeList != 0) {
                o = (JAVA_OBJECT)p->freeList;
                p->freeList = *(void**)o;
                p->freeCount--;
                break;
            }
            int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
            if(bi < p->slotCount) {
                o = cn1BibopSlot(p, bi);
                cn1BibopInitSlot(threadStateData, o, size, parent);
                // publish the new cursor with release AFTER the slot (incl. its
                // mark) is fully initialized.
                atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_release);
                atomic_fetch_add_explicit(&bibopBytesSinceGc, p->slotSize, memory_order_relaxed);
                return o;
            }
        }
        // Need a fresh/partial page. This is the rare slow path (~once per page).
        cn1BibopMaybeGc(threadStateData);
        p = cn1BibopAcquirePage(ci);
        if(p == 0) {
            return 0; // out of pages -> legacy heap path
        }
        // loop: allocate from the freshly-acquired page (free-list or bump).
    }
    // free-list slot path
    cn1BibopInitSlot(threadStateData, o, size, parent);
    atomic_fetch_add_explicit(&bibopBytesSinceGc, p->slotSize, memory_order_relaxed);
    return o;
}

// Run finalizer + free monitor for a dead page slot (does NOT free() the slot;
// the slot is recycled into the page free-list by the caller). Mirrors
// freeAndFinalize / codenameOneGcFree minus the free().
static void cn1BibopReclaimSlot(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    finalizerFunctionPointer ptr = (finalizerFunctionPointer)o->__codenameOneParentClsReference->finalizerFunction;
    if(ptr != 0) {
        ptr(threadStateData, o);
    }
    if(o->__codenameOneThreadData) {
        free(o->__codenameOneThreadData);
        o->__codenameOneThreadData = 0;
    }
}

// Sweep all retired pages. Runs on the GC thread AFTER mark completes; the
// pages it processes are off the SWEEP stack (owner==0), so no mutator is
// allocating into them and no marking is in flight -> plain header access.
static void cn1BibopSweep(CODENAME_ONE_THREAD_STATE) {
    CN1BibopPage* list = atomic_exchange_explicit(&bibopSweepStack, (CN1BibopPage*)0, memory_order_acquire);
    while(list != 0) {
        CN1BibopPage* page = list;
        list = page->nextPool;
        int n = atomic_load_explicit(&page->bumpIndex, memory_order_relaxed);
        void* fl = 0;
        int freeCount = 0;
        int liveCount = 0;
        for(int i = 0 ; i < n ; i++) {
            JAVA_OBJECT o = cn1BibopSlot(page, i);
            int m = o->__codenameOneGcMark;
            if(m == CN1_BIBOP_FREE_MARK) {
                *(void**)o = fl; fl = o; freeCount++;
            } else if(m == -1) {
                // fresh, never marked -> one cycle of grace (legacy parity)
                o->__codenameOneGcMark = currentGcMarkValue;
                liveCount++;
            } else if(m < currentGcMarkValue - 1) {
                cn1BibopReclaimSlot(threadStateData, o);
                o->__codenameOneGcMark = CN1_BIBOP_FREE_MARK;
                *(void**)o = fl; fl = o; freeCount++;
            } else {
                liveCount++;
            }
        }
        page->freeList = fl;
        page->freeCount = freeCount;
        pthread_mutex_lock(&bibopMutex);
        if(liveCount == 0) {
            page->nextPool = bibopFreePool;
            bibopFreePool = page;
        } else {
            page->nextPool = bibopPartialPool[page->classIndex];
            bibopPartialPool[page->classIndex] = page;
        }
        pthread_mutex_unlock(&bibopMutex);
    }
    atomic_store_explicit(&bibopBytesSinceGc, 0, memory_order_relaxed);
}

// (The overflow-rescan helpers cn1BibopRescanStart / cn1BibopRescanStep live
// further down, next to gcMarkDrain, because they use the mark worklist.)

// Called on the dying thread (collectThreadResources): retire all of its
// current pages so their slots become collectable.
void cn1BibopRetireThreadPages() {
    pthread_once(&bibopOnce, cn1BibopDoInit);
    for(int ci = 0 ; ci < CN1_BIBOP_NUM_CLASSES ; ci++) {
        CN1BibopPage* old = bibopCurrent[ci];
        if(old != 0) {
            old->owned = JAVA_FALSE;
            CN1BibopPage* sh = atomic_load_explicit(&bibopSweepStack, memory_order_relaxed);
            do {
                old->nextPool = sh;
            } while(!atomic_compare_exchange_weak_explicit(&bibopSweepStack, &sh, old,
                        memory_order_release, memory_order_relaxed));
            bibopCurrent[ci] = 0;
        }
    }
}
#endif /* CN1_DISABLE_BIBOP */

JAVA_OBJECT codenameOneGcMalloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent) {
    if(isAppSuspended) {
        mallocWhileSuspended += size;
        if(mallocWhileSuspended > 100000) {
            java_lang_System_startGCThread__(threadStateData);
            isAppSuspended = NO;
        }
    }
    allocationsSinceLastGC += size;
    totalAllocations += size;
#ifdef CN1_GC_INSTRUMENT
    extern long long cn1_instr_allocCount; cn1_instr_allocCount++;
#endif
#ifdef CN1_NURSERY
    // Small objects go to the thread-local young generation and bypass the global
    // heap table entirely. Returns 0 (arena exhausted) -> fall through to the heap.
    if(size <= CN1_NURSERY_MAX_OBJECT && constantPoolObjects != 0 && !threadStateData->nativeAllocationMode) {
        JAVA_OBJECT nurseryObj = cn1NurseryAlloc(threadStateData, size, parent);
        if(nurseryObj != JAVA_NULL) {
            return nurseryObj;
        }
    }
#endif
#if !defined(CN1_DISABLE_BIBOP) && !defined(DEBUG_GC_OBJECTS_IN_HEAP)
    // Small NON-ARRAY objects: serve from the per-thread BiBOP page heap, which
    // skips placeObjectInHeapCollection / allObjectsInHeap entirely. Arrays and
    // objects larger than the biggest size class fall through to the legacy
    // calloc + table-registration path below. 0 => pages unavailable, fall back.
    if(parent != 0 && !parent->isArray && size <= CN1_BIBOP_MAX_OBJECT &&
       constantPoolObjects != 0 && !threadStateData->nativeAllocationMode) {
        JAVA_OBJECT bibopObj = cn1BibopAlloc(threadStateData, size, parent);
        if(bibopObj != JAVA_NULL) {
            return bibopObj;
        }
    }
#endif
    if(lowMemoryMode && !threadStateData->nativeAllocationMode) {
        threadStateData->threadActive = JAVA_FALSE;
        usleep((JAVA_INT)(1000));
        while(threadStateData->threadBlockedByGC) {
            usleep((JAVA_INT)(1000));
        }
        threadStateData->threadActive = JAVA_TRUE;
    }
#ifdef DEBUG_GC_OBJECTS_IN_HEAP
    totalAllocatedHeap += size;
    int* ptr = (int*)malloc(size + sizeof(int));
    *ptr = size;
    ptr++;
    JAVA_OBJECT o = (JAVA_OBJECT)ptr;
    JAVA_BOOLEAN needsZeroing = JAVA_TRUE;
#else
    // calloc instead of malloc+memset: the allocator returns zero-filled memory,
    // and for large/array allocations the kernel can hand back lazily-zeroed
    // (copy-on-write zero) pages, avoiding an eager memset pass over memory that
    // is about to be written anyway. Object-header fields are set explicitly below.
    JAVA_OBJECT o = (JAVA_OBJECT)calloc(1, size);
    JAVA_BOOLEAN needsZeroing = JAVA_FALSE;
#endif
    if(o == NULL) {
        // malloc failed! We need to free up RAM FAST!
        invokedGC = YES;
        threadStateData->threadActive = JAVA_FALSE;
        java_lang_System_gc__(getThreadLocalData());
        while(threadStateData->threadBlockedByGC) {
            usleep((JAVA_INT)(1000));
        }
        invokedGC = NO;
        threadStateData->threadActive = JAVA_TRUE;
        return codenameOneGcMalloc(threadStateData, size, parent);
    }
    if(needsZeroing) {
        memset(o, 0, size);
    }
    o->__codenameOneParentClsReference = parent;
    o->__codenameOneGcMark = -1;
    o->__ownerThread = threadStateData;
    o->__heapPosition = -1;
    o->__codenameOneReferenceCount = 1;
#ifdef DEBUG_GC_ALLOCATIONS
    o->className = threadStateData->callStackClass[threadStateData->callStackOffset - 1];
    o->line = threadStateData->callStackLine[threadStateData->callStackOffset - 1];
#endif
    
    if(threadStateData->heapAllocationSize == threadStateData->threadHeapTotalSize) {
        if(threadStateData->threadBlockedByGC && !threadStateData->nativeAllocationMode) {
            threadStateData->threadActive = JAVA_FALSE;
            while(threadStateData->threadBlockedByGC) {
                usleep(1000);
            }
            threadStateData->threadActive = JAVA_TRUE;
        }
        long maxHeapSize = CN1_MAX_HEAP_SIZE;
        if (isEdt(threadStateData->threadId) && !lowMemoryMode) {
            maxHeapSize = CN1_MAX_HEAP_SIZE_EDT;
        }
        
        
        
        if(threadStateData->heapAllocationSize > maxHeapSize && constantPoolObjects != 0 && !threadStateData->nativeAllocationMode) {
            threadStateData->threadActive=JAVA_FALSE;
            while(gcCurrentlyRunning) {
                usleep((JAVA_INT)(1000));
            }
            threadStateData->threadActive=JAVA_TRUE;
            
            if(threadStateData->heapAllocationSize > 0 ) {
                invokedGC = YES;
                threadStateData->nativeAllocationMode = JAVA_TRUE;
                java_lang_System_gc__(threadStateData);
                threadStateData->nativeAllocationMode = JAVA_FALSE;
                threadStateData->threadActive = JAVA_FALSE;
                while(threadStateData->threadBlockedByGC || threadStateData->heapAllocationSize > 0) {
                    if (get_static_java_lang_System_gcThreadInstance() == JAVA_NULL) {
                        // For some reason the gcThread is dead
                        threadStateData->nativeAllocationMode = JAVA_TRUE;
                        java_lang_System_gc__(threadStateData);
                        threadStateData->nativeAllocationMode = JAVA_FALSE;
                        threadStateData->threadActive = JAVA_FALSE;
                    }
                    usleep((JAVA_INT)(1000));
                }
                invokedGC = NO;
                threadStateData->threadActive = JAVA_TRUE;
            }
        } else {
            if(threadStateData->heapAllocationSize == threadStateData->threadHeapTotalSize) {
                
                // Let's trigger a GC here.
                if(!gcCurrentlyRunning && constantPoolObjects != 0 && !threadStateData->nativeAllocationMode) {
                    threadStateData->nativeAllocationMode = JAVA_TRUE;
                    java_lang_System_gc__(threadStateData);
                    threadStateData->nativeAllocationMode = JAVA_FALSE;
                }
                if (!threadStateData->lightweightThread) {
                    lockThreadHeapMutex();
                }
                void** tmp = malloc(threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
                memset(tmp, 0, threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
                memcpy(tmp, threadStateData->pendingHeapAllocations, threadStateData->threadHeapTotalSize * sizeof(void *));
                threadStateData->threadHeapTotalSize *= 2;
                free(threadStateData->pendingHeapAllocations);
                threadStateData->pendingHeapAllocations = tmp;
                if (!threadStateData->lightweightThread) {
                    unlockThreadHeapMutex();
                }
            }
        }
    }
    threadStateData->pendingHeapAllocations[threadStateData->heapAllocationSize] = o;
    threadStateData->heapAllocationSize++;
    return o;
}

void codenameOneGcFree(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
#ifndef CN1_DISABLE_BIBOP
    // A BiBOP page slot must never be free()'d -- it is reclaimed in place into
    // its page free-list by cn1BibopSweep. This is a defensive guard; no legacy
    // path should reach here with a page slot (they are not in allObjectsInHeap).
    if(obj->__heapPosition == CN1_BIBOP_HEAP_POS) {
        return;
    }
#endif
    if(obj->__codenameOneThreadData) {
        free(obj->__codenameOneThreadData);
        obj->__codenameOneThreadData = 0;
    }
#ifdef CN1_NURSERY
    // A promoted nursery object lives inside an arena block; never free() it -- just
    // drop the block's live count and recycle the whole block once it hits zero.
    if(cn1InNursery(obj)) {
        extern void cn1NurseryObjectFreed(JAVA_OBJECT o);
        cn1NurseryObjectFreed(obj);
        return;
    }
#endif
#ifdef DEBUG_GC_OBJECTS_IN_HEAP
    int* ptr = (int*)obj;
    ptr--;
    totalAllocatedHeap -= *ptr;
    free(ptr);
#else
    free(obj);
#endif
}

typedef void (*gcMarkFunctionPointer)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force);

//JAVA_OBJECT* recursionBlocker = 0;
//int recursionBlockerPosition = 0;
int recursionKey = 1;

// Iterative mark using an explicit worklist. The previous implementation recursed
// through reference fields, building one C stack frame per Java reference traversed.
// iOS gives secondary pthreads a ~512KB stack, so a chain of a few thousand references
// (linked list, parse tree, deeply nested container) would SIGBUS the GC thread.
// Issue #3136.
//
// gcMarkObject now sets the mark bit and pushes onto a fixed worklist. gcMarkDrain
// pops entries and invokes their per-class mark function, which calls gcMarkObject on
// each reference field -- push, not recurse. If the worklist fills, the offending
// object is still marked (so sweep preserves it) but its field scan is deferred to
// the heap-rescan pass: walk the live-object table, re-invoke mark functions on
// already-marked objects to pick up children that were skipped on overflow. Idempotent
// because already-marked children are no-ops in gcMarkObject.
//
// CN1_GC_MARK_WORKLIST_SIZE is overridable at compile time (e.g. via -D in the Xcode
// build settings or the maven plugin). 65536 entries is ~1MB on 64-bit. Sized so the
// constant pool alone fits comfortably (HelloCodenameOne has ~15K entries, real apps
// can have more). Smaller sizes still work via the heap-rescan slow path, but the
// rescan adds non-trivial cost and the path is harder to test, so the default errs
// on the side of avoiding overflow for any normal app.
#ifndef CN1_GC_MARK_WORKLIST_SIZE
#define CN1_GC_MARK_WORKLIST_SIZE 65536
#endif

struct gcMarkWorklistEntry {
    JAVA_OBJECT obj;
    JAVA_BOOLEAN force;
};

static struct gcMarkWorklistEntry gcMarkWorklist[CN1_GC_MARK_WORKLIST_SIZE];
static int gcMarkWorklistTop = 0;
static JAVA_BOOLEAN gcMarkWorklistOverflow = JAVA_FALSE;
// Set whenever gcMarkObject transitions an object from unmarked to marked. Used by
// the overflow-rescan loop to detect a fixed point: if a rescan+drain pass marks
// nothing new, the reachable set is fully closed under "marked" and we're done --
// otherwise we'd spin forever re-pushing the same marked-and-already-scanned
// objects when the marked set is larger than the worklist. Only touched on the
// serial path (gcMarkLocalBuf == 0); the parallel workers never run the rescan, and
// writing it from many workers would be a benign-value-but-still-reported data race.
static JAVA_BOOLEAN gcMarkFoundUnmarkedChildInPass = JAVA_FALSE;

// ===================== Parallel mark drain =====================
//
// The transitive drain (popping objects and running their per-class mark functions,
// which push reference fields back onto the worklist) is the dominant cost of a mark
// cycle, and it is embarrassingly parallel: marking is already type-specialized and
// the only shared mutable state is (a) each object's mark bit and (b) the worklist.
//
// We parallelize ONLY the drain, leaving codenameOneGCMark's per-thread park / root
// snapshot / aggressive-allocator handling exactly as it was. The roots are pushed
// onto the worklist serially while the mutator thread is paused (snapshot-at-the-
// beginning, invariant #1), then gcMarkDrainParallel marks the whole reachable set
// before the thread is released -- the workers just do it faster.
//
// Correctness rests on three things:
//  * The mark bit is claimed with an atomic compare-and-swap (gcMarkObject), so for
//    any object exactly one worker wins the unmarked->marked transition and exactly
//    one worker pushes it. No double-push, no double-scan, no torn mark bit.
//  * The shared worklist is guarded by gcMarkWorklistMutex. Workers pop a BATCH under
//    the lock and buffer the children they produce in a thread-local buffer, flushing
//    in batches, so the lock is taken ~once per CN1_GC_MARK_BATCH objects.
//  * Termination = worklist empty AND every worker idle, tracked by gcMarkActiveWorkers
//    under the lock. A worker stays "active" from the moment it pops work until, after
//    flushing everything it produced, it observes the global worklist empty; only then
//    does it go idle. So the count hits zero only when no in-hand or global work
//    remains anywhere -- a worker that produces new work re-wakes the idle ones.
//
// The bounded explicit worklist (invariant #2, no recursion) and the overflow->heap-
// rescan fixed point (invariant #3) are preserved: on overflow the parallel region
// still drains to empty (marked-but-unscanned objects are dropped from the worklist
// but stay marked) and then gcMarkDrainParallel finishes with one serial gcMarkDrain,
// which runs the rescan fixed point exactly as before.
//
// CN1_GC_MARK_THREADS overrides the worker count at compile time (-D...); when it is
// not set the count is min(4, online-cpus - 1) computed at runtime. A count of 1 (the
// historical behavior) takes the serial path with no pool, no atomics and no locks.
#ifndef CN1_GC_MARK_BATCH
#define CN1_GC_MARK_BATCH 64
#endif
#ifndef CN1_GC_MARK_LOCAL_CAP
#define CN1_GC_MARK_LOCAL_CAP 256
#endif

// Per-worker production buffer. While a thread is acting as a parallel mark worker its
// gcMarkLocalBuf points here (on that worker's stack); gcMarkWorklistPush appends to it
// and flushes to the shared worklist in batches. When NULL the thread is on the serial
// path and pushes straight to the shared worklist with no locking (single-threaded by
// construction: root snapshot, the serial drain, the nursery promote drain). Being a
// thread-local pointer it also doubles as the per-thread "am I a parallel worker?" flag
// that gcMarkObject uses to choose the atomic mark-claim path.
struct gcMarkLocalBuffer {
    int count;
    struct gcMarkWorklistEntry entries[CN1_GC_MARK_LOCAL_CAP];
};
static __thread struct gcMarkLocalBuffer* gcMarkLocalBuf = 0;

// Worklist / termination state (guarded by gcMarkWorklistMutex during a parallel drain)
static pthread_mutex_t gcMarkWorklistMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  gcMarkWorklistCond  = PTHREAD_COND_INITIALIZER;
static int gcMarkActiveWorkers = 0;
static JAVA_BOOLEAN gcMarkDone = JAVA_FALSE;
static struct ThreadLocalData* gcMarkThreadState = 0; // 'd' passed through to mark functions

// Pool dispatch / completion handshake (guarded by gcMarkCtlMutex)
static pthread_mutex_t gcMarkCtlMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  gcMarkCtlCond  = PTHREAD_COND_INITIALIZER;
static unsigned long gcMarkGeneration = 0;     // bumped to dispatch a drain
static int gcMarkWorkersFinished = 0;          // helpers that completed the current generation
static int gcMarkPoolSize = 0;                 // number of spawned helper threads (= count - 1)
static int gcMarkThreadCount = 0;              // total markers incl. the GC thread (resolved once)
static JAVA_BOOLEAN gcMarkPoolReady = JAVA_FALSE;

// Append a worker's local production buffer to the shared worklist. On overflow the
// surplus entries are dropped -- they are already MARKED (their mark bit was claimed
// before the push) so dropping only defers their field scan to the serial rescan, which
// is exactly the existing overflow contract.
static void gcMarkFlushLocal(struct gcMarkLocalBuffer* lb) {
    if(lb->count == 0) {
        return;
    }
    pthread_mutex_lock(&gcMarkWorklistMutex);
    int appended = 0;
    for(int i = 0 ; i < lb->count ; i++) {
        if(gcMarkWorklistTop >= CN1_GC_MARK_WORKLIST_SIZE) {
            gcMarkWorklistOverflow = JAVA_TRUE;
            break;
        }
        gcMarkWorklist[gcMarkWorklistTop] = lb->entries[i];
        gcMarkWorklistTop++;
        appended++;
    }
    if(appended > 0) {
        pthread_cond_broadcast(&gcMarkWorklistCond);
    }
    pthread_mutex_unlock(&gcMarkWorklistMutex);
    lb->count = 0;
}

static inline void gcMarkWorklistPush(JAVA_OBJECT obj, JAVA_BOOLEAN force) {
    struct gcMarkLocalBuffer* lb = gcMarkLocalBuf;
    if(lb != 0) {
        // Parallel worker: buffer locally, flush in batches (see gcMarkFlushLocal).
        if(lb->count >= CN1_GC_MARK_LOCAL_CAP) {
            gcMarkFlushLocal(lb);
        }
        lb->entries[lb->count].obj = obj;
        lb->entries[lb->count].force = force;
        lb->count++;
        return;
    }
    // Serial path: identical to the original single-threaded push.
    if(gcMarkWorklistTop >= CN1_GC_MARK_WORKLIST_SIZE) {
        gcMarkWorklistOverflow = JAVA_TRUE;
        return;
    }
    gcMarkWorklist[gcMarkWorklistTop].obj = obj;
    gcMarkWorklist[gcMarkWorklistTop].force = force;
    gcMarkWorklistTop++;
}

#ifdef CN1_NURSERY
extern void cn1NurseryPromote(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o);
#endif

#if CN1_TAGGED_ACTIVE
// Object-shaped proxy whose header is Integer's class; see CN1_CLASS_OF in cn1_globals.h.
// Lets a tagged Integer resolve to Integer without dereferencing the tagged pointer.
struct JavaObjectPrototype cn1TaggedProxy = { .__codenameOneParentClsReference = &class__java_lang_Integer };
#endif

void gcMarkObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {
    if(obj == JAVA_NULL || CN1_IS_TAGGED(obj) || obj->__codenameOneParentClsReference == 0 || obj->__codenameOneParentClsReference == (&class__java_lang_Class)) {
        return;
    }
#ifdef CN1_NURSERY
    // During THIS thread's minor collection we reuse the per-class mark functions to
    // walk the object graph, but PROMOTE nursery objects instead of marking (and stop
    // at heap objects -- the write barrier guarantees they don't point into the
    // nursery). The flag is per-thread so the concurrent GC thread is unaffected.
    if(threadStateData->nurseryPromoting) {
        if(cn1InNursery(obj) && obj->__heapPosition == -1) {
            cn1NurseryPromote(threadStateData, obj);
        }
        return;
    }
#endif

    int markVal = currentGcMarkValue;

    // Parallel worker path: claim the object's mark bit with an atomic CAS so exactly
    // one worker transitions it unmarked->marked and pushes it (no double-scan, no torn
    // mark bit). 'force' is never set on the parallel path -- the per-thread roots are
    // pushed with force==FALSE and mark functions propagate that -- so the force/
    // recursionKey re-scan (which writes __codenameOneReferenceCount and is rare) stays
    // entirely on the serial path below, keeping the parallel region race-free.
    if(gcMarkLocalBuf != 0) {
        int old = __atomic_load_n(&obj->__codenameOneGcMark, __ATOMIC_RELAXED);
        if(old == markVal) {
            return; // already marked this cycle
        }
        if(__sync_bool_compare_and_swap(&obj->__codenameOneGcMark, old, markVal)) {
            if(obj->__codenameOneParentClsReference->markFunction != 0) {
                gcMarkWorklistPush(obj, force);
            }
        }
        // else: another worker won the claim and is responsible for pushing it.
        return;
    }

    // Serial path: byte-for-byte the original behavior (single writer, plain store).
    // if this is a Class object or already marked this should be ignored
    if(obj->__codenameOneGcMark == markVal) {
        if(force) {
            if(obj->__codenameOneReferenceCount == recursionKey) {
                return;
            }
            obj->__codenameOneReferenceCount = recursionKey;
            if(obj->__codenameOneParentClsReference->markFunction != 0) {
                gcMarkWorklistPush(obj, force);
            }
        }
        return;
    }
    obj->__codenameOneGcMark = markVal;
    gcMarkFoundUnmarkedChildInPass = JAVA_TRUE;
    if(obj->__codenameOneParentClsReference->markFunction != 0) {
        gcMarkWorklistPush(obj, force);
    }
}

#ifdef CN1_NURSERY
// ===================== Thread-local young generation (nursery) =====================
char* cn1NurseryArenaStart = 0;
char* cn1NurseryArenaEnd = 0;
static int cn1NurseryBlockCount = 0;
// young: the block is in some thread's young set (being bump-allocated). A young block
// is reclaimed ONLY by that thread's minor collection. cn1NurseryObjectFreed (sweep
// thread) must never push a young block to the free stack, or it races the minor
// collection's release and double-pushes -> free-stack overflow -> SIGABRT.
typedef struct { int liveCount; JAVA_BOOLEAN tenured; JAVA_BOOLEAN young; } CN1NurseryBlockMeta;
static CN1NurseryBlockMeta* cn1NurseryBlocks = 0;
static int* cn1NurseryFreeStack = 0;
static int cn1NurseryFreeTop = 0;
static pthread_mutex_t cn1NurseryMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_once_t cn1NurseryOnce = PTHREAD_ONCE_INIT;

static void cn1NurseryDoInit() {
    cn1NurseryArenaStart = (char*)malloc(CN1_NURSERY_ARENA_SIZE);
    cn1NurseryArenaEnd = cn1NurseryArenaStart + CN1_NURSERY_ARENA_SIZE;
    cn1NurseryBlockCount = CN1_NURSERY_ARENA_SIZE / CN1_NURSERY_BLOCK_SIZE;
    cn1NurseryBlocks = (CN1NurseryBlockMeta*)calloc(cn1NurseryBlockCount, sizeof(CN1NurseryBlockMeta));
    cn1NurseryFreeStack = (int*)malloc(sizeof(int) * cn1NurseryBlockCount);
    for(int i = 0 ; i < cn1NurseryBlockCount ; i++) {
        cn1NurseryFreeStack[i] = cn1NurseryBlockCount - 1 - i;
    }
    cn1NurseryFreeTop = cn1NurseryBlockCount;
}

static inline int cn1NurseryBlockIndex(void* p) {
    return (int)(((char*)p - cn1NurseryArenaStart) / CN1_NURSERY_BLOCK_SIZE);
}

static int cn1NurseryGrabBlock() {
    int idx = -1;
    pthread_mutex_lock(&cn1NurseryMutex);
    if(cn1NurseryFreeTop > 0) {
        idx = cn1NurseryFreeStack[--cn1NurseryFreeTop];
        // Reset under the mutex so the sweep thread can't observe a half-initialized
        // block (it reads liveCount/tenured/young in cn1NurseryObjectFreed).
        cn1NurseryBlocks[idx].liveCount = 0;
        cn1NurseryBlocks[idx].tenured = JAVA_FALSE;
        cn1NurseryBlocks[idx].young = JAVA_TRUE;
    }
    pthread_mutex_unlock(&cn1NurseryMutex);
    return idx;
}

// Called by the global sweep when it frees a promoted (tenured-block) object. The
// object stays in place; we just drop the block's live count and recycle the whole
// block once every survivor in it has died -- but ONLY if the block has been retired
// from its thread's young set. A still-young block is reclaimed by the minor
// collection instead; freeing it here too would double-push and overflow the stack.
void cn1NurseryObjectFreed(JAVA_OBJECT o) {
    int idx = cn1NurseryBlockIndex(o);
    pthread_mutex_lock(&cn1NurseryMutex);
    int lc = --cn1NurseryBlocks[idx].liveCount;
    if(lc <= 0 && cn1NurseryBlocks[idx].tenured && !cn1NurseryBlocks[idx].young) {
        cn1NurseryBlocks[idx].tenured = JAVA_FALSE;
        cn1NurseryFreeStack[cn1NurseryFreeTop++] = idx;
    }
    pthread_mutex_unlock(&cn1NurseryMutex);
}

static void cn1PromotePush(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    if(threadStateData->nurseryPromoteTop >= threadStateData->nurseryPromoteCap) {
        threadStateData->nurseryPromoteCap = threadStateData->nurseryPromoteCap ? threadStateData->nurseryPromoteCap * 2 : 8192;
        threadStateData->nurseryPromoteWorklist = (JAVA_OBJECT*)realloc(threadStateData->nurseryPromoteWorklist, sizeof(JAVA_OBJECT) * threadStateData->nurseryPromoteCap);
    }
    threadStateData->nurseryPromoteWorklist[threadStateData->nurseryPromoteTop++] = o;
}

// Add an object to this thread's pending-allocation buffer, exactly like a normal
// heap allocation. The mark phase migrates pending -> allObjectsInHeap while the
// thread is paused, so registration never races the concurrent sweep/mark.
static void cn1AddPending(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    if(threadStateData->heapAllocationSize >= threadStateData->threadHeapTotalSize) {
        if (!threadStateData->lightweightThread) lockThreadHeapMutex();
        void** tmp = malloc(threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
        memset(tmp, 0, threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
        memcpy(tmp, threadStateData->pendingHeapAllocations, threadStateData->threadHeapTotalSize * sizeof(void *));
        threadStateData->threadHeapTotalSize *= 2;
        free(threadStateData->pendingHeapAllocations);
        threadStateData->pendingHeapAllocations = tmp;
        if (!threadStateData->lightweightThread) unlockThreadHeapMutex();
    }
    threadStateData->pendingHeapAllocations[threadStateData->heapAllocationSize++] = o;
}

// Promote one nursery object IN PLACE (address unchanged): tenure its block and hand
// it to the normal pending-allocation path so the next paused mark registers it in
// allObjectsInHeap. __heapPosition: -1 = un-promoted nursery, -2 = promoted/pending,
// >=0 = migrated into the global table.
void cn1NurseryPromote(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    pthread_mutex_lock(&cn1NurseryMutex);
    int idx = cn1NurseryBlockIndex(o);
    cn1NurseryBlocks[idx].tenured = JAVA_TRUE;
    cn1NurseryBlocks[idx].liveCount++;
    pthread_mutex_unlock(&cn1NurseryMutex);
    o->__heapPosition = -2;
    threadStateData->nurseryPromotedSinceMinor++;
    cn1AddPending(threadStateData, o);
    cn1PromotePush(threadStateData, o);
}

static void cn1PromoteDrain(CODENAME_ONE_THREAD_STATE) {
    while(threadStateData->nurseryPromoteTop > 0) {
        JAVA_OBJECT o = threadStateData->nurseryPromoteWorklist[--threadStateData->nurseryPromoteTop];
        gcMarkFunctionPointer fp = o->__codenameOneParentClsReference->markFunction;
        if(fp != 0) {
            fp(threadStateData, o, JAVA_FALSE);
        }
    }
}

void cn1NurseryMinorCollect(CODENAME_ONE_THREAD_STATE) {
    threadStateData->nurseryPromoting = JAVA_TRUE;
    threadStateData->nurseryPromoteTop = 0;
    int top = threadStateData->threadObjectStackOffset;
    struct elementStruct* stack = threadStateData->threadObjectStack;
    for(int i = 0 ; i < top ; i++) {
        if(stack[i].type == CN1_TYPE_OBJECT) {
            JAVA_OBJECT o = stack[i].data.o;
            if(o != JAVA_NULL && cn1InNursery(o) && o->__heapPosition == -1) {
                cn1NurseryPromote(threadStateData, o);
            }
        }
    }
    JAVA_OBJECT ct = threadStateData->currentThreadObject;
    if(ct != JAVA_NULL && cn1InNursery(ct) && ct->__heapPosition == -1) cn1NurseryPromote(threadStateData, ct);
    JAVA_OBJECT ex = threadStateData->exception;
    if(ex != JAVA_NULL && cn1InNursery(ex) && ex->__heapPosition == -1) cn1NurseryPromote(threadStateData, ex);
    // Static fields are also roots. If the write barrier holds (statics never point
    // into the nursery) this is cheap -- it just walks heap objects, which the
    // promotion hook ignores -- but it also catches any store path that bypassed the
    // barrier, so a still-live nursery object can never be left unpromoted (and then
    // wrongly reclaimed). markStatics calls gcMarkObject, which promotes in this mode.
    extern void markStatics(CODENAME_ONE_THREAD_STATE);
    markStatics(threadStateData);
    cn1PromoteDrain(threadStateData);
    threadStateData->nurseryPromoting = JAVA_FALSE;
    // Retire every young block from the young set (under the mutex, so the sweep thread
    // sees a consistent young flag). A block with no live promoted survivors (liveCount
    // <= 0: never tenured, or every survivor it held already died) is reclaimed now;
    // one that still has survivors stays tenured and is freed later by
    // cn1NurseryObjectFreed when its last survivor dies. Clearing `young` first hands
    // that responsibility cleanly to the sweep with no double-push window.
    pthread_mutex_lock(&cn1NurseryMutex);
    for(int i = 0 ; i < threadStateData->nurseryYoungCount ; i++) {
        int idx = threadStateData->nurseryYoungBlocks[i];
        cn1NurseryBlocks[idx].young = JAVA_FALSE;
#ifndef CN1_NURSERY_NO_RECLAIM
        if(cn1NurseryBlocks[idx].liveCount <= 0) {
            cn1NurseryBlocks[idx].tenured = JAVA_FALSE;
            cn1NurseryFreeStack[cn1NurseryFreeTop++] = idx;
        }
#endif
    }
    pthread_mutex_unlock(&cn1NurseryMutex);
    threadStateData->nurseryYoungCount = 0;
    threadStateData->nurseryCurrentBlock = -1;
    threadStateData->nurseryBump = 0;
    threadStateData->nurseryEnd = 0;
    threadStateData->nurseryBytesSinceMinor = 0;
    // Adaptive bypass decision. If most of what we allocated since the last minor
    // survived, the nursery (bump + write barrier + promote-to-pending) was strictly
    // more work than allocating into the heap directly would have been. Bypass it for
    // a while, then re-probe. A churny phase reclaims whole blocks here and keeps the
    // nursery on; an escaping phase trips this and stops paying the overhead.
    int allocated = threadStateData->nurseryAllocSinceMinor;
    int promoted = threadStateData->nurseryPromotedSinceMinor;
    if(allocated >= CN1_NURSERY_BYPASS_MIN_SAMPLE &&
       promoted * 100 >= allocated * CN1_NURSERY_BYPASS_SURVIVAL_PCT) {
        threadStateData->nurseryBypass = JAVA_TRUE;
        threadStateData->nurseryBypassCountdown = CN1_NURSERY_BYPASS_ALLOCS;
    }
#ifdef CN1_NURSERY_DEBUG
    fprintf(stderr, "[NURSERY] minor: alloc=%d promoted=%d survival=%d%% reprobe=%d -> bypass=%d\n",
            allocated, promoted, allocated ? (promoted*100/allocated) : 0,
            threadStateData->nurseryReprobing, threadStateData->nurseryBypass);
#endif
    threadStateData->nurseryReprobing = JAVA_FALSE;
    threadStateData->nurseryAllocSinceMinor = 0;
    threadStateData->nurseryPromotedSinceMinor = 0;
}

JAVA_OBJECT cn1NurseryAlloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent) {
    pthread_once(&cn1NurseryOnce, cn1NurseryDoInit);
    // GC safepoint. The concurrent GC pauses lightweight threads (threadBlockedByGC +
    // wait on threadActive) before scanning their stacks/nursery objects. The normal
    // allocation path yields here too (~line 1141); the nursery fast path must as
    // well, otherwise the GC either scans this thread's nursery while a minor
    // collection mutates it (corruption) or waits forever. A minor collection itself
    // keeps threadActive true throughout, so the GC never scans mid-collection.
    if(threadStateData->threadBlockedByGC && !threadStateData->nativeAllocationMode) {
        threadStateData->threadActive = JAVA_FALSE;
        while(threadStateData->threadBlockedByGC) {
            usleep(1000);
        }
        threadStateData->threadActive = JAVA_TRUE;
    }
    // Adaptive bypass: a recent minor collection saw high survival, so skip the
    // nursery and let the caller allocate into the global heap. Decrement toward a
    // re-probe; when it elapses, allocate in the nursery again to re-measure survival.
    if(threadStateData->nurseryBypass) {
        if(--threadStateData->nurseryBypassCountdown > 0) {
            return JAVA_NULL;
        }
        threadStateData->nurseryBypass = JAVA_FALSE;
        threadStateData->nurseryReprobing = JAVA_TRUE;
    }
    if(threadStateData->nurseryYoungBlocks == 0) {
        threadStateData->nurseryYoungCapacity = 256;
        threadStateData->nurseryYoungBlocks = (int*)malloc(sizeof(int) * threadStateData->nurseryYoungCapacity);
        threadStateData->nurseryYoungCount = 0;
        threadStateData->nurseryCurrentBlock = -1;
    }
    int asize = (size + 15) & ~15;
    if(threadStateData->nurseryBump == 0 || threadStateData->nurseryBump + asize > threadStateData->nurseryEnd) {
        long minorTrigger = threadStateData->nurseryReprobing ? CN1_NURSERY_REPROBE_BYTES : CN1_NURSERY_MINOR_TRIGGER;
        if(threadStateData->nurseryBytesSinceMinor >= minorTrigger) {
            cn1NurseryMinorCollect(threadStateData);
        }
        int idx = cn1NurseryGrabBlock();
        if(idx < 0) {
            return JAVA_NULL; // arena exhausted -> use the global heap
        }
        if(threadStateData->nurseryYoungCount >= threadStateData->nurseryYoungCapacity) {
            threadStateData->nurseryYoungCapacity *= 2;
            threadStateData->nurseryYoungBlocks = (int*)realloc(threadStateData->nurseryYoungBlocks, sizeof(int) * threadStateData->nurseryYoungCapacity);
        }
        threadStateData->nurseryYoungBlocks[threadStateData->nurseryYoungCount++] = idx;
        threadStateData->nurseryCurrentBlock = idx;
        threadStateData->nurseryBump = cn1NurseryArenaStart + (long)idx * CN1_NURSERY_BLOCK_SIZE;
        threadStateData->nurseryEnd = threadStateData->nurseryBump + CN1_NURSERY_BLOCK_SIZE;
    }
    JAVA_OBJECT o = (JAVA_OBJECT)threadStateData->nurseryBump;
    threadStateData->nurseryBump += asize;
    threadStateData->nurseryBytesSinceMinor += asize;
    threadStateData->nurseryAllocSinceMinor++;
    memset(o, 0, size);
    o->__codenameOneParentClsReference = parent;
    o->__codenameOneGcMark = -1;
    o->__ownerThread = threadStateData;
    o->__heapPosition = -1;
    o->__codenameOneReferenceCount = 1;
    return o;
}

// Write barrier: an object reference is being stored into a non-nursery location, so
// the value escapes the thread-local nursery and must be promoted to the global heap.
void cn1NurseryWriteBarrier(JAVA_OBJECT target, JAVA_OBJECT value) {
    if(value != JAVA_NULL && cn1InNursery(value) && value->__heapPosition == -1 && !cn1InNursery(target)) {
        struct ThreadLocalData* threadStateData = getThreadLocalData();
        // Re-entrancy guard: promotion walks markFunctions which can store refs and
        // re-enter the barrier; the outermost call owns the worklist drain.
        if(threadStateData->nurseryPromoting) {
            cn1NurseryPromote(threadStateData, value);
            return;
        }
        threadStateData->nurseryPromoting = JAVA_TRUE;
        threadStateData->nurseryPromoteTop = 0;
        cn1NurseryPromote(threadStateData, value);
        cn1PromoteDrain(threadStateData);
        threadStateData->nurseryPromoting = JAVA_FALSE;
    }
}
#endif

void gcMarkArrayObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {
    if(obj == JAVA_NULL) {
        return;
    }
#ifdef CN1_NURSERY
    // The minor collection reuses array mark functions to walk arrays for promotion;
    // there the array's mark bit is NOT claimed through gcMarkObject, so set it as the
    // pre-existing code did.
    if(threadStateData->nurseryPromoting) {
        obj->__codenameOneGcMark = currentGcMarkValue;
    }
#endif
    // In the concurrent GC drain (serial or parallel) this array's mark bit was already
    // claimed atomically by the gcMarkObject that enqueued it. We must NOT rewrite it
    // here: a redundant non-atomic store would race with other workers reading the bit
    // (and with the winning worker's CAS) under ThreadSanitizer.
    JAVA_ARRAY arr = (JAVA_ARRAY)obj;
    if(arr->length > 0) {
        JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)arr->data;
        for(int iter = 0 ; iter < arr->length ; iter++) {
            if(data[iter] != JAVA_NULL) {
                gcMarkObject(threadStateData, data[iter], force);
            }
        }
    }
}

#ifndef CN1_DISABLE_BIBOP
// ---- BiBOP overflow rescan (see the module header up top). Only engaged AFTER
// the mark worklist has overflowed, so the common (no-overflow) path pays
// nothing. Resumable cursor over the append-only all-pages registry. Only ever
// driven from the serial gcMarkDrain, so the static cursor is race-free. ----
static CN1BibopPage* bibopRescanPage = 0;
static int bibopRescanSlot = 0;
static void cn1BibopRescanStart() {
    bibopRescanPage = atomic_load_explicit(&bibopAllPages, memory_order_acquire);
    bibopRescanSlot = 0;
}
// Push every currently-marked page object whose class has a mark function,
// resuming where it left off when the worklist fills. Returns JAVA_TRUE once the
// whole registry has been scanned. A slot's header is dereferenced only when its
// (atomically read) mark equals the current cycle, which never holds for a slot
// a mutator is mid-initializing (its mark goes oldDead/FREE -> -1, not via cur).
static JAVA_BOOLEAN cn1BibopRescanStep() {
    while(bibopRescanPage != 0) {
        CN1BibopPage* p = bibopRescanPage;
        int n = atomic_load_explicit(&p->bumpIndex, memory_order_acquire);
        while(bibopRescanSlot < n) {
            if(gcMarkWorklistTop >= CN1_GC_MARK_WORKLIST_SIZE) {
                return JAVA_FALSE; // worklist full; caller drains and resumes
            }
            JAVA_OBJECT o = cn1BibopSlot(p, bibopRescanSlot);
            bibopRescanSlot++;
            int m = __atomic_load_n(&o->__codenameOneGcMark, __ATOMIC_ACQUIRE);
            if(m == currentGcMarkValue && o->__codenameOneParentClsReference->markFunction != 0) {
                gcMarkWorklistPush(o, JAVA_FALSE);
            }
        }
        bibopRescanPage = atomic_load_explicit(&p->nextAll, memory_order_acquire);
        bibopRescanSlot = 0;
    }
    return JAVA_TRUE;
}
#endif

// Pops worklist entries and runs their mark functions. On overflow, rescans the live
// heap to push every marked-but-unscanned object so its children get visited (the
// children's pushes are what overflowed in the first place). The rescan uses a cursor
// that resumes across batches -- restarting from iter=0 on every batch would just
// re-push the same first WORKLIST_SIZE marked objects forever while later indices got
// starved, leaving their children unmarked and freeing reachable memory at sweep.
// BiBOP page slots are NOT in allObjectsInHeap, so once an overflow is seen the rescan
// additionally walks the page registry (cn1BibopRescan*) under the same fixed point.
static void gcMarkDrain(CODENAME_ONE_THREAD_STATE) {
    int rescanCursor = 0;
#ifndef CN1_DISABLE_BIBOP
    JAVA_BOOLEAN bibopActive = JAVA_FALSE;
    JAVA_BOOLEAN bibopDone = JAVA_TRUE;
#endif
    while(JAVA_TRUE) {
        while(gcMarkWorklistTop > 0) {
            gcMarkWorklistTop--;
            JAVA_OBJECT obj = gcMarkWorklist[gcMarkWorklistTop].obj;
            JAVA_BOOLEAN force = gcMarkWorklist[gcMarkWorklistTop].force;
            gcMarkFunctionPointer fp = obj->__codenameOneParentClsReference->markFunction;
            if(fp != 0) {
                fp(threadStateData, obj, force);
            }
        }
        int total = currentSizeOfAllObjectsInHeap;
#ifndef CN1_DISABLE_BIBOP
        // First time we observe an overflow, start also rescanning page slots.
        if(gcMarkWorklistOverflow && !bibopActive) {
            bibopActive = JAVA_TRUE;
            bibopDone = JAVA_FALSE;
            cn1BibopRescanStart();
        }
        JAVA_BOOLEAN scanDone = (rescanCursor >= total) && bibopDone;
#else
        JAVA_BOOLEAN scanDone = (rescanCursor >= total);
#endif
        // Done when the worklist drained without re-overflow AND we've finished a full
        // sweep of the heap (cursor at end) AND nothing new got marked during the most
        // recent sweep. Without the cursor==total check, we'd return while there are
        // still marked objects past `cursor` whose mark functions haven't been called.
        if(!gcMarkWorklistOverflow && scanDone) {
            return;
        }
        gcMarkWorklistOverflow = JAVA_FALSE;
        if(scanDone) {
            if(!gcMarkFoundUnmarkedChildInPass) {
                // We finished a full heap sweep, drained the resulting pushes, and the
                // drain marked nothing new. Fixed point.
                return;
            }
            // Pushes from the previous sweep's drain may have marked new objects past
            // indices we already visited this round; restart the sweep so they get
            // their mark functions called too.
            rescanCursor = 0;
            gcMarkFoundUnmarkedChildInPass = JAVA_FALSE;
#ifndef CN1_DISABLE_BIBOP
            if(bibopActive) {
                cn1BibopRescanStart();
                bibopDone = JAVA_FALSE;
            }
#endif
        }
        while(rescanCursor < total && gcMarkWorklistTop < CN1_GC_MARK_WORKLIST_SIZE) {
            JAVA_OBJECT o = allObjectsInHeap[rescanCursor];
            rescanCursor++;
            if(o != JAVA_NULL && o->__codenameOneGcMark == currentGcMarkValue) {
                if(o->__codenameOneParentClsReference->markFunction != 0) {
                    gcMarkWorklistPush(o, JAVA_FALSE);
                }
            }
        }
#ifndef CN1_DISABLE_BIBOP
        // Once the table is exhausted, continue the single linear rescan space into
        // the page registry (resumes its own cursor when the worklist refills).
        if(bibopActive && rescanCursor >= total && !bibopDone) {
            bibopDone = cn1BibopRescanStep();
        }
#endif
    }
}

// Resolve the total number of markers (the GC thread + helper threads). Computed once.
static int gcMarkResolveThreadCount() {
#ifdef CN1_GC_MARK_THREADS
    int n = CN1_GC_MARK_THREADS;
#else
    long ncpu = sysconf(_SC_NPROCESSORS_ONLN);
    int n = (int)(ncpu - 1);
    if(n > 4) {
        n = 4;
    }
#endif
    if(n < 1) {
        n = 1;
    }
    return n;
}

// The body each marker (GC thread + helpers) runs for one parallel drain. Pops batches
// from the shared worklist, runs their mark functions (which push children into this
// thread's local buffer), flushes, and repeats until the worklist is empty and every
// marker is idle. See the design note at the worklist declarations.
static void gcMarkWorkerDrainLoop() {
    struct gcMarkLocalBuffer localBuf;
    localBuf.count = 0;
    gcMarkLocalBuf = &localBuf;
    struct gcMarkWorklistEntry batch[CN1_GC_MARK_BATCH];
    struct ThreadLocalData* d = gcMarkThreadState;

    pthread_mutex_lock(&gcMarkWorklistMutex);
    for(;;) {
        if(gcMarkWorklistTop > 0) {
            int n = gcMarkWorklistTop;
            if(n > CN1_GC_MARK_BATCH) {
                n = CN1_GC_MARK_BATCH;
            }
            gcMarkWorklistTop -= n;
            memcpy(batch, &gcMarkWorklist[gcMarkWorklistTop], n * sizeof(struct gcMarkWorklistEntry));
            pthread_mutex_unlock(&gcMarkWorklistMutex);

            for(int i = 0 ; i < n ; i++) {
                JAVA_OBJECT obj = batch[i].obj;
                gcMarkFunctionPointer fp = obj->__codenameOneParentClsReference->markFunction;
                if(fp != 0) {
                    fp(d, obj, batch[i].force);
                }
            }
            gcMarkFlushLocal(&localBuf);

            pthread_mutex_lock(&gcMarkWorklistMutex);
            continue;
        }
        // No work in hand and the global worklist is empty: this marker goes idle.
        gcMarkActiveWorkers--;
        if(gcMarkActiveWorkers == 0) {
            // Empty worklist AND every marker idle => the reachable set is fully drained.
            gcMarkDone = JAVA_TRUE;
            pthread_cond_broadcast(&gcMarkWorklistCond);
            break;
        }
        while(gcMarkWorklistTop == 0 && !gcMarkDone) {
            pthread_cond_wait(&gcMarkWorklistCond, &gcMarkWorklistMutex);
        }
        if(gcMarkDone) {
            break;
        }
        // Work appeared (another marker produced children) -- become active again.
        gcMarkActiveWorkers++;
    }
    pthread_mutex_unlock(&gcMarkWorklistMutex);
    gcMarkLocalBuf = 0;
}

// Helper-thread entry point. Sleeps on the control condition until the GC thread bumps
// the generation to dispatch a drain, participates, then reports completion. Lives for
// the lifetime of the process (like the GC thread itself).
static void* gcMarkWorkerMain(void* arg) {
    unsigned long myGen = 0;
    for(;;) {
        pthread_mutex_lock(&gcMarkCtlMutex);
        while(gcMarkGeneration == myGen) {
            pthread_cond_wait(&gcMarkCtlCond, &gcMarkCtlMutex);
        }
        myGen = gcMarkGeneration;
        pthread_mutex_unlock(&gcMarkCtlMutex);

        gcMarkWorkerDrainLoop();

        pthread_mutex_lock(&gcMarkCtlMutex);
        gcMarkWorkersFinished++;
        pthread_cond_broadcast(&gcMarkCtlCond);
        pthread_mutex_unlock(&gcMarkCtlMutex);
    }
    return 0;
}

// Lazily create the helper pool. Only ever called from the GC thread (single-threaded),
// so no synchronization is needed around the one-time setup.
static void gcMarkPoolEnsure() {
    if(gcMarkPoolReady) {
        return;
    }
    gcMarkThreadCount = gcMarkResolveThreadCount();
    gcMarkPoolSize = gcMarkThreadCount - 1;
    for(int i = 0 ; i < gcMarkPoolSize ; i++) {
        pthread_t tid;
        if(pthread_create(&tid, 0, gcMarkWorkerMain, 0) == 0) {
            pthread_detach(tid);
        } else {
            // Could not spawn a helper; fall back to fewer markers (at least the GC thread).
            gcMarkPoolSize = i;
            gcMarkThreadCount = i + 1;
            break;
        }
    }
    gcMarkPoolReady = JAVA_TRUE;
}

// Parallel transitive drain. The worklist has already been seeded with roots (serially,
// while the relevant mutator thread is paused). Dispatches the helper pool, participates
// on the GC thread, waits for everyone to finish, then -- only if the worklist overflowed
// -- runs one serial gcMarkDrain to execute the heap-rescan fixed point (invariant #3).
static void gcMarkDrainParallel(CODENAME_ONE_THREAD_STATE) {
    gcMarkPoolEnsure();
    if(gcMarkThreadCount <= 1) {
        // Single marker configured: behave exactly like before -- no pool, no atomics.
        gcMarkDrain(threadStateData);
        return;
    }

    gcMarkThreadState = threadStateData;

    // Reset termination state. Safe to touch unlocked here: the previous generation's
    // helpers have all reported finished (we waited below) and are parked on the control
    // condition, and the GC thread is the only one running between generations.
    pthread_mutex_lock(&gcMarkWorklistMutex);
    gcMarkActiveWorkers = gcMarkThreadCount; // GC thread + helpers
    gcMarkDone = JAVA_FALSE;
    pthread_mutex_unlock(&gcMarkWorklistMutex);

    // Dispatch the helpers.
    pthread_mutex_lock(&gcMarkCtlMutex);
    gcMarkWorkersFinished = 0;
    gcMarkGeneration++;
    pthread_cond_broadcast(&gcMarkCtlCond);
    pthread_mutex_unlock(&gcMarkCtlMutex);

    // The GC thread participates as one marker.
    gcMarkWorkerDrainLoop();

    // Wait for the helpers to finish this generation before returning (so no helper is
    // still touching mark bits when the caller proceeds to release threads / sweep).
    pthread_mutex_lock(&gcMarkCtlMutex);
    while(gcMarkWorkersFinished < gcMarkPoolSize) {
        pthread_cond_wait(&gcMarkCtlCond, &gcMarkCtlMutex);
    }
    pthread_mutex_unlock(&gcMarkCtlMutex);

    // Overflow safety net: finish deferred field scans with the serial rescan fixed point.
    if(gcMarkWorklistOverflow) {
        gcMarkDrain(threadStateData);
    }
}

JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim) {
    int actualSize = length * primitiveSize;
    JAVA_ARRAY array = (JAVA_ARRAY)codenameOneGcMalloc(threadStateData, sizeof(struct JavaArrayPrototype) + actualSize + sizeof(void*), type);
    (*array).length = length;
    (*array).dimensions = dim;
    (*array).primitiveSize = primitiveSize;
    if(actualSize > 0) {
        void* arr = &(array->data);
        arr += sizeof(void*);
        (*array).data = arr;
    } else {
        (*array).data = 0;
    }
    return (JAVA_OBJECT)array;
}

JAVA_OBJECT allocArrayAligned(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim, int alignment) {
    int actualSize = length * primitiveSize;
    int requestedAlignment = alignment;
    if (requestedAlignment < (int)sizeof(void*)) {
        requestedAlignment = (int)sizeof(void*);
    }
    if ((requestedAlignment & (requestedAlignment - 1)) != 0) {
        requestedAlignment = 16;
    }
    int extraPadding = requestedAlignment - 1;
    JAVA_ARRAY array = (JAVA_ARRAY)codenameOneGcMalloc(threadStateData, sizeof(struct JavaArrayPrototype) + actualSize + sizeof(void*) + extraPadding, type);
    (*array).length = length;
    (*array).dimensions = dim;
    (*array).primitiveSize = primitiveSize;
    if (actualSize > 0) {
        char* arr = (char*)(&(array->data));
        arr += sizeof(void*);
        uintptr_t aligned = (((uintptr_t)arr) + ((uintptr_t)requestedAlignment - 1)) & ~((uintptr_t)requestedAlignment - 1);
        (*array).data = (void*)aligned;
    } else {
        (*array).data = 0;
    }
    return (JAVA_OBJECT)array;
}

JAVA_OBJECT alloc2DArray(CODENAME_ONE_THREAD_STATE, int length2, int length1, struct clazz* parentType, struct clazz* childType, int primitiveSize) {
    JAVA_ARRAY base = (JAVA_ARRAY)allocArray(threadStateData, length1, parentType, sizeof(JAVA_OBJECT), 2);
    JAVA_ARRAY_OBJECT* objs = base->data;
    if(length2 > -1) {
        for(int iter = 0 ; iter < length1 ; iter++) {
            objs[iter] = allocArray(threadStateData, length2, childType, primitiveSize, 1);
        }
    }
    return (JAVA_OBJECT)base;
}

JAVA_OBJECT alloc3DArray(CODENAME_ONE_THREAD_STATE, int length3, int length2, int length1, struct clazz* parentType, struct clazz* childType, struct clazz* grandChildType, int primitiveSize) {
    JAVA_ARRAY base = (JAVA_ARRAY)allocArray(threadStateData, length1, parentType, sizeof(JAVA_OBJECT), 3);
    JAVA_ARRAY_OBJECT* objs = base->data;
    if(length2 > -1) {
        for(int iter = 0 ; iter < length1 ; iter++) {
            objs[iter] = allocArray(threadStateData, length2, childType, sizeof(JAVA_OBJECT), 2);
            if(length3 > -1) {
                JAVA_ARRAY_OBJECT* internal = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)objs[iter])->data;
                for(int inner = 0 ; inner < length2 ; inner++) {
                    internal[inner] = allocArray(threadStateData, length3, grandChildType, primitiveSize, 1);
                }
            }
        }
    }
    return (JAVA_OBJECT)base;
}

JAVA_OBJECT alloc4DArray(CODENAME_ONE_THREAD_STATE, int length4, int length3, int length2, int length1, struct clazz* parentType, struct clazz* childType, struct clazz* grandChildType, struct clazz* greatGrandChildType, int primitiveSize) {
    JAVA_ARRAY base = (JAVA_ARRAY)allocArray(threadStateData, length1, parentType, sizeof(JAVA_OBJECT), 4);
    JAVA_ARRAY_OBJECT* objs = base->data;
    if(length2 > -1) {
        for(int iter = 0 ; iter < length1 ; iter++) {
            objs[iter] = allocArray(threadStateData, length2, childType, sizeof(JAVA_OBJECT), 3);
            if(length3 > -1) {
                JAVA_ARRAY_OBJECT* internal = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)objs[iter])->data;
                for(int inner = 0 ; inner < length2 ; inner++) {
                    internal[inner] = allocArray(threadStateData, length3, grandChildType, sizeof(JAVA_OBJECT), 2);
                    if(length4 > -1) {
                        JAVA_ARRAY_OBJECT* deep = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)internal[inner])->data;
                        for(int deepInner = 0 ; deepInner < length3 ; deepInner++) {
                            deep[deepInner] = allocArray(threadStateData, length4, greatGrandChildType, primitiveSize, 1);
                        }
                    }
                }
            }
        }
    }
    return (JAVA_OBJECT)base;
}

/**
 * Creates a java.lang.String object from an array of integers, this is useful
 * for the constant pool
 */
JAVA_OBJECT newString(CODENAME_ONE_THREAD_STATE, int length, JAVA_CHAR data[]) {
    enteringNativeAllocations();
    JAVA_ARRAY dat = (JAVA_ARRAY)allocArray(threadStateData, length, &class_array1__JAVA_CHAR, sizeof(JAVA_CHAR), 1);
    memcpy((*dat).data, data, length * sizeof(JAVA_ARRAY_CHAR));
    JAVA_OBJECT o = __NEW_java_lang_String(threadStateData);
    java_lang_String___INIT____(threadStateData, o);
    struct obj__java_lang_String* str = (struct obj__java_lang_String*)o;
    str->java_lang_String_value = (JAVA_OBJECT)dat;
    str->java_lang_String_count = length;
    finishedNativeAllocations();
    return o;
}

/**
 * Creates a java.lang.String object from a c string
 */
JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char *str) {
    if(str == 0) {
        return JAVA_NULL;
    }
    enteringNativeAllocations();
    int length = (int)strlen(str);
    JAVA_ARRAY dat = (JAVA_ARRAY)allocArray(threadStateData, length, &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), 1);
    JAVA_ARRAY_CHAR* arr = (JAVA_ARRAY_CHAR*) (*dat).data;
    JAVA_BOOLEAN slash = JAVA_FALSE;
    int offset = 0;
    for(int iter = 0 ; iter < length ; iter++) {
        arr[offset] = str[iter];
        if(str[iter] == '~' && iter + 6 < length && str[iter+1] == '~' && str[iter+2] == 'u') {
            char constructB[5];
            constructB[0] = str[iter + 3];
            constructB[1] = str[iter + 4];
            constructB[2] = str[iter + 5];
            constructB[3] = str[iter + 6];
            constructB[4] = 0;
            arr[offset] = strtol(constructB, NULL, 16);
            iter += 6;
        }
        offset++;
    }
    JAVA_OBJECT o = __NEW_java_lang_String(threadStateData);
    //java_lang_String___INIT_____char_1ARRAY(threadStateData, o, (JAVA_OBJECT)dat);
    //releaseObj(threadStateData, (JAVA_OBJECT)dat);
    java_lang_String___INIT____(threadStateData, o);
    struct obj__java_lang_String* ss = (struct obj__java_lang_String*)o;
    ss->java_lang_String_value = (JAVA_OBJECT)dat;
    ss->java_lang_String_count = offset;
    finishedNativeAllocations();
    return o;
}

/**
 * XMLVM compatibility layer
 */
JAVA_OBJECT xmlvm_create_java_string(CODENAME_ONE_THREAD_STATE, const char *chr) {
    return newStringFromCString(threadStateData, chr);
}

void initConstantPool() {
    __STATIC_INITIALIZER_java_lang_Class(getThreadLocalData());
    struct ThreadLocalData* threadStateData = getThreadLocalData();
    enteringNativeAllocations();
    JAVA_ARRAY arr = (JAVA_ARRAY)allocArray(threadStateData, CN1_CONSTANT_POOL_SIZE, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    JAVA_OBJECT* tmpConstantPoolObjects = (JAVA_ARRAY_OBJECT*)(*arr).data;
    
    // the constant pool should not be deleted...
    for(int iter = 0 ; iter < threadStateData->heapAllocationSize ; iter++) {
        if(threadStateData->pendingHeapAllocations[iter] == arr)  {
            threadStateData->pendingHeapAllocations[iter] = JAVA_NULL;
            break;
        }
    }
    invokedGC = YES;
    //int cStringSize = CN1_CONSTANT_POOL_SIZE * sizeof(char*);
    //int jStringSize = CN1_CONSTANT_POOL_SIZE * sizeof(JAVA_ARRAY);
    //JAVA_OBJECT internedStrings = get_static_java_lang_String_str();
    for(int iter = 0 ; iter < CN1_CONSTANT_POOL_SIZE ; iter++) {
        //long length = strlen(constantPool[iter]);
        //cStringSize += length + 1;
        //jStringSize += length * sizeof(JAVA_ARRAY_CHAR) + sizeof(struct JavaArrayPrototype) + sizeof(struct obj__java_lang_String);
        JAVA_OBJECT oo = newStringFromCString(threadStateData, constantPool[iter]);
        tmpConstantPoolObjects[iter] = oo;
        tmpConstantPoolObjects[iter]->__codenameOneReferenceCount = 999999;
       // java_util_ArrayList_add___java_lang_Object_R_boolean(threadStateData, internedStrings, oo);
    }
    #if defined(__OBJC__)
    //NSLog(@"Size of constant pool in c: %i and j: %i", cStringSize, jStringSize);
    #endif
    constantPoolObjects = tmpConstantPoolObjects;
    invokedGC = NO;

    enteringNativeAllocations();

    // it will wait two seconds unless an explicit GC occurs
    java_lang_System_startGCThread__(threadStateData);
    finishedNativeAllocations();
}

JAVA_OBJECT utf8String = NULL;

#if defined(__APPLE__) && defined(__OBJC__)
JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str) {
    if (str == nil) {
        return JAVA_NULL;
    }
    enteringNativeAllocations();
    if (utf8String == JAVA_NULL) {
        utf8String = newStringFromCString(threadStateData, "UTF-8");
        removeObjectFromHeapCollection(threadStateData, utf8String);
        removeObjectFromHeapCollection(threadStateData, ((struct obj__java_lang_String*)utf8String)->java_lang_String_value);
        ((struct obj__java_lang_String*)utf8String)->java_lang_String_value->__codenameOneReferenceCount = 999999;
        utf8String->__codenameOneReferenceCount = 999999;
    }
    JAVA_OBJECT s = __NEW_java_lang_String(threadStateData);
    const char* chars = [str UTF8String];
    int length = (int)strlen(chars);
    
    JAVA_ARRAY dat = (JAVA_ARRAY)allocArray(threadStateData, length, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    memcpy((*dat).data, chars, length * sizeof(JAVA_ARRAY_BYTE));
    java_lang_String___INIT_____byte_1ARRAY_java_lang_String(threadStateData, s, (JAVA_OBJECT)dat, utf8String);
    struct obj__java_lang_String* nnn = (struct obj__java_lang_String*)s;
    nnn->java_lang_String_nsString = str;
    [str retain];
    finishedNativeAllocations();
    return s;
}
#endif

const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str) {
    if(str == NULL) {
        return NULL;
    }
    if (utf8String == JAVA_NULL) {
        utf8String = newStringFromCString(threadStateData, "UTF-8");
        removeObjectFromHeapCollection(threadStateData, utf8String);
        removeObjectFromHeapCollection(threadStateData, ((struct obj__java_lang_String*)utf8String)->java_lang_String_value);
        ((struct obj__java_lang_String*)utf8String)->java_lang_String_value->__codenameOneReferenceCount = 999999;
        utf8String->__codenameOneReferenceCount = 999999;
    }

    JAVA_ARRAY byteArray = (JAVA_ARRAY)java_lang_String_getBytes___java_lang_String_R_byte_1ARRAY(threadStateData, str, utf8String);
    JAVA_ARRAY_BYTE* data = (*byteArray).data;

    JAVA_INT len = byteArray->length;

    if(threadStateData->utf8Buffer == 0) {
        threadStateData->utf8Buffer = malloc(len + 1);
        threadStateData->utf8BufferSize = len+1;
    } else {
        if(threadStateData->utf8BufferSize < len + 1) {
            free(threadStateData->utf8Buffer);
            threadStateData->utf8Buffer = malloc(len + 1);
            threadStateData->utf8BufferSize = len+1;
        }
    }
    char* cs = threadStateData->utf8Buffer;
    memcpy(cs, data, len);
    cs[len] = '\0';
    return cs;
}

#if defined(__APPLE__) && defined(__OBJC__)
NSString* toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    if(o == JAVA_NULL) {
        return 0;
    }
    struct obj__java_lang_String* str = (struct obj__java_lang_String*)o;
    if(str->java_lang_String_nsString != 0) {
        void* v = (void*)str->java_lang_String_nsString;
        return (__bridge NSString*)v;
    }
    const char* chrs = stringToUTF8(threadStateData, o);
    NSString* st = [[NSString stringWithUTF8String:chrs] retain];
    void *x = (__bridge void *)(st);
    str->java_lang_String_nsString = (JAVA_LONG)x;
    return st;
}
#endif

JAVA_OBJECT __NEW_ARRAY_JAVA_BOOLEAN(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_BOOLEAN, sizeof(JAVA_ARRAY_BOOLEAN), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_BOOLEAN;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_CHAR(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_CHAR;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_BYTE(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_BYTE;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_SHORT(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_SHORT, sizeof(JAVA_ARRAY_SHORT), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_SHORT;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_INT(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_INT, sizeof(JAVA_ARRAY_INT), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_INT;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_LONG(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_LONG, sizeof(JAVA_ARRAY_LONG), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_LONG;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_FLOAT(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_FLOAT, sizeof(JAVA_ARRAY_FLOAT), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_FLOAT;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_DOUBLE(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_DOUBLE, sizeof(JAVA_ARRAY_DOUBLE), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_DOUBLE;
    return o;
}

void throwException(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exceptionArg) {
    #if defined(__OBJC__)
    //NSLog(@"Throwing exception!"); 
    #endif
    java_lang_Throwable_fillInStack__(threadStateData, exceptionArg); 
    threadStateData->exception = exceptionArg; 
    threadStateData->tryBlockOffset--; 
    while(threadStateData->tryBlockOffset >= 0) { 
        if (threadStateData->blocks[threadStateData->tryBlockOffset].monitor != 0) {
            // This tryblock was actually created by a synchronized method's monitorEnterBlock
            // We need to exit the monitor since the exception will cause us to 
            // leave the method.
            monitorExitBlock(threadStateData, threadStateData->blocks[threadStateData->tryBlockOffset].monitor);
            // Continue to search for a matching exception ...
            continue;
        } else if(threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass <= 0 || instanceofFunction(threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass, exceptionArg->__codenameOneParentClsReference->classId)) {
            int off = threadStateData->tryBlockOffset;
            longjmp(threadStateData->blocks[off].destination, 1);
            return;
        } 
        threadStateData->tryBlockOffset--; 
    } 
}

JAVA_INT throwException_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exceptionArg) {
    throwException(threadStateData, exceptionArg);
    return 0;
}

JAVA_BOOLEAN throwException_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exceptionArg) {
    throwException(threadStateData, exceptionArg);
    return JAVA_FALSE;
}

void throwArrayIndexOutOfBoundsException(CODENAME_ONE_THREAD_STATE, int index) {
    JAVA_OBJECT arrayIndexOutOfBoundsException = __NEW_java_lang_ArrayIndexOutOfBoundsException(threadStateData);
    java_lang_ArrayIndexOutOfBoundsException___INIT_____int(threadStateData, arrayIndexOutOfBoundsException, index);
    throwException(threadStateData, arrayIndexOutOfBoundsException);
}

JAVA_BOOLEAN throwArrayIndexOutOfBoundsException_R_boolean(CODENAME_ONE_THREAD_STATE, int index) {
    throwArrayIndexOutOfBoundsException(threadStateData, index);
    return JAVA_FALSE;
}

void** interfaceVtableGlobal = 0;

void** initVtableForInterface() {
    if(interfaceVtableGlobal == 0) {
        interfaceVtableGlobal = malloc(9 * sizeof(void*));
        interfaceVtableGlobal[0] = &java_lang_Object_equals___java_lang_Object_R_boolean;
        interfaceVtableGlobal[1] = &java_lang_Object_getClass___R_java_lang_Class;
        interfaceVtableGlobal[2] = &java_lang_Object_hashCode___R_int;
        interfaceVtableGlobal[3] = &java_lang_Object_notify__;
        interfaceVtableGlobal[4] = &java_lang_Object_notifyAll__;
        interfaceVtableGlobal[5] = &java_lang_Object_toString___R_java_lang_String;
        interfaceVtableGlobal[6] = &java_lang_Object_wait__;
        interfaceVtableGlobal[7] = &java_lang_Object_wait___long;
        interfaceVtableGlobal[8] = &java_lang_Object_wait___long_int;
        class_array1__JAVA_BOOLEAN.vtable = interfaceVtableGlobal;
        class_array2__JAVA_BOOLEAN.vtable = interfaceVtableGlobal;
        class_array3__JAVA_BOOLEAN.vtable = interfaceVtableGlobal;
        class_array1__JAVA_CHAR.vtable = interfaceVtableGlobal;
        class_array2__JAVA_CHAR.vtable = interfaceVtableGlobal;
        class_array3__JAVA_CHAR.vtable = interfaceVtableGlobal;
        class_array1__JAVA_BYTE.vtable = interfaceVtableGlobal;
        class_array2__JAVA_BYTE.vtable = interfaceVtableGlobal;
        class_array3__JAVA_BYTE.vtable = interfaceVtableGlobal;
        class_array1__JAVA_SHORT.vtable = interfaceVtableGlobal;
        class_array2__JAVA_SHORT.vtable = interfaceVtableGlobal;
        class_array3__JAVA_SHORT.vtable = interfaceVtableGlobal;
        class_array1__JAVA_INT.vtable = interfaceVtableGlobal;
        class_array2__JAVA_INT.vtable = interfaceVtableGlobal;
        class_array3__JAVA_INT.vtable = interfaceVtableGlobal;
        class_array1__JAVA_LONG.vtable = interfaceVtableGlobal;
        class_array2__JAVA_LONG.vtable = interfaceVtableGlobal;
        class_array3__JAVA_LONG.vtable = interfaceVtableGlobal;
        class_array1__JAVA_FLOAT.vtable = interfaceVtableGlobal;
        class_array2__JAVA_FLOAT.vtable = interfaceVtableGlobal;
        class_array3__JAVA_FLOAT.vtable = interfaceVtableGlobal;
        class_array1__JAVA_DOUBLE.vtable = interfaceVtableGlobal;
        class_array2__JAVA_DOUBLE.vtable = interfaceVtableGlobal;
        class_array3__JAVA_DOUBLE.vtable = interfaceVtableGlobal;
    }
    return interfaceVtableGlobal;
}

int byteSizeForArray(struct clazz* cls) {
    int byteSize = sizeof(JAVA_ARRAY_BYTE);
    if( cls->primitiveType ) {
        if((*cls).arrayType == &class__java_lang_Long) {
            byteSize = sizeof(JAVA_ARRAY_LONG);
        } else {
            if((*cls).arrayType == &class__java_lang_Double) {
                byteSize = sizeof(JAVA_ARRAY_DOUBLE);
            } else {
                if((*cls).arrayType == &class__java_lang_Float) {
                    byteSize = sizeof(JAVA_ARRAY_FLOAT);
                } else {
                    if((*cls).arrayType == &class__java_lang_Byte) {
                        byteSize = sizeof(JAVA_ARRAY_BYTE);
                    } else {
                        if((*cls).arrayType == &class__java_lang_Short) {
                            byteSize = sizeof(JAVA_ARRAY_SHORT);
                        } else {
                            if((*cls).arrayType == &class__java_lang_Integer) {
                                byteSize = sizeof(JAVA_ARRAY_INT);
                            } else {
                                if((*cls).arrayType == &class__java_lang_Character) {
                                    byteSize = sizeof(JAVA_ARRAY_CHAR);
                                } else {
                                    if((*cls).arrayType == &class__java_lang_Boolean) {
                                        byteSize = sizeof(JAVA_ARRAY_BOOLEAN);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        byteSize = sizeof(JAVA_OBJECT);
    }
    return byteSize;
}

JAVA_OBJECT cloneArray(JAVA_OBJECT array) {
    JAVA_ARRAY src = (JAVA_ARRAY)array;

    struct clazz* cls = array->__codenameOneParentClsReference;
    int byteSize = byteSizeForArray(cls);

    JAVA_ARRAY arr = (JAVA_ARRAY)allocArray(getThreadLocalData(), src->length, cls, byteSize, src->dimensions);
    memcpy( (*arr).data, (*src).data, arr->length * byteSize);
    return (JAVA_OBJECT)arr;
}

#ifdef CN1_ON_DEVICE_DEBUG
// Default-zero flag. The iOS on-device-debug listener flips this to 1 once
// it has accepted a proxy connection. Weak so a stronger definition in
// cn1_debugger.m (iOS port) wins when that file is linked into the build.
__attribute__((weak)) volatile int cn1DebuggerActive = 0;

// Weak stub that lets non-iOS / clean-output builds link even without the
// real listener. The strong implementation lives in
// Ports/iOSPort/nativeSources/cn1_debugger.m and is included in the iOS
// build when ios.onDeviceDebug=true.
__attribute__((weak)) void cn1_debugger_check(struct ThreadLocalData* threadStateData, int line) {
    (void)threadStateData;
    (void)line;
}
#endif
