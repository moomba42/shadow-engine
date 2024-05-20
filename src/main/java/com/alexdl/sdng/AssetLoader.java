package com.alexdl.sdng;

import com.alexdl.sdng.backend.vulkan.Mesh;
import com.alexdl.sdng.backend.vulkan.ResourceHandle;

import javax.annotation.Nonnull;

public interface AssetLoader {
    @Nonnull Mesh loadMesh(@Nonnull ResourceHandle resourceHandle);
}
