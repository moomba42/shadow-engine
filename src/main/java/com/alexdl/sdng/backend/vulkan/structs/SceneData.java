package com.alexdl.sdng.backend.vulkan.structs;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Struct;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static com.alexdl.sdng.backend.vulkan.SizeConstants.MATRIX_4F_SIZE_BYTES;
import static com.alexdl.sdng.backend.vulkan.SizeConstants.MATRIX_4F_SIZE_FLOATS;
import static org.lwjgl.system.MemoryUtil.*;

public class SceneData extends Struct<SceneData> implements NativeResource {
    public static final int SIZE_BYTES, ALIGNMENT_BYTES;
    public static final int FIELD_PROJECTION, FIELD_VIEW;

    static {
        Layout layout = __struct(
                __member(MATRIX_4F_SIZE_BYTES),
                __member(MATRIX_4F_SIZE_BYTES)
        );

        SIZE_BYTES = layout.getSize();
        ALIGNMENT_BYTES = layout.getAlignment();

        FIELD_PROJECTION = layout.offsetof(0);
        FIELD_VIEW = layout.offsetof(1);
    }

    private final FloatBuffer projection, view;

    protected SceneData(long address, @Nullable ByteBuffer container) {
        super(address, container);
        this.projection = MemoryUtil.memFloatBuffer(address() + FIELD_PROJECTION, MATRIX_4F_SIZE_FLOATS);
        this.view = MemoryUtil.memFloatBuffer(address() + FIELD_VIEW, MATRIX_4F_SIZE_FLOATS);
    }

    public SceneData(ByteBuffer container) {
        this(memAddress(container), __checkContainer(container, SIZE_BYTES));
    }

    public static SceneData calloc() {
        return new SceneData(nmemCallocChecked(1, SIZE_BYTES), null);
    }

    public static SceneData calloc(MemoryStack stack) {
        return new SceneData(stack.ncalloc(ALIGNMENT_BYTES, 1, SIZE_BYTES), null);
    }

    public SceneData view(Matrix4f value) {
        value.get(0, view);
        return this;
    }
    public Matrix4f view() {
        return new Matrix4f(view);
    }

    public SceneData projection(Matrix4f value) {
        value.get(0, projection);
        return this;
    }
    public Matrix4f projection() {
        return new Matrix4f(projection);
    }

    public SceneData set(Matrix4f view, Matrix4f projection) {
        view(view);
        projection(projection);
        return this;
    }

    public SceneData set(SceneData src) {
        memCopy(src.address(), address(), SIZE_BYTES);
        return this;
    }

    @Override
    protected @Nonnull SceneData create(long address, @Nullable ByteBuffer container) {
        return new SceneData(address, container);
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
