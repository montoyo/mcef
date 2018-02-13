# MCEF
Minecraft Chromium Embedded Framework (MCEF) is an API to allow Minecraft Modders to add custom web browsers into Minecraft.
The project was initialy made for WebDisplays (www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/1291044-web-displays-browse-on-the-internet-in-minecraft).
It is based on JCEF (https://code.google.com/p/javachromiumembedded/), which is based on CEF (https://bitbucket.org/chromiumembedded/java-cef) which is based on chromium (http://www.chromium.org).

# Features
- 2D & 3D web view rendering (not only in GUIs)
- Java -> JavaScript (IBrowser.runJavaScript)
- JavaScript -> Java (IJSQueryHandler)
- Embedded files (mod://modname/file.html => /assets/modname/html/file.html)
- HTML5, CSS3 are supported.

# What can I do with this?
- If you're tired of Minecraft's GuiScreen, you can make a HTML/JS/CSS GUI.
- Open a link to your wiki.
- Whatever you want, but please not a WebDisplays clone.

# Currently supported platforms
Right now MCEF supports Windows 32 and 64 bits. I'll compile JCEF for linux as soon as possible. However I can't compile it for Mac.
If you want to compile the natives for Mac, you can follow the instructions here: https://bitbucket.org/chromiumembedded/java-cef/wiki/BranchesAndBuilding

# For players
This is the Github project of MCEF; here you can only read the source code of it.
You can download the mod in its latest version from here: http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2324969-minecraft-chromium-embedded-framework-mcef

# For modders
**DONT** copy the net.montoyo.mcef.api package into your project. Instead, download the latest API release from https://github.com/montoyo/mcef/releases and put it in the libs folder. Users will have to download the mod from the MinecraftForum thread.
To understand how it works, you may look at the net.montoyo.mcef.example package, which demos:
* The IBrowser interface, how to draw it and control it
* The IJSQueryHandler interface, how to handle JavaScript queries
* The IDisplayHandler interface, how to handle browser URL changes
* How to use the mod:// scheme

# For forkers
Don't forget to add "-Dfml.coreMods.load=net.montoyo.mcef.coremod.ShutdownPatcher" to the VM options, otherwise the Java process will hang forever!
