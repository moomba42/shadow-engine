package com.alexdl.sdng.backend;

import com.alexdl.sdng.AssetLoader;
import com.alexdl.sdng.backend.vulkan.Mesh;
import com.alexdl.sdng.backend.vulkan.ResourceHandle;

import javax.annotation.Nonnull;

public class SampleDataAssetLoader implements AssetLoader {
    @Nonnull
    public Mesh loadMesh(@Nonnull ResourceHandle resourceHandle) {
        throw new RuntimeException("Not implemented yet");
    }
}
