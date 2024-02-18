package com.alexdl.shadowhaven.engine;


import com.formdev.flatlaf.FlatDarkLaf;
import org.lwjgl.PointerBuffer;
import org.lwjgl.awt.AWT;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.awt.AWTVK;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static com.alexdl.shadowhaven.engine.vulkan.VulkanUtils.translateVulkanResult;
import static org.lwjgl.vulkan.KHRSurface.VK_KHR_SURFACE_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanAWTTest {
    private static VkInstance createInstance() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo
                    .calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8("AWT Vulkan Demo"))
                    .pEngineName(stack.UTF8(""))
                    .apiVersion(VK_MAKE_VERSION(1, 0, 2));

            PointerBuffer ppEnabledExtensionNames = stack.pointers(stack.UTF8(VK_KHR_SURFACE_EXTENSION_NAME), stack.UTF8(AWTVK.getSurfaceExtensionName()));

            VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo
                    .calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(ppEnabledExtensionNames);

            PointerBuffer pInstance = stack.mallocPointer(1);
            int err = vkCreateInstance(pCreateInfo, null, pInstance);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to create VkInstance: " + translateVulkanResult(err));
            }

            long instance = pInstance.get(0);
            return new VkInstance(instance, pCreateInfo);

        }
    }
    public static void main(String[] args) throws AWTException {
        if (!AWT.isPlatformSupported()) {
            throw new RuntimeException("Platform not supported.");
        }
        System.setProperty("apple.awt.application.appearance", "system");
        FlatDarkLaf.setup();

        VkInstance instance = createInstance();

        JFrame frame = new JFrame("Editor");
        frame.setBackground(Color.black);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        Canvas canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(600, 600));

        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.add(new JButton("Some button"));
        options.add(new JTextField("Input something!"));

        JPanel panel = new JPanel();
        panel.add(options);
        panel.add(canvas);

        frame.add(panel);
        frame.pack();
        long surface = AWTVK.create(canvas, instance);

        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);

                // Destroy the surface to prevent leaks and errors
                KHRSurface.vkDestroySurfaceKHR(instance, surface, null);
            }
        });
    }
}
