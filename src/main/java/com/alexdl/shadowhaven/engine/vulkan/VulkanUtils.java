package com.alexdl.shadowhaven.engine.vulkan;

import com.alexdl.shadowhaven.engine.GLFWRuntimeException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memASCII;
import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

@SuppressWarnings("SameParameterValue")
public class VulkanUtils {
    public static final String NULL_STRING = null;

    private static final VkDebugUtilsMessengerCallbackEXT debugCallbackFunction = VkDebugUtilsMessengerCallbackEXT.create(
            (messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                String severity = getVkDebugMessageSeverity(messageSeverity);

                String type = getVkDebugMessageType(messageTypes);

                VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                System.err.format(
                        "%s %s: [%s]\n\t%s\n",
                        type, severity, data.pMessageIdNameString(), data.pMessageString()
                );

                /*
                 * false indicates that layer should not bail out of an
                 * API call that had validation failures. This may mean that the
                 * app dies inside the driver due to invalid parameter(s).
                 * That's what would happen without validation layers, so we'll
                 * keep that behavior here.
                 */
                return VK_FALSE;
            }
    );

    private static String getVkDebugMessageType(int messageTypes) {
        String type;
        if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT) != 0) {
            type = "GENERAL";
        } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT) != 0) {
            type = "VALIDATION";
        } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT) != 0) {
            type = "PERFORMANCE";
        } else {
            type = "UNKNOWN";
        }
        return type;
    }

    private static String getVkDebugMessageSeverity(int messageSeverity) {
        String severity;
        if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT) != 0) {
            severity = "VERBOSE";
        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
            severity = "INFO";
        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
            severity = "WARNING";
        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
            severity = "ERROR";
        } else {
            severity = "UNKNOWN";
        }
        return severity;
    }

    public static void throwIfFailed(int vulkanResultCode) {
        if (vulkanResultCode != VK_SUCCESS) {
            throw new VulkanRuntimeException(vulkanResultCode, translateVulkanResult(vulkanResultCode));
        }
    }

    public static String translateVulkanResult(int vulkanResultCode) {
        return switch (vulkanResultCode) {
            // Success codes
            case VK_SUCCESS -> "Command successfully completed.";
            case VK_NOT_READY -> "A fence or query has not yet completed.";
            case VK_TIMEOUT -> "A wait operation has not completed in the specified time.";
            case VK_EVENT_SET -> "An event is signaled.";
            case VK_EVENT_RESET -> "An event is unsignaled.";
            case VK_INCOMPLETE -> "A return array was too small for the result.";
            case VK_SUBOPTIMAL_KHR ->
                    "A swap chain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";

            // Error codes
            case VK_ERROR_OUT_OF_HOST_MEMORY -> "A host memory allocation has failed.";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "A device memory allocation has failed.";
            case VK_ERROR_INITIALIZATION_FAILED ->
                    "Initialization of an object could not be completed for implementation-specific reasons.";
            case VK_ERROR_DEVICE_LOST -> "The logical or physical device has been lost.";
            case VK_ERROR_MEMORY_MAP_FAILED -> "Mapping of a memory object has failed.";
            case VK_ERROR_LAYER_NOT_PRESENT -> "A requested layer is not present or could not be loaded.";
            case VK_ERROR_EXTENSION_NOT_PRESENT -> "A requested extension is not supported.";
            case VK_ERROR_FEATURE_NOT_PRESENT -> "A requested feature is not supported.";
            case VK_ERROR_INCOMPATIBLE_DRIVER ->
                    "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
            case VK_ERROR_TOO_MANY_OBJECTS -> "Too many objects of the type have already been created.";
            case VK_ERROR_FORMAT_NOT_SUPPORTED -> "A requested format is not supported on this device.";
            case VK_ERROR_SURFACE_LOST_KHR -> "A surface is no longer available.";
            case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR ->
                    "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
            case VK_ERROR_OUT_OF_DATE_KHR ->
                    "A surface has changed in such a way that it is no longer compatible with the swap chain, and further presentation requests using the "
                    + "swap chain will fail. Applications must query the new surface properties and recreate their swap chain if they wish to continue"
                    + "presenting to the surface.";
            case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR ->
                    "The display used by a swap chain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an"
                    + " image.";
            case VK_ERROR_VALIDATION_FAILED_EXT -> "A validation layer found an error.";
            default -> "Unknown result code";
        };
    }

    static PointerBuffer enumeratePhysicalDevices(VkInstance instance, MemoryStack stack) {
        IntBuffer physicalDeviceCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumeratePhysicalDevices(instance, physicalDeviceCountPointer, null));

        PointerBuffer physicalDevices = stack.mallocPointer(physicalDeviceCountPointer.get(0));
        throwIfFailed(vkEnumeratePhysicalDevices(instance, physicalDeviceCountPointer, physicalDevices));

        return physicalDevices;
    }

    static VkExtensionProperties.Buffer enumerateDeviceExtensionProperties(VkPhysicalDevice physicalDevice, MemoryStack stack) {
        IntBuffer deviceExtensionCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateDeviceExtensionProperties(physicalDevice, NULL_STRING, deviceExtensionCountPointer, null));

        VkExtensionProperties.Buffer deviceExtensions = VkExtensionProperties.malloc(deviceExtensionCountPointer.get(0), stack);
        throwIfFailed(vkEnumerateDeviceExtensionProperties(physicalDevice, NULL_STRING, deviceExtensionCountPointer, deviceExtensions));

        return deviceExtensions;
    }

    static VkExtensionProperties.Buffer enumerateInstanceExtensionProperties(MemoryStack stack) {
        IntBuffer extensionCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateInstanceExtensionProperties(NULL_STRING, extensionCountPointer, null));

        VkExtensionProperties.Buffer extensions = VkExtensionProperties.malloc(extensionCountPointer.get(0), stack);
        throwIfFailed(vkEnumerateInstanceExtensionProperties(NULL_STRING, extensionCountPointer, extensions));
        return extensions;
    }

    static VkLayerProperties.Buffer enumerateInstanceLayerProperties(MemoryStack stack) {
        IntBuffer layerCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateInstanceLayerProperties(layerCountPointer, null));

        VkLayerProperties.Buffer layers = VkLayerProperties.malloc(layerCountPointer.get(0), stack);
        throwIfFailed(vkEnumerateInstanceLayerProperties(layerCountPointer, layers));
        return layers;
    }

    static VkQueueFamilyProperties.Buffer getPhysicalDeviceQueueFamilyProperties(VkPhysicalDevice physicalDevice, MemoryStack stack) {
        IntBuffer queueFamilyCountPointer = stack.mallocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCountPointer, null);

        VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCountPointer.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCountPointer, queueFamilies);
        return queueFamilies;
    }

    static List<String> getAllGlfwExtensions() {
        PointerBuffer glfwExtensionsBuffer = glfwGetRequiredInstanceExtensions();
        if (glfwExtensionsBuffer == null) {
            throw new GLFWRuntimeException("No set of extensions allowing GLFW integration was found");
        }
        List<String> glfwExtensions = new ArrayList<>(glfwExtensionsBuffer.limit());
        for (int i = 0; i < glfwExtensionsBuffer.capacity(); i++) {
            glfwExtensions.add(i, memASCII(glfwExtensionsBuffer.get(i)));
        }
        return glfwExtensions;
    }

    static VkPhysicalDevice findFirstSuitablePhysicalDevice(VkInstance instance, long surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer physicalDevices = enumeratePhysicalDevices(instance, stack);
            if (physicalDevices.limit() == 0) {
                return null;
            }

            for (int i = 0; i < physicalDevices.limit(); i++) {
                VkPhysicalDevice physicalDevice = new VkPhysicalDevice(physicalDevices.get(i), instance);
                if (isSuitableDevice(physicalDevice, surface)) {
                    return physicalDevice;
                }
            }
        }
        return null;
    }

    static boolean isSuitableDevice(VkPhysicalDevice physicalDevice, long surface) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer surfaceFormatCountBuffer = stack.mallocInt(1);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, surfaceFormatCountBuffer, null);
            int surfaceFormatCount = surfaceFormatCountBuffer.get(0);

            IntBuffer presentationModeCountBuffer = stack.mallocInt(1);
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, presentationModeCountBuffer, null);
            int presentationModeCount = presentationModeCountBuffer.get(0);

            QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);

            return queueIndices.graphical() >= 0 &&
                   queueIndices.surfaceSupporting() >= 0 &&
                   deviceSupportsExtensions(physicalDevice, List.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME)) &&
                   surfaceFormatCount > 0 &&
                   presentationModeCount > 0;
        }
    }

    static boolean deviceSupportsExtensions(VkPhysicalDevice physicalDevice, List<String> expectedExtensionNames) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkExtensionProperties.Buffer availableExtensionsBuffer = enumerateDeviceExtensionProperties(physicalDevice, stack);
            for (String expectedExtensionName : expectedExtensionNames) {
                boolean isSupported = false;
                for (VkExtensionProperties availableExtensionProperty : availableExtensionsBuffer) {
                    if(availableExtensionProperty.extensionNameString().equals(expectedExtensionName)) {
                        isSupported = true;
                    }
                }
                if(!isSupported) {
                    return false;
                }
            }
            return true;
        }
    }

    static QueueIndices findQueueIndices(VkPhysicalDevice physicalDevice, long surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkQueueFamilyProperties.Buffer queueFamilies = getPhysicalDeviceQueueFamilyProperties(physicalDevice, stack);
            List<Integer> graphicalQueueIndices = new ArrayList<>(1);
            List<Integer> surfaceSupportingQueueIndices = new ArrayList<>(1);
            for (int i = 0; i < queueFamilies.queueCount(); i++) {
                VkQueueFamilyProperties queueFamily = queueFamilies.get(i);
                if (queueFamily.queueCount() <= 0) {
                    continue;
                }

                if (queueFamily.queueCount() > 0 && (queueFamily.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    graphicalQueueIndices.add(i);
                }

                IntBuffer supportSurfacePointer = stack.mallocInt(1);
                vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, surface, supportSurfacePointer);

                if(supportSurfacePointer.get(0) != 0) {
                    surfaceSupportingQueueIndices.add(i);
                }
            }
            return new QueueIndices(
                    graphicalQueueIndices.isEmpty() ? -1 : graphicalQueueIndices.getFirst(),
                    surfaceSupportingQueueIndices.isEmpty() ? -1 : surfaceSupportingQueueIndices.getFirst()
            );
        }
    }

    static VkInstance createInstance(String applicationName, boolean enableDebugging) {
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

            // Create flags list
            int flags = 0;

            // Extension list
            HashSet<String> requiredExtensions = new HashSet<>(getAllGlfwExtensions());
            HashSet<String> availableExtensions = enumerateInstanceExtensionProperties(stack).stream().map(VkExtensionProperties::extensionNameString).collect(Collectors.toCollection(HashSet::new));
            for (String requiredExtension : requiredExtensions) {
                if (!availableExtensions.contains(requiredExtension)) {
                    throw new RuntimeException("Unsupported extension: " + requiredExtension);
                }
            }
            // Required on macOS in later version of vulkan SDK
            if (availableExtensions.contains(KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME)) {
                requiredExtensions.add(KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME);
                flags |= KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
            }
            if (enableDebugging && availableExtensions.contains(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)) {
                requiredExtensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
            }
            PointerBuffer requiredExtensionsBuffer = stack.pointers(requiredExtensions.stream().map(stack::UTF8).toArray(ByteBuffer[]::new));

            // Validation layer list
            PointerBuffer requiredLayersBuffer = enableDebugging ? allocateAndValidateLayerList(List.of(
                    // This will not exist if you're using the bundled lwjgl MoltenVK lib.
                    // See README.md#Vulkan_validation_layers
                    "VK_LAYER_KHRONOS_validation"
            ), stack) : null;

            // Create instance
            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(flags)
                    .pApplicationInfo(applicationInfo)
                    .ppEnabledLayerNames(requiredLayersBuffer)
                    .ppEnabledExtensionNames(requiredExtensionsBuffer);

            PointerBuffer instancePointer = stack.mallocPointer(1);
            throwIfFailed(vkCreateInstance(instanceCreateInfo, null, instancePointer));
            return new VkInstance(instancePointer.get(0), instanceCreateInfo);
        }
    }

    public static long createSurface(VkInstance instance, long glfwWindowPointer) {
        try(MemoryStack stack = MemoryStack.stackPush()){
            LongBuffer surfacePointer = stack.mallocLong(1);
            throwIfFailed(glfwCreateWindowSurface(instance, glfwWindowPointer, null, surfacePointer));
            return surfacePointer.get(0);
        }
    }

    public static long createDebugMessenger(VkInstance instance) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .messageSeverity(
                            // VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                            // VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                    )
                    .messageType(
                            VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                    )
                    .pfnUserCallback(debugCallbackFunction)
                    .pUserData(NULL);
            LongBuffer callbackPointer = stack.mallocLong(1);
            vkCreateDebugUtilsMessengerEXT(instance, debugCreateInfo, null, callbackPointer);
            return callbackPointer.get(0);
        }
    }

    private static PointerBuffer allocateAndValidateLayerList(List<String> requiredLayerNames, MemoryStack stack) {
        VkLayerProperties.Buffer availableLayersBuffer = enumerateInstanceLayerProperties(stack);
        List<String> availableLayerNames = availableLayersBuffer.stream().map(VkLayerProperties::layerNameString).toList();
        for (String requiredLayerName : requiredLayerNames) {
            boolean found = false;
            for (String availableLayerName : availableLayerNames) {
                if (Objects.equals(availableLayerName, requiredLayerName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new RuntimeException("Requested layer name is not available: " + requiredLayerName);
            }
        }

        return stack.pointers(requiredLayerNames.stream().map(stack::UTF8).toArray(ByteBuffer[]::new));
    }

    public static VkDevice createLogicalDevice(VkPhysicalDevice physicalDevice, long surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);
            // Queues
            List<VkDeviceQueueCreateInfo> queueCreateInfos = new ArrayList<>(1);
            queueCreateInfos.add(VkDeviceQueueCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .queueFamilyIndex(queueIndices.graphical())
                    .pQueuePriorities(stack.floats(1.0f)));
            if (queueIndices.graphical() != queueIndices.surfaceSupporting()) {
                queueCreateInfos.add(VkDeviceQueueCreateInfo.malloc(stack)
                        .sType$Default()
                        .pNext(NULL)
                        .flags(0)
                        .queueFamilyIndex(queueIndices.surfaceSupporting())
                        .pQueuePriorities(stack.floats(1.0f)));
            }
            VkDeviceQueueCreateInfo.Buffer queueCreateInfosBuffer = VkDeviceQueueCreateInfo.malloc(queueCreateInfos.size(), stack);
            for (int i = 0; i < queueCreateInfos.size(); i++) {
                queueCreateInfosBuffer.put(i, queueCreateInfos.get(i));
            }

            // Features
            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);

            // Extensions
            VkExtensionProperties.Buffer availableExtensions = enumerateDeviceExtensionProperties(physicalDevice, stack);
            List<String> availableExtensionNames = availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .toList();
            List<String> requiredExtensionNames = new ArrayList<>();
            requiredExtensionNames.add(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
            for (String availableDeviceExtensionName : availableExtensionNames) {
                if (Objects.equals(availableDeviceExtensionName, "VK_KHR_portability_subset")) {
                    requiredExtensionNames.add("VK_KHR_portability_subset");
                }
            }
            PointerBuffer requiredExtensionNamesBuffer = stack.pointers(requiredExtensionNames.stream().map(stack::UTF8).toArray(ByteBuffer[]::new));

            // Create
            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pQueueCreateInfos(queueCreateInfosBuffer) // Also sets queueCreateInfoCount
                    .ppEnabledLayerNames(null)
                    .ppEnabledExtensionNames(requiredExtensionNamesBuffer) // Also sets enabledExtensionCount
                    .pEnabledFeatures(deviceFeatures);

            PointerBuffer logicalDevicePointer = stack.mallocPointer(1);
            throwIfFailed(vkCreateDevice(physicalDevice, deviceCreateInfo, null, logicalDevicePointer));

            return new VkDevice(logicalDevicePointer.get(0), physicalDevice, deviceCreateInfo);
        }
    }

    public static VkQueue findFirstQueueByFamily(VkDevice logicalDevice, int familyIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer queuePointer = stack.mallocPointer(1);
            vkGetDeviceQueue(logicalDevice, familyIndex, 0, queuePointer);
            return new VkQueue(queuePointer.get(0), logicalDevice);
        }
    }
}
