package com.alexdl.sdng;

import javax.annotation.Nonnull;

public interface FileLoader {
    @Nonnull File loadFile(@Nonnull FileHandle fileHandle);
}
