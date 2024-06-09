package com.alexdl.sdng;

import com.alexdl.sdng.rendering.Model;
import com.alexdl.sdng.rendering.Texture;

import javax.annotation.Nonnull;

public interface AssetLoader {
    @Nonnull
    Model loadModel(@Nonnull FileHandle resourceHandle);

    @Nonnull
    Texture loadTexture(@Nonnull FileHandle resourceHandle);
}
