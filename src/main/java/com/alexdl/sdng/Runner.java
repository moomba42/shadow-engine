package com.alexdl.sdng;

import org.lwjgl.glfw.GlfwWindow;

import javax.inject.Inject;

import static org.lwjgl.glfw.GLFW.*;

public class Runner {
    private final Game game;
    private final GlfwWindow window;

    @Inject
    public Runner(Game game, GlfwWindow window) {
        this.game = game;
        this.window = window;
    }

    public void run() {
        game.init();
        double lastTime = 0.0;
        while(!glfwWindowShouldClose(window.address())) {
            glfwPollEvents();
            double now = glfwGetTime();
            double deltaTime = now - lastTime;
            lastTime = now;
            game.update(deltaTime);
            game.render();
        }
        glfwDestroyWindow(window.address());
        glfwTerminate();
    }
}
