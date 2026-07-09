// glibc/musl hide pthread_getattr_np and the ucontext REG_* gregs indices
// behind _GNU_SOURCE; must be defined before the first libc include.
#if defined(__linux__) && !defined(_GNU_SOURCE)
#define _GNU_SOURCE
#endif
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
#if defined(__APPLE__) && !defined(__OBJC__)
#include <sys/sysctl.h>   // sysctlbyname for the non-OBJC get_free_memory headroom proxy
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

// AVAILABLE memory (not just free_count) -- used ONLY by the dynamic GC pacing cap, kept separate
// from get_free_memory so the existing heap-threshold sizing (init_gc_thresholds) is unchanged.
// iOS/macOS keep RAM full of reclaimable file cache, so free_count alone is always tiny (~100MB)
// and badly under-reports what the process can still allocate; inactive + purgeable pages are
// reclaimable under pressure, so free + inactive + purgeable ~= the real headroom the collector
// can safely let a high-throughput thread run into. Non-OBJC Apple (bench/desktop) lacks the mach
// vm_statistics headers here, so it falls back to half of physical RAM via sysctl.
static long cn1_available_memory(void)
 {
#if defined(__APPLE__) && defined(__OBJC__)
   mach_port_t host_port = mach_host_self();
   mach_msg_type_number_t host_size = sizeof(vm_statistics_data_t) / sizeof(integer_t);
   vm_size_t pagesize;
   host_page_size(host_port, &pagesize);
   vm_statistics_data_t vm_stat;
   if (host_statistics(host_port, HOST_VM_INFO, (host_info_t)&vm_stat, &host_size) != KERN_SUCCESS) {
     return 1024L * 1024 * 100;
   }
   return ((long)vm_stat.free_count + (long)vm_stat.inactive_count
           + (long)vm_stat.purgeable_count) * (long)pagesize;
#elif defined(__APPLE__)
   uint64_t __total = 0; size_t __len = sizeof(__total);
   if (sysctlbyname("hw.memsize", &__total, &__len, NULL, 0) == 0 && __total > 0) {
     return (long)(__total / 2);
   }
   return 1024L * 1024 * 100;
#else
   return 1024L * 1024 * 100;
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
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_BOOLEAN, "boolean[]", JAVA_TRUE, 1, &class__java_lang_Boolean, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_BOOLEAN = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_BOOLEAN, "boolean[]", JAVA_TRUE, 2, &class__java_lang_Boolean, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_BOOLEAN = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_BOOLEAN, "boolean[]", JAVA_TRUE, 3, &class__java_lang_Boolean, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_CHAR = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_CHAR, "char[]", JAVA_TRUE, 1, &class__java_lang_Character, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_CHAR = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_CHAR, "char[]", JAVA_TRUE, 2, &class__java_lang_Character, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_CHAR = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_CHAR, "char[]", JAVA_TRUE, 3, &class__java_lang_Character, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_BYTE = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_BYTE, "byte[]", JAVA_TRUE, 1, &class__java_lang_Byte, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_BYTE = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_BYTE, "byte[]", JAVA_TRUE, 2, &class__java_lang_Byte, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_BYTE = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_BYTE, "byte[]", JAVA_TRUE, 3, &class__java_lang_Byte, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_SHORT = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_SHORT, "short[]", JAVA_TRUE, 1, &class__java_lang_Short, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_SHORT = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_SHORT, "short[]", JAVA_TRUE, 2, &class__java_lang_Short, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_SHORT = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_SHORT, "short[]", JAVA_TRUE, 3, &class__java_lang_Short, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_INT = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_INT, "int[]", JAVA_TRUE, 1, &class__java_lang_Integer, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_INT = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_INT, "int[]", JAVA_TRUE, 2, &class__java_lang_Integer, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_INT = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_INT, "int[]", JAVA_TRUE, 3, &class__java_lang_Integer, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_LONG = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_LONG, "long[]", JAVA_TRUE, 1, &class__java_lang_Long, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_LONG = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_LONG, "long[]", JAVA_TRUE, 2, &class__java_lang_Long, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_LONG = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_LONG, "long[]", JAVA_TRUE, 3, &class__java_lang_Long, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_FLOAT = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_FLOAT, "float[]", JAVA_TRUE, 1, &class__java_lang_Float, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_FLOAT = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_FLOAT, "float[]", JAVA_TRUE, 2, &class__java_lang_Float, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_FLOAT = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_FLOAT, "float[]", JAVA_TRUE, 3, &class__java_lang_Float, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_DOUBLE = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_DOUBLE, "double[]", JAVA_TRUE, 1, &class__java_lang_Double, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_DOUBLE = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_DOUBLE, "double[]", JAVA_TRUE, 2, &class__java_lang_Double, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_DOUBLE = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_DOUBLE, "double[]", JAVA_TRUE, 3, &class__java_lang_Double, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
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

// java.lang.String has NO Java finalizer (it would tax every string-bearing
// page with per-slot reclaim walks on every platform). Its cached NSString
// peer is released here instead -- a cost only ObjC targets pay, and (for
// BiBOP pages) only on pages flagged by cn1BibopNoteNativePeer at cache time.
#if defined(__APPLE__) && defined(__OBJC__)
extern struct clazz class__java_lang_String;
static inline void cn1ReleaseStringPeer(JAVA_OBJECT o) {
    if(o->__codenameOneParentClsReference == &class__java_lang_String) {
        struct obj__java_lang_String* s = (struct obj__java_lang_String*)o;
        if(s->java_lang_String_nsString != 0) {
            void* v = (void*)s->java_lang_String_nsString;
            [(__bridge NSString*)v release];
            s->java_lang_String_nsString = 0;
        }
    }
}
#else
#define cn1ReleaseStringPeer(o) do {} while(0)
#endif

void freeAndFinalize(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    finalizerFunctionPointer ptr = (finalizerFunctionPointer)obj->__codenameOneParentClsReference->finalizerFunction;
    if(ptr != 0) {
        // Per the Java spec, an exception thrown by finalize() is IGNORED -- and it must
        // NEVER escape the collector. This runs during sweep on the GC thread; if a
        // finalizer throws (observed on iOS: a native peer finalizer during DrawGradientStops)
        // and the exception is allowed to unwind, it propagates out through codenameOneGCSweep
        // -> java_lang_System_gcMarkSweep__ (whose gcCurrentlyRunning=FALSE reset is then
        // SKIPPED, leaving the flag stuck TRUE) -> the GC thread's run loop, which only catches
        // InterruptedException -> the GC thread dies WITHOUT clearing gcThreadInstance. The EDT
        // then deadlocks forever in cn1BibopMaybeGc's allocation backpressure spin (it can never
        // trigger a GC because gcCurrentlyRunning is stuck true, and spins because the dead GC
        // thread's instance is still non-null) -> deterministic mid-suite hang. Run the finalizer
        // inside a catch-all try block and swallow anything it throws.
        int __savedTryBlock = threadStateData->tryBlockOffset;
        jmp_buf __finTryJmp;
        if(setjmp(__finTryJmp) == 0) {
            threadStateData->blocks[threadStateData->tryBlockOffset].monitor = 0;
            threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass = 0; // catch-all
            memcpy(threadStateData->blocks[threadStateData->tryBlockOffset].destination, __finTryJmp, sizeof(jmp_buf));
            threadStateData->tryBlockOffset++;
            ptr(threadStateData, obj);
            threadStateData->tryBlockOffset = __savedTryBlock;
        } else {
            // finalizer threw -> restore the try-block stack and drop the exception
            threadStateData->tryBlockOffset = __savedTryBlock;
            threadStateData->exception = JAVA_NULL;
        }
    }
    cn1ReleaseStringPeer(obj);
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
const char* volatile cn1LastNamSetter = 0;
JAVA_OBJECT* allObjectsInHeap = 0;
int sizeOfAllObjectsInHeap = 30000;
int currentSizeOfAllObjectsInHeap = 0;

// SINGLE-WRITER INVARIANT for allObjectsInHeap: the table is grown and walked
// ONLY on the GC thread (mark migration, the dead-thread drain below, sweep,
// root-snapshot build, overflow rescan -- all phases of the same thread, so
// they are mutually sequential). The only non-GC-thread accesses are one-shot
// slot writes under the critical section (getStack's immortal-string removal).
// Dying threads therefore must NOT call placeObjectInHeapCollection (a growth's
// realloc-and-free would race an in-flight GC-thread walk -- observed shape:
// a sweep's slot-NULL lost in the memcpy'd copy resurrects a freed pointer, or
// two growths during one hoisted-pointer walk free the array under the reader).
// Instead markDeadThread queues the dying thread's TLD here (critical section
// held) and the GC thread drains it at the start of the next mark -- strictly
// before that cycle's sweep, so the migration always precedes any possible
// finalization of the Thread object. Objects still in a queued TLD's pending
// list are invisible to the sweep (only table entries are swept), so the
// deferral can never free them early.
static struct ThreadLocalData* cn1DeadPendingThreads = 0;  // guarded by criticalSection
extern void cn1ReleaseThreadLocalData(struct ThreadLocalData* head);

// ---- Immortal roots ------------------------------------------------------
// VM-internal objects referenced ONLY from C globals (e.g. getStack's cached
// separator strings) are invisible to every root source; the old trick of
// removeObjectFromHeapCollection'ing them only worked while such objects lived
// in the legacy table -- for BiBOP-resident objects (all small objects now,
// arrays too) removal is a no-op and the object WOULD be swept while the C
// global still points at it. Register them here instead; the mark phase treats
// the array as a root set every cycle. Tiny and append-only.
static JAVA_OBJECT* cn1ImmortalRoots = 0;
static int cn1ImmortalRootsN = 0, cn1ImmortalRootsCap = 0;
void cn1AddImmortalRoot(JAVA_OBJECT o) {
    if(o == JAVA_NULL) return;
    lockCriticalSection();
    if(cn1ImmortalRootsN == cn1ImmortalRootsCap) {
        cn1ImmortalRootsCap = cn1ImmortalRootsCap ? cn1ImmortalRootsCap * 2 : 64;
        cn1ImmortalRoots = (JAVA_OBJECT*)realloc(cn1ImmortalRoots, cn1ImmortalRootsCap * sizeof(JAVA_OBJECT));
    }
    cn1ImmortalRoots[cn1ImmortalRootsN++] = o;
    unlockCriticalSection();
}
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
            // Immediate free is safe under the SINGLE-WRITER INVARIANT (see the
            // cn1DeadPendingThreads comment): growth only ever runs on the GC
            // thread, whose own walks are sequential with it, and every non-GC-
            // thread access holds the critical section that the growth callers
            // (mark migration / dead-thread drain) also hold. The old one-growth
            // deferral could still double-free under two growths in one walk.
            JAVA_OBJECT* replaced = allObjectsInHeap;
            allObjectsInHeap = tmpAllObjectsInHeap;
            free(replaced);
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

// Graduate a surviving BiBOP object into the legacy mark/sweep (poor-man's generational
// promotion). NON-MOVING: the object's memory stays in its BiBOP slot; we only register
// it in allObjectsInHeap (so the unconditional legacy rescan traces it -- always complete,
// unlike the overflow-gated BiBOP rescan) and flag its slot CN1_BIBOP_ADOPTED so the BiBOP
// page sweep skips it (one owner, no double-clearing). Runs on the GC thread during the
// mark; mutators never touch allObjectsInHeap directly and nothing moves, so no reference
// can dangle. placeObjectInHeapCollection sets heapPosition to the array index; we override
// it to the -4 sentinel (the legacy sweep finds the object by walk-index, not heapPosition).
#ifndef CN1_DISABLE_BIBOP
// Objects matured during a mark are buffered here and registered into allObjectsInHeap
// AFTER the mark completes (cn1DrainAdoptBuffer). placeObjectInHeapCollection reallocs +
// frees that table, which is UNSAFE during the mark: the drain walks the same table, and
// under parallel markers several threads would grow it at once. So maturation only FLAGS
// the object (-4) during the mark and defers the table mutation to a single-threaded,
// locked, post-mark pass.
static pthread_mutex_t gcAdoptMutex = PTHREAD_MUTEX_INITIALIZER;
static JAVA_OBJECT* gcAdoptStack = 0;
static long gcAdoptTop = 0, gcAdoptCap = 0;
#endif

static void cn1MatureObject(JAVA_OBJECT obj) {
#ifndef CN1_DISABLE_BIBOP
    // Claim the object for adoption exactly ONCE with a CAS -3 -> -4. Under parallel
    // markers two threads can both reach the same object; the CAS loser must not
    // double-buffer/double-register. The -4 flag takes effect immediately so the cascade,
    // the mark-stamp and the sweep-skip all see it during THIS mark.
    int expected = CN1_BIBOP_HEAP_POS;
    if(!__atomic_compare_exchange_n(&obj->__heapPosition, &expected, CN1_BIBOP_ADOPTED,
                                    0, __ATOMIC_RELAXED, __ATOMIC_RELAXED)) {
        return;
    }
    // Sticky-flag the host page so its slots always take the full per-slot sweep walk
    // (which skips live -4 slots) instead of the O(1) page reset, which would recycle this
    // still-live object's memory out from under the legacy collector.
    ((CN1BibopPage*)(((uintptr_t)obj) & ~((uintptr_t)CN1_BIBOP_PAGE_SIZE - 1)))->gcHasAdopted = JAVA_TRUE;
    // Buffer for post-mark registration (NOT placeObjectInHeapCollection here -- see above).
    pthread_mutex_lock(&gcAdoptMutex);
    if(gcAdoptTop >= gcAdoptCap) {
        long ncap = gcAdoptCap ? gcAdoptCap * 2 : 4096;
        JAVA_OBJECT* n = (JAVA_OBJECT*)realloc(gcAdoptStack, (size_t)ncap * sizeof(JAVA_OBJECT));
        if(n == 0) { pthread_mutex_unlock(&gcAdoptMutex); return; } // OOM: leave it flagged -4, unregistered (alive; retried next cycle it survives)
        gcAdoptStack = n; gcAdoptCap = ncap;
    }
    gcAdoptStack[gcAdoptTop++] = obj;
    pthread_mutex_unlock(&gcAdoptMutex);
#endif
}

#ifndef CN1_DISABLE_BIBOP
// Register every object matured during the just-finished mark into allObjectsInHeap.
// Runs on the GC thread AFTER the parallel mark has joined (single-threaded) and holds the
// critical section -- the invariant placeObjectInHeapCollection's grow-and-free relies on.
// Called before the sweep, so the legacy sweep sees these -4 objects (marked live) and
// keeps them; from next cycle the complete legacy rescan traces them.
static void cn1DrainAdoptBuffer() {
    if(gcAdoptTop == 0) {
        return;
    }
    lockCriticalSection();
    for(long i = 0 ; i < gcAdoptTop ; i++) {
        JAVA_OBJECT o = gcAdoptStack[i];
        placeObjectInHeapCollection(o);      // sets heapPosition to the array index...
        o->__heapPosition = CN1_BIBOP_ADOPTED; // ...restore the -4 sentinel (found by walk-index)
    }
    gcAdoptTop = 0;
    unlockCriticalSection();
}
#endif

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
    // LEVER A: flush any unaccounted per-thread bytes into the global GC trigger.
    CN1_BIBOP_FLUSH_BYTES(current);
#endif
    if(current->utf8Buffer != 0) {
        free(current->utf8Buffer);
        current->utf8Buffer = 0;
    }
    // SINGLE-WRITER allObjectsInHeap: do NOT migrate pendingHeapAllocations here
    // -- this runs on the DYING thread, and a table growth here races the GC
    // thread's lock-free sweep/snapshot walks (see cn1DeadPendingThreads above).
    // Queue the TLD instead; the GC drains it at the start of the next mark.
    // Caller (markDeadThread) holds the critical section that guards the list.
    current->gcQueuedForDrain = JAVA_TRUE;
    current->gcDeadNext = cn1DeadPendingThreads;
    cn1DeadPendingThreads = current;
}

// Drain the dead-thread queue on the GC thread at mark start: migrate each queued
// TLD's pending allocations into allObjectsInHeap (the only place besides the
// live-thread mark migration where the table may grow) and perform any TLD free
// that the Thread finalizer requested while the TLD was still queued.
static void cn1DrainDeadThreadPending() {
    lockCriticalSection();
    struct ThreadLocalData* head = cn1DeadPendingThreads;
    cn1DeadPendingThreads = 0;
    while(head != 0) {
        struct ThreadLocalData* next = head->gcDeadNext;
        for(int heapTrav = 0 ; heapTrav < head->heapAllocationSize ; heapTrav++) {
            JAVA_OBJECT obj = (JAVA_OBJECT)head->pendingHeapAllocations[heapTrav];
            if(obj) {
                head->pendingHeapAllocations[heapTrav] = 0;
                placeObjectInHeapCollection(obj);
            }
        }
        head->heapAllocationSize = 0;
        head->gcDeadNext = 0;
        head->gcQueuedForDrain = JAVA_FALSE;
        if(head->gcReleaseRequested) {
            // the Thread object was finalized while this TLD awaited the drain
            cn1ReleaseThreadLocalData(head);
        }
        head = next;
    }
    unlockCriticalSection();
}
static void gcMarkDrain(CODENAME_ONE_THREAD_STATE);
// Parallel variant of gcMarkDrain: fans the transitive mark-drain out across a small
// pool of worker threads. Falls back to the serial gcMarkDrain when only one marker
// is configured. Defined further down (after gcMarkDrain). See the big comment block
// at the worklist declarations for the design and the invariants it preserves.
static void gcMarkDrainParallel(CODENAME_ONE_THREAD_STATE);

#ifdef CN1_CONSERVATIVE_GC_ROOTS
// PHASE 3b forward declarations (definitions live after the BiBOP block because the
// resolver reuses the BiBOP page structures). These make the conservative native-stack
// scan a REAL root source for object-bearing FRAMELESS frames. See the big block below.
static void cn1GcScanThreadNativeStack(CODENAME_ONE_THREAD_STATE, struct ThreadLocalData* t);
static void cn1GcScanOwnStack(CODENAME_ONE_THREAD_STATE);
static void cn1GcSignalStopThreads(struct ThreadLocalData* self);
static void cn1GcSignalReleaseThreads(struct ThreadLocalData* self);
void cn1GcBuildRootSnapshots(void);
JAVA_OBJECT cn1ConservativeResolve(void* w);
#ifdef CN1_CONSERVATIVE_GC_SELFCHECK
// Transient ⊇ self-check (NOT in the shipping path): asserts every precise object
// root on a paused thread's object stack is also resolved by the conservative scan.
static void cn1GcSelfCheckThreadStack(struct ThreadLocalData* t, int stackSize);
#endif
#endif

/**
 * A simple concurrent mark algorithm that traverses the currently running threads
 */
extern int recursionKey; // force-mark pass epoch (defined below, near gcMarkObject)

// ---- SATB (snapshot-at-the-beginning) deletion-barrier log -------------------
// gcSatbActive is set for the whole concurrent mark and read by CN1_SATB_DELETE on
// every heap object-reference store (all mutators, including native threads). The
// barrier pushes the OVERWRITTEN reference here; codenameOneGCMark drains it to a
// fixpoint before sweep, so a reference present at the start of the cycle is never
// lost to a concurrent move/null between a thread's scan and the end of mark.
volatile int gcSatbActive = 0;
static JAVA_OBJECT* gcSatbStack = 0;
static long gcSatbTop = 0;                 // guarded by gcSatbMutex
static long gcSatbCap = 0;
static pthread_mutex_t gcSatbMutex = PTHREAD_MUTEX_INITIALIZER;
// Monotonic count of objects transitioned unmarked->marked this process; the SATB
// drain snapshots it around a batch to detect "marked nothing new" (fixpoint).
long gcMarkNewObjectCount = 0;

// ---- Poor-man's generational adoption: mature surviving BiBOP subtrees into legacy ----
// Policy (compile-time A/B): TENURE(1) matures a reachable non-leaf BiBOP object once it
// has SURVIVED a prior cycle (its old mark was a positive prior-cycle value), plus a
// CASCADE so the whole reachable subtree matures together (never half a tree). ONMARK(2)
// matures every reachable non-leaf BiBOP object immediately. 0 disables adoption.
#ifndef CN1_ADOPT_POLICY
#define CN1_ADOPT_POLICY 1
#endif
#if CN1_ADOPT_POLICY != 0 && !defined(CN1_DISABLE_BIBOP)
// Set by gcMarkDrain while running a MATURED object's mark function, so the children it
// marks are matured too -- this is the cascade that keeps the whole subtree in one system.
// THREAD-LOCAL: each parallel mark worker cascades within the subtree it is draining
// without racing the others on this flag.
static __thread JAVA_BOOLEAN gcCurrentlyMaturing = JAVA_FALSE;
#endif
// Forward (tentative) declaration -- the real definition is below near the worklist;
// the belt pass in codenameOneGCMark forces it to trigger the full BiBOP rescan.
static JAVA_BOOLEAN gcMarkWorklistOverflow;
#ifndef CN1_DISABLE_BIBOP
// Forward declarations -- defined below; the grace-subtree pass in codenameOneGCMark
// walks the page registry and its slots before their definitions.
static inline JAVA_OBJECT cn1BibopSlot(CN1BibopPage* p, int i);
static CN1BibopPage* _Atomic bibopAllPages;
#endif
#ifdef CN1_BIBOP_VALIDATE
// Belt diagnostic: while set, gcMarkObject logs the class of each newly-marked object
// (a reachable object the main drain missed) and its drain parent, to name the
// systematic drain-incompleteness pattern. Throttled by gcBeltDiagCount.
static int gcBeltDiagActive = 0;
static int gcBeltDiagCount = 0;
#endif

void cn1SatbEnqueue(JAVA_OBJECT old) {
    pthread_mutex_lock(&gcSatbMutex);
    if(gcSatbTop >= gcSatbCap) {
        long ncap = gcSatbCap ? gcSatbCap * 2 : 8192;
        JAVA_OBJECT* n = (JAVA_OBJECT*)realloc(gcSatbStack, (size_t)ncap * sizeof(JAVA_OBJECT));
        if(n == 0) { pthread_mutex_unlock(&gcSatbMutex); return; } // OOM: drop (rare; only re-opens the original race)
        gcSatbStack = n; gcSatbCap = ncap;
    }
    gcSatbStack[gcSatbTop++] = old;
    pthread_mutex_unlock(&gcSatbMutex);
}

// Atomically take the current SATB batch (swap the log empty) into *out (caller owns
// the returned buffer contents until the next take). Returns the count taken.
static long cn1SatbTake(JAVA_OBJECT** out) {
    pthread_mutex_lock(&gcSatbMutex);
    long n = gcSatbTop;
    static JAVA_OBJECT* scratch = 0; static long scratchCap = 0;
    if(n > scratchCap) {
        long nc = n < 8192 ? 8192 : n;
        scratch = (JAVA_OBJECT*)realloc(scratch, (size_t)nc * sizeof(JAVA_OBJECT));
        scratchCap = nc;
    }
    if(n > 0 && scratch != 0) memcpy(scratch, gcSatbStack, (size_t)n * sizeof(JAVA_OBJECT));
    gcSatbTop = 0;
    pthread_mutex_unlock(&gcSatbMutex);
    *out = scratch;
    return (scratch != 0) ? n : 0;
}

void cn1RefreshFreeMemCache(void);   // defined near cn1BibopMaybeGc; drives the dynamic pacing cap

void codenameOneGCMark() {
    currentGcMarkValue++;
    cn1RefreshFreeMemCache();   // snapshot free RAM once per cycle for the dynamic pacing cap
    // Bump the force-mark pass epoch so the force-visited side table's prior-cycle entries
    // read as not-visited (relocated from the old per-object __codenameOneReferenceCount).
    recursionKey++;
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    // PHASE 3b: ensure the universal thread-stop signal handler is installed (idempotent,
    // first GC only). Used to stop+scan threads we cannot cooperatively park.
    cn1GcInstallSignalHandler();
#endif
    init_gc_thresholds();
    hasAgressiveAllocator = JAVA_FALSE;
    // Arm the SATB deletion barrier for the whole mark (drained to a fixpoint + cleared
    // before sweep, below). Released mutators and native threads take the barrier on any
    // heap ref store, preserving snapshot-time references they concurrently overwrite.
    // The release fence orders this ahead of any thread being unblocked (963).
#if !defined(CN1_DISABLE_SATB)
    __atomic_store_n(&gcSatbActive, 1, __ATOMIC_RELEASE);
#endif
    struct ThreadLocalData* d = getThreadLocalData();
    //int marked = 0;
    
    // copy the allocated objects from already deleted threads so we can delete that data
    #if defined(__OBJC__)
    //NSLog(@"GC mark, %d dead processes pending",nThreadsToKill);
    #endif

    // Migrate dead threads' pending allocations into allObjectsInHeap NOW, on the
    // GC thread, before any table walk of this cycle (root snapshots, sweep) --
    // the single place besides the live-thread migration below where the table
    // may grow. See the cn1DeadPendingThreads single-writer comment.
    cn1DrainDeadThreadPending();

    // Immortal roots: VM-internal objects held only by C globals (see
    // cn1AddImmortalRoot). Marked as roots every cycle; the per-thread drains
    // below trace them transitively.
    lockCriticalSection();
    for(int ir = 0 ; ir < cn1ImmortalRootsN ; ir++) {
        gcMarkObject(d, cn1ImmortalRoots[ir], JAVA_FALSE);
    }
    unlockCriticalSection();

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

#ifdef CN1_CONSERVATIVE_GC_ROOTS
                // PHASE 3b: demand a FRESH native-stack capture this round. Only a
                // thread that actually parks at a safepoint (CN1_GC_PARK_CAPTURE)
                // re-raises this; a stale capture from a previous cycle is never
                // reused (the scanner falls back to a signal-stop instead).
                // This must run for EVERY thread -- a NATIVE (non-lightweight)
                // thread can also park once (lowMemoryMode / max-heap backpressure)
                // and would otherwise satisfy useCoop with that stale SP forever,
                // silently skipping the live region below it (missed roots -> UAF).
                t->gcParkCaptured = JAVA_FALSE;
#endif
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
#ifdef CN1_CONSERVATIVE_GC_ROOTS
                // Refresh the page/extent snapshot for the VALIDATED precise scan
                // below (also rebuilt in cn1GcScanThreadNativeStack before any
                // signal-stop; building here first only makes it fresher).
                cn1GcBuildRootSnapshots();
#endif
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
#ifdef CN1_CONSERVATIVE_GC_ROOTS
                        // VALIDATED precise scan: a SIGNAL-STOPPED thread can be frozen
                        // between the type and data stores of a push -- and since the
                        // elementStruct fields are plain (non-volatile) stores, clang may
                        // also reorder them -- so a type==OBJECT slot can transiently hold
                        // a stale primitive (observed: gcMarkObject(0x4e20) from a frozen
                        // PUSH_INT window). Resolve the word against the page/extent
                        // snapshot exactly like a conservative root: garbage never reaches
                        // gcMarkObject; every live PUBLISHED object resolves to itself
                        // (objects in pages/extents newer than the snapshot are mark==-1
                        // fresh and survive via the sweep's grace rule; immortal class
                        // objects are skipped by gcMarkObject anyway). Liveness of a value
                        // hidden by a torn window is covered by the conservative native
                        // stack + register scan -- the value is still in a C temp.
                        JAVA_OBJECT resolved = cn1ConservativeResolve((void*)current->data.o);
                        if(resolved != JAVA_NULL) {
                            gcMarkObject(t, resolved, JAVA_FALSE);
                        }
#else
                        gcMarkObject(t, current->data.o, JAVA_FALSE);
#endif
                        //marked++;
                    }
                }
