<template>
  <div class="lawyer-page">
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索律师姓名、专业领域..."
        clearable
        @keyup.enter="handleSearch"
        style="width: 300px"
      >
        <template #append>
          <el-button @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
      <el-select v-model="selectedSpecialty" placeholder="专业领域" clearable @change="handleSearch" style="width: 160px; margin-left: 12px">
        <el-option v-for="s in specialties" :key="s" :label="s" :value="s" />
      </el-select>
    </div>

    <div v-loading="lawyerStore.loading" class="lawyer-grid">
      <el-card v-for="lawyer in lawyerStore.lawyers" :key="lawyer.id" shadow="hover" class="lawyer-card" @click="goDetail(lawyer.id)">
        <div class="lawyer-header">
          <div class="lawyer-avatar">
            <el-avatar :size="56" :style="{ background: avatarColor(lawyer.id) }">
              {{ (lawyer.realName || '律')[0] }}
            </el-avatar>
          </div>
          <div class="lawyer-info">
            <div class="lawyer-name">
              {{ lawyer.realName }}
              <el-tag v-if="lawyer.verified" type="success" size="small" style="margin-left: 6px">认证</el-tag>
            </div>
            <div class="lawyer-firm">{{ lawyer.lawFirm || '独立律师' }}</div>
            <div class="lawyer-location">
              <span v-if="lawyer.province">{{ lawyer.province }} {{ lawyer.city }}</span>
              <span v-if="lawyer.yearsOfExperience"> · {{ lawyer.yearsOfExperience }}年经验</span>
            </div>
          </div>
          <div class="lawyer-rating">
            <el-rate :model-value="lawyer.rating / 2" disabled show-score size="small" />
          </div>
        </div>
        <div class="lawyer-specialties">
          <el-tag v-for="s in lawyer.specialties?.slice(0, 4)" :key="s" size="small" type="info" style="margin-right: 4px; margin-bottom: 4px">
            {{ s }}
          </el-tag>
        </div>
        <div class="lawyer-bio">{{ truncate(lawyer.bio, 80) }}</div>
        <div class="lawyer-footer">
          <span class="consult-count">已咨询 {{ lawyer.consultationCount || 0 }}次</span>
          <el-button type="primary" size="small" @click.stop="openAppointment(lawyer)">预约咨询</el-button>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!lawyerStore.loading && lawyerStore.lawyers.length === 0" description="暂无律师信息" />

    <div class="pagination" v-if="lawyerStore.totalLawyers > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="lawyerStore.totalLawyers"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 预约对话框 -->
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useLawyerStore } from '@/stores/lawyer'
import type { LawyerProfile } from '@/stores/lawyer'

const router = useRouter()
const lawyerStore = useLawyerStore()

const searchKeyword = ref('')
const selectedSpecialty = ref('')
const currentPage = ref(1)
const pageSize = 10
const specialties = ['劳动法', '合同法', '婚姻法', '房产法', '刑事辩护', '知识产权', '公司法', '税务法', '交通事故', '医疗纠纷']

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
  const colors = ['#1a1a2e', '#16213e', '#0f3460', '#533483', '#2b2d42', '#3d5a80']
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
  padding: 20px;
}

.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.lawyer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.lawyer-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.lawyer-card:hover {
  transform: translateY(-2px);
}

.lawyer-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
}

.lawyer-info {
  flex: 1;
}

.lawyer-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.lawyer-firm {
  font-size: 13px;
  color: #666;
  margin-top: 2px;
}

.lawyer-location {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.lawyer-rating {
  flex-shrink: 0;
}

.lawyer-specialties {
  margin-bottom: 8px;
}

.lawyer-bio {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 10px;
}

.lawyer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f0f2f5;
  padding-top: 10px;
}

.consult-count {
  font-size: 12px;
  color: #999;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
