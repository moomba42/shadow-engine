package com.alexdl.shadowhaven.engine.vulkan;

import com.alexdl.shadowhaven.engine.Disposable;
import com.alexdl.shadowhaven.engine.GlfwWindow;
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
    private final GlfwWindow window;
    private final VkInstance instance;
    private final VkSurface surface;
    private final VkPhysicalDevice physicalDevice;
    private final VkDevice logicalDevice;
    private final VkQueue graphicsQueue;
    private final VkQueue surfaceSupportingQueue;
    private final Long debugMessengerPointer;

    public VulkanRenderer(GlfwWindow window, boolean enableDebugging) {
        this.window = window;
        instance = createInstance("Vulkan Test App", enableDebugging);
        surface = createSurface(instance, window);
        debugMessengerPointer = enableDebugging ? VulkanUtils.createDebugMessenger(instance) : null;

        physicalDevice = findFirstSuitablePhysicalDevice(instance, surface);
        if (physicalDevice == null) {
            throw new RuntimeException("Could not find a suitable physical device");
        }

        logicalDevice = createLogicalDevice(physicalDevice, surface);
        QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);
        graphicsQueue = findFirstQueueByFamily(logicalDevice, queueIndices.graphical());
        surfaceSupportingQueue = findFirstQueueByFamily(logicalDevice, queueIndices.surfaceSupporting());
    }
    
    @Override
    public void dispose() {
        vkDestroySurfaceKHR(instance, surface.address(), null);
        vkDestroyDevice(logicalDevice, null);
        if(debugMessengerPointer != null) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessengerPointer, null);
        }
        vkDestroyInstance(instance, null);
    }
}
