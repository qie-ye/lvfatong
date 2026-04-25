import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export type FeedbackRating = 'GOOD' | 'BAD'

export const useFeedbackStore = defineStore('feedback', () => {
  // key: `${sessionId}-${messageIndex}` → rating
  const feedbackMap = ref<Map<string, FeedbackRating>>(new Map())

  function feedbackKey(sessionId: number, messageIndex: number): string {
    return `${sessionId}-${messageIndex}`
  }

  function getRating(sessionId: number, messageIndex: number): FeedbackRating | undefined {
    return feedbackMap.value.get(feedbackKey(sessionId, messageIndex))
  }

  function hasRating(sessionId: number, messageIndex: number): boolean {
    return feedbackMap.value.has(feedbackKey(sessionId, messageIndex))
  }

  async function submitFeedback(sessionId: number, messageIndex: number, rating: FeedbackRating) {
    const key = feedbackKey(sessionId, messageIndex)
    if (feedbackMap.value.has(key)) return

    await api.post('/feedback', { sessionId, messageIndex, rating })
    feedbackMap.value.set(key, rating)
  }

  return { feedbackMap, getRating, hasRating, submitFeedback }
})
