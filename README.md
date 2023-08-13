<p align="center">
  <img src="https://github.com/CinemaMod/mcef/assets/30220598/938896d7-2589-49df-8f82-29266c64dfb7" alt="MCEF Logo" style="width:66px;height:66px;">
</p>

# MCEF (Minecraft Chromium Embedded Framework)
MCEF is a mod and library for adding the Chromium web browser into Minecraft.

MCEF is based on java-cef (Java Chromium Embedded Framework), which is based on CEF (Chromium Embedded Framework), which is based on Chromium. It was originally created by montoyo. It was rewritten and currently maintained by the CinemaMod Group.

MCEF contains a downloader system for downloading the java-cef & CEF binaries required by the Chromium browser. This requires a connection to https://mcef-download.cinemamod.com.

Discussion: https://discord.gg/rNrh5kW8Ty

## Supported Platforms
- Windows 10/11 (x86_64, arm64)
- macOS 11 or greater (Intel, Apple Silicon)
- GNU Linux glibc 2.31 or greater (x86_64, arm64)*

*This mod will not work on Android.

## For Players
This is the source code for MCEF. You can find the download for the mod on CurseForge.

TODO: LINK.

## For Modders
MCEF is LGPL, as long as your project doesn't modify or include MCEF source code, you can choose a different license. Read the full license in the LICENSE file in this directory.

The submodule for the modified version of java-cef which MCEF relies on is located in common/java-cef.

TODO: Release maven artifacts
