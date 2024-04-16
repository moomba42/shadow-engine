package com.alexdl.sdng.backend.vulkan;

import org.lwjgl.vulkan.VkDeviceMemory;
import org.lwjgl.vulkan.VkImage;
import org.lwjgl.vulkan.VkImageView;
import org.lwjgl.vulkan.enums.VkFormat;

public record Image(
        VkFormat format,
        VkImage image,
        VkDeviceMemory memory,
        VkImageView view
) {
}
