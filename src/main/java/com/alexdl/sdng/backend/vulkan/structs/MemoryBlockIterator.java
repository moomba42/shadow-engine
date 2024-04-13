package com.alexdl.sdng.backend.vulkan.structs;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class MemoryBlockIterator<Element extends MemoryBlock<Element>> implements Iterator<Element> {
    private final long address;
    private final int count;
    private final int stride;
    private final Element factory;
    private int currentIndex;

    public MemoryBlockIterator(long address, int count, int stride, @Nonnull Element factory) {
        this.address = address;
        this.count = count;
        this.stride = stride;
        this.factory = factory;
        this.currentIndex = 0;
    }

    @Override
    public boolean hasNext() {
        return currentIndex < count;
    }

    @Override
    public Element next() {
        if(currentIndex >= count) {
            throw new NoSuchElementException();
        }
        return factory.createAt(address + Integer.toUnsignedLong(stride * currentIndex++));
    }
}
