package com.alexdl.sdng.backend.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.enums.VkPresentModeKHR;

import javax.annotation.Nonnull;
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

    public MemoryStack stack() {
        return stack;
    }

    public PointerBuffer enumeratePhysicalDevices(VkInstance instance) {
        IntBuffer physicalDeviceCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumeratePhysicalDevices(instance, physicalDeviceCountPointer, null));

        PointerBuffer physicalDevices = stack.mallocPointer(physicalDeviceCountPointer.get(0));
        throwIfFailed(vkEnumeratePhysicalDevices(instance, physicalDeviceCountPointer, physicalDevices));

        return physicalDevices;
    }

    public VkExtensionProperties.Buffer enumerateDeviceExtensionProperties(VkPhysicalDevice physicalDevice) {
        IntBuffer deviceExtensionCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateDeviceExtensionProperties(physicalDevice, NULL_STRING, deviceExtensionCountPointer, null));

        VkExtensionProperties.Buffer deviceExtensions = VkExtensionProperties.malloc(deviceExtensionCountPointer.get(0), stack);
        throwIfFailed(vkEnumerateDeviceExtensionProperties(physicalDevice, NULL_STRING, deviceExtensionCountPointer, deviceExtensions));

        return deviceExtensions;
    }

    public VkExtensionProperties.Buffer enumerateInstanceExtensionProperties() {
        IntBuffer extensionCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateInstanceExtensionProperties(NULL_STRING, extensionCountPointer, null));

        VkExtensionProperties.Buffer extensions = VkExtensionProperties.malloc(extensionCountPointer.get(0), stack);
        throwIfFailed(vkEnumerateInstanceExtensionProperties(NULL_STRING, extensionCountPointer, extensions));
        return extensions;
    }

    public VkLayerProperties.Buffer enumerateInstanceLayerProperties() {
        IntBuffer layerCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateInstanceLayerProperties(layerCountPointer, null));

        VkLayerProperties.Buffer layers = VkLayerProperties.malloc(layerCountPointer.get(0), stack);
        throwIfFailed(vkEnumerateInstanceLayerProperties(layerCountPointer, layers));
        return layers;
    }

    public VkQueueFamilyProperties.Buffer getPhysicalDeviceQueueFamilyProperties(VkPhysicalDevice physicalDevice) {
        IntBuffer queueFamilyCountPointer = stack.mallocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCountPointer, null);

        VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCountPointer.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCountPointer, queueFamilies);
        return queueFamilies;
    }

    public List<VkPresentModeKHR> getPhysicalDeviceSurfacePresentModesKHR(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
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

    public VkSurfaceFormatKHR.Buffer getPhysicalDeviceSurfaceFormatsKHR(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        IntBuffer surfaceFormatsCount = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.address(), surfaceFormatsCount, null);
        VkSurfaceFormatKHR.Buffer surfaceFormatBuffer = VkSurfaceFormatKHR.malloc(surfaceFormatsCount.get(0), stack);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.address(), surfaceFormatsCount, surfaceFormatBuffer);
        return surfaceFormatBuffer;
    }

    public VkSurfaceCapabilitiesKHR getPhysicalDeviceSurfaceCapabilitiesKHR(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        VkSurfaceCapabilitiesKHR surfaceCapabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface.address(), surfaceCapabilities);
        return surfaceCapabilities;
    }

    public List<VkImage> getSwapchainImagesKHR(VkDevice logicalDevice, VkSwapchainKHR swapchain) {
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
}
