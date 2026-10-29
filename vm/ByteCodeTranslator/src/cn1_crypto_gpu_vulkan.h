/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

/*
 * The Vulkan backend of the GPU crypto batches (Android, Linux, Windows); included by
 * cn1_crypto_gpu_batch.h. The loader is opened at run time, so nothing is linked and an application
 * on a machine with no Vulkan driver simply keeps every batch on the CPU.
 *
 * The API is declared here by hand -- the Vulkan 1.0 subset a compute dispatch needs -- because the
 * SDK headers are not on every build host. scripts/crypto-gpu/vulkan-abi-check.c holds every
 * struct, offset and constant below to the real vulkan_core.h.
 *
 * A CPU implementation of Vulkan (lavapipe, SwiftShader) is never chosen: it is slower than the CPU
 * kernels on the same cores. CN1_CRYPTO_GPU_ALLOW_CPU_DEVICE=1 lifts that for testing only.
 */
#include <stdlib.h>
#include <string.h>
#if defined(_WIN32)
#  include <windows.h>
#else
#  include <dlfcn.h>
#  include <pthread.h>
#endif
#include "cn1_crypto_gpu_shaders.h"

#if defined(_WIN32)
#  define CN1VK_CALL __stdcall
#elif defined(__ANDROID__) && defined(__ARM_ARCH) && __ARM_ARCH >= 7 && defined(__ARM_32BIT_STATE)
#  define CN1VK_CALL __attribute__((pcs("aapcs-vfp")))
#else
#  define CN1VK_CALL
#endif

typedef uint32_t cn1vk_bool;
typedef uint64_t cn1vk_size;
typedef int32_t cn1vk_result;
typedef void *cn1vk_instance;
typedef void *cn1vk_physical;
typedef void *cn1vk_device;
typedef void *cn1vk_queue;
typedef void *cn1vk_cmd;
typedef uint64_t cn1vk_handle;

/* sType values and enumerants, from vulkan_core.h */
#define CN1VK_ST_APPLICATION_INFO 0
#define CN1VK_ST_INSTANCE_CREATE_INFO 1
#define CN1VK_ST_DEVICE_QUEUE_CREATE_INFO 2
#define CN1VK_ST_DEVICE_CREATE_INFO 3
#define CN1VK_ST_SUBMIT_INFO 4
#define CN1VK_ST_MEMORY_ALLOCATE_INFO 5
#define CN1VK_ST_FENCE_CREATE_INFO 8
#define CN1VK_ST_BUFFER_CREATE_INFO 12
#define CN1VK_ST_SHADER_MODULE_CREATE_INFO 16
#define CN1VK_ST_PIPELINE_SHADER_STAGE_CREATE_INFO 18
#define CN1VK_ST_COMPUTE_PIPELINE_CREATE_INFO 29
#define CN1VK_ST_PIPELINE_LAYOUT_CREATE_INFO 30
#define CN1VK_ST_DESCRIPTOR_SET_LAYOUT_CREATE_INFO 32
#define CN1VK_ST_DESCRIPTOR_POOL_CREATE_INFO 33
#define CN1VK_ST_DESCRIPTOR_SET_ALLOCATE_INFO 34
#define CN1VK_ST_WRITE_DESCRIPTOR_SET 35
#define CN1VK_ST_COMMAND_POOL_CREATE_INFO 39
#define CN1VK_ST_COMMAND_BUFFER_ALLOCATE_INFO 40
#define CN1VK_ST_COMMAND_BUFFER_BEGIN_INFO 42
#define CN1VK_ST_MEMORY_BARRIER 46
#define CN1VK_API_1_0 (1u << 22)
#define CN1VK_QUEUE_COMPUTE 0x2u
#define CN1VK_DEVICE_CPU 4u
#define CN1VK_DEVICE_DISCRETE 2u
#define CN1VK_DEVICE_INTEGRATED 1u
#define CN1VK_DESCRIPTOR_STORAGE_BUFFER 7u
#define CN1VK_STAGE_COMPUTE 0x20u
#define CN1VK_BIND_COMPUTE 1u
#define CN1VK_USAGE_STORAGE 0x20u
#define CN1VK_MEM_HOST_VISIBLE 0x2u
#define CN1VK_MEM_HOST_COHERENT 0x4u
#define CN1VK_POOL_RESET_BUFFER 0x2u
#define CN1VK_CMD_ONE_TIME 0x1u
#define CN1VK_PIPE_COMPUTE_SHADER 0x800u
#define CN1VK_PIPE_HOST 0x4000u
#define CN1VK_ACCESS_SHADER_WRITE 0x40u
#define CN1VK_ACCESS_HOST_READ 0x2000u

typedef struct { uint32_t sType; const void *pNext; const char *pApplicationName; uint32_t applicationVersion;
                 const char *pEngineName; uint32_t engineVersion; uint32_t apiVersion; } cn1vk_app_info;
typedef struct { uint32_t sType; const void *pNext; uint32_t flags; const cn1vk_app_info *pApplicationInfo;
                 uint32_t enabledLayerCount; const char *const *ppEnabledLayerNames;
                 uint32_t enabledExtensionCount; const char *const *ppEnabledExtensionNames; } cn1vk_instance_info;
