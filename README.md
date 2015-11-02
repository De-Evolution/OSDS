# Open Source Driver Station (OSDS)

This app is an open-source version of the FTC 2015-16 Driver Station app in the Play Store.  It is based off of the beta Driver Station source code distributed to teams at preview events. I combined it with the up-to-date backend libraries taken from the FTC SDK, as well as some decompiled bits of the Play Store Driver Station code to create a modified driver station which works with the current Robot Controller app.


### Code Ownership
The GUI source code in this was written by Qualcomm and modified by me.  It is still their property.  The `FtcCommon` and `RobotCore` libraries are both taken in their compiled form from the ftc_sdk project.

### Pros & Cons
#### Why you WOULD use this driver station
- It supports a larger range of devices and screen sizes than the default app (though it won't magically fix Wifi Direct on devices where it is broken or not present)
- It gives you quite a bit more screen real estate for Telemetry, depending on the size of your device's screen
- It allows you to customize your team's DS.  You can do simple color and icon theming, or something as complicated as parsing the Telemetry feed and creating a GUI for your robot.
- It supports LAN mode (see below).
- It features a much less jarring and aggressive "Configuring Wifi, please wait..." dialog, which hopefully won't get stuck in a loop.  All this is doing is toggling wifi off and on anyway.
- It includes the old, device-agnostic (though root-requiring) wifi channel changer.  The robot controller decides which channel is used, not the driver station, but I suppose you could install OSDS on your robot controller phone and use it there.

#### Why you WOULDN'T use this driver station
- I have NO IDEA if it is at all competition legal, or will work at competition.  Make sure to keep a supported driver station device for competition use.
- It may be buggier than the official DS, as the GUI was not originally meant to work with this version of the backend library.
- Maybe you really like the official DS's color scheme?

### Download
Check the releases section at the top.

### LAN Mode
LAN mode is an alternate method of connecting the robot controller and the driver station.  It connects through a router over WiFi instead of over Wifi Direct, which lets you connect to the robot simply by joining the phones to the same wifi network and typing in the IP address of the controller on the DS. In my experience, it connects far more quickly and robustly then the wifi direct version, and hardly ever requires anything to be rebooted or restarted (yay!).  Since it uses a regular wifi network, you can use Android's network debugging with ease, and even (double yay!) download and run a robot controller program without you ever having to touch the robot controller phone.
 
LAN mode does require modifications to your robot controller code, though.  Download [this zip file](https://dl.dropboxusercontent.com/u/27566023/Software%20Host/LAN_DS_Enabler_v2.zip) and extract it to the root of your ftc_app source tree, and you should be good to go.  It overwrites your `FtcRobotController` activity and your strings and menu xml files with LAN Mode-enabled ones, and adds some of its own files as well.

### Screenshot
[![Screenshot](https://dl.dropboxusercontent.com/u/27566023/Image%20Host/OSDS-Screenshot-small.png)](https://dl.dropboxusercontent.com/u/27566023/Image%20Host/OSDS-Screenshot-large.png)
