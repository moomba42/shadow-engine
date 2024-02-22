package com.alexdl.shadowhaven.engine.vulkan;

import com.alexdl.shadowhaven.engine.Disposable;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;

import static com.alexdl.shadowhaven.engine.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;

public class VulkanRenderer implements Disposable {
    private final long glfwWindowPointer;
    private final VkInstance instance;
    private final long surface;
    private final VkPhysicalDevice physicalDevice;
    private final int graphicsQueueFamilyIndex;
    private final int surfaceSupportingQueueFamilyIndex;
    private final VkDevice logicalDevice;
    private final VkQueue graphicsQueue;
    private final VkQueue surfaceSupportingQueue;
    private final Long debugMessengerPointer;

    public VulkanRenderer(long glfwWindowPointer, boolean enableDebugging) {
        this.glfwWindowPointer = glfwWindowPointer;
        instance = createInstance("Vulkan Test App", enableDebugging);
        surface = createSurface(instance, glfwWindowPointer);
        debugMessengerPointer = enableDebugging ? VulkanUtils.createDebugMessenger(instance) : null;

        physicalDevice = findFirstSuitablePhysicalDevice(instance);
        if (physicalDevice == null) {
            throw new RuntimeException("Could not find a suitable physical device");
        }

        graphicsQueueFamilyIndex = findGraphicsQueueFamilyLocation(physicalDevice);
        if(graphicsQueueFamilyIndex < 0) {
            throw new RuntimeException("Could not find a graphics queue");
        }
        surfaceSupportingQueueFamilyIndex = findSurfaceSupportingQueueFamilyIndex(physicalDevice, surface);
        if(surfaceSupportingQueueFamilyIndex < 0) {
            throw new RuntimeException("Could not find a queue supporting the given surface");
        }

        logicalDevice = createLogicalDevice(physicalDevice, graphicsQueueFamilyIndex, surfaceSupportingQueueFamilyIndex);
        graphicsQueue = findFirstQueueByFamily(logicalDevice, graphicsQueueFamilyIndex);
        surfaceSupportingQueue = findFirstQueueByFamily(logicalDevice, surfaceSupportingQueueFamilyIndex);
    }
    
    @Override
    public void dispose() {
        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyDevice(logicalDevice, null);
        if(debugMessengerPointer != null) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessengerPointer, null);
        }
        vkDestroyInstance(instance, null);
    }
}
