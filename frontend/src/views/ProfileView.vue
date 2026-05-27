<template>
  <div class="profile-page">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <h2>个人资料</h2>
        </div>
      </template>
      <el-form :model="profileForm" label-width="80px" v-loading="loading">
        <el-form-item label="用户名">
          <el-input :value="profileForm.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag :type="roleTagType">{{ roleLabel }}</el-tag>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="100" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" maxlength="100" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" maxlength="20" />
        </el-form-item>
        <el-form-item label="注册时间">
          <el-input :value="profileForm.createdAt" disabled />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleUpdateProfile" :loading="saving">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="password-card">
      <template #header>
        <div class="card-header">
          <h2>修改密码</h2>
        </div>
      </template>
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6位以上新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="handleChangePassword" :loading="changingPwd">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const saving = ref(false)
const changingPwd = ref(false)
const passwordFormRef = ref<FormInstance>()

const profileForm = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  role: '',
  createdAt: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const roleLabel = computed(() => {
  const map: Record<string, string> = { USER: '普通用户', LAWYER: '律师', ADMIN: '管理员' }
  return map[profileForm.role] || profileForm.role
})

const roleTagType = computed(() => {
  const map: Record<string, string> = { USER: 'info', LAWYER: 'success', ADMIN: 'danger' }
  return (map[profileForm.role] || 'info') as 'info' | 'success' | 'danger'
})

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(loadProfile)

async function loadProfile() {
  loading.value = true
  try {
    const res = await api.get('/user/profile')
    const data = res.data as Record<string, string>
    profileForm.username = data.username || ''
    profileForm.nickname = data.nickname || ''
    profileForm.email = data.email || ''
    profileForm.phone = data.phone || ''
    profileForm.role = data.role || ''
    profileForm.createdAt = data.createdAt || ''
  } catch (e: unknown) {
    ElMessage.error('加载个人资料失败')
  } finally {
    loading.value = false
  }
}

async function handleUpdateProfile() {
  saving.value = true
  try {
    await api.put('/user/profile', {
      nickname: profileForm.nickname,
      email: profileForm.email,
      phone: profileForm.phone
    })
    ElMessage.success('资料更新成功')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '更新失败')
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  changingPwd.value = true
  try {
    await api.put('/user/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '密码修改失败')
  } finally {
    changingPwd.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 600px;
  margin: 30px auto;
  padding: 0 20px;
}

.profile-card {
  margin-bottom: 20px;
}

.password-card {
  margin-bottom: 20px;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
}
</style>
