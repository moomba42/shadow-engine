package com.alexdl.sdng.backend;

import org.joml.Vector3f;

public class Light {
    private Vector3f position;
    private Vector3f color;
    private float outerRadius;
    private float innerRadius;
    private float decaySpeed;

    public Light() {
        this.position = new Vector3f(0, 0, 0);
        this.color = new Vector3f(1.0f, 1.0f, 1.0f);
        this.outerRadius = 5;
        this.innerRadius = 0;
        this.decaySpeed = 0.1f;
    }

    public Light(Vector3f position, Vector3f color, float outerRadius, float innerRadius, float decaySpeed) {
        this.position = position;
        this.color = color;
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        this.decaySpeed = decaySpeed;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public Vector3f getColor() {
        return color;
    }

    public void setColor(Vector3f color) {
        this.color = color;
    }

    public float getOuterRadius() {
        return outerRadius;
    }

    public void setOuterRadius(float outerRadius) {
        this.outerRadius = outerRadius;
    }

    public float getInnerRadius() {
        return innerRadius;
    }

    public void setInnerRadius(float innerRadius) {
        this.innerRadius = innerRadius;
    }

    public float getDecaySpeed() {
        return decaySpeed;
    }

    public void setDecaySpeed(float decaySpeed) {
        this.decaySpeed = decaySpeed;
    }
}
