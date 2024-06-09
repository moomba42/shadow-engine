package com.alexdl.sdng.backend.vulkan;

import com.alexdl.sdng.rendering.Texture;
import org.lwjgl.vulkan.VkDescriptorSet;

public record VulkanTexture(VkDescriptorSet descriptorSet, Image image) implements Texture {
}
