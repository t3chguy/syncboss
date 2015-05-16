# Introduction #

See [Install](Install.md) guide here on how to get SyncBoss up and running. Once you have the required files, you can follow this guide.

SyncBoss works with one 'DJ' computer, and one or more client computers.

The DJ sets up SyncBoss to run in DJ mode, and transmits music over LAN. Client computers can then connect to the DJ and stream music in perfect millisecond-precise sync.

# Details #

## Guide: DJ Mode ##

  1. Open SyncBoss by double-clicking on SyncBoss.jar
  1. Select an "Output Device" from the drop-down box at the top (Leave as default if you're not sure). Note: Java Audio Engine is not suitable, direct sound card selection is necessary for sufficiently low latency. If Java Audio Engine is the only option, you may need to try installing the 32 bit Java Runtime Environment (the 64 bit JRE may not allow you to select your sound card directly).
  1. The "Sound Lag" slider can be adjusted to reflect the time between your sound card and speakers. If you're not sure what it is, leave it at default. If the sound equipment on one computer is relatively higher or poorer quality than that on another computer, you can adjust this slider to correct this discrepancy.
  1. The Volume slider allows you to adjust the volume (it only sets it for the current computer).
  1. Ignore the D.J. address field (this is only for clients)
  1. Click "Start DJ Mode"
  1. Open Winamp (Make sure the plugin is installed and selected, as per the guide [here](Install.md) - the plugin is not automatically active after you copy it to the Winamp plugins directory).
  1. Click play
  1. After a few seconds, your music should start playing & broadcasting!
  1. Your music may initially skip a lot as the sync engine trains.

## Guide: Bitch (Client) Mode ##

  1. Open SyncBoss by double-clicking on SyncBoss.jar
  1. Select an "Output Device" from the drop-down box at the top (Leave as default if you're not sure). Note: Java Audio Engine is not suitable, direct sound card selection is necessary for sufficiently low latency. If Java Audio Engine is the only option, you may need to try installing the 32 bit Java Runtime Environment (the 64 bit JRE may not allow you to select your sound card directly).
  1. The "Sound Lag" slider can be adjusted to reflect the time between your sound card and speakers. If you're not sure what it is, leave it at default. If the sound equipment on one computer is relatively higher or poorer quality than that on another computer, you can adjust this slider to correct this discrepancy.
  1. The Volume slider allows you to adjust the volume (it only sets it for the current computer).
  1. In the D.J. address field, enter the IP address of the DJ computer (e.g., 192.168.1.2)
  1. Click "Start Bitch Mode"
  1. After a few seconds, your music should start playing & broadcasting!
  1. Your music may initially skip a lot as the sync engine trains.


## Known Issues ##

  * Pause, stop & volume controls in winamp don't work
  * Playback may cease after a few hours playback, or Winamp may crash. Restarting Winamp should overcome this issue