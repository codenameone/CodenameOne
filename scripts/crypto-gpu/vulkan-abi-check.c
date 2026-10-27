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
 * Holds the hand-declared Vulkan subset in cn1_crypto_gpu_vulkan.h to the real vulkan_core.h:
 * every struct's size and field offsets, and every constant. It only has to compile.
 *
 *     cc -std=c11 -fsyntax-only -I vm/ByteCodeTranslator/src scripts/crypto-gpu/vulkan-abi-check.c
 */
#include <stddef.h>
#include <stdint.h>
#include <vulkan/vulkan_core.h>
#include "cn1_crypto_gpu.h"
#define CN1G_KERNEL_ED25519 4
#include "cn1_crypto_gpu_vulkan.h"

#define SAME_SIZE(ours, theirs) _Static_assert(sizeof(ours) == sizeof(theirs), #ours " size")
#define SAME_AT(ours, theirs, a, b) _Static_assert(offsetof(ours, a) == offsetof(theirs, b), #ours "." #a)
#define SAME(a, b) _Static_assert((uint64_t) (a) == (uint64_t) (b), #a)

SAME_SIZE(cn1vk_app_info, VkApplicationInfo);
SAME_AT(cn1vk_app_info, VkApplicationInfo, apiVersion, apiVersion);
SAME_SIZE(cn1vk_instance_info, VkInstanceCreateInfo);
SAME_AT(cn1vk_instance_info, VkInstanceCreateInfo, ppEnabledExtensionNames, ppEnabledExtensionNames);
_Static_assert(sizeof(cn1vk_props) >= sizeof(VkPhysicalDeviceProperties), "cn1vk_props room");
SAME_AT(cn1vk_props, VkPhysicalDeviceProperties, deviceType, deviceType);
SAME_AT(cn1vk_props, VkPhysicalDeviceProperties, deviceName, deviceName);
SAME_SIZE(cn1vk_family, VkQueueFamilyProperties);
SAME_SIZE(cn1vk_mem_props, VkPhysicalDeviceMemoryProperties);
SAME_AT(cn1vk_mem_props, VkPhysicalDeviceMemoryProperties, memoryHeapCount, memoryHeapCount);
SAME_AT(cn1vk_mem_props, VkPhysicalDeviceMemoryProperties, memoryHeaps, memoryHeaps);
SAME_SIZE(cn1vk_queue_info, VkDeviceQueueCreateInfo);
SAME_AT(cn1vk_queue_info, VkDeviceQueueCreateInfo, pQueuePriorities, pQueuePriorities);
SAME_SIZE(cn1vk_device_info, VkDeviceCreateInfo);
SAME_AT(cn1vk_device_info, VkDeviceCreateInfo, pEnabledFeatures, pEnabledFeatures);
SAME_SIZE(cn1vk_module_info, VkShaderModuleCreateInfo);
SAME_AT(cn1vk_module_info, VkShaderModuleCreateInfo, pCode, pCode);
SAME_SIZE(cn1vk_binding, VkDescriptorSetLayoutBinding);
SAME_SIZE(cn1vk_set_layout_info, VkDescriptorSetLayoutCreateInfo);
SAME_SIZE(cn1vk_push_range, VkPushConstantRange);
SAME_SIZE(cn1vk_layout_info, VkPipelineLayoutCreateInfo);
SAME_AT(cn1vk_layout_info, VkPipelineLayoutCreateInfo, pPushConstantRanges, pPushConstantRanges);
SAME_SIZE(cn1vk_stage_info, VkPipelineShaderStageCreateInfo);
SAME_AT(cn1vk_stage_info, VkPipelineShaderStageCreateInfo, module, module);
SAME_AT(cn1vk_stage_info, VkPipelineShaderStageCreateInfo, pName, pName);
SAME_SIZE(cn1vk_pipeline_info, VkComputePipelineCreateInfo);
SAME_AT(cn1vk_pipeline_info, VkComputePipelineCreateInfo, layout, layout);
SAME_AT(cn1vk_pipeline_info, VkComputePipelineCreateInfo, basePipelineIndex, basePipelineIndex);
SAME_SIZE(cn1vk_pool_size, VkDescriptorPoolSize);
SAME_SIZE(cn1vk_pool_info, VkDescriptorPoolCreateInfo);
SAME_SIZE(cn1vk_set_alloc_info, VkDescriptorSetAllocateInfo);
SAME_SIZE(cn1vk_buffer_ref, VkDescriptorBufferInfo);
SAME_SIZE(cn1vk_write, VkWriteDescriptorSet);
SAME_AT(cn1vk_write, VkWriteDescriptorSet, pBufferInfo, pBufferInfo);
SAME_SIZE(cn1vk_buffer_info, VkBufferCreateInfo);
SAME_AT(cn1vk_buffer_info, VkBufferCreateInfo, usage, usage);
SAME_SIZE(cn1vk_mem_req, VkMemoryRequirements);
SAME_SIZE(cn1vk_alloc_info, VkMemoryAllocateInfo);
SAME_SIZE(cn1vk_cmd_pool_info, VkCommandPoolCreateInfo);
SAME_SIZE(cn1vk_cmd_alloc_info, VkCommandBufferAllocateInfo);
SAME_SIZE(cn1vk_begin_info, VkCommandBufferBeginInfo);
SAME_SIZE(cn1vk_submit_info, VkSubmitInfo);
SAME_AT(cn1vk_submit_info, VkSubmitInfo, pSignalSemaphores, pSignalSemaphores);
SAME_SIZE(cn1vk_fence_info, VkFenceCreateInfo);
SAME_SIZE(cn1vk_barrier, VkMemoryBarrier);

