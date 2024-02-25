package org.lwjgl.vulkan.enums;

import org.lwjgl.vulkan.KHRSharedPresentableImage;
import org.lwjgl.vulkan.KHRSurface;

public enum VkPresentModeKHR {
    VK_PRESENT_MODE_IMMEDIATE_KHR(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR),
    VK_PRESENT_MODE_MAILBOX_KHR(KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR),
    VK_PRESENT_MODE_FIFO_KHR(KHRSurface.VK_PRESENT_MODE_FIFO_KHR),
    VK_PRESENT_MODE_FIFO_RELAXED_KHR(KHRSurface.VK_PRESENT_MODE_FIFO_RELAXED_KHR),
    VK_PRESENT_MODE_SHARED_DEMAND_REFRESH_KHR(KHRSharedPresentableImage.VK_PRESENT_MODE_SHARED_DEMAND_REFRESH_KHR),
    VK_PRESENT_MODE_SHARED_CONTINUOUS_REFRESH_KHR(KHRSharedPresentableImage.VK_PRESENT_MODE_SHARED_CONTINUOUS_REFRESH_KHR);

    private final int value;

    VkPresentModeKHR(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static VkPresentModeKHR of(int value) {
        for (VkPresentModeKHR mode : values()) {
            if(mode.value == value) {
                return mode;
            }
        }
        throw new IllegalStateException("Unexpected value: " + value);
    }
}
