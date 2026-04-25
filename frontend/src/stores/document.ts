import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export interface LegalDocument {
  id: number
  title: string
  docType: string
  domain: string
  facts: string
  claims: string
  content: string
  status: string
  model: string
  createdAt: string
}

export const useDocumentStore = defineStore('document', () => {
  const documents = ref<LegalDocument[]>([])
  const currentDoc = ref<LegalDocument | null>(null)
  const loading = ref(false)

  async function generateDocument(title: string, docType: string, domain: string, facts: string, claims: string) {
    const res = await api.post('/documents', { title, docType, domain, facts, claims })
    return res.data as LegalDocument
  }

  async function getDocument(id: number) {
    loading.value = true
    try {
      const res = await api.get(`/documents/${id}`)
      currentDoc.value = res.data as LegalDocument
      return currentDoc.value
    } finally {
      loading.value = false
    }
  }

  async function loadDocuments(docType?: string) {
    loading.value = true
    try {
      const params: Record<string, string> = {}
      if (docType) params.docType = docType
      const res = await api.get('/documents', { params })
      documents.value = (res.data as LegalDocument[]) || []
    } finally {
      loading.value = false
    }
  }

  return { documents, currentDoc, loading, generateDocument, getDocument, loadDocuments }
})
