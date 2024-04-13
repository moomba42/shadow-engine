package com.alexdl.sdng.backend.vulkan.structs;

import static com.alexdl.sdng.backend.vulkan.SizeConstants.VECTOR_3F_SIZE_BYTES;
import static org.lwjgl.system.MemoryUtil.memFloatBuffer;
import static org.lwjgl.system.MemoryUtil.memPutFloat;

public class VertexDataStruct extends MemoryBlock<VertexDataStruct> {

    public VertexDataStruct() {
        super(VECTOR_3F_SIZE_BYTES * 2);
    }

    private VertexDataStruct(long address) {
        super(address, VECTOR_3F_SIZE_BYTES * 2);
    }

    public VertexDataStruct position(float x, float y, float z) {
        memPutFloat(address(), x);
        memPutFloat(address() + Float.BYTES, y);
        memPutFloat(address() + (Float.BYTES * 2), z);
        return this;
    }

    public VertexDataStruct color(float r, float g, float b) {
        memPutFloat(address() + VECTOR_3F_SIZE_BYTES, r);
        memPutFloat(address() + VECTOR_3F_SIZE_BYTES + Float.BYTES, g);
        memPutFloat(address() + VECTOR_3F_SIZE_BYTES + (Float.BYTES * 2), b);
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
