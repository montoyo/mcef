# MCEF (fork of ds58 fork)
A fabric version of mcef to support multiple platforms and provide an updated Chromium release.

Chromium Version `100.0.4896`

Thanks to ds58 for the original chromium update and platform support. Only the fabric and ui porting is done by me. 

[See the original project here for more information](https://github.com/montoyo/mcef)

[ds58's JCEF mirror](https://ds58-mcef-mirror.ewr1.vultrobjects.com/)

# Currently supported platforms
- Windows 10/11 64 bit
- macOS (Intel-based Macs only)
- Linux 64 bit (tested on Fedora 34 and Ubuntu 20.04) 
  (note:  linux actually has an incompatible glibc 2.32 in the newest CEF builds sadly so 20.04 and lower will run into library issues). This issues I can't really fix unless I get the time to build CEF myself. 
  