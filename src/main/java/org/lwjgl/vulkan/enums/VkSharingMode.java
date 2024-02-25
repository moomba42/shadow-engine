package org.lwjgl.vulkan.enums;

import org.lwjgl.vulkan.VK10;

public enum VkSharingMode {
    VK_SHARING_MODE_EXCLUSIVE(VK10.VK_SHARING_MODE_EXCLUSIVE),
    VK_SHARING_MODE_CONCURRENT(VK10.VK_SHARING_MODE_CONCURRENT);

    private final int value;

    VkSharingMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static VkSharingMode of(int value) {
        for (VkSharingMode mode : values()) {
            if(mode.value == value) {
                return mode;
            }
        }
        throw new IllegalStateException("Unexpected value: " + value);
    }
}
