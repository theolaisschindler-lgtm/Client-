#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

out vec2 localPosition;
out vec4 accentColor;
flat out vec2 panelSize;
flat out float phase;

void main() {
    // GUI depth stays at zero; the quad's third coordinate carries animation time.
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    panelSize = vec2(UV2);
    localPosition = UV0 * (panelSize + vec2(48.0)) - vec2(24.0);
    accentColor = Color * ColorModulator;
    phase = Position.z;
}
