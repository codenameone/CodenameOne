/*
 * The few ParparVM declarations cn1_crypto_kernels.c reads, so the benchmark
 * compiles the shipped kernel file unchanged outside a translated project.
 */
#ifndef CN1_BENCH_GLOBALS_H
#define CN1_BENCH_GLOBALS_H
#include <stdint.h>
typedef int32_t JAVA_INT;
typedef int8_t JAVA_BOOLEAN;
typedef void JAVA_VOID;
typedef void *JAVA_OBJECT;
typedef struct { void *data; int length; } *JAVA_ARRAY;
#define CN1_ARRAY_DATA(a) (((JAVA_ARRAY)(a))->data)
#define CODENAME_ONE_THREAD_STATE void *threadStateData
#define JAVA_TRUE 1
#define JAVA_FALSE 0
#define JAVA_NULL ((JAVA_OBJECT)0)
#endif
