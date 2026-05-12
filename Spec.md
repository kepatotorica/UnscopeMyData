I have an issue. I love to use syncthing to keep data up to date between my devices. I have a few android devices and I like to run emulators on them. The problem with this is that syncthing cannot look into data folders for android devices. This is okay for some emulators where you can choose the save file path, but this doesn't work for all emulators. For example, dolphin saves to /storage/emulated/0/Android/data/org.dolphinemu.dolphinemu/files/SaveStates. I would like to build an app that allows a user to configure data paths like the dolphin path above. It needs a few parts, and it needs to be a list that has a high level tracking manifest.
1. dataFolderName
2. Path (this is the data path) ie: /storage/emulated/0/Android/data/org.dolphinemu.dolphinemu/files/SaveStates
3. MostRecentUpdator: ie device name like pixel-8-pro

The top level manifest should be used to track a few things:
1. The device name where the most recent change comes from
2. a flag "HasBeenLoadedOnThisDevice", this will be set as false whenever there is a file change inside of a certain folder.
3. a path outside of scoped storage for each file, this should be automated and follow this format /stroage/emulatored/0/UnscopeMyData/syncMe/dataFolderName


As for the ui of the app, we should just see the list of folders we have, and inside a card for each folder some of the manifest data, like hasBeenLoaded, and the device name for the most recent update. On the right side of each card there should be a delete button, and a copy path button. At the bottom we should havea few buttons, addNewFolder, pullDataFolders, pushDataFolders. When you click addNewFolder it will open a very simple file browser that lets you pick a folder using shizuku, it should open to /storage/emulated/0/Android/data/ by default, they should then be able to navigate wherever they want, and they should have the option on the bottom that says AddFolder, then a small cancel button to the right. If a users adds the folder ask them for the dataFolderName, this must be unique vs other folders. The Path must also be unique, and is not from user input, but instead the path of the folder. Then the MostRecentUpdator should get the name of the device.
