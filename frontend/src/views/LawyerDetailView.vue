<template>
  <div class="lawyer-detail" v-loading="loading">
    <el-page-header @back="$router.push('/lawyers')" title="返回律师列表" />

    <div v-if="lawyer" class="detail-content">
      <el-card class="profile-card">
        <div class="profile-header">
          <el-avatar :size="80" :style="{ background: avatarColor(lawyer.id) }">
            {{ (lawyer.realName || '律')[0] }}
          </el-avatar>
          <div class="profile-info">
            <h2>
              {{ lawyer.realName }}
              <el-tag v-if="lawyer.verified" type="success" size="small">认证律师</el-tag>
            </h2>
            <p class="firm">{{ lawyer.lawFirm || '独立律师' }}</p>
            <p class="location">
              {{ lawyer.province }} {{ lawyer.city }}
              <span v-if="lawyer.yearsOfExperience"> · {{ lawyer.yearsOfExperience }}年执业经验</span>
            </p>
            <el-rate :model-value="lawyer.rating / 2" disabled show-score />
            <p class="count">累计咨询 {{ lawyer.consultationCount || 0 }}次</p>
          </div>
          <el-button type="primary" size="large" @click="showAppointmentDialog">预约咨询</el-button>
        </div>

        <el-divider />

        <el-descriptions :column="2" border>
          <el-descriptions-item label="执业证号">{{ lawyer.licenseNo || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="咨询方式">{{ consultTypeLabel(lawyer.consultationType) }}</el-descriptions-item>
          <el-descriptions-item label="学历背景" :span="2">{{ lawyer.education || '未填写' }}</el-descriptions-item>
        </el-descriptions>

        <div class="section" v-if="lawyer.specialties?.length">
          <h3>专业领域</h3>
          <div class="tag-list">
            <el-tag v-for="s in lawyer.specialties" :key="s" type="info">{{ s }}</el-tag>
          </div>
        </div>

        <div class="section" v-if="lawyer.tags?.length">
          <h3>标签</h3>
          <div class="tag-list">
            <el-tag v-for="t in lawyer.tags" :key="t" size="small">{{ t }}</el-tag>
          </div>
        </div>

        <div class="section" v-if="lawyer.bio">
          <h3>个人简介</h3>
          <p class="bio-text">{{ lawyer.bio }}</p>
        </div>
      </el-card>

      <!-- 评价区域 -->
      <el-card class="review-card" style="margin-top: 16px">
        <div class="review-header">
          <h3>用户评价 ({{ reviews.length }})</h3>
          <el-button type="primary" size="small" @click="showReviewDialog = true">写评价</el-button>
        </div>
        <div v-if="reviews.length === 0" style="color: #999; text-align: center; padding: 20px">暂无评价</div>
        <div v-for="r in reviews" :key="r.id" class="review-item">
          <div class="review-meta">
            <span class="review-user">{{ r.username || '匿名用户' }}</span>
            <el-rate :model-value="r.rating" disabled size="small" />
            <span class="review-date">{{ formatDate(r.createdAt) }}</span>
          </div>
          <p v-if="r.comment" class="review-comment">{{ r.comment }}</p>
        </div>
      </el-card>
    </div>

    <!-- 评价对话框 -->
    <el-dialog v-model="showReviewDialog" title="评价律师" width="460px">
      <el-form :model="reviewForm" label-width="90px">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" show-text :texts="['很差', '较差', '一般', '较好', '很好']" />
        </el-form-item>
        <el-form-item label="服务类型">
          <el-radio-group v-model="reviewForm.serviceType">
            <el-radio value="ONLINE">线上</el-radio>
            <el-radio value="OFFLINE">线下</el-radio>
            <el-radio value="PHONE">电话</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="3" placeholder="分享您的咨询体验" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReviewDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateReview" :loading="reviewSubmitting">提交评价</el-button>
      </template>
    </el-dialog>

    <!-- 预约对话框 -->
    <el-dialog v-model="appointmentDialogVisible" title="预约律师咨询" width="460px">
      <el-form :model="appointmentForm" label-width="90px">
        <el-form-item label="律师">
          <span>{{ lawyer?.realName }}</span>
        </el-form-item>
        <el-form-item label="预约时间">
          <el-date-picker v-model="appointmentForm.appointmentTime" type="datetime" placeholder="选择预约时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="咨询方式">
          <el-radio-group v-model="appointmentForm.consultationType">
            <el-radio value="ONLINE">线上咨询</el-radio>
            <el-radio value="OFFLINE">线下见面</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input v-model="appointmentForm.description" type="textarea" :rows="3" placeholder="简要描述您的法律问题" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appointmentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateAppointment" :loading="submitting">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useLawyerStore } from '@/stores/lawyer'
import type { LawyerProfile } from '@/stores/lawyer'
import api from '@/api'

const route = useRoute()
const lawyerStore = useLawyerStore()
const loading = ref(false)
const lawyer = ref<LawyerProfile | null>(null)

const appointmentDialogVisible = ref(false)
const submitting = ref(false)
const appointmentForm = reactive({
  appointmentTime: '',
  consultationType: 'ONLINE',
  description: ''
})

const showReviewDialog = ref(false)
const reviewSubmitting = ref(false)
const reviews = ref<any[]>([])
const reviewForm = reactive({
  rating: 5,
  comment: '',
  serviceType: 'ONLINE'
})

onMounted(async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    lawyer.value = await lawyerStore.getLawyer(id)
    loadReviews(id)
  } catch {
    ElMessage.error('律师信息加载失败')
  } finally {
    loading.value = false
  }
})

