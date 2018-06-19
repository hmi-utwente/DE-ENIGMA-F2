# DE-ENIGMA-F2
De-Enigma project using Flipper2.
In this branch we have configured all modules to use the ROS middleware

## Requirements
The tablet apps attempt to connect to a hardcoded IP address: 192.168.0.22.
Make sure a ROS Bridge Server is running on this IP, and both the ROS PC and the tablets are connected to the same WIFI network. 

## To install
1. Go to empty dir where you want to install, for example: cd ~/UTwente
1. git clone https://github.com/ArticulatedSocialAgentsPlatform/hmibuild.git
1. git clone https://github.com/hmi-utwente/DE-ENIGMA-F2.git
1. cd DE-ENIGMA-F2
1. git lfs pull

## To download dependencies
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
1. A GUI pops up with two buttons:
	* Press the "Start all modules" button to start Mechio, ASAP and Flipper
	* Press the "Stop all modules" button to stop them again
1. Start the child and adult tablet apps, they should connect automatically
	

