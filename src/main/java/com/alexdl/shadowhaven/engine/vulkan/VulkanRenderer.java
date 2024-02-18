package com.alexdl.shadowhaven.engine.vulkan;

import com.alexdl.shadowhaven.engine.Disposable;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;

import static com.alexdl.shadowhaven.engine.vulkan.VulkanUtils.createInstance;
import static com.alexdl.shadowhaven.engine.vulkan.VulkanUtils.findFirstSuitablePhysicalDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;

public class VulkanRenderer implements Disposable {
    private final long glfwWindowPointer;
    private final VkInstance instance;
    private final VkPhysicalDevice physicalDevice;

    public VulkanRenderer(long glfwWindowPointer) {
        this.glfwWindowPointer = glfwWindowPointer;
        instance = createInstance("Vulkan Test App");
        physicalDevice = findFirstSuitablePhysicalDevice(instance);
        if (physicalDevice == null) {
            throw new RuntimeException("Could not find a suitable physical device");
        }
    }


    @Override
    public void dispose() {
        vkDestroyInstance(instance, null);
    }
}
