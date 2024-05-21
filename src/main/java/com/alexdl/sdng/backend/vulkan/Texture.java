package com.alexdl.sdng.backend.vulkan;

import org.lwjgl.vulkan.VkDescriptorSet;

public record Texture(VkDescriptorSet descriptorSet, Image image) {
}
