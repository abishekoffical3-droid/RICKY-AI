import unittest
import torch
from ricky_model.model import RickyGPT

class ModelTests(unittest.TestCase):
    def test_logits_shape(self):
        model = RickyGPT(vocab_size=20, width=32, layers=2, heads=4, max_seq_len=16, dropout=0.0)
        output = model(torch.randint(0, 20, (2, 8)))
        self.assertEqual(tuple(output.shape), (2, 8, 20))

    def test_truncates_long_context(self):
        model = RickyGPT(vocab_size=20, width=32, layers=2, heads=4, max_seq_len=8, dropout=0.0)
        output = model(torch.randint(0, 20, (1, 12)))
        self.assertEqual(tuple(output.shape), (1, 8, 20))

if __name__ == "__main__":
    unittest.main()
