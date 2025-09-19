# CriminalIntent — Learning Project (Android / Kotlin)

> **Not for production use.**  
> Built while following the **Big Nerd Ranch** Android curriculum to practice Jetpack + Kotlin patterns.

A simple “crime log” app used to explore **MVVM**, **RecyclerView**, **Navigation**, **coroutines/Flow**, and **ViewBinding**.  
The UI uses a dark “hacker” theme (green on black). You can add a crime from the top-right app-bar “+”, view details, and mark it solved.

---

## ✨ Features

- Crime list with **RecyclerView**
- Crime detail screen (edit title, date, solved flag)
- **App-bar “+”** action to create a new crime (FAB removed)
- Empty-state placeholder (shows when the list is empty)
- State management with **ViewModel** and **Kotlin Flow**
- Safe, type-safe navigation with **Navigation Component**
- **ViewBinding** for view access
- Material 3 styling with custom dark palette (`hacker_black`, `hacker_green`)

---

## 🧱 Architecture

**MVVM** + Jetpack:

