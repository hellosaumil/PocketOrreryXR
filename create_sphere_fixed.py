#!/usr/bin/env python3
"""
Generate a UV-mapped sphere glTF model for planets.
"""

import struct
import base64
import json
import math
import os

def generate_sphere_data(radius=0.5, width_segments=64, height_segments=32):
    """Generate vertices, normals, uvs, and indices for a sphere."""
    vertices = []
    normals = []
    uvs = []
    indices = []

    for y in range(height_segments + 1):
        v = y / height_segments
        angle_v = v * math.pi

        for x in range(width_segments + 1):
            u = x / width_segments
            angle_u = u * 2 * math.pi

            # Position
            # We want the poles on Y axis to match orbit rotation
            # -x to x for UV wrapping?
            px = -radius * math.cos(angle_u) * math.sin(angle_v)
            py = radius * math.cos(angle_v)
            pz = radius * math.sin(angle_u) * math.sin(angle_v)
            
            vertices.extend([px, py, pz])

            # Normal (normalized position)
            nx = px / radius
            ny = py / radius
            nz = pz / radius
            normals.extend([nx, ny, nz])

            # UVs
            # Reverting: 1.0 - u caused horizontal flip. Using u for standard mapping.
            uvs.extend([u, v])

    # Indices
    for y in range(height_segments):
        for x in range(width_segments):
            first = (y * (width_segments + 1)) + x
            second = first + width_segments + 1
            
            indices.extend([first, second, first + 1])
            indices.extend([second, second + 1, first + 1])

    return vertices, normals, uvs, indices

def create_gltf(output_file, texture_name):
    vertices, normals, uvs, indices = generate_sphere_data()

    # Pack data
    vertex_bytes = struct.pack(f'{len(vertices)}f', *vertices)
    normal_bytes = struct.pack(f'{len(normals)}f', *normals)
    uv_bytes = struct.pack(f'{len(uvs)}f', *uvs)
    index_bytes = struct.pack(f'{len(indices)}H', *indices)

    # Calculate offsets
    vertex_offset = 0
    vertex_len = len(vertex_bytes)
    normal_offset = vertex_offset + vertex_len
    normal_len = len(normal_bytes)
    uv_offset = normal_offset + normal_len
    uv_len = len(uv_bytes)
    index_offset = uv_offset + uv_len
    index_len = len(index_bytes)
    
    total_len = index_offset + index_len
    
    # Combine buffer
    buffer_data = vertex_bytes + normal_bytes + uv_bytes + index_bytes
    buffer_b64 = base64.b64encode(buffer_data).decode('utf-8')
    
    vertex_count = len(vertices) // 3
    index_count = len(indices)

    gltf = {
        "asset": {"version": "2.0", "generator": "PocketOrrery Sphere Fix"},
        "scene": 0,
        "scenes": [{"nodes": [0]}],
        "nodes": [{"mesh": 0, "name": "Sphere"}],
        "meshes": [{
            "name": "Sphere",
            "primitives": [{
                "attributes": {
                    "POSITION": 0,
                    "NORMAL": 1,
                    "TEXCOORD_0": 2
                },
                "indices": 3,
                "material": 0
            }]
        }],
        "materials": [{
            "name": "Default",
            "pbrMetallicRoughness": {
                "baseColorFactor": [1.0, 1.0, 1.0, 1.0],
                "metallicFactor": 0.0,
                "roughnessFactor": 0.9,
                "baseColorTexture": {"index": 0}
            },
            "emissiveFactor": [0.3, 0.3, 0.3], # Reduced from 0.8
            "emissiveTexture": {"index": 0}
        }],
        "textures": [{"source": 0}],
        "images": [{"uri": texture_name}], 
        "accessors": [
            { # POSITION
                "bufferView": 0,
                "byteOffset": 0,
                "componentType": 5126, # FLOAT
                "count": vertex_count,
                "type": "VEC3",
                "max": [0.5, 0.5, 0.5],
                "min": [-0.5, -0.5, -0.5]
            },
            { # NORMAL
                "bufferView": 1,
                "byteOffset": 0,
                "componentType": 5126, # FLOAT
                "count": vertex_count,
                "type": "VEC3"
            },
            { # TEXCOORD_0
                "bufferView": 2,
                "byteOffset": 0,
                "componentType": 5126, # FLOAT
                "count": vertex_count,
                "type": "VEC2"
            },
            { # INDICES
                "bufferView": 3,
                "byteOffset": 0,
                "componentType": 5123, # UNSIGNED_SHORT
                "count": index_count,
                "type": "SCALAR"
            }
        ],
        "bufferViews": [
            {"buffer": 0, "byteOffset": vertex_offset, "byteLength": vertex_len, "target": 34962},
            {"buffer": 0, "byteOffset": normal_offset, "byteLength": normal_len, "target": 34962},
            {"buffer": 0, "byteOffset": uv_offset, "byteLength": uv_len, "target": 34962},
            {"buffer": 0, "byteOffset": index_offset, "byteLength": index_len, "target": 34963}
        ],
        "buffers": [{
            "byteLength": total_len,
            "uri": f"data:application/octet-stream;base64,{buffer_b64}"
        }]
    }
    
    with open(output_file, 'w') as f:
        json.dump(gltf, f, indent=2)
    print(f"Generated {output_file}")

if __name__ == "__main__":
    planets = [
        ("earth", "earth_texture.jpg"),
        ("mars", "mars_texture.jpg"),
        ("mercury", "mercury_texture.jpg"),
        ("venus", "venus_texture.jpg"),
        ("jupiter", "jupiter_texture.jpg"),
        ("saturn", "saturn_texture.jpg"),
        ("uranus", "uranus_texture.jpg"),
        ("neptune", "neptune_texture.jpg"),
        ("sun", "sun_texture.jpg")
    ]
    
    base_dir = "app/src/main/assets/models/"
    
    # Run for all bodies
    for name, texture in planets:
        print(f"Processing {name}...")
        create_gltf(os.path.join(base_dir, f"{name}.gltf"), texture)
