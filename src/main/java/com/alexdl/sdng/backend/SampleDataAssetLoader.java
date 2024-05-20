package com.alexdl.sdng.backend;

import com.alexdl.sdng.AssetLoader;
import com.alexdl.sdng.Disposables;
import com.alexdl.sdng.backend.vulkan.Mesh;
import com.alexdl.sdng.backend.vulkan.MeshData;
import com.alexdl.sdng.backend.vulkan.ResourceHandle;
import com.alexdl.sdng.backend.vulkan.VulkanRenderer;
import com.alexdl.sdng.backend.vulkan.structs.VertexDataStruct;
import org.lwjgl.BufferUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.nio.IntBuffer;

public class SampleDataAssetLoader implements AssetLoader {
    private final VulkanRenderer renderer;
    private final Disposables disposables;

    @Inject
    public SampleDataAssetLoader(VulkanRenderer renderer, Disposables disposables) {
        this.renderer = renderer;
        this.disposables = disposables;
    }

    @Nonnull
    public Mesh loadMesh(@Nonnull ResourceHandle resourceHandle) {
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
                quad, quadIndices,
                renderer.createTexture("smiley.png")
        );

        quad.dispose();
        disposables.add(data);
        return new Mesh(data);
    }
}
