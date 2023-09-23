/*
 *     MCEF (Minecraft Chromium Embedded Framework)
 *     Copyright (C) 2023 CinemaMod Group
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */

package com.cinemamod.mcef;

public enum MCEFPlatform {
    LINUX_AMD64,
    LINUX_ARM64,
    WINDOWS_AMD64,
    WINDOWS_ARM64,
    MACOS_AMD64,
    MACOS_ARM64;

    public String getNormalizedName() {
        return name().toLowerCase();
    }

    public boolean isLinux() {
        return (this == LINUX_AMD64 || this == LINUX_ARM64);
    }

    public boolean isWindows() {
        return (this == WINDOWS_AMD64 || this == WINDOWS_ARM64);
    }

    public boolean isMacOS() {
        return (this == MACOS_AMD64 || this == MACOS_ARM64);
    }

    public static MCEFPlatform getPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (os.startsWith("linux")) {
            if (arch.equals("amd64")) {
                return LINUX_AMD64;
            } else if (arch.equals("aarch64")) {
                return LINUX_ARM64;
            }
        } else if (os.startsWith("windows")) {
            if (arch.equals("amd64")) {
                return WINDOWS_AMD64;
            } else if (arch.equals("aarch64")) {
                return WINDOWS_ARM64;
            }
        } else if (os.startsWith("mac os x")) {
            if (arch.equals("x86_64")) {
                return MACOS_AMD64;
            } else if (arch.equals("aarch64")) {
                return MACOS_ARM64;
            }
        }

        throw new RuntimeException("Unsupported platform: " + os + " " + arch);
    }
}
