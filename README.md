# StereoCraft: Stereoscopic 3D

Side-by-side (SBS) stereoscopic 3D rendering for 3D TVs, 3D monitors, and SBS-compatible displays. StereoCraft splits the Minecraft window into left and right eye views with a configurable interpupillary distance (IPD), producing a true stereoscopic image you can pipe into any SBS-capable display or HMD.

## Requirements

- Minecraft **1.21.11**
- Fabric Loader **0.19.2+**
- Java **21**
- [Sodium](https://modrinth.com/mod/sodium) **0.8.11+** (hard dependency)

## Optional

- [Iris](https://modrinth.com/mod/iris) **1.10.7+1.21.11-fabric** for shader compatibility.
  - Tested against this exact Iris version. Other Iris versions may load but may silently degrade per-eye rendering — see *Known limitations* below.

## Installation

1. Install Fabric Loader 0.19.2+ for Minecraft 1.21.11.
2. Install [Sodium 0.8.11+](https://modrinth.com/mod/sodium) into your `mods/` folder. This is required.
3. (Optional) Install [Iris 1.10.7+1.21.11-fabric](https://modrinth.com/mod/iris) for shader support.
4. Download StereoCraft from [Modrinth](https://modrinth.com/) (or grab a release jar) and drop it into `mods/`.
5. Launch the game.

## Configuration

Open the in-game options menu and navigate to **Video Settings → Stereoscopic** (added by Sodium's options page integration).

- Toggle stereo mode on/off from this page. Toggling the mode triggers a renderer reload, so the screen may flash briefly.
- IPD and other per-eye settings are adjusted on the same page.

The config file lives at `config/stereoscopic-options.json`. Manual edits round-trip cleanly, but values changed in the in-game options page will overwrite the file on save.

## Known limitations

- **Cursor in split-screen GUIs:** the async-cursor present feature that mirrors the OS cursor into both eyes is **Windows-only**. On Linux and macOS the cursor will only appear in one eye while a screen (inventory, chat, menu, etc.) is open.
- **Iris compatibility:** only `1.10.7+1.21.11-fabric` is tested. Other Iris versions may load and silently degrade per-eye rendering. If you hit issues, please file a bug report including your exact Iris version.

## Screenshot

![StereoCraft in SBS mode](docs/screenshot.png)

## License

StereoCraft is licensed under **LGPL-3.0-only**. See [LICENSE](LICENSE) for the full license text.
