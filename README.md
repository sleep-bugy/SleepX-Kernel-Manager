# Xtra Kernel Manager

![Platform](https://img.shields.io/badge/platform-Android-green?style=for-the-badge&logo=android)
![Language](https://img.shields.io/badge/language-Kotlin-purple?style=for-the-badge&logo=kotlin)
![UI](https://img.shields.io/badge/Jetpack-Compose-blue?style=for-the-badge&logo=jetpackcompose)
![License](https://img.shields.io/github/license/Gustyx-Power/Xtra-Kernel-Manager?style=for-the-badge)
![Root Required](https://img.shields.io/badge/Root-Required-critical?style=for-the-badge&logo=android)
![GitHub commits](https://img.shields.io/github/commit-activity/t/Gustyx-Power/Xtra-Kernel-Manager?style=for-the-badge)
[![Repo Size](https://img.shields.io/github/repo-size/Gustyx-Power/Xtra-Kernel-Manager?style=for-the-badge&logo=github)](https://github.com/Gustyx-Power/Xtra-Kernel-Manager)

**Xtra Kernel Manager** is a modern, rooted Android application built with Kotlin and Jetpack Compose, designed for real-time monitoring and tuning of CPU performance, thermal behavior, and power-saving configurations on devices running custom kernels.
## Apps Logo 
![XKM Logo](app/src/main/res/drawable/logo.png)
---

## ✨ Features

- 📊 **Real-time CPU Temperature Monitoring**  
  View individual core temperatures in a clean, responsive UI.

- 🌡️ **Thermal Zone Status**  
  Retrieve and display system thermal zone data for advanced thermal debugging.

- ⚙️ **CPU Tuning** *(Root Required)*  
  Apply governor changes (e.g. `performance`, `powersave`) on-the-fly with native shell execution via [libsu](https://github.com/topjohnwu/libsu).

- 💡 **Material 3 UI**  
  Elegant light-themed interface using the latest Jetpack Compose and Material Design 3 components.

- 🚀 **Fast & Minimal**  
  Lightweight architecture using MVVM pattern, ensuring smooth performance on rooted devices.

---

## 📱 Requirements

- ✅ Root access (Magisk / KernelSU supported)  
- ✅ Android 10 (API 29) or above  
- ✅ Custom kernel with tunable CPU governors  

---

## 🔐 Permissions

- `root` access via libsu (automatic permission request)
- No internet access or telemetry. 100% offline and private.

---

## 🛠 Built With

- [Kotlin](https://kotlinlang.org/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [libsu by Topjohnwu](https://github.com/topjohnwu/libsu)
- MVVM Architecture (ViewModel + LiveData)
- Material Design 3

---

## 📂 Repository

This is the official repository for **Xtra Kernel Manager**, maintained by the **Xtra Manager Software** team.  
Feel free to fork, open issues, or contribute via pull requests.

---

> [💡 Tips and Tricks]
>
> - **Performance Mode**: For demanding tasks like gaming, switch to `performance` governor for maximum CPU speed.
> - **Battery Saving**: Use `powersave` governor when battery life is a priority.
> - **Balanced Profile**: Most custom kernels offer a `schedutil` or `interactive` governor for a balance between performance and power saving.
> - **Thermal Monitoring**: Keep an eye on CPU temperatures during heavy usage to prevent overheating.

---

### Developer
1. Gustyx-Power ( Developer All Code For XKM )
2. Pavellc ( Ui XKM Supports )
3. Ziyu ( Implementation Tuning Help )

---

## 📣 Disclaimer

> ⚠️ This app performs privileged operations that may affect system stability.  
> Use at your own risk. The developer is not responsible for any damage caused by improper configuration.

---

© 2025 Xtra Manager Software. All rights reserved.
