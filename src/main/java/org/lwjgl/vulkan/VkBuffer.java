package org.lwjgl.vulkan;

import javax.annotation.Nullable;

public record VkBuffer(long address, @Nullable VkDeviceMemory memory) {
}
