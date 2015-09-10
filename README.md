# Open Source Driver Station (OSDS)

This app is an open-source version of the FTC 2015-16 Driver Station app in the Play Store.  It is based off of the beta Driver Station source code distributed to teams at preview events. I combined it with the up-to-date backend libraries taken from the FTC SDK, as well as some decompiled bits of the Play Store Driver Station code to create a modified driver station which works with the current Robot Controller app.

### Code Ownership
The GUI source code in this was written by Qualcomm and modified by me.  It is still their property.  The `FtcCommon` and `RobotCore` libraries are both taken in their compiled form from the ftc_sdk project.

### Pros & Cons
#### Why you WOULD use this driver station
- It supports a larger range of devices and screen sizes than the default app (though it won't magically fix Wifi Direct on devices where it is broken or not present)
- It gives you quite a bit more screen real estate for Telemetry, depending on the size of your device's screen
- It allows you to customize your team's DS.  You can do simple color and icon theming, or something as complicated as parsing the Telemetry feed and creating a GUI for your robot.
- It supports LAN mode, which lets you connect to the robot by joining the same wifi network and typing in the IP address, instead of futzing around with Wifi Direct.  This does require modifications to your robot controller code, though.  Download [this zip file](https://dl.dropboxusercontent.com/u/27566023/Software%20Host/LAN_DS_enabler.zip) and extract it to the root of your ftc_sdk source tree, and you should be good to go.
- It features a much less jarring and aggressive "Configuring Wifi, please wait..." dialog, which hopefully won't get stuck in a loop.
- It includes the old, device-agnostic (though root-requiring) wifi channel changer.  The robot controller decides which channel is used, not the driver station, but I suppose you could install OSDS on your robot controller phone and use it there.

#### Why you WOULDN'T use this driver station
- I have NO IDEA if it is at all competition legal, or will work at competition.  Make sure to keep a supported driver station device for competition use.
- It may be buggier than the official DS, as the GUI was not originally meant to work with this version of the backend library.
- Maybe you really like the official DS's color scheme?

### Download
Check the releases section at the top.