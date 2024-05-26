package com.alexdl.sdng;

import com.alexdl.sdng.backend.vulkan.Model;
import com.alexdl.sdng.backend.vulkan.ResourceHandle;
import com.alexdl.sdng.backend.vulkan.Texture;

import javax.annotation.Nonnull;

public interface AssetLoader {
    @Nonnull
    Model loadModel(@Nonnull ResourceHandle resourceHandle);

    @Nonnull
    Texture loadTexture(@Nonnull ResourceHandle resourceHandle);
}
