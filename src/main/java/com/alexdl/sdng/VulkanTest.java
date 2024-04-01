package com.alexdl.sdng;

import com.alexdl.sdng.backend.glfw.GlfwWindow;
import com.alexdl.sdng.backend.vulkan.VulkanRenderer;
import org.joml.Matrix4f;

import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;

public class VulkanTest {
    public static void main(String[] args) {
        boolean enableDebugging = args != null && args.length >= 1 && Objects.equals(args[0], "debug");

        glfwInit();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        GlfwWindow window = new GlfwWindow(glfwCreateWindow(800, 600, "Vulkan Test", 0, 0));

        VulkanRenderer renderer = new VulkanRenderer(window, new Configuration(enableDebugging));

        double angle = 0.0;
        double deltaTime = 0.0;
        double lastTime = 0.0;

        while(!glfwWindowShouldClose(window.address())) {
            glfwPollEvents();

            double now = glfwGetTime();
            deltaTime = now - lastTime;
            lastTime = now;
            angle += 100.0 * deltaTime;
            while(angle > 360.0) {
                angle -= 360.0;
            }

            renderer.updateModel(0, new Matrix4f().identity().translate(-2.0f, 0.0f, -5.0f).rotate((float) Math.toRadians(angle), 0, 1, 0));
            renderer.updateModel(1, new Matrix4f().identity().translate(2.0f, 0.0f, -5.0f).rotate((float) Math.toRadians(-angle), 1, 0, 0));

            renderer.draw();
        }

        renderer.dispose();
        glfwDestroyWindow(window.address());
        glfwTerminate();
    }
}
