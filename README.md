# Mobile Programming Homework Repository

Welcome! This repo contains homework assignments for Mobile Programming. It now includes both a Kotlin (HW1) project and a Swift (HW2) command-line app.

## Contents

- HW1 (Kotlin/JVM): Console app that fetches GitHub user info and repositories (Retrofit + Coroutines). Source in `HW1/src/main/kotlin`.
- HW2 (Swift): Mastermind CLI game that talks to an online API (`https://mastermind.darkube.app`). Entry point at `HW2/main.swift`.

## Getting Started

1. Clone the repo

```powershell
git clone https://github.com/saj306/mobile-programming-hw1.git
cd mobile-programming-hw1
```

2. Run HW1 (Kotlin)

Build with Gradle Wrapper:

```powershell
cd HW1/HW1
.\gradlew.bat clean build
```

Run options:

- Recommended: Open the `HW1` project in IntelliJ IDEA or VS Code (with Kotlin) and run `project.MainKt`.
- Or add the Gradle Application plugin and a `mainClass` to enable `gradlew run` (optional).

3. Run HW2 (Swift)

Compile and run the CLI:

```powershell
cd ..\HW2
swiftc main.swift -o Mastermind.exe
.\Mastermind.exe
```

When it starts:

- Enter 4 digits (each 1–6). B = correct digit+position, W = correct digit wrong position.
- Type `exit` to end the game (it will clean up the game on the server).

## Notes

- HW1 targets JVM with Kotlin 2.1.x and requires JDK 21. If Gradle can’t find Java, set `JAVA_HOME` to your JDK 21 path in Windows.
- HW2 is a single-file Swift app; no SwiftPM package is required. You can also run it directly with `swift main.swift` if your Swift install supports it.
