package com.alexdl.sdng.backend.vulkan;

import com.alexdl.sdng.backend.vulkan.structs.VertexDataStruct;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.enums.VkColorSpaceKHR;
import org.lwjgl.vulkan.enums.VkFormat;
import org.lwjgl.vulkan.enums.VkImageLayout;
import org.lwjgl.vulkan.enums.VkImageTiling;
import org.lwjgl.vulkan.enums.VkPresentModeKHR;
import org.lwjgl.vulkan.enums.VkSharingMode;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
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


    public static List<VkDescriptorSet> createDescriptorSets(VkDevice logicalDevice, VkDescriptorPool descriptorPool, VkDescriptorSetLayout descriptorSetLayout, int size) {
        try (VulkanSession vk = new VulkanSession()) {
            LongBuffer setLayouts = vk.stack().mallocLong(size);
            for (int i = 0; i < size; i++) {
                setLayouts.put(i, descriptorSetLayout.address());
            }

            VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.calloc(vk.stack())
                    .sType$Default()
                    .descriptorPool(descriptorPool.address())
                    .pSetLayouts(setLayouts);
            return vk.allocateDescriptorSets(logicalDevice, descriptorSetAllocateInfo);
        }
    }

    public static VkDescriptorPool createDescriptorPool(VkDevice logicalDevice, int sceneDescriptorCount, int modelDescriptorCount, int maxSets) {
        try (VulkanSession vk = new VulkanSession()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, vk.stack());
            poolSizes.get(0)
                    .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(sceneDescriptorCount);
            poolSizes.get(1)
                    .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
                    .descriptorCount(modelDescriptorCount);
            VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .maxSets(maxSets)
                    .pPoolSizes(poolSizes);

            return vk.createDescriptorPool(logicalDevice, descriptorPoolCreateInfo, null);
        }
    }

    public static VkDescriptorPool createSamplerDescriptorPool(VkDevice logicalDevice, int samplerDescriptorCount, int maxSets) {
        try (VulkanSession vk = new VulkanSession()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(1, vk.stack());
            poolSizes.get(0)
                    .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(samplerDescriptorCount);
            VkDescriptorPoolCreateInfo poolCreateInfo = VkDescriptorPoolCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .maxSets(maxSets)
                    .pPoolSizes(poolSizes);

            return vk.createDescriptorPool(logicalDevice, poolCreateInfo, null);
        }
    }

    public static List<VkBuffer> createUniformBuffers(VkDevice logicalDevice, int bufferCount, int bufferSize) {
        List<VkBuffer> buffers = new ArrayList<>(bufferCount);
        for (int i = 0; i < bufferCount; i++) {
            buffers.add(i, createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT));
        }
        return buffers;
    }

    public static VkSampler createTextureSampler(VkDevice logicalDevice) {
        try (VulkanSession vk = new VulkanSession()) {
            VkSamplerCreateInfo samplerCreateInfo = VkSamplerCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .magFilter(VK_FILTER_LINEAR)
                    .minFilter(VK_FILTER_LINEAR)
                    .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .mipLodBias(0)
                    .anisotropyEnable(true)
                    .maxAnisotropy(16)
                    .minLod(0)
                    .maxLod(0)
                    .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                    .unnormalizedCoordinates(false);
            return vk.createSampler(logicalDevice, samplerCreateInfo, null);
        }
    }


    public static int findMemoryTypeIndex(VkPhysicalDevice physicalDevice, int allowedTypes, VkMemoryPropertyFlags requiredFlags) {
        try (VulkanSession vk = new VulkanSession()) {
            VkPhysicalDeviceMemoryProperties memoryProperties = vk.getPhysicalDeviceMemoryProperties(physicalDevice);
            for (int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
                if ((allowedTypes & (1 << i)) != 0 && (memoryProperties.memoryTypes(i).propertyFlags() & requiredFlags.value()) == requiredFlags.value()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static Image createDepthBufferImage(VkDevice logicalDevice, int width, int height) {
        Set<VkFormat> allowedFormats = Set.of(VkFormat.VK_FORMAT_D32_SFLOAT_S8_UINT, VkFormat.VK_FORMAT_D32_SFLOAT, VkFormat.VK_FORMAT_D24_UNORM_S8_UINT, VkFormat.VK_FORMAT_D16_UNORM_S8_UINT);
        VkFormat format = findBestImageFormat(logicalDevice, allowedFormats, VkImageTiling.VK_IMAGE_TILING_OPTIMAL, new VkFormatFeatureFlags(VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT));
        return createImage(logicalDevice, width, height, format,
                VkImageTiling.VK_IMAGE_TILING_OPTIMAL,
                new VkImageUsageFlags(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT),
                new VkMemoryPropertyFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
                new VkImageAspectFlags(VK_IMAGE_ASPECT_DEPTH_BIT));
    }

    public static @Nonnull Image createImage(@Nonnull VkDevice logicalDevice, int width, int height, @Nonnull VkFormat format, @Nonnull VkImageTiling tiling, @Nonnull VkImageUsageFlags usageFlags, @Nonnull VkMemoryPropertyFlags memoryFlags, VkImageAspectFlags imageAspectFlags) {
        try (VulkanSession vk = new VulkanSession()) {
            VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(format.getValue())
                    .extent(VkExtent3D.calloc(vk.stack())
                            .width(width)
                            .height(height)
                            .depth(1)
                    )
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(tiling.getValue())
                    .usage(usageFlags.value())
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE) // Whether image can be shared between queues
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            VkImage image = vk.createImage(logicalDevice, imageCreateInfo, null);

            VkMemoryRequirements memoryRequirements = vk.getImageMemoryRequirements(logicalDevice, image);
            VkMemoryAllocateInfo memoryAllocateInfo = VkMemoryAllocateInfo.calloc(vk.stack())
                    .sType$Default()
                    .allocationSize(memoryRequirements.size())
                    .memoryTypeIndex(findMemoryTypeIndex(logicalDevice.getPhysicalDevice(), memoryRequirements.memoryTypeBits(), memoryFlags));
            VkDeviceMemory deviceMemory = vk.allocateMemory(logicalDevice, memoryAllocateInfo);
            vk.bindImageMemory(logicalDevice, image, deviceMemory, 0);

            VkImageView imageView = createImageView(logicalDevice, image, format, imageAspectFlags);

            return new Image(
                    format,
                    image,
                    deviceMemory,
                    imageView
            );
        }
    }

    public static VkFormat findBestImageFormat(VkDevice logicalDevice, Set<VkFormat> possibleFormats, VkImageTiling tiling, VkFormatFeatureFlags featureFlags) {
        try (VulkanSession vk = new VulkanSession()) {
            for (VkFormat format : possibleFormats) {
                VkFormatProperties properties = vk.getPhysicalDeviceFormatProperties(logicalDevice.getPhysicalDevice(), format);

                if (VkImageTiling.VK_IMAGE_TILING_LINEAR.equals(tiling) && (properties.linearTilingFeatures() & featureFlags.value()) == featureFlags.value()) {
                    return format;
                } else if (VkImageTiling.VK_IMAGE_TILING_OPTIMAL.equals(tiling) && (properties.optimalTilingFeatures() & featureFlags.value()) == featureFlags.value()) {
                    return format;
                }
            }
        }

        throw new RuntimeException("Failed to find best image format");
    }

    public static void copyColorImageBuffer(VkDevice logicalDevice, VkQueue queue, VkCommandPool commandPool, VkBuffer srcBuffer, VkImage dstImage, int width, int height) {
        try (VulkanSession vk = new VulkanSession()) {
            VkCommandBuffer commandBuffer = beginCommandBuffer(logicalDevice, commandPool);

            VkBufferImageCopy bufferImageCopy = VkBufferImageCopy.calloc(vk.stack())
                    .bufferOffset(0)
                    .bufferRowLength(0)
                    .bufferImageHeight(0)
                    .imageSubresource(VkImageSubresourceLayers.calloc(vk.stack())
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .mipLevel(0)
                            .baseArrayLayer(0)
                            .layerCount(1))
                    .imageOffset(VkOffset3D.calloc(vk.stack()).set(0, 0, 0))
                    .imageExtent(VkExtent3D.calloc(vk.stack()).set(width, height, 1));
            vk.cmdCopyBufferToImage(commandBuffer, srcBuffer, dstImage, VkImageLayout.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, bufferImageCopy);

            endAndSubmitCommandBuffer(logicalDevice, commandPool, queue, commandBuffer);
        }
    }

    public static void transitionImageLayout(VkDevice logicalDevice, VkQueue queue, VkCommandPool commandPool, VkImage image, VkImageLayout currentLayout, VkImageLayout targetLayout) {
        try (VulkanSession vk = new VulkanSession()) {
            VkCommandBuffer commandBuffer = beginCommandBuffer(logicalDevice, commandPool);

            VkImageMemoryBarrier.Buffer imageMemoryBarrier = VkImageMemoryBarrier.calloc(1, vk.stack())
                    .sType$Default()
                    .oldLayout(currentLayout.getValue())
                    .newLayout(targetLayout.getValue())
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image.address())
                    .subresourceRange(VkImageSubresourceRange.calloc(vk.stack())
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1));

            VkPipelineStageFlags srcStage;
            VkPipelineStageFlags dstStage;

            if (currentLayout == VkImageLayout.VK_IMAGE_LAYOUT_UNDEFINED && targetLayout == VkImageLayout.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                imageMemoryBarrier.srcAccessMask(0); // Transition must happen AFTER this memory access stage
                imageMemoryBarrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT); // Transition must happen BEFORE this memory access stage
                srcStage = new VkPipelineStageFlags(VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT);
                dstStage = new VkPipelineStageFlags(VK_PIPELINE_STAGE_TRANSFER_BIT);
            } else if (currentLayout == VkImageLayout.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && targetLayout == VkImageLayout.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                imageMemoryBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT); // Transition must happen AFTER this memory access stage
                imageMemoryBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT); // Transition must happen BEFORE this memory access stage
                srcStage = new VkPipelineStageFlags(VK_PIPELINE_STAGE_TRANSFER_BIT);
                dstStage = new VkPipelineStageFlags(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT);
            } else {
                throw new RuntimeException(String.format("Unsupported image layout combination: %s / %s", currentLayout, targetLayout));
            }

            vk.cmdPipelineBarrier(commandBuffer, srcStage, dstStage, null, null, null, imageMemoryBarrier);

            endAndSubmitCommandBuffer(logicalDevice, commandPool, queue, commandBuffer);
        }
    }

    public static List<String> getAllGlfwExtensions() {
        PointerBuffer glfwExtensionsBuffer = glfwGetRequiredInstanceExtensions();
        if (glfwExtensionsBuffer == null) {
            throw new RuntimeException("No set of extensions allowing GLFW integration was found");
        }
        List<String> glfwExtensions = new ArrayList<>(glfwExtensionsBuffer.limit());
        for (int i = 0; i < glfwExtensionsBuffer.capacity(); i++) {
            glfwExtensions.add(i, memASCII(glfwExtensionsBuffer.get(i)));
        }
        return glfwExtensions;
    }

    public static @Nonnull VkPhysicalDevice findFirstSuitablePhysicalDevice(VkInstance instance, VkSurfaceKHR surface) {
        try (VulkanSession vk = new VulkanSession()) {
            PointerBuffer physicalDevices = vk.enumeratePhysicalDevices(instance);
            if (physicalDevices.limit() == 0) {
                throw new RuntimeException("Could not find a suitable physical device");
            }

            for (int i = 0; i < physicalDevices.limit(); i++) {
                VkPhysicalDevice physicalDevice = new VkPhysicalDevice(physicalDevices.get(i), instance);
                if (isSuitableDevice(physicalDevice, surface)) {
                    return physicalDevice;
                }
            }
        }
        throw new RuntimeException("Could not find a suitable physical device");
    }

    public static boolean isSuitableDevice(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        try (VulkanSession vk = new VulkanSession()) {
            VkPhysicalDeviceFeatures deviceFeatures = vk.getPhysicalDeviceFeatures(physicalDevice);

            IntBuffer surfaceFormatCountBuffer = vk.stack().mallocInt(1);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.address(), surfaceFormatCountBuffer, null);
            int surfaceFormatCount = surfaceFormatCountBuffer.get(0);

            IntBuffer presentationModeCountBuffer = vk.stack().mallocInt(1);
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.address(), presentationModeCountBuffer, null);
            int presentationModeCount = presentationModeCountBuffer.get(0);

            QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);

            return queueIndices.graphical() >= 0 &&
                   queueIndices.surfaceSupporting() >= 0 &&
                   deviceSupportsExtensions(physicalDevice, List.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME)) &&
                   surfaceFormatCount > 0 &&
                   presentationModeCount > 0 &&
                   deviceFeatures.samplerAnisotropy();
        }
    }

    public static boolean deviceSupportsExtensions(VkPhysicalDevice physicalDevice, List<String> expectedExtensionNames) {
        try (VulkanSession vk = new VulkanSession()) {
            VkExtensionProperties.Buffer availableExtensionsBuffer = vk.enumerateDeviceExtensionProperties(physicalDevice);
            for (String expectedExtensionName : expectedExtensionNames) {
                boolean isSupported = false;
                for (VkExtensionProperties availableExtensionProperty : availableExtensionsBuffer) {
                    if (availableExtensionProperty.extensionNameString().equals(expectedExtensionName)) {
                        isSupported = true;
                    }
                }
                if (!isSupported) {
                    return false;
                }
            }
            return true;
        }
    }

    public static QueueIndices findQueueIndices(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        try (VulkanSession vk = new VulkanSession()) {
            VkQueueFamilyProperties.Buffer queueFamilies = vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice);
            List<Integer> graphicalQueueIndices = new ArrayList<>(1);
            List<Integer> surfaceSupportingQueueIndices = new ArrayList<>(1);
            for (int i = 0; i < queueFamilies.queueCount(); i++) {
                VkQueueFamilyProperties queueFamily = queueFamilies.get(i);
                if (queueFamily.queueCount() <= 0) {
                    continue;
                }

                if (queueFamily.queueCount() > 0 && (queueFamily.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    graphicalQueueIndices.add(i);
                }

                IntBuffer supportSurfacePointer = vk.stack().mallocInt(1);
                vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, surface.address(), supportSurfacePointer);

                if (supportSurfacePointer.get(0) != 0) {
                    surfaceSupportingQueueIndices.add(i);
                }
            }
            return new QueueIndices(
                    graphicalQueueIndices.isEmpty() ? -1 : graphicalQueueIndices.getFirst(),
                    surfaceSupportingQueueIndices.isEmpty() ? -1 : surfaceSupportingQueueIndices.getFirst()
            );
        }
    }

    public static VkInstance createInstance(boolean enableDebugging) {
        try (VulkanSession vk = new VulkanSession()) {
            // Info about the app itself
            VkApplicationInfo applicationInfo = VkApplicationInfo.malloc(vk.stack())
                    // We specify the type of struct that this struct is because there is no reflection in C.
                    .sType$Default()
                    .pNext(NULL)
                    .pApplicationName(vk.stack().UTF8("Vulkan Test App"))
                    .applicationVersion(VK_MAKE_API_VERSION(0, 1, 0, 0))
                    .pEngineName(vk.stack().UTF8("Shadow Engine"))
                    .apiVersion(VK_MAKE_API_VERSION(0, 1, 1, 0)); // this affects the app

            // Create flags list
            int flags = 0;

            // Extension list
            HashSet<String> requiredExtensions = new HashSet<>(getAllGlfwExtensions());
            HashSet<String> availableExtensions = vk.enumerateInstanceExtensionProperties().stream().map(VkExtensionProperties::extensionNameString).collect(Collectors.toCollection(HashSet::new));
            for (String requiredExtension : requiredExtensions) {
                if (!availableExtensions.contains(requiredExtension)) {
                    throw new RuntimeException("Unsupported extension: " + requiredExtension);
                }
            }
            // Required on macOS in later version of vulkan SDK
            if (availableExtensions.contains(KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME)) {
                requiredExtensions.add(KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME);
                flags |= KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
            }
            if (enableDebugging && availableExtensions.contains(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)) {
                requiredExtensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
            }
            PointerBuffer requiredExtensionsBuffer = vk.stack().pointers(requiredExtensions.stream().map(vk.stack()::UTF8).toArray(ByteBuffer[]::new));

            // Validation layer list
            PointerBuffer requiredLayersBuffer = enableDebugging ? allocateAndValidateLayerList(List.of(
                    // This will not exist if you're using the bundled lwjgl MoltenVK lib.
                    // See README.md#Vulkan_validation_layers
                    "VK_LAYER_KHRONOS_validation"
            ), vk) : null;

            // Create instance
            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.malloc(vk.stack())
                    .sType$Default()
                    .pNext(NULL)
                    .flags(flags)
                    .pApplicationInfo(applicationInfo)
                    .ppEnabledLayerNames(requiredLayersBuffer)
                    .ppEnabledExtensionNames(requiredExtensionsBuffer);

            PointerBuffer instancePointer = vk.stack().mallocPointer(1);
            throwIfFailed(vkCreateInstance(instanceCreateInfo, null, instancePointer));
            return new VkInstance(instancePointer.get(0), instanceCreateInfo);
        }
    }

    public static VkSurfaceKHR createSurface(VkInstance instance, long window) {
        try (VulkanSession vk = new VulkanSession()) {
            LongBuffer surfacePointer = vk.stack().mallocLong(1);
            throwIfFailed(glfwCreateWindowSurface(instance, window, null, surfacePointer));
            return new VkSurfaceKHR(surfacePointer.get(0));
        }
    }

    public static final VkDebugUtilsMessengerCallbackEXT debugCallbackFunction = VkDebugUtilsMessengerCallbackEXT.create(
            (messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                String severity = getVkDebugMessageSeverity(messageSeverity);

                String type = getVkDebugMessageType(messageTypes);

                VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                System.err.format(
                        "%s %s: [%s]\n\t%s\n",
                        type, severity, data.pMessageIdNameString(), data.pMessageString()
                );

                /*
                 * false indicates that layer should not bail out of an
                 * API call that had validation failures. This may mean that the
                 * app dies inside the driver due to invalid parameter(s).
                 * That's what would happen without validation layers, so we'll
                 * keep that behavior here.
                 */
                return VK_FALSE;
            }
    );

    public static long createDebugMessenger(VkInstance instance) {
        try (VulkanSession vk = new VulkanSession()) {
            VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.malloc(vk.stack())
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .messageSeverity(
                            // VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                            // VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                    )
                    .messageType(
                            VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                    )
                    .pfnUserCallback(debugCallbackFunction)
                    .pUserData(NULL);
            LongBuffer callbackPointer = vk.stack().mallocLong(1);
            vkCreateDebugUtilsMessengerEXT(instance, debugCreateInfo, null, callbackPointer);
            return callbackPointer.get(0);
        }
    }

    public static PointerBuffer allocateAndValidateLayerList(List<String> requiredLayerNames, VulkanSession vk) {
        VkLayerProperties.Buffer availableLayersBuffer = vk.enumerateInstanceLayerProperties();
        List<String> availableLayerNames = availableLayersBuffer.stream().map(VkLayerProperties::layerNameString).toList();
        for (String requiredLayerName : requiredLayerNames) {
            boolean found = false;
            for (String availableLayerName : availableLayerNames) {
                if (Objects.equals(availableLayerName, requiredLayerName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new RuntimeException("Requested layer name is not available: " + requiredLayerName);
            }
        }

        return vk.stack().pointers(requiredLayerNames.stream().map(vk.stack()::UTF8).toArray(ByteBuffer[]::new));
    }

    public static VkDevice createLogicalDevice(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        try (VulkanSession vk = new VulkanSession()) {
            QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);
            // Queues
            List<VkDeviceQueueCreateInfo> queueCreateInfos = new ArrayList<>(1);
            queueCreateInfos.add(VkDeviceQueueCreateInfo.malloc(vk.stack())
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .queueFamilyIndex(queueIndices.graphical())
                    .pQueuePriorities(vk.stack().floats(1.0f)));
            if (queueIndices.graphical() != queueIndices.surfaceSupporting()) {
                queueCreateInfos.add(VkDeviceQueueCreateInfo.malloc(vk.stack())
                        .sType$Default()
                        .pNext(NULL)
                        .flags(0)
                        .queueFamilyIndex(queueIndices.surfaceSupporting())
                        .pQueuePriorities(vk.stack().floats(1.0f)));
            }
            VkDeviceQueueCreateInfo.Buffer queueCreateInfosBuffer = VkDeviceQueueCreateInfo.malloc(queueCreateInfos.size());
            for (int i = 0; i < queueCreateInfos.size(); i++) {
                queueCreateInfosBuffer.put(i, queueCreateInfos.get(i));
            }

            // Features
            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(vk.stack())
                    .samplerAnisotropy(true);

            // Extensions
            VkExtensionProperties.Buffer availableExtensions = vk.enumerateDeviceExtensionProperties(physicalDevice);
            List<String> availableExtensionNames = availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .toList();
            List<String> requiredExtensionNames = new ArrayList<>();
            requiredExtensionNames.add(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
            for (String availableDeviceExtensionName : availableExtensionNames) {
                if (Objects.equals(availableDeviceExtensionName, "VK_KHR_portability_subset")) {
                    requiredExtensionNames.add("VK_KHR_portability_subset");
                }
            }
            PointerBuffer requiredExtensionNamesBuffer = vk.stack().pointers(requiredExtensionNames.stream().map(vk.stack()::UTF8).toArray(ByteBuffer[]::new));

            // Create
            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.malloc(vk.stack())
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pQueueCreateInfos(queueCreateInfosBuffer) // Also sets queueCreateInfoCount
                    .ppEnabledLayerNames(null)
                    .ppEnabledExtensionNames(requiredExtensionNamesBuffer) // Also sets enabledExtensionCount
                    .pEnabledFeatures(deviceFeatures);

            PointerBuffer logicalDevicePointer = vk.stack().mallocPointer(1);
            throwIfFailed(vkCreateDevice(physicalDevice, deviceCreateInfo, null, logicalDevicePointer));

            return new VkDevice(logicalDevicePointer.get(0), physicalDevice, deviceCreateInfo);
        }
    }

    public static VkQueue findFirstQueueByFamily(VkDevice logicalDevice, int familyIndex) {
        try (VulkanSession vk = new VulkanSession()) {
            PointerBuffer queuePointer = vk.stack().mallocPointer(1);
            vkGetDeviceQueue(logicalDevice, familyIndex, 0, queuePointer);
            return new VkQueue(queuePointer.get(0), logicalDevice);
        }
    }

    public static VkPresentModeKHR findBestPresentationMode(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        try (VulkanSession vk = new VulkanSession()) {
            List<VkPresentModeKHR> presentationModes = vk.getPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface);
            for (VkPresentModeKHR presentationMode : presentationModes) {
                if (presentationMode == VkPresentModeKHR.VK_PRESENT_MODE_MAILBOX_KHR) {
                    return presentationMode;
                }
            }
        }
        // This one is always available (required by vulkan specs)
        return VkPresentModeKHR.VK_PRESENT_MODE_FIFO_KHR;
    }

    public static SwapchainImageConfig findBestSwapchainImageConfig(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface, long window) {
        try (VulkanSession vk = new VulkanSession()) {
            VkSurfaceCapabilitiesKHR surfaceCapabilities = vk.getPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface);

            VkSurfaceFormatKHR.Buffer availableSurfaceFormats = vk.getPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface);

            VkFormat bestFormat = VkFormat.VK_FORMAT_R8G8B8A8_UNORM;
            VkColorSpaceKHR bestColorSpace = VkColorSpaceKHR.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
            boolean allFormatsSupported = availableSurfaceFormats.limit() == 1 && availableSurfaceFormats.get(0).format() == VK_FORMAT_UNDEFINED;
            if (!allFormatsSupported) {
                for (VkSurfaceFormatKHR availableSurfaceFormat : availableSurfaceFormats) {
                    if ((availableSurfaceFormat.format() == VkFormat.VK_FORMAT_R8G8B8A8_UNORM.getValue() ||
                         availableSurfaceFormat.format() == VkFormat.VK_FORMAT_B8G8R8A8_UNORM.getValue()) &&
                        availableSurfaceFormat.colorSpace() == VkColorSpaceKHR.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR.getValue()) {
                        bestFormat = VkFormat.of(availableSurfaceFormat.format());
                        bestColorSpace = VkColorSpaceKHR.of(availableSurfaceFormat.colorSpace());
                    }
                }
            }

            VkExtent2D bestResolution = surfaceCapabilities.currentExtent();
            if (surfaceCapabilities.currentExtent().width() == Integer.MAX_VALUE) {
                IntBuffer widthPointer = vk.stack().mallocInt(1);
                IntBuffer heightPointer = vk.stack().mallocInt(1);
                glfwGetFramebufferSize(window, widthPointer, heightPointer);
                VkExtent2D min = surfaceCapabilities.minImageExtent();
                VkExtent2D max = surfaceCapabilities.maxImageExtent();
                bestResolution = VkExtent2D.malloc(vk.stack())
                        .width(Math.clamp(widthPointer.get(0), min.width(), max.width()))
                        .height(Math.clamp(heightPointer.get(0), min.height(), max.height()));
            }

            //noinspection resource
            return new SwapchainImageConfig(
                    bestFormat,
                    bestColorSpace,
                    VkExtent2D.malloc().set(bestResolution)
            );
        }
    }

    public static VkSwapchainKHR createSwapchain(VkPhysicalDevice physicalDevice, VkDevice logicalDevice,
                                                  VkSurfaceKHR surface, SwapchainImageConfig swapchainImageConfig,
                                                  VkPresentModeKHR presentationMode) {
        try (VulkanSession vk = new VulkanSession()) {
            VkSurfaceCapabilitiesKHR surfaceCapabilities = vk.getPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface);

            int minImageCount = surfaceCapabilities.minImageCount() + 1; // Have an extra one for triple buffering
            if (surfaceCapabilities.maxImageCount() > 0) { // If the max image count is 0, then that means there is no limit
                minImageCount = Math.min(minImageCount, surfaceCapabilities.maxImageCount());
            }

            VkSharingMode sharingMode;
            IntBuffer queueIndexBuffer;
            QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);
            if (queueIndices.graphical() == queueIndices.surfaceSupporting()) {
                sharingMode = VkSharingMode.VK_SHARING_MODE_EXCLUSIVE;
                queueIndexBuffer = vk.stack().ints(queueIndices.graphical(), queueIndices.surfaceSupporting());
            } else {
                sharingMode = VkSharingMode.VK_SHARING_MODE_CONCURRENT;
                queueIndexBuffer = vk.stack().ints(queueIndices.graphical());
            }

            VkSwapchainCreateInfoKHR swapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc(vk.stack())
                    .sType$Default()
                    .surface(surface.address())
                    .minImageCount(minImageCount)
                    .imageFormat(swapchainImageConfig.format().getValue())
                    .imageColorSpace(swapchainImageConfig.colorSpace().getValue())
                    .imageExtent(swapchainImageConfig.extent())
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .imageSharingMode(sharingMode.getValue())
                    .queueFamilyIndexCount(queueIndexBuffer.limit()) // Make a PR to include this into the buffer
                    .pQueueFamilyIndices(queueIndexBuffer)
                    .preTransform(surfaceCapabilities.currentTransform())
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(presentationMode.getValue())
                    .clipped(true)
                    .oldSwapchain(NULL);
            LongBuffer swapchainPointer = vk.stack().mallocLong(1);
            throwIfFailed(vkCreateSwapchainKHR(logicalDevice, swapchainCreateInfo, null, swapchainPointer));
            return new VkSwapchainKHR(swapchainPointer.get(0));
        }
    }

    public static List<SwapchainImage> createSwapchainImageViews(VkDevice logicalDevice, VkSwapchainKHR swapchain, VkFormat format) {
        try (VulkanSession vk = new VulkanSession()) {
            List<VkImage> swapchainImages = vk.getSwapchainImagesKHR(logicalDevice, swapchain);
            return swapchainImages.stream()
                    .map(image -> new SwapchainImage(image, createImageView(logicalDevice, image, format, new VkImageAspectFlags(VK_IMAGE_ASPECT_COLOR_BIT))))
                    .collect(Collectors.toList());
        }
    }

    public static VkImageView createImageView(VkDevice logicalDevice, VkImage image, VkFormat format, VkImageAspectFlags aspectFlags) {
        try (VulkanSession vk = new VulkanSession()) {
            VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.malloc(vk.stack())
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .image(image.address())
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format.getValue())
                    .components(VkComponentMapping.malloc(vk.stack())
                            .r(VK_COMPONENT_SWIZZLE_R)
                            .g(VK_COMPONENT_SWIZZLE_G)
                            .b(VK_COMPONENT_SWIZZLE_B)
                            .a(VK_COMPONENT_SWIZZLE_A))
                    .subresourceRange(VkImageSubresourceRange.malloc(vk.stack())
                            .aspectMask(aspectFlags.value())
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(VK_REMAINING_ARRAY_LAYERS));
            LongBuffer imageViewPointer = vk.stack().mallocLong(1);
            throwIfFailed(vkCreateImageView(logicalDevice, imageViewCreateInfo, null, imageViewPointer));
            return new VkImageView(imageViewPointer.get(0));
        }
    }

    public static VkPipelineLayout createPipelineLayout(VkDevice logicalDevice, List<VkDescriptorSetLayout> descriptorSetLayouts) {
        try (VulkanSession vk = new VulkanSession()) {
            LongBuffer descriptorSetLayoutsBuffer = toAddressBuffer(descriptorSetLayouts, vk.stack(), VkDescriptorSetLayout::address);

            // This is how you would add a push constant to the pipeline layout
            //
            // VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1, vk.stack())
            //         .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
            //         .offset(0)
            //         .size(PushConstantStruct.SIZE);

            VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .pSetLayouts(descriptorSetLayoutsBuffer)
            //        .pPushConstantRanges(pushConstantRange)
                    .pPushConstantRanges(null);

            return vk.createPipelineLayout(logicalDevice, pipelineLayoutCreateInfo, null);
        }
    }

    public static VkRenderPass createRenderPass(VkDevice logicalDevice, VkFormat colorFormat, VkFormat depthBufferFormat) {
        try (VulkanSession vk = new VulkanSession()) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(2, vk.stack());
            // Color Attachment
            // noinspection resource
            attachments.get(0)
                    .format(colorFormat.getValue())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            // Depth Buffer Attachment
            // noinspection resource
            attachments.get(1)
                    .format(depthBufferFormat.getValue())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkAttachmentReference.Buffer colorAttachmentRefs = VkAttachmentReference.calloc(1, vk.stack())
                    // Color Attachment Reference
                    .attachment(0) // The index in the list that we pass to VkRenderPassCreateInfo.pAttachments
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            // Depth Attachment Reference
            VkAttachmentReference depthAttachmentRef = VkAttachmentReference.calloc(vk.stack())
                    .attachment(1) // The index in the list that we pass to VkRenderPassCreateInfo.pAttachments
                    .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpasses = VkSubpassDescription.calloc(1)
                    // Subpass 1
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(colorAttachmentRefs.limit())
                    .pColorAttachments(colorAttachmentRefs)
                    .pDepthStencilAttachment(depthAttachmentRef);

            // We need to determine when layout transitions occur using subpass dependencies
            VkSubpassDependency.Buffer dependencies = VkSubpassDependency.malloc(2, vk.stack());
            // Conversion from VK_IMAGE_LAYOUT_UNDEFINED to VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
            // Start converting after the external pipeline has completely finished and the reading has stopped there
            // End converting before we reach the color attachment output stage, before we read or write anything in that stage.
            dependencies.put(0, VkSubpassDependency.malloc(vk.stack())
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .srcStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .srcAccessMask(VK_ACCESS_MEMORY_READ_BIT)

                    .dstSubpass(0) // id of the subpass that we pass into the VkRenderPassCreateInfo.pSubpasses
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)

                    .dependencyFlags(0));
            dependencies.put(1, VkSubpassDependency.malloc(vk.stack())
                    .srcSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)

                    .dstSubpass(VK_SUBPASS_EXTERNAL) // id of the subpass that we pass into the VkRenderPassCreateInfo.pSubpasses
                    .dstStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .dstAccessMask(VK_ACCESS_MEMORY_READ_BIT)

                    .dependencyFlags(0));

            VkRenderPassCreateInfo renderPassCreateInfo = VkRenderPassCreateInfo.malloc(vk.stack())
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pAttachments(attachments)
                    .pSubpasses(subpasses)
                    .pDependencies(dependencies);

            return vk.createRenderPass(logicalDevice, renderPassCreateInfo, null);
        }
    }

    public static VkShaderModule createShaderModule(VkDevice logicalDevice, ByteBuffer code) {
        try (VulkanSession vk = new VulkanSession()) {
            VkShaderModuleCreateInfo shaderModuleCreateInfo = VkShaderModuleCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .pCode(code);

            return vk.createShaderModule(logicalDevice, shaderModuleCreateInfo, null);
        }
    }

    public static VkDescriptorSetLayout createDescriptorSetLayout(VkDevice logicalDevice) {
        try (VulkanSession vk = new VulkanSession()) {
            VkDescriptorSetLayoutBinding.Buffer descriptorSetLayoutBindings = VkDescriptorSetLayoutBinding.calloc(2, vk.stack());
            descriptorSetLayoutBindings.get(0)
                    .binding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                    .pImmutableSamplers(null);
            descriptorSetLayoutBindings.get(1)
                    .binding(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                    .pImmutableSamplers(null);

            VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .pBindings(descriptorSetLayoutBindings);

            return vk.createDescriptorSetLayout(logicalDevice, descriptorSetLayoutCreateInfo, null);
        }
    }

    public static VkDescriptorSetLayout createSamplerSetLayout(VkDevice logicalDevice) {
        try (VulkanSession vk = new VulkanSession()) {
            VkDescriptorSetLayoutBinding.Buffer descriptorSetLayoutBindings = VkDescriptorSetLayoutBinding.calloc(1, vk.stack());
            descriptorSetLayoutBindings.get(0)
                    .binding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .pImmutableSamplers(null);

            VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .pBindings(descriptorSetLayoutBindings);

            return vk.createDescriptorSetLayout(logicalDevice, descriptorSetLayoutCreateInfo, null);
        }
    }

    public static void connectDescriptorSetsToUniformBuffers(VkDevice logicalDevice, List<VkDescriptorSet> descriptorSets, List<VkBuffer> sceneUniformBuffers, long sceneUniformSize, List<VkBuffer> modelUniformBuffers, long modelUniformElementSize) {
        assert descriptorSets.size() == sceneUniformBuffers.size();
        assert descriptorSets.size() == modelUniformBuffers.size();
        try (VulkanSession vk = new VulkanSession()) {
            for (int i = 0; i < descriptorSets.size(); i++) {
                VkWriteDescriptorSet.Buffer setWrites = VkWriteDescriptorSet.calloc(2, vk.stack());

                VkDescriptorBufferInfo.Buffer sceneDescriptorBufferInfos = VkDescriptorBufferInfo.calloc(1, vk.stack())
                        .buffer(sceneUniformBuffers.get(i).address())
                        .offset(0)
                        .range(sceneUniformSize);
                setWrites.get(0)
                        .sType$Default()
                        .dstSet(descriptorSets.get(i).address())
                        .dstBinding(0)
                        .dstArrayElement(0)
                        .descriptorCount(1)
                        .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                        .pBufferInfo(sceneDescriptorBufferInfos);

                VkDescriptorBufferInfo.Buffer modelDescriptorBufferInfos = VkDescriptorBufferInfo.calloc(1, vk.stack())
                        .buffer(modelUniformBuffers.get(i).address())
                        .offset(0)
                        .range(modelUniformElementSize);
                setWrites.get(1)
                        .sType$Default()
                        .dstSet(descriptorSets.get(i).address())
                        .dstBinding(1)
                        .dstArrayElement(0)
                        .descriptorCount(1)
                        .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
                        .pBufferInfo(modelDescriptorBufferInfos);

                vk.updateDescriptorSets(logicalDevice, setWrites, null);
            }
        }
    }

    public static VkPipeline createGraphicsPipeline(VkDevice logicalDevice, VkExtent2D extent, VkPipelineLayout pipelineLayout, VkRenderPass renderPass) {
        try (VulkanSession vk = new VulkanSession()) {
            ByteBuffer vertexShader = readBinaryResource("shaders/vert.spv");
            ByteBuffer fragmentShader = readBinaryResource("shaders/frag.spv");

            VkShaderModule vertexShaderModule = createShaderModule(logicalDevice, vertexShader);
            VkShaderModule fragmentShaderModule = createShaderModule(logicalDevice, fragmentShader);


            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2);
            ByteBuffer main = vk.stack().UTF8("main");
            // Vertex shader stage
            //noinspection resource
            shaderStages.get(0)
                    .sType$Default()
                    .stage(VK_SHADER_STAGE_VERTEX_BIT)
                    .module(vertexShaderModule.address())
                    .pName(main);
            // Fragment shader stage
            //noinspection resource
            shaderStages.get(1)
                    .sType$Default()
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(fragmentShaderModule.address())
                    .pName(main);

            ////// VERTEX LAYOUT & SHADER LOCATIONS //////
            VkVertexInputBindingDescription.Buffer vertexInputBindingDescriptions = VkVertexInputBindingDescription.calloc(1, vk.stack())
                    .binding(0)
                    .stride(Vertex.BYTES)
                    .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

            VkVertexInputAttributeDescription.Buffer vertexInputAttributeDescriptions = VkVertexInputAttributeDescription.calloc(4, vk.stack());
            vertexInputAttributeDescriptions.get(0)
                    .location(0)
                    .binding(0)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(Vertex.POSITION_OFFSET_BYTES);
            vertexInputAttributeDescriptions.get(1)
                    .location(1)
                    .binding(0)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(Vertex.NORMAL_OFFSET_BYTES);
            vertexInputAttributeDescriptions.get(2)
                    .location(2)
                    .binding(0)
                    .format(VK_FORMAT_R32G32_SFLOAT)
                    .offset(Vertex.UV_OFFSET_BYTES);
            vertexInputAttributeDescriptions.get(3)
                    .location(3)
                    .binding(0)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(Vertex.COLOR_OFFSET_BYTES);

            VkPipelineVertexInputStateCreateInfo vertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .pVertexBindingDescriptions(vertexInputBindingDescriptions)
                    .pVertexAttributeDescriptions(vertexInputAttributeDescriptions);
            ///////////////////////

            VkPipelineInputAssemblyStateCreateInfo inputAssemblyStateCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                    .primitiveRestartEnable(false);

            VkViewport.Buffer viewports = VkViewport.calloc(1);
            //noinspection resource
            viewports.get(0)
                    .x(0)
                    .y(0)
                    .width(extent.width())
                    .height(extent.height())
                    .minDepth(0)
                    .maxDepth(1);

            VkRect2D.Buffer scissors = VkRect2D.calloc(1);
            scissors.get(0)
                    .offset(VkOffset2D.malloc(vk.stack()).set(0, 0))
                    .extent(extent);

            VkPipelineViewportStateCreateInfo viewportStateCreateInfo = VkPipelineViewportStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .viewportCount(viewports.limit())
                    .pViewports(viewports)
                    .scissorCount(scissors.limit())
                    .pScissors(scissors);

            VkPipelineRasterizationStateCreateInfo rasterizationStateCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .depthClampEnable(false)
                    .rasterizerDiscardEnable(false)
                    .polygonMode(VK_POLYGON_MODE_FILL)
                    .cullMode(VK_CULL_MODE_NONE) // TODO: Change back to VK_CULL_MODE_BACK_BIT
                    .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                    .depthBiasEnable(false)
                    .lineWidth(1);

            VkPipelineMultisampleStateCreateInfo multisampleStateCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
                    .sampleShadingEnable(false)
                    .minSampleShading(0)
                    .pSampleMask(null)
                    .alphaToCoverageEnable(false)
                    .alphaToOneEnable(false);

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachmentState = VkPipelineColorBlendAttachmentState.calloc(1)
                    .blendEnable(true)
                    .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                    .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                    .colorBlendOp(VK_BLEND_OP_ADD)
                    .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                    .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
                    .alphaBlendOp(VK_BLEND_OP_ADD)
                    .colorWriteMask(VK_COLOR_COMPONENT_R_BIT |
                                    VK_COLOR_COMPONENT_G_BIT |
                                    VK_COLOR_COMPONENT_B_BIT |
                                    VK_COLOR_COMPONENT_A_BIT);

            VkPipelineColorBlendStateCreateInfo colorBlendStateCreateInfo = VkPipelineColorBlendStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .logicOpEnable(false)
                    .attachmentCount(1)
                    .pAttachments(colorBlendAttachmentState);

            VkPipelineDepthStencilStateCreateInfo depthStencilStateCreateInfo = VkPipelineDepthStencilStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .depthTestEnable(true)
                    .depthWriteEnable(true)
                    .depthCompareOp(VK_COMPARE_OP_LESS)
                    .depthBoundsTestEnable(false)
                    .stencilTestEnable(false);

            VkGraphicsPipelineCreateInfo.Buffer graphicsPipelineCreateInfos = VkGraphicsPipelineCreateInfo.calloc(1);
            //noinspection resource
            graphicsPipelineCreateInfos.get(0)
                    .sType$Default()
                    .stageCount(2)
                    .pStages(shaderStages)
                    .pVertexInputState(vertexInputStateCreateInfo)
                    .pInputAssemblyState(inputAssemblyStateCreateInfo)
                    .pViewportState(viewportStateCreateInfo)
                    .pRasterizationState(rasterizationStateCreateInfo)
                    .pMultisampleState(multisampleStateCreateInfo)
                    .pDepthStencilState(depthStencilStateCreateInfo)
                    .pColorBlendState(colorBlendStateCreateInfo)
                    .pDynamicState(null)
                    .layout(pipelineLayout.address())
                    .renderPass(renderPass.address())
                    .subpass(0)
                    .basePipelineHandle(VK_NULL_HANDLE)
                    .basePipelineIndex(-1);

            List<VkPipeline> pipelines = vk.createGraphicsPipelines(logicalDevice, null, graphicsPipelineCreateInfos, null);

            vk.destroyShaderModule(logicalDevice, fragmentShaderModule, null);
            vk.destroyShaderModule(logicalDevice, vertexShaderModule, null);

            return pipelines.getFirst();
        }
    }

    public static ByteBuffer readBinaryResource(String resourcePath) {
        try (InputStream inputStream = VulkanUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Could not read file");
            }
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length).put(bytes);
            buffer.flip();
            buffer.rewind();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<VkFramebuffer> createFramebuffers(VkDevice logicalDevice, VkRenderPass renderPass, SwapchainImageConfig imageConfig, List<SwapchainImage> swapchainImages, Image depthBufferImage) {
        try (VulkanSession vk = new VulkanSession()) {
            List<VkFramebuffer> framebuffers = new ArrayList<>(swapchainImages.size());
            for (int i = 0; i < swapchainImages.size(); i++) {
                LongBuffer attachments = vk.stack().longs(swapchainImages.get(i).view().address(), depthBufferImage.view().address());
                VkFramebufferCreateInfo framebufferCreateInfo = VkFramebufferCreateInfo.calloc(vk.stack())
                        .sType$Default()
                        .renderPass(renderPass.address())
                        .pAttachments(attachments)
                        .width(imageConfig.extent().width())
                        .height(imageConfig.extent().height())
                        .layers(1);

                LongBuffer framebufferPointer = vk.stack().mallocLong(1);
                throwIfFailed(vkCreateFramebuffer(logicalDevice, framebufferCreateInfo, null, framebufferPointer));

                framebuffers.add(i, new VkFramebuffer(framebufferPointer.get(0)));
            }

            return framebuffers;
        }
    }

    public static VkCommandPool createCommandPool(VkDevice logicalDevice, int queueFamilyIndex) {
        try (VulkanSession vk = new VulkanSession()) {
            VkCommandPoolCreateInfo commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    .queueFamilyIndex(queueFamilyIndex);
            LongBuffer commandPoolPointer = vk.stack().mallocLong(1);
            throwIfFailed(vkCreateCommandPool(logicalDevice, commandPoolCreateInfo, null, commandPoolPointer));
            return new VkCommandPool(commandPoolPointer.get(0));
        }
    }

    public static List<VkCommandBuffer> createCommandBuffers(VkDevice logicalDevice, VkCommandPool commandPool, List<VkFramebuffer> framebuffers) {
        try (VulkanSession vk = new VulkanSession()) {
            VkCommandBufferAllocateInfo commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(vk.stack())
                    .sType$Default()
                    .commandPool(commandPool.address())
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY) // the secondary level is used to indicate that this buffer can only be run from another buffer (basically you can record a command to run a buffer of commands).
                    .commandBufferCount(framebuffers.size());
            return vk.allocateCommandBuffers(logicalDevice, commandBufferAllocateInfo);
        }
    }
}
