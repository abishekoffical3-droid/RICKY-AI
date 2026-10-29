# RICKY AI quality checks

## Checks run while preparing this archive

- Python unit tests: 8 passed (`python -m unittest discover -v` from `training/`).
- Python syntax check: passed (`python -m compileall -q training`).
- CPU training smoke test: passed (2 optimization steps with a tiny model).
- Held-out evaluation smoke test: passed; the evaluator printed loss/perplexity and token count.
- All JSONL lines parsed as valid JSON.
- Gradle version catalog parsed as valid TOML.
- Android manifest parsed as valid XML.
- CI workflow contains Android unit tests + debug APK build, Python regression tests, training smoke test, held-out evaluation, and ONNX export validation.

## Checks not run in this preparation environment

The Android APK build and ONNX export could not be executed here because Gradle/Android SDK and the Python `onnx` package are not installed, and this environment cannot download missing packages. GitHub Actions is configured to run these checks after the project is pushed. A successful local Python smoke test is not a substitute for a successful Android CI run.

## Model-quality limitation

The bundled 97-example training file is a starter corpus, not a production language-model dataset. The scratch model starts from random weights and is character-level. It will not behave like ChatGPT after a short training run. Meaningful quality requires a much larger legally usable corpus, appropriate tokenizer/model design, sufficient compute, and held-out evaluation for factuality and helpfulness.
