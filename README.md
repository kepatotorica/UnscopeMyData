# UnscopeMyData

UnscopeMyData is an Android utility designed to help users bypass "Scoped Storage" limitations for specific app data folders. It uses **Shizuku** to perform elevated filesystem operations, allowing you to sync data between protected `/Android/data/` directories and a public "Unscoped" directory on your internal storage.

## Why?

I use emulators on my devices and it is a pain to sync the data between all of them. I use syncthing fork, which allows me to sync data across devices, but not in any data folder. This app allows me to quickly move all data that I care about into a folder that syncthing can access, then on my other device allows me to write that same data where it belongs.

## Features

- **Scoped Storage Bypass:** Access and sync folders usually restricted by Android.
- **Shizuku Integration:** Uses a system-level privileged process for file operations.
- **Visual Status:** Real-time Shizuku connectivity status in Settings.
- **Bare Bones UI:** Very simple UI with little customization.
- **Portable Folder:** Allowing for syncthing, manual copy and paste, or other syncing services to quickly transfer many different folders at once without having to navigate individual apps menus.

## Prerequisites

- **Shizuku:** The Shizuku app must be installed and running on your device.
- **All Files Access:** The app requires the "All Files Access" permission to manage your public "Unscoped" directory.

## How it Works

1. **Add a Folder:** Pick a folder on your device (defaults to `storage/emulated/0/android/data/` but it can be any folder).
2. **Assign a Name:** Give it a unique identifier for the unscoped directory.
3. **Sync:**
   - `data -> unscoped`: Copies the protected app data to your public unscoped folder.
   - `unscoped -> data`: Pushes your modified unscoped data back into the protected app folder.

## Technical Details

- Built with **Jetpack Compose**.
- Uses **AIDL** for IPC with the privileged Shizuku process.
- Implements a local fallback for public storage operations to ensure reliability.

## Releases & F-Droid

- **GitHub Releases:** Automated builds are available on the [Releases](https://github.com/your-username/UnscopeMyData/releases) page.
- **F-Droid:** This project includes **Fastlane metadata** (located in `fastlane/metadata`) to be compatible with F-Droid's build system.
