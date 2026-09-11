# Safari Group Helper

A Fabric client mod for Hypixel SkyBlock that tracks the **unique critters** your group catches
during a single Critter Safari run.

Targets **Minecraft 26.1.x** (built against 26.1.2) and needs Fabric Loader 0.19.3+, Fabric API
and Fabric Language Kotlin. ModMenu is optional.

`hypixel-mod-api` is **strongly recommended**: it tells the mod exactly where you are
(`mode=safari` / `mode=foraging_3`). Without it the mod falls back to the `⏣ <area>` line of the
sidebar plus the chat message you get when entering the safari.

## Features

* **Pick your biome** - Cavern / Forest / Haunted / Icy. Change it by clicking the
  `[Switch biome]` label in the HUD or with `/sgh biome <cavern|forest|haunted|icy>`.
  The button only shows up **while an inventory or chest is open**, so it cannot be hit by
  accident; where it is available at all (Safari only / Safari + Torrhus Canyon / everywhere)
  is configurable.
* **Critter list for your biome** with ✔/✖ marks and a repeat counter (`x3`).
* **Progress of the other biomes** in a single block: off / just `3/9` / full critter list.
* **Overall progress** from 0 to 100% with a bar (37 critters in total).
* **Run timer** plus catch and repeat counts, with a `[Reset run]` button that asks for a
  second click before it throws the run away.
* **Personal best**: on 100% across all biomes the mod prints your run time in chat
  (client side only, nothing is sent to the server) and stores your best result.
* **Draggable HUD blocks** - `/sgh gui`, drag with the mouse, scroll to resize,
  right click to hide a block, `R` resets every position.
* **Progress is only shown inside the Critter Safari**, and chat is only parsed there.
* **Settings with categories and descriptions**, from ModMenu or `/sgh settings`.

## Commands

| Command | What it does |
| --- | --- |
| `/sgh` / `/sgh settings` | Settings screen |
| `/sgh gui` | HUD position editor |
| `/sgh biome` | Biome picker screen |
| `/sgh biome forest` | Pick a biome directly |
| `/sgh others off\|compact\|full` | Display mode of the "other biomes" block |
| `/sgh status` | Print the current progress in chat |
| `/sgh dump` | Print what the mod sees about your location (for debugging) |
| `/sgh reset` | Reset the progress of the current run |
| `/sgh debug` | Toggle chat parsing debug output |

## Bundled software

The settings screen is built on [MoulConfig](https://github.com/NotEnoughUpdates/MoulConfig),
which is bundled into the jar under a relocated package so it cannot clash with other mods'
copies. MoulConfig is licensed under the MPL 2.0; its licence travels with the jar in
`licenses/MoulConfig-MPL-2.0.txt`. Everything else is MIT, see `LICENSE`.

## Something broken?

Do not just describe it - **grab the log and send it to me straight away**, otherwise there is
nothing to go on.

Discord: **soldey**

What to send:

1. `logs/latest.log` from your game folder. In the Modrinth App: right click the instance ->
   `Open folder` -> `logs/latest.log`. Attach the file itself, not a screenshot.
2. The output of `/sgh dump` - it shows what the mod thinks about your location.
3. If catches are not being counted: turn on `/sgh debug`, play one run, then also send
   `config/safarigrouphelper/chat-debug.log`. It contains every chat line the mod saw inside
   the safari, which is exactly what is needed to fix the parsing.

Send the log right after the problem happens: `latest.log` is overwritten every time the game
starts.
