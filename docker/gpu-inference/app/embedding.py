"""
Embedding Service using BGE-M3
"""

import logging
from typing import List
from pathlib import Path

import torch
from sentence_transformers import SentenceTransformer

logger = logging.getLogger(__name__)


class EmbeddingService:
    """Embedding service using BGE-M3 model"""

    def __init__(
        self,
        model_name: str = "BAAI/bge-m3",
        model_path: str = None,
        device: str = "cuda:0",
        max_length: int = 8192
    ):
        self.device = device
        self.max_length = max_length
        self.model_name = model_name

        # Try to load from local path first, then from model name
        load_path = model_path if Path(model_path).exists() else model_name

        logger.info(f"Loading embedding model from {load_path}")

        try:
            self.model = SentenceTransformer(
                load_path,
                device=device
            )
            self.dimension = self.model.get_sentence_embedding_dimension()
            logger.info(f"Embedding model loaded successfully on {device}, dimension={self.dimension}")
        except Exception as e:
            logger.error(f"Failed to load embedding model: {e}")
            raise

    def embed(self, texts: List[str]) -> List[List[float]]:
        """
        Generate embeddings for a list of texts

        Args:
            texts: List of text strings

        Returns:
            List of embedding vectors
        """
        if not texts:
            return []

        try:
            embeddings = self.model.encode(
                texts,
                show_progress_bar=False,
                normalize_embeddings=True,
                max_length=self.max_length
            )

            return embeddings.tolist()
        except Exception as e:
            logger.error(f"Embedding generation failed: {e}")
            raise

    def embed_query(self, query: str) -> List[float]:
        """
        Generate embedding for a single query

        Args:
            query: Query string

        Returns:
            Embedding vector
        """
        try:
            embedding = self.model.encode(
                [query],
                show_progress_bar=False,
                normalize_embeddings=True,
                max_length=self.max_length
            )

            return embedding[0].tolist()
        except Exception as e:
            logger.error(f"Query embedding generation failed: {e}")
            raise
