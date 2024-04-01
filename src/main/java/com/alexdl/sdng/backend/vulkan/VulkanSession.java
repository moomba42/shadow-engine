package com.alexdl.sdng.backend.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.enums.VkPresentModeKHR;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.alexdl.sdng.backend.vulkan.VulkanUtils.throwIfFailed;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanSession implements AutoCloseable {
    private static final String NULL_STRING = null;

    private final MemoryStack stack;

    public VulkanSession() {
        this.stack = MemoryStack.stackPush();
    }

    @Override
    public void close() {
        stack.close();
    }

    public @Nonnull MemoryStack stack() {
        return stack;
    }

    public @Nonnull PointerBuffer enumeratePhysicalDevices(@Nonnull VkInstance instance) {
        IntBuffer physicalDeviceCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumeratePhysicalDevices(instance, physicalDeviceCountPointer, null));

        PointerBuffer physicalDevices = stack.mallocPointer(physicalDeviceCountPointer.get(0));
        throwIfFailed(vkEnumeratePhysicalDevices(instance, physicalDeviceCountPointer, physicalDevices));

        return physicalDevices;
    }

    public @Nonnull VkExtensionProperties.Buffer enumerateDeviceExtensionProperties(@Nonnull VkPhysicalDevice physicalDevice) {
        IntBuffer deviceExtensionCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateDeviceExtensionProperties(physicalDevice, NULL_STRING, deviceExtensionCountPointer, null));

        VkExtensionProperties.Buffer deviceExtensions = VkExtensionProperties.malloc(deviceExtensionCountPointer.get(0), stack);
        throwIfFailed(vkEnumerateDeviceExtensionProperties(physicalDevice, NULL_STRING, deviceExtensionCountPointer, deviceExtensions));

        return deviceExtensions;
    }

    public @Nonnull VkExtensionProperties.Buffer enumerateInstanceExtensionProperties() {
        IntBuffer extensionCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateInstanceExtensionProperties(NULL_STRING, extensionCountPointer, null));

        VkExtensionProperties.Buffer extensions = VkExtensionProperties.malloc(extensionCountPointer.get(0), stack);
        throwIfFailed(vkEnumerateInstanceExtensionProperties(NULL_STRING, extensionCountPointer, extensions));
        return extensions;
    }

    public @Nonnull VkLayerProperties.Buffer enumerateInstanceLayerProperties() {
        IntBuffer layerCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateInstanceLayerProperties(layerCountPointer, null));

        VkLayerProperties.Buffer layers = VkLayerProperties.malloc(layerCountPointer.get(0), stack);
        throwIfFailed(vkEnumerateInstanceLayerProperties(layerCountPointer, layers));
        return layers;
    }

    public @Nonnull VkPhysicalDeviceProperties getPhysicalDeviceProperties(@Nonnull VkPhysicalDevice physicalDevice) {
        VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
        vkGetPhysicalDeviceProperties(physicalDevice, properties);
        return properties;
    }

    public @Nonnull VkQueueFamilyProperties.Buffer getPhysicalDeviceQueueFamilyProperties(@Nonnull VkPhysicalDevice physicalDevice) {
        IntBuffer queueFamilyCountPointer = stack.mallocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCountPointer, null);

        VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCountPointer.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCountPointer, queueFamilies);
        return queueFamilies;
    }

    public @Nonnull List<VkPresentModeKHR> getPhysicalDeviceSurfacePresentModesKHR(@Nonnull VkPhysicalDevice physicalDevice, @Nonnull VkSurfaceKHR surface) {
        IntBuffer presentationModeCount = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.address(), presentationModeCount, null);
        IntBuffer presentationModeBuffer = stack.mallocInt(presentationModeCount.get(0));
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.address(), presentationModeCount, presentationModeBuffer);

        List<VkPresentModeKHR> list = new ArrayList<>(presentationModeBuffer.limit());
        for (int i = 0; i < presentationModeBuffer.limit(); i++) {
            list.add(i, VkPresentModeKHR.of(presentationModeBuffer.get(i)));
        }
        return list;
    }

    public @Nonnull VkSurfaceFormatKHR.Buffer getPhysicalDeviceSurfaceFormatsKHR(@Nonnull VkPhysicalDevice physicalDevice, @Nonnull VkSurfaceKHR surface) {
        IntBuffer surfaceFormatsCount = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.address(), surfaceFormatsCount, null);
        VkSurfaceFormatKHR.Buffer surfaceFormatBuffer = VkSurfaceFormatKHR.malloc(surfaceFormatsCount.get(0), stack);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.address(), surfaceFormatsCount, surfaceFormatBuffer);
        return surfaceFormatBuffer;
    }

    public @Nonnull VkSurfaceCapabilitiesKHR getPhysicalDeviceSurfaceCapabilitiesKHR(@Nonnull VkPhysicalDevice physicalDevice, @Nonnull VkSurfaceKHR surface) {
        VkSurfaceCapabilitiesKHR surfaceCapabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface.address(), surfaceCapabilities);
        return surfaceCapabilities;
    }

    public @Nonnull List<VkImage> getSwapchainImagesKHR(@Nonnull VkDevice logicalDevice, @Nonnull VkSwapchainKHR swapchain) {
        IntBuffer swapchainImageCount = stack.mallocInt(1);
        vkGetSwapchainImagesKHR(logicalDevice, swapchain.address(), swapchainImageCount, null);
        LongBuffer swapchainImageBuffer = stack.mallocLong(swapchainImageCount.get(0));
        vkGetSwapchainImagesKHR(logicalDevice, swapchain.address(), swapchainImageCount, swapchainImageBuffer);

        List<VkImage> list = new ArrayList<>(swapchainImageBuffer.limit());
        for (int i = 0; i < swapchainImageBuffer.limit(); i++) {
            list.add(i, new VkImage(swapchainImageBuffer.get(i)));
        }
        return list;
    }

    public @Nonnull VkSemaphore createSemaphore(@Nonnull VkDevice logicalDevice) {
        VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack).sType$Default();
        LongBuffer semaphorePointer = stack.mallocLong(1);
        throwIfFailed(vkCreateSemaphore(logicalDevice, semaphoreCreateInfo, null, semaphorePointer));
        return new VkSemaphore(semaphorePointer.get(0));
    }

    public @Nonnull VkFence createFence(@Nonnull VkDevice logicalDevice, int flags) {
        VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(stack).sType$Default().flags(flags);
        LongBuffer fencePointer = stack.mallocLong(1);
        throwIfFailed(vkCreateFence(logicalDevice, fenceCreateInfo, null, fencePointer));
        return new VkFence(fencePointer.get(0));
    }

    public @Nonnull VkBuffer createBuffer(@Nonnull VkDevice logicalDevice, @Nonnull VkBufferCreateInfo info) {
        LongBuffer bufferPointer = stack.mallocLong(1);
        throwIfFailed(vkCreateBuffer(logicalDevice, info, null, bufferPointer));
        return new VkBuffer(bufferPointer.get(0), null);
    }

    public @Nonnull VkMemoryRequirements getBufferMemoryRequirements(@Nonnull VkDevice logicalDevice,
                                                                     @Nonnull VkBuffer buffer) {
        VkMemoryRequirements memoryRequirements = VkMemoryRequirements.malloc(stack);
        vkGetBufferMemoryRequirements(logicalDevice, buffer.address(), memoryRequirements);
        return memoryRequirements;
    }

    public @Nonnull VkPhysicalDeviceMemoryProperties getPhysicalDeviceMemoryProperties(@Nonnull VkPhysicalDevice physicalDevice) {
        VkPhysicalDeviceMemoryProperties physicalDeviceMemoryProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, physicalDeviceMemoryProperties);
        return physicalDeviceMemoryProperties;
    }

    public @Nonnull VkDeviceMemory allocateMemory(@Nonnull VkDevice logicalDevice, VkMemoryAllocateInfo info) {
        LongBuffer deviceMemoryPointer = stack.mallocLong(1);
        throwIfFailed(vkAllocateMemory(logicalDevice, info, null, deviceMemoryPointer));
        return new VkDeviceMemory(deviceMemoryPointer.get(0));
    }

    public void bindBufferMemory(@Nonnull VkDevice logicalDevice, @Nonnull VkBuffer buffer, @Nonnull VkDeviceMemory deviceMemory, long memoryOffsetBytes) {
        vkBindBufferMemory(logicalDevice, buffer.address(), deviceMemory.address(), memoryOffsetBytes);
    }

    public long mapMemoryPointer(@Nonnull VkDevice logicalDevice, @Nonnull VkDeviceMemory deviceMemory, long offsetBytes, long sizeBytes, int flags) {
        PointerBuffer pointer = stack.mallocPointer(1);
        vkMapMemory(logicalDevice, deviceMemory.address(), offsetBytes, sizeBytes, flags, pointer);
        return pointer.get(0);
    }

    public @Nonnull FloatBuffer mapMemoryFloat(@Nonnull VkDevice logicalDevice, @Nonnull VkDeviceMemory deviceMemory, long offsetBytes, long sizeBytes, int flags) {
        PointerBuffer pointer = stack.mallocPointer(1);
        vkMapMemory(logicalDevice, deviceMemory.address(), offsetBytes, sizeBytes, flags, pointer);
        return pointer.getFloatBuffer(0, (int) (sizeBytes / Float.BYTES));
    }

    public @Nonnull IntBuffer mapMemoryInt(@Nonnull VkDevice logicalDevice, @Nonnull VkDeviceMemory deviceMemory, long offsetBytes, long sizeBytes, int flags) {
        PointerBuffer pointer = stack.mallocPointer(1);
        vkMapMemory(logicalDevice, deviceMemory.address(), offsetBytes, sizeBytes, flags, pointer);
        return pointer.getIntBuffer(0, (int) (sizeBytes / Integer.BYTES));
    }

    public @Nonnull ByteBuffer mapMemoryByte(@Nonnull VkDevice logicalDevice, @Nonnull VkDeviceMemory deviceMemory, long offsetBytes, long sizeBytes, int flags) {
        PointerBuffer pointer = stack.mallocPointer(1);
        vkMapMemory(logicalDevice, deviceMemory.address(), offsetBytes, sizeBytes, flags, pointer);
        return pointer.getByteBuffer(0, (int) sizeBytes);
    }

    public void unmapMemory(@Nonnull VkDevice logicalDevice, @Nonnull VkDeviceMemory deviceMemory) {
        vkUnmapMemory(logicalDevice, deviceMemory.address());
    }

    public void destroyBuffer(@Nonnull VkDevice logicalDevice, @Nonnull VkBuffer stagingBuffer, @Nullable VkAllocationCallbacks pAllocator) {
        vkDestroyBuffer(logicalDevice, stagingBuffer.address(), pAllocator);
    }

    public void freeMemory(@Nonnull VkDevice logicalDevice, @Nonnull VkDeviceMemory memory, @Nullable VkAllocationCallbacks pAllocator) {
        vkFreeMemory(logicalDevice, memory.address(), pAllocator);
    }

    public @Nonnull List<VkCommandBuffer> allocateCommandBuffers(@Nonnull VkDevice logicalDevice, @Nonnull VkCommandBufferAllocateInfo info) {
        PointerBuffer pointer = stack.mallocPointer(info.commandBufferCount());
        throwIfFailed(vkAllocateCommandBuffers(logicalDevice, info, pointer));
        List<VkCommandBuffer> buffers = new ArrayList<>(pointer.limit());
        for (int i = 0; i < pointer.limit(); i++) {
            buffers.add(i, new VkCommandBuffer(pointer.get(i), logicalDevice));
        }
        return buffers;
    }

    public void beginCommandBuffer(@Nonnull VkCommandBuffer commandBuffer, @Nonnull VkCommandBufferBeginInfo beginInfo) {
        vkBeginCommandBuffer(commandBuffer, beginInfo);
    }

    public void cmdCopyBuffer(@Nonnull VkCommandBuffer transferCommandBuffer, @Nonnull VkBuffer srcBuffer, @Nonnull VkBuffer dstBuffer, @Nonnull VkBufferCopy.Buffer params) {
        vkCmdCopyBuffer(transferCommandBuffer, srcBuffer.address(), dstBuffer.address(), params);
    }

    public void endCommandBuffer(@Nonnull VkCommandBuffer commandBuffer) {
        vkEndCommandBuffer(commandBuffer);
    }

    public void queueSubmit(@Nonnull VkQueue queue, @Nonnull VkSubmitInfo submitInfo, @Nullable VkFence fence) {
        vkQueueSubmit(queue, submitInfo, fence == null ? VK_NULL_HANDLE : fence.address());
    }

    public void queueWaitIdle(@Nonnull VkQueue queue) {
        vkQueueWaitIdle(queue);
    }

    public void freeCommandBuffers(@Nonnull VkDevice logicalDevice, @Nonnull VkCommandPool commandPool, @Nonnull VkCommandBuffer commandBuffer) {
        vkFreeCommandBuffers(logicalDevice, commandPool.address(), commandBuffer);
    }

    public @Nonnull VkDescriptorSetLayout createDescriptorSetLayout(@Nonnull VkDevice logicalDevice, @Nonnull VkDescriptorSetLayoutCreateInfo createInfo, @Nullable VkAllocationCallbacks allocator) {
        LongBuffer pointer = stack.mallocLong(1);
        throwIfFailed(vkCreateDescriptorSetLayout(logicalDevice, createInfo, allocator, pointer));
        return new VkDescriptorSetLayout(pointer.get(0));
    }

    public @Nonnull VkPipelineLayout createPipelineLayout(@Nonnull VkDevice logicalDevice, @Nonnull VkPipelineLayoutCreateInfo createInfo, @Nullable VkAllocationCallbacks allocator) {
        LongBuffer pointer = stack.mallocLong(1);
        throwIfFailed(vkCreatePipelineLayout(logicalDevice, createInfo, allocator, pointer));
        return new VkPipelineLayout(pointer.get(0));
    }

    public @Nonnull VkRenderPass createRenderPass(@Nonnull VkDevice logicalDevice, @Nonnull VkRenderPassCreateInfo createInfo, @Nullable VkAllocationCallbacks allocator) {
        LongBuffer pointer = stack.mallocLong(1);
        throwIfFailed(vkCreateRenderPass(logicalDevice, createInfo, allocator, pointer));
        return new VkRenderPass(pointer.get(0));
    }

    public @Nonnull VkShaderModule createShaderModule(@Nonnull VkDevice logicalDevice, @Nonnull VkShaderModuleCreateInfo createInfo, @Nullable VkAllocationCallbacks allocator) {
        LongBuffer pointer = stack.mallocLong(1);
        throwIfFailed(vkCreateShaderModule(logicalDevice, createInfo, allocator, pointer));
        return new VkShaderModule(pointer.get(0));
    }

    public @Nonnull List<VkPipeline> createGraphicsPipelines(@Nonnull VkDevice logicalDevice, @Nullable VkPipelineCache pipelineCache, @Nonnull VkGraphicsPipelineCreateInfo.Buffer createInfos, @Nullable VkAllocationCallbacks allocator) {
        LongBuffer pointers = stack.mallocLong(createInfos.limit());
        long pipelineCacheAddress = pipelineCache != null ? pipelineCache.address() : VK_NULL_HANDLE;
        throwIfFailed(vkCreateGraphicsPipelines(logicalDevice, pipelineCacheAddress, createInfos, allocator, pointers));
        List<VkPipeline> pipelines = new ArrayList<>(pointers.limit());
        for (int i = 0; i < pointers.limit(); i++) {
            pipelines.add(i, new VkPipeline(pointers.get(i)));
        }
        return pipelines;
    }

    public void destroyShaderModule(@Nonnull VkDevice logicalDevice, @Nonnull VkShaderModule fragmentShaderModule, @Nullable VkAllocationCallbacks allocator) {
        vkDestroyShaderModule(logicalDevice, fragmentShaderModule.address(), allocator);
    }

    public @Nonnull VkDescriptorPool createDescriptorPool(@Nonnull VkDevice logicalDevice, @Nonnull VkDescriptorPoolCreateInfo createInfo, @Nullable VkAllocationCallbacks allocator) {
        LongBuffer pointer = stack.mallocLong(1);
        throwIfFailed(vkCreateDescriptorPool(logicalDevice, createInfo, allocator, pointer));
        return new VkDescriptorPool(pointer.get(0));
    }

    public @Nonnull List<VkDescriptorSet> allocateDescriptorSets(@Nonnull VkDevice logicalDevice, @Nonnull VkDescriptorSetAllocateInfo createInfo) {
        LongBuffer pointers = stack.mallocLong(createInfo.descriptorSetCount());
        throwIfFailed(vkAllocateDescriptorSets(logicalDevice, createInfo, pointers));
        List<VkDescriptorSet> descriptorSets = new ArrayList<>(pointers.limit());
        for (int i = 0; i < pointers.limit(); i++) {
            descriptorSets.add(i, new VkDescriptorSet(pointers.get(i)));
        }
        return descriptorSets;
    }

    public void updateDescriptorSets(@Nonnull VkDevice logicalDevice, @Nullable VkWriteDescriptorSet.Buffer descriptorSetWrites, @Nullable VkCopyDescriptorSet.Buffer descriptorSetCopies) {
        vkUpdateDescriptorSets(logicalDevice, descriptorSetWrites, descriptorSetCopies);
    }
}
