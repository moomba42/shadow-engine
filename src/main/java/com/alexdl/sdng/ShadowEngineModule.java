package com.alexdl.sdng;

import com.alexdl.sdng.backend.SampleDataAssetLoader;
import com.alexdl.sdng.backend.vulkan.VulkanRenderer;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import org.lwjgl.glfw.GlfwWindow;

import javax.inject.Named;
import javax.inject.Singleton;

import static org.lwjgl.glfw.GLFW.*;

@Module
public abstract class ShadowEngineModule {
    @Provides
    @Singleton
    static GlfwWindow provideGlfwWindow() {
        glfwInit();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        long address = glfwCreateWindow(800, 600, "Vulkan Test", 0, 0);
        return new GlfwWindow(address);
    }

    @Provides
    static Disposables provideDisposables() {
        return new Disposables();
    }

    @Provides
    static AssetLoader provideAssetLoader() {
        return new SampleDataAssetLoader();
    }

    @Provides
    static VulkanRenderer provideVulkanRenderer(GlfwWindow window, Configuration configuration, Disposables disposables) {
        var renderer = new VulkanRenderer(window, configuration);
        disposables.add(renderer);
        return renderer;
    }

    @Provides
    static Configuration provideConfiguration() {
        return new Configuration(true);
    }


    @Provides
    static Runner provideRunner(Game game, GlfwWindow window, Disposables disposables) {
        return new Runner(game, window, disposables);
    }

    @Binds
    abstract Renderer bindRenderer(VulkanRenderer vulkanRenderer);
}
