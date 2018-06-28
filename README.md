# DE-ENIGMA-F2
De-Enigma project using Flipper2.
In this branch we have configured all modules to use the ROS middleware

## Requirements
Install Java JDK 8 (http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).

Install GIT (https://git-scm.com/downloads) and GIT-LFS (https://github.com/git-lfs/git-lfs/wiki/Installation)

Install Apache ANT (https://ant.apache.org/bindownload.cgi)

The tablet apps attempt to connect to a hardcoded IP address: 192.168.0.22.
Make sure a ROS Bridge Server is running on this IP, and both the ROS PC and the tablets are connected to the same WIFI network. 

## To install
1. Go to empty dir where you want to install, for example: cd ~/UTwente
1. git clone https://github.com/ArticulatedSocialAgentsPlatform/hmibuild.git
1. git clone https://github.com/hmi-utwente/DE-ENIGMA-F2.git
1. cd DE-ENIGMA-F2
1. git checkout ros
1. git lfs pull

Please download and install the "ROS" version of the adult and child tablet apps. The apps can be downloaded from here: https://drive.google.com/drive/folders/1Nd1P6NPuA0G7qcIwc_o6ZeZXVLHUn6ac?usp=sharing

## To download and compile dependencies for your specific platform
1. ant resolve
1. ant compile

## To Configure
### ROS Bridge Server IP address
Edit the file /DE-ENIGMA-F2/resource/defaultmiddleware.properties and change the following line: websocketURI=ws://192.168.0.22:9090

### Robot IP address
Edit the file /DE-ENIGMA-F2/resource/mechio.properties and change the following line: mechio_ip=192.168.0.107

### Switch between English and Serbian language
Edit files /DE-ENIGMA-F2/resource/behaviours/P2/animationAudioBehaviour.xml and /DE-ENIGMA-F2/resource/behaviours/P2/audioBehaviour.xml. 
* To switch to English, change the placeholder for the audio file path to: audio/P2/EN/$speak1$.wav
* For Serbian, change the placeholder to: audio/P2/SR/$speak1$.wav

## To run
1. Start ROSCore: roscore
1. Start ROS Bridge Server: roslaunch rosbridge_server rosbridge_websocket.launch
1. Start the UTwente launcher: ant run -Drun.main.class=starters.UTStarterStopper
1. To launch the modules automatically when the GUI appears, start the UTwente launcher with the --autorun parameter: ant run -Drun.main.class=starters.UTStarterStopper -Drun.argline="--autorun"
1. A GUI pops up with two buttons:
	* Press the "Start all modules" button to start Mechio, ASAP and Flipper
	* Press the "Stop all modules" button to stop them again
1. Start the child and adult tablet apps, they should connect automatically
	
