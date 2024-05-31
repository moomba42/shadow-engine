package com.alexdl.sdng;

import com.alexdl.sdng.backend.ResourceAssetLoader;
import com.alexdl.sdng.backend.vulkan.VulkanRenderer;
import com.alexdl.sdng.logging.Logger;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import org.lwjgl.glfw.GlfwWindow;

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
    static ResourceFileLoader provideResourceFileLoader(FileCache fileCache) {
        return new ResourceFileLoader(fileCache);
    }

    @Provides
    static LRUFileCache provideLRUFileCache() {
        return new LRUFileCache(10);
    }

    @Provides
    static ResourceAssetLoader provideResourceAssetLoader(VulkanRenderer vulkanRenderer, ResourceFileLoader resourceFileLoader, Disposables disposables) {
        return new ResourceAssetLoader(vulkanRenderer, resourceFileLoader, disposables, new Logger(ResourceAssetLoader.class));
    }

    @Provides
    @Singleton
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

    @Binds
    abstract FileLoader bindFileLoader(ResourceFileLoader resourceFileLoader);

    @Binds
    abstract AssetLoader bindAssetLoader(ResourceAssetLoader resourceAssetLoader);

    @Binds
    abstract FileCache bindFileCache(LRUFileCache lruFileCache);
}
