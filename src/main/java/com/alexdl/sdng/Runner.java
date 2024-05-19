package com.alexdl.sdng;

import org.lwjgl.glfw.GlfwWindow;

import javax.inject.Inject;

import static org.lwjgl.glfw.GLFW.*;

public class Runner {
    private final Game game;
    private final GlfwWindow window;
    private final Disposables disposables;

    @Inject
    public Runner(Game game, GlfwWindow window, Disposables disposables) {
        this.game = game;
        this.window = window;
        this.disposables = disposables;
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
        game.dispose();
        disposables.dispose();
        window.dispose();
    }
}
