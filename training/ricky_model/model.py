from __future__ import annotations

import math
import torch
from torch import nn


class CausalSelfAttention(nn.Module):
    def __init__(self, width: int, heads: int, dropout: float = 0.1):
        super().__init__()
        if width % heads:
            raise ValueError("width must be divisible by heads")
        self.heads = heads
        self.head_dim = width // heads
        self.qkv = nn.Linear(width, width * 3)
        self.proj = nn.Linear(width, width)
        self.attn_drop = nn.Dropout(dropout)
        self.resid_drop = nn.Dropout(dropout)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        batch, length, width = x.shape
        qkv = self.qkv(x).view(batch, length, 3, self.heads, self.head_dim)
        q, k, v = qkv.permute(2, 0, 3, 1, 4).unbind(0)
        scores = (q @ k.transpose(-2, -1)) / math.sqrt(self.head_dim)
        mask = torch.triu(torch.ones(length, length, device=x.device, dtype=torch.bool), diagonal=1)
        scores = scores.masked_fill(mask, torch.finfo(scores.dtype).min)
        weights = torch.softmax(scores, dim=-1)
        weights = self.attn_drop(weights)
        out = (weights @ v).transpose(1, 2).contiguous().view(batch, length, width)
        return self.resid_drop(self.proj(out))


class Block(nn.Module):
    def __init__(self, width: int, heads: int, dropout: float):
        super().__init__()
        self.norm1 = nn.LayerNorm(width)
        self.attn = CausalSelfAttention(width, heads, dropout)
        self.norm2 = nn.LayerNorm(width)
        self.ff = nn.Sequential(
            nn.Linear(width, 4 * width), nn.GELU(), nn.Linear(4 * width, width), nn.Dropout(dropout)
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        x = x + self.attn(self.norm1(x))
        return x + self.ff(self.norm2(x))


class RickyGPT(nn.Module):
    """Small educational decoder-only transformer initialized from random weights."""
    def __init__(self, vocab_size: int, width: int = 128, layers: int = 4,
                 heads: int = 4, max_seq_len: int = 256, dropout: float = 0.1):
        super().__init__()
        self.max_seq_len = max_seq_len
        self.token_emb = nn.Embedding(vocab_size, width)
        self.pos_emb = nn.Embedding(max_seq_len, width)
        self.drop = nn.Dropout(dropout)
        self.blocks = nn.ModuleList([Block(width, heads, dropout) for _ in range(layers)])
        self.norm = nn.LayerNorm(width)
        self.lm_head = nn.Linear(width, vocab_size, bias=False)
        self.lm_head.weight = self.token_emb.weight
        self.apply(self._init_weights)

    @staticmethod
    def _init_weights(module: nn.Module) -> None:
        if isinstance(module, nn.Linear):
            nn.init.normal_(module.weight, mean=0.0, std=0.02)
            if module.bias is not None:
                nn.init.zeros_(module.bias)
        elif isinstance(module, nn.Embedding):
            nn.init.normal_(module.weight, mean=0.0, std=0.02)

    def forward(self, input_ids: torch.Tensor) -> torch.Tensor:
        _, length = input_ids.shape
        if length > self.max_seq_len:
            input_ids = input_ids[:, -self.max_seq_len:]
            length = input_ids.shape[1]
        positions = torch.arange(length, device=input_ids.device)
        x = self.drop(self.token_emb(input_ids) + self.pos_emb(positions)[None, :, :])
        for block in self.blocks:
            x = block(x)
        return self.lm_head(self.norm(x))
