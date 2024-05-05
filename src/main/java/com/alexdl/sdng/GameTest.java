package com.alexdl.sdng;

import com.alexdl.sdng.backend.vulkan.VulkanRenderer;
import org.joml.Matrix4f;

import java.util.Objects;

import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static org.lwjgl.glfw.GLFW.*;

public class GameTest implements Game{

    private final Renderer renderer;

    private double timer = 0;

    public GameTest(Renderer renderer) {
        this.renderer = renderer;
    }

    public static void main(String[] args) {
        boolean enableDebugging = args != null && args.length >= 1 && Objects.equals(args[0], "debug");

        glfwInit();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        long window = glfwCreateWindow(800, 600, "Vulkan Test", 0, 0);
        VulkanRenderer renderer = new VulkanRenderer(window, new Configuration(enableDebugging));
        Game game = new GameTest(renderer);
        game.init();

        double lastTime = 0.0;
        while(!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            double now = glfwGetTime();
            double deltaTime = now - lastTime;
            lastTime = now;
            game.update(deltaTime);
            game.render();
        }

        game.dispose();
        renderer.dispose();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    @Override
    public void init() {

    }

    @Override
    public void update(double delta) {
        timer += delta;
        renderer.updateModel(0, new Matrix4f().identity()
                .translate(0.0f, 0.0f, -2.0f)
                .rotate((float) tan(sin(sin(timer * 3))), 1, 0, 0));
        renderer.updateModel(1, new Matrix4f().identity()
                .translate(0.0f, 0.0f, -2.0f)
                .rotate((float) tan(sin(timer)) * 2, 0, 0, 1));
        renderer.updatePushConstant(new Matrix4f().identity().rotate((float) timer / 2, 0, 1, 0));
    }

    @Override
    public void render() {
        renderer.draw();
    }

    @Override
    public void dispose() {

    }
}
