package com.alexdl.shadowhaven.engine.vulkan;

import com.alexdl.shadowhaven.engine.GLFWRuntimeException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.lwjgl.vulkan.*;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memASCII;
import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_SURFACE_LOST_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanUtils {
    private static final int VULKAN_SDK_VERSION = VK_MAKE_API_VERSION(0, 1, 3, 215);
    private static final int PORTABILITY_REQUIREMENT_SDK_VERSION = VK_MAKE_API_VERSION(0, 1, 3, 216);
    private static final boolean REQUIRES_PORTABILITY_EXTENSION = Platform.get().equals(Platform.MACOSX) && VULKAN_SDK_VERSION >= PORTABILITY_REQUIREMENT_SDK_VERSION;
    public static final String NULL_STRING = null;
    public static void throwIfFailed(int vulkanResultCode) {
        if(vulkanResultCode != VK_SUCCESS) {
            throw new VulkanRuntimeException(vulkanResultCode, translateVulkanResult(vulkanResultCode));
        }
    }
    public static String translateVulkanResult(int vulkanResultCode) {
        switch (vulkanResultCode) {
            // Success codes
            case VK_SUCCESS:
                return "Command successfully completed.";
            case VK_NOT_READY:
                return "A fence or query has not yet completed.";
            case VK_TIMEOUT:
                return "A wait operation has not completed in the specified time.";
            case VK_EVENT_SET:
                return "An event is signaled.";
            case VK_EVENT_RESET:
                return "An event is unsignaled.";
            case VK_INCOMPLETE:
                return "A return array was too small for the result.";
            case VK_SUBOPTIMAL_KHR:
                return "A swap chain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";

            // Error codes
            case VK_ERROR_OUT_OF_HOST_MEMORY:
                return "A host memory allocation has failed.";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY:
                return "A device memory allocation has failed.";
            case VK_ERROR_INITIALIZATION_FAILED:
                return "Initialization of an object could not be completed for implementation-specific reasons.";
            case VK_ERROR_DEVICE_LOST:
                return "The logical or physical device has been lost.";
            case VK_ERROR_MEMORY_MAP_FAILED:
                return "Mapping of a memory object has failed.";
            case VK_ERROR_LAYER_NOT_PRESENT:
                return "A requested layer is not present or could not be loaded.";
            case VK_ERROR_EXTENSION_NOT_PRESENT:
                return "A requested extension is not supported.";
            case VK_ERROR_FEATURE_NOT_PRESENT:
                return "A requested feature is not supported.";
            case VK_ERROR_INCOMPATIBLE_DRIVER:
                return "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
            case VK_ERROR_TOO_MANY_OBJECTS:
                return "Too many objects of the type have already been created.";
            case VK_ERROR_FORMAT_NOT_SUPPORTED:
                return "A requested format is not supported on this device.";
            case VK_ERROR_SURFACE_LOST_KHR:
                return "A surface is no longer available.";
            case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR:
                return "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
            case VK_ERROR_OUT_OF_DATE_KHR:
                return "A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the "
                       + "swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue"
                       + "presenting to the surface.";
            case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR:
                return "The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an"
                       + " image.";
            case VK_ERROR_VALIDATION_FAILED_EXT:
                return "A validation layer found an error.";
            default:
                return "Unknown result code";
        }
    }

    static PointerBuffer enumeratePhysicalDevices(VkInstance instance, MemoryStack stack) {
        IntBuffer physicalDeviceCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumeratePhysicalDevices(instance, physicalDeviceCountPointer, null));

        PointerBuffer physicalDevices = stack.mallocPointer(physicalDeviceCountPointer.get(0));
        throwIfFailed(vkEnumeratePhysicalDevices(instance, physicalDeviceCountPointer, physicalDevices));

        return physicalDevices;
    }

    static VkExtensionProperties.Buffer enumerateInstanceExtensionProperties(@Nullable String layerName, MemoryStack stack) {
        IntBuffer extensionCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateInstanceExtensionProperties(layerName, extensionCountPointer, null));

        VkExtensionProperties.Buffer extensions = VkExtensionProperties.malloc(extensionCountPointer.get(0), stack);
        throwIfFailed(vkEnumerateInstanceExtensionProperties(layerName, extensionCountPointer, extensions));
        return extensions;
    }

    static VkQueueFamilyProperties.Buffer getPhysicalDeviceQueueFamilyProperties(VkPhysicalDevice physicalDevice, MemoryStack stack) {
        IntBuffer queueFamilyCountPointer = stack.mallocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCountPointer, null);

        VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCountPointer.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCountPointer, queueFamilies);
        return queueFamilies;
    }

    static void putAllGlfwExtensions(PointerBuffer target) {
        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();
        if(glfwExtensions == null) {
            throw new GLFWRuntimeException("No set of extensions allowing GLFW integration was found");
        }
        for (int i = 0; i < glfwExtensions.capacity(); i++) {
            // Each pointer points to an ASCII string which is the name of the required extension
            target.put(glfwExtensions.get(i));
        }
    }

    static VkPhysicalDevice findFirstSuitablePhysicalDevice(VkInstance instance) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer physicalDevices = enumeratePhysicalDevices(instance, stack);
            if(physicalDevices.limit() == 0) {
                return null;
            }

            for(int i = 0; i < physicalDevices.limit(); i++) {
                VkPhysicalDevice physicalDevice = new VkPhysicalDevice(physicalDevices.get(i), instance);
                if(isSuitableDevice(physicalDevice)) {
                    return physicalDevice;
                }
            }
        }
        return null;
    }

    static boolean isSuitableDevice(VkPhysicalDevice physicalDevice) {
        int graphicsQueueFamilyLocation = findGraphicsQueueFamilyLocation(physicalDevice);
        return graphicsQueueFamilyLocation >= 0;
    }

    static int findGraphicsQueueFamilyLocation(VkPhysicalDevice physicalDevice) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkQueueFamilyProperties.Buffer queueFamilies = getPhysicalDeviceQueueFamilyProperties(physicalDevice, stack);
            for(int i = 0; i < queueFamilies.queueCount(); i++) {
                VkQueueFamilyProperties queueFamily = queueFamilies.get(i);
                if(queueFamily.queueCount() > 0 && (queueFamily.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    static String findFirstUnsupportedExtension(VkExtensionProperties.Buffer actualExtensions, PointerBuffer requiredExtensionNamesAscii) {
        for (int i = 0; i < requiredExtensionNamesAscii.limit(); i++) {
            String requiredExtensionName = requiredExtensionNamesAscii.getStringASCII(i);
            boolean isContained = false;
            for (int j = 0; j < actualExtensions.capacity(); j++) {
                String actualExtensionName = actualExtensions.get(j).extensionNameString();
                if (Objects.equals(requiredExtensionName, actualExtensionName)) {
                    isContained = true;
                }
            }
            if (!isContained) {
                return requiredExtensionName;
            }
        }
        return null;
    }

    static VkInstance createInstance(String applicationName) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Info about the app itself
            VkApplicationInfo applicationInfo = VkApplicationInfo.malloc(stack)
                    // We specify the type of struct that this struct is because there is no reflection in C.
                    .sType$Default()
                    .pNext(NULL)
                    .pApplicationName(stack.UTF8(applicationName))
                    .applicationVersion(VK_MAKE_API_VERSION(0, 1, 0, 0))
                    .pEngineName(stack.UTF8("Shadow Engine"))
                    .apiVersion(VK_MAKE_API_VERSION(0, 1, 1, 0)); // this affects the app

            // Create required extensions list
            PointerBuffer requiredExtensionNamesAscii = stack.mallocPointer(64);
            putAllGlfwExtensions(requiredExtensionNamesAscii);
            if (REQUIRES_PORTABILITY_EXTENSION) {
                // This is needed for vulkan sdk version >= 1.3.216 on macOS
                requiredExtensionNamesAscii.put(memASCII(KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME));
            }
            requiredExtensionNamesAscii.flip();

            // Get actual extension list and compare with required
            VkExtensionProperties.Buffer actualExtensions = enumerateInstanceExtensionProperties(NULL_STRING, stack);
            String unsupportedExtension = findFirstUnsupportedExtension(actualExtensions, requiredExtensionNamesAscii);
            if (unsupportedExtension != null) {
                throw new RuntimeException("Unsupported extension: " + unsupportedExtension);
            }

            // Create flags list
            int flags = 0;
            if (REQUIRES_PORTABILITY_EXTENSION) {
                // This is needed for vulkan sdk version >= 1.3.216 on macOS
                flags |= KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
            }

            // Info to create a Vulkan instance
            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(flags)
                    .pApplicationInfo(applicationInfo)
                    .ppEnabledLayerNames(null) // null is interpreted as null pointer
                    .ppEnabledExtensionNames(requiredExtensionNamesAscii);

            PointerBuffer instancePointer = stack.mallocPointer(1);
            throwIfFailed(vkCreateInstance(createInfo, null, instancePointer));
            return new VkInstance(instancePointer.get(0), createInfo);
        }
    }

    public static VkDevice createLogicalDevice(VkPhysicalDevice physicalDevice) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            int graphicsQueueFamilyIndex = findGraphicsQueueFamilyLocation(physicalDevice);
            FloatBuffer priorityPointer = stack.floats(1.0f);

            VkDeviceQueueCreateInfo queueCreateInfo = VkDeviceQueueCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .queueFamilyIndex(graphicsQueueFamilyIndex)
                    .pQueuePriorities(priorityPointer); // Also sets queueCount to the number of priorities
            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.malloc(1, stack);
            queueCreateInfos.put(0, queueCreateInfo);

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);

            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pQueueCreateInfos(queueCreateInfos) // Also sets queueCreateInfoCount
                    .ppEnabledExtensionNames(null) // Also sets enabledExtensionCount
                    .pEnabledFeatures(deviceFeatures);

            PointerBuffer logicalDevicePointer = stack.mallocPointer(1);
            throwIfFailed(vkCreateDevice(physicalDevice, deviceCreateInfo, null, logicalDevicePointer));

            return new VkDevice(logicalDevicePointer.get(0), physicalDevice, deviceCreateInfo);
        }
    }

    public static VkQueue findFirstQueueByFamily(VkDevice logicalDevice, int familyIndex) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer queuePointer = stack.mallocPointer(1);
            vkGetDeviceQueue(logicalDevice, familyIndex, 0, queuePointer);
            return new VkQueue(queuePointer.get(0), logicalDevice);
        }
    }
}
