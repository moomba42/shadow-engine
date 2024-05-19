package com.alexdl.sdng.examples;

import com.alexdl.sdng.Game;
import com.alexdl.sdng.Renderer;
import com.alexdl.sdng.Runner;
import com.alexdl.sdng.ShadowEngineModule;
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

    private double timer = 0;

    @Inject
    public GameTest(Renderer renderer) {
        this.renderer = renderer;
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
