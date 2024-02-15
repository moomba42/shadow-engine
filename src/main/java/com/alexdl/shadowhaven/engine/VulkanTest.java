package com.alexdl.shadowhaven.engine;

import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceExtensionProperties;

public class VulkanTest {
    public static void main(String[] args) {
        glfwInit();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        long window = glfwCreateWindow(800, 600, "Vulkan Test", 0, 0);


        try(MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer extensionCount = memoryStack.mallocInt(1);
            vkEnumerateInstanceExtensionProperties((String) null, extensionCount, null);
            System.out.printf("Extension count: %d", extensionCount.get(0));
        }

        while(!glfwWindowShouldClose(window)) {
            glfwPollEvents();
        }
    }
}
