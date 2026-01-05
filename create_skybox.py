import json
import struct
import math
import os

def create_sphere(radius=500.0, rings=64, sectors=64):
    # Generates Sphere Geometry
    # Returns (positions, normals, uvs, indices) as flat lists
    positions = []
    normals = []
    uvs = []
    indices = []

    R = 1.0 / (rings - 1)
    S = 1.0 / (sectors - 1)

    for r in range(rings):
        for s in range(sectors):
            y = math.sin(-math.pi/2 + math.pi * r * R)
            x = math.cos(2 * math.pi * s * S) * math.sin(math.pi * r * R)
            z = math.sin(2 * math.pi * s * S) * math.sin(math.pi * r * R)

            # Positions
            positions.extend([x * radius, y * radius, z * radius])
            # Normals (same as pos for unit sphere) - Inverted for inside view
            normals.extend([-x, -y, -z])
            # UVs
            uvs.extend([1.0 - (s * S), 1.0 - (r * R)])
            
    for r in range(rings - 1):
        for s in range(sectors - 1):
            i0 = r * sectors + s
            i1 = r * sectors + (s + 1)
            i2 = (r + 1) * sectors + (s + 1)
            i3 = (r + 1) * sectors + s
            
            # Invert winding order for inside rendering
            # Standard: i0, i1, i2 (CCW)
            # Inverted: i0, i2, i1 (CW viewed from outside, CCW from inside)
            
            indices.extend([i0, i2, i1])
            indices.extend([i2, i0, i3]) 
            
    return positions, normals, uvs, indices

def write_bin(filename, positions, normals, uvs, indices):
    with open(filename, 'wb') as f:
        # Positions (Vec3 float)
        pos_offset = 0
        for val in positions:
            f.write(struct.pack('f', val))
        pos_len = len(positions) * 4
        
        # Normals (Vec3 float)
        norm_offset = f.tell()
        for val in normals:
            f.write(struct.pack('f', val))
        norm_len = len(normals) * 4
        
        # UVs (Vec2 float)
        uv_offset = f.tell()
        for val in uvs:
            f.write(struct.pack('f', val))
        uv_len = len(uvs) * 4
        
        # Indices (Scalar UShort)
        ind_offset = f.tell()
        for val in indices:
            f.write(struct.pack('H', val)) # unsigned short
        ind_len = len(indices) * 2
        
        return {
            "pos": (pos_offset, pos_len),
            "norm": (norm_offset, norm_len),
            "uv": (uv_offset, uv_len),
            "ind": (ind_offset, ind_len),
            "total": f.tell(),
            "count": len(indices),
            "vertex_count": len(positions) // 3
        }

def create_gltf(bin_filename, offsets, texture_filename):
    min_pos = [-50.0, -50.0, -50.0]
    max_pos = [50.0, 50.0, 50.0]
    
    return {
        "asset": { "version": "2.0", "generator": "Python Skybox Gen" },
        "extensionsUsed": ["KHR_materials_unlit"],
        "extensionsRequired": ["KHR_materials_unlit"],
        "scene": 0,
        "scenes": [{ "nodes": [0] }],
        "nodes": [{ "mesh": 0, "name": "Skybox" }],
        "meshes": [{
            "primitives": [{
                "attributes": {
                    "POSITION": 0,
                    "NORMAL": 1,
                    "TEXCOORD_0": 2
                },
                "indices": 3,
                "material": 0
            }],
            "name": "Skybox"
        }],
        "materials": [{
            "pbrMetallicRoughness": {
                "baseColorFactor": [1,1,1,1],
                "metallicFactor": 0,
                "roughnessFactor": 1,
                "baseColorTexture": { "index": 0 }
            },
            "extensions": {
                "KHR_materials_unlit": {}
            },
            "doubleSided": True,
            "name": "SkyboxMaterial"
        }],
        "textures": [{ "source": 0 }],
        "images": [{ "uri": texture_filename }],
        "accessors": [
            { "bufferView": 0, "byteOffset": 0, "componentType": 5126, "count": offsets["vertex_count"], "type": "VEC3", "max": max_pos, "min": min_pos },
            { "bufferView": 1, "byteOffset": 0, "componentType": 5126, "count": offsets["vertex_count"], "type": "VEC3" },
            { "bufferView": 2, "byteOffset": 0, "componentType": 5126, "count": offsets["vertex_count"], "type": "VEC2" },
            { "bufferView": 3, "byteOffset": 0, "componentType": 5123, "count": offsets["count"], "type": "SCALAR" }
        ],
        "bufferViews": [
            { "buffer": 0, "byteOffset": 0, "byteLength": offsets["pos"][1], "target": 34962 },
            { "buffer": 0, "byteOffset": offsets["pos"][1], "byteLength": offsets["norm"][1], "target": 34962 },
            { "buffer": 0, "byteOffset": offsets["pos"][1] + offsets["norm"][1], "byteLength": offsets["uv"][1], "target": 34962 },
            { "buffer": 0, "byteOffset": offsets["pos"][1] + offsets["norm"][1] + offsets["uv"][1], "byteLength": offsets["ind"][1], "target": 34963 }
        ],
        "buffers": [{ "uri": bin_filename, "byteLength": offsets["total"] }]
    }

if __name__ == "__main__":
    models_dir = "app/src/main/assets/models"
    bin_name = "milky_way.bin"
    gltf_name = "milky_way.gltf"
    texture_name = "milky_way_texture.jpg"

    print("Generating skybox geometry...")
    pos, norm, uv, ind = create_sphere(radius=50.0, rings=64, sectors=64) 

    print("Writing binary...")
    offsets = write_bin(os.path.join(models_dir, bin_name), pos, norm, uv, ind)

    print("Writing glTF...")
    gltf = create_gltf(bin_name, offsets, texture_name)
    with open(os.path.join(models_dir, gltf_name), 'w') as f:
        json.dump(gltf, f, indent=4)
        
    print("Done! Created milky_way.gltf and milky_way.bin")
