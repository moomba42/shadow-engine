package com.alexdl.sdng.backend.vulkan.structs;

import org.joml.Matrix4f;

import java.nio.FloatBuffer;

import static com.alexdl.sdng.backend.vulkan.SizeConstants.MATRIX_4F_SIZE_BYTES;
import static com.alexdl.sdng.backend.vulkan.SizeConstants.MATRIX_4F_SIZE_FLOATS;
import static org.lwjgl.system.MemoryUtil.memFloatBuffer;

public class PushConstantStruct extends MemoryBlock<PushConstantStruct> {
    public static final int SIZE = MATRIX_4F_SIZE_BYTES;
    private final FloatBuffer transform;

    public PushConstantStruct() {
        super(SIZE);
        this.transform = memFloatBuffer(address(), MATRIX_4F_SIZE_FLOATS);
    }

    private PushConstantStruct(long address) {
        super(address, SIZE);
        this.transform = memFloatBuffer(address(), MATRIX_4F_SIZE_FLOATS);
    }

    public Matrix4f transform() {
        return new Matrix4f(transform);
    }

    public PushConstantStruct transform(Matrix4f value) {
        value.get(0, transform);
        return this;
    }

    @Override
    public PushConstantStruct createAt(long address) {
        return new PushConstantStruct(address);
    }
}
