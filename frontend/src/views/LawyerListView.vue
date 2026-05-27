<template>
  <div class="lawyer-page">
    <h1 class="page-title">律师服务</h1>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索律师姓名、专业领域..."
        clearable
        @keyup.enter="handleSearch"
        class="search-input"
      >
        <template #prefix>
          <el-icon :size="16"><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="selectedSpecialty" placeholder="专业领域" clearable @change="handleSearch" class="specialty-select">
        <el-option v-for="s in specialties" :key="s" :label="s" :value="s" />
      </el-select>
    </div>

    <div class="quick-filters">
      <span
        v-for="s in quickSpecialties"
        :key="s"
        :class="['filter-tag', { active: selectedSpecialty === s }]"
        @click="handleQuickFilter(s)"
      >{{ s }}</span>
    </div>

    <div v-loading="lawyerStore.loading" class="lawyer-grid">
      <div v-for="lawyer in lawyerStore.lawyers" :key="lawyer.id" class="lawyer-card" @click="goDetail(lawyer.id)">
        <div class="card-top">
          <div class="avatar-wrap">
            <el-avatar :size="52" :style="{ background: avatarColor(lawyer.id) }" class="avatar-circle">
              {{ (lawyer.realName || '律')[0] }}
            </el-avatar>
          </div>
          <div class="lawyer-info">
            <div class="lawyer-name">
              {{ lawyer.realName }}
              <el-tag v-if="lawyer.verified" size="small" class="verified-tag">已认证</el-tag>
            </div>
            <div class="lawyer-firm">{{ lawyer.lawFirm || '独立律师' }}</div>
            <div class="lawyer-location">
              <template v-if="lawyer.province">{{ lawyer.province }} {{ lawyer.city }}</template>
              <template v-if="lawyer.yearsOfExperience">{{ lawyer.province ? ' · ' : '' }}{{ lawyer.yearsOfExperience }}年经验</template>
            </div>
          </div>
        </div>
        <div class="lawyer-specialties">
          <span v-for="s in lawyer.specialties?.slice(0, 4)" :key="s" class="spec-tag">{{ s }}</span>
        </div>
        <div class="lawyer-bio">{{ truncate(lawyer.bio, 80) }}</div>
        <div class="lawyer-footer">
          <span class="consult-count">已咨询 {{ lawyer.consultationCount || 0 }} 次</span>
          <el-button type="primary" size="small" class="consult-btn" @click.stop="openAppointment(lawyer)">预约咨询</el-button>
        </div>
      </div>
    </div>

    <div v-if="!lawyerStore.loading && lawyerStore.lawyers.length === 0" class="empty-state">
      <el-empty description="暂无律师信息" />
    </div>

    <div class="pagination" v-if="lawyerStore.totalLawyers > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="lawyerStore.totalLawyers"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog v-model="appointmentDialogVisible" title="预约律师咨询" width="460px">
      <el-form :model="appointmentForm" label-width="90px">
        <el-form-item label="律师">
          <span>{{ selectedLawyer?.realName }}</span>
        </el-form-item>
        <el-form-item label="预约时间">
          <el-date-picker
            v-model="appointmentForm.appointmentTime"
            type="datetime"
            placeholder="选择预约时间"
            :disabled-hours="disabledHours"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="咨询方式">
          <el-radio-group v-model="appointmentForm.consultationType">
            <el-radio value="ONLINE">线上咨询</el-radio>
            <el-radio value="OFFLINE">线下见面</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input
            v-model="appointmentForm.description"
            type="textarea"
            :rows="3"
            placeholder="简要描述您的法律问题"
            maxlength="500"
            show-word-limit
          />
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useLawyerStore } from '@/stores/lawyer'
import type { LawyerProfile } from '@/stores/lawyer'

const router = useRouter()
const lawyerStore = useLawyerStore()

const searchKeyword = ref('')
const selectedSpecialty = ref('')
const currentPage = ref(1)
const pageSize = 10
const specialties = ['劳动法', '合同法', '婚姻法', '房产法', '刑事辩护', '知识产权', '公司法', '税务法', '交通事故', '医疗纠纷']
const quickSpecialties = ['劳动纠纷', '婚姻家庭', '合同纠纷', '刑事辩护', '知识产权', '公司法']

const appointmentDialogVisible = ref(false)
const selectedLawyer = ref<LawyerProfile | null>(null)
const submitting = ref(false)
const appointmentForm = reactive({
  appointmentTime: '',
  consultationType: 'ONLINE',
  description: ''
})

onMounted(() => {
  lawyerStore.listLawyers(0, pageSize)
})

function handleSearch() {
  currentPage.value = 1
  lawyerStore.searchLawyers(searchKeyword.value || undefined, selectedSpecialty.value || undefined, 0, pageSize)
}

function handleQuickFilter(s: string) {
  selectedSpecialty.value = selectedSpecialty.value === s ? '' : s
  handleSearch()
}

