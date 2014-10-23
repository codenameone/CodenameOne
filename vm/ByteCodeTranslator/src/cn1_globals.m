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

struct clazz class_array1__JAVA_BOOLEAN = {
   0, 999999, 0, 0, 0, 0, cn1_array_1_id_JAVA_BOOLEAN, "boolean[]", JAVA_TRUE, 1, &class__java_lang_Boolean, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_BOOLEAN = {
   0, 999999, 0, 0, 0, 0, cn1_array_2_id_JAVA_BOOLEAN, "boolean[]", JAVA_TRUE, 2, &class__java_lang_Boolean, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_BOOLEAN = {
   0, 999999, 0, 0, 0, 0, cn1_array_3_id_JAVA_BOOLEAN, "boolean[]", JAVA_TRUE, 3, &class__java_lang_Boolean, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_CHAR = {
   0, 999999, 0, 0, 0, 0, cn1_array_1_id_JAVA_CHAR, "char[]", JAVA_TRUE, 1, &class__java_lang_Character, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_CHAR = {
   0, 999999, 0, 0, 0, 0, cn1_array_2_id_JAVA_CHAR, "char[]", JAVA_TRUE, 2, &class__java_lang_Character, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_CHAR = {
   0, 999999, 0, 0, 0, 0, cn1_array_3_id_JAVA_CHAR, "char[]", JAVA_TRUE, 3, &class__java_lang_Character, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_BYTE = {
   0, 999999, 0, 0, 0, 0, cn1_array_1_id_JAVA_BYTE, "byte[]", JAVA_TRUE, 1, &class__java_lang_Byte, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_BYTE = {
   0, 999999, 0, 0, 0, 0, cn1_array_2_id_JAVA_BYTE, "byte[]", JAVA_TRUE, 2, &class__java_lang_Byte, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_BYTE = {
   0, 999999, 0, 0, 0, 0, cn1_array_3_id_JAVA_BYTE, "byte[]", JAVA_TRUE, 3, &class__java_lang_Byte, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_SHORT = {
   0, 999999, 0, 0, 0, 0, cn1_array_1_id_JAVA_SHORT, "short[]", JAVA_TRUE, 1, &class__java_lang_Short, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_SHORT = {
   0, 999999, 0, 0, 0, 0, cn1_array_2_id_JAVA_SHORT, "short[]", JAVA_TRUE, 2, &class__java_lang_Short, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_SHORT = {
   0, 999999, 0, 0, 0, 0, cn1_array_3_id_JAVA_SHORT, "short[]", JAVA_TRUE, 3, &class__java_lang_Short, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_INT = {
   0, 999999, 0, 0, 0, 0, cn1_array_1_id_JAVA_INT, "int[]", JAVA_TRUE, 1, &class__java_lang_Integer, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_INT = {
   0, 999999, 0, 0, 0, 0, cn1_array_2_id_JAVA_INT, "int[]", JAVA_TRUE, 2, &class__java_lang_Integer, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_INT = {
   0, 999999, 0, 0, 0, 0, cn1_array_3_id_JAVA_INT, "int[]", JAVA_TRUE, 3, &class__java_lang_Integer, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_LONG = {
   0, 999999, 0, 0, 0, 0, cn1_array_1_id_JAVA_LONG, "long[]", JAVA_TRUE, 1, &class__java_lang_Long, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_LONG = {
   0, 999999, 0, 0, 0, 0, cn1_array_2_id_JAVA_LONG, "long[]", JAVA_TRUE, 2, &class__java_lang_Long, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_LONG = {
   0, 999999, 0, 0, 0, 0, cn1_array_3_id_JAVA_LONG, "long[]", JAVA_TRUE, 3, &class__java_lang_Long, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_FLOAT = {
   0, 999999, 0, 0, 0, 0, cn1_array_1_id_JAVA_FLOAT, "float[]", JAVA_TRUE, 1, &class__java_lang_Float, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_FLOAT = {
   0, 999999, 0, 0, 0, 0, cn1_array_2_id_JAVA_FLOAT, "float[]", JAVA_TRUE, 2, &class__java_lang_Float, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_FLOAT = {
   0, 999999, 0, 0, 0, 0, cn1_array_3_id_JAVA_FLOAT, "float[]", JAVA_TRUE, 3, &class__java_lang_Float, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_DOUBLE = {
   0, 999999, 0, 0, 0, 0, cn1_array_1_id_JAVA_DOUBLE, "double[]", JAVA_TRUE, 1, &class__java_lang_Double, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_DOUBLE = {
   0, 999999, 0, 0, 0, 0, cn1_array_2_id_JAVA_DOUBLE, "double[]", JAVA_TRUE, 2, &class__java_lang_Double, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_DOUBLE = {
   0, 999999, 0, 0, 0, 0, cn1_array_3_id_JAVA_DOUBLE, "double[]", JAVA_TRUE, 3, &class__java_lang_Double, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

void safeRelease(CODENAME_ONE_THREAD_STATE, struct elementStruct* es) {
    if(es != 0 && es->type == CN1_TYPE_OBJECT) {
        releaseObj(threadStateData, es->data.o);
    }
}

void safeRetain(struct elementStruct* es) {
    if(es != 0) {
        CODENAME_ONE_ASSERT(es->type != CN1_TYPE_INVALID);
        if(es->type == CN1_TYPE_OBJECT) {
            retainObj(es->data.o);
        }
    }
}

struct elementStruct* pop(struct elementStruct* array, int* sp) {
    --(*sp);
    struct elementStruct* retVal = &array[*sp];
    return retVal;
}

struct elementStruct* popAndRelease(CODENAME_ONE_THREAD_STATE, struct elementStruct* array, int* sp) {
    --(*sp);
    struct elementStruct* retVal = &array[*sp];
    releaseObj(threadStateData, retVal->data.o);
    retVal->type = CN1_TYPE_INVALID;
    return retVal;
}

void popMany(CODENAME_ONE_THREAD_STATE, int count, struct elementStruct* array, int* sp) {
    while(count > 0) {
        --(*sp);
        safeRelease(threadStateData, &array[*sp]);
        count--;
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

void retainObj(JAVA_OBJECT o) {
    if(o == JAVA_NULL) {
        return;
    }
    if(o->__codenameOneReferenceCount > 999990) {
        return;
    }
    
    //lockCriticalSection();
    (*o).__codenameOneReferenceCount++;
    //unlockCriticalSection();
}

JAVA_OBJECT* releaseQueue = 0;
JAVA_INT releaseQueueSize = 0;
typedef void (*finalizerFunctionPointer)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);

// invokes finalizers and iterates over the release queue
void flushReleaseQueue() {
    // double locking pattern, check first to save the lock cost
    if(releaseQueueSize == 0) {
        return;
    }
    lockCriticalSection();
    if(releaseQueueSize == 0) {
        unlockCriticalSection();
        return;
    }
    JAVA_OBJECT* localQueue = releaseQueue;
    JAVA_INT localCount = releaseQueueSize;
    releaseQueue = 0;
    releaseQueueSize = 0;
    unlockCriticalSection();
    
    struct ThreadLocalData* threadStateData = getThreadLocalData();
    
    DEFINE_METHOD_STACK(1, 1, 0, cn1_class_id_java_lang_Object, 1);
    // exceptions might be thrown by the finalizer methods
    DEFINE_EXCEPTION_HANDLING_CONSTANTS();

    DEFINE_CATCH_BLOCK(grabAllExceptions, label_continueToDeletion, 0);

    // now we can just free asynchronously...
    for(int iter = 0 ; iter < localCount ; iter++) {
        if(localQueue[iter] == JAVA_NULL) {
            continue;
        }
        
        // double deletion can occur because of GC/ARC
        if(!removeObjectFromHeapCollection(localQueue[iter])) {
            continue;
        }
        

        finalizerFunctionPointer ptr = (finalizerFunctionPointer)localQueue[iter]->__codenameOneParentClsReference->finalizerFunction;
        if(ptr != 0) {
            BEGIN_TRY(-1, grabAllExceptions);
            ptr(threadStateData, localQueue[iter]);
            END_TRY();
        }
label_continueToDeletion:
        codenameOneGcFree(threadStateData, localQueue[iter]);
    }
    free(localQueue);
    RETURN_FROM_VOID(1);
}

/**
 * Invoked to destroy an array and release all the objects within it
 */
void arrayFinalizerFunction(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array) {
    JAVA_ARRAY arr = (JAVA_ARRAY)array;
    int l = arr->length;
    JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)arr->data;
    for(int iter = 0 ; iter < l ; iter++) {
        releaseObj(threadStateData, data[iter]);
        data[iter] = JAVA_NULL;
    }
}

BOOL invokedGC = NO;
extern int findPointerPosInHeap(JAVA_OBJECT obj);
extern pthread_mutex_t* getMemoryAccessMutex();
extern long gcThreadId;
void releaseObj(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    // if this is a Class object we will never delete it, reference count might have already reached 0 and
    // we got here thru the GC
    if(o == 0 || o->__codenameOneParentClsReference == 0 || (*o).__codenameOneReferenceCount == 0) {
        return;
    }
    
    if(o->__codenameOneReferenceCount > 999990) {
        return;
    }
    if(o->__ownerThread != threadStateData) {
        o->__ownerThread = 0;
        o->__codenameOneReferenceCount = 999999;
        return;
    }
        
    (*o).__codenameOneReferenceCount--;
    if((*o).__codenameOneReferenceCount == 0) {
        // remove object from local heap if its very short lived
        /*for(int heapTrav = 0 ; heapTrav < threadStateData->heapAllocationSize ; heapTrav++) {
            JAVA_OBJECT obj = (JAVA_OBJECT)threadStateData->pendingHeapAllocations[heapTrav];
            if(obj == o) {
                threadStateData->pendingHeapAllocations[heapTrav] = 0;
            }
        }*/
        threadStateData->pendingHeapReleases[threadStateData->heapReleaseSize] = o;
        threadStateData->heapReleaseSize++;
    }

    // if we are going overboard with object creation/release e.g. creating objects in loops without giving GC time to work
    if(gcThreadId != threadStateData->threadId && !invokedGC && releaseQueueSize > CN1_FINALIZER_QUEUE_SIZE / 2) {
        invokedGC = YES;
        java_lang_System_gc__(getThreadLocalData());
        while(releaseQueueSize > CN1_FINALIZER_QUEUE_SIZE - 200) {
            usleep((JAVA_INT)(1000));
        }
        invokedGC = NO;
    }
}

void gcReleaseObj(JAVA_OBJECT o) {
    // if we are going overboard with object creation/release e.g. creating objects in loops without giving GC time to work
    if(releaseQueueSize > CN1_FINALIZER_QUEUE_SIZE - 200) {
        // this is on the GC queue so we can just invoke it directly
        flushReleaseQueue();
    }
    
    //lockCriticalSection();
    if(releaseQueue == 0) {
        releaseQueue = malloc(CN1_FINALIZER_QUEUE_SIZE * sizeof(JAVA_OBJECT));
    }
    for(int iter = 0 ; iter < releaseQueueSize ; iter++) {
        if(releaseQueue[iter] == o) {
            //unlockCriticalSection();
            return;
        }
    }
    releaseQueue[releaseQueueSize] = o;
    releaseQueueSize++;
    //unlockCriticalSection();
}

JAVA_OBJECT allocObj(int size) {
    JAVA_OBJECT o = (JAVA_OBJECT)malloc(size);
    memset(o, 0, size);
    //retainObj(o);
    return o;
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
    /*if(oldAllObjectsInHeap != 0) {
        free(oldAllObjectsInHeap);
        oldAllObjectsInHeap = 0;
    }
    JAVA_OBJECT* currentAllObjectsInHeap = allObjectsInHeap;
    int currentSize = currentSizeOfAllObjectsInHeap;
    for(int iter = 0 ; iter < currentSize ; iter++) {
        if(currentAllObjectsInHeap[iter] == obj) {
            return iter;
        }
    }*/
    return -1;
}

void placeObjectInHeapCollection(JAVA_OBJECT obj) {
    if(allObjectsInHeap == 0) {
        allObjectsInHeap = malloc(sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
        memset(allObjectsInHeap, 0, sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
    }
    if(currentSizeOfAllObjectsInHeap < sizeOfAllObjectsInHeap) {
        allObjectsInHeap[currentSizeOfAllObjectsInHeap] = obj;
        currentSizeOfAllObjectsInHeap++;
    } else {
        int pos = findPointerPosInHeap(JAVA_NULL);
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
        } else {
            allObjectsInHeap[pos] = obj;
        }
        obj->__heapPosition = pos;
    }    
}

extern struct ThreadLocalData** allThreads; 
extern struct ThreadLocalData** threadsToDelete;

/**
 * A simple concurrent mark algorithm that traverses the currently running threads
 */
void codenameOneGCMark() {
    struct ThreadLocalData* d = getThreadLocalData();
    if(constantPoolObjects != 0) {
        for(int iter = 0 ; iter < CN1_CONSTANT_POOL_SIZE ; iter++) {
            gcMarkObject(d, (JAVA_OBJECT)constantPoolObjects[iter]);
        }
    }
    markStatics(d);
    //int marked = 0;
    for(int iter = 0 ; iter < NUMBER_OF_SUPPORTED_THREADS ; iter++) {
        lockCriticalSection();
        struct ThreadLocalData* t = allThreads[iter];
        unlockCriticalSection();
        if(t != 0 && t != d) {
            t->threadBlockedByGC = JAVA_TRUE;
            struct elementStruct* objects = t->threadObjectStack;
            
            // wait for the thread to pause so we can traverse its stack but not for native threads where
            // we don't have much control and who barely call into Java anyway
            if(t->lightweightThread) {
                while(t->threadActive) {
                    if(releaseQueueSize > CN1_FINALIZER_QUEUE_SIZE / 2) {
                        flushReleaseQueue();
                    }
                    usleep(500);
                }
            }
            
            // place allocations from the local thread into the global heap list
            for(int heapTrav = 0 ; heapTrav < t->heapAllocationSize ; heapTrav++) {
                JAVA_OBJECT obj = (JAVA_OBJECT)t->pendingHeapAllocations[heapTrav];
                if(obj) {
                    t->pendingHeapAllocations[heapTrav] = 0;
                    if(obj->__codenameOneReferenceCount > 0) {
                        placeObjectInHeapCollection(obj);
                    }
                }
            }
            t->heapAllocationSize = 0;
            
            // delete all the objects released by the threads reference counter
            for(int heapTrav = 0 ; heapTrav < t->heapReleaseSize ; heapTrav++) {
                JAVA_OBJECT obj = (JAVA_OBJECT)t->pendingHeapReleases[heapTrav];
                if(obj) {
                    removeObjectFromHeapCollection(obj);
                    t->pendingHeapReleases[heapTrav] = 0;
                    gcReleaseObj(obj);
                }
            }
            t->heapReleaseSize = 0;
            
            int stackSize = t->threadObjectStackOffset;
            for(int stackIter = 0 ; stackIter < stackSize ; stackIter++) {
                struct elementStruct* current = &t->threadObjectStack[stackIter];
                CODENAME_ONE_ASSERT(current->type >= CN1_TYPE_INVALID && current->type <= CN1_TYPE_PRIMITIVE);
                if(current != 0 && current->type == CN1_TYPE_OBJECT && current->data.o != JAVA_NULL) {
                    gcMarkObject(t, current->data.o);
                    //marked++;
                }
            }
            t->threadBlockedByGC = JAVA_FALSE;
        }
    }
    //NSLog(@"Mark set %i objects to %i", marked, currentGcMarkValue);
}

/**
 * The sweep GC phase iterates the memory block and deletes unmarked memory
 * since it always runs from the same thread and concurrent work doesn't matter
 * it can just delete everything it finds
 */
void codenameOneGCSweep() {
    //int counter = 0;
    int t = currentSizeOfAllObjectsInHeap;
    for(int iter = 0 ; iter < t ; iter++) {
        JAVA_OBJECT o = allObjectsInHeap[iter];
        if(o != JAVA_NULL) {
            if(o->__codenameOneGcMark != currentGcMarkValue) {
                CODENAME_ONE_ASSERT(o->__codenameOneGcMark > 0);
                allObjectsInHeap[iter] = JAVA_NULL;
                //if(o->__codenameOneReferenceCount > 0) {
                //    NSLog(@"Sweped %X", (int)o);
                //}
                gcReleaseObj(o);
                //counter++;
            }
        }
    }
    
    //NSLog(@"Sweep removed %i objects", counter);
     
    currentGcMarkValue++;

    if(threadsToDelete != 0) {
        lockCriticalSection();
        for(int i = 0 ; i < NUMBER_OF_SUPPORTED_THREADS ; i++) {
            if(threadsToDelete[i] != 0) {
                NSLog(@"Deleting thread: %i", i);
                struct ThreadLocalData* current = threadsToDelete[i];
                free(current->blocks);
                free(current->threadObjectStack);
                free(current->callStackClass);
                free(current->callStackLine);
                free(current->callStackMethod);
                free(current->pendingHeapAllocations);
                free(current->pendingHeapReleases);
                free(current);
                threadsToDelete[i] = 0;
            }
        }
        unlockCriticalSection();
    }
}

JAVA_BOOLEAN removeObjectFromHeapCollection(JAVA_OBJECT o) {
    int pos = findPointerPosInHeap(o);

    // double deletion might occur when the GC and the reference counting collide
    if(pos < 0) {
        return JAVA_FALSE;
    }
    o->__heapPosition = -1;

    allObjectsInHeap[pos] = JAVA_NULL;
    
    return JAVA_TRUE;
}

JAVA_OBJECT codenameOneGcMalloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent) {
    JAVA_OBJECT o = (JAVA_OBJECT)malloc(size);
    memset(o, 0, size);
    o->__codenameOneParentClsReference = parent;
    o->__codenameOneGcMark = currentGcMarkValue;
    o->__ownerThread = threadStateData;
    o->__heapPosition = -1;
    
    if(threadStateData->heapAllocationSize == threadStateData->threadHeapTotalSize) {
        void** tmp = malloc(threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
        memset(tmp, 0, threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
        memcpy(tmp, threadStateData->pendingHeapAllocations, threadStateData->threadHeapTotalSize * sizeof(void *));
        threadStateData->threadHeapTotalSize *= 2;
        free(threadStateData->pendingHeapAllocations);
        threadStateData->pendingHeapAllocations = tmp;
    }
    threadStateData->pendingHeapAllocations[threadStateData->heapAllocationSize] = o;
    threadStateData->heapAllocationSize++;
    
    return o;
}

void codenameOneGcFree(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    if(obj->__codenameOneParentClsReference != 0 && obj->__codenameOneParentClsReference->isArray) {
        free(((JAVA_ARRAY)obj)->data);
        ((JAVA_ARRAY)obj)->data = 0;
    }
    if(obj->__codenameOneThreadData) {
        free(obj->__codenameOneThreadData);
        obj->__codenameOneThreadData = 0;
    }
    free(obj);
}

typedef void (*gcMarkFunctionPointer)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);

void gcMarkObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    // if this is a Class object or already marked this should be ignored
    if(obj == JAVA_NULL || obj->__codenameOneParentClsReference == 0 || obj->__codenameOneGcMark == currentGcMarkValue) {
        return;
    }
    obj->__codenameOneGcMark = currentGcMarkValue;
    gcMarkFunctionPointer fp = obj->__codenameOneParentClsReference->markFunction;
    if(fp != 0) {
        fp(threadStateData, obj);
    }
}

void gcMarkArrayObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    if(obj == JAVA_NULL) {
        return;
    }
    obj->__codenameOneGcMark = currentGcMarkValue;
    JAVA_ARRAY arr = (JAVA_ARRAY)obj;
    if(arr->length > 0) {
        JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)arr->data;
        for(int iter = 0 ; iter < arr->length ; iter++) {
            if(data[iter] != JAVA_NULL) {
                gcMarkObject(threadStateData, data[iter]);
            }
        }
    }
}

JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim) {
    JAVA_ARRAY array = (JAVA_ARRAY)codenameOneGcMalloc(threadStateData, sizeof(struct JavaArrayPrototype), type);
    (*array).length = length;
    (*array).dimensions = dim;
    int actualSize = length * primitiveSize;
    if(actualSize > 0) {
        (*array).data = malloc(actualSize);
        memset((*array).data, 0, actualSize);
    } else {
        (*array).data = 0;
    }
    return (JAVA_OBJECT)array;
}

JAVA_OBJECT alloc2DArray(CODENAME_ONE_THREAD_STATE, int length1, int length2, struct clazz* parentType, struct clazz* childType, int primitiveSize) {
    JAVA_ARRAY base = (JAVA_ARRAY)allocArray(threadStateData, length1, parentType, sizeof(JAVA_OBJECT), 2);
    JAVA_ARRAY_OBJECT* objs = base->data;
    if(length2 > -1) {
        for(int iter = 0 ; iter < length1 ; iter++) {
            objs[iter] = allocArray(threadStateData, length2, childType, primitiveSize, 1);
            retainObj(objs[iter]);
        }
    }
    return (JAVA_OBJECT)base;
}

JAVA_OBJECT alloc3DArray(CODENAME_ONE_THREAD_STATE, int length1, int length2, int length3, struct clazz* parentType, struct clazz* childType, struct clazz* grandChildType, int primitiveSize) {
    JAVA_ARRAY base = (JAVA_ARRAY)allocArray(threadStateData, length1, parentType, sizeof(JAVA_OBJECT), 3);
    JAVA_ARRAY_OBJECT* objs = base->data;
    if(length2 > -1) {
        for(int iter = 0 ; iter < length1 ; iter++) {
            objs[iter] = allocArray(threadStateData, length2, childType, sizeof(JAVA_OBJECT), 2);
            retainObj(objs[iter]);
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

JAVA_OBJECT alloc4DArray(CODENAME_ONE_THREAD_STATE, int length1, int length2, int length3, int length4, struct clazz* parentType, struct clazz* childType, struct clazz* grandChildType, struct clazz* greatGrandChildType, int primitiveSize) {
    JAVA_ARRAY base = (JAVA_ARRAY)allocArray(threadStateData, length1, parentType, sizeof(JAVA_OBJECT), 4);
    JAVA_ARRAY_OBJECT* objs = base->data;
    if(length2 > -1) {
        for(int iter = 0 ; iter < length1 ; iter++) {
            objs[iter] = allocArray(threadStateData, length2, childType, sizeof(JAVA_OBJECT), 3);
            retainObj(objs[iter]);
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
    JAVA_ARRAY dat = (JAVA_ARRAY)allocArray(threadStateData, length, &class_array1__JAVA_CHAR, sizeof(JAVA_CHAR), 1);
    memcpy((*dat).data, data, length * sizeof(JAVA_ARRAY_CHAR));
    JAVA_OBJECT o = __NEW_java_lang_String(threadStateData);
    java_lang_String___INIT_____char_1ARRAY(threadStateData, o, (JAVA_OBJECT)dat);
    return o;
}

/**
 * Creates a java.lang.String object from a c string
 */
JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char *str) {
    int length = (int)strlen(str);
    JAVA_ARRAY dat = (JAVA_ARRAY)allocArray(threadStateData, length, &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), 1);
    JAVA_ARRAY_CHAR* arr = (JAVA_ARRAY_CHAR*) (*dat).data;
    for(int iter = 0 ; iter < length ; iter++) {
        arr[iter] = str[iter];
    }
    JAVA_OBJECT o = __NEW_java_lang_String(threadStateData);
    retainObj((JAVA_OBJECT)dat);
    java_lang_String___INIT_____char_1ARRAY(threadStateData, o, (JAVA_OBJECT)dat);
    releaseObj(threadStateData, (JAVA_OBJECT)dat);
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
    JAVA_ARRAY arr = (JAVA_ARRAY)allocArray(threadStateData, CN1_CONSTANT_POOL_SIZE, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    JAVA_OBJECT* tmpConstantPoolObjects = (JAVA_ARRAY_OBJECT*)(*arr).data;
    invokedGC = YES;
    for(int iter = 0 ; iter < CN1_CONSTANT_POOL_SIZE ; iter++) {
        tmpConstantPoolObjects[iter] = newStringFromCString(threadStateData, constantPool[iter]);
        tmpConstantPoolObjects[iter]->__codenameOneReferenceCount = 999999;
    }
    constantPoolObjects = tmpConstantPoolObjects;
    invokedGC = NO;

    // it will wait two seconds unless an explicit GC occurs
    java_lang_System_startGCThread__(getThreadLocalData());
}

JAVA_OBJECT utf8String = NULL;

JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str) {
    if (str == nil) {
        return JAVA_NULL;
    }
    if (utf8String == JAVA_NULL) {
        utf8String = newStringFromCString(threadStateData, "UTF-8");
    }
    JAVA_OBJECT s = __NEW_java_lang_String(threadStateData);
    const char* chars = [str UTF8String];
    int length = (int)strlen(chars);
    
    JAVA_ARRAY dat = (JAVA_ARRAY)allocArray(threadStateData, length, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    memcpy((*dat).data, chars, length * sizeof(JAVA_ARRAY_BYTE));
    retainObj((JAVA_OBJECT)dat);
    java_lang_String___INIT_____byte_1ARRAY_java_lang_String(threadStateData, s, (JAVA_OBJECT)dat, utf8String);
    releaseObj(threadStateData, (JAVA_OBJECT)dat);
    return s;
}


const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str) {
    if(str == NULL) {
        return NULL;
    }
    if (utf8String == JAVA_NULL) {
        utf8String = newStringFromCString(threadStateData, "UTF-8");
    }

    JAVA_ARRAY byteArray = (JAVA_ARRAY)java_lang_String_getBytes___java_lang_String_R_byte_1ARRAY(threadStateData, str, utf8String);
    JAVA_ARRAY_BYTE* data = (*byteArray).data;

    JAVA_INT len = byteArray->length;

    // TODO: fix memory leak!
    char* cs = malloc(len + 1);
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
    NSLog(@"Throwing exception!"); 
    java_lang_Throwable_fillInStack__(threadStateData, exceptionArg); 
    threadStateData->exception = exceptionArg; 
    threadStateData->tryBlockOffset--; 
    while(threadStateData->tryBlockOffset >= 0) { 
        if(threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass == -1 || instanceofFunction(threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass, exceptionArg->__codenameOneParentClsReference->classId)) { 
            int off = threadStateData->tryBlockOffset; 
            threadStateData->tryBlockOffset--; 
            longjmp(threadStateData->blocks[off].destination, 1); 
        } 
        threadStateData->tryBlockOffset--; 
    } 
}

void throwArrayIndexOutOfBoundsException(CODENAME_ONE_THREAD_STATE, int index) {
    JAVA_OBJECT arrayIndexOutOfBoundsException = __NEW_java_lang_ArrayIndexOutOfBoundsException(threadStateData);
    java_lang_ArrayIndexOutOfBoundsException___INIT_____int(threadStateData, arrayIndexOutOfBoundsException, index);
    throwException(threadStateData, arrayIndexOutOfBoundsException);
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
