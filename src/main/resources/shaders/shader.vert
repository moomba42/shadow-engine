#version 450

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 color;

layout(binding = 0) uniform Scene {
    mat4 projection;
    mat4 view;
} scene;

layout(binding = 1) uniform Model {
    mat4 model;
} model;

layout(location = 0) out vec3 out_color;

void main() {
    out_color = color;
    gl_Position = scene.projection * scene.view * model.model * vec4(position, 1.0);
}