function showAppointmentDialog() {
  appointmentForm.appointmentTime = ''
  appointmentForm.consultationType = 'ONLINE'
  appointmentForm.description = ''
  appointmentDialogVisible.value = true
}

async function handleCreateAppointment() {
  if (!appointmentForm.appointmentTime) {
    ElMessage.warning('请选择预约时间')
    return
  }
  submitting.value = true
  try {
    await lawyerStore.createAppointment(
      lawyer.value!.id,
      appointmentForm.appointmentTime,
      appointmentForm.consultationType,
      appointmentForm.description
    )
    ElMessage.success('预约成功，请等待律师确认')
    appointmentDialogVisible.value = false
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '预约失败')
  } finally {
    submitting.value = false
  }
}

function consultTypeLabel(type: string) {
  const map: Record<string, string> = { ONLINE: '线上', OFFLINE: '线下', BOTH: '线上/线下' }
  return map[type] || type
}

function avatarColor(id: number) {
  const colors = ['#1a1a2e', '#16213e', '#0f3460', '#533483', '#2b2d42', '#3d5a80']
  return colors[id % colors.length]
}

async function loadReviews(lawyerId: number) {
  try {
    const res = await api.get(`/lawyers/${lawyerId}/reviews`)
    reviews.value = (res.data as any[]) || []
  } catch {
    reviews.value = []
  }
}

async function handleCreateReview() {
  if (!lawyer.value) return
  reviewSubmitting.value = true
  try {
    await api.post(`/lawyers/${lawyer.value.id}/reviews`, reviewForm)
    ElMessage.success('评价成功')
    showReviewDialog.value = false
    loadReviews(lawyer.value.id)
    // Refresh lawyer to get updated rating
    lawyer.value = await lawyerStore.getLawyer(lawyer.value.id)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '评价失败')
  } finally {
    reviewSubmitting.value = false
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.lawyer-detail {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.profile-card {
  margin-top: 16px;
}

.profile-header {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.profile-info {
  flex: 1;
}

.profile-info h2 {
  margin: 0 0 4px;
  font-size: 22px;
  color: #1a1a2e;
}

.firm {
  color: #666;
  margin: 2px 0;
}

.location {
  color: #999;
  font-size: 13px;
  margin: 2px 0;
}

.count {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}

.section {
  margin-top: 20px;
}

.section h3 {
  font-size: 15px;
  color: #333;
  margin-bottom: 8px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.bio-text {
  color: #555;
  line-height: 1.8;
  font-size: 14px;
}
.review-card h3 {
  margin: 0;
}
.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.review-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.review-item:last-child {
  border-bottom: none;
}
.review-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.review-user {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}
.review-date {
  font-size: 12px;
  color: #999;
}
.review-comment {
  color: #555;
  font-size: 13px;
  line-height: 1.6;
  margin: 4px 0 0;
}
</style>
