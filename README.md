# What did I do?
Update MCEF binding to JCEF 78.3.9

# h264 support? Play Bilibili Video?
Because of issues of license, Chromium project does not provide official *.dlls which support h264. 
- 1. Build JCEF 
https://bitbucket.org/chromiumembedded/java-cef/wiki/BranchesAndBuilding

- 2. Replace the (J)CEF DLLs.
copy files in `jcef_build\native\Release` to your `MC folder`. 

- 3. Build CEF (you can enable ffmpeg to support h264) 
https://bitbucket.org/chromiumembedded/cef/wiki/BranchesAndBuilding

or download compiled h264 CEF here
https://blog.csdn.net/epubcn/article/details/82689929

- 4. Replace the (J)CEF DLLs.
copy CEF DLLs to your `MC folder` and replace existing files 

- 5. Enjoy the amazing mod