#ifdef CN1_CONSERVATIVE_GC_ROOTS
                // PHASE 3b HYBRID GC: in ADDITION to the precise threadObjectStack scan
                // above (which still covers legacy frames), conservatively scan this
                // thread's native C stack [sp, base) + its register snapshot and MARK
                // every resolved live object. This is the ONLY root source for object-
                // bearing FRAMELESS frames, whose object refs live in native C locals /
                // the method-local operand array rather than threadObjectStack. A given
                // object is reachable from whichever frame holds it, so the boundary
                // between a legacy caller and a frameless callee (or vice versa) is
                // covered: the conservative scan walks the WHOLE native stack regardless.
                cn1GcScanThreadNativeStack(d, t);
#ifdef CN1_CONSERVATIVE_GC_SELFCHECK
                cn1GcSelfCheckThreadStack(t, stackSize);
#endif
#endif
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

#ifdef CN1_CONSERVATIVE_GC_ROOTS
    // PHASE 3b: scan the GC thread's OWN native stack last -- a root could be live only
    // in a GC-thread C local. Marks for real; the drain below propagates it.
    cn1GcScanOwnStack(d);
#endif

    // Drain the worklist that the calls above populated. gcMarkObject no longer recurses
    // through reference fields, so we need an explicit drain pass before sweep runs.
    gcMarkDrain(d);

    // NOTE: the SATB log is drained + gcSatbActive cleared AFTER the grace pass and belt
    // below, so the insertion/deletion barriers stay armed through them -- a mutator that
    // links an object into a fresh grace object DURING those phases still gets it logged
    // and marked, closing the residual window.

    // Belt pass -- guaranteed drain completeness before sweep. gcMarkDrain triggers the
    // BiBOP page rescan only on a worklist OVERFLOW; if a marked object ever had its mark
    // function skipped, its reachable children go untraversed and are swept while live --
    // the intermittent Linux crash (a marked Component.BGPainter whose owning Component,
    // reached only through this$0, was freed). Force one full rescan + drain to a fixpoint
    // unconditionally so EVERY marked object's mark function runs and all reachable children
    // are marked. gcMarkDrain re-pushes each marked slot and loops until a pass marks nothing
    // new -> O(reachable) and idempotent; recovers any marked-but-untraversed subtree.
#ifndef CN1_DISABLE_BIBOP
    // Grace-subtree marking (CORRECTNESS): a fresh BiBOP object (gcMark==-1) survives this
    // cycle via grace, and the sweep promotes it to live (gcMark=V, cn1BibopSweep) or pools
    // its whole grace page WITHOUT draining it -- so an OLD object reachable ONLY through a
    // fresh, not-yet-linked object is left unmarked and swept. When a mutator later links
    // that fresh object into the live graph, next cycle it is drained and marks the now
    // dangling child -> the intermittent Property->Double / container->content crash. Drain
    // every grace object here so a surviving grace object's subtree survives WITH it.
    // parentCls==0 skips a mid-construction memset-elided slot (its class isn't published
    // yet); such an object is reached again next cycle once fully built.
    {
        CN1BibopPage* gp = atomic_load_explicit(&bibopAllPages, memory_order_acquire);
        while(gp != 0) {
            int gn = atomic_load_explicit(&gp->bumpIndex, memory_order_acquire);
            for(int gi = 0 ; gi < gn ; gi++) {
                JAVA_OBJECT go = cn1BibopSlot(gp, gi);
                if(__atomic_load_n(&go->__codenameOneGcMark, __ATOMIC_ACQUIRE) == -1
                   && go->__codenameOneParentClsReference != 0) {
                    gcMarkObject(d, go, JAVA_FALSE);
                }
            }
            gp = atomic_load_explicit(&gp->nextAll, memory_order_acquire);
        }
        gcMarkDrain(d);
    }
#endif

    {
        long __beltBefore = gcMarkNewObjectCount;
#ifdef CN1_BIBOP_VALIDATE
        gcBeltDiagActive = 1;
#endif
        // One forced full rescan+drain, recovering marked-but-undrained subtrees before
        // sweep. NOTE: must NOT loop to convergence -- mutators are still active during
        // this phase, so a "mark nothing new" fixpoint can livelock against ongoing
        // allocation (observed hanging/breaking FusedTest). A single pass is bounded and
        // safe; residual incompleteness is handled by the drain-gap fix, not by looping.
        gcMarkWorklistOverflow = JAVA_TRUE;   // force the BiBOP page-rescan path on
        gcMarkDrain(d);
#ifdef CN1_BIBOP_VALIDATE
        gcBeltDiagActive = 0;
        if(gcMarkNewObjectCount != __beltBefore) {
            fprintf(stderr, "CN1BIBOP DRAIN INCOMPLETE: belt recovered %ld "
                    "reachable-but-unmarked object(s) before sweep\n",
                    gcMarkNewObjectCount - __beltBefore);
            fflush(stderr);
        }
#endif
    }

    // SATB termination (LAST, after grace+belt): mark everything the deletion+insertion
    // barriers logged during the WHOLE mark (including the grace pass and belt above), to
    // a fixpoint -- gcMarkObject is idempotent, so once a take-and-drain marks nothing new
    // the start-of-cycle snapshot is closed. Draining it here (not before grace+belt) is
    // what keeps the barrier armed through those phases and closes the residual grace
    // window. Bounded by the live set (only genuinely-new marks reset the fixpoint).
    for(;;) {
        JAVA_OBJECT* batch;
        long n = cn1SatbTake(&batch);
        if(n == 0) break;                    // log empty at this instant
        long before = gcMarkNewObjectCount;
        for(long i = 0 ; i < n ; i++) {
            gcMarkObject(d, batch[i], JAVA_FALSE);
        }
        gcMarkDrain(d);
        if(gcMarkNewObjectCount == before) break; // processed a batch, marked nothing new -> closed
    }
    // Snapshot closed; stop logging. A store racing this clear either logged already
    // (drained just below) or overwrites/adds an already-marked reference (harmless).
    __atomic_store_n(&gcSatbActive, 0, __ATOMIC_RELEASE);
    {
        JAVA_OBJECT* batch;
        long n = cn1SatbTake(&batch);        // final catch of anything logged during the tail
        for(long i = 0 ; i < n ; i++) {
            gcMarkObject(d, batch[i], JAVA_FALSE);
        }
        if(n > 0) gcMarkDrain(d);
    }
