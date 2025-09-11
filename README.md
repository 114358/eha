# Empowering Healthcare Access

Empowering Healthcare Access or `eha` is an open-source, privacy-first translation app designed to facilitate **real-time
communication between doctors and patients** in multilingual medical environments — especially
**when no human translator is available**.

Built for urgency and privacy, it processes everything **locally on-device**, enabling use in
hospitals, clinics, or field settings where connectivity is limited or sensitive data must remain
private.


## 🩺 Purpose

In Switzerland and many multilingual regions, urgent medical care is often delayed or compromised
due to **language barriers**. `eha` aims to bridge this gap by enabling **instant, offline
translation** between patients and healthcare professionals — without relying on cloud services or
internet access.


## 🎯 Key Features

- **One-tap speech recording**: Patient or doctor presses a button and speaks.
- **On-device processing**:
    - Speech-to-text (ASR using **Vosk**)
    - Translation via local **language model**
    - Text-to-speech (TTS) output in target language
- **Privacy-focused**: All audio, text, and translations are processed locally.


## 🌍 Supported Languages

- English
- French
- German
- Italian
- Turkish
- Arabic

(More languages coming soon.)


## 🚧 Current Status

`eha` is in **early development**. It’s not yet production-ready.

Here’s what’s working:

- Full local loop: ASR → translation → TTS
- Basic UI for recording and playback
- Android compatibility

What needs work:

- UI/UX improvements
- Translation quality (especially for medical terms)
- Better error handling
- Smoother audio playback
---

## 🧭 Roadmap

Planned improvements:

- **Fine-tune ASR and LLM on medical-domain data**
- Evaluate **end-to-end models** like *Gemma3n* that translate audio directly to translated text
- Introduce pre-defined **medical phrasebooks** for common scenarios
- UX/UI improvements tailored for emergency situations (e.g., large buttons, minimal steps)
- Expand supported languages
- Optimize performance for low-end Android devices
- Explore alternatives to Android’s built-in TTS engine for improved quality
- CI/CD and test coverage


## 🛠️ Tech Stack

- **Platform**: Android
- **Language**: Kotlin
- **ASR**: [Vosk](https://alphacephei.com/vosk/)
- **Translation**: Local LLM (planned medical fine-tuning)
- **TTS**: Android's built-in Text-to-Speech (subject to change)


## 🧰 Setup & Development

To get started with `eha` on your local machine:

### 1. Clone the repository

```bash
git clone https://github.com/114358/eha.git
cd eha
git submodule update --init --recursive
```

### 2. Open in Android Studio

- Open Android Studio
- Choose "Open an Existing Project"
- Select the `eha/` folder

Android Studio will automatically sync Gradle and build the project.

### 3. Run the App

- Plug in an Android device (or start an emulator)
- Press ▶️ to build and run the app



## 🤝 Contributing

We welcome all kinds of contributions:

- Feature development
- UX design
- New language support
- Documentation
- Medical phrase suggestions
- Testing and bug reports

Feel free to open issues or submit pull requests!


## 🪪 License

`eha` is released under the **GNU General Public License v3.0 (GPL-3.0)**.  
See [`LICENSE`](./LICENSE) for full terms.


## 📷 Screenshots / Demo

Coming soon — we’re working on a video walkthrough and sample use cases.


## 🙌 Acknowledgements

Inspired by the real-world challenges of providing urgent care in multilingual settings, and by
the people who continue to build accessible, privacy-respecting open source tools.

