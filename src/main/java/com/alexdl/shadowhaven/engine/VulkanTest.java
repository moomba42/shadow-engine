package com.alexdl.shadowhaven.engine;

import com.alexdl.shadowhaven.engine.vulkan.VulkanRenderer;

import static org.lwjgl.glfw.GLFW.*;

public class VulkanTest {
    public static void main(String[] args) {
        glfwInit();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        long window = glfwCreateWindow(800, 600, "Vulkan Test", 0, 0);

        VulkanRenderer renderer = new VulkanRenderer(window);

        while(!glfwWindowShouldClose(window)) {
            glfwPollEvents();
        }

        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
