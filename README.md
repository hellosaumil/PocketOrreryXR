# Pocket Orrery XR 🌎🔭

Welcome to **Pocket Orrery XR**, an interactive 3D solar system experience built for **Android XR**.

This project holds a special place for me as it is my **very first Android application**, and naturally my first foray into the exciting world of **Spatial Computing** with Android XR.

## Demo

[![Watch the Full Demo Video](demo/demo_1080p.gif)](https://drive.google.com/file/d/1ip8ex6O7Irwo94Ih39Fd6Lcx7jpFMWaA/view?usp=drive_link)

> [!TIP]
> **Click the animated preview above** to watch the full high-resolution demo on Google Drive.

### Screenshots
| Initial View | Expanded Controls |
| :---: | :---: |
| <img src="demo/Default%20Entry.jpg" width="400"> | <img src="demo/Solar%20System%20Scaled%20Down%20+%20Menu%20Expanded.jpg" width="400"> |

---

## Overview

Pocket Orrery XR allows you to bring the solar system into your physical space. You can observe the planets in their orbits, scale the entire system to fit on your desk or fill your room, and learn interesting facts about each celestial body.

## Features

- **Spatial 3D Visualization**: High-fidelity 3D models of the Sun and all eight planets, rendered using the Jetpack XR SDK.
- **Interactive Orbit Simulation**: Watch the planets move in real-time, or pause the simulation to take a closer look.
- **Dynamic Scaling**: Use the UI slider to adjust the scale of the solar system from a miniature model to a room-filling experience.
- **Galaxy Background**: Toggle a beautiful Milky Way skybox to immerse yourself in deep space.
- **Planet Information**: Select any planet to see detailed information and educational facts.
- **Native XR Integration**: Built using Jetpack Compose and SceneCore, supporting Full Space mode for a truly immersive experience on Android XR.

## Getting Started

### Prerequisites

- Android Studio (Ladybug or newer recommended for XR support)
- Android XR Emulator or a compatible Android XR device

### Building the Project

1. Clone the repository:
   ```bash
   git clone https://github.com/hellosaumil/PocketOrreryXR.git
   ```
2. Open the project in Android Studio.
3. Sync Project with Gradle Files.
4. Run the `app` module on your Android XR device or emulator.

## Project Structure

- `app/src/main/java`: Contains the Kotlin source code, including the ViewModel, Compose UI, and 3D scene logic.
- `app/src/main/assets/models`: Contains the glTF models and textures for the planets and skybox.
- `tools/`: Utility Python scripts used for generating sphere geometry and managing assets.

## About the Author

Hi, I'm Saumil Shah! Pocket Orrery XR is not only my first step into XR but my **first-ever Android application**. The entire project was **vibecoded** using **Google Antigravity**. I created this app to explore the possibilities of spatial computing and to build something that combines my interests in technology and astronomy.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- High-quality planet textures sourced from [Solar System Scope](https://www.solarsystemscope.com/textures/).
- Built with the Google Jetpack XR SDK.
