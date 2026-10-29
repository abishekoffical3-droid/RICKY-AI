# RICKY Model Lab — train from random initialization

This is a **small educational scratch-training pipeline**. It does not download or fine-tune a pretrained model. The weights start randomly initialized. It is designed to demonstrate the full workflow: data → character tokenizer → decoder-only transformer → checkpoint → ONNX export → Android local inference.

## Reality check before training

- `datasets/starter.jsonl` is only a tiny demo dataset. It is not enough for a useful general-purpose assistant.
- The character-level model is deliberately simple and will usually produce weak or repetitive text. It will not match ChatGPT or other large language models.
- Use only data you have rights to use; remove private information and verify data quality.
- The training script uses CPU automatically when CUDA is unavailable. On a small 4 GB RAM / 14 GB disk laptop, use tiny settings and expect slow training. Do not run the default 128-width model on a machine that is already nearly out of storage.
- A checkpoint is a trained artifact, not proof of quality. Evaluate outputs before distributing it.

## 1. Install Python dependencies

Use Python 3.10–3.12 in a virtual environment if possible.

```bash
cd training
python -m venv .venv
# Windows:
.venv\Scripts\activate
# Linux/macOS:
source .venv/bin/activate
python -m pip install -r requirements.txt
```

## 2. Prepare your own JSONL dataset

Each line is one JSON object. Either format is accepted:

```json
{"prompt":"What is a variable?","response":"A variable stores a value under a name."}
{"text":"User: Hello\nRICKY: Hello!\n"}
```

Create a separate file such as `datasets/my_data.jsonl`. Keep examples correct, varied, and in the languages you want RICKY to learn. Do not put API keys, passwords, private chats, or personal data in the dataset.

## 3. Train from scratch

Tiny smoke-test configuration:

```bash
python train.py --data datasets/starter.jsonl --out artifacts --steps 20 --batch-size 2 --block-size 64 --width 32 --layers 2 --heads 4
```

Small learning run (more compute and data needed):

```bash
python train.py --data datasets/my_data.jsonl --out artifacts --steps 2000 --batch-size 8 --block-size 128 --width 128 --layers 4 --heads 4
```

Training reports the loss and writes `artifacts/ricky_checkpoint.pt` and `artifacts/vocab.json`. The included examples are still small; memorization and weak generalization are expected.

## 4. Evaluate a checkpoint

Use a separate held-out JSONL file (do not use the training file if you want a meaningful generalization check):

```bash
python evaluate.py --checkpoint artifacts/ricky_checkpoint.pt --vocab artifacts/vocab.json --data datasets/validation.jsonl
```

The script reports next-token loss and perplexity. These are useful for comparing runs on the same held-out data, but they do not prove that answers are correct or helpful.

## 5. Sample from the checkpoint

```bash
python generate.py --checkpoint artifacts/ricky_checkpoint.pt --vocab artifacts/vocab.json --prompt "User: What is Python?\nRICKY:" --tokens 160
```

## 6. Export to ONNX for Android

```bash
python export_onnx.py --checkpoint artifacts/ricky_checkpoint.pt --vocab artifacts/vocab.json --out artifacts/model.onnx
```

Copy these three files into the Android app assets directory, creating it if needed:

- `artifacts/model.onnx` → `app/src/main/assets/ricky/model.onnx`
- `artifacts/vocab.json` → `app/src/main/assets/ricky/vocab.json`
- `artifacts/model.metadata.json` → `app/src/main/assets/ricky/model.metadata.json`

Then rebuild the Android app. If the assets are absent, RICKY remains in offline rule-fallback mode. If installed, the app attempts local ONNX inference. Keep model files reasonably small; very large models may exceed device RAM and APK size limits.

## Model design

- Decoder-only causal transformer initialized from random weights
- Character-level vocabulary built from the training corpus
- Next-character prediction objective
- PyTorch checkpoint for training/evaluation
- ONNX export with `input_ids` input and `logits` output
- Android ONNX Runtime integration for local autoregressive generation

This is a learning foundation, not a production-ready LLM. For a genuinely capable model trained from scratch, you will need a much larger curated corpus, a better tokenizer, careful evaluation, substantial compute, and likely a larger training setup.
