# What did I do?
Update MCEF binding to JCEF 78.3.9

# h264 support? Play Bilibili Video?
Because of issues of license, Chromium project does not provide official *.dlls which support h264. 
1. Build JCEF 

&emsp; &emsp;https://bitbucket.org/chromiumembedded/java-cef/wiki/BranchesAndBuilding

2. Replace the (J)CEF DLLs. 

&emsp; &emsp;copy files in `jcef_build\native\Release` to your `MC folder`. 

3. Build CEF (you can enable ffmpeg to support h264) 

&emsp; &emsp;https://bitbucket.org/chromiumembedded/cef/wiki/BranchesAndBuilding

&emsp; &emsp;or download compiled h264 CEF here

&emsp; &emsp;https://blog.csdn.net/epubcn/article/details/82689929

4. Replace the (J)CEF DLLs.

&emsp; &emsp;copy CEF DLLs to your `MC folder` and replace existing files 

5. Enjoy the amazing mod

![image](https://github.com/xueyeshengdan/mcef/blob/master/QQ%E6%88%AA%E5%9B%BE20200827211134.png)
