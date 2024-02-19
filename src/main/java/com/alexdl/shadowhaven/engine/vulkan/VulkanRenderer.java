package com.alexdl.shadowhaven.engine.vulkan;

import com.alexdl.shadowhaven.engine.Disposable;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;

import static com.alexdl.shadowhaven.engine.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;

public class VulkanRenderer implements Disposable {
    private final long glfwWindowPointer;
    private final VkInstance instance;
    private final VkPhysicalDevice physicalDevice;
    private final int graphicsQueueFamilyIndex;
    private final VkDevice logicalDevice;
    private final VkQueue graphicsQueue;

    public VulkanRenderer(long glfwWindowPointer) {
        this.glfwWindowPointer = glfwWindowPointer;
        instance = createInstance("Vulkan Test App");
        physicalDevice = findFirstSuitablePhysicalDevice(instance);
        graphicsQueueFamilyIndex = findGraphicsQueueFamilyLocation(physicalDevice);
        logicalDevice = createLogicalDevice(physicalDevice);
        graphicsQueue = findFirstQueueByFamily(logicalDevice, graphicsQueueFamilyIndex);
        if (physicalDevice == null) {
            throw new RuntimeException("Could not find a suitable physical device");
        }
    }


    @Override
    public void dispose() {
        vkDestroyInstance(instance, null);
    }
}
