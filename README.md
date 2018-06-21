# DE-ENIGMA-F2
De-Enigma project using Flipper2.

## Requirements
Install Java JDK 8 (http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).

Install GIT (https://git-scm.com/downloads) and GIT-LFS (https://github.com/git-lfs/git-lfs/wiki/Installation)

Install Apache ANT (https://ant.apache.org/bindownload.cgi)

Install an Apollo server (https://activemq.apache.org/apollo/). On Windows, I recommend to install it as a service so that it starts in the background automatically.

The tablet apps automatically attempt to connect to a hardcoded IP address: 192.168.0.22.
Make sure the Apollo server is running and is accessible on this IP, and both the Apollo PC and the tablets are connected to the same WIFI network. 

## To install
1. Go to empty dir where you want to install, for example: cd ~/UTwente
1. git clone https://github.com/ArticulatedSocialAgentsPlatform/hmibuild.git
1. git clone https://github.com/hmi-utwente/DE-ENIGMA-F2.git
1. cd DE-ENIGMA-F2
1. git lfs pull

Please download and install the "Apollo" version of the adult and child tablet apps. The apps can be downloaded from here: https://drive.google.com/drive/folders/1Nd1P6NPuA0G7qcIwc_o6ZeZXVLHUn6ac?usp=sharing

## To download and compile dependencies for your specific platform
1. ant resolve
1. ant compile

## To Configure
### Apollo Server IP address
Edit the file /DE-ENIGMA-F2/resource/defaultmiddleware.properties and change the following line: apolloIP=192.168.0.22

### Robot IP address
Edit the file /DE-ENIGMA-F2/resource/mechio.properties and change the following line: mechio_ip=192.168.0.107

### Switch between English and Serbian language
Edit files /DE-ENIGMA-F2/resource/behaviours/P2/animationAudioBehaviour.xml and /DE-ENIGMA-F2/resource/behaviours/P2/audioBehaviour.xml. 
* To switch to English, change the placeholder for the audio file path to: audio/P2/EN/$speak1$.wav
* For Serbian, change the placeholder to: audio/P2/SR/$speak1$.wav

## To run
### On windows
1. Start an Apollo server (see Apollo documentation). If you installed Apollo as a service, you may skip this step.
1. Double-click the executable file: /DE-ENIGMA-F2/UTLauncher.vbs
1. A GUI pops up with two buttons:
	* Press the "Start all modules" button to start Mechio, ASAP and Flipper
	* Press the "Stop all modules" button to stop them again
1. Start the child and adult tablet apps, they should connect automatically

### On other platforms
1. Start an Apollo server (platform dependent, see Apollo documentation)
1. Start the UTwente launcher: ant run -Drun.main.class=starters.UTStarterStopper
1. A GUI pops up with two buttons:
	* Press the "Start all modules" button to start Mechio, ASAP and Flipper
	* Press the "Stop all modules" button to stop them again
1. Start the child and adult tablet apps, they should connect automatically
	
