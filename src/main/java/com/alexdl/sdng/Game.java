package com.alexdl.sdng;

import com.alexdl.sdng.backend.Disposable;

public interface Game extends Disposable {
    void init();
    void update(double delta);
    void render();
}
