package com.alexdl.sdng.backend.vulkan;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Struct;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static com.alexdl.sdng.backend.vulkan.VulkanConstants.MATRIX_4F_SIZE_BYTES;
import static com.alexdl.sdng.backend.vulkan.VulkanConstants.MATRIX_4F_SIZE_FLOATS;
import static org.lwjgl.system.MemoryUtil.*;

public class MVP extends Struct<MVP> implements NativeResource {
    public static final int SIZE_BYTES;

    public static final int ALIGNMENT_BYTES;

    public static final int FIELD_PROJECTION, FIELD_VIEW, FIELD_MODEL;

    static {
        Layout layout = __struct(
                __member(MATRIX_4F_SIZE_BYTES),
                __member(MATRIX_4F_SIZE_BYTES),
                __member(MATRIX_4F_SIZE_BYTES)
        );

        SIZE_BYTES = layout.getSize();
        ALIGNMENT_BYTES = layout.getAlignment();

        FIELD_PROJECTION = layout.offsetof(0);
        FIELD_VIEW = layout.offsetof(1);
        FIELD_MODEL = layout.offsetof(2);
    }

    private final FloatBuffer projection, view, model;

    /**
     * Creates a struct instance at the specified address.
     *
     * @param address   the struct memory address
     * @param container an optional container buffer, to be referenced strongly by the struct instance.
     */
    protected MVP(long address, @Nullable ByteBuffer container) {
        super(address, container);
        this.projection = MemoryUtil.memFloatBuffer(address() + FIELD_PROJECTION, MATRIX_4F_SIZE_FLOATS);
        this.view = MemoryUtil.memFloatBuffer(address() + FIELD_VIEW, MATRIX_4F_SIZE_FLOATS);
        this.model = MemoryUtil.memFloatBuffer(address() + FIELD_MODEL, MATRIX_4F_SIZE_FLOATS);
    }

    public MVP(ByteBuffer container) {
        this(memAddress(container), __checkContainer(container, SIZE_BYTES));
    }

    public static MVP calloc() {
        return new MVP(nmemCallocChecked(1, SIZE_BYTES), null);
    }

    public static MVP calloc(MemoryStack stack) {
        return new MVP(stack.ncalloc(ALIGNMENT_BYTES, 1, SIZE_BYTES), null);
    }


    public MVP model(Matrix4f value) {
        value.get(0, model);
        return this;
    }

    public MVP view(Matrix4f value) {
        value.get(0, view);
        return this;
    }

    public MVP projection(Matrix4f value) {
        value.get(0, projection);
        return this;
    }

    public MVP set(Matrix4f model, Matrix4f view, Matrix4f projection) {
        model(model);
        view(view);
        projection(projection);
        return this;
    }

    public MVP set(MVP src) {
        memCopy(src.address(), address(), SIZE_BYTES);
        return this;
    }

    public Matrix4f model() {
        return new Matrix4f(model);
    }

    public Matrix4f view() {
        return new Matrix4f(view);
    }

    public Matrix4f projection() {
        return new Matrix4f(projection);
    }

    @Override
    protected @Nonnull MVP create(long address, @Nullable ByteBuffer container) {
        return new MVP(address, container);
    }

    @Override
    public int sizeof() {
        return SIZE_BYTES;
    }

    @Override
    public void close() {
        NativeResource.super.close();
    }
}