/* VkPhysicalDeviceProperties: only the leading fields are read, the rest is room */
typedef struct { uint32_t apiVersion; uint32_t driverVersion; uint32_t vendorID; uint32_t deviceID;
                 uint32_t deviceType; char deviceName[256]; uint64_t rest[256]; } cn1vk_props;
typedef struct { uint32_t queueFlags; uint32_t queueCount; uint32_t timestampValidBits;
                 uint32_t granularity[3]; } cn1vk_family;
typedef struct { uint32_t propertyFlags; uint32_t heapIndex; } cn1vk_mem_type;
typedef struct { cn1vk_size size; uint32_t flags; } cn1vk_mem_heap;
typedef struct { uint32_t memoryTypeCount; cn1vk_mem_type memoryTypes[32];
                 uint32_t memoryHeapCount; cn1vk_mem_heap memoryHeaps[16]; } cn1vk_mem_props;
typedef struct { uint32_t sType; const void *pNext; uint32_t flags; uint32_t queueFamilyIndex;
                 uint32_t queueCount; const float *pQueuePriorities; } cn1vk_queue_info;
typedef struct { uint32_t sType; const void *pNext; uint32_t flags; uint32_t queueCreateInfoCount;
                 const cn1vk_queue_info *pQueueCreateInfos; uint32_t enabledLayerCount;
                 const char *const *ppEnabledLayerNames; uint32_t enabledExtensionCount;
                 const char *const *ppEnabledExtensionNames; const void *pEnabledFeatures; } cn1vk_device_info;
typedef struct { uint32_t sType; const void *pNext; uint32_t flags; size_t codeSize;
                 const uint32_t *pCode; } cn1vk_module_info;
typedef struct { uint32_t binding; uint32_t descriptorType; uint32_t descriptorCount; uint32_t stageFlags;
                 const void *pImmutableSamplers; } cn1vk_binding;
typedef struct { uint32_t sType; const void *pNext; uint32_t flags; uint32_t bindingCount;
                 const cn1vk_binding *pBindings; } cn1vk_set_layout_info;
typedef struct { uint32_t stageFlags; uint32_t offset; uint32_t size; } cn1vk_push_range;
typedef struct { uint32_t sType; const void *pNext; uint32_t flags; uint32_t setLayoutCount;
                 const cn1vk_handle *pSetLayouts; uint32_t pushConstantRangeCount;
                 const cn1vk_push_range *pPushConstantRanges; } cn1vk_layout_info;
typedef struct { uint32_t sType; const void *pNext; uint32_t flags; uint32_t stage; cn1vk_handle module;
                 const char *pName; const void *pSpecializationInfo; } cn1vk_stage_info;
typedef struct { uint32_t sType; const void *pNext; uint32_t flags; cn1vk_stage_info stage; cn1vk_handle layout;
                 cn1vk_handle basePipelineHandle; int32_t basePipelineIndex; } cn1vk_pipeline_info;
typedef struct { uint32_t type; uint32_t descriptorCount; } cn1vk_pool_size;
typedef struct { uint32_t sType; const void *pNext; uint32_t flags; uint32_t maxSets; uint32_t poolSizeCount;
                 const cn1vk_pool_size *pPoolSizes; } cn1vk_pool_info;
typedef struct { uint32_t sType; const void *pNext; cn1vk_handle descriptorPool; uint32_t descriptorSetCount;
                 const cn1vk_handle *pSetLayouts; } cn1vk_set_alloc_info;
typedef struct { cn1vk_handle buffer; cn1vk_size offset; cn1vk_size range; } cn1vk_buffer_ref;
typedef struct { uint32_t sType; const void *pNext; cn1vk_handle dstSet; uint32_t dstBinding;
                 uint32_t dstArrayElement; uint32_t descriptorCount; uint32_t descriptorType;
                 const void *pImageInfo; const cn1vk_buffer_ref *pBufferInfo;
                 const void *pTexelBufferView; } cn1vk_write;
typedef struct { uint32_t sType; const void *pNext; uint32_t flags; cn1vk_size size; uint32_t usage;
                 uint32_t sharingMode; uint32_t queueFamilyIndexCount;
                 const uint32_t *pQueueFamilyIndices; } cn1vk_buffer_info;
typedef struct { cn1vk_size size; cn1vk_size alignment; uint32_t memoryTypeBits; } cn1vk_mem_req;
typedef struct { uint32_t sType; const void *pNext; cn1vk_size allocationSize;
                 uint32_t memoryTypeIndex; } cn1vk_alloc_info;
typedef struct { uint32_t sType; const void *pNext; uint32_t flags; uint32_t queueFamilyIndex; } cn1vk_cmd_pool_info;
typedef struct { uint32_t sType; const void *pNext; cn1vk_handle commandPool; uint32_t level;
                 uint32_t commandBufferCount; } cn1vk_cmd_alloc_info;
