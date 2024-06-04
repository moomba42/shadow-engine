package com.alexdl.sdng.backend.vulkan.structs;

import static com.alexdl.sdng.backend.vulkan.structs.MemoryAlignment.*;
import static org.lwjgl.system.MemoryUtil.memPutFloat;
import static org.lwjgl.system.MemoryUtil.memPutInt;

public class EnvironmentUboDataStruct extends MemoryBlock<EnvironmentUboDataStruct> {
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

    public EnvironmentUboDataStruct(int maxLightCount) {
        super(alignmentLight.getOffset() + (maxLightCount * alignmentLight.getAlignedSize()));
        this.maxLightCount = maxLightCount;
    }

    public EnvironmentUboDataStruct(long address, int maxLightCount) {
        super(address, alignmentLight.getOffset() + (maxLightCount * alignmentLight.getAlignedSize()));
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
        memPutFloat(address + alignmentLightColor.getOffset() + (Float.BYTES * 0), colorR);
        memPutFloat(address + alignmentLightColor.getOffset() + (Float.BYTES * 1), colorG);
        memPutFloat(address + alignmentLightColor.getOffset() + (Float.BYTES * 2), colorB);
        memPutFloat(address + alignmentLightOuterRadius.getOffset(), outerRadius);
        memPutFloat(address + alignmentLightInnerRadius.getOffset(), innerRadius);
        memPutFloat(address + alignmentLightDecaySpeed.getOffset(), decaySpeed);
    }

    @Override
    public EnvironmentUboDataStruct createAt(long address) {
        return new EnvironmentUboDataStruct(address, maxLightCount);
    }
}
