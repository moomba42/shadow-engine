package com.alexdl.sdng.examples;

import com.alexdl.sdng.AssetLoader;
import com.alexdl.sdng.Game;
import com.alexdl.sdng.Renderer;
import com.alexdl.sdng.Runner;
import com.alexdl.sdng.ShadowEngineModule;
import com.alexdl.sdng.backend.vulkan.Model;
import com.alexdl.sdng.backend.vulkan.ResourceHandle;
import dagger.Binds;
import dagger.Component;
import dagger.Module;
import org.joml.Matrix4f;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.lang.Math.sin;
import static java.lang.Math.tan;

public class GameTest implements Game {
    @Module
    interface GameTestModule {
        @Binds
        Game bindsGameTest(GameTest gameTest);
    }

    @Singleton
    @Component(modules = {ShadowEngineModule.class, GameTestModule.class})
    interface GameComponent {
        Runner runner();
    }

    public static void main(String[] args) {
        DaggerGameTest_GameComponent.create().runner().run();
    }

    private final Renderer renderer;
    private final AssetLoader loader;

    private double timer = 0;
    private Model teapot;
    private Model cube;

    @Inject
    public GameTest(Renderer renderer, AssetLoader assetLoader) {
        this.renderer = renderer;
        this.loader = assetLoader;
    }

    @Override
    public void init() {
        teapot = loader.loadModel(new ResourceHandle("teapot.obj"));
        cube = loader.loadModel(new ResourceHandle("cube.obj"));
    }

    @Override
    public void update(double delta) {
        timer += delta;
        teapot.transform().set(new Matrix4f().identity()
                .translate(-3.0f, 0.0f, -2.0f)
                .rotate((float) tan(sin(sin((timer+0.5) * 3))), 1, 0, 0))
                .translate(0, -1.5f, 0);
        cube.transform().set(new Matrix4f().identity()
                .translate(3.0f, 0.0f, -2.0f)
                .rotate((float) tan(sin(sin(timer * 3))), 1, 0, 0));
        renderer.updatePushConstant(new Matrix4f().identity().rotate((float) timer / 2, 0, 1, 0));
    }

    @Override
    public void render() {
        renderer.queueModel(teapot);
        renderer.queueModel(cube);
        renderer.draw();
    }

    @Override
    public void dispose() {

    }
}
