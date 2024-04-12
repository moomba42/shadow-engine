package com.alexdl.sdng.backend.vulkan.structs;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
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

public class PushConstantData extends Struct<PushConstantData> implements NativeResource {
    public static final int SIZE_BYTES, ALIGNMENT_BYTES;
    public static final int FIELD_MODEL;

    static {
        Layout layout = __struct(
                __member(MATRIX_4F_SIZE_BYTES)
        );

        SIZE_BYTES = layout.getSize();
        ALIGNMENT_BYTES = layout.getAlignment();

        FIELD_MODEL = layout.offsetof(0);
    }

    private final FloatBuffer model;

    protected PushConstantData(long address, @Nullable ByteBuffer container) {
        super(address, container);
        this.model = MemoryUtil.memFloatBuffer(address() + FIELD_MODEL, MATRIX_4F_SIZE_FLOATS);
    }

    public PushConstantData(ByteBuffer container) {
        this(memAddress(container), __checkContainer(container, SIZE_BYTES));
    }

    public static PushConstantData calloc() {
        return new PushConstantData(nmemCallocChecked(1, SIZE_BYTES), null);
    }

    public static PushConstantData.Buffer calloc(int capacity) {
        return new PushConstantData.Buffer(nmemCallocChecked(capacity, SIZE_BYTES), capacity);
    }

    public static PushConstantData calloc(MemoryStack stack) {
        return new PushConstantData(stack.ncalloc(ALIGNMENT_BYTES, 1, SIZE_BYTES), null);
    }

    public static PushConstantData calloc(MemoryStack stack, int capacity) {
        return new PushConstantData(stack.ncalloc(ALIGNMENT_BYTES, capacity, SIZE_BYTES), null);
    }

    public PushConstantData model(Matrix4f value) {
        value.get(0, model);
        return this;
    }
    public Matrix4f model() {
        return new Matrix4f(model);
    }

    public PushConstantData set(PushConstantData src) {
        memCopy(src.address(), address(), SIZE_BYTES);
        return this;
    }

    @Override
    protected @Nonnull PushConstantData create(long address, @Nullable ByteBuffer container) {
        return new PushConstantData(address, container);
    }

    @Override
    public int sizeof() {
        return SIZE_BYTES;
    }

    @Override
    public void close() {
        NativeResource.super.close();
    }
    public static class Buffer extends StructBuffer<PushConstantData, PushConstantData.Buffer> implements NativeResource {
        private static final PushConstantData ELEMENT_FACTORY = new PushConstantData(-1, null);

        protected Buffer(ByteBuffer container) {
            super(container, container.remaining() / SIZE_BYTES);
        }

        protected Buffer(long address, int capacity) {
            super(address, null, -1, 0, capacity, capacity);
        }

        @Override
        protected @Nonnull PushConstantData getElementFactory() {
            return ELEMENT_FACTORY;
        }

        @Override
        protected @Nonnull PushConstantData.Buffer self() {
            return this;
        }
    }
}
