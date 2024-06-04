package com.alexdl.sdng.backend.vulkan.structs;

import static com.alexdl.sdng.backend.vulkan.structs.MemoryAlignment.*;
import static org.lwjgl.system.MemoryUtil.memPutFloat;
import static org.lwjgl.system.MemoryUtil.memPutInt;

public class EnvironmentDataStruct extends MemoryBlock<EnvironmentDataStruct> {
    /**
     * Aligned to 16 because:
     * - We already have an int (size 4B), so we cant start at 0
     * - A light struct's biggest component is a vec3
     * - vec3 and vec4 is aligned to 4N (4 * sizeof(float)).
     */
    private static final int LIGHT_OFFSET = 16;
    private static final int LIGHT_SIZE_BYTES_ALIGNED = 48;

    private static final MemoryAlignment alignmentLightCount = glslInt();
    private static final MemoryAlignment alignmentLightPosition = glslVec3();
    private static final MemoryAlignment alignmentLightColor = glslVec3();
    private static final MemoryAlignment alignmentLightOuterRadius = glslFloat();
    private static final MemoryAlignment alignmentLightInnerRadius = glslFloat();
    private static final MemoryAlignment alignmentLightDecaySpeed = glslFloat();
    private static final MemoryAlignment alignmentLight = align(16, alignmentLightPosition, alignmentLightColor, alignmentLightOuterRadius, alignmentLightInnerRadius, alignmentLightDecaySpeed);
    static {
        align(0, alignmentLightCount, alignmentLight);
    }

    private final int maxLightCount;

    public EnvironmentDataStruct(int maxLightCount) {
        super(LIGHT_OFFSET + (maxLightCount * LIGHT_SIZE_BYTES_ALIGNED));
        this.maxLightCount = maxLightCount;
    }

    public EnvironmentDataStruct(long address, int maxLightCount) {
        super(address, LIGHT_OFFSET + (maxLightCount * LIGHT_SIZE_BYTES_ALIGNED));
        this.maxLightCount = maxLightCount;
    }

    public void setLightCount(int lightCount) {
        assert lightCount >= 0 && lightCount < maxLightCount;
        memPutInt(address() + alignmentLightCount.getOffset(), lightCount);
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    public void setLight(int index, float positionX, float positionY, float positionZ, float colorR, float colorG, float colorB, float outerRadius, float innerRadius, float decaySpeed) {
        long address = address() + alignmentLight.getOffset() + ((long) index * alignmentLight.getAlignedSize());
        memPutFloat(address + alignmentLightPosition.getOffset() + (Float.BYTES * 0), positionX);
        memPutFloat(address + alignmentLightPosition.getOffset() + (Float.BYTES * 1), positionY);
        memPutFloat(address + alignmentLightPosition.getOffset() + (Float.BYTES * 2), positionZ);
        // vec3s are aligned to 4 floats, so we skip a byte here
        memPutFloat(address + alignmentLightColor.getOffset() + (Float.BYTES * 0), colorR);
        memPutFloat(address + alignmentLightColor.getOffset() + (Float.BYTES * 1), colorG);
        memPutFloat(address + alignmentLightColor.getOffset() + (Float.BYTES * 2), colorB);
        // vec3s are aligned to 4 floats, so we skip a byte here
        memPutFloat(address + alignmentLightOuterRadius.getOffset(), outerRadius);
        memPutFloat(address + alignmentLightInnerRadius.getOffset(), innerRadius);
        memPutFloat(address + alignmentLightDecaySpeed.getOffset(), decaySpeed);
    }

    @Override
    public EnvironmentDataStruct createAt(long address) {
        return new EnvironmentDataStruct(address, maxLightCount);
    }
}