typedef struct { uint32_t sType; const void *pNext; uint32_t flags; const void *pInheritanceInfo; } cn1vk_begin_info;
typedef struct { uint32_t sType; const void *pNext; uint32_t waitSemaphoreCount; const cn1vk_handle *pWaitSemaphores;
                 const uint32_t *pWaitDstStageMask; uint32_t commandBufferCount; const cn1vk_cmd *pCommandBuffers;
                 uint32_t signalSemaphoreCount; const cn1vk_handle *pSignalSemaphores; } cn1vk_submit_info;
typedef struct { uint32_t sType; const void *pNext; uint32_t flags; } cn1vk_fence_info;
typedef struct { uint32_t sType; const void *pNext; uint32_t srcAccessMask; uint32_t dstAccessMask; } cn1vk_barrier;

typedef void (CN1VK_CALL *cn1vk_void_fn)(void);
typedef cn1vk_void_fn (CN1VK_CALL *cn1vk_gipa_fn)(cn1vk_instance, const char*);

/* the entry points, resolved once through vkGetInstanceProcAddr */
#define CN1VK_FUNCS(X) \
    X(cn1vk_result, vkEnumeratePhysicalDevices, (cn1vk_instance, uint32_t*, cn1vk_physical*)) \
    X(void, vkGetPhysicalDeviceProperties, (cn1vk_physical, cn1vk_props*)) \
    X(void, vkGetPhysicalDeviceQueueFamilyProperties, (cn1vk_physical, uint32_t*, cn1vk_family*)) \
    X(void, vkGetPhysicalDeviceMemoryProperties, (cn1vk_physical, cn1vk_mem_props*)) \
    X(cn1vk_result, vkCreateDevice, (cn1vk_physical, const cn1vk_device_info*, const void*, cn1vk_device*)) \
    X(void, vkGetDeviceQueue, (cn1vk_device, uint32_t, uint32_t, cn1vk_queue*)) \
    X(cn1vk_result, vkCreateShaderModule, (cn1vk_device, const cn1vk_module_info*, const void*, cn1vk_handle*)) \
    X(void, vkDestroyShaderModule, (cn1vk_device, cn1vk_handle, const void*)) \
    X(cn1vk_result, vkCreateDescriptorSetLayout, (cn1vk_device, const cn1vk_set_layout_info*, const void*, cn1vk_handle*)) \
    X(cn1vk_result, vkCreatePipelineLayout, (cn1vk_device, const cn1vk_layout_info*, const void*, cn1vk_handle*)) \
    X(cn1vk_result, vkCreateComputePipelines, (cn1vk_device, cn1vk_handle, uint32_t, const cn1vk_pipeline_info*, const void*, cn1vk_handle*)) \
    X(cn1vk_result, vkCreateDescriptorPool, (cn1vk_device, const cn1vk_pool_info*, const void*, cn1vk_handle*)) \
    X(cn1vk_result, vkAllocateDescriptorSets, (cn1vk_device, const cn1vk_set_alloc_info*, cn1vk_handle*)) \
    X(void, vkUpdateDescriptorSets, (cn1vk_device, uint32_t, const cn1vk_write*, uint32_t, const void*)) \
    X(cn1vk_result, vkCreateBuffer, (cn1vk_device, const cn1vk_buffer_info*, const void*, cn1vk_handle*)) \
    X(void, vkDestroyBuffer, (cn1vk_device, cn1vk_handle, const void*)) \
    X(void, vkGetBufferMemoryRequirements, (cn1vk_device, cn1vk_handle, cn1vk_mem_req*)) \
    X(cn1vk_result, vkAllocateMemory, (cn1vk_device, const cn1vk_alloc_info*, const void*, cn1vk_handle*)) \
    X(void, vkFreeMemory, (cn1vk_device, cn1vk_handle, const void*)) \
    X(cn1vk_result, vkBindBufferMemory, (cn1vk_device, cn1vk_handle, cn1vk_handle, cn1vk_size)) \
    X(cn1vk_result, vkMapMemory, (cn1vk_device, cn1vk_handle, cn1vk_size, cn1vk_size, uint32_t, void**)) \
    X(cn1vk_result, vkCreateCommandPool, (cn1vk_device, const cn1vk_cmd_pool_info*, const void*, cn1vk_handle*)) \
    X(cn1vk_result, vkAllocateCommandBuffers, (cn1vk_device, const cn1vk_cmd_alloc_info*, cn1vk_cmd*)) \
    X(cn1vk_result, vkResetCommandBuffer, (cn1vk_cmd, uint32_t)) \
    X(cn1vk_result, vkBeginCommandBuffer, (cn1vk_cmd, const cn1vk_begin_info*)) \
    X(cn1vk_result, vkEndCommandBuffer, (cn1vk_cmd)) \
    X(void, vkCmdBindPipeline, (cn1vk_cmd, uint32_t, cn1vk_handle)) \
    X(void, vkCmdBindDescriptorSets, (cn1vk_cmd, uint32_t, cn1vk_handle, uint32_t, uint32_t, const cn1vk_handle*, uint32_t, const uint32_t*)) \
    X(void, vkCmdPushConstants, (cn1vk_cmd, cn1vk_handle, uint32_t, uint32_t, uint32_t, const void*)) \
    X(void, vkCmdDispatch, (cn1vk_cmd, uint32_t, uint32_t, uint32_t)) \
    X(void, vkCmdPipelineBarrier, (cn1vk_cmd, uint32_t, uint32_t, uint32_t, uint32_t, const cn1vk_barrier*, uint32_t, const void*, uint32_t, const void*)) \
    X(cn1vk_result, vkCreateFence, (cn1vk_device, const cn1vk_fence_info*, const void*, cn1vk_handle*)) \
    X(cn1vk_result, vkResetFences, (cn1vk_device, uint32_t, const cn1vk_handle*)) \
    X(cn1vk_result, vkWaitForFences, (cn1vk_device, uint32_t, const cn1vk_handle*, cn1vk_bool, uint64_t)) \
    X(cn1vk_result, vkQueueSubmit, (cn1vk_queue, uint32_t, const cn1vk_submit_info*, cn1vk_handle))

