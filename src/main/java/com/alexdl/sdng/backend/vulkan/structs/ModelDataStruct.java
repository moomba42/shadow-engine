package com.alexdl.sdng.backend.vulkan.structs;

import org.joml.Matrix4f;

import java.nio.FloatBuffer;

import static com.alexdl.sdng.backend.vulkan.SizeConstants.MATRIX_4F_SIZE_BYTES;
import static com.alexdl.sdng.backend.vulkan.SizeConstants.MATRIX_4F_SIZE_FLOATS;
import static org.lwjgl.system.MemoryUtil.memFloatBuffer;

public class ModelDataStruct extends MemoryBlock<ModelDataStruct> {
    private final FloatBuffer modelTransform;
    private final FloatBuffer normalTransform;

    public ModelDataStruct() {
        super(MATRIX_4F_SIZE_BYTES * 2);
        this.modelTransform = memFloatBuffer(address(), MATRIX_4F_SIZE_FLOATS);
        this.normalTransform = memFloatBuffer(address() + MATRIX_4F_SIZE_BYTES, MATRIX_4F_SIZE_FLOATS);
    }

    private ModelDataStruct(long address) {
        super(address, MATRIX_4F_SIZE_BYTES * 2);
        this.modelTransform = memFloatBuffer(address(), MATRIX_4F_SIZE_FLOATS);
        this.normalTransform = memFloatBuffer(address() + MATRIX_4F_SIZE_BYTES, MATRIX_4F_SIZE_FLOATS);
    }

    public Matrix4f modelTransform() {
        return new Matrix4f(modelTransform);
    }

    public ModelDataStruct modelTransform(Matrix4f value) {
        value.get(0, modelTransform);
        return this;
    }

    public Matrix4f normalTransform() {
        return new Matrix4f(normalTransform);
    }

    public ModelDataStruct normalTransform(Matrix4f value) {
        value.get(0, normalTransform);
        return this;
    }

    @Override
    public ModelDataStruct createAt(long address) {
        return new ModelDataStruct(address);
    }
}
