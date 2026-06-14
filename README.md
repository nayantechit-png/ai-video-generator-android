# 🎬 AI Video Generator – Android App

Generate stunning AI-powered videos from text prompts or images, directly on your Android device.

![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Stability AI](https://img.shields.io/badge/Stability%20AI-FF6600?style=flat)
![OpenAI](https://img.shields.io/badge/OpenAI-412991?style=flat&logo=openai&logoColor=white)

---

## ✨ Features

| Feature | Description |
|---|---|
| 🖊️ **Text → Video** | Describe your scene in words — AI animates it |
| 🖼️ **Image → Video** | Upload photos and bring them to life |
| 🎨 **11 Visual Styles** | Cinematic, Anime, Cyberpunk, Watercolor, and more |
| 📐 **6 Resolutions** | From 480p to 1080p, square, portrait, landscape |
| ⚙️ **Fine Controls** | Motion strength, CFG scale, duration, FPS |
| 🤖 **Multi-Provider** | Stability AI, OpenAI DALL-E, or Local (offline) slideshow |
| 💾 **Local History** | Room database keeps all your projects |
| 🎥 **Built-in Player** | ExoPlayer playback with full controls |
| 📤 **Share** | Share generated videos to any app |
| 🌙 **Dark Theme** | Eye-friendly dark AMOLED UI |

---

## 🏗️ Architecture

```
app/
└── java/com/aivideogen/
    ├── AIVideoGeneratorApp.kt          # Hilt application class
    ├── data/
    │   ├── model/                      # VideoProject, enums, DTOs
    │   ├── local/                      # Room DB, DAO, TypeConverters
    │   ├── remote/                     # Retrofit – Stability AI & OpenAI
    │   └── repository/VideoRepository  # Single source of truth
    ├── di/AppModule.kt                 # Hilt DI bindings
    ├── viewmodel/
    │   └── VideoGeneratorViewModel.kt  # Shared ViewModel
    ├── ui/
    │   ├── MainActivity / SplashActivity
    │   ├── home/                       # Home screen + recent projects
    │   ├── generate/                   # Generation form + image picker
    │   ├── gallery/                    # Grid gallery + video player
    │   └── settings/                   # API keys + preferences
    ├── service/VideoGenerationService  # Foreground service
    └── utils/
        ├── FileUtils.kt                # File I/O helpers
        ├── VideoUtils.kt               # MediaCodec video encoding
        ├── PreferencesManager.kt       # DataStore wrapper
        └── Extensions.kt              # Kotlin extension functions
```

**Tech stack:** Kotlin · MVVM · Hilt · Coroutines/Flow · Retrofit · Room · WorkManager · ExoPlayer · Glide

---

## 🚀 Quick Start

### 1. Clone
```bash
git clone https://github.com/YOUR_USERNAME/ai-video-generator-android.git
cd ai-video-generator-android
```

### 2. Get API Keys (free tiers available)

| Provider | URL | Used for |
|---|---|---|
| **Stability AI** | [platform.stability.ai](https://platform.stability.ai) | Image-to-video, text-to-image |
| **OpenAI** | [platform.openai.com](https://platform.openai.com) | DALL-E image generation |

### 3. Build & Run
```bash
./gradlew assembleDebug
# or open in Android Studio → Run
```

### 4. Enter API keys in the app
Open the app → **Settings** tab → paste your API keys → **Save Keys**

> Keys are stored encrypted with DataStore and never leave your device.

---

## 🔧 Configuration

Add keys to `local.properties` (gitignored) for build-time injection:

```properties
STABILITY_AI_KEY=sk-your-stability-key-here
OPENAI_API_KEY=sk-your-openai-key-here
```

Or enter them at runtime in the **Settings** screen.

---

## 🎬 How It Works

### Text → Video
1. Enter a descriptive prompt
2. Choose style, resolution, duration
3. App calls **Stability AI text-to-image** to generate a source frame
4. Source frame goes to **Stability AI image-to-video** for animation
5. Video is saved locally and shown in gallery

### Image → Video
1. Pick images from your gallery
2. Tune motion strength and CFG scale
3. App uploads your image directly to **Stability AI image-to-video**
4. Polls for completion (typically 30–120 seconds)
5. Video saved and ready to play/share

### Local (Offline)
No API key needed — creates a slideshow with Ken Burns-style effects using Android's **MediaCodec** hardware encoder.

---

## 📱 Minimum Requirements

- Android 7.0 (API 24)+
- 2 GB RAM recommended
- Internet connection (for AI providers)

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

## 🙏 Credits

Built with:
- [Stability AI](https://stability.ai) — Stable Video Diffusion
- [OpenAI](https://openai.com) — DALL-E 3
- [ExoPlayer / Media3](https://developer.android.com/guide/topics/media/media3)
- [Glide](https://bumptech.github.io/glide/)
- [Hilt](https://dagger.dev/hilt/)
