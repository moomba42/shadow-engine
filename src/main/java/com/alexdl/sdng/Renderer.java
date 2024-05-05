package com.alexdl.sdng;

import org.joml.Matrix4f;

public interface Renderer {
    void draw();

    void updateModel(int modelId, Matrix4f transform);

    void updatePushConstant(Matrix4f transform);
}
