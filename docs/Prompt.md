I just wrote a design spec for a new Fabric mod called "Stereoscopic" — a side-by-side
stereoscopic 3D renderer for Minecraft 1.21.11 / Fabric Loader 0.19.0.

The spec lives at:
  docs/superpowers/specs/2026-05-13-stereoscopic-fabric-1.21-design.md
(committed as b5ab484 on main — this repo's first commit)

Reference implementation (Forge 1.7.10) lives in a sibling repo:
  C:\code\Angelica  on branch stereo-sbs  (base: upstream/releases/2.8.x)
  Browse the SBS commits with:
    git -C C:\code\Angelica log --oneline upstream/releases/2.8.x..stereo-sbs

Please:
  1. Read the spec end-to-end.
  2. Invoke the superpowers:writing-plans skill to produce an implementation plan
      under docs/superpowers/plans/.
  3. After the plan is written and I've reviewed it, scaffold the Fabric mod
      (Gradle/Loom, fabric.mod.json, source tree, mixin configs) per the spec.

Test environment for the deploy hook:
  C:\Users\felix\AppData\Roaming\ModrinthApp\profiles\Fall 2025 Let_s Play\mods
