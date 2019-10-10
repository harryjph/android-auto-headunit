# Headunit

This is an Android Auto Headunit emulator, based off of work of the owners of the upstream repositories.

Things I've changed / added:
* Cleaner & more performant codebase
* Multi-touch support
* Proper fullscreen / immersive mode with hidden system bars
* Re-implemented TLS layer in Kotlin using Java APIs, avoiding C in codebase, making app more portable & smaller and simplifying the build process.
* Trust any connected phone, so once phone is connected, app will immediately launch.

Currently requires API level 18 (Android 4.3), can easily be made to support API level 16 (Android 4.1) (USB `bulkTransfer` needs to use alternative method on older APIs)
