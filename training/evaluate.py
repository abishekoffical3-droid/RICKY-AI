"""Evaluate a saved RICKY checkpoint on a JSONL dataset using next-token loss.

This metric is a useful regression signal, not proof of conversational quality.
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

import torch
import torch.nn.functional as F

from ricky_model.model import RickyGPT
from train import read_records


def evaluate(checkpoint_path: Path, vocab_path: Path, data_path: Path) -> tuple[float, float, int]:
    checkpoint = torch.load(checkpoint_path, map_location="cpu", weights_only=False)
    vocab_data = json.loads(vocab_path.read_text(encoding="utf-8"))
    vocab = vocab_data.get("tokens")
    if not isinstance(vocab, list) or not vocab:
        raise ValueError("Vocabulary JSON must contain a non-empty 'tokens' list")
    token_to_id = {str(token): i for i, token in enumerate(vocab)}
    unk_id = int(vocab_data.get("unk_token_id", 1))
    eos_id = int(vocab_data.get("eos_token_id", 2))
    records = read_records(data_path)
    ids: list[int] = []
    for record in records:
        ids.extend(token_to_id.get(char, unk_id) for char in record["text"])
        ids.append(eos_id)
    if len(ids) < 2:
        raise ValueError("Evaluation dataset must contain at least two tokens")

    model = RickyGPT(**checkpoint["model_config"])
    model.load_state_dict(checkpoint["model_state_dict"])
    model.eval()
    block_size = int(checkpoint["model_config"]["max_seq_len"])
    total_loss = 0.0
    total_tokens = 0
    with torch.no_grad():
        for start in range(0, len(ids) - 1, block_size):
            chunk = ids[start:start + block_size + 1]
            if len(chunk) < 2:
                continue
            x = torch.tensor(chunk[:-1], dtype=torch.long).unsqueeze(0)
            y = torch.tensor(chunk[1:], dtype=torch.long).unsqueeze(0)
            logits = model(x)
            loss = F.cross_entropy(logits.reshape(-1, logits.size(-1)), y.reshape(-1), reduction="sum")
            total_loss += float(loss.item())
            total_tokens += y.numel()
    if total_tokens == 0:
        raise ValueError("No evaluation tokens were produced")
    mean_loss = total_loss / total_tokens
    perplexity = math.exp(min(mean_loss, 80.0))
    return mean_loss, perplexity, total_tokens


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checkpoint", type=Path, default=Path("artifacts/ricky_checkpoint.pt"))
    parser.add_argument("--vocab", type=Path, default=Path("artifacts/vocab.json"))
    parser.add_argument("--data", type=Path, required=True, help="JSONL evaluation dataset")
    args = parser.parse_args()
    for path in (args.checkpoint, args.vocab, args.data):
        if not path.is_file():
            parser.error(f"file not found: {path}")
    try:
        loss, ppl, tokens = evaluate(args.checkpoint, args.vocab, args.data)
    except (ValueError, KeyError, RuntimeError, json.JSONDecodeError) as exc:
        parser.error(str(exc))
    print(f"evaluation_tokens={tokens}")
    print(f"mean_next_token_loss={loss:.4f}")
    print(f"perplexity={ppl:.2f}")
    print("Interpretation: compare runs on the same held-out dataset; lower is generally better, but this does not measure factuality or helpfulness.")


if __name__ == "__main__":
    main()
