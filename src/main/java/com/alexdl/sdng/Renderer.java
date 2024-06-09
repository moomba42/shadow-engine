package com.alexdl.sdng;

import com.alexdl.sdng.backend.Disposable;
import com.alexdl.sdng.backend.Light;
import com.alexdl.sdng.backend.vulkan.Model;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.List;

public interface Renderer extends Disposable {
    void queueModel(@Nonnull Model model);

    void draw();

    void updatePushConstant(@Nonnull Matrix4f transform);

    void updateLights(@Nonnull List<Light> lights);
}
