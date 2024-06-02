package com.alexdl.sdng.backend.vulkan.structs;

import com.alexdl.sdng.backend.Disposable;

import static org.lwjgl.system.Checks.CHECKS;
import static org.lwjgl.system.MemoryUtil.*;

// TODO: Create an alignment mechanism to avoid problems with vulkan
//  https://stackoverflow.com/questions/45638520/ubos-and-their-alignments-in-vulkan
//  https://fvcaputo.github.io/2019/02/06/memory-alignment.html
public abstract class MemoryBlock<Subclass extends MemoryBlock<Subclass>> implements Disposable {
    private final long address;
    private final int size;

    public MemoryBlock(long address, int size) {
        if (CHECKS && address == NULL) {
            throw new NullPointerException();
        }
        this.size = size;
        this.address = address;
    }

    public MemoryBlock(int size) {
        this(nmemCalloc(1, size), size);
    }

    public int size() {
        return size;
    }

    public long address() {
        return address;
    }

    @Override
    public void dispose() {
        nmemFree(address);
    }

    public void clear() {
        memSet(address, 0, size);
    }

    public abstract Subclass createAt(long address);
}
