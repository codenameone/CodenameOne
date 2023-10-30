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
#import <mach/mach.h>
#import <mach/mach_host.h>

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
extern JAVA_BOOLEAN lowMemoryMode;

static JAVA_BOOLEAN isEdt(long threadId) {
    return (CN1_EDT_THREAD_ID == threadId);
}

// Gets the amount of free memory in the system.
 static natural_t get_free_memory(void)
 {
   mach_port_t host_port;
   mach_msg_type_number_t host_size;
   vm_size_t pagesize;
   host_port = mach_host_self();
   host_size = sizeof(vm_statistics_data_t) / sizeof(integer_t);
   host_page_size(host_port, &pagesize);
   vm_statistics_data_t vm_stat;
   if (host_statistics(host_port, HOST_VM_INFO, (host_info_t)&vm_stat, &host_size) != KERN_SUCCESS)
   {
     NSLog(@"Failed to fetch vm statistics");
     return 0;
   }
   /* Stats in bytes */
   natural_t mem_free = vm_stat.free_count * pagesize;
   return mem_free;
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

struct elementStruct* pop(struct elementStruct** sp) {
    --(*sp);
    return *sp;
}

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
            oldAllObjectsInHeap = allObjectsInHeap;
            allObjectsInHeap = tmpAllObjectsInHeap;
            allObjectsInHeap[currentSizeOfAllObjectsInHeap] = obj;
            currentSizeOfAllObjectsInHeap++;
            free(oldAllObjectsInHeap);
        } else {
            allObjectsInHeap[pos] = obj;
        }
        obj->__heapPosition = pos;
    }
}

extern struct ThreadLocalData** allThreads; 
extern int nThreadsToKill;

JAVA_BOOLEAN hasAgressiveAllocator;

// the thread just died, mark its remaining resources
void collectThreadResources(struct ThreadLocalData *current)
{
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
    //NSLog(@"GC mark, %d dead processes pending",nThreadsToKill);
    
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
                            NSLog(@"GC trapped for %d seconds waiting for thread %d in slot %d (%d)",
                                  (int)(later/1000),(int)t->threadId,iter,t->threadKilled);
                            }
                        }
                    }
                }
                
                // place allocations from the local thread into the global heap list
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
                    NSLog(@"[GC] Blocking EDT as aggressive allocator, free memory=%lld", freeMemory);
                    
                }
                
                t->heapAllocationSize = 0;
                
                int stackSize = t->threadObjectStackOffset;
                for(int stackIter = 0 ; stackIter < stackSize ; stackIter++) {
                    struct elementStruct* current = &t->threadObjectStack[stackIter];
                    CODENAME_ONE_ASSERT(current->type >= CN1_TYPE_INVALID && current->type <= CN1_TYPE_PRIMITIVE);
                    if(current != 0 && current->type == CN1_TYPE_OBJECT && current->data.o != JAVA_NULL) {
                        gcMarkObject(t, current->data.o, JAVA_FALSE);
                        //marked++;
                    }
                }
                markStatics(d);
                if(!agressiveAllocator) {
                    t->threadBlockedByGC = JAVA_FALSE;
                } else {
                    hasAgressiveAllocator = JAVA_TRUE;
                }
            }
        }
    }
    //NSLog(@"Mark set %i objects to %i", marked, currentGcMarkValue);
    // since they are immutable this probably doesn't need as much sync as the statics...
    for(int iter = 0 ; iter < CN1_CONSTANT_POOL_SIZE ; iter++) {
        gcMarkObject(d, (JAVA_OBJECT)constantPoolObjects[iter], JAVA_TRUE);
    }
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
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    
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
    NSLog(@"\n\n**** There are %i - %i = %i nulls available entries out of %i objects in heap which take up %i, sweep saved %i ****", nullSpaces, nullSpacesPreSweep, nullSpaces - nullSpacesPreSweep, t, totalAllocatedHeap, preSweepRam - totalAllocatedHeap);
    for(int iter = 0 ; iter < cn1_array_3_id_java_util_Vector ; iter++) {
        if(classTypeCount[iter] > 0) {
            if(classTypeCountPreSweep[iter] - classTypeCount[iter] > 0) {
                if(iter > cn1_array_start_offset) {
                    NSLog(@"There are %i instances of %@ taking up %i bytes, %i were cleaned which saved %i bytes", classTypeCount[iter], [NSString stringWithUTF8String:arrayOfNames[iter]], sizeInHeapForType[iter], classTypeCountPreSweep[iter] - classTypeCount[iter], sizeInHeapForTypePreSweep[iter] - sizeInHeapForType[iter]);
                } else {
                    JAVA_OBJECT str = STRING_FROM_CONSTANT_POOL_OFFSET(classNameLookup[iter]);
                    NSLog(@"There are %i instances of %@ taking up %i bytes, %i were cleaned which saved %i bytes", classTypeCount[iter], toNSString(threadStateData, str), sizeInHeapForType[iter], classTypeCountPreSweep[iter] - classTypeCount[iter], sizeInHeapForTypePreSweep[iter] - sizeInHeapForType[iter]);
                }
            }
            actualTotalMemory += sizeInHeapForType[iter];
        }
    }
    //NSLog(@"Actual ram = %i vs total mallocs = %i", actualTotalMemory, totalAllocatedHeap);
    NSLog(@"**** GC cycle complete ****");
    
    free(arrayOfNames);
    [pool release];
}

