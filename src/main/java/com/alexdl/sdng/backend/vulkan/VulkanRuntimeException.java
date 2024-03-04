package com.alexdl.sdng.backend.vulkan;

public class VulkanRuntimeException extends RuntimeException {
    private final int vulkanResult;

    public VulkanRuntimeException(int vulkanResult, String message) {
        super(message);
        this.vulkanResult = vulkanResult;
    }

    @Override
    public String toString() {
        return String.format("%s (Code: %d)", getLocalizedMessage(), vulkanResult);
    }
}