SAME(CN1VK_ST_APPLICATION_INFO, VK_STRUCTURE_TYPE_APPLICATION_INFO);
SAME(CN1VK_ST_INSTANCE_CREATE_INFO, VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
SAME(CN1VK_ST_DEVICE_QUEUE_CREATE_INFO, VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
SAME(CN1VK_ST_DEVICE_CREATE_INFO, VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
SAME(CN1VK_ST_SUBMIT_INFO, VK_STRUCTURE_TYPE_SUBMIT_INFO);
SAME(CN1VK_ST_MEMORY_ALLOCATE_INFO, VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
SAME(CN1VK_ST_FENCE_CREATE_INFO, VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
SAME(CN1VK_ST_BUFFER_CREATE_INFO, VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
SAME(CN1VK_ST_SHADER_MODULE_CREATE_INFO, VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
SAME(CN1VK_ST_PIPELINE_SHADER_STAGE_CREATE_INFO, VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
SAME(CN1VK_ST_COMPUTE_PIPELINE_CREATE_INFO, VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO);
SAME(CN1VK_ST_PIPELINE_LAYOUT_CREATE_INFO, VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
SAME(CN1VK_ST_DESCRIPTOR_SET_LAYOUT_CREATE_INFO, VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
SAME(CN1VK_ST_DESCRIPTOR_POOL_CREATE_INFO, VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
SAME(CN1VK_ST_DESCRIPTOR_SET_ALLOCATE_INFO, VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
SAME(CN1VK_ST_WRITE_DESCRIPTOR_SET, VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
SAME(CN1VK_ST_COMMAND_POOL_CREATE_INFO, VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
SAME(CN1VK_ST_COMMAND_BUFFER_ALLOCATE_INFO, VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
SAME(CN1VK_ST_COMMAND_BUFFER_BEGIN_INFO, VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
SAME(CN1VK_ST_MEMORY_BARRIER, VK_STRUCTURE_TYPE_MEMORY_BARRIER);
SAME(CN1VK_API_1_0, VK_API_VERSION_1_0);
SAME(CN1VK_QUEUE_COMPUTE, VK_QUEUE_COMPUTE_BIT);
SAME(CN1VK_DEVICE_CPU, VK_PHYSICAL_DEVICE_TYPE_CPU);
SAME(CN1VK_DEVICE_DISCRETE, VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU);
SAME(CN1VK_DEVICE_INTEGRATED, VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU);
SAME(CN1VK_DESCRIPTOR_STORAGE_BUFFER, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
SAME(CN1VK_STAGE_COMPUTE, VK_SHADER_STAGE_COMPUTE_BIT);
SAME(CN1VK_BIND_COMPUTE, VK_PIPELINE_BIND_POINT_COMPUTE);
SAME(CN1VK_USAGE_STORAGE, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
SAME(CN1VK_MEM_HOST_VISIBLE, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
SAME(CN1VK_MEM_HOST_COHERENT, VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
SAME(CN1VK_POOL_RESET_BUFFER, VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
SAME(CN1VK_CMD_ONE_TIME, VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
SAME(CN1VK_PIPE_COMPUTE_SHADER, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT);
SAME(CN1VK_PIPE_HOST, VK_PIPELINE_STAGE_HOST_BIT);
SAME(CN1VK_ACCESS_SHADER_WRITE, VK_ACCESS_SHADER_WRITE_BIT);
SAME(CN1VK_ACCESS_HOST_READ, VK_ACCESS_HOST_READ_BIT);