#if CN1_ADOPT_POLICY != 0 && !defined(CN1_DISABLE_BIBOP)
    // Marking (incl. grace, belt and SATB) is fully done. Register the objects matured
    // this cycle into allObjectsInHeap now -- single-threaded, locked, before the sweep.
    cn1DrainAdoptBuffer();
#endif
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
#ifndef CN1_DISABLE_BIBOP
                    // A MATURED (-4) object that died: its memory is a BiBOP slot, so its
                    // DEATH belongs entirely to the BiBOP collector. The slot is already
                    // deregistered (nulled above); revert it to a normal dead -3 slot and
                    // let the next BiBOP sweep run cn1BibopReclaimSlot ONCE (finalizer +
                    // native peer + monitor). Do NOT freeAndFinalize here: that runs the
                    // finalizer a SECOND time (cn1BibopReclaimSlot runs it too), which
                    // double-frees a native-resource finalizer's buffer -- the deterministic
                    // mid-suite "corrupted unsorted chunks" heap abort.
                    if(o->__heapPosition == CN1_BIBOP_ADOPTED) {
                        o->__heapPosition = CN1_BIBOP_HEAP_POS;
                        continue;
                    }
#endif
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
#ifdef CN1_RESOLVE_DIAG
    { extern void cn1ResolveDiagReport(void); cn1ResolveDiagReport(); }
#endif
}

