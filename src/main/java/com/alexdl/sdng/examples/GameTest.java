package com.alexdl.sdng.examples;

import com.alexdl.sdng.AssetLoader;
import com.alexdl.sdng.Game;
import com.alexdl.sdng.Renderer;
import com.alexdl.sdng.Runner;
import com.alexdl.sdng.ShadowEngineModule;
import com.alexdl.sdng.backend.vulkan.Mesh;
import com.alexdl.sdng.backend.vulkan.ResourceHandle;
import dagger.Binds;
import dagger.Component;
import dagger.Module;
import org.joml.Matrix4f;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.lang.Math.*;

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
    private Mesh cubeMesh;
    private Mesh planeMesh;
    private Mesh planeMesh2;
    private Mesh planeMesh3;

    @Inject
    public GameTest(Renderer renderer, AssetLoader assetLoader) {
        this.renderer = renderer;
        this.loader = assetLoader;
    }

    @Override
    public void init() {
        planeMesh = loader.loadMesh(new ResourceHandle("plane1.obj"));
        planeMesh2 = loader.loadMesh(new ResourceHandle("plane2.obj"));
        planeMesh3 = loader.loadMesh(new ResourceHandle("plane3.obj"));
    }

    @Override
    public void update(double delta) {
        timer += delta;
        planeMesh.meshData().getTransform().set(new Matrix4f().identity()
                .translate(0.0f, 0.0f, -2.0f)
                .rotate((float) tan(sin(timer)) * 2, 0, 0, 1));
        planeMesh2.meshData().getTransform().set(new Matrix4f().identity()
                .translate(0.0f, 0.0f, -2.0f)
                .rotate((float) tan(sin(sin(timer * 3))), 1, 0, 0));
        planeMesh3.meshData().getTransform().set(new Matrix4f().identity()
                .translate(-1.0f, 0.0f, -2.0f)
                .rotate((float) tan(cos(timer * 3)), 1, 0, 0));
        renderer.updatePushConstant(new Matrix4f().identity().rotate((float) timer / 2, 0, 1, 0));
    }

    @Override
    public void render() {
        renderer.queueMesh(planeMesh);
        renderer.queueMesh(planeMesh2);
        renderer.queueMesh(planeMesh3);
        renderer.draw();
    }

    @Override
    public void dispose() {
    }
}
