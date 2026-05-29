"""
Reranker Service using BGE-Reranker-v2-m3
"""

import logging
from typing import List, Dict, Any
from pathlib import Path

import torch
from transformers import AutoModelForSequenceClassification, AutoTokenizer

logger = logging.getLogger(__name__)


class RerankerService:
    """Reranker service using BGE-Reranker-v2-m3 model"""

    def __init__(
        self,
        model_name: str = "BAAI/bge-reranker-v2-m3",
        model_path: str = None,
        device: str = "cuda:0",
        max_length: int = 512
    ):
        self.device = device
        self.max_length = max_length
        self.model_name = model_name

        # Try to load from local path first, then from model name
        load_path = model_path if Path(model_path).exists() else model_name

        logger.info(f"Loading reranker model from {load_path}")

        try:
            self.tokenizer = AutoTokenizer.from_pretrained(load_path)
            self.model = AutoModelForSequenceClassification.from_pretrained(load_path)
            self.model.to(self.device)
            self.model.eval()
            logger.info(f"Reranker model loaded successfully on {device}")
        except Exception as e:
            logger.error(f"Failed to load reranker model: {e}")
            raise

    @torch.no_grad()
    def rerank(
        self,
        query: str,
        documents: List[str],
        top_k: int = 5
    ) -> List[Dict[str, Any]]:
        """
        Rerank documents based on query relevance

        Args:
            query: Search query
            documents: List of document texts
            top_k: Number of top results to return

        Returns:
            List of dicts with index, score, and document
        """
        if not documents:
            return []

        # Prepare pairs for scoring
        pairs = [[query, doc] for doc in documents]

        # Tokenize
        inputs = self.tokenizer(
            pairs,
            padding=True,
            truncation=True,
            max_length=self.max_length,
            return_tensors="pt"
        ).to(self.device)

        # Get scores
        outputs = self.model(**inputs)
        scores = outputs.logits.squeeze(-1).cpu().numpy()

        # Sort by score
        scored_docs = []
        for i, score in enumerate(scores):
            scored_docs.append({
                "index": i,
                "score": float(score),
                "document": documents[i]
            })

        # Sort by score descending
        scored_docs.sort(key=lambda x: x["score"], reverse=True)

        # Return top_k
        return scored_docs[:top_k]

    @torch.no_grad()
    def compute_score(self, query: str, document: str) -> float:
        """Compute relevance score for a single query-document pair"""
        inputs = self.tokenizer(
            [[query, document]],
            padding=True,
            truncation=True,
            max_length=self.max_length,
            return_tensors="pt"
        ).to(self.device)

        outputs = self.model(**inputs)
        score = outputs.logits.squeeze(-1).cpu().item()

        return score
