package com.alexdl.sdng;

import com.alexdl.sdng.backend.glfw.GlfwWindow;
import com.alexdl.sdng.backend.vulkan.VulkanRenderer;

import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;

public class VulkanTest {
    public static void main(String[] args) {
        boolean enableDebugging = args != null && args.length >= 1 && Objects.equals(args[0], "debug");

        glfwInit();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        GlfwWindow window = new GlfwWindow(glfwCreateWindow(800, 600, "Vulkan Test", 0, 0));

        VulkanRenderer renderer = new VulkanRenderer(window, new Configuration(enableDebugging));

        while(!glfwWindowShouldClose(window.address())) {
            glfwPollEvents();
        }

        renderer.dispose();
        glfwDestroyWindow(window.address());
        glfwTerminate();
    }
}
