# AI contributions

Parts of this mod were written with the help of **Claude** (Anthropic), working as a coding assistant in Claude Code at the
direction of the mod's author. The author decided what the mod does and reviewed the work. This file only credits the
tooling; it says nothing about the mod's content.

## What Claude helped with

- Fabric setup: item and block registration, ID/registry-key classes, creative tab entry.
- Resource-format fixes for the 26.x item format (item definitions, models, lang).
- A flat, wire-style block with straight, diagonal and stepped connections, and the code behind its placement and shape.
- A settings class for the block (tint colour, diagonal linking, sounds) and code that tints grayscale textures at render time.
- A script (`tools/GenerateDustAssets.ps1`) that procedurally generates the grayscale textures in two resolutions and the related model files.
- A client config option for high-res or low-res textures, with an optional config screen (YetAnotherConfigLib, Mod Menu).
- Redstone-style behaviour for that block, including diagonal links and inputs, through small hooks into vanilla's signal handling.
- A custom entity type with simple AI and a 2D billboard renderer with a live, code-generated texture effect. The entity's artwork is the author's own.
- Small mixin hooks into vanilla's game-mode and command handling.
- Server-side events and a small settings file for the entity, and a launch-time content-warning screen.
- Additional entity types sharing that renderer, collectible items with hover text, and scripts that draw the flat placeholder art for them. The main artwork is the author's own.
- Build configuration for those libraries, and a development setup for hot-reloading code.

## What this file is not

It is not documentation of the mod's design, story or any hidden content. Please see `FAIR_PLAY.md`.