JAVA_BOOLEAN removeObjectFromHeapCollection(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    // Initialize allObjectsInHeap if it hasn't been initialized yet
    // This can happen if GC runs before any objects are allocated
    if(allObjectsInHeap == 0) {
        allObjectsInHeap = malloc(sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
        memset(allObjectsInHeap, 0, sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
    }

    // BiBOP-resident object: there is no table entry to remove -- the page sweep
    // frees it regardless, so the caller's intent ("make this immortal", used by
    // the generated static-final removal and VM-internal caches) would silently
    // do NOTHING and the object would be swept while a static/C global still
    // points at it (observed: java.lang.System.LOCK freed under the GC thread's
    // own wait()). Deliver the intended semantics: register it as a permanent
    // GC root instead.
    // ONLY a plain BiBOP slot (-3) gets the make-immortal shortcut: dead -3 objects never
    // reach this function (the legacy sweep only walks allObjectsInHeap, which they are not
    // in), so a -3 here can only be a caller asking to pin a live object as a root.
    // A MATURED (-4) object must NOT take this branch: the legacy sweep DOES call this on
    // dead -4 objects to remove them before freeing, and pinning a dead object as an
    // immortal root while freeAndFinalize frees it corrupts the heap. -4 falls through to
    // findPointerPosInHeap removal, which is correct for both the sweep and the (never
    // observed in practice) make-immortal-of-an-already-matured-object case.
    if(o != JAVA_NULL && !CN1_IS_TAGGED(o) && o->__heapPosition == CN1_BIBOP_HEAP_POS) {
        cn1AddImmortalRoot(o);
        return JAVA_TRUE;
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
// BYTES since the last cycle -- MUST be 64-bit: an allocation-churn workload
// moves multiple GB between cycles, and the old int accumulator wrapped
// NEGATIVE, so isHighFrequencyGC returned false and the GC thread slept its
// full 30s idle wait while dead pages ballooned into the GB range.
long long allocationsSinceLastGC = 0;
long long totalAllocations = 0;
long long cn1_instr_allocCount = 0;

JAVA_BOOLEAN java_lang_System_isHighFrequencyGC___R_boolean(CODENAME_ONE_THREAD_STATE) {
    long long alloc = allocationsSinceLastGC;
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
// CN1_BIBOP_HEAP_POS is defined in cn1_globals.h (shared with the inlined
// bump fast path); keep the .m self-consistent if the header changes.
#ifndef CN1_BIBOP_HEAP_POS
#define CN1_BIBOP_HEAP_POS   (-3)
#endif

// Size classes (slot sizes, 16-aligned). size <= CN1_BIBOP_MAX_OBJECT maps to
// the smallest class >= size; everything else takes the legacy path.
// CN1_BIBOP_NUM_CLASSES is fixed in cn1_globals.h (must equal this array length).
static const int cn1BibopClassSize[] = {
    32, 48, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, 448, 512
};
_Static_assert(sizeof(cn1BibopClassSize)/sizeof(int) == CN1_BIBOP_NUM_CLASSES,
               "cn1BibopClassSize length must match CN1_BIBOP_NUM_CLASSES in cn1_globals.h");
static signed char cn1BibopSizeToClass[CN1_BIBOP_MAX_OBJECT + 1];

// struct CN1BibopPage is defined in cn1_globals.h (shared with the inlined bump).

static CN1BibopPage* _Atomic bibopAllPages = 0;   // registry head (atomic)
static _Atomic long long bibopAllPagesCount = 0;  // grow-only registration count
static CN1BibopPage* bibopFreePool = 0;           // bibopMutex
static CN1BibopPage* bibopPartialPool[CN1_BIBOP_NUM_CLASSES]; // bibopMutex
static CN1BibopPage* _Atomic bibopSweepStack = 0; // Treiber-ish (push CAS / swap)
static pthread_mutex_t bibopMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_once_t  bibopOnce  = PTHREAD_ONCE_INIT;
// Non-static: also read/written by the inlined bump fast path (cn1_globals.h).
_Atomic long bibopBytesSinceGc = 0;

// (The old global BiBOP-monitor count that suppressed the O(1) all-dead reclaim
// for EVERY page while ANY monitor existed is gone: java.lang.System.LOCK is a
// permanently-monitored BiBOP object, so the gate degraded every sweep to the
// full per-slot walk -- measured 3-4x on allocation churn. Replaced by the
// STICKY per-page gcHasMonitors flag: the visibility concern the global count
// sidestepped (attach by a foreign thread with no happens-before to the page's
// retire-push) is covered by the mark handshake -- a page can only be PROVEN all-dead
// after a full mark in which the attaching thread was stopped and scanned,
// which orders its attach store before the sweep's read.)

// Per-thread current page per size class. Only ever touched by the owning
// thread (allocation) and by that same thread on death (collectThreadResources
// runs on the dying thread), so __thread is correct and the GC never needs to
// reach into it -- retired pages are handed to the GC via the global stack.
// Non-static: the inlined bump fast path (cn1_globals.h) reads bibopCurrent[ci].
__thread CN1BibopPage* bibopCurrent[CN1_BIBOP_NUM_CLASSES];

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
#ifndef CN1_BIBOP_NO_FASTSWEEP
    p->gcAllocedSinceSweep = JAVA_FALSE;
    p->gcNeedsReclaim = JAVA_FALSE;
    p->gcHasMonitors = JAVA_FALSE;
    p->gcHasAdopted = JAVA_FALSE;
    atomic_store_explicit(&p->gcLastMarkedEpoch, 0, memory_order_relaxed);
    p->gcGraceEpoch = 0;
#endif
}

// Raw 64KB page memory comes from large arenas -- one posix_memalign per
// CN1_BIBOP_ARENA_PAGES pages -- instead of one syscall per page. Under
// allocation churn the free pool drains faster than the concurrent sweep refills
// it, so cn1BibopNewPage was hitting posix_memalign -> mach_vm_map (a kernel
// trap) per 64KB page (~17% of an alloc-heavy benchmark, profiled). A 64KB-
// aligned arena yields 64KB-aligned pages (the mask-to-page resolve is
// unaffected), the arena is lazily faulted (RSS tracks touched pages, not the
// reservation), and BiBOP never free()s a page (swept pages are pooled), so the
// interior arena pointers are never individually released. Disable for A/B with
// -DCN1_BIBOP_NO_ARENA.
#ifndef CN1_BIBOP_ARENA_PAGES
#define CN1_BIBOP_ARENA_PAGES 64   /* 64 * 64KB = 4MB reserved per kernel mmap */
#endif
static char* bibopArenaBase = 0;
static size_t bibopArenaUsed = 0;
static size_t bibopArenaCap = 0;
static pthread_mutex_t bibopArenaMutex = PTHREAD_MUTEX_INITIALIZER;

static void* cn1BibopRawPage(void) {
#ifdef CN1_BIBOP_NO_ARENA
    void* mem = 0;
    if(posix_memalign(&mem, CN1_BIBOP_PAGE_SIZE, CN1_BIBOP_PAGE_SIZE) != 0) return 0;
    return mem;
#else
    pthread_mutex_lock(&bibopArenaMutex);
    if(bibopArenaBase == 0 || bibopArenaUsed + CN1_BIBOP_PAGE_SIZE > bibopArenaCap) {
        size_t sz = (size_t)CN1_BIBOP_PAGE_SIZE * CN1_BIBOP_ARENA_PAGES;
        void* mem = 0;
        if(posix_memalign(&mem, CN1_BIBOP_PAGE_SIZE, sz) != 0 || mem == 0) {
            pthread_mutex_unlock(&bibopArenaMutex);
            return 0;
        }
        bibopArenaBase = (char*)mem;
        bibopArenaUsed = 0;
        bibopArenaCap = sz;
    }
    void* p = bibopArenaBase + bibopArenaUsed;
    bibopArenaUsed += CN1_BIBOP_PAGE_SIZE;
    pthread_mutex_unlock(&bibopArenaMutex);
    return p;
#endif
}

static CN1BibopPage* cn1BibopNewPage(int ci) {
    void* mem = cn1BibopRawPage();
    if(mem == 0) {
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
    // grow-only registration count: the snapshot builder keys its cached
    // base-sorted page array off this (nodes never unlink or reorder)
    atomic_fetch_add_explicit(&bibopAllPagesCount, 1, memory_order_release);
    return p;
}

static inline JAVA_OBJECT cn1BibopSlot(CN1BibopPage* p, int i) {
    return (JAVA_OBJECT)((char*)p + p->firstSlotOffset + (long)i * p->slotSize);
}

// Trigger a full GC if BiBOP allocation volume since the last collection has
// crossed the threshold (these objects don't feed the legacy heapAllocationSize
// trigger). Mirrors codenameOneGcMalloc's simple self-triggering branch.
// Adaptive-backpressure ceiling. The old design paced every mutator a fixed
// Thread.sleep(2) on each GC trigger (in System.gc()), which is pure stall when
// the concurrent collector is keeping up -- on allocate-and-drop churn that was
// the dominant cost (objectAllocation 54ms -> 18ms once removed). But removing it
// unconditionally let the mutator outrun the collector and balloon RSS to ~2GB.
// Instead, pace PROPORTIONALLY: only when uncollected BiBOP volume since the last
// GC exceeds this hard cap does the mutator wait for the collector to catch up,
// bounding RSS. When the collector keeps up (bytes stays near the trigger) this
// never waits. Disable with -DCN1_BIBOP_NO_PACING for A/B.
#ifndef CN1_BIBOP_GC_HARD_CAP
#define CN1_BIBOP_GC_HARD_CAP (CN1_BIBOP_GC_TRIGGER_BYTES * 3)
#endif
// A thread with more than this many legacy allocations since the last GC (heapAllocationSize,
// reset each cycle) is treated as high-throughput and gets the deeper pacing headroom below.
#ifndef CN1_BIBOP_HIGH_THROUGHPUT_ALLOCS
#define CN1_BIBOP_HIGH_THROUGHPUT_ALLOCS 50000
#endif

// Cached free-memory reading, refreshed once per GC cycle (cn1RefreshFreeMemCache, called from
// codenameOneGCMark) so the dynamic pacing cap costs no per-page-acquire syscall.
_Atomic long cn1CachedFreeMem = 0;
void cn1RefreshFreeMemCache(void) {
    atomic_store_explicit(&cn1CachedFreeMem, cn1_available_memory(), memory_order_relaxed);
}

// DYNAMIC PACING CAP (perf-tier1). The fixed 3x-trigger cap starves a high-throughput allocator:
// a thread that allocates faster than the collector (e.g. the EDT decoding/parsing a heavy
// vector-map tile) is parked in cn1BibopMaybeGc's backpressure spin for most of each GC cycle,
// so the render is serialized behind the collector instead of overlapping it (~20% of wall on
// the MvtBench repro, larger on device). Instead of a constant, allow the thread to run further
// ahead of the collector when free RAM is ample (they overlap -> the fast thread is not starved)
// and tighten toward the static cap as free memory shrinks (RSS stays bounded). The EDT -- the
// thread we most want to keep responsive -- gets double headroom; it must never be throttled
// unless memory is genuinely tight. Never returns LESS than the old static cap, so no workload
// gets a tighter bound than before. -DCN1_BIBOP_NO_PACING still disables pacing entirely for A/B.
static long cn1BibopPacingCap(CODENAME_ONE_THREAD_STATE) {
    long base = (long)CN1_BIBOP_GC_HARD_CAP;                 // old static cap (3x trigger)
    long fm = atomic_load_explicit(&cn1CachedFreeMem, memory_order_relaxed);
    long cap = fm / 8;                                       // baseline: 1/8 of available RAM of slack
    if(cap < base) cap = base;                              // never tighter than before
    // High-throughput threads must not be starved by the collector. The EDT (UI/render thread)
    // always qualifies; so does any thread allocating hard RIGHT NOW (heapAllocationSize is its
    // legacy allocations since the last GC, reset each cycle) -- e.g. a worker decoding a heavy
    // vector-map tile. Give them up to 1/2 of AVAILABLE RAM of headroom so they keep running
    // while the concurrent GC catches up, instead of parking in the backpressure spin. Still
    // bounded by real available memory (get_free_memory now reports reclaimable pages), so RSS
    // stays safe and the collector reclaims the transient churn.
    if(isEdt(threadStateData->threadId)
       || threadStateData->heapAllocationSize > CN1_BIBOP_HIGH_THROUGHPUT_ALLOCS) {
        long hi = fm / 2;
        if(hi > cap) cap = hi;
    }
    return cap;
}

static void cn1BibopMaybeGc(CODENAME_ONE_THREAD_STATE) {
    // LEVER A: flush this thread's plain-add byte accumulator into the global atomic
    // (once per page-acquire). No-op unless -DCN1_DEATOMIC_BYTES.
    CN1_BIBOP_FLUSH_BYTES(threadStateData);
    if(constantPoolObjects == 0) {
        return;
    }
#ifndef CN1_CONSERVATIVE_GC_ROOTS
    // Without conservative roots, a thread inside a native-allocation bracket
    // must be neither parked nor made to trigger a cycle (its half-built
    // objects aren't rooted). Under conservative roots the native C locals ARE
    // scanned, so both are safe -- and skipping here was starving the 24MB
    // trigger entirely on workloads whose every allocation happens inside a
    // native (e.g. StringBuilder.toString): the GC thread then slept its idle
    // wait while dead pages accumulated into the GB range.
    if(threadStateData->nativeAllocationMode) {
        return;
    }
#endif
    // GC SAFEPOINT (once per page-acquire): a thread allocating ONLY small BiBOP
    // objects never reaches the legacy alloc path's pending-buffer park, so without
    // this check the collector's while(threadActive) wait is satisfied only by
    // monitor/sleep/native yields -- a tight allocation loop could stall the GC's
    // root scan for a long time. Same idiom as the legacy park: capture the native
    // stack for the cooperative conservative scan, mark inactive, wait out the GC.
    if(threadStateData->threadBlockedByGC) {
        CN1_GC_PARK_CAPTURE(threadStateData);
        threadStateData->threadActive = JAVA_FALSE;
        while(threadStateData->threadBlockedByGC) {
            usleep((JAVA_INT)(500));
        }
        threadStateData->threadActive = JAVA_TRUE;
    }
    if(!gcCurrentlyRunning &&
       atomic_load_explicit(&bibopBytesSinceGc, memory_order_relaxed) > CN1_BIBOP_GC_TRIGGER_BYTES) {
        // save/restore: we may already be INSIDE a caller's native-allocation
        // bracket (reachable here under CN1_CONSERVATIVE_GC_ROOTS)
        JAVA_BOOLEAN wasNam = threadStateData->nativeAllocationMode;
        threadStateData->nativeAllocationMode = JAVA_TRUE;
        java_lang_System_gc__(threadStateData);
        threadStateData->nativeAllocationMode = wasNam;
    }
#ifndef CN1_BIBOP_NO_PACING
    // Backpressure: bound RSS when the collector falls behind. This wait MUST be a
    // GC safepoint -- otherwise the collector blocks waiting for this spinning
    // thread to become scannable and never advances, so bibopBytesSinceGc never
    // resets and the spin livelocks (observed as an MtStress hang). Mark the thread
    // inactive (as the legacy alloc-path park does) so the collector can scan/pass
    // it; restore on exit. Bounded spin with a safety cap so a dead/stuck GC
    // degrades to RSS growth, never a permanent hang.
    long __pacingCap = cn1BibopPacingCap(threadStateData);
    if(atomic_load_explicit(&bibopBytesSinceGc, memory_order_relaxed) > __pacingCap &&
       get_static_java_lang_System_gcThreadInstance() != JAVA_NULL) {
        CN1_GC_PARK_CAPTURE(threadStateData);   // fresh capture for the coop conservative scan
        threadStateData->threadActive = JAVA_FALSE;
        int spins = 0;
        while(atomic_load_explicit(&bibopBytesSinceGc, memory_order_relaxed) > __pacingCap &&
              get_static_java_lang_System_gcThreadInstance() != JAVA_NULL &&
              spins++ < 200000) {
            usleep(50);
        }
        // The spin can exit via the safety cap while a mark is STILL RUNNING and the
        // collector believes this thread is paused (it already scanned our roots).
        // Waking mid-drain violates snapshot-at-the-beginning: we could load a grey
        // object's field into a local and null the field -- the referent would never
        // be marked and gets swept while reachable. Honor the GC pause before
        // resuming, exactly like every other park in the codebase.
        while(threadStateData->threadBlockedByGC) {
            usleep((JAVA_INT)(500));
        }
        threadStateData->threadActive = JAVA_TRUE;
    }
#endif
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
    // __codenameOneReferenceCount + __codenameOneThreadData relocated out of the header
    // (force-visited / monitor side tables); no per-object stores.
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
#ifndef CN1_BIBOP_NO_FASTSWEEP
                p->gcAllocedSinceSweep = JAVA_TRUE;
#endif
                CN1_BIBOP_ACCOUNT_BYTES(threadStateData, p->slotSize);
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
#ifndef CN1_BIBOP_NO_FASTSWEEP
    p->gcAllocedSinceSweep = JAVA_TRUE;
#endif
    CN1_BIBOP_ACCOUNT_BYTES(threadStateData, p->slotSize);
    return o;
}

// ---- Monitor side table (relocated __codenameOneThreadData out of the object header) ----
// The lazily-attached per-object monitor (CN1ThreadData*) is NULL on virtually every
// object, so storing it in every header wasted 8 bytes/object. It now lives in an
// address-keyed chained hash map; only objects that are actually monitorEnter'd ever
// get an entry. All ops take a single dedicated mutex (monitor ops are rare relative to
// allocation). Lock discipline: callers NEVER hold this mutex across lockCriticalSection
// or across a blocking pthread_mutex_lock(data->mutex) -- the data pointer is copied out
// and the table mutex released first -- so there is no inversion with the GC critical
// section (which only ever takes the table mutex AFTER it, during reclaim/free).
struct CN1MonitorEntry { JAVA_OBJECT key; void* data; struct CN1MonitorEntry* next; };
#define CN1_MON_BUCKETS 4096
static struct CN1MonitorEntry* cn1MonitorBuckets[CN1_MON_BUCKETS];
static pthread_mutex_t cn1MonitorTableMutex = PTHREAD_MUTEX_INITIALIZER;

static inline unsigned cn1MonHash(JAVA_OBJECT o) {
    uintptr_t p = (uintptr_t)o;
    p >>= 4; // objects are at least 16-byte aligned
    return (unsigned)((p ^ (p >> 16)) & (CN1_MON_BUCKETS - 1));
}

// Lookup: returns the attached CN1ThreadData* (or 0). Safe for concurrent callers.
void* cn1MonitorDataGet(JAVA_OBJECT o) {
    unsigned h = cn1MonHash(o);
    pthread_mutex_lock(&cn1MonitorTableMutex);
    struct CN1MonitorEntry* e = cn1MonitorBuckets[h];
    void* r = 0;
    while(e) { if(e->key == o) { r = e->data; break; } e = e->next; }
    pthread_mutex_unlock(&cn1MonitorTableMutex);
    return r;
}

// Insert or overwrite the monitor for o.
void cn1MonitorDataSet(JAVA_OBJECT o, void* data) {
    unsigned h = cn1MonHash(o);
    pthread_mutex_lock(&cn1MonitorTableMutex);
    struct CN1MonitorEntry* e = cn1MonitorBuckets[h];
    while(e) { if(e->key == o) { e->data = data; pthread_mutex_unlock(&cn1MonitorTableMutex); return; } e = e->next; }
    e = (struct CN1MonitorEntry*)malloc(sizeof(struct CN1MonitorEntry));
    e->key = o; e->data = data; e->next = cn1MonitorBuckets[h];
    cn1MonitorBuckets[h] = e;
    pthread_mutex_unlock(&cn1MonitorTableMutex);
}

// Remove o's entry and return its data (or 0 if none). The caller frees the data.
void* cn1MonitorDataRemove(JAVA_OBJECT o) {
    unsigned h = cn1MonHash(o);
    pthread_mutex_lock(&cn1MonitorTableMutex);
    struct CN1MonitorEntry** pp = &cn1MonitorBuckets[h];
    void* r = 0;
    while(*pp) {
        if((*pp)->key == o) {
            struct CN1MonitorEntry* d = *pp;
            r = d->data; *pp = d->next; free(d);
            break;
        }
        pp = &(*pp)->next;
    }
    pthread_mutex_unlock(&cn1MonitorTableMutex);
    return r;
}

// Run finalizer + free monitor for a dead page slot (does NOT free() the slot;
// the slot is recycled into the page free-list by the caller). Mirrors
// freeAndFinalize / codenameOneGcFree minus the free().
static void cn1BibopReclaimSlot(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    finalizerFunctionPointer ptr = (finalizerFunctionPointer)o->__codenameOneParentClsReference->finalizerFunction;
    if(ptr != 0) {
        ptr(threadStateData, o);
    }
    cn1ReleaseStringPeer(o);
    void* md = cn1MonitorDataRemove(o);
    if(md) {
        free(md);
    }
}

#ifndef CN1_BIBOP_NO_FASTSWEEP
// A native peer (cached NSString) was attached to a BiBOP object: flag its
// page exactly like a monitor so the dead slot always reaches
// cn1BibopReclaimSlot (which releases the peer) instead of the O(1) all-dead
// page reclaim. Same sticky-flag visibility argument as monitors.
void cn1BibopNoteNativePeer(JAVA_OBJECT obj) {
    if(obj != JAVA_NULL && (obj->__heapPosition == CN1_BIBOP_HEAP_POS || obj->__heapPosition == CN1_BIBOP_ADOPTED)) {
        CN1BibopPage* p = (CN1BibopPage*)((uintptr_t)obj & ~((uintptr_t)(CN1_BIBOP_PAGE_SIZE - 1)));
        p->gcHasMonitors = JAVA_TRUE;
    }
}

void cn1BibopNoteMonitorAttached(JAVA_OBJECT obj) {
    if(obj != JAVA_NULL && (obj->__heapPosition == CN1_BIBOP_HEAP_POS || obj->__heapPosition == CN1_BIBOP_ADOPTED)) {
        // STICKY per-page flag (plain store): visible to any sweep that could
        // legitimately take the all-dead shortcut for this page, because that
        // requires a full mark completed AFTER this (live) object died, and the
        // mark's thread-stop handshake orders this store before the GC's reads.
        CN1BibopPage* p = (CN1BibopPage*)((uintptr_t)obj & ~((uintptr_t)(CN1_BIBOP_PAGE_SIZE - 1)));
        p->gcHasMonitors = JAVA_TRUE;
    }
}
#else
// When the O(live-pages) fast sweep is disabled there is no all-dead shortcut to
// suppress: every retired page is full-walked and every dead slot reaches
// cn1BibopReclaimSlot (which releases the native peer) regardless. The header
// declares cn1BibopNoteNativePeer unconditionally and toNSString() (Apple/ObjC
// builds) calls it unconditionally, so provide a no-op definition here to keep
// the symbol resolvable in the CN1_BIBOP_NO_FASTSWEEP configuration.
// (cn1BibopNoteMonitorAttached is declared and called only under !NO_FASTSWEEP,
// so it needs no counterpart here.)
void cn1BibopNoteNativePeer(JAVA_OBJECT obj) { (void)obj; }
#endif

// Sweep all retired pages. Runs on the GC thread AFTER mark completes; the
// pages it processes are off the SWEEP stack (owner==0), so no mutator is
// allocating into them and no marking is in flight -> plain header access.
static void cn1BibopSweep(CODENAME_ONE_THREAD_STATE) {
    CN1BibopPage* list = atomic_exchange_explicit(&bibopSweepStack, (CN1BibopPage*)0, memory_order_acquire);
    int V = currentGcMarkValue;  // stable during the sweep (mark done, not yet incremented)
#ifndef CN1_BIBOP_NO_FASTSWEEP
    // Snapshot once: if ANY BiBOP object currently carries a monitor, suppress the O(1)
    // all-dead shortcut this whole sweep so dead monitored slots are full-walked and
    // their monitor freed. Safe to cache: a homogeneous all-dead page's slots are
    // unreachable (not marked this cycle, aged >=2 cycles), so no mutator can hold a
    // reference to monitorEnter one during this sweep; any concurrent attach is to a live
    // object on some OTHER page and is observed by the next sweep.
#endif
    while(list != 0) {
        CN1BibopPage* page = list;
        list = page->nextPool;
#ifdef CN1_BIBOP_VALIDATE
        // INVARIANT: only RETIRED (non-owned) pages reach the sweep. If an OWNED
        // page (some thread's live bibopCurrent[ci]) is on the sweep stack, the
        // sweep will reset/recycle it out from under that thread -> the
        // intermittent cn1BibopFastAlloc crash. Catch it here, at the source.
        if(page->owned == JAVA_TRUE) {
            fprintf(stderr, "CN1BIBOP SWEEP OF OWNED PAGE: page=%p classIndex=%d bumpIndex=%d\n",
                    (void*)page, page->classIndex,
                    atomic_load_explicit(&page->bumpIndex, memory_order_relaxed));
            fflush(stderr);
            abort();
        }
#endif
#ifndef CN1_BIBOP_NO_FASTSWEEP
        // ---- O(1) page decision (no per-slot walk). -------------------------------
        // A page is HOMOGENEOUS when every occupied slot is a dead-or-graced object
        // sitting at a single (upper-bounded) epoch. That holds iff:
        //   * !gcAllocedSinceSweep  -> nothing was allocated into it since its last
        //       sweep, so it has NO fresh mark==-1 grace-candidate slots; and
        //   * gcLastMarkedEpoch != V -> nothing on it was marked THIS cycle, so it holds
        //       no live (reachable) object -- a reachable object is always marked, so an
        //       unmarked-this-cycle occupant is unreachable garbage in grace-aging; and
        //   * !gcNeedsReclaim       -> no survivor carries a finalizer (monitors handled
        //       by the page's sticky gcHasMonitors flag); and
        //   * freeList == 0         -> the page is full (defensive: a homogeneous page
        //       can only reach here full -- a partial page, once adopted, is always
        //       allocated into before re-retire, which sets gcAllocedSinceSweep).
        // gcGraceEpoch is the upper bound on survivor epochs as of the last full walk.
        if(page->gcAllocedSinceSweep == JAVA_FALSE &&
           page->gcNeedsReclaim == JAVA_FALSE &&
           page->gcHasAdopted == JAVA_FALSE &&
           page->freeList == 0 &&
           atomic_load_explicit(&page->gcLastMarkedEpoch, memory_order_relaxed) != V) {
            int graceEpoch = page->gcGraceEpoch;
            if(graceEpoch >= V - 1) {
                // STILL IN GRACE -> all occupants survive this cycle. The full walk would
                // grace/keep them and route the full all-live page to partialPool; do
                // exactly that without a walk. Leave gcGraceEpoch UNCHANGED so the page
                // ages and a later cycle re-evaluates it (-> all-dead) rather than pinning
                // the garbage forever.
                pthread_mutex_lock(&bibopMutex);
                page->nextPool = bibopPartialPool[page->classIndex];
                bibopPartialPool[page->classIndex] = page;
                pthread_mutex_unlock(&bibopMutex);
                continue;
            } else if(!page->gcHasMonitors) {
                // AGED PAST GRACE (even the youngest survivor at gcGraceEpoch < V-1 is
                // dead) and no BiBOP monitor exists to free -> the full walk would
                // reclaim every slot (no finalizers) and route the page to freePool,
                // where it is reformatted on reuse. Byte-identical outcome WITHOUT
                // touching a single slot: just reset the page and pool it.
                atomic_store_explicit(&page->bumpIndex, 0, memory_order_relaxed);
                page->freeList = 0;
                page->freeCount = 0;
                page->gcAllocedSinceSweep = JAVA_FALSE;
                page->gcNeedsReclaim = JAVA_FALSE;
                pthread_mutex_lock(&bibopMutex);
                page->nextPool = bibopFreePool;
                bibopFreePool = page;
                pthread_mutex_unlock(&bibopMutex);
                continue;
            }
            // else: all-dead but a BiBOP monitor is live -> fall through to the full walk
            // so the dead monitored slot(s) reach cn1BibopReclaimSlot.
        }
#endif
        // ACQUIRE pairs with the allocator's RELEASE store of bumpIndex: for every
        // slot i < n the header stores (parentCls / heapPosition / mark) that
        // preceded that release are visible to this walk. Relaxed could observe a
        // freshly-bumped slot with a garbage header.
        int n = atomic_load_explicit(&page->bumpIndex, memory_order_acquire);
        void* fl = 0;
        int freeCount = 0;
        int liveCount = 0;
#ifndef CN1_BIBOP_NO_FASTSWEEP
        JAVA_BOOLEAN needsReclaim = JAVA_FALSE;
#endif
        for(int i = 0 ; i < n ; i++) {
            JAVA_OBJECT o = cn1BibopSlot(page, i);
            // MATURED (adopted) slot: its lifecycle belongs to the legacy mark/sweep now.
            // Skip it entirely (no double-clearing) -- BiBOP counts it as occupied/live so
            // the page isn't reclaimed. The legacy sweep flips it back to -3 on death, and a
            // LATER BiBOP sweep of this page then reclaims it as a normal dead slot.
            if(o->__heapPosition == CN1_BIBOP_ADOPTED) {
                liveCount++;
                continue;
            }
            int m = o->__codenameOneGcMark;
            if(m == CN1_BIBOP_FREE_MARK) {
                *(void**)o = fl; fl = o; freeCount++;
            } else if(m == -1) {
                // fresh, never marked -> one cycle of grace (legacy parity)
                o->__codenameOneGcMark = V;
                liveCount++;
#ifndef CN1_BIBOP_NO_FASTSWEEP
                // parentCls==0 => a MID-CONSTRUCTION memset-elided object (the
                // translator publishes the class pointer only once every field is
                // written -- see cn1BibopFastAllocNoZero). Such classes are barred
                // from the elision if they declare a finalizer, so skipping the
                // finalizer probe here is exact, and dereferencing would be a NULL
                // crash (this sweep runs concurrently with the constructing thread).
                if(o->__codenameOneParentClsReference != 0 &&
                   o->__codenameOneParentClsReference->finalizerFunction != 0) needsReclaim = JAVA_TRUE;
#endif
            } else if(m < V - 1) {
                cn1BibopReclaimSlot(threadStateData, o);
                o->__codenameOneGcMark = CN1_BIBOP_FREE_MARK;
                *(void**)o = fl; fl = o; freeCount++;
            } else {
                liveCount++;
#ifndef CN1_BIBOP_NO_FASTSWEEP
                if(o->__codenameOneParentClsReference->finalizerFunction != 0) needsReclaim = JAVA_TRUE;
#endif
            }
        }
        page->freeList = fl;
        page->freeCount = freeCount;
#ifndef CN1_BIBOP_NO_FASTSWEEP
        // The monitor (CN1ThreadData) no longer lives in the object header, so the
        // per-slot "has a monitor" test is gone. Conservatively flag any page that still
        // has survivors while ANY BiBOP monitor is live globally: this can never miss a
        // monitored survivor (over-approximation only suppresses a future O(1) shortcut,
        // never a needed reclaim) and keeps the dead-monitor freeing exactly as before.
        if(page->gcHasMonitors && liveCount > 0) needsReclaim = JAVA_TRUE;
        // Refresh the per-page facts for the next sweep. gcGraceEpoch = V is a safe upper
        // bound on every survivor's epoch (survivors are at V from grace/mark-this-cycle,
        // or at V-1 from aging) so the all-dead test (gcGraceEpoch < V-1) can never fire
        // while a live/grace object remains.
        page->gcAllocedSinceSweep = JAVA_FALSE;
        page->gcNeedsReclaim = needsReclaim;
        page->gcGraceEpoch = V;
#endif
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

#ifdef CN1_CONSERVATIVE_GC_ROOTS
// =========================================================================
// PHASE 3b: conservative native-C-stack scanning AS A REAL GC ROOT SOURCE.
//
// WHY: object-bearing FRAMELESS methods (BytecodeMethod.isFramelessEligible with
// -Dcn1.frameless.objects) keep their object operand stack + object locals in a
// method-LOCAL C array (cn1_frameless_frame) on the native C stack, NOT in the
// side-allocated threadObjectStack the precise collector walks. Their object roots
// are therefore invisible to the precise scan. To keep those objects alive we make
// the GC additionally walk each stopped thread's native C stack [sp, stackBase) +
// its register snapshot and mark every word that resolves to a live heap object.
// The collector is now HYBRID: precise threadObjectStack scan (legacy frames) PLUS
// conservative native-stack scan (frameless frames). An object is reachable from
// whichever frame holds it; the conservative scan covers the whole native stack so
// the legacy<->frameless caller/callee boundary is never a gap.
//
// (a) cn1ConservativeResolve(word) -> base of the live heap object the word points
//     into (interior pointers included) or JAVA_NULL, dereferencing nothing it has
//     not first proven to be a registered heap address:
//       * BiBOP small objects: (w & ~(PAGE-1)) is the candidate page base; confirm
//         it is a registered page by binary-searching a snapshot of bibopAllPages;
//         map the interior word to its slot; liveness = slot not on the page
//         free-list (mark != FREE) and header sentinel intact.
//       * large/array objects (allObjectsInHeap + every thread's pending): a sorted,
//         non-overlapping [lo,hi) extent table; array element blocks are covered so
//         an interior element pointer resolves to the array base.
//       * garbage robustness: bounds-checked before any deref; unaligned/tagged words
//         rejected (filters tagged-Integer immediates whose bit0 is set).
// (b) cn1ConservativeMarkRange([lo,hi)): read every aligned word, resolve, gcMarkObject
//     it for REAL (serial worklist push -- lock-free in the GC-thread context). Marks
//     a SUPERSET of the precise set: nothing live is freed; at most a little floating
//     garbage (a stale stack slot's referent) is retained one cycle until the frame is
//     reused. Measured <0.3% over live (Phase 2).
//
// THREAD STOPPING (every thread that can hold a frameless root must be scanned):
//   * COOPERATIVE (lightweight Java threads -- the common case, proven in Phase 3a):
//     a thread that pauses at an allocation safepoint runs CN1_GC_PARK_CAPTURE in the
//     parking frame (setjmp flushes callee-saved regs into a scanned jmp_buf; records
//     the parked SP). The GC already waits for threadActive==FALSE, so the capture is
//     complete and the whole live call chain (including any frameless frame above the
//     safepoint) is resident in [sp, base).
//   * SIGNAL (genuine native threads the GC does not park, OR validation forcing via
//     CN1_GC_SIGNAL_STOP=1): pthread_kill(thread, SIGUSR2); the async-signal-safe
//     handler captures the interrupted SP + a raw copy of the ucontext register file
//     and spins on a release flag (store + spin only -- no malloc/lock). LOCK SAFETY:
//     the resolver snapshot (which reallocs) is rebuilt BEFORE the thread is stopped,
//     so the GC never reallocs while a thread is frozen mid-malloc.
// =========================================================================

// SIGUSR2 is used so SIGUSR1 stays free for app/JNI use.
#define CN1_GC_STOP_SIGNAL SIGUSR2

// =========================================================================
// CONSERVATIVE ROOT RESOLVER -- architecture + perf notes
// =========================================================================
// Under CN1_CONSERVATIVE_GC_ROOTS the collector finds roots by scanning stopped
// threads' native stacks + register snapshots word by word. Every candidate word is
// a raw machine value; to decide "does this word point at (or into) a live heap
// object, and if so which one?" we need an address->object resolver. That resolver is
// what cn1ConservativeResolve() queries and what cn1GcBuildRootSnapshots() (re)builds
// at the start of every mark cycle. There are TWO backing structures, because the heap
// has two allocation regimes with very different lifecycle:
//
//   1. BiBOP objects (small, non-array): live in size-class pages held in a GROW-ONLY
//      registry (bibopAllPages -- pages are never unlinked or reordered). A pointer is
//      resolved by locating its containing page (binary search over the page bases) then
//      its slot (O(1) from the page geometry). Because the registry is grow-only, the
//      base-sorted page array (cn1ConsPgSorted) is CACHED and only re-sorted when the
//      registration COUNT changes -- NOT every cycle. See cn1ConsPgSortedKey below.
//
//   2. Legacy objects (arrays + anything not BiBOP): tracked in allObjectsInHeap[]. These
//      are resolved via cn1ConsExt[] -- a flat array of (lo,hi,base) extents sorted by lo
//      address, binary-searched on lookup.
//
// WHY cn1ConsExt IS REBUILT + qsort()ed EVERY CYCLE (and the BiBOP pages are not):
//   allObjectsInHeap[] is NOT grow-only. The sweep removes a dead object by TOMBSTONING
//   its slot (allObjectsInHeap[pos] = JAVA_NULL), and a later allocation REFILLS that same
//   slot with a DIFFERENT object at a DIFFERENT address (placeObjectInHeapCollection's
//   NULL-slot scan from lastOffsetInRam). So neither the slot->address mapping nor the
//   address-sorted order is stable across cycles -- the cached-and-reuse trick that works
//   for the grow-only page registry does NOT apply. The whole extent array is therefore
//   rebuilt from the live legacy set and re-sorted each cycle.
//
// PERF: on allocation-heavy, array-heavy workloads with a large live set (e.g. the
//   vector-map MVT render: parparvm-bench MvtBench holds ~213k live legacy/array objects),
//   this per-cycle rebuild+qsort is a measurable slice of GC time (profiled ~8% of wall,
//   larger as a fraction of the collector itself). It is NOT the dominant cost of that
//   workload -- the conservative allocation path and marking the large live set dominate --
//   but it is the most self-contained target. Future optimization directions, in rough
//   order of payoff/risk: (a) incrementally maintain cn1ConsExt across cycles (the live
//   set is non-moving and largely stable between collections; only the transient churn and
//   the swept entries change) rather than a full rebuild; (b) route arrays through a
//   grow-only size-class arena so they resolve via the cached page path and skip cn1ConsExt
//   entirely; (c) replace the libc qsort (a function-pointer comparator call per compare)
//   with an inlined/radix sort. All three are correctness-critical (a resolver miss is a
//   use-after-free), so they must be validated against MvtBench + the GcStress/MtStress
//   gauntlet, not just the small-heap tests.
// =========================================================================

// ---- large/array snapshot: sorted by low address, non-overlapping extents ----
typedef struct { char* lo; char* hi; JAVA_OBJECT base; } CN1ConsExtent;
static CN1ConsExtent* cn1ConsExt = 0;
static int cn1ConsExtN = 0, cn1ConsExtCap = 0;

#ifndef CN1_DISABLE_BIBOP
// ---- BiBOP page snapshot: sorted by page base ----
typedef struct { char* base; int firstSlotOffset; int slotSize; int slotCount; int bumpIndex; } CN1ConsPage;
static CN1ConsPage* cn1ConsPg = 0;
static int cn1ConsPgN = 0, cn1ConsPgCap = 0;
// Cached base-sorted page pointers (GC thread only). The registry is grow-only,
// so this order is valid until the registration count changes.
static CN1BibopPage** cn1ConsPgSorted = 0;
static int cn1ConsPgSortedN = 0, cn1ConsPgSortedCap = 0;
static long long cn1ConsPgSortedKey = -1;

static int cn1ConsPgPtrCmp(const void* a, const void* b) {
    char* la = (char*)(*(CN1BibopPage* const*)a);
    char* lb = (char*)(*(CN1BibopPage* const*)b);
    return (la > lb) - (la < lb);
}
#endif

// Per-thread current ThreadLocalData, readable async-signal-safely from the stop
// handler (a plain TLS load). Set in getThreadLocalData (nativeMethods.m).
__thread struct ThreadLocalData* cn1TlsSelf = 0;
static volatile sig_atomic_t cn1GcSignalHandlerInstalled = 0;
static int cn1GcSignalStopMode = -1;  // -1 = uninit; 0 = cooperative+signal-for-native; 1 = signal-for-all

static void cn1ConsExtAdd(JAVA_OBJECT o) {
    if(o == JAVA_NULL) return;
    struct clazz* cls = o->__codenameOneParentClsReference;
    if(cls == 0) return;
    char* base = (char*)o;
    char* hi;
    if(cls->isArray) {
        JAVA_ARRAY a = (JAVA_ARRAY)o;
        char* hdrEnd = base + sizeof(struct JavaArrayPrototype);
        char* dataEnd = hdrEnd;
        if(a->data != 0 && a->length > 0 && a->primitiveSize > 0) {
            dataEnd = (char*)a->data + (long)a->length * a->primitiveSize;
        }
        hi = hdrEnd > dataEnd ? hdrEnd : dataEnd;
    } else {
        hi = base + sizeof(struct JavaObjectPrototype); // header only (instance size not recorded)
    }
    if(cn1ConsExtN == cn1ConsExtCap) {
        cn1ConsExtCap = cn1ConsExtCap ? cn1ConsExtCap * 2 : 4096;
        cn1ConsExt = (CN1ConsExtent*)realloc(cn1ConsExt, cn1ConsExtCap * sizeof(CN1ConsExtent));
    }
    cn1ConsExt[cn1ConsExtN].lo = base;
    cn1ConsExt[cn1ConsExtN].hi = hi;
    cn1ConsExt[cn1ConsExtN].base = o;
    cn1ConsExtN++;
}

static int cn1ConsExtCmp(const void* a, const void* b) {
    char* la = ((const CN1ConsExtent*)a)->lo;
    char* lb = ((const CN1ConsExtent*)b)->lo;
    return (la > lb) - (la < lb);
}

// Rebuild the resolver index. MUST be called while no thread we are about to scan is
// signal-stopped (it reallocs -> would deadlock against a thread frozen mid-malloc).
// O(allObjectsInHeap + pending + bibop pages) -- bounded by the heap the sweep already
// walks, so one rebuild per scanned thread is within the GC's existing complexity.
// Build ONCE PER MARK CYCLE: the walk over allObjectsInHeap + every thread's
// pending list plus the qsort is O(legacy objects * log) -- rebuilding it per
// scanned thread (as the per-thread scan paths naively would) made the GC thread
// spend more time in qsort than in marking on array-heavy workloads, stalling
// mutators parked behind threadBlockedByGC. Rebuilding within one cycle can only
// ever ADD objects allocated after the cycle started -- and those are mark==-1
// fresh, kept alive by the sweep's grace rule whether or not they resolve -- so
// the first build of the cycle is complete for correctness purposes. Nothing is
// freed during mark (sweep runs after), so entries can never go stale mid-cycle.
static int cn1ConsSnapEpoch = -1;
void cn1GcBuildRootSnapshots(void) {
    if(cn1ConsSnapEpoch == currentGcMarkValue) {
        return; // already built this cycle
    }
    cn1ConsSnapEpoch = currentGcMarkValue;
    cn1ConsExtN = 0;
    int n = currentSizeOfAllObjectsInHeap;
    for(int i = 0 ; i < n ; i++) {
        cn1ConsExtAdd(allObjectsInHeap[i]);
    }
    // A still-running LIGHTWEIGHT thread grows its pendingHeapAllocations lock-free in
    // codenameOneGcMalloc / cn1AddPending: malloc tmp; memcpy; free(old); pending = tmp.
    // Threads other than the one currently being scanned are NOT parked here, so reading
    // their array without serialization is a use-after-free: the free() of the old array
    // turns our read of th->pendingHeapAllocations[j] into a read of reclaimed memory, and
    // the resulting garbage word is taken as a heap extent base -> SIGBUS in gcMarkObject.
    // Take threadHeapMutex around the whole read loop; the realloc fast-paths now take the
    // SAME mutex (for lightweight threads too) so the malloc/memcpy/free/swap is atomic wrt
    // this read. The lock is acquired and RELEASED entirely here, before the caller signal-
    // stops any thread, so no thread is ever frozen mid-realloc holding it (no deadlock);
    // it is never inverted against lockCriticalSection (the migration path takes
    // criticalSection THEN threadHeapMutex -- this path takes only threadHeapMutex); and
    // the only libc-allocator calls under it (cn1ConsExtAdd's realloc) acquire the libc
    // lock in the same order as the realloc fast-paths, so there is no lock cycle.
    lockThreadHeapMutex();
    for(int ti = 0 ; ti < NUMBER_OF_SUPPORTED_THREADS ; ti++) {
        struct ThreadLocalData* th = allThreads[ti];
        if(th == 0 || th->pendingHeapAllocations == 0) continue;
        int pn = th->heapAllocationSize;
        for(int j = 0 ; j < pn ; j++) {
            JAVA_OBJECT o = (JAVA_OBJECT)th->pendingHeapAllocations[j];
            if(o != JAVA_NULL && o->__heapPosition == -1) cn1ConsExtAdd(o);
        }
    }
    unlockThreadHeapMutex();
    qsort(cn1ConsExt, cn1ConsExtN, sizeof(CN1ConsExtent), cn1ConsExtCmp);
#ifndef CN1_DISABLE_BIBOP
    // The page registry is GROW-ONLY (nodes never unlink or reorder), so its
    // base-sorted order only changes when a page is registered. Cache the
    // sorted page-pointer array and rebuild+qsort ONLY when the registration
    // count moved -- the per-cycle qsort of thousands of pages was one of the
    // largest GC costs on allocation-churn workloads (profiled: ~1/3 of the
    // snapshot build). A page registered mid-snapshot is missed by this cycle
    // exactly as it was by the old head-once walk (its objects are covered by
    // the mark==-1 grace); the count mismatch rebuilds on the NEXT cycle.
    {
        long long pageCount = atomic_load_explicit(&bibopAllPagesCount, memory_order_acquire);
        if(pageCount != cn1ConsPgSortedKey) {
            cn1ConsPgSortedN = 0;
            CN1BibopPage* p = atomic_load_explicit(&bibopAllPages, memory_order_acquire);
            while(p != 0) {
                if(cn1ConsPgSortedN == cn1ConsPgSortedCap) {
                    cn1ConsPgSortedCap = cn1ConsPgSortedCap ? cn1ConsPgSortedCap * 2 : 256;
                    cn1ConsPgSorted = (CN1BibopPage**)realloc(cn1ConsPgSorted, cn1ConsPgSortedCap * sizeof(CN1BibopPage*));
                }
                cn1ConsPgSorted[cn1ConsPgSortedN++] = p;
                p = atomic_load_explicit(&p->nextAll, memory_order_acquire);
            }
            qsort(cn1ConsPgSorted, cn1ConsPgSortedN, sizeof(CN1BibopPage*), cn1ConsPgPtrCmp);
            // key on the number we actually WALKED: if registrations raced past
            // the count we read, the next cycle's count differs and rebuilds
            cn1ConsPgSortedKey = cn1ConsPgSortedN;
        }
    }
    if(cn1ConsPgSortedN > cn1ConsPgCap) {
        cn1ConsPgCap = cn1ConsPgSortedN * 2;
        cn1ConsPg = (CN1ConsPage*)realloc(cn1ConsPg, cn1ConsPgCap * sizeof(CN1ConsPage));
    }
    cn1ConsPgN = cn1ConsPgSortedN;
    for(int pgI = 0 ; pgI < cn1ConsPgSortedN ; pgI++) {
        CN1BibopPage* p = cn1ConsPgSorted[pgI];
        // Load bumpIndex FIRST (acquire), then the geometry. A page popped from
        // freePool is reformatted by the acquiring MUTATOR (cn1BibopFormatPage
        // rewrites slotSize/firstSlotOffset/slotCount) concurrently with this walk;
        // its bumpIndex is 0 from the O(1) reclaim until the first allocation's
        // RELEASE store raises it -- which also publishes the new geometry (same
        // thread). So: bump==0 -> the resolver rejects every word into this page
        // (geometry may be torn but is never used); bump>0 -> the acquire makes the
        // matching geometry visible. Reading geometry BEFORE the acquire could pair
        // old geometry with the new bump -> misresolved interior words.
        cn1ConsPg[pgI].bumpIndex = atomic_load_explicit(&p->bumpIndex, memory_order_acquire);
        cn1ConsPg[pgI].base = (char*)p;
        cn1ConsPg[pgI].firstSlotOffset = p->firstSlotOffset;
        cn1ConsPg[pgI].slotSize = p->slotSize;
        cn1ConsPg[pgI].slotCount = p->slotCount;
    }
#endif
    if(getenv("CN1_SNAP_DEBUG")) {
        fprintf(stderr, "[SNAP] ext=%d pages=%d tableSize=%d\n",
            cn1ConsExtN,
#ifndef CN1_DISABLE_BIBOP
            cn1ConsPgN,
#else
            0,
#endif
            currentSizeOfAllObjectsInHeap);
    }
}

#ifdef CN1_RESOLVE_DIAG
static long cn1ResolveDiagCounts[4] = {0,0,0,0};
static const char* cn1ResolveDiagSampleCls[4] = {0,0,0,0};
void cn1ResolveDiagNote(int reason, JAVA_OBJECT o) {
    if(reason < 1 || reason > 3) return;
    cn1ResolveDiagCounts[reason]++;
    if(cn1ResolveDiagSampleCls[reason] == 0 && o->__codenameOneParentClsReference
       && o->__codenameOneParentClsReference->clsName) {
        cn1ResolveDiagSampleCls[reason] = o->__codenameOneParentClsReference->clsName;
    }
}
void cn1ResolveDiagReport(void) {
    if(cn1ResolveDiagCounts[1] || cn1ResolveDiagCounts[2] || cn1ResolveDiagCounts[3]) {
        fprintf(stderr, "CN1RESOLVEDIAG idx>=bump=%ld(%s) FREE=%ld(%s) heapPosBad=%ld(%s)\n",
            cn1ResolveDiagCounts[1], cn1ResolveDiagSampleCls[1] ? cn1ResolveDiagSampleCls[1] : "-",
            cn1ResolveDiagCounts[2], cn1ResolveDiagSampleCls[2] ? cn1ResolveDiagSampleCls[2] : "-",
            cn1ResolveDiagCounts[3], cn1ResolveDiagSampleCls[3] ? cn1ResolveDiagSampleCls[3] : "-");
        fflush(stderr);
    }
    cn1ResolveDiagCounts[1] = cn1ResolveDiagCounts[2] = cn1ResolveDiagCounts[3] = 0;
}
#endif

// (a) Resolve an arbitrary machine word to the base of the live heap object it points
// into (interior pointers included), or JAVA_NULL.
JAVA_OBJECT cn1ConservativeResolve(void* w) {
    uintptr_t v = (uintptr_t)w;
    if(v == 0) return JAVA_NULL;
    if((v & (sizeof(void*) - 1)) != 0) return JAVA_NULL; // reject unaligned / tagged-Integer

#ifndef CN1_DISABLE_BIBOP
    if(cn1ConsPgN > 0) {
        char* cand = (char*)(v & ~((uintptr_t)(CN1_BIBOP_PAGE_SIZE - 1)));
        int lo = 0, hi = cn1ConsPgN - 1;
        while(lo <= hi) {
            int mid = (lo + hi) >> 1;
            char* b = cn1ConsPg[mid].base;
            if(b == cand) {
                CN1ConsPage* pg = &cn1ConsPg[mid];
                long off = (long)((char*)w - cand);
                if(off < pg->firstSlotOffset) return JAVA_NULL;  // inside page header
                int idx = (int)((off - pg->firstSlotOffset) / pg->slotSize);
#ifdef CN1_RESOLVE_DIAG
                // Forensic: count rejections of slot-region words whose slot LOOKS like a
                // live object (plausible aligned parentCls in the app text) but is rejected.
                // A nonzero count during the paint cycle = the conservative scan is dropping
                // a live frameless root. Distinguishes "resolve rejects it" from "not scanned
                // at all" (referenced from an untraced field elsewhere).
                if(idx >= 0 && idx < pg->slotCount) {
                    JAVA_OBJECT __o = (JAVA_OBJECT)(cand + pg->firstSlotOffset + (long)idx * pg->slotSize);
                    struct clazz* __pc = __o->__codenameOneParentClsReference;
                    extern void cn1ResolveDiagNote(int reason, JAVA_OBJECT o);
                    if(__pc != 0 && (((uintptr_t)__pc & 7) == 0)) {
                        int __m = __o->__codenameOneGcMark;
                        if(idx >= pg->bumpIndex) cn1ResolveDiagNote(1, __o);         // idx >= snapshot bump
                        else if(__m == CN1_BIBOP_FREE_MARK) cn1ResolveDiagNote(2, __o); // FREE_MARK
                        else if(__o->__heapPosition != CN1_BIBOP_HEAP_POS && __o->__heapPosition != CN1_BIBOP_ADOPTED) cn1ResolveDiagNote(3, __o); // heapPos
                    }
                }
#endif
                if(idx < 0 || idx >= pg->bumpIndex || idx >= pg->slotCount) return JAVA_NULL;
                JAVA_OBJECT o = (JAVA_OBJECT)(cand + pg->firstSlotOffset + (long)idx * pg->slotSize);
                // ACQUIRE: the slot may be getting reused RIGHT NOW by a mutator
                // (freelist pop -> header stores -> mark=-1 RELEASE). Pairing with
                // that release orders our subsequent __heapPosition and (in
                // gcMarkObject) parentCls/body reads after the mark read, so a
                // stale stack word can never resolve a half-reinitialized slot
                // whose first 8 bytes still hold the free-list next pointer.
                int m = __atomic_load_n(&o->__codenameOneGcMark, __ATOMIC_ACQUIRE);
                if(m == CN1_BIBOP_FREE_MARK) return JAVA_NULL;   // on the page free-list
                // Accept both a normal BiBOP slot and a MATURED (adopted) slot -- a
                // matured object's memory is still in this page, so a conservative stack
                // word must still resolve to it or it would be missed as a root and swept.
                if(o->__heapPosition != CN1_BIBOP_HEAP_POS && o->__heapPosition != CN1_BIBOP_ADOPTED) return JAVA_NULL;
                return o;                                        // interior -> slot base
            } else if(b < cand) lo = mid + 1; else hi = mid - 1;
        }
    }
#endif

    if(cn1ConsExtN > 0) {
        int lo = 0, hi = cn1ConsExtN - 1, found = -1;
        while(lo <= hi) {
            int mid = (lo + hi) >> 1;
            if(cn1ConsExt[mid].lo <= (char*)w) { found = mid; lo = mid + 1; }
            else hi = mid - 1;
        }
        if(found >= 0 && (char*)w < cn1ConsExt[found].hi) {
            return cn1ConsExt[found].base;                       // base or array interior
        }
    }
    return JAVA_NULL;
}

// (b) Conservative range scan: read every aligned word in [lo,hi), resolve it, and MARK
// it for real. gcMarkObject in the GC-thread serial context just pushes to the worklist
// (no lock, no malloc), so this is safe to run while mutator threads are stopped.
//
// A conservative stack scan reads EVERY aligned word in [lo,hi), including the
// inter-variable padding a normal build treats as ordinary stack memory. Under
// -fsanitize=address those reads land in ASan's poisoned stack redzones and raise
// guaranteed stack-buffer-underflow false positives that bury any real finding.
// Exempt the scan (standard practice for conservative collectors) so ASan builds
// of the VM surface genuine heap bugs instead. No effect on a normal build.
__attribute__((no_sanitize("address")))
void cn1ConservativeMarkRange(CODENAME_ONE_THREAD_STATE, char* lo, char* hi) {
    if(lo == 0 || hi == 0 || hi <= lo) return;
    char* p = (char*)(((uintptr_t)lo + (sizeof(void*) - 1)) & ~((uintptr_t)(sizeof(void*) - 1)));
    for(; p + sizeof(void*) <= hi ; p += sizeof(void*)) {
        JAVA_OBJECT o = cn1ConservativeResolve(*(void**)p);
        if(o != JAVA_NULL) {
            gcMarkObject(threadStateData, o, JAVA_FALSE);
        }
    }
}

// Portable [high) stack base + size for a given pthread. Stacks grow DOWN, so the base
// is the HIGH address and the live region is [sp, base).
static char* cn1GcStackBase(pthread_t pt, size_t* outSize) {
#if defined(__APPLE__)
    void* base = pthread_get_stackaddr_np(pt);
    size_t ssz = pthread_get_stacksize_np(pt);
    *outSize = ssz;
    return (char*)base;
#elif defined(__linux__)
    pthread_attr_t attr;
    if(pthread_getattr_np(pt, &attr) != 0) { *outSize = 0; return 0; }
    void* addr = 0; size_t ssz = 0;
    pthread_attr_getstack(&attr, &addr, &ssz);  // addr = LOW end on Linux
    pthread_attr_destroy(&attr);
    *outSize = ssz;
    return (char*)addr + ssz;                    // convert to HIGH base
#else
    *outSize = 0; return 0;
#endif
}

// ---- async-signal-safe universal-stop handler ----------------------------------------
// Only stores + spins. Captures the interrupted SP (from the ucontext when available,
// else a handler local that is strictly deeper than the interrupted frame -- safe, it
// only widens the scanned range) and a raw copy of the ucontext (its inline mcontext
// holds the GPRs on macOS/Linux), then spins until the GC publishes gcSigRelease.
#if !defined(_WIN32)
static void cn1GcSignalHandler(int sig, siginfo_t* info, void* ucv) {
    struct ThreadLocalData* t = cn1TlsSelf;
    if(t == 0) return;
    // GENERATION HANDSHAKE (strand-proof): the stop request carries a generation
    // number > 0. We park by publishing gcSigStopped = gen and spin until the GC
    // publishes gcSigRelease >= gen (monotonic). This survives every abandonment
    // interleaving the old boolean protocol did not:
    //   * StopOne times out after the handler passed the request gate -> StopOne
    //     PRE-RELEASES the generation, so the late park exits immediately.
    //   * The GC's bounded release wait expires while we are descheduled, and a
    //     LATER cycle stops us again -> its release value is LARGER, so >= still
    //     frees us; a monotonic release is never reset to 0.
    int gen = (int)t->gcSigStopRequest;
    if(gen == 0) return;
    volatile int marker = 0;
    void* sp = (void*)&marker;
#if !defined(_WIN32)
    if(ucv != 0) {
        ucontext_t* uc = (ucontext_t*)ucv;
#if defined(__APPLE__)
        // CRITICAL (iOS/tvOS/macOS): on Apple, ucontext_t.uc_mcontext is a POINTER to the
        // register file, NOT an inline struct like glibc. memcpy'ing sizeof(ucontext_t) from
        // ucv here would capture only the ~56-byte ucontext header (the uc_mcontext pointer +
        // sigmask/stack), NOT the interrupted GPRs -- so an object reference that is live only
        // in a register (frameless codegen keeps hot object refs in callee-saved x19-x28 across
        // the native draw calls made from paintComponent) is invisible when the EDT is
        // signal-stopped mid-paint, and gets swept -> the intermittent paintComponent NPE /
        // use-after-free on tvOS. Copy the POINTED-TO mcontext (holds __ss with x0-x28/fp/lr/
        // sp/pc) so those registers are scanned by cn1ConservativeMarkRange below.
        if(uc->uc_mcontext) {
#if defined(__aarch64__)
            sp = (void*)uc->uc_mcontext->__ss.__sp;
#elif defined(__x86_64__)
            sp = (void*)uc->uc_mcontext->__ss.__rsp;
#endif
            // Scan ONLY the general-purpose thread state (__ss: x0-x28/fp/lr/sp/pc on arm64,
            // rax..r15/rip on x86_64). Object references only ever live in GPRs -- never in the
            // NEON/FP vector state (__ns is 528 of the 816-byte arm64 mcontext) or the exception
            // state (__es). Copying the whole mcontext would feed all that float data to the
            // conservative scan as spurious "pointers", pinning garbage and bloating the live heap
            // -> heavier/more-frequent GC on allocation-heavy paths (vector-tile rendering). __ss
            // alone captures every object root with the minimum false-positive surface.
            size_t mlen = sizeof(uc->uc_mcontext->__ss);
            if(mlen > sizeof(t->gcSigRegs)) mlen = sizeof(t->gcSigRegs);
            memcpy(t->gcSigRegs, (const void*)&uc->uc_mcontext->__ss, mlen); // GPRs only
            t->gcSigRegsLen = (sig_atomic_t)mlen;
        }
#else
        // Linux: uc_mcontext is inline in ucontext_t, and the GPRs (regs[]/gregs[]) sit at the
        // start, so a bounded copy of the ucontext captures them as scannable data.
#if defined(__x86_64__)
        sp = (void*)uc->uc_mcontext.gregs[REG_RSP];
#elif defined(__aarch64__)
        sp = (void*)uc->uc_mcontext.sp;
#endif
        size_t ulen = sizeof(ucontext_t);
        if(ulen > sizeof(t->gcSigRegs)) ulen = sizeof(t->gcSigRegs);
        memcpy(t->gcSigRegs, ucv, ulen);   // capture the interrupted GPRs as scannable data
        t->gcSigRegsLen = (sig_atomic_t)ulen;
#endif
    }
#endif
    t->gcSigStackPointer = sp;
    __atomic_thread_fence(__ATOMIC_RELEASE);
    t->gcSigStopped = (sig_atomic_t)gen;
    while((int)t->gcSigRelease < gen) { /* async-signal-safe spin */ }
    // Only clear our own park marker -- a late-exiting older handler must not
    // wipe a newer generation's park the GC is currently waiting on.
    if((int)t->gcSigStopped == gen) t->gcSigStopped = 0;
}
#endif // !_WIN32 (signal-stop unavailable; Windows uses the cooperative path)

void cn1GcInstallSignalHandler(void) {
    if(cn1GcSignalHandlerInstalled) return;
    if(cn1GcSignalStopMode < 0) {
        const char* e = getenv("CN1_GC_SIGNAL_STOP");
        cn1GcSignalStopMode = (e != 0 && e[0] == '1') ? 1 : 0;
    }
#if !defined(_WIN32)
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = cn1GcSignalHandler;
    sa.sa_flags = SA_SIGINFO | SA_RESTART;
    sigemptyset(&sa.sa_mask);
    sigaction(CN1_GC_STOP_SIGNAL, &sa, 0);
#endif
    cn1GcSignalHandlerInstalled = 1;
}

// Signal-stop one thread, returning its captured SP (or 0 on failure/timeout). The
// resolver snapshot MUST already be built (we do not realloc after the thread freezes).
static char* cn1GcSignalStopOne(struct ThreadLocalData* t) {
#if !defined(_WIN32)
    if(!t->gcPthreadValid) return 0;
    // Next generation for this thread (only the GC thread writes it). gcSigRelease
    // is MONOTONIC and never reset -- see the handler's generation handshake.
    int gen = (int)t->gcSigStopGen + 1;
    t->gcSigStopGen = (sig_atomic_t)gen;
    t->gcSigRegsLen = 0;
    t->gcSigStackPointer = 0;
    __atomic_thread_fence(__ATOMIC_RELEASE);
    t->gcSigStopRequest = (sig_atomic_t)gen;
    if(pthread_kill(t->gcPthread, CN1_GC_STOP_SIGNAL) != 0) { t->gcSigStopRequest = 0; return 0; }
    // bounded wait for the handler to park THIS generation
    int spins = 0;
    while((int)t->gcSigStopped != gen) {
        if(++spins > 2000000) { /* ~timeout: could not stop */ break; }
        if((spins & 1023) == 0) usleep(50);
    }
    if((int)t->gcSigStopped != gen) {
        // Abandon: the signal may still be pending, and the handler may ALREADY be
        // past its request gate about to park. PRE-RELEASE the generation so that
        // park (whenever it happens) exits immediately instead of spinning forever
        // on a release nobody will send -- the strand bug of the boolean protocol.
        t->gcSigRelease = (sig_atomic_t)gen;
        t->gcSigStopRequest = 0;
        return 0;
    }
    return (char*)t->gcSigStackPointer;
#else
    return 0;
#endif
}

static void cn1GcSignalReleaseOne(struct ThreadLocalData* t) {
#if !defined(_WIN32)
    t->gcSigRelease = t->gcSigStopGen;   // monotonic: frees this AND any older park
    __atomic_thread_fence(__ATOMIC_RELEASE);
    int spins = 0;
    while(t->gcSigStopped) { if(++spins > 2000000) break; }
    // Clear the request so a stale still-queued SIGUSR2 delivered after this point
    // sees gen==0 at the handler gate and returns without parking.
    t->gcSigStopRequest = 0;
#endif
}

// Scan ONE thread's native C stack [sp, base) + its register snapshot, marking every
// resolved live object. threadStateData = the GC thread; t = the thread being scanned.
static void cn1GcScanThreadNativeStack(CODENAME_ONE_THREAD_STATE, struct ThreadLocalData* t) {
    if(!t->gcPthreadValid) return;
    size_t ssz = 0;
    char* base = cn1GcStackBase(t->gcPthread, &ssz);
    if(base == 0 || ssz == 0) return;

    // Snapshot rebuilt BEFORE any signal-stop (realloc-while-frozen would deadlock).
    cn1GcBuildRootSnapshots();

    // Cooperative scan iff the thread published a FRESH capture this cycle (it parked at
    // an allocation safepoint). This is the proven, race-free path for lightweight threads.
    // The signal path is reserved for threads with no fresh capture (genuine native threads
    // that the GC does not park) or the forced validation mode.
    int useCoop = t->gcParkCaptured && t->gcStackPointerAtPark != 0 && cn1GcSignalStopMode == 0;
    if(useCoop) {
        char* sp = (char*)t->gcStackPointerAtPark;
        if(sp >= base - (long)ssz && sp < base) {
            cn1ConservativeMarkRange(threadStateData, sp, base);
            cn1ConservativeMarkRange(threadStateData, (char*)&t->gcRegisterSnapshot,
                                     (char*)&t->gcRegisterSnapshot + sizeof(t->gcRegisterSnapshot));
            return;
        }
        // fall through to signal stop if the cooperative capture looked stale
    }

    // SIGNAL path: stop, scan, release. Used for native threads, for the forced
    // CN1_GC_SIGNAL_STOP=1 validation mode, or as a fallback for a stale capture.
    char* sp = cn1GcSignalStopOne(t);
    if(sp == 0) {
        // Could not stop the thread. If it is lightweight and cooperatively captured we
        // can still fall back to that capture; otherwise this thread's frameless roots
        // are at risk -- log once (honest gap).
        if(t->gcParkCaptured && t->gcStackPointerAtPark != 0) {
            char* csp = (char*)t->gcStackPointerAtPark;
            if(csp >= base - (long)ssz && csp < base) {
                cn1ConservativeMarkRange(threadStateData, csp, base);
                cn1ConservativeMarkRange(threadStateData, (char*)&t->gcRegisterSnapshot,
                                         (char*)&t->gcRegisterSnapshot + sizeof(t->gcRegisterSnapshot));
            }
        }
        return;
    }
    if(sp >= base - (long)ssz && sp < base) {
        cn1ConservativeMarkRange(threadStateData, sp, base);
    }
    if(t->gcSigRegsLen > 0) {
        cn1ConservativeMarkRange(threadStateData, t->gcSigRegs, t->gcSigRegs + t->gcSigRegsLen);
    }
    cn1GcSignalReleaseOne(t);
}

// Scan the GC thread's OWN native stack (a root could be live only in a GC-thread C
// local). flushes our callee-saved regs via setjmp into a scanned buffer.
static void cn1GcScanOwnStack(CODENAME_ONE_THREAD_STATE) {
    if(!threadStateData->gcPthreadValid) return;
    jmp_buf ownRegs; (void)setjmp(ownRegs);
    volatile void* spv = (void*)&spv;
    char* sp = (char*)spv;
    size_t ssz = 0;
    char* base = cn1GcStackBase(threadStateData->gcPthread, &ssz);
    if(base == 0 || ssz == 0) return;
    if(sp < base - (long)ssz || sp >= base) return;
    cn1GcBuildRootSnapshots();
    cn1ConservativeMarkRange(threadStateData, sp, base);
    cn1ConservativeMarkRange(threadStateData, (char*)&ownRegs, (char*)&ownRegs + sizeof(ownRegs));
}

static void cn1GcSignalStopThreads(struct ThreadLocalData* self) { (void)self; }
static void cn1GcSignalReleaseThreads(struct ThreadLocalData* self) { (void)self; }

#ifdef CN1_CONSERVATIVE_GC_SELFCHECK
// Transient ⊇ self-check (NOT shipped): every precise OBJECT root on a paused thread's
// object stack must also be resolvable conservatively. A failure means an is-heap-
// address / interior-pointer bug. Counts unmanaged (static/VM-singleton) roots that
// live outside every GC region separately -- those are out of scope, never swept.
static long long cn1SelfMiss = 0, cn1SelfChecked = 0, cn1SelfUnmanaged = 0;
static void cn1GcSelfCheckThreadStack(struct ThreadLocalData* t, int stackSize) {
    cn1GcBuildRootSnapshots();
    for(int i = 0 ; i < stackSize ; i++) {
        struct elementStruct* e = &t->threadObjectStack[i];
        if(e->type != CN1_TYPE_OBJECT) continue;
        JAVA_OBJECT o = e->data.o;
        if(o == JAVA_NULL || CN1_IS_TAGGED(o)) continue;
        if(o->__codenameOneParentClsReference == 0 ||
           o->__codenameOneParentClsReference == (&class__java_lang_Class)) continue;
        cn1SelfChecked++;
        JAVA_OBJECT r = cn1ConservativeResolve((void*)o);
        if(r == JAVA_NULL) { cn1SelfUnmanaged++; continue; } // static/VM singleton, out of scope
        if(r != o) {
            cn1SelfMiss++;
            fprintf(stderr, "[CONS-GC][SELFCHECK][MISS] precise root %p resolved to %p (class=%s)\n",
                (void*)o, (void*)r,
                (o->__codenameOneParentClsReference ? o->__codenameOneParentClsReference->clsName : "?"));
        }
    }
    fprintf(stderr, "[CONS-GC][SELFCHECK] checked=%lld unmanaged=%lld MISS=%lld\n",
        cn1SelfChecked, cn1SelfUnmanaged, cn1SelfMiss);
}
#endif
#endif /* CN1_CONSERVATIVE_GC_ROOTS */

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
    // Small objects AND small arrays: serve from the per-thread BiBOP page heap,
    // which skips placeObjectInHeapCollection / allObjectsInHeap entirely. Arrays
    // are single-block (header + contiguous data, see allocArray) so a slot holds
    // the whole thing; the page sweep walks slot boundaries only and the
    // conservative resolver maps interior element pointers to the slot base, so
    // no array-specific GC handling is needed. Moving arrays here removes the
    // dominant legacy-table traffic (char[] buffers, boxed-free int[] work sets):
    // no table registration, no extent-snapshot entry, no per-object free.
    // Objects larger than the biggest size class fall through to the legacy
    // calloc + table-registration path below. 0 => pages unavailable, fall back.
    // nativeAllocationMode no longer forces the legacy path: its purpose was to
    // keep native-held objects visible to the collector via the pending table,
    // and under CN1_CONSERVATIVE_GC_ROOTS (always on) a native's C locals are
    // scanned as roots directly. Keeping natives on the legacy path made every
    // native-bracketed allocation (e.g. StringBuilder.append's buffer growth) a
    // table+extent entry -- measured: 1.5M-entry extent snapshots qsorted per GC
    // cycle during string churn, stalling mutators. The mode still suppresses GC
    // triggers/parks inside the bracket (cn1BibopMaybeGc honors it).
    if(parent != 0 && size <= CN1_BIBOP_MAX_OBJECT && constantPoolObjects != 0
#ifndef CN1_CONSERVATIVE_GC_ROOTS
       && !threadStateData->nativeAllocationMode
#endif
       ) {
        JAVA_OBJECT bibopObj = cn1BibopAlloc(threadStateData, size, parent);
        if(bibopObj != JAVA_NULL) {
            return bibopObj;
        }
    }
#endif
    if(getenv("CN1_LEGACY_DEBUG")) {
        static _Atomic long cn1LegacyDbgCount = 0;
        long c = atomic_fetch_add_explicit(&cn1LegacyDbgCount, 1, memory_order_relaxed);
        if((c % 100000) == 0) {
            fprintf(stderr, "[LEGACY] #%ld cls=%s size=%d nativeMode=%d lastSetter=%s\n",
                c, parent ? parent->clsName : "?", size,
                (int)threadStateData->nativeAllocationMode,
                cn1LastNamSetter ? cn1LastNamSetter : "(cleared)");
        }
    }
    if(lowMemoryMode && !threadStateData->nativeAllocationMode) {
        CN1_GC_PARK_CAPTURE(threadStateData);   // PHASE 3b: native-stack capture at park
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
    o->__heapPosition = -1;
#ifdef DEBUG_GC_ALLOCATIONS
    o->className = threadStateData->callStackClass[threadStateData->callStackOffset - 1];
    o->line = threadStateData->callStackLine[threadStateData->callStackOffset - 1];
#endif
    
    if(threadStateData->heapAllocationSize == threadStateData->threadHeapTotalSize) {
        if(threadStateData->threadBlockedByGC && !threadStateData->nativeAllocationMode) {
            CN1_GC_PARK_CAPTURE(threadStateData);   // PHASE 3b: native-stack capture at park
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
            CN1_GC_PARK_CAPTURE(threadStateData);   // PHASE 3b: native-stack capture at park
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
                CN1_GC_PARK_CAPTURE(threadStateData);   // PHASE 3b: native-stack capture at park
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
                // Serialize the grow-and-free against cn1GcBuildRootSnapshots, which reads
                // this array from the GC thread while this (possibly lightweight, still-
                // running) thread is NOT parked. The OLD guard skipped the lock for
                // lightweight threads, so the GC could read pendingHeapAllocations right as
                // free() reclaimed it -> use-after-free. Lock unconditionally; the snapshot
                // reader takes the SAME mutex. Held only across malloc/memcpy/free/swap (no
                // park, no signal-stop inside), so it cannot deadlock the GC.
                lockThreadHeapMutex();
                void** tmp = malloc(threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
                memset(tmp, 0, threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
                memcpy(tmp, threadStateData->pendingHeapAllocations, threadStateData->threadHeapTotalSize * sizeof(void *));
                threadStateData->threadHeapTotalSize *= 2;
                free(threadStateData->pendingHeapAllocations);
                threadStateData->pendingHeapAllocations = tmp;
                unlockThreadHeapMutex();
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
    // A MATURED (-4) object's memory is also a page slot -- never free() it either;
    // the legacy sweep flips it back to -3 (below) and the BiBOP sweep reclaims it.
    if(obj->__heapPosition == CN1_BIBOP_HEAP_POS || obj->__heapPosition == CN1_BIBOP_ADOPTED) {
        if(obj->__heapPosition == CN1_BIBOP_ADOPTED) {
            // Hand the slot back to BiBOP: revert to a normal dead BiBOP slot so the next
            // sweep of its (retired) page reclaims it to the page free-list.
            obj->__heapPosition = CN1_BIBOP_HEAP_POS;
        }
        return;
    }
#endif
    {
        void* md = cn1MonitorDataRemove(obj);
        if(md) {
            free(md);
        }
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
// recursionKey is the force-mark "pass epoch". It used to be a constant (1) compared
// against the per-object __codenameOneReferenceCount; that field has been relocated out
// of the header into the force-visited side table below. recursionKey is now bumped once
// per GC cycle (in codenameOneGCMark) so a stale table entry from a previous cycle reads
// as not-visited, and no per-pass clearing is needed.
int recursionKey = 1;

// ---- Force-visited side table (relocated __codenameOneReferenceCount recursion guard) ----
// The force-mark re-scan (gcMarkObject with force=JAVA_TRUE on an already-marked object)
// needs a per-object "already force-visited THIS pass" flag to terminate on cyclic
// already-marked subgraphs. It used to live in __codenameOneReferenceCount. It now lives
// here, keyed by object pointer, storing the recursionKey epoch of the last force visit.
// The force re-scan runs ONLY on the serial GC-mark path (the parallel worker path returns
// before ever touching this), so no locking is required.
struct CN1FVEntry { JAVA_OBJECT key; int epoch; struct CN1FVEntry* next; };
#define CN1_FV_BUCKETS 4096
static struct CN1FVEntry* cn1FVBuckets[CN1_FV_BUCKETS];

static inline unsigned cn1FVHash(JAVA_OBJECT o) {
    uintptr_t p = (uintptr_t)o; p >>= 4;
    return (unsigned)((p ^ (p >> 16)) & (CN1_FV_BUCKETS - 1));
}

// Returns 1 if obj was already force-visited at the current `key` epoch (caller should
// skip re-traversal); otherwise records this visit and returns 0.
static int cn1ForceVisitedTestAndSet(JAVA_OBJECT obj, int key) {
    unsigned h = cn1FVHash(obj);
    struct CN1FVEntry* e = cn1FVBuckets[h];
    while(e) {
        if(e->key == obj) {
            if(e->epoch == key) return 1;
            e->epoch = key; return 0;
        }
        e = e->next;
    }
    e = (struct CN1FVEntry*)malloc(sizeof(struct CN1FVEntry));
    e->key = obj; e->epoch = key; e->next = cn1FVBuckets[h];
    cn1FVBuckets[h] = e;
    return 0;
}

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

#ifdef CN1_BIBOP_VALIDATE
// Forensic (serial mark): the object gcMarkDrain is currently tracing -- i.e. the
// PARENT whose mark function is marking children. The gcMarkObject child-side
// validation dumps this so a corrupt child tells us WHO referenced it, which
// distinguishes a conservative-scan root-miss (parent live, child wrongly freed)
// from a worklist/slot reuse (the parent itself is stale). Written on the single
// GC/mark thread only, read in the same-thread child-mark below.
volatile JAVA_OBJECT gcMarkCurrentDrainObj = JAVA_NULL;
#endif

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

#if !defined(CN1_DISABLE_BIBOP) && !defined(CN1_BIBOP_NO_FASTSWEEP)
// Stamp the BiBOP page of a just-marked-live object with the current epoch so the sweep
// knows the page had a live slot THIS cycle (-> must full-walk, never O(1) all-dead).
// Relaxed + idempotent: every parallel marker that newly marks a slot on this page stores
// the same value; the GC-thread sweep reads it after the mark-pool join barrier.
static inline void cn1BibopStampMarked(JAVA_OBJECT obj, int markVal) {
    // Stamp for a normal BiBOP slot AND a MATURED (-4) slot: a live matured object's
    // memory is still in this page, so its page must not be O(1) all-dead reclaimed.
    if(obj->__heapPosition == CN1_BIBOP_HEAP_POS || obj->__heapPosition == CN1_BIBOP_ADOPTED) {
        CN1BibopPage* pg = (CN1BibopPage*)(((uintptr_t)obj) & ~((uintptr_t)CN1_BIBOP_PAGE_SIZE - 1));
        atomic_store_explicit(&pg->gcLastMarkedEpoch, markVal, memory_order_relaxed);
    }
}
#define CN1_BIBOP_STAMP_MARKED(o, m) cn1BibopStampMarked((o), (m))
#else
#define CN1_BIBOP_STAMP_MARKED(o, m) do {} while(0)
#endif

void gcMarkObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {
    if(obj == JAVA_NULL || CN1_IS_TAGGED(obj)) {
        return;
    }
    // ACQUIRE-load the mark word (the object's publication point) BEFORE reading any
    // other header field. cn1BibopInitSlot writes parentClsReference/heapPosition and
    // THEN release-stores the mark word LAST, so the mark word is the single
    // happens-before edge that publishes a freshly-allocated object. The parallel
    // marker used a RELAXED load here: on arm64's weak memory model that let a worker
    // observe the object (mark word) WITHOUT observing the preceding parentClsReference
    // store, so it dereferenced a stale/garbage parentClsReference->markFunction and
    // crashed at a wild PC (random SIGSEGV in the theme phase, x86 masked it because
    // every x86 load is already acquire). The acquire here pairs with that release and
    // orders every parentClsReference read below -- the guard, the CAS-success deref,
    // and (transitively, through the worklist mutex) the drain worker's deref.
    int markSnapshot = __atomic_load_n(&obj->__codenameOneGcMark, __ATOMIC_ACQUIRE);
#if !defined(CN1_DISABLE_BIBOP)
    // A FREED BiBOP slot sits on its page free-list, which stores the intrusive
    // next pointer in the slot's first word -- i.e. OVER __codenameOneParentClsReference
    // (offset 0). Such a slot is NOT a live object: its parentCls is a garbage
    // free-list pointer and its class/mark functions are meaningless. The
    // conservative native-stack scan legitimately hands us interior pointers to
    // whatever a stack word happens to hold, including a free slot (this is how it
    // manifested: an x86 stack word pointed at a freed slot -- arm64's differing
    // layout, and ASan's redzone layout, simply never produced that word, which is
    // why the crash looked x64-only and vanished under ASan). Marking it would
    // stamp gcMark=current over FREE_MARK, push it, and the drain would then call
    // through its clobbered parentCls -> jump to a garbage address. Reject it here:
    // a free slot has no live fields to trace. FREE_MARK is never a live mark
    // (live == currentGcMarkValue or -1 grace), so this can't skip a real object.
    if(markSnapshot == CN1_BIBOP_FREE_MARK) {
        return;
    }
#endif
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    // Conservative-scan safety guard. The native-stack scan can legitimately false-positive-
    // mark a DEAD object (a stale native-stack word happens to resolve to it -- unavoidable
    // with conservative roots). Precisely draining that dead object then FOLLOWS its now-
    // dangling reference fields; e.g. a freed StringBuilder value[] whose first word (offset
    // 0, over __codenameOneParentClsReference) now holds a BiBOP free-list next pointer. Left
    // unchecked, gcMarkObject dereferences that garbage parentCls -> markFunction and jumps to
    // a wild address (observed on arm64: parentCls=0x200000000, unmapped, deterministic SIGSEGV
    // in the theme phase draining a dead StringBuilder's value[]; x86_64/musl simply never
    // produced the offending stack word, which is why it looked arch-specific).
    //
    // A real clazz is a static inside the loaded image, close to the code; validate the
    // parentCls VALUE cheaply (aligned + within 512MB of the code) and, only when it looks
    // implausible, confirm against the authoritative resolver -- which never dereferences the
    // suspect pointer: a genuine live published slot resolves to itself, a dangling/free/garbage
    // pointer does not. Skipping a non-live object is correct (it has no live fields to trace);
    // a truly-live object with a far class still resolves to itself and is traced normally. The
    // filter passes every real object in a couple of instructions, so resolve runs only on the
    // rare suspicious pointer -- negligible on the mark hot path.
    {
        uintptr_t __pc = (uintptr_t)obj->__codenameOneParentClsReference;
        uintptr_t __anchor = (uintptr_t)(void*)&gcMarkObject;
        uintptr_t __dist = __pc > __anchor ? __pc - __anchor : __anchor - __pc;
        if(__pc != 0 && (((__pc & (sizeof(void*) - 1)) != 0) || __dist > (512ULL << 20))) {
            if(cn1ConservativeResolve((void*)obj) != obj) {
                return;   // dangling / freed / garbage -> not a live object, skip
            }
        }
    }
#endif
    if(obj->__codenameOneParentClsReference == 0 || obj->__codenameOneParentClsReference == (&class__java_lang_Class)) {
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
        int old = markSnapshot; // acquire-loaded above; pairs with the alloc release
        if(old == markVal) {
            return; // already marked this cycle
        }
        if(__sync_bool_compare_and_swap(&obj->__codenameOneGcMark, old, markVal)) {
            CN1_BIBOP_STAMP_MARKED(obj, markVal);
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
            if(cn1ForceVisitedTestAndSet(obj, recursionKey)) {
                return;
            }
            if(obj->__codenameOneParentClsReference->markFunction != 0) {
                gcMarkWorklistPush(obj, force);
            }
        }
        return;
    }
#ifdef CN1_BIBOP_VALIDATE
    // Forensic (child side): the intermittent arm64 suite crash faults EXACTLY at the
    // stamp below -- gcMarkObject marking a child (e.g. Component.BGPainter.this$0)
    // whose header is readable at the acquire-load (above) but reclaimed by the time
    // we write its mark. Detect a corrupt/reclaimed child BEFORE that write and dump
    // both the child and the parent that referenced it (gcMarkCurrentDrainObj), so a
    // reproduction says root-miss (parent live, child wrongly freed) vs slot-reuse.
    // A live child carries heapPosition CN1_BIBOP_HEAP_POS or >= -1, and its clazz's
    // markFunction lies in the app text (never a libc/heap address); anything else is
    // a freed/reused slot or a dangling reference.
    {
        gcMarkFunctionPointer __cfp = obj->__codenameOneParentClsReference
            ? obj->__codenameOneParentClsReference->markFunction : (gcMarkFunctionPointer)0;
        uintptr_t __anchor = (uintptr_t)(void*)&gcMarkObject;
        uintptr_t __fpv = (uintptr_t)(void*)__cfp;
        int __fpBad = (__cfp != 0) &&
            ((__fpv > __anchor ? __fpv - __anchor : __anchor - __fpv) > (256ULL << 20));
        int __hp = obj->__heapPosition;
        // CN1_BIBOP_ADOPTED (-4) is a live MATURED slot (owned by the legacy sweep), not
        // corruption -- accept it exactly like a normal BiBOP slot.
        if((__hp != CN1_BIBOP_HEAP_POS && __hp != CN1_BIBOP_ADOPTED && __hp < -1) || __fpBad) {
            JAVA_OBJECT __p = gcMarkCurrentDrainObj;
            // Name the culprit: the drain parent is validated live below, so its class
            // name is safe to read and identifies WHICH object holds the lost reference.
            // markCallSite is the return address into the parent's generated mark
            // function -> addr2line / the gdb bt maps it to the exact field being marked.
            const char* __pcls = "?";
            if(__p != JAVA_NULL
               && (__p->__heapPosition == CN1_BIBOP_HEAP_POS || __p->__heapPosition == CN1_BIBOP_ADOPTED)
               && __p->__codenameOneParentClsReference != 0
               && __p->__codenameOneParentClsReference->clsName != 0) {
                __pcls = __p->__codenameOneParentClsReference->clsName;
            }
            void* __callSite = __builtin_return_address(0);
            fprintf(stderr, "CN1BIBOP MARKOBJ CORRUPT CHILD: parentClass=%s markCallSite=%p :: "
                    "child=%p parentCls=%p childMarkFn=%p "
                    "childHeapPos=%d childGcMark=%d curMark=%d FREE_MARK=%d :: drainParent=%p "
                    "parentCls=%p parentMarkFn=%p parentHeapPos=%d parentGcMark=%d\n",
                    __pcls, __callSite,
                    (void*)obj, (void*)obj->__codenameOneParentClsReference, (void*)__cfp,
                    __hp, obj->__codenameOneGcMark, currentGcMarkValue, CN1_BIBOP_FREE_MARK,
                    (void*)__p,
                    __p ? (void*)__p->__codenameOneParentClsReference : (void*)0,
                    (__p && __p->__codenameOneParentClsReference)
                        ? (void*)__p->__codenameOneParentClsReference->markFunction : (void*)0,
                    __p ? __p->__heapPosition : -999,
                    __p ? __p->__codenameOneGcMark : -999);
            fflush(stderr);
            abort();
        }
    }
#endif
    obj->__codenameOneGcMark = markVal;
    CN1_BIBOP_STAMP_MARKED(obj, markVal);
    gcMarkFoundUnmarkedChildInPass = JAVA_TRUE;
    gcMarkNewObjectCount++;   // SATB fixpoint detection (mark-thread only)
#ifdef CN1_BIBOP_VALIDATE
    // Belt diagnostic: name what the main drain systematically MISSED. When set, every
    // object the belt newly marks is a reachable-but-unmarked object; log its class and
    // its drain parent's class (throttled) to identify the drain-incompleteness pattern.
    if(gcBeltDiagActive && gcBeltDiagCount < 60) {
        JAVA_OBJECT __p = gcMarkCurrentDrainObj;
        const char* __cc = (obj->__codenameOneParentClsReference && obj->__codenameOneParentClsReference->clsName)
            ? obj->__codenameOneParentClsReference->clsName : "?";
        const char* __pc = (__p && __p->__heapPosition == CN1_BIBOP_HEAP_POS
            && __p->__codenameOneParentClsReference && __p->__codenameOneParentClsReference->clsName)
            ? __p->__codenameOneParentClsReference->clsName : "?";
        fprintf(stderr, "CN1BIBOP BELT RECOVERED: child=%s <- drainParent=%s\n", __cc, __pc);
        fflush(stderr);
        gcBeltDiagCount++;
    }
#endif
    gcMarkFunctionPointer __markFn = obj->__codenameOneParentClsReference->markFunction;
#if CN1_ADOPT_POLICY != 0 && !defined(CN1_DISABLE_BIBOP)
    // Poor-man's generational adoption: a reachable, non-leaf (markFunction != 0) BiBOP
    // object graduates into the legacy mark/sweep, which traces it unconditionally =
    // complete (the split-reachability bug only affects non-leaf BiBOP objects whose
    // subtree the overflow-gated BiBOP rescan can drop). Leaf objects have no subtree, so
    // they stay in the fast BiBOP path. TENURE waits for one survival (markSnapshot > 0)
    // but cascades (gcCurrentlyMaturing) so a maturing subtree matures WHOLE, never half a
    // tree. Stamp already fired above (heapPosition still -3), so ordering is fine.
    if(__markFn != 0 && obj->__heapPosition == CN1_BIBOP_HEAP_POS
#if CN1_ADOPT_POLICY == 1
       && (markSnapshot > 0 || gcCurrentlyMaturing)
#endif
       ) {
        cn1MatureObject(obj);
    }
#endif
    if(__markFn != 0) {
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
        // Same use-after-free as codenameOneGcMalloc: lock unconditionally (lightweight
        // threads included) so the GC's cn1GcBuildRootSnapshots never reads this array
        // mid-free. Held only across the grow; no park/signal-stop inside -> no deadlock.
        lockThreadHeapMutex();
        void** tmp = malloc(threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
        memset(tmp, 0, threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
        memcpy(tmp, threadStateData->pendingHeapAllocations, threadStateData->threadHeapTotalSize * sizeof(void *));
        threadStateData->threadHeapTotalSize *= 2;
        free(threadStateData->pendingHeapAllocations);
        threadStateData->pendingHeapAllocations = tmp;
        unlockThreadHeapMutex();
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
        CN1_GC_PARK_CAPTURE(threadStateData);   // PHASE 3b: native-stack capture at park
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
    o->__heapPosition = -1;
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
#ifdef CN1_BIBOP_VALIDATE
            // The (serial) mark drain crashed here calling obj->parentCls->markFunction
            // on an object whose parentCls is a non-null GARBAGE pointer (fp jumped into
            // libc). A live, correctly-enqueued object always carries gcMark ==
            // currentGcMarkValue (gcMarkObject stamps it BEFORE pushing) or -1 (grace),
            // and a real clazz's markFunction lies in the app text (never a libc/heap
            // address). Abort AT the source with the full object + fp state so the next
            // run says whether this is a freed/reused slot (stale gcMark) or a
            // corrupted-parentCls object.
            {
                gcMarkFunctionPointer __vfp = (obj != JAVA_NULL && !CN1_IS_TAGGED(obj)
                    && obj->__codenameOneParentClsReference != 0)
                    ? obj->__codenameOneParentClsReference->markFunction : (gcMarkFunctionPointer)0;
                int __vmark = (obj != JAVA_NULL && !CN1_IS_TAGGED(obj)) ? obj->__codenameOneGcMark : -999;
                int __vhp = (obj != JAVA_NULL && !CN1_IS_TAGGED(obj)) ? obj->__heapPosition : -999;
                // A real markFunction lives in the app text segment; anchor off a
                // known app function (&gcMarkObject) and flag any fp more than 256MB
                // away (the observed garbage fp was a libc-range address).
                uintptr_t __anchor = (uintptr_t)(void*)&gcMarkObject;
                uintptr_t __fpv = (uintptr_t)(void*)__vfp;
                int __fpBad = (__vfp != 0) &&
                    ((__fpv > __anchor ? __fpv - __anchor : __anchor - __fpv) > (256ULL << 20));
                if(obj == JAVA_NULL || CN1_IS_TAGGED(obj) ||
                   obj->__codenameOneParentClsReference == 0 ||
                   (__vhp != CN1_BIBOP_HEAP_POS && __vhp != CN1_BIBOP_ADOPTED && __vhp < -1) ||
                   (__vmark != currentGcMarkValue && __vmark != -1) ||
                   __fpBad) {
                    fprintf(stderr, "CN1BIBOP MARKDRAIN CORRUPT: obj=%p tagged=%d parentCls=%p "
                            "markFn=%p heapPosition=%d gcMark=%d curMark=%d FREE_MARK=%d force=%d\n",
                            (void*)obj, (int)CN1_IS_TAGGED(obj),
                            (obj && !CN1_IS_TAGGED(obj)) ? (void*)obj->__codenameOneParentClsReference : (void*)0,
                            (void*)__vfp, __vhp, __vmark, currentGcMarkValue,
                            CN1_BIBOP_FREE_MARK, (int)force);
                    fflush(stderr);
                    abort();
                }
            }
#endif
            gcMarkFunctionPointer fp = obj->__codenameOneParentClsReference->markFunction;
            if(fp != 0) {
#ifdef CN1_BIBOP_VALIDATE
                gcMarkCurrentDrainObj = obj;
#endif
#if CN1_ADOPT_POLICY != 0 && !defined(CN1_DISABLE_BIBOP)
                // Cascade: if this object has been MATURED, mature the children its mark
                // function is about to mark, so the whole reachable subtree graduates
                // together (never half a tree). Restored after the call.
                JAVA_BOOLEAN __savedMaturing = gcCurrentlyMaturing;
                gcCurrentlyMaturing = (obj->__heapPosition == CN1_BIBOP_ADOPTED) ? JAVA_TRUE : __savedMaturing;
#endif
                fp(threadStateData, obj, force);
#if CN1_ADOPT_POLICY != 0 && !defined(CN1_DISABLE_BIBOP)
                gcCurrentlyMaturing = __savedMaturing;
#endif
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
#elif 1
    // ISOLATION EXPERIMENT (git-A/B): default to SERIAL marking. The acquire-load
    // fix removed the parallel mark-worker crash, but arm64 Linux still corrupts the
    // heap (crash moved to a frameless method reading a smashed threadStateData), so
    // a SECOND ordering hole remains somewhere in the branch-only parallel-GC work.
    // Forcing one marker here bypasses the entire parallel path (gcMarkDrainParallel
    // -> gcMarkDrain, no atomics, no pool, no local buffers). If arm64 goes green,
    // parallel marking is the sole remaining corruptor and the concurrency audit
    // continues offline with CN1_GC_MARK_THREADS>1; if it still crashes, the bug is
    // elsewhere in the branch GC changes (nursery / tagged-int / BiBOP sweep).
    int n = 1;
#elif defined(_WIN32)
    // no sysconf in the Win32 shim; NUMBER_OF_PROCESSORS is always set on Windows
    const char* np = getenv("NUMBER_OF_PROCESSORS");
    long ncpu = np != 0 ? atol(np) : 2;
    int n = (int)(ncpu - 1);
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
#if CN1_ADOPT_POLICY != 0 && !defined(CN1_DISABLE_BIBOP)
                    // Same cascade as the serial gcMarkDrain: a matured object's children
                    // mature with it. gcCurrentlyMaturing is thread-local, so workers don't
                    // race. (Registration is deferred + locked, so this is only about which
                    // objects get flagged -- always safe.)
                    JAVA_BOOLEAN __savedMaturing = gcCurrentlyMaturing;
                    gcCurrentlyMaturing = (obj->__heapPosition == CN1_BIBOP_ADOPTED) ? JAVA_TRUE : __savedMaturing;
                    fp(d, obj, batch[i].force);
                    gcCurrentlyMaturing = __savedMaturing;
#else
                    fp(d, obj, batch[i].force);
#endif
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
extern void cn1InstallThreadAltStack(void);
static void* gcMarkWorkerMain(void* arg) {
    cn1InstallThreadAltStack();  // so a fault in the GC mark dumps a backtrace, not a silent die
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

// ---- FUSED OBJECTS -------------------------------------------------------
// A fused object is an owner whose ENCAPSULATED child (e.g. java.lang.String's
// char[] value -- never exposed outside the class) is laid out INSIDE the
// owner's own allocation block instead of being a separate heap object. The
// child keeps a full, ordinary object header so every reader treats it as a
// normal object, but it has NO independent GC identity:
//   * it is never registered anywhere (no table entry, no page slot of its own),
//     so the sweep -- which walks BiBOP slot boundaries / table entries only --
//     can never free it separately: it dies with its owner's slot;
//   * the conservative resolver maps any pointer into the block (including the
//     child header and its interior data) to the SLOT BASE, i.e. the OWNER, so
//     a stack/register reference to the child keeps the whole block alive;
//   * the owner's generated mark function still gcMarkObject()s the child --
//     harmless stores into our own block (nothing ever sweeps by that mark).
// The block must live in a BiBOP page for the resolver-covers-interior property,
// so this returns NULL for oversized requests (or when BiBOP is unavailable)
// and the caller falls back to ordinary two-object allocation. The returned
// block is fully zeroed (cn1BibopAlloc mid-build safety) with the owner's
// parentCls set; the caller lays out the child header + data.
// OWNERSHIP CONTRACT (verified per class, not enforced at runtime): the child
// reference must never be stored anywhere that can outlive the owner. Reading
// it, passing it as a transient call argument, or returning copies is fine.
JAVA_OBJECT cn1AllocFused(CODENAME_ONE_THREAD_STATE, int totalSize, struct clazz* cls) {
#if !defined(CN1_DISABLE_BIBOP) && !defined(DEBUG_GC_OBJECTS_IN_HEAP)
    if(totalSize <= CN1_BIBOP_MAX_OBJECT && constantPoolObjects != 0
#ifndef CN1_CONSERVATIVE_GC_ROOTS
       && !threadStateData->nativeAllocationMode
#endif
       ) {
        return cn1BibopAlloc(threadStateData, totalSize, cls);
    }
#endif
    return JAVA_NULL;
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

// Preallocated StackOverflowError (the JDK does the same): an SOE is thrown at
// STACK EXHAUSTION, where building a fresh error's stack trace calls more
// methods -- each of which trips the same overflow guard and throws again,
// recursing until the hard guard page (observed as a 500+ frame
// throwException/fillInStack/getStack storm ending in SIGSEGV on iOS).
// The preallocated instance has its stack field PRE-FILLED, so
// fillInStack's null-check skips trace building entirely: throwing it
// allocates nothing and calls nothing.
JAVA_OBJECT cn1PreallocSOE = JAVA_NULL;
extern void set_field_java_lang_Throwable_stack(JAVA_OBJECT __cn1Val, JAVA_OBJECT __cn1T);

void cn1ThrowStackOverflow(CODENAME_ONE_THREAD_STATE) {
    JAVA_OBJECT soe = cn1PreallocSOE;
    if(soe == JAVA_NULL) {
        // startup-only fallback (before initConstantPool preallocates)
        soe = __NEW_INSTANCE_java_lang_StackOverflowError(threadStateData);
    }
    throwException(threadStateData, soe);
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
       // java_util_ArrayList_add___java_lang_Object_R_boolean(threadStateData, internedStrings, oo);
    }
    #if defined(__OBJC__)
    //NSLog(@"Size of constant pool in c: %i and j: %i", cStringSize, jStringSize);
    #endif
    constantPoolObjects = tmpConstantPoolObjects;
    invokedGC = NO;

    // preallocate the shared StackOverflowError with a pre-filled trace (see
    // cn1ThrowStackOverflow above); built HERE where stack is plentiful
    cn1PreallocSOE = __NEW_INSTANCE_java_lang_StackOverflowError(threadStateData);
    set_field_java_lang_Throwable_stack(
        newStringFromCString(threadStateData, "java.lang.StackOverflowError\n    (trace suppressed: thrown at stack exhaustion)\n"),
        cn1PreallocSOE);
    cn1AddImmortalRoot(cn1PreallocSOE);

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
    // ensure the dead slot reaches cn1BibopReclaimSlot to release the peer
    cn1BibopNoteNativePeer(o);
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
