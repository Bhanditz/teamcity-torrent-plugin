

[![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


 TeamCity Torrent plugin
 ===========================

 With this plugin, users and build agents can download TeamCity build artifacts faster,
 especially in a distributed environment.

## 1. Downloading binaries
 
 The latest build of the plugin is available on the public TeamCity server and can be downloaded from the public TeamCity server:
  * [for TeamCity 2018.1+](
http://teamcity.jetbrains.com/repository/download/TeamCityPluginsByJetBrains_TorrentPlugin_TorrentPluginTeamcity20181Compatible/.lastPinned/torrent-plugin.zip)
  * [for TeamCity 2017.x]( http://teamcity.jetbrains.com/repository/download/TeamCityPluginsByJetBrains_TorrentPlugin_TorrentPluginTeamcity20172Compatible/.lastPinned/torrent-plugin.zip)  

 ## 2. Building sources


 Run the following command in the root of the checked out repository:
 
    `mvn clean package`

 ## 3. Installing
 
 Install the plugin as described in the [TeamCity documentation](http://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins).


## 4. Setting up the plugin

 Once you restart the server, a new link, Torrent Settings, will appear in the Administration area. The plugin is disabled by default. You can enable it on this page.
 If the plugin works correctly and you checked both options on the Torrent settings page, then once a large enough artifact is published, you should see the torrent icon near the artifact name.
 Clicking this icon should download .torrent file and you can open it with your favorite torrent client. 
 
## 5. Tech notes

* server and agent bind on the first available port in 6881-6889 interval, so these ports should be reachable.
* Torrent files are created only for large artifact (by default more then 10mb). So you cannot download small file via bittorrent 
 
 