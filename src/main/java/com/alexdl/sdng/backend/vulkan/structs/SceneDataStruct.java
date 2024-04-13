package com.alexdl.sdng.backend.vulkan.structs;

import org.joml.Matrix4f;

import java.nio.FloatBuffer;

import static com.alexdl.sdng.backend.vulkan.SizeConstants.MATRIX_4F_SIZE_BYTES;
import static com.alexdl.sdng.backend.vulkan.SizeConstants.MATRIX_4F_SIZE_FLOATS;
import static org.lwjgl.system.MemoryUtil.memFloatBuffer;

public class SceneDataStruct extends MemoryBlock<SceneDataStruct> {
    private final FloatBuffer projection, view;

    public SceneDataStruct() {
        super(MATRIX_4F_SIZE_BYTES * 2);
        this.projection = memFloatBuffer(address(), MATRIX_4F_SIZE_FLOATS);
        this.view = memFloatBuffer(address() + MATRIX_4F_SIZE_BYTES, MATRIX_4F_SIZE_FLOATS);
    }

    private SceneDataStruct(long address) {
        super(address, MATRIX_4F_SIZE_BYTES * 2);
        this.projection = memFloatBuffer(address(), MATRIX_4F_SIZE_FLOATS);
        this.view = memFloatBuffer(address() + MATRIX_4F_SIZE_BYTES, MATRIX_4F_SIZE_FLOATS);
    }

    public SceneDataStruct view(Matrix4f value) {
        value.get(0, view);
        return this;
    }
    public Matrix4f view() {
        return new Matrix4f(view);
    }

    public SceneDataStruct projection(Matrix4f value) {
        value.get(0, projection);
        return this;
    }
    public Matrix4f projection() {
        return new Matrix4f(projection);
    }

    @Override
    public SceneDataStruct createAt(long address) {
        return new SceneDataStruct(address);
    }
}
