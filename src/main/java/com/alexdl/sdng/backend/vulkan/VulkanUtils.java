package com.alexdl.sdng.backend.vulkan;

import com.alexdl.sdng.backend.vulkan.structs.VertexDataStruct;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.function.Function;

import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_SURFACE_LOST_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.VK10.*;

@SuppressWarnings("SameParameterValue")
public class VulkanUtils {

    public static String getVkDebugMessageType(int messageTypes) {
        String type;
        if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT) != 0) {
            type = "GENERAL";
        } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT) != 0) {
            type = "VALIDATION";
        } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT) != 0) {
            type = "PERFORMANCE";
        } else {
            type = "UNKNOWN";
        }
        return type;
    }

    public static String getVkDebugMessageSeverity(int messageSeverity) {
        String severity;
        if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT) != 0) {
            severity = "VERBOSE";
        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
            severity = "INFO";
        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
            severity = "WARNING";
        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
            severity = "ERROR";
        } else {
            severity = "UNKNOWN";
        }
        return severity;
    }

    public static void throwIfFailed(int vulkanResultCode) {
        if (vulkanResultCode != VK_SUCCESS) {
            throw new VulkanRuntimeException(vulkanResultCode, translateVulkanResult(vulkanResultCode));
        }
    }

    public static String translateVulkanResult(int vulkanResultCode) {
        return switch (vulkanResultCode) {
            // Success codes
            case VK_SUCCESS -> "Command successfully completed.";
            case VK_NOT_READY -> "A fence or query has not yet completed.";
            case VK_TIMEOUT -> "A wait operation has not completed in the specified time.";
            case VK_EVENT_SET -> "An event is signaled.";
            case VK_EVENT_RESET -> "An event is unsignaled.";
            case VK_INCOMPLETE -> "A return array was too small for the result.";
            case VK_SUBOPTIMAL_KHR ->
                    "A swap chain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";

            // Error codes
            case VK_ERROR_OUT_OF_HOST_MEMORY -> "A host memory allocation has failed.";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "A device memory allocation has failed.";
            case VK_ERROR_INITIALIZATION_FAILED ->
                    "Initialization of an object could not be completed for implementation-specific reasons.";
            case VK_ERROR_DEVICE_LOST -> "The logical or physical device has been lost.";
            case VK_ERROR_MEMORY_MAP_FAILED -> "Mapping of a memory object has failed.";
            case VK_ERROR_LAYER_NOT_PRESENT -> "A requested layer is not present or could not be loaded.";
            case VK_ERROR_EXTENSION_NOT_PRESENT -> "A requested extension is not supported.";
            case VK_ERROR_FEATURE_NOT_PRESENT -> "A requested feature is not supported.";
            case VK_ERROR_INCOMPATIBLE_DRIVER ->
                    "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
            case VK_ERROR_TOO_MANY_OBJECTS -> "Too many objects of the type have already been created.";
            case VK_ERROR_FORMAT_NOT_SUPPORTED -> "A requested format is not supported on this device.";
            case VK_ERROR_SURFACE_LOST_KHR -> "A surface is no longer available.";
            case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR ->
                    "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
            case VK_ERROR_OUT_OF_DATE_KHR ->
                    "A surface has changed in such a way that it is no longer compatible with the swap chain, and further presentation requests using the "
                    + "swap chain will fail. Applications must query the new surface properties and recreate their swap chain if they wish to continue"
                    + "presenting to the surface.";
            case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR ->
                    "The display used by a swap chain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an"
                    + " image.";
            case VK_ERROR_VALIDATION_FAILED_EXT -> "A validation layer found an error.";
            default -> "Unknown result code";
        };
    }

    public static <T> LongBuffer toAddressBuffer(List<T> objects, MemoryStack stack, Function<T, Long> mapping) {
        LongBuffer buffer = stack.mallocLong(objects.size());
        for (int i = 0; i < objects.size(); i++) {
            buffer.put(i, mapping.apply(objects.get(i)));
        }
        return buffer;
    }

    public static VkBuffer createBuffer(VkDevice logicalDevice, long size, int usage, int memoryFlags) {
        try (VulkanSession vk = new VulkanSession()) {
            var bufferCreateInfo = VkBufferCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .size(size)
                    .usage(usage)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            VkBuffer buffer = vk.createBuffer(logicalDevice, bufferCreateInfo);

            var memoryRequirements = vk.getBufferMemoryRequirements(logicalDevice, buffer);
            var memoryProperties = vk.getPhysicalDeviceMemoryProperties(logicalDevice.getPhysicalDevice());
            int memoryTypeIndex = -1;
            for (int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
                if ((memoryRequirements.memoryTypeBits() & (1 << i)) != 0 &&
                    (memoryProperties.memoryTypes(i).propertyFlags() & memoryFlags) == memoryFlags) {
                    memoryTypeIndex = i;
                    break;
                }
            }

            var memoryAllocateInfo = VkMemoryAllocateInfo.calloc(vk.stack())
                    .sType$Default()
                    .allocationSize(memoryRequirements.size())
                    .memoryTypeIndex(memoryTypeIndex);
            VkDeviceMemory bufferMemory = vk.allocateMemory(logicalDevice, memoryAllocateInfo);

            vk.bindBufferMemory(logicalDevice, buffer, bufferMemory, 0);

            return new VkBuffer(buffer.address(), bufferMemory);
        }
    }

    public static VkCommandBuffer beginCommandBuffer(VkDevice logicalDevice, VkCommandPool commandPool) {
        try(VulkanSession vk = new VulkanSession()) {
            VkCommandBufferAllocateInfo commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(vk.stack())
                    .sType$Default()
                    .commandPool(commandPool.address())
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);
            VkCommandBuffer commandBuffer = vk.allocateCommandBuffers(logicalDevice, commandBufferAllocateInfo).getFirst();

            VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc(vk.stack())
                    .sType$Default()
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            vk.beginCommandBuffer(commandBuffer, commandBufferBeginInfo);

            return commandBuffer;
        }
    }

    public static void endAndSubmitCommandBuffer(VkDevice logicalDevice, VkCommandPool commandPool, VkQueue queue, VkCommandBuffer commandBuffer) {
        try(VulkanSession vk = new VulkanSession()) {
            vk.endCommandBuffer(commandBuffer);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(vk.stack())
                    .sType$Default()
                    .pCommandBuffers(vk.stack().pointers(commandBuffer));
            vk.queueSubmit(queue, submitInfo, null);

            vk.queueWaitIdle(queue);
            vk.freeCommandBuffers(logicalDevice, commandPool, commandBuffer);
        }
    }

    public static VkBuffer createVertexBuffer(VertexDataStruct.Buffer vertexData, VkQueue transferQueue, VkCommandPool transferCommandPool) {
        try (VulkanSession vk = new VulkanSession()) {
            long bufferSize = vertexData.size();
            VkDevice logicalDevice = transferQueue.getDevice();
            VkBuffer stagingBuffer = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            assert stagingBuffer.memory() != null;

            long stagingBufferMappedMemoryAddress = vk.mapMemoryPointer(logicalDevice, stagingBuffer.memory(), 0, bufferSize, 0);
            memCopy(vertexData.address(), stagingBufferMappedMemoryAddress, bufferSize);
            vk.unmapMemory(logicalDevice, stagingBuffer.memory());

            VkBuffer vertexBuffer = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            copyBuffer(transferQueue, transferCommandPool, stagingBuffer, vertexBuffer, bufferSize);
            vk.destroyBuffer(logicalDevice, stagingBuffer, null);
            vk.freeMemory(logicalDevice, stagingBuffer.memory(), null);

            return vertexBuffer;
        }
    }

    public static VkBuffer createIndexBuffer(IntBuffer indexData, VkQueue transferQueue, VkCommandPool transferCommandPool) {
        try (VulkanSession vk = new VulkanSession()) {
            long bufferSize = (long) Integer.BYTES * indexData.limit();
            VkDevice logicalDevice = transferQueue.getDevice();
            VkBuffer stagingBuffer = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            assert stagingBuffer.memory() != null;

            long stagingBufferMappedMemoryAddress = vk.mapMemoryPointer(logicalDevice, stagingBuffer.memory(), 0, bufferSize, 0);
            memCopy(memAddress(indexData), stagingBufferMappedMemoryAddress, bufferSize);
            vk.unmapMemory(logicalDevice, stagingBuffer.memory());

            VkBuffer indexBuffer = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            copyBuffer(transferQueue, transferCommandPool, stagingBuffer, indexBuffer, bufferSize);
            vk.destroyBuffer(logicalDevice, stagingBuffer, null);
            vk.freeMemory(logicalDevice, stagingBuffer.memory(), null);

            return indexBuffer;
        }
    }

    public static void copyBuffer(VkQueue transferQueue, VkCommandPool transferCommandPool, VkBuffer srcBuffer, VkBuffer dstBuffer, long bufferSize) {
        try (VulkanSession vk = new VulkanSession()) {
            VkDevice logicalDevice = transferQueue.getDevice();
            VkCommandBuffer transferCommandBuffer = beginCommandBuffer(logicalDevice, transferCommandPool);

            VkBufferCopy.Buffer bufferRegionCopy = VkBufferCopy.calloc(1, vk.stack())
                    .srcOffset(0)
                    .dstOffset(0)
                    .size(bufferSize);
            vk.cmdCopyBuffer(transferCommandBuffer, srcBuffer, dstBuffer, bufferRegionCopy);

            endAndSubmitCommandBuffer(logicalDevice, transferCommandPool, transferQueue, transferCommandBuffer);
        }
    }
}
