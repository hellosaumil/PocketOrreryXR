import json
import os
import base64

models_dir = "app/src/main/assets/models"
planets = ["mercury", "venus", "earth", "mars", "jupiter", "saturn", "uranus", "neptune", "sun"]

# Configuration
EMBED_GEOMETRY = True  # Embed .bin data into .gltf
EXTERNAL_TEXTURES = True # Keep .jpg files external
ADD_SUN_LIGHT = True   # Add KHR_lights_punctual to Sun

def run():
    # 1. Load Geometry (if needed for embedding)
    # If the user deleted the glTFs but kept sphere.bin, we can reload.
    # Ideally we expect sphere.bin OR ref_sphere.gltf to exist if we are starting fresh.
    # But if we are maintaining, we assume the glTF exists.
    
    bin_path = os.path.join(models_dir, "sphere.bin")
    bin_data_uri = None
    if os.path.exists(bin_path):
        with open(bin_path, 'rb') as f:
            bin_data = f.read()
        b64_data = base64.b64encode(bin_data).decode('utf-8')
        bin_data_uri = f"data:application/octet-stream;base64,{b64_data}"
    
    for p in planets:
        gltf_path = os.path.join(models_dir, f"{p}.gltf")
        if not os.path.exists(gltf_path):
            print(f"Skipping {p}, not found")
            continue

        with open(gltf_path, 'r') as f:
            gltf = json.load(f)

        # A. Embed Geometry
        if EMBED_GEOMETRY:
            # Check if buffer is already embedded
            uri = gltf["buffers"][0].get("uri", "")
            if bin_data_uri: # Always update if we have new bin data
                # Replace with embedded data
                gltf["buffers"][0]["uri"] = bin_data_uri
                gltf["buffers"][0]["byteLength"] = len(base64.b64decode(bin_data_uri.split(",")[1]))
                print(f"Embedded geometry in {p}.gltf")
            elif not uri.startswith("data:") and not bin_data_uri:
                print(f"Warning: Could not embed geometry in {p}, sphere.bin missing")

        # B. Externalize Textures
        if EXTERNAL_TEXTURES:
            if "images" in gltf:
                for img in gltf["images"]:
                    uri = img.get("uri", "")
                    if uri.startswith("data:"):
                        # Extract Base64
                        try:
                            header, encoded = uri.split(",", 1)
                            data = base64.b64decode(encoded)
                            
                            ext = "png" if "png" in header else "jpg"
                            tex_filename = f"{p}_texture.{ext}"
                            tex_path = os.path.join(models_dir, tex_filename)
                            
                            with open(tex_path, 'wb') as tf:
                                tf.write(data)
                            
                            img["uri"] = tex_filename
                            if "mimeType" in img: del img["mimeType"]
                            if "bufferView" in img: del img["bufferView"]
                            print(f"Externalized texture to {tex_filename}")
                        except Exception as e:
                            print(f"Failed to extract texture for {p}: {e}")

        # C. Add Sun Light
        if p == "sun" and ADD_SUN_LIGHT:
            # Add Extension Decl
            if "extensionsUsed" not in gltf: gltf["extensionsUsed"] = []
            if "KHR_lights_punctual" not in gltf["extensionsUsed"]:
                gltf["extensionsUsed"].append("KHR_lights_punctual")
            
            # Light Def
            light_def = {
                "type": "point",
                "color": [1.0, 1.0, 1.0],
                "intensity": 2000.0, 
                "range": 100.0 
            }
            
            if "extensions" not in gltf: gltf["extensions"] = {}
            gltf["extensions"]["KHR_lights_punctual"] = { "lights": [light_def] }
            
            # Attach to Node 0
            node = gltf["nodes"][0]
            if "extensions" not in node: node["extensions"] = {}
            node["extensions"]["KHR_lights_punctual"] = { "light": 0 }
            print("Ensured Light on Sun")

        with open(gltf_path, 'w') as f:
            json.dump(gltf, f, indent=4)

if __name__ == "__main__":
    run()