void printObjectTypesInHeap(CODENAME_ONE_THREAD_STATE) {
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    
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
    NSLog(@"There are %i null available entries out of %i objects in heap which take up %i", nullSpaces, t, totalAllocatedHeap);
    for(int iter = 0 ; iter < cn1_array_3_id_java_util_Vector ; iter++) {
        if(classTypeCount[iter] > 0) {
            float f = ((float)classTypeCount[iter]) / ((float)t) * 100.0f;
            float f2 = ((float)sizeInHeapForType[iter]) / ((float)totalAllocatedHeap) * 100.0f;
            if(iter > cn1_array_start_offset) {
                NSLog(@"There are %i instances of %@ which is %i percent its %i bytes which is %i mem percent", classTypeCount[iter], [NSString stringWithUTF8String:arrayOfNames[iter]], (int)f, sizeInHeapForType[iter], (int)f2);
            } else {
                JAVA_OBJECT str = STRING_FROM_CONSTANT_POOL_OFFSET(classNameLookup[iter]);
                NSLog(@"There are %i instances of %@ which is %i percent its %i bytes which is %i mem percent", classTypeCount[iter], toNSString(threadStateData, str), (int)f, sizeInHeapForType[iter], (int)f2);
            }
            actualTotalMemory += sizeInHeapForType[iter];
        }
    }
    NSLog(@"Actual ram = %i vs total mallocs = %i", actualTotalMemory, totalAllocatedHeap);
    
    free(arrayOfNames);
    [pool release];
}
#endif

/**
 * The sweep GC phase iterates the memory block and deletes unmarked memory
 * since it always runs from the same thread and concurrent work doesn't matter
 * it can just delete everything it finds
 */
void codenameOneGCSweep() {
    struct ThreadLocalData* threadStateData = getThreadLocalData();
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
                    CODENAME_ONE_ASSERT(o->__codenameOneGcMark > 0);
                    allObjectsInHeap[iter] = JAVA_NULL;
                    //if(o->__codenameOneReferenceCount > 0) {
                    //    NSLog(@"Sweped %X", (int)o);
                    //}
                    
#ifdef DEBUG_GC_ALLOCATIONS
                    int classId = o->className;
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
                            NSLog(@"Sweeping: %X, Mark: %i, Allocated: %@ %i type: %@, which is: '%@'", (int)o, o->__codenameOneGcMark, whereIs, o->line, [NSString stringWithUTF8String:o->__codenameOneParentClsReference->clsName], [NSString stringWithUTF8String:data]);
                        } else {
                            NSLog(@"Sweeping: %X, Mark: %i, Allocated: %@ %i , type: %@", (int)o, o->__codenameOneGcMark, whereIs, o->line, [NSString stringWithUTF8String:o->__codenameOneParentClsReference->clsName]);
                        }
                    } else {
                        JAVA_OBJECT str = java_lang_Object_toString___R_java_lang_String(threadStateData, o);
                        NSString* ns = toNSString(threadStateData, str);
                        if(ns == nil) {
                            ns = @"[NULL]";
                        }
                        NSLog(@"Sweeping: %X, Mark: %i, Allocated: %@ %i , type: %@, toString: '%@'", (int)o, o->__codenameOneGcMark, whereIs, o->line, [NSString stringWithUTF8String:o->__codenameOneParentClsReference->clsName], ns);
                    }
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

JAVA_BOOLEAN java_lang_System_isHighFrequencyGC___R_boolean(CODENAME_ONE_THREAD_STATE) {
    int alloc = allocationsSinceLastGC;
    allocationsSinceLastGC = 0;
    return alloc > CN1_HIGH_FREQUENCY_ALLOCATION_THRESHOLD && totalAllocations > CN1_HIGH_FREQUENCY_ALLOCATION_ACTIVATED_THRESHOLD;
}

extern int mallocWhileSuspended;
extern BOOL isAppSuspended;

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
#else
    JAVA_OBJECT o = (JAVA_OBJECT)malloc(size);
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
    memset(o, 0, size);
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
    if(obj->__codenameOneThreadData) {
        free(obj->__codenameOneThreadData);
        obj->__codenameOneThreadData = 0;
    }
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
void gcMarkObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {
    if(obj == JAVA_NULL || obj->__codenameOneParentClsReference == 0 || obj->__codenameOneParentClsReference == (&class__java_lang_Class)) {
        return;
    }

    // if this is a Class object or already marked this should be ignored
    if(obj->__codenameOneGcMark == currentGcMarkValue) {
        if(force) {
            if(obj->__codenameOneReferenceCount == recursionKey) {
                return;
            }
            obj->__codenameOneReferenceCount = recursionKey;
            obj->__codenameOneGcMark = currentGcMarkValue;
            gcMarkFunctionPointer fp = obj->__codenameOneParentClsReference->markFunction;
            if(fp != 0) {
                fp(threadStateData, obj, force);
            }
        }
        return;
        
    }
    obj->__codenameOneGcMark = currentGcMarkValue;
    gcMarkFunctionPointer fp = obj->__codenameOneParentClsReference->markFunction;
    if(fp != 0) {
        fp(threadStateData, obj, force);
    }
}

void gcMarkArrayObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {
    if(obj == JAVA_NULL) {
        return;
    }
    obj->__codenameOneGcMark = currentGcMarkValue;
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
    //NSLog(@"Size of constant pool in c: %i and j: %i", cStringSize, jStringSize);
    constantPoolObjects = tmpConstantPoolObjects;
    invokedGC = NO;

    enteringNativeAllocations();

    // it will wait two seconds unless an explicit GC occurs
    java_lang_System_startGCThread__(threadStateData);
    finishedNativeAllocations();
}

JAVA_OBJECT utf8String = NULL;

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
    //NSLog(@"Throwing exception!"); 
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