#define CN1VK_DECLARE(ret, name, args) ret (CN1VK_CALL *name) args;
static struct { CN1VK_FUNCS(CN1VK_DECLARE) int unused; } cn1vk;

#define CN1G_VK_KERNELS 5
/* one submission per slice, so no single one runs long enough to trip a driver's GPU watchdog */
#define CN1G_VK_MAX_PER_DISPATCH 16384u

static cn1vk_device cn1vk_dev;
static cn1vk_queue cn1vk_q;
static cn1vk_mem_props cn1vk_mem;
static cn1vk_handle cn1vk_set_layout, cn1vk_layout, cn1vk_set, cn1vk_fence, cn1vk_pool;
static cn1vk_cmd cn1vk_cb;
static cn1vk_handle cn1vk_pipes[CN1G_VK_KERNELS];
/* the fixed-base tables, uploaded once: 0 secp256k1, 1 ed25519 */
static cn1vk_handle cn1vk_tbl_buf[2], cn1vk_tbl_mem[2];
static const uint32_t *cn1vk_tbl_key[2];
/* 0 not started, 1 starting, 2 ready, 3 unavailable */
static int cn1vk_state = 0;
#if defined(_WIN32)
static CRITICAL_SECTION cn1vk_lock;
#else
static pthread_mutex_t cn1vk_lock = PTHREAD_MUTEX_INITIALIZER;
#endif

