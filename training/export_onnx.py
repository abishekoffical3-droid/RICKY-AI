from __future__ import annotations
import argparse
import json
from pathlib import Path
import torch
from ricky_model.model import RickyGPT

parser = argparse.ArgumentParser(description="Export a trained RICKY checkpoint to ONNX for Android ONNX Runtime.")
parser.add_argument("--checkpoint", type=Path, default=Path("artifacts/ricky_checkpoint.pt"))
parser.add_argument("--vocab", type=Path, default=Path("artifacts/vocab.json"))
parser.add_argument("--out", type=Path, default=Path("artifacts/model.onnx"))
args = parser.parse_args()
checkpoint = torch.load(args.checkpoint, map_location="cpu", weights_only=False)
config = checkpoint["model_config"]
model = RickyGPT(**config)
model.load_state_dict(checkpoint["model_state_dict"])
model.eval()
args.out.parent.mkdir(parents=True, exist_ok=True)
example = torch.ones((1, min(8, config["max_seq_len"])), dtype=torch.long)
torch.onnx.export(
    model, (example,), str(args.out), input_names=["input_ids"], output_names=["logits"],
    dynamic_axes={"input_ids": {0: "batch", 1: "sequence"}, "logits": {0: "batch", 1: "sequence"}},
    opset_version=17, do_constant_folding=True, dynamo=False,
)
# Validate artifact metadata is available alongside the ONNX file.
metadata = {"model_type": "ricky-char-gpt", "max_seq_len": config["max_seq_len"],
            "vocab_size": config["vocab_size"], "vocab_file": args.vocab.name}
args.out.with_suffix(".metadata.json").write_text(json.dumps(metadata, indent=2), encoding="utf-8")
print(f"Exported {args.out} ({args.out.stat().st_size:,} bytes)")
