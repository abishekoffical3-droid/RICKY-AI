"""Regression tests for dataset parsing and training helpers."""
import json
import tempfile
import unittest
from pathlib import Path

import torch

from train import build_vocab, encode, get_batch, read_records


class DatasetTests(unittest.TestCase):
    def test_reads_prompt_response_and_text_records(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "data.jsonl"
            path.write_text(
                json.dumps({"prompt": "Hi", "response": "Hello"}) + "\n" +
                json.dumps({"text": "User: test"}) + "\n",
                encoding="utf-8",
            )
            records = read_records(path)
        self.assertEqual(len(records), 2)
        self.assertIn("User: Hi", records[0]["text"])
        self.assertEqual(records[1]["text"], "User: test")

    def test_reports_bad_json_with_line_number(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "bad.jsonl"
            path.write_text('{bad json}\n', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "line 1"):
                read_records(path)

    def test_reports_invalid_record_shape(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "bad.jsonl"
            path.write_text('{"question":"Hi"}\n', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "needs text or prompt"):
                read_records(path)

    def test_empty_dataset_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "empty.jsonl"
            path.write_text('\n  \n', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "No training records"):
                read_records(path)

    def test_vocab_has_reserved_tokens_and_round_trip(self):
        vocab = build_vocab(["abc", "cde"])
        self.assertEqual(vocab[:3], ["<pad>", "<unk>", "<eos>"])
        mapping = {token: index for index, token in enumerate(vocab)}
        self.assertEqual(len(encode("abc", mapping)), 3)
        self.assertEqual(encode("?", mapping), [mapping["<unk>"]])

    def test_batch_shapes_are_correct_even_for_short_data(self):
        x, y = get_batch(torch.tensor([1, 2, 3]), block_size=8, batch_size=2, device=torch.device("cpu"))
        self.assertEqual(tuple(x.shape), (2, 8))
        self.assertEqual(tuple(y.shape), (2, 8))
        self.assertTrue(torch.equal(x[:, 1:], y[:, :-1]))


if __name__ == "__main__":
    unittest.main()
