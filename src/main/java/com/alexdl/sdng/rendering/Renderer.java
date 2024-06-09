package com.alexdl.sdng.rendering;

import com.alexdl.sdng.backend.Disposable;

import javax.annotation.Nonnull;
import java.util.List;

public interface Renderer extends Disposable {
    void updateLights(@Nonnull List<Light> lights);

    void queueModel(@Nonnull Model model);

    void draw();
}
