# RetroFrame — Android camera app

RetroFrame is a clean, minimal film-camera app prototype built with Kotlin + Jetpack Compose + CameraX.

## Included

- Full-screen CameraX viewfinder
- Front/back camera toggle
- Film-inspired presets: Neutral, Portra 400, Gold 200, Ektar 100, Superia 400, HP5 Plus, Tri-X 400, 800T
- Independent per-film adjustments: Exposure, Contrast, Saturation, Warmth, Fade, Grain, Vignette, Intensity
- Switching between films preserves each film's custom tuning
- One-tap reset for the selected film
- Captures are processed at photo resolution and written to `Pictures/RetroFrame`
- Gallery action opens the most recently saved image when possible
- Minimal black/cream UI with compact typography and restrained controls
- KomoUI 0.4.0 for the shadcn-inspired Compose theme/action styling

## Stack

- Kotlin 2.2.20
- Android Gradle Plugin 8.13.0
- Gradle 8.13
- Jetpack Compose 1.9-era BOM 2025.09.01
- CameraX 1.6.2
- KomoUI 0.4.0 — current continuation/rebrand of `shadcn-ui-kmp`
- minSdk 26 / targetSdk 36

## Build

Open this folder in Android Studio. Let Gradle download dependencies and use JDK 17.

The current environment used to generate this project does not contain the Android SDK or a Gradle installation, so a device APK build could not be executed here. The source is organized as a standard Android Studio project.

The included `gradle/wrapper/gradle-wrapper.properties` points to Gradle 8.13; run Android Studio's Gradle wrapper setup if the wrapper scripts are not present in your environment.

## Filter design

The presets are intentionally “film-inspired” recipes rather than claims of exact film emulation. Captures use a deterministic CPU pixel pipeline so grain and vignette are repeatable and the saved result follows the slider settings.
