package com.alexdl.sdng.backend.vulkan.structs;

import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;

import static com.alexdl.sdng.backend.vulkan.SizeConstants.VECTOR_3F_SIZE_BYTES;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.nmemCallocChecked;

public class VertexData extends Struct<VertexData> implements NativeResource {
    public static final int SIZE_BYTES, ALIGNMENT_BYTES;
    public static final int FIELD_POSITION, FIELD_COLOR;

    static {
        Layout layout = __struct(0, VECTOR_3F_SIZE_BYTES * 2,
                __member(VECTOR_3F_SIZE_BYTES, VECTOR_3F_SIZE_BYTES, true),
                __member(VECTOR_3F_SIZE_BYTES, VECTOR_3F_SIZE_BYTES, true)
        );

        SIZE_BYTES = layout.getSize();
        ALIGNMENT_BYTES = layout.getAlignment();

        FIELD_POSITION = layout.offsetof(0);
        FIELD_COLOR = layout.offsetof(1);
    }

    protected VertexData(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected @Nonnull VertexData create(long address, @Nullable ByteBuffer container) {
        return new VertexData(address, container);
    }

    @Override
    public int sizeof() {
        return SIZE_BYTES;
    }

    @Override
    public void close() {
        NativeResource.super.close();
    }

    public Vector3f position() {
        return nativePosition(address());
    }

    public VertexData position(float x, float y, float z) {
        nativePosition(address(), x, y, z);
        return this;
    }

    public VertexData position(Vector3f value) {
        nativePosition(address(), value.x(), value.y(), value.z());
        return this;
    }

    public Vector3f color() {
        return nativeColor(address());
    }

    public VertexData color(float r, float g, float b) {
        nativeColor(address(), r, g, b);
        return this;
    }

    public VertexData color(Vector3f value) {
        nativeColor(address(), value.x(), value.y(), value.z());
        return this;
    }

    public VertexData copyFrom(VertexData src) {
        memCopy(src.address(), address(), SIZE_BYTES);
        return this;
    }

    public static Vector3f nativePosition(long struct) {
        return new Vector3f().setFromAddress(struct + FIELD_POSITION);
    }

    public static void nativePosition(long struct, float x, float y, float z) {
        UNSAFE.putFloat(struct + FIELD_POSITION, x);
        UNSAFE.putFloat(struct + FIELD_POSITION + Float.BYTES, y);
        UNSAFE.putFloat(struct + FIELD_POSITION + (Float.BYTES * 2), z);
    }

    public static Vector3f nativeColor(long struct) {
        return new Vector3f().setFromAddress(struct + FIELD_COLOR);
    }

    public static void nativeColor(long struct, float r, float g, float b) {
        UNSAFE.putFloat(struct + FIELD_COLOR, r);
        UNSAFE.putFloat(struct + FIELD_COLOR + Float.BYTES, g);
        UNSAFE.putFloat(struct + FIELD_COLOR + (Float.BYTES * 2), b);
    }

    public static VertexData calloc() {
        return new VertexData(nmemCallocChecked(1, SIZE_BYTES), null);
    }

    public static VertexData calloc(MemoryStack stack) {
        return new VertexData(stack.ncalloc(ALIGNMENT_BYTES, 1, SIZE_BYTES), null);
    }

    public static VertexData.Buffer calloc(int capacity) {
        return new VertexData.Buffer(nmemCallocChecked(capacity, SIZE_BYTES), capacity);
    }

    public static VertexData.Buffer calloc(int capacity, MemoryStack stack) {
        return new VertexData.Buffer(stack.ncalloc(ALIGNMENT_BYTES, capacity, SIZE_BYTES), capacity);
    }

    public static VertexData.Buffer create(float[] values) {
        assert((values.length * Float.BYTES) % SIZE_BYTES  == 0);
        int count = (values.length * Float.BYTES) / SIZE_BYTES;
        ByteBuffer container = __create(count, SIZE_BYTES);
        container.asFloatBuffer().put(0, values);
        return new VertexData.Buffer(container);
    }

    public static class Buffer extends StructBuffer<VertexData, Buffer> implements NativeResource {
        private static final VertexData ELEMENT_FACTORY = new VertexData(-1, null);

        protected Buffer(ByteBuffer container) {
            super(container, container.remaining() / SIZE_BYTES);
        }

        protected Buffer(long address, int capacity) {
            super(address, null, -1, 0, capacity, capacity);
        }

        @Override
        protected @Nonnull VertexData getElementFactory() {
            return ELEMENT_FACTORY;
        }

        @Override
        protected @Nonnull Buffer self() {
            return this;
        }
    }
}
