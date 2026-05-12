# UnscopeMyData

UnscopeMyData is an Android utility designed to help users bypass "Scoped Storage" limitations for specific app data folders. It uses **Shizuku** to perform elevated filesystem operations, allowing you to sync data between protected `/Android/data/` directories and a public "Unscoped" directory on your internal storage.

## Features

- **Scoped Storage Bypass:** Access and sync folders usually restricted by Android.
- **Shizuku Integration:** Uses a system-level privileged process for file operations.
- **Automatic Sync:** Sync data from protected app folders to public storage and vice versa.
- **Visual Status:** Real-time Shizuku connectivity status in Settings.
- **Clean UI:** Modern Material 3 interface with intuitive folder management.

## Prerequisites

- **Shizuku:** The Shizuku app must be installed and running on your device.
- **All Files Access:** The app requires the "All Files Access" permission to manage your public "Unscoped" directory.

## How it Works

1. **Add a Folder:** Pick a folder on your device (usually in `/Android/data/`).
2. **Assign a Name:** Give it a unique identifier for the unscoped directory.
3. **Sync:**
   - `data -> unscoped`: Copies the protected app data to your public unscoped folder.
   - `unscoped -> data`: Pushes your modified unscoped data back into the protected app folder.

## Technical Details

- Built with **Jetpack Compose**.
- Uses **AIDL** for IPC with the privileged Shizuku process.
- Implements a local fallback for public storage operations to ensure reliability.
