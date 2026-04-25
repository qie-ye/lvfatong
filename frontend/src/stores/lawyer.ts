import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export interface LawyerProfile {
  id: number
  userId: number
  realName: string
  lawFirm: string
  licenseNo: string
  bio: string
  education: string
  specialties: string[]
  tags: string[]
  province: string
  city: string
  yearsOfExperience: number
  rating: number
  consultationCount: number
  verified: boolean
  available: boolean
  consultationType: string
}

export interface Appointment {
  id: number
  userId: number
  lawyerId: number
  lawyerName: string
  status: string
  appointmentTime: string
  consultationType: string
  description: string
  cancelReason: string
  createdAt: string
}

export const useLawyerStore = defineStore('lawyer', () => {
  const lawyers = ref<LawyerProfile[]>([])
  const currentLawyer = ref<LawyerProfile | null>(null)
  const appointments = ref<Appointment[]>([])
  const totalLawyers = ref(0)
  const loading = ref(false)

  async function listLawyers(page = 0, size = 10) {
    loading.value = true
    try {
      const res = await api.get('/lawyers', { params: { page, size } })
      const data = res.data as { content: LawyerProfile[]; totalElements: number }
      lawyers.value = data.content || []
      totalLawyers.value = data.totalElements || 0
    } finally {
      loading.value = false
    }
  }

  async function searchLawyers(keyword?: string, specialty?: string, page = 0, size = 10) {
    loading.value = true
    try {
      const res = await api.get('/lawyers/search', { params: { keyword, specialty, page, size } })
      const data = res.data as { content: LawyerProfile[]; totalElements: number }
      lawyers.value = data.content || []
      totalLawyers.value = data.totalElements || 0
    } finally {
      loading.value = false
    }
  }

  async function getLawyer(id: number) {
    const res = await api.get(`/lawyers/${id}`)
    currentLawyer.value = res.data as LawyerProfile
    return currentLawyer.value
  }

  async function createAppointment(lawyerId: number, appointmentTime: string, consultationType?: string, description?: string) {
    const res = await api.post('/lawyers/appointments', {
      lawyerId, appointmentTime, consultationType, description
    })
    return res.data as Appointment
  }

  async function loadAppointments() {
    const res = await api.get('/lawyers/appointments')
    appointments.value = (res.data as Appointment[]) || []
  }

  async function cancelAppointment(id: number, reason?: string) {
    await api.put(`/lawyers/appointments/${id}/cancel`, null, { params: { reason } })
    appointments.value = appointments.value.map(a =>
      a.id === id ? { ...a, status: 'CANCELLED', cancelReason: reason || '' } : a
    )
  }

  return {
    lawyers, currentLawyer, appointments, totalLawyers, loading,
    listLawyers, searchLawyers, getLawyer, createAppointment, loadAppointments, cancelAppointment
  }
})
