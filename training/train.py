from __future__ import annotations

import argparse
import json
import random
from pathlib import Path

import torch
import torch.nn.functional as F

from ricky_model.model import RickyGPT

ROOT = Path(__file__).resolve().parent
DEFAULT_DATA = ROOT / "datasets" / "starter.jsonl"
DEFAULT_OUT = ROOT / "artifacts"
RESERVED = ["<pad>", "<unk>", "<eos>"]


def read_records(path: Path) -> list[dict[str, str]]:
    records: list[dict[str, str]] = []
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip():
            continue
        try:
            item = json.loads(line)
        except json.JSONDecodeError as exc:
            raise ValueError(f"Invalid JSON on line {line_number}: {exc}") from exc
        if isinstance(item.get("text"), str):
            text = item["text"]
        elif isinstance(item.get("prompt"), str) and isinstance(item.get("response"), str):
            text = f"User: {item['prompt']}\nRICKY: {item['response']}\n"
        else:
            raise ValueError(f"Line {line_number} needs text or prompt + response fields")
        records.append({"text": text})
    if not records:
        raise ValueError(f"No training records found in {path}")
    return records


def build_vocab(texts: list[str]) -> list[str]:
    chars = sorted(set("".join(texts)))
    return RESERVED + [char for char in chars if char not in RESERVED]


def encode(text: str, token_to_id: dict[str, int]) -> list[int]:
    unk = token_to_id["<unk>"]
    return [token_to_id.get(char, unk) for char in text]


def get_batch(data: torch.Tensor, block_size: int, batch_size: int, device: torch.device):
    if len(data) <= block_size:
        repeated = data.repeat((block_size // max(1, len(data))) + 2)
        data = repeated
    starts = torch.randint(0, len(data) - block_size - 1, (batch_size,))
    x = torch.stack([data[int(i):int(i) + block_size] for i in starts])
    y = torch.stack([data[int(i) + 1:int(i) + block_size + 1] for i in starts])
    return x.to(device), y.to(device)


def main() -> None:
    parser = argparse.ArgumentParser(description="Train a small RICKY language model from random initialization.")
    parser.add_argument("--data", type=Path, default=DEFAULT_DATA)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    parser.add_argument("--steps", type=int, default=1000)
    parser.add_argument("--batch-size", type=int, default=16)
    parser.add_argument("--block-size", type=int, default=128)
    parser.add_argument("--width", type=int, default=128)
    parser.add_argument("--layers", type=int, default=4)
    parser.add_argument("--heads", type=int, default=4)
    parser.add_argument("--lr", type=float, default=3e-4)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    # Fail early with clear messages instead of obscure tensor/optimizer errors.
    if args.steps < 1:
        parser.error("--steps must be at least 1")
    if args.batch_size < 1:
        parser.error("--batch-size must be at least 1")
    if args.block_size < 2:
        parser.error("--block-size must be at least 2")
    if args.width < 4:
        parser.error("--width must be at least 4")
    if args.layers < 1:
        parser.error("--layers must be at least 1")
    if args.heads < 1:
        parser.error("--heads must be at least 1")
    if args.width % args.heads != 0:
        parser.error("--width must be divisible by --heads")
    if args.lr <= 0:
        parser.error("--lr must be greater than 0")
    if not args.data.is_file():
        parser.error(f"dataset file does not exist: {args.data}")

    random.seed(args.seed)
    torch.manual_seed(args.seed)
    args.out.mkdir(parents=True, exist_ok=True)
    records = read_records(args.data)
    vocab = build_vocab([record["text"] for record in records])
    token_to_id = {token: idx for idx, token in enumerate(vocab)}
    ids: list[int] = []
    for record in records:
        ids.extend(encode(record["text"], token_to_id))
        ids.append(token_to_id["<eos>"])
    data = torch.tensor(ids, dtype=torch.long)
    if len(data) < args.block_size + 2:
        data = data.repeat((args.block_size + 2) // len(data) + 2)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = RickyGPT(len(vocab), width=args.width, layers=args.layers, heads=args.heads,
                     max_seq_len=args.block_size, dropout=0.1).to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr)
    print(f"device={device} examples={len(records)} tokens={len(data)} vocab={len(vocab)}")
    print(f"parameters={sum(p.numel() for p in model.parameters()):,}; training from random initialization")

    model.train()
    for step in range(1, args.steps + 1):
        x, y = get_batch(data, args.block_size, args.batch_size, device)
        logits = model(x)
        loss = F.cross_entropy(logits.reshape(-1, logits.size(-1)), y.reshape(-1))
        optimizer.zero_grad(set_to_none=True)
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
        optimizer.step()
        if step == 1 or step % 100 == 0 or step == args.steps:
            print(f"step={step}/{args.steps} loss={loss.item():.4f}")

    model.eval()
    checkpoint = {
        "model_state_dict": model.cpu().state_dict(),
        "model_config": {"vocab_size": len(vocab), "width": args.width, "layers": args.layers,
                         "heads": args.heads, "max_seq_len": args.block_size, "dropout": 0.1},
        "training": {"steps": args.steps, "seed": args.seed, "data_file": str(args.data.name)},
    }
    torch.save(checkpoint, args.out / "ricky_checkpoint.pt")
    (args.out / "vocab.json").write_text(json.dumps({"tokens": vocab, "pad_token_id": 0,
        "unk_token_id": 1, "eos_token_id": 2}, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Saved checkpoint and vocabulary to {args.out}")
    print("Next: python export_onnx.py --checkpoint artifacts/ricky_checkpoint.pt --vocab artifacts/vocab.json --out artifacts/model.onnx")
    print("Reminder: starter.jsonl is tiny demo data; use a much larger, clean, legally usable dataset for meaningful quality.")


if __name__ == "__main__":
    main()
