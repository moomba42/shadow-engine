package com.alexdl.sdng.backend.vulkan.structs;

import static com.alexdl.sdng.backend.vulkan.SizeConstants.VECTOR_2F_SIZE_BYTES;
import static com.alexdl.sdng.backend.vulkan.SizeConstants.VECTOR_3F_SIZE_BYTES;
import static org.lwjgl.system.MemoryUtil.memFloatBuffer;
import static org.lwjgl.system.MemoryUtil.memPutFloat;

public class VertexDataStruct extends MemoryBlock<VertexDataStruct> {
    private static final int SIZE_BYTES = (VECTOR_3F_SIZE_BYTES * 3) + VECTOR_2F_SIZE_BYTES;

    public VertexDataStruct() {
        super(SIZE_BYTES);
    }

    private VertexDataStruct(long address) {
        super(address, SIZE_BYTES);
    }

    public VertexDataStruct position(float x, float y, float z) {
        memPutFloat(address(), x);
        memPutFloat(address() + Float.BYTES, y);
        memPutFloat(address() + (Float.BYTES * 2), z);
        return this;
    }

    public VertexDataStruct normal(float x, float y, float z) {
        memPutFloat(address() + VECTOR_3F_SIZE_BYTES, x);
        memPutFloat(address() + VECTOR_3F_SIZE_BYTES + Float.BYTES, y);
        memPutFloat(address() + VECTOR_3F_SIZE_BYTES + (Float.BYTES * 2), z);
        return this;
    }

    public VertexDataStruct uv(float u, float v) {
        memPutFloat(address() + (VECTOR_3F_SIZE_BYTES * 2), u);
        memPutFloat(address() + (VECTOR_3F_SIZE_BYTES * 2) + Float.BYTES, v);
        return this;
    }

    public VertexDataStruct color(float r, float g, float b) {
        memPutFloat(address() + (VECTOR_3F_SIZE_BYTES * 2) + VECTOR_2F_SIZE_BYTES, r);
        memPutFloat(address() + (VECTOR_3F_SIZE_BYTES * 2) + VECTOR_2F_SIZE_BYTES + Float.BYTES, g);
        memPutFloat(address() + (VECTOR_3F_SIZE_BYTES * 2) + VECTOR_2F_SIZE_BYTES + (Float.BYTES * 2), b);
        return this;
    }

    @Override
    public VertexDataStruct createAt(long address) {
        return new VertexDataStruct(address);
    }

    public static class Buffer extends MemoryBlockBuffer<VertexDataStruct> {
        public Buffer(float[] data) {
            this(data, new VertexDataStruct(-1));
        }

        private Buffer(float[] data, VertexDataStruct factory) {
            super(Math.floorDiv(data.length * Float.BYTES, factory.size()), factory);
            memFloatBuffer(address(), count() * (factory.size() / Float.BYTES)).put(0, data);
        }
    }
}
