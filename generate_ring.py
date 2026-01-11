#!/usr/bin/env python3
"""Generate a simple ring/torus glTF model for orbit visualization."""

import struct
import base64
import json
import math

def generate_torus_vertices(major_radius=1.0, minor_radius=0.002, major_segments=64, minor_segments=6):
    """Generate vertices and indices for a torus."""
    vertices = []
    normals = []
    indices = []
    
    for i in range(major_segments):
        theta = 2.0 * math.pi * i / major_segments
        cos_theta = math.cos(theta)
        sin_theta = math.sin(theta)
        
        for j in range(minor_segments):
            phi = 2.0 * math.pi * j / minor_segments
            cos_phi = math.cos(phi)
            sin_phi = math.sin(phi)
            
            # Position
            x = (major_radius + minor_radius * cos_phi) * cos_theta
            y = minor_radius * sin_phi
            z = (major_radius + minor_radius * cos_phi) * sin_theta
            vertices.extend([x, y, z])
            
            # Normal
            nx = cos_phi * cos_theta
            ny = sin_phi
            nz = cos_phi * sin_theta
            normals.extend([nx, ny, nz])
    
    # Generate indices
    for i in range(major_segments):
        for j in range(minor_segments):
            i_next = (i + 1) % major_segments
            j_next = (j + 1) % minor_segments
            
            v0 = i * minor_segments + j
            v1 = i_next * minor_segments + j
            v2 = i_next * minor_segments + j_next
            v3 = i * minor_segments + j_next
            
            # Two triangles per quad
            indices.extend([v0, v1, v2])
            indices.extend([v0, v2, v3])
    
    return vertices, normals, indices

def create_gltf():
    vertices, normals, indices = generate_torus_vertices()
    
    # Pack data into binary
    vertex_data = struct.pack(f'{len(vertices)}f', *vertices)
    normal_data = struct.pack(f'{len(normals)}f', *normals)
    index_data = struct.pack(f'{len(indices)}H', *indices)
    
    # Combine all buffers
    buffer_data = vertex_data + normal_data + index_data
    buffer_base64 = base64.b64encode(buffer_data).decode('utf-8')
    
    vertex_count = len(vertices) // 3
    index_count = len(indices)
    
    # Calculate bounds
    xs = vertices[0::3]
    ys = vertices[1::3]
    zs = vertices[2::3]
    
    gltf = {
        "asset": {"version": "2.0", "generator": "PocketOrrery Ring Generator"},
        "scene": 0,
        "scenes": [{"nodes": [0]}],
        "nodes": [{"mesh": 0}],
        "meshes": [{
            "primitives": [{
                "attributes": {"POSITION": 0, "NORMAL": 1},
                "indices": 2,
                "material": 0
            }]
        }],
        "materials": [{
            "name": "OrbitRingMaterial",
            "pbrMetallicRoughness": {
                "baseColorFactor": [0.7, 0.7, 0.8, 0.02],
                "metallicFactor": 0.0,
                "roughnessFactor": 0.9
            },
            "emissiveFactor": [0.2, 0.2, 0.25],
            "alphaMode": "BLEND"
        }],
        "accessors": [
            {
                "bufferView": 0,
                "componentType": 5126,  # FLOAT
                "count": vertex_count,
                "type": "VEC3",
                "min": [min(xs), min(ys), min(zs)],
                "max": [max(xs), max(ys), max(zs)]
            },
            {
                "bufferView": 1,
                "componentType": 5126,  # FLOAT
                "count": vertex_count,
                "type": "VEC3"
            },
            {
                "bufferView": 2,
                "componentType": 5123,  # UNSIGNED_SHORT
                "count": index_count,
                "type": "SCALAR"
            }
        ],
        "bufferViews": [
            {"buffer": 0, "byteOffset": 0, "byteLength": len(vertex_data)},
            {"buffer": 0, "byteOffset": len(vertex_data), "byteLength": len(normal_data)},
            {"buffer": 0, "byteOffset": len(vertex_data) + len(normal_data), "byteLength": len(index_data)}
        ],
        "buffers": [{
            "byteLength": len(buffer_data),
            "uri": f"data:application/octet-stream;base64,{buffer_base64}"
        }]
    }
    
    return gltf

if __name__ == "__main__":
    gltf = create_gltf()
    output_path = "app/src/main/assets/models/ring.gltf"
    with open(output_path, 'w') as f:
        json.dump(gltf, f, indent=2)
    print(f"Created {output_path}")
