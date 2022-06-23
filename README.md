# Why did you stop porting
I'm spending some time working of CinemaMod which seems to be much more complete and includes it's own CEF integeration that is probaly better than mine (at least for keyboard input). 
[Check it out here!](https://github.com/CinemaMod/)
It's probaly closer to the WebDisplays for modern versions you've been looking for. 
![CinemaMod Teaser Images from their mod GitHub page](https://user-images.githubusercontent.com/30220598/173701589-b093e08b-7568-465e-87c3-14574d645c1f.jpg)

# MCEF (fork of javaarchives fabric fork)
A forge version of mcef to support multiple platforms and provide an updated Chromium release.

Chromium Version `100.0.4896`

Thanks to ds58 for the original chromium update and platform support. Only the fabric and ui porting is done by javaarchive. Forge port from fabric done by Mysticpasta1

[See the original project here for more information](https://github.com/montoyo/mcef)

[ds58's JCEF mirror](https://ds58-mcef-mirror.ewr1.vultrobjects.com/)


# What JavaArchive got working
![MCEF on Windows 11 Virtual Machine](https://cdn.discordapp.com/attachments/985588552735809700/986510944848973824/Windows_11-2022-06-14-21-43-16.png)
This was before JavaArchive put in proper mouse and keyboard input. 

![MCEF on Arch + Gnome](https://cdn.discordapp.com/attachments/986642832695627816/987765385971515482/Screenshot_from_2022-06-15_08-33-38.png)
JavaArchive fixed the bottom bit being glitchy later on. 

# What doesn't work
* Pressing enter in text boxes
* Scroll wheel
* Focusing the addressbar (probaly easily fixable)

# Currently supported platforms
- Windows 10/11 64 bit
- macOS (Intel-based Macs only)
- Linux 64 bit (tested on Fedora 34 and Ubuntu 20.04) 
  (note:  linux actually has an incompatible glibc 2.32 in the newest CEF builds sadly so 20.04 and lower will run into library issues). These issues JavaArchive can't really fix unless JavaArchive get the time to build CEF myself. 
  
