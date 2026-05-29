"""
Pydantic models for GPU inference service
"""

from typing import List, Optional
from pydantic import BaseModel, Field


class RerankRequest(BaseModel):
    """Request model for reranking"""
    query: str = Field(..., description="Search query")
    documents: List[str] = Field(..., description="List of documents to rerank")
    top_k: int = Field(default=5, description="Number of top results to return")


class RerankResult(BaseModel):
    """Single rerank result"""
    index: int = Field(..., description="Original document index")
    score: float = Field(..., description="Relevance score")
    document: str = Field(..., description="Document text")


class RerankResponse(BaseModel):
    """Response model for reranking"""
    results: List[RerankResult] = Field(..., description="Reranked results")
    model: str = Field(..., description="Model used for reranking")


class EmbedRequest(BaseModel):
    """Request model for embedding"""
    texts: List[str] = Field(..., description="List of texts to embed")


class EmbedResponse(BaseModel):
    """Response model for embedding"""
    embeddings: List[List[float]] = Field(..., description="Embedding vectors")
    model: str = Field(..., description="Model used for embedding")
    dimension: int = Field(..., description="Embedding dimension")


class HealthResponse(BaseModel):
    """Health check response"""
    status: str = Field(..., description="Service status")
    reranker_ready: bool = Field(..., description="Whether reranker is ready")
    embedding_ready: bool = Field(..., description="Whether embedding service is ready")


class ErrorResponse(BaseModel):
    """Error response"""
    error: str = Field(..., description="Error message")
    detail: Optional[str] = Field(None, description="Error details")
