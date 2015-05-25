# hiddenservice-android
This Project shows how to implement the SilverTunnel-NG library into an Android Application to run a hiddenservice which is accessible through the Tor network.

SilverTunnel-ng is available through maven or you can download it at : https://sourceforge.net/projects/silvertunnel-ng/

This example project shows the following:
- access a "normal" URL through Tor
- access a hiddenservice (onion-URL)
- provide a hiddenservice

Tested on Nexus 5 Android 5.1

SilverTunnel-NG currently uses around 100-200 MB of RAM so it is not possible to "just" include the library and run requests through Tor, you have to overcome the memory limit of Android.

This is not 100% tested so expect bugs ;)

You can report bugs or feature requests at the sourceforge Project page. 
