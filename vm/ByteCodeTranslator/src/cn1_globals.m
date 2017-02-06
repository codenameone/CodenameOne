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

int currentGcMarkValue = 1;
extern JAVA_BOOLEAN lowMemoryMode;
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

int instanceofFunction(int sourceClass, int destId) {
    if(sourceClass >= cn1_array_start_offset || destId >= cn1_array_start_offset) {
        return sourceClass == destId;
    }
    if(sourceClass == destId) {
        return JAVA_TRUE;
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
extern struct ThreadLocalData** threadsToDelete;

JAVA_BOOLEAN hasAgressiveAllocator;

/**
 * A simple concurrent mark algorithm that traverses the currently running threads
 */
void codenameOneGCMark() {
    currentGcMarkValue++;

    hasAgressiveAllocator = JAVA_FALSE;
    struct ThreadLocalData* d = getThreadLocalData();
    //int marked = 0;
    
    // copy the allocated objects from already deleted threads so we can delete that data
    if(threadsToDelete != 0) {
        for(int i = 0 ; i < NUMBER_OF_SUPPORTED_THREADS ; i++) {
            if(threadsToDelete[i] != 0) {
                struct ThreadLocalData* current = threadsToDelete[i];
                for(int heapTrav = 0 ; heapTrav < current->heapAllocationSize ; heapTrav++) {
                    JAVA_OBJECT obj = (JAVA_OBJECT)current->pendingHeapAllocations[heapTrav];
                    if(obj) {
                        current->pendingHeapAllocations[heapTrav] = 0;
                        placeObjectInHeapCollection(obj);
                    }
                }
            }
        }
    }

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
                    while(t->threadActive) {
                        usleep(500);
                    }
                }
                
                // place allocations from the local thread into the global heap list
                for(int heapTrav = 0 ; heapTrav < t->heapAllocationSize ; heapTrav++) {
                    JAVA_OBJECT obj = (JAVA_OBJECT)t->pendingHeapAllocations[heapTrav];
                    if(obj) {
                        t->pendingHeapAllocations[heapTrav] = 0;
                        placeObjectInHeapCollection(obj);
                    }
                }
                
                // this is a thread that allocates a lot and might demolish RAM. We will hold it until the sweep is finished...
                JAVA_BOOLEAN agressiveAllocator = t->heapAllocationSize > 5000;
                
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
    
    //NSLog(@"Sweep removed %i objects", counter);
     
    /*if(threadsToDelete != 0) {
        lockCriticalSection();
        for(int i = 0 ; i < NUMBER_OF_SUPPORTED_THREADS ; i++) {
            if(threadsToDelete[i] != 0) {
                //NSLog(@"Deleting thread: %i", i);
                struct ThreadLocalData* current = threadsToDelete[i];
                free(current->blocks);
                free(current->threadObjectStack);
                free(current->callStackClass);
                free(current->callStackLine);
                free(current->callStackMethod);
                free(current->pendingHeapAllocations);
                free(current);
                threadsToDelete[i] = 0;
            }
        }
        unlockCriticalSection();
    }*/
    
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
    return alloc > 1024 * 1024 && totalAllocations > 10 * 1024 * 1024;
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
        if(threadStateData->heapAllocationSize > 10000 && constantPoolObjects != 0 && !threadStateData->nativeAllocationMode) {
            threadStateData->threadActive=JAVA_FALSE;
            while(gcCurrentlyRunning) {
                usleep((JAVA_INT)(1000));
            }
            threadStateData->threadActive=JAVA_TRUE;
            
            if(threadStateData->heapAllocationSize > 0) {
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
                void** tmp = malloc(threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
                memset(tmp, 0, threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
                memcpy(tmp, threadStateData->pendingHeapAllocations, threadStateData->threadHeapTotalSize * sizeof(void *));
                threadStateData->threadHeapTotalSize *= 2;
                free(threadStateData->pendingHeapAllocations);
                threadStateData->pendingHeapAllocations = tmp;
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
    } else {
        if(threadStateData->utf8BufferSize < len + 1) {
            free(threadStateData->utf8Buffer);
            threadStateData->utf8Buffer = malloc(len + 1);
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
        if(threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass <= 0 || instanceofFunction(threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass, exceptionArg->__codenameOneParentClsReference->classId)) {
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
