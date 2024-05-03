#version 450

layout(location = 0) in vec3 in_color;
layout(location = 1) in vec2 in_uv;

layout(set = 1, binding = 0) uniform sampler2D textureSampler;

layout(location = 0) out vec4 out_color;

void main() {
    out_color = texture(textureSampler, in_uv) * vec4(in_color, 1.0);
}
