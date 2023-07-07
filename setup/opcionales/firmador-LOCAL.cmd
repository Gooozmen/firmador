call setVersion.cmd
start /B /MIN jre\bin\javaw.exe -cp "%RGP_VERSION%;libs/*" ar.com.rgp.RgpMain 1
