package com.alexdl.sdng.examples;

import com.alexdl.sdng.AssetLoader;
import com.alexdl.sdng.FileHandle;
import com.alexdl.sdng.Game;
import com.alexdl.sdng.rendering.Renderer;
import com.alexdl.sdng.Runner;
import com.alexdl.sdng.ShadowEngineModule;
import com.alexdl.sdng.rendering.Light;
import com.alexdl.sdng.rendering.Model;
import dagger.Binds;
import dagger.Component;
import dagger.Module;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

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
    private Model teapot;
    private Model cube;
    private List<Light> lights;

    @Inject
    public GameTest(Renderer renderer, AssetLoader assetLoader) {
        this.renderer = renderer;
        this.loader = assetLoader;
    }

    @Override
    public void init() {
        teapot = loader.loadModel(new FileHandle("teapot.obj"));
        cube = loader.loadModel(new FileHandle("cube.obj"));
        lights = List.of(
                new Light(new Vector3f(-3, 3, 4),
                        new Vector3f(1, 0, 0),
                        6, 0, 0.1f),
                new Light(new Vector3f(3, -3, 0),
                        new Vector3f(0, 1, 0),
                        6, 0, 0.1f));
    }

    @Override
    public void update(double delta) {
        timer += delta;
        teapot.transform().set(new Matrix4f().identity()
                        .translate(-3.0f, -1.5f, -2.0f));
        cube.transform().set(new Matrix4f().identity()
                .translate(3.0f, 0.0f, -2.0f)
                .rotate((float) timer / 4f, 0, 1, 0)
                .rotate((float) tan(sin(sin(timer * 3))) * 0.1f, 1, 0, 0));
        lights.get(0).getPosition().set(0, sin(timer * 2) * 4, 0);
        lights.get(1).getPosition().set(sin(timer / 2) * 5, 0, cos(timer / 2) * 5);
    }

    @Override
    public void render() {
        renderer.updateLights(lights);
        renderer.queueModel(teapot);
        renderer.queueModel(cube);
        renderer.draw();
    }

    @Override
    public void dispose() {

    }
}
