call setVersion.cmd
start /B /MIN jre\bin\java.exe -cp "%RGP_VERSION%;libs/*" ar.com.rgp.RgpMain 1 NO_USER
