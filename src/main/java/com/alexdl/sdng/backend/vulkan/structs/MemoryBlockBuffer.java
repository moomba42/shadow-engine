package com.alexdl.sdng.backend.vulkan.structs;

import com.alexdl.sdng.backend.Disposable;

import javax.annotation.Nonnull;
import java.util.Iterator;

import static org.lwjgl.system.MemoryUtil.*;

public class MemoryBlockBuffer<Element extends MemoryBlock<Element>> implements Iterable<Element>, Disposable {
    private final long address;
    private final int count;
    private final int size;
    private final int elementSize;
    private final Element factory;

    public MemoryBlockBuffer(long address, int count, int elementSize, Element factory) {
        this.count = count;
        this.elementSize = elementSize;
        this.size = elementSize * factory.size();
        this.factory = factory;
        this.address = address;
    }

    public MemoryBlockBuffer(long address, int count, Element factory) {
        this.count = count;
        this.elementSize = factory.size();
        this.size = elementSize * count;
        this.factory = factory;
        this.address = address;
    }

    public MemoryBlockBuffer(int count, Element factory) {
        this.count = count;
        this.elementSize = factory.size();
        this.size = elementSize * count;
        this.factory = factory;
        this.address = nmemCalloc(count, factory.size());
    }

    public MemoryBlockBuffer(int count, Element factory, int alignment) {
        this.count = count;
        this.elementSize = alignSize(factory.size(), alignment);
        this.size = elementSize * count;
        this.factory = factory;
        this.address = nmemAlignedAlloc(alignment, size);
        clear();
    }

    public int size() {
        return size;
    }

    public int elementSize() {
        return elementSize;
    }

    public long address() {
        return address;
    }

    public int count() {
        return count;
    }

    public void clear() {
        memSet(address, 0, size);
    }

    public @Nonnull Element get(int index) {
        return factory.createAt(address + Integer.toUnsignedLong(elementSize * index));
    }

    @Override
    public @Nonnull Iterator<Element> iterator() {
        return new MemoryBlockIterator<>(address, count, elementSize, factory);
    }

    @Override
    public void dispose() {
        nmemFree(address);
    }

    private static int alignSize(int size, int alignment) {
        return (size + alignment - 1) & -alignment;
    }
}
