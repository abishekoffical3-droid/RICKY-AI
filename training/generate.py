from __future__ import annotations
import argparse, json
from pathlib import Path
import torch
from ricky_model.model import RickyGPT

p = argparse.ArgumentParser(description="Quickly sample text from a trained RICKY checkpoint.")
p.add_argument("--checkpoint", type=Path, default=Path("artifacts/ricky_checkpoint.pt"))
p.add_argument("--vocab", type=Path, default=Path("artifacts/vocab.json"))
p.add_argument("--prompt", default="User: Who are you?\nRICKY:")
p.add_argument("--tokens", type=int, default=180)
p.add_argument("--temperature", type=float, default=0.8)
a = p.parse_args()
if a.tokens < 0:
    p.error("--tokens must be zero or greater")
if a.temperature <= 0:
    p.error("--temperature must be greater than zero")
if not a.checkpoint.is_file():
    p.error(f"checkpoint file not found: {a.checkpoint}")
if not a.vocab.is_file():
    p.error(f"vocabulary file not found: {a.vocab}")
ckpt = torch.load(a.checkpoint, map_location="cpu", weights_only=False)
vocab_data = json.loads(a.vocab.read_text(encoding="utf-8"))
vocab = vocab_data["tokens"]
to_id = {t: i for i, t in enumerate(vocab)}
ids = [to_id.get(ch, vocab_data["unk_token_id"]) for ch in a.prompt]
model = RickyGPT(**ckpt["model_config"])
model.load_state_dict(ckpt["model_state_dict"])
model.eval()
for _ in range(a.tokens):
    inp = torch.tensor([ids[-model.max_seq_len:]], dtype=torch.long)
    with torch.no_grad(): logits = model(inp)[0, -1]
    logits = logits / max(a.temperature, 0.05)
    next_id = int(torch.multinomial(torch.softmax(logits, dim=-1), 1).item())
    if next_id == vocab_data["eos_token_id"]: break
    ids.append(next_id)
print("".join(vocab[i] if i >= 3 else "" for i in ids[len([to_id.get(ch, 1) for ch in a.prompt]):]))
