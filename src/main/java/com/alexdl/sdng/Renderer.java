package com.alexdl.sdng;

import com.alexdl.sdng.backend.Disposable;
import org.joml.Matrix4f;

public interface Renderer extends Disposable {
    void draw();

    void updateModel(int modelId, Matrix4f transform);

    void updatePushConstant(Matrix4f transform);
}
