package com.alexdl.sdng.backend;

import com.alexdl.sdng.AssetLoader;
import com.alexdl.sdng.Disposables;
import com.alexdl.sdng.backend.vulkan.Material;
import com.alexdl.sdng.backend.vulkan.Mesh;
import com.alexdl.sdng.backend.vulkan.MeshData;
import com.alexdl.sdng.backend.vulkan.Model;
import com.alexdl.sdng.backend.vulkan.ResourceHandle;
import com.alexdl.sdng.backend.vulkan.Texture;
import com.alexdl.sdng.backend.vulkan.VulkanRenderer;
import com.alexdl.sdng.backend.vulkan.structs.VertexDataStruct;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.nio.IntBuffer;

public class StandardAssetLoader implements AssetLoader {
    private final VulkanRenderer renderer;
    private final Disposables disposables;

    @Inject
    public StandardAssetLoader(VulkanRenderer renderer, Disposables disposables) {
        this.renderer = renderer;
        this.disposables = disposables;
    }

    @Nonnull
    public Model loadModel(@Nonnull ResourceHandle resourceHandle) {
        VertexDataStruct.Buffer quad = new VertexDataStruct.Buffer(new float[]{
                -0.25f, 0.6f, 0, 1, 0, 0, 1, 1,
                -0.25f, -0.6f, 0, 0, 1, 0, 1, 0,
                0.25f, -0.6f, 0, 0, 0, 1, 0, 0,
                0.25f, 0.6f, 0, 1, 1, 0, 0, 1,
        });
        IntBuffer quadIndices = BufferUtils.createIntBuffer(6).put(0, new int[]{0, 1, 2, 2, 3, 0});

        MeshData data = new MeshData(
                renderer.getGraphicsQueue(),
                renderer.getGraphicsCommandPool(),
                quad, quadIndices
        );

        quad.dispose();
        disposables.add(data);

        Texture diffuse = loadTexture(new ResourceHandle("art.png"));
        Material material = new Material(diffuse);

        return new Model(new Mesh(data, material), new Matrix4f().identity());
    }

    @NotNull
    @Override
    public Texture loadTexture(@NotNull ResourceHandle resourceHandle) {
        return renderer.createTexture(resourceHandle.uri());
    }
}
