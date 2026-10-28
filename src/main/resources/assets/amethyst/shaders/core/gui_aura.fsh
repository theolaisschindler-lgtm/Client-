#version 330

in vec2 localPosition;
in vec4 accentColor;
flat in vec2 panelSize;
flat in float phase;

out vec4 fragColor;

void main() {
    vec2 p = localPosition - panelSize * 0.5;
    vec2 q = abs(p) - (panelSize * 0.5 - vec2(8.0));
    float distanceToPanel = length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - 8.0;
    float aa = max(fwidth(distanceToPanel), 0.5);
    float inside = 1.0 - smoothstep(-aa, aa, distanceToPanel);
    float outside = max(distanceToPanel, 0.0);
    float halo = exp(-outside / 6.0) * (1.0 - inside);
    float rim = exp(-abs(distanceToPanel) / 1.8);
    vec2 uv = localPosition / max(panelSize, vec2(1.0));
    float ribbon = 0.5 + 0.5 * sin(uv.x * 7.0 + sin(uv.y * 5.0 + phase) + phase);
    float sweep = 0.5 + 0.5 * cos(uv.y * 4.0 - uv.x * 3.0 + phase * 2.0);
    vec3 tint = mix(accentColor.rgb, min(accentColor.rgb + vec3(0.14, 0.08, 0.2), vec3(1.0)), ribbon);
    float wash = inside * (0.025 + ribbon * sweep * 0.065);
    float alpha = (halo * 0.30 + rim * (0.16 + 0.10 * sweep) + wash) * accentColor.a;
    alpha *= 1.0 - smoothstep(18.0, 24.0, outside);
    if (alpha < 0.001) discard;
    fragColor = vec4(tint, alpha);
}
