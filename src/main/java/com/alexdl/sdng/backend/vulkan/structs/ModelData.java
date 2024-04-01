package com.alexdl.sdng.backend.vulkan.structs;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static com.alexdl.sdng.backend.vulkan.SizeConstants.MATRIX_4F_SIZE_BYTES;
import static com.alexdl.sdng.backend.vulkan.SizeConstants.MATRIX_4F_SIZE_FLOATS;
import static org.lwjgl.system.MemoryUtil.*;

public class ModelData extends Struct<ModelData> implements NativeResource {
    private static final long MIN_SIZE_BYTES = MATRIX_4F_SIZE_BYTES;
    private final long sizeBytes;

    private final FloatBuffer model;

    protected ModelData(long address, @Nullable ByteBuffer container, long sizeBytes) {
        super(address, container);

        this.sizeBytes = sizeBytes;
        this.model = MemoryUtil.memFloatBuffer(address(), MATRIX_4F_SIZE_FLOATS);
    }

    protected ModelData(long address, @Nullable ByteBuffer container) {
        super(address, container);

        this.sizeBytes = getAlignedSizeBytes(0);
        this.model = MemoryUtil.memFloatBuffer(address(), MATRIX_4F_SIZE_FLOATS);
    }

    public static long getAlignedSizeBytes(long alignmentBytes) {
        return (MIN_SIZE_BYTES + alignmentBytes - 1) & ~(alignmentBytes - 1);
    }

    public static ModelData alignedAlloc(int alignmentBytes) {
        long alignedSizeBytes = getAlignedSizeBytes(alignmentBytes);
        return new ModelData(nmemAlignedAlloc(alignedSizeBytes, alignedSizeBytes), null, alignedSizeBytes);
    }

    public static ModelData.Buffer alignedAlloc(long alignmentBytes, int count) {
        long alignedSizeBytes = getAlignedSizeBytes(alignmentBytes);
        return new ModelData.Buffer(nmemAlignedAlloc(alignedSizeBytes, alignedSizeBytes * count), alignedSizeBytes, count);
    }

    public ModelData model(Matrix4f value) {
        value.get(0, model);
        return this;
    }

    public Matrix4f model() {
        return new Matrix4f(model);
    }

    public ModelData set(Matrix4f model) {
        model(model);
        return this;
    }

    public ModelData set(ModelData src) {
        memCopy(src.address(), address(), sizeBytes);
        return this;
    }


    @Override
    protected @Nonnull ModelData create(long address, @Nullable ByteBuffer container) {
        return new ModelData(address, container, sizeBytes);
    }

    @Override
    public int sizeof() {
        return (int) sizeBytes;
    }

    @Override
    public void close() {
        NativeResource.super.close();
    }

    public static class Buffer extends StructBuffer<ModelData, ModelData.Buffer> implements NativeResource {
        private final ModelData elementFactory;

        protected Buffer(long address, long alignedSizeBytes, int capacity) {
            super(address, null, -1, 0, capacity, capacity);
            this.elementFactory = new ModelData(-1, null, alignedSizeBytes);
        }

        @Override
        protected @Nonnull ModelData getElementFactory() {
            return elementFactory;
        }

        @Override
        protected @Nonnull ModelData.Buffer self() {
            return this;
        }
    }
}
