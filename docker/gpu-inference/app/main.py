"""
GPU Inference Service for Reranker and Embedding
Provides REST API for legal document reranking and text embedding
"""

import logging
from contextlib import asynccontextmanager
from typing import List, Optional

import yaml
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from app.reranker import RerankerService
from app.embedding import EmbeddingService

# Load config
with open("config.yaml", "r") as f:
    config = yaml.safe_load(f)

# Configure logging
logging.basicConfig(
    level=getattr(logging, config["logging"]["level"]),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

# Global services
reranker_service: Optional[RerankerService] = None
embedding_service: Optional[EmbeddingService] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Initialize models on startup"""
    global reranker_service, embedding_service

    logger.info("Initializing GPU inference services...")

    try:
        reranker_config = config["models"]["reranker"]
        reranker_service = RerankerService(
            model_name=reranker_config["name"],
            model_path=reranker_config["path"],
            device=reranker_config["device"],
            max_length=reranker_config["max_length"]
        )
        logger.info("Reranker service initialized")
    except Exception as e:
        logger.error(f"Failed to initialize reranker: {e}")
        reranker_service = None

    try:
        embedding_config = config["models"]["embedding"]
        embedding_service = EmbeddingService(
            model_name=embedding_config["name"],
            model_path=embedding_config["path"],
            device=embedding_config["device"],
            max_length=embedding_config["max_length"]
        )
        logger.info("Embedding service initialized")
    except Exception as e:
        logger.error(f"Failed to initialize embedding: {e}")
        embedding_service = None

    yield

    logger.info("Shutting down GPU inference services...")


app = FastAPI(
    title="LvFaTong GPU Inference Service",
    description="Reranker and Embedding service for legal document retrieval",
    version="1.0.0",
    lifespan=lifespan
)


# Request/Response models
class RerankRequest(BaseModel):
    query: str
    documents: List[str]
    top_k: int = 5


class RerankResult(BaseModel):
    index: int
    score: float
    document: str


class RerankResponse(BaseModel):
    results: List[RerankResult]
    model: str


class EmbedRequest(BaseModel):
    texts: List[str]


class EmbedResponse(BaseModel):
    embeddings: List[List[float]]
    model: str
    dimension: int


class HealthResponse(BaseModel):
    status: str
    reranker_ready: bool
    embedding_ready: bool


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint"""
    return HealthResponse(
        status="ok",
        reranker_ready=reranker_service is not None,
        embedding_ready=embedding_service is not None
    )


@app.post("/rerank", response_model=RerankResponse)
async def rerank(request: RerankRequest):
    """Rerank documents based on query relevance"""
    if reranker_service is None:
        raise HTTPException(status_code=503, detail="Reranker service not available")

    try:
        results = reranker_service.rerank(
            query=request.query,
            documents=request.documents,
            top_k=request.top_k
        )

        return RerankResponse(
            results=[
                RerankResult(index=r["index"], score=r["score"], document=r["document"])
                for r in results
            ],
            model=config["models"]["reranker"]["name"]
        )
    except Exception as e:
        logger.error(f"Rerank failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/embed", response_model=EmbedResponse)
async def embed(request: EmbedRequest):
    """Generate embeddings for texts"""
    if embedding_service is None:
        raise HTTPException(status_code=503, detail="Embedding service not available")

    try:
        embeddings = embedding_service.embed(request.texts)

        return EmbedResponse(
            embeddings=embeddings,
            model=config["models"]["embedding"]["name"],
            dimension=len(embeddings[0]) if embeddings else 0
        )
    except Exception as e:
        logger.error(f"Embedding failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/embed/query")
async def embed_query(query: str):
    """Generate embedding for a single query"""
    if embedding_service is None:
        raise HTTPException(status_code=503, detail="Embedding service not available")

    try:
        embedding = embedding_service.embed_query(query)
        return {"embedding": embedding, "model": config["models"]["embedding"]["name"]}
    except Exception as e:
        logger.error(f"Query embedding failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=config["server"]["host"],
        port=config["server"]["port"],
        reload=False
    )
