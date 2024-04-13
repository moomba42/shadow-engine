package com.alexdl.sdng.backend.vulkan.structs;

import org.joml.Matrix4f;

import java.nio.FloatBuffer;

import static com.alexdl.sdng.backend.vulkan.SizeConstants.MATRIX_4F_SIZE_BYTES;
import static com.alexdl.sdng.backend.vulkan.SizeConstants.MATRIX_4F_SIZE_FLOATS;
import static org.lwjgl.system.MemoryUtil.memFloatBuffer;

public class ModelDataStruct extends MemoryBlock<ModelDataStruct> {
    private final FloatBuffer transform;

    public ModelDataStruct() {
        super(MATRIX_4F_SIZE_BYTES);
        this.transform = memFloatBuffer(address(), MATRIX_4F_SIZE_FLOATS);
    }

    private ModelDataStruct(long address) {
        super(address, MATRIX_4F_SIZE_BYTES);
        this.transform = memFloatBuffer(address(), MATRIX_4F_SIZE_FLOATS);
    }

    public Matrix4f transform() {
        return new Matrix4f(transform);
    }

    public ModelDataStruct transform(Matrix4f value) {
        value.get(0, transform);
        return this;
    }

    @Override
    public ModelDataStruct createAt(long address) {
        return new ModelDataStruct(address);
    }
}
