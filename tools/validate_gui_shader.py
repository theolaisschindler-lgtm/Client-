"""Compile and exercise the shipped GLSL using a standalone OpenGL 3.3+ EGL context.

Optional development check: python -m pip install moderngl==5.12.0
Then: python tools/validate_gui_shader.py --preview /tmp/amethyst-aura.png
This exercises the actual shaders, not a Minecraft client or the complete GUI.
"""
import argparse
import json
from pathlib import Path
import struct
import zlib

import moderngl


def png(path, pixels, width, height):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    rows = [pixels[y * width * 4:(y + 1) * width * 4] for y in range(height)]
    raw = b"".join(b"\0" + row for row in reversed(rows))
    Path(path).write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">2I5B", width, height, 8, 6, 0, 0, 0))
                           + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1] / "src/main/resources/assets/amethyst/shaders/core"
    ctx = moderngl.create_standalone_context(require=330, backend="egl")
    program = ctx.program(vertex_shader=(root / "gui_aura.vsh").read_text(),
                          fragment_shader=(root / "gui_aura.fsh").read_text())
    identity = [1., 0., 0., 0., 0., 1., 0., 0., 0., 0., 1., 0., 0., 0., 0., 1.]
    transforms = ctx.buffer(struct.pack("40f", *(identity + [1., 1., 1., 1.] + [0.] * 4 + identity)))
    projection = ctx.buffer(reserve=64)
    program["DynamicTransforms"].binding = 0
    program["Projection"].binding = 1
    transforms.bind_to_uniform_block(0)
    projection.bind_to_uniform_block(1)
    vertices = ctx.buffer(reserve=28 * 4)
    indices = ctx.buffer(struct.pack("6I", 0, 1, 2, 0, 2, 3))
    # Match Minecraft POSITION_COLOR_TEX_LIGHTMAP: float position, normalized
    # unsigned-byte color, float UV, unsigned-short integer UV2 (panel dimensions).
    quad = ctx.vertex_array(program, [(vertices, "3f 4f1 2f 2u2", "Position", "Color", "UV0", "UV2")], indices)
    ctx.enable(moderngl.BLEND)
    ctx.blend_func = moderngl.SRC_ALPHA, moderngl.ONE_MINUS_SRC_ALPHA
    ctx.disable(moderngl.CULL_FACE | moderngl.DEPTH_TEST)
    checks = ["GLSL compile and link"]

    for width, height in [(870, 550), (304, 164)]:
        fw, fh = width + 48, height + 48
        texture = ctx.texture((fw, fh), 4)
        target = ctx.framebuffer(color_attachments=[texture])
        target.use()
        ctx.viewport = (0, 0, fw, fh)
        projection.write(struct.pack("16f", 2 / fw, 0, 0, 0, 0, -2 / fh, 0, 0, 0, 0, -1, 0, -1, 1, 0, 1))

        def draw(phase, intensity, accent=(155, 124, 255)):
            data = bytearray()
            for x, y, u, v in [(0, 0, 0, 0), (0, fh, 0, 1), (fw, fh, 1, 1), (fw, 0, 1, 0)]:
                data += struct.pack("<3f4B2f2H", x, y, phase, *accent, round(intensity * 255), u, v, width, height)
            vertices.write(data)
            target.clear(17 / 255, 16 / 255, 22 / 255, 1)
            quad.render(moderngl.TRIANGLES)
            return target.read(components=4, alignment=1)

        off = draw(0, 0)
        still = draw(0, 0.6)
        animated = draw(1, 0.6)
        assert off == bytes((17, 16, 22, 255)) * (fw * fh), "Zero intensity must draw nothing"
        assert still != off, "Enabled glow must change the framebuffer"
        assert animated != still, "Animation phase must change the shader output"
        assert still == draw(0, 0.6), "Frozen animation must be stable across frames"
        assert still != draw(0, 0.6, (255, 160, 80)), "The selected accent must affect the shader"
        assert still[:4] == off[:4], "Halo must fade out at its padded corners"
        checks.append(f"{width}x{height}: intensity, animation, freeze, accent, bounded halo")
        if args.preview and width == 870:
            png(args.preview, animated, fw, fh)
        target.release()
        texture.release()

    print(json.dumps({"renderer": ctx.info["GL_RENDERER"], "checks": checks, "passed": True}))
    quad.release()
    indices.release()
    vertices.release()
    transforms.release()
    projection.release()
    program.release()
    ctx.release()


if __name__ == "__main__":
    main()