#if defined(_MSC_VER) && !defined(__clang__)
#  define CN1VK_LOAD_STATE() ((int) InterlockedCompareExchange((volatile LONG*) &cn1vk_state, 0, 0))
#  define CN1VK_STORE_STATE(v) InterlockedExchange((volatile LONG*) &cn1vk_state, (LONG) (v))
#  define CN1VK_START() (InterlockedCompareExchange((volatile LONG*) &cn1vk_state, 1, 0) == 0)
#  define CN1VK_LOAD_PIPE(i) ((cn1vk_handle) InterlockedCompareExchange64((volatile LONG64*) &cn1vk_pipes[i], 0, 0))
#  define CN1VK_STORE_PIPE(i, v) InterlockedExchange64((volatile LONG64*) &cn1vk_pipes[i], (LONG64) (v))
#else
#  define CN1VK_LOAD_PIPE(i) __atomic_load_n(&cn1vk_pipes[i], __ATOMIC_ACQUIRE)
#  define CN1VK_STORE_PIPE(i, v) __atomic_store_n(&cn1vk_pipes[i], (v), __ATOMIC_RELEASE)
#  define CN1VK_LOAD_STATE() __atomic_load_n(&cn1vk_state, __ATOMIC_ACQUIRE)
#  define CN1VK_STORE_STATE(v) __atomic_store_n(&cn1vk_state, (v), __ATOMIC_RELEASE)
static int cn1vk_start(void) {
    int expected = 0;
    return __atomic_compare_exchange_n(&cn1vk_state, &expected, 1, 0, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
}
#  define CN1VK_START() cn1vk_start()
#endif

static int cn1vk_memory_type(uint32_t bits, uint32_t want) {
    uint32_t i;
    for (i = 0; i < cn1vk_mem.memoryTypeCount; i++) {
        if ((bits & (1u << i)) && (cn1vk_mem.memoryTypes[i].propertyFlags & want) == want) {
            return (int) i;
        }
    }
    return -1;
}

/* a host-visible, coherent storage buffer, mapped; 0 on failure with nothing left allocated */
static int cn1vk_buffer(size_t bytes, cn1vk_handle *buf, cn1vk_handle *mem, void **map) {
    cn1vk_buffer_info bi;
    cn1vk_mem_req req;
    cn1vk_alloc_info ai;
    int type;
    memset(&bi, 0, sizeof(bi));
    bi.sType = CN1VK_ST_BUFFER_CREATE_INFO;
    bi.size = bytes < 4 ? 4 : (bytes + 3) & ~(size_t) 3;
    bi.usage = CN1VK_USAGE_STORAGE;
    *buf = 0;
    *mem = 0;
    if (cn1vk.vkCreateBuffer(cn1vk_dev, &bi, NULL, buf) != 0) {
        return 0;
    }
    cn1vk.vkGetBufferMemoryRequirements(cn1vk_dev, *buf, &req);
    type = cn1vk_memory_type(req.memoryTypeBits, CN1VK_MEM_HOST_VISIBLE | CN1VK_MEM_HOST_COHERENT);
    memset(&ai, 0, sizeof(ai));
    ai.sType = CN1VK_ST_MEMORY_ALLOCATE_INFO;
    ai.allocationSize = req.size;
    ai.memoryTypeIndex = (uint32_t) type;
    if (type < 0 || cn1vk.vkAllocateMemory(cn1vk_dev, &ai, NULL, mem) != 0) {
        cn1vk.vkDestroyBuffer(cn1vk_dev, *buf, NULL);
        *buf = 0;
        *mem = 0;
        return 0;
    }
    if (cn1vk.vkBindBufferMemory(cn1vk_dev, *buf, *mem, 0) != 0
            || cn1vk.vkMapMemory(cn1vk_dev, *mem, 0, (cn1vk_size) ~(uint64_t) 0, 0, map) != 0) {
        cn1vk.vkDestroyBuffer(cn1vk_dev, *buf, NULL);
        cn1vk.vkFreeMemory(cn1vk_dev, *mem, NULL);
        *buf = 0;
        *mem = 0;
        return 0;
    }
    return 1;
}

/* freeing memory unmaps it */
static void cn1vk_release(cn1vk_handle buf, cn1vk_handle mem) {
    if (buf != 0) {
        cn1vk.vkDestroyBuffer(cn1vk_dev, buf, NULL);
    }
    if (mem != 0) {
        cn1vk.vkFreeMemory(cn1vk_dev, mem, NULL);
    }
}

static int cn1vk_init(void) {
    cn1vk_gipa_fn gipa;
    cn1vk_result (CN1VK_CALL *createInstance)(const cn1vk_instance_info*, const void*, cn1vk_instance*);
    cn1vk_instance inst;
    cn1vk_app_info app;
    cn1vk_instance_info ii;
    cn1vk_physical devs[16];
    uint32_t ndev = 16, i, best = 0, bestScore = 0, family = 0;
    int allowCpu;
    cn1vk_queue_info qi;
    cn1vk_device_info di;
    float prio = 1.0f;
    cn1vk_binding binds[3];
    cn1vk_set_layout_info sli;
    cn1vk_push_range pr;
    cn1vk_layout_info li;
    cn1vk_pool_size ps;
    cn1vk_pool_info pi;
    cn1vk_set_alloc_info sai;
    cn1vk_cmd_pool_info cpi;
    cn1vk_cmd_alloc_info cai;
    cn1vk_fence_info fi;
#if defined(_WIN32)
    HMODULE lib = LoadLibraryA("vulkan-1.dll");
    gipa = lib == NULL ? NULL : (cn1vk_gipa_fn) (void*) GetProcAddress(lib, "vkGetInstanceProcAddr");
#else
    void *lib = dlopen("libvulkan.so.1", RTLD_NOW | RTLD_LOCAL);
    if (lib == NULL) {
        lib = dlopen("libvulkan.so", RTLD_NOW | RTLD_LOCAL);
    }
    gipa = lib == NULL ? NULL : (cn1vk_gipa_fn) dlsym(lib, "vkGetInstanceProcAddr");
#endif
    if (gipa == NULL) {
        return 0;
    }
    createInstance = (cn1vk_result (CN1VK_CALL *)(const cn1vk_instance_info*, const void*, cn1vk_instance*))
            gipa(NULL, "vkCreateInstance");
    if (createInstance == NULL) {
        return 0;
    }
    memset(&app, 0, sizeof(app));
    app.sType = CN1VK_ST_APPLICATION_INFO;
    app.pApplicationName = "cn1crypto";
    app.pEngineName = "Codename One";
    app.apiVersion = CN1VK_API_1_0;
    memset(&ii, 0, sizeof(ii));
    ii.sType = CN1VK_ST_INSTANCE_CREATE_INFO;
    ii.pApplicationInfo = &app;
    if (createInstance(&ii, NULL, &inst) != 0) {
        return 0;
    }
#define CN1VK_RESOLVE(ret, name, args) \
    cn1vk.name = (ret (CN1VK_CALL *) args) gipa(inst, #name); \
    if (cn1vk.name == NULL) { return 0; }
    CN1VK_FUNCS(CN1VK_RESOLVE)
#undef CN1VK_RESOLVE
    /* the instance stays alive for the life of the process, as does everything made from it */
    if (cn1vk.vkEnumeratePhysicalDevices(inst, &ndev, devs) < 0 || ndev == 0) {
        return 0;
    }
    {
        const char *env = getenv("CN1_CRYPTO_GPU_ALLOW_CPU_DEVICE");
        allowCpu = env != NULL && env[0] == '1';
    }
    for (i = 0; i < ndev; i++) {
        cn1vk_props p;
        cn1vk_family fams[16];
        uint32_t nfam = 16, f, score;
        cn1vk.vkGetPhysicalDeviceProperties(devs[i], &p);
        if (p.deviceType == CN1VK_DEVICE_CPU && !allowCpu) {
            continue;
        }
        score = p.deviceType == CN1VK_DEVICE_DISCRETE ? 4 : p.deviceType == CN1VK_DEVICE_INTEGRATED ? 3 : 1;
        cn1vk.vkGetPhysicalDeviceQueueFamilyProperties(devs[i], &nfam, fams);
        for (f = 0; f < nfam; f++) {
            if ((fams[f].queueFlags & CN1VK_QUEUE_COMPUTE) && fams[f].queueCount > 0) {
                break;
            }
        }
        if (f < nfam && score > bestScore) {
            bestScore = score;
            best = i;
            family = f;
        }
    }
    if (bestScore == 0) {
        return 0;
    }
    cn1vk.vkGetPhysicalDeviceMemoryProperties(devs[best], &cn1vk_mem);
    memset(&qi, 0, sizeof(qi));
    qi.sType = CN1VK_ST_DEVICE_QUEUE_CREATE_INFO;
    qi.queueFamilyIndex = family;
    qi.queueCount = 1;
    qi.pQueuePriorities = &prio;
    memset(&di, 0, sizeof(di));
    di.sType = CN1VK_ST_DEVICE_CREATE_INFO;
    di.queueCreateInfoCount = 1;
    di.pQueueCreateInfos = &qi;
    if (cn1vk.vkCreateDevice(devs[best], &di, NULL, &cn1vk_dev) != 0) {
        return 0;
    }
    cn1vk.vkGetDeviceQueue(cn1vk_dev, family, 0, &cn1vk_q);

    /* one layout for every kernel: three storage buffers and 16 bytes of push constants */
    memset(binds, 0, sizeof(binds));
    for (i = 0; i < 3; i++) {
        binds[i].binding = i;
        binds[i].descriptorType = CN1VK_DESCRIPTOR_STORAGE_BUFFER;
        binds[i].descriptorCount = 1;
        binds[i].stageFlags = CN1VK_STAGE_COMPUTE;
    }
    memset(&sli, 0, sizeof(sli));
    sli.sType = CN1VK_ST_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
    sli.bindingCount = 3;
    sli.pBindings = binds;
    pr.stageFlags = CN1VK_STAGE_COMPUTE;
    pr.offset = 0;
    pr.size = 16;
    memset(&li, 0, sizeof(li));
    li.sType = CN1VK_ST_PIPELINE_LAYOUT_CREATE_INFO;
    li.setLayoutCount = 1;
    li.pSetLayouts = &cn1vk_set_layout;
    li.pushConstantRangeCount = 1;
    li.pPushConstantRanges = &pr;
    ps.type = CN1VK_DESCRIPTOR_STORAGE_BUFFER;
    ps.descriptorCount = 3;
    memset(&pi, 0, sizeof(pi));
    pi.sType = CN1VK_ST_DESCRIPTOR_POOL_CREATE_INFO;
    pi.maxSets = 1;
    pi.poolSizeCount = 1;
    pi.pPoolSizes = &ps;
    memset(&sai, 0, sizeof(sai));
    sai.sType = CN1VK_ST_DESCRIPTOR_SET_ALLOCATE_INFO;
    sai.descriptorSetCount = 1;
    sai.pSetLayouts = &cn1vk_set_layout;
    memset(&cpi, 0, sizeof(cpi));
    cpi.sType = CN1VK_ST_COMMAND_POOL_CREATE_INFO;
    cpi.flags = CN1VK_POOL_RESET_BUFFER;
    cpi.queueFamilyIndex = family;
    memset(&cai, 0, sizeof(cai));
    cai.sType = CN1VK_ST_COMMAND_BUFFER_ALLOCATE_INFO;
    cai.commandBufferCount = 1;
    memset(&fi, 0, sizeof(fi));
    fi.sType = CN1VK_ST_FENCE_CREATE_INFO;
    if (cn1vk.vkCreateDescriptorSetLayout(cn1vk_dev, &sli, NULL, &cn1vk_set_layout) != 0
            || cn1vk.vkCreatePipelineLayout(cn1vk_dev, &li, NULL, &cn1vk_layout) != 0
            || cn1vk.vkCreateDescriptorPool(cn1vk_dev, &pi, NULL, &cn1vk_pool) != 0
            || (sai.descriptorPool = cn1vk_pool, cn1vk.vkAllocateDescriptorSets(cn1vk_dev, &sai, &cn1vk_set)) != 0
            || cn1vk.vkCreateCommandPool(cn1vk_dev, &cpi, NULL, &cai.commandPool) != 0
            || cn1vk.vkAllocateCommandBuffers(cn1vk_dev, &cai, &cn1vk_cb) != 0
            || cn1vk.vkCreateFence(cn1vk_dev, &fi, NULL, &cn1vk_fence) != 0) {
        return 0;
    }

    return 1;
}

/*
 * The pipelines, one at a time and each published as it is built: a driver can take seconds over
 * a curve kernel, and the hashes need not wait for it. The cheapest go first.
 */
static void cn1vk_build_pipelines(void) {
    static const int order[CN1G_VK_KERNELS] = {
        CN1G_KERNEL_SHA256, CN1G_KERNEL_KECCAK256, CN1G_KERNEL_ED25519, CN1G_KERNEL_ECDSA, CN1G_KERNEL_SCHNORR
    };
    const uint32_t *code[CN1G_VK_KERNELS];
    size_t codeBytes[CN1G_VK_KERNELS];
    int o;
    code[0] = cn1g_spv_sha256; codeBytes[0] = sizeof(cn1g_spv_sha256);
    code[1] = cn1g_spv_keccak256; codeBytes[1] = sizeof(cn1g_spv_keccak256);
    code[2] = cn1g_spv_ecdsa; codeBytes[2] = sizeof(cn1g_spv_ecdsa);
    code[3] = cn1g_spv_schnorr; codeBytes[3] = sizeof(cn1g_spv_schnorr);
    code[4] = cn1g_spv_ed25519; codeBytes[4] = sizeof(cn1g_spv_ed25519);
    /* a kernel the driver will not build stays on the CPU; the others still run here */
    for (o = 0; o < CN1G_VK_KERNELS; o++) {
        int i = order[o];
        cn1vk_module_info mi;
        cn1vk_handle pipe = 0;
        cn1vk_pipeline_info cp;
        cn1vk_handle module = 0;
        memset(&mi, 0, sizeof(mi));
        mi.sType = CN1VK_ST_SHADER_MODULE_CREATE_INFO;
        mi.codeSize = codeBytes[i];
        mi.pCode = code[i];
        if (cn1vk.vkCreateShaderModule(cn1vk_dev, &mi, NULL, &module) != 0) {
            continue;
        }
        memset(&cp, 0, sizeof(cp));
        cp.sType = CN1VK_ST_COMPUTE_PIPELINE_CREATE_INFO;
        cp.stage.sType = CN1VK_ST_PIPELINE_SHADER_STAGE_CREATE_INFO;
        cp.stage.stage = CN1VK_STAGE_COMPUTE;
        cp.stage.module = module;
        cp.stage.pName = "main";
        cp.layout = cn1vk_layout;
        cp.basePipelineIndex = -1;
        if (cn1vk.vkCreateComputePipelines(cn1vk_dev, 0, 1, &cp, NULL, &pipe) == 0) {
            CN1VK_STORE_PIPE(i, pipe);
        }
        cn1vk.vkDestroyShaderModule(cn1vk_dev, module, NULL);
    }
}

#if defined(_WIN32)
static DWORD WINAPI cn1vk_init_thread(LPVOID unused) {
    (void) unused;
    if (cn1vk_init()) {
        CN1VK_STORE_STATE(2);
        cn1vk_build_pipelines();
    } else {
        CN1VK_STORE_STATE(3);
    }
    return 0;
}
#else
static void *cn1vk_init_thread(void *unused) {
    (void) unused;
    if (cn1vk_init()) {
        CN1VK_STORE_STATE(2);
        cn1vk_build_pipelines();
    } else {
        CN1VK_STORE_STATE(3);
    }
    return NULL;
}
#endif

/* the device and the pipelines are made on a thread of their own, and until a kernel's pipeline
 * exists its batches run on the CPU */
static int cn1vk_ready(int kernel) {
    int s = CN1VK_LOAD_STATE();
    if (s == 0 && CN1VK_START()) {
#if defined(_WIN32)
        HANDLE t;
        InitializeCriticalSection(&cn1vk_lock);
        t = CreateThread(NULL, 0, cn1vk_init_thread, NULL, 0, NULL);
        if (t == NULL) {
            CN1VK_STORE_STATE(3);
        } else {
            CloseHandle(t);
        }
#else
        pthread_t t;
        if (pthread_create(&t, NULL, cn1vk_init_thread, NULL) != 0) {
            CN1VK_STORE_STATE(3);
        } else {
            pthread_detach(t);
        }
#endif
        return 0;
    }
    return s == 2 && kernel >= 0 && kernel < CN1G_VK_KERNELS && CN1VK_LOAD_PIPE(kernel) != 0;
}

static int cn1vk_run_locked(int kernel, const void *in, size_t inBytes, void *out, size_t outBytes,
                            const uint32_t *table, size_t tableBytes, uint32_t len, uint32_t count) {
    cn1vk_handle inBuf, inMem, outBuf, outMem, tbuf;
    void *inMap, *outMap;
    cn1vk_buffer_ref refs[3];
    cn1vk_write w[3];
    int slot = kernel == CN1G_KERNEL_ED25519 ? 1 : 0, ok = 1, i;
    uint32_t at = 0;
    if (table != NULL && cn1vk_tbl_key[slot] != table) {
        void *map;
        cn1vk_release(cn1vk_tbl_buf[slot], cn1vk_tbl_mem[slot]);
        cn1vk_tbl_key[slot] = NULL;
        if (!cn1vk_buffer(tableBytes, &cn1vk_tbl_buf[slot], &cn1vk_tbl_mem[slot], &map)) {
            return 0;
        }
        memcpy(map, table, tableBytes);
        cn1vk_tbl_key[slot] = table;
    }
    if (!cn1vk_buffer(inBytes, &inBuf, &inMem, &inMap)) {
        return 0;
    }
    if (!cn1vk_buffer(outBytes, &outBuf, &outMem, &outMap)) {
        cn1vk_release(inBuf, inMem);
        return 0;
    }
    memcpy(inMap, in, inBytes);
    /* a hash binds no table; the input stands in for the unused binding */
    tbuf = table != NULL ? cn1vk_tbl_buf[slot] : inBuf;
    refs[0].buffer = inBuf;
    refs[1].buffer = outBuf;
    refs[2].buffer = tbuf;
    memset(w, 0, sizeof(w));
    for (i = 0; i < 3; i++) {
        refs[i].offset = 0;
        refs[i].range = (cn1vk_size) ~(uint64_t) 0;
        w[i].sType = CN1VK_ST_WRITE_DESCRIPTOR_SET;
        w[i].dstSet = cn1vk_set;
        w[i].dstBinding = (uint32_t) i;
        w[i].descriptorCount = 1;
        w[i].descriptorType = CN1VK_DESCRIPTOR_STORAGE_BUFFER;
        w[i].pBufferInfo = &refs[i];
    }
    cn1vk.vkUpdateDescriptorSets(cn1vk_dev, 3, w, 0, NULL);
    while (ok && at < count) {
        uint32_t n = count - at < CN1G_VK_MAX_PER_DISPATCH ? count - at : CN1G_VK_MAX_PER_DISPATCH;
        uint32_t pc[4];
        cn1vk_begin_info bi;
        cn1vk_barrier mb;
        cn1vk_submit_info si;
        pc[0] = len;
        pc[1] = at + n;
        pc[2] = at;
        pc[3] = 0;
        memset(&bi, 0, sizeof(bi));
        bi.sType = CN1VK_ST_COMMAND_BUFFER_BEGIN_INFO;
        bi.flags = CN1VK_CMD_ONE_TIME;
        memset(&mb, 0, sizeof(mb));
        mb.sType = CN1VK_ST_MEMORY_BARRIER;
        mb.srcAccessMask = CN1VK_ACCESS_SHADER_WRITE;
        mb.dstAccessMask = CN1VK_ACCESS_HOST_READ;
        memset(&si, 0, sizeof(si));
        si.sType = CN1VK_ST_SUBMIT_INFO;
        si.commandBufferCount = 1;
        si.pCommandBuffers = &cn1vk_cb;
        if (cn1vk.vkResetCommandBuffer(cn1vk_cb, 0) != 0 || cn1vk.vkBeginCommandBuffer(cn1vk_cb, &bi) != 0) {
            ok = 0;
            break;
        }
        cn1vk.vkCmdBindPipeline(cn1vk_cb, CN1VK_BIND_COMPUTE, CN1VK_LOAD_PIPE(kernel));
        cn1vk.vkCmdBindDescriptorSets(cn1vk_cb, CN1VK_BIND_COMPUTE, cn1vk_layout, 0, 1, &cn1vk_set, 0, NULL);
        cn1vk.vkCmdPushConstants(cn1vk_cb, cn1vk_layout, CN1VK_STAGE_COMPUTE, 0, sizeof(pc), pc);
        /* local_size_x is 64 in every kernel */
        cn1vk.vkCmdDispatch(cn1vk_cb, (n + 63) / 64, 1, 1);
        cn1vk.vkCmdPipelineBarrier(cn1vk_cb, CN1VK_PIPE_COMPUTE_SHADER, CN1VK_PIPE_HOST, 0, 1, &mb, 0, NULL, 0, NULL);
        ok = cn1vk.vkEndCommandBuffer(cn1vk_cb) == 0
                && cn1vk.vkQueueSubmit(cn1vk_q, 1, &si, cn1vk_fence) == 0
                && cn1vk.vkWaitForFences(cn1vk_dev, 1, &cn1vk_fence, 1, (uint64_t) 30000000000ull) == 0
                && cn1vk.vkResetFences(cn1vk_dev, 1, &cn1vk_fence) == 0;
        at += n;
    }
    if (ok) {
        memcpy(out, outMap, outBytes);
    }
    cn1vk_release(inBuf, inMem);
    cn1vk_release(outBuf, outMem);
    return ok;
}

static int cn1vk_run(int kernel, const void *in, size_t inBytes, void *out, size_t outBytes,
                     const uint32_t *table, size_t tableBytes, uint32_t len, uint32_t count) {
    int ok;
    if (!cn1vk_ready(kernel)) {
        return 0;
    }
    /* one queue, one command buffer, one descriptor set: batches from several threads take turns */
#if defined(_WIN32)
    EnterCriticalSection(&cn1vk_lock);
#else
    pthread_mutex_lock(&cn1vk_lock);
#endif
    ok = cn1vk_run_locked(kernel, in, inBytes, out, outBytes, table, tableBytes, len, count);
#if defined(_WIN32)
    LeaveCriticalSection(&cn1vk_lock);
#else
    pthread_mutex_unlock(&cn1vk_lock);
#endif
    return ok;
}

static const cn1g_backend cn1g_vulkan = { "Vulkan", cn1vk_ready, cn1vk_run };

static const cn1g_backend *cn1g_vulkan_get(void) {
    return CN1VK_LOAD_STATE() == 3 ? NULL : &cn1g_vulkan;
}
