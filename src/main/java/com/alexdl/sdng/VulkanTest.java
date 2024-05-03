package com.alexdl.sdng;

import com.alexdl.sdng.backend.vulkan.VulkanRenderer;
import org.joml.Matrix4f;

import java.util.Objects;

import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static org.lwjgl.glfw.GLFW.*;

public class VulkanTest {
    public static void main(String[] args) {
        boolean enableDebugging = args != null && args.length >= 1 && Objects.equals(args[0], "debug");

        glfwInit();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        long window = glfwCreateWindow(800, 600, "Vulkan Test", 0, 0);

        VulkanRenderer renderer = new VulkanRenderer(window, new Configuration(enableDebugging));

        while(!glfwWindowShouldClose(window)) {
            glfwPollEvents();

            double now = glfwGetTime();

            renderer.updateModel(0, new Matrix4f().identity()
                    .translate(0.0f, 0.0f, -2.0f)
                    .rotate((float) tan(sin(sin(now * 3))), 1, 0, 0));
            renderer.updateModel(1, new Matrix4f().identity()
                    .translate(0.0f, 0.0f, -2.0f)
                    .rotate((float) tan(sin(now)) * 2, 0, 0, 1));
            renderer.updatePushConstant(new Matrix4f().identity().rotate((float) now / 2, 0, 1, 0));

            renderer.draw();
        }

        renderer.dispose();
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
