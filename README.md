# RICKY AI — local-first Android assistant + scratch model lab

RICKY AI is an Android Compose assistant project with a local-first default, offline tools, local memory, optional voice, optional web research, optional cloud providers, and a separate training workspace for a small model trained from random initialization.

> **Honest status:** the app code is not itself a trained AI model. The starter fallback is rule-based. Train a model in `training/`, export it to ONNX, and add the resulting files to `app/src/main/assets/ricky/` to enable the optional on-device neural inference path. The starter dataset is only for demonstration and will not produce ChatGPT-level answers.

## What is included

- Android app built with Kotlin + Jetpack Compose
- Local-first provider selected by default; no API key required to open the app's offline mode
- Existing local tools, chat history/memory, calculator, voice UI, and optional web research features
- Optional ONNX Runtime adapter for a user-trained model on-device
- `training/`: PyTorch decoder-only transformer, multilingual educational starter examples, JSONL validation, training script, checkpoint evaluator, sampling script, ONNX export script, and regression tests
- `.github/workflows/android.yml`: Android build/test workflow and model-lab checks; debug APK is uploaded as a workflow artifact after a successful build

## Build the Android app with GitHub Actions

1. Create a new GitHub repository and upload/push the contents of this project (the files inside the project folder, not the ZIP file itself).
2. Open **Actions** and allow workflows if GitHub asks.
3. Open the **Android CI** workflow and run it, or push a commit to `main` / `master`.
4. If successful, open the workflow run and download the `ricky-ai-debug-apk` artifact.
5. If it fails, open the failed step and share the first relevant error lines. This repository snapshot has no Gradle wrapper JAR, so CI uses a pinned Gradle installation rather than `./gradlew`.

No API key is required for the local-first debug build. The workflow generates the debug keystore required by this AI Studio project. For a local build, generate it from the project root with `keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -dname "CN=Android Debug,O=Android,C=US" -keyalg RSA -keysize 2048 -validity 10000`. Optional online provider features remain unavailable until configured. Never commit real secrets.

## Train RICKY from scratch

Read [`training/README.md`](training/README.md). The smallest smoke test is:

```bash
cd training
python -m pip install -r requirements.txt
python train.py --steps 20 --batch-size 2 --block-size 64 --width 32 --layers 2 --heads 4
```

Then export the checkpoint with `python export_onnx.py` and copy the generated model/vocabulary/metadata into `app/src/main/assets/ricky/` as described in the training guide.

## Repository structure

```text
app/                         Android application
training/                    Scratch-training + ONNX export pipeline
training/datasets/           Tiny starter dataset (demo only)
docs/ARCHITECTURE.md          Architecture and current limitations
.github/workflows/android.yml CI build, tests, and APK artifact upload
```

## Important limitations

- The included educational dataset is still small and only suitable for smoke tests and experiments; it is not a production-grade pretraining corpus.
- Training from scratch does not automatically make a model smart. Quality depends on corpus size/quality, tokenizer, architecture, training time, and compute.
- Web search requires internet and a search backend; it cannot operate live while fully offline.
- Do not store real API keys in source code or public repositories.

## License

Add a license that matches the rights for your own code and training data before distributing the project.
