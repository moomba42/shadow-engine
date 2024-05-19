package org.lwjgl.glfw;

import com.alexdl.sdng.backend.Disposable;

import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

public record GlfwWindow(long address) implements Disposable {
    @Override
    public void dispose() {
        glfwDestroyWindow(address());
        glfwTerminate();
    }
}