function handlePageChange(page: number) {
  const p = page - 1
  if (searchKeyword.value || selectedSpecialty.value) {
    lawyerStore.searchLawyers(searchKeyword.value || undefined, selectedSpecialty.value || undefined, p, pageSize)
  } else {
    lawyerStore.listLawyers(p, pageSize)
  }
}

function goDetail(id: number) {
  router.push(`/lawyers/${id}`)
}

function openAppointment(lawyer: LawyerProfile) {
  selectedLawyer.value = lawyer
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
      selectedLawyer.value!.id,
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

function truncate(text: string | null | undefined, len: number) {
  if (!text) return ''
  return text.length > len ? text.substring(0, len) + '...' : text
}

function avatarColor(id: number) {
  const colors = ['#1e40af', '#7c3aed', '#059669', '#d97706', '#dc2626', '#2563eb']
  return colors[id % colors.length]
}

function disabledHours() {
  return [...Array(8).keys(), ...Array(24).keys().slice(19)]
}
</script>

<style scoped>
.lawyer-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 24px 60px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 24px;
  letter-spacing: -0.02em;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.search-input {
  flex: 1;
  max-width: 360px;
}

.search-input :deep(.el-input__wrapper) {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(37, 99, 235, 0.04);
  transition: border-color 0.2s ease;
}

.search-input :deep(.el-input__wrapper:hover) {
  border-color: #2563eb;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
}

.specialty-select {
  width: 160px;
  flex-shrink: 0;
}

.specialty-select :deep(.el-input__wrapper) {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: none;
  transition: border-color 0.2s ease;
}

.specialty-select :deep(.el-input__wrapper:hover) {
  border-color: #2563eb;
}

.quick-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.filter-tag {
  cursor: pointer;
  font-size: 12px;
  padding: 4px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #ffffff;
  color: #4b5563;
  transition: all 0.2s ease;
  user-select: none;
}

.filter-tag:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.04);
}

.filter-tag.active {
  background: #2563eb;
  border-color: #2563eb;
  color: #ffffff;
}

.lawyer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.lawyer-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(37, 99, 235, 0.04);
  cursor: pointer;
  transition: all 0.25s ease;
}

.lawyer-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.08);
}

.card-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.avatar-wrap {
  flex-shrink: 0;
}

.avatar-circle :deep(.el-avatar) {
  border: 2px solid #e5e7eb;
  transition: border-color 0.3s ease;
}

.lawyer-card:hover .avatar-circle :deep(.el-avatar) {
  border-color: #2563eb;
}

.lawyer-info {
  flex: 1;
  min-width: 0;
}

.lawyer-name {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  display: flex;
  align-items: center;
  gap: 6px;
}

.verified-tag {
  background: rgba(34, 197, 94, 0.1);
  border-color: rgba(34, 197, 94, 0.25);
  color: #16a34a;
}

.lawyer-firm {
  font-size: 13px;
  color: #4b5563;
  margin-top: 3px;
}

.lawyer-location {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 3px;
}

.lawyer-specialties {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.spec-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  font-size: 11px;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.06);
  border: 1px solid rgba(37, 99, 235, 0.12);
  border-radius: 4px;
  line-height: 1.5;
}

.lawyer-bio {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 14px;
}

.lawyer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f3f4f6;
  padding-top: 14px;
}

.consult-count {
  font-size: 12px;
  color: #9ca3af;
}

.consult-btn {
  transition: all 0.2s ease;
}

.consult-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
}

.empty-state {
  padding: 60px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

html.dark .page-title { color: var(--text-primary); }
html.dark .search-input :deep(.el-input__wrapper),
html.dark .specialty-select :deep(.el-input__wrapper) { background: #1e293b; border-color: rgba(255,255,255,0.08); box-shadow: none; }
html.dark .filter-tag { background: transparent; color: var(--text-secondary); border-color: rgba(255,255,255,0.05); }
html.dark .filter-tag:hover { background: rgba(255,255,255,0.08); border-color: rgba(255,255,255,0.08); }
html.dark .filter-tag.active { background: #3b82f6; border-color: #3b82f6; color: #ffffff; }
html.dark .lawyer-card { background: var(--bg-card); border-color: rgba(255,255,255,0.08); }
html.dark .lawyer-card:hover { background: rgba(59,130,246,0.06); }
html.dark .lawyer-name { color: var(--text-primary); }
html.dark .lawyer-firm { color: var(--text-secondary); }
html.dark .lawyer-location { color: var(--text-tertiary); }
html.dark .lawyer-bio { color: var(--text-secondary); }
html.dark .lawyer-footer { border-color: rgba(255,255,255,0.08); }
html.dark .consult-count { color: var(--text-tertiary); }
html.dark .avatar-circle :deep(.el-avatar) { border-color: rgba(255,255,255,0.08); }
html.dark .spec-tag { color: #60a5fa; background: rgba(59,130,246,0.1); border-color: rgba(59,130,246,0.15); }
html.dark .lawyer-card:hover .avatar-circle :deep(.el-avatar) { border-color: var(--color-primary-400); }
</style>
