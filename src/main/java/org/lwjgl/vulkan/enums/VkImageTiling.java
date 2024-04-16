package org.lwjgl.vulkan.enums;

import org.lwjgl.vulkan.EXTImageDrmFormatModifier;
import org.lwjgl.vulkan.VK10;

public enum VkImageTiling {
    VK_IMAGE_TILING_OPTIMAL(VK10.VK_IMAGE_TILING_OPTIMAL),
    VK_IMAGE_TILING_LINEAR(VK10.VK_IMAGE_TILING_LINEAR),
    VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT(EXTImageDrmFormatModifier.VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT);
    // VK_IMAGE_TILING_MAX_ENUM // Not available for some reason

    private final int value;

    VkImageTiling(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static VkImageTiling of(int value) {
        for (VkImageTiling mode : values()) {
            if(mode.value == value) {
                return mode;
            }
        }
        throw new IllegalStateException("Unexpected value: " + value);
    }
}
