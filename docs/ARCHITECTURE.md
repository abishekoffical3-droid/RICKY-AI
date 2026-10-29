# RICKY AI architecture

## Local-first principle

The app now defaults to `LOCAL_ASSISTANT`. It does not require a cloud API key for the built-in offline calculator, local notes/memory, voice features supported by Android, or the rule-based fallback.

When trained model assets are supplied under `app/src/main/assets/ricky/`, `OnDeviceModelRunner` uses ONNX Runtime for local inference. The exported model is a tiny character-level transformer trained from random initialization in `training/`. Without those files, the app tells the truth and uses its built-in fallback rather than pretending a trained model exists.

## Online features are separate

Live web search needs an internet connection and a search service. Cloud AI providers need their own credentials. Those features are optional and must not be confused with offline model inference. Do not put real API keys in GitHub commits or ship private service keys in an APK.

## Current limitations

- Starter training corpus is a demo only; it cannot produce high-quality general conversation.
- Local ONNX generation uses greedy decoding and is intended as a small reference implementation.
- No trained model weights are included in this repository because a checkpoint must be trained from a chosen dataset.
- Android build is validated by the GitHub Actions workflow; if CI fails, inspect the exact log before claiming a successful APK build.
