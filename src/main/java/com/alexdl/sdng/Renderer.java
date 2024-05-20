package com.alexdl.sdng;

import com.alexdl.sdng.backend.Disposable;
import com.alexdl.sdng.backend.vulkan.Mesh;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

public interface Renderer extends Disposable {
    void queueMesh(@Nonnull Mesh mesh);

    void draw();

    void updatePushConstant(Matrix4f transform);
}
