<template>
  <div class="login-view">
    <!-- Brand panel -->
    <div class="brand-panel">
      <div class="brand-content">
        <h1>⚖ 律法通</h1>
        <p class="brand-subtitle">AI智能法律咨询平台</p>
        <div class="brand-features">
          <div class="brand-feature" v-for="f in brandFeatures" :key="f">
            <span class="feature-dot"></span>
            {{ f }}
          </div>
        </div>
      </div>
    </div>

    <!-- Form panel -->
    <div class="form-panel">
      <div class="form-container">
        <h2>{{ activeTab === 'login' ? '欢迎回来' : '创建账户' }}</h2>
        <p class="form-subtitle">{{ activeTab === 'login' ? '登录您的律法通账户' : '注册开始使用律法通' }}</p>

        <el-tabs v-model="activeTab" class="auth-tabs">
          <el-tab-pane label="登录" name="login">
            <el-form :model="loginForm" @submit.prevent="handleLogin" label-position="top" size="large">
              <el-form-item label="用户名">
                <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password prefix-icon="Lock" />
              </el-form-item>
              <el-button type="primary" @click="handleLogin" :loading="loading" round style="width: 100%; margin-top: 8px">登录</el-button>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="注册" name="register">
            <el-form :model="registerForm" @submit.prevent="handleRegister" label-position="top" size="large">
              <el-form-item label="用户名">
                <el-input v-model="registerForm.username" placeholder="3-50字符" prefix-icon="User" />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="registerForm.password" type="password" placeholder="6位以上" show-password prefix-icon="Lock" />
              </el-form-item>
              <el-form-item label="昵称">
                <el-input v-model="registerForm.nickname" placeholder="可选" prefix-icon="UserFilled" />
              </el-form-item>
              <el-button type="primary" @click="handleRegister" :loading="loading" round style="width: 100%; margin-top: 8px">注册</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <div class="form-footer">
          使用即表示同意《服务条款》和《隐私政策》
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const activeTab = ref('login')
const loading = ref(false)

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', nickname: '' })

const brandFeatures = [
  'AI智能法律问答，专业准确',
  '合同风险分析，一键生成报告',
  '法律文书生成，6类文书模板',
  '律师智能匹配，协同过滤推荐'
]

async function handleLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login(loginForm.value.username, loginForm.value.password)
    ElMessage.success('登录成功')
    router.push('/chat')
  } catch (e: any) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.value.username || !registerForm.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    await authStore.register(registerForm.value.username, registerForm.value.password, registerForm.value.nickname)
    ElMessage.success('注册成功')
    router.push('/chat')
  } catch (e: any) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-view {
  display: flex;
  min-height: calc(100vh - var(--header-height, 56px));
}

/* Brand panel */
.brand-panel {
  flex: 1;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 40%, #0f3460 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;
  position: relative;
  overflow: hidden;
}

.brand-panel::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 80%, rgba(79, 195, 247, 0.12) 0%, transparent 50%);
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 400px;
}

.brand-content h1 {
  font-size: 42px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: 2px;
}

.brand-subtitle {
  font-size: 18px;
  color: var(--primary, #4fc3f7);
  margin: 0 0 40px;
  font-weight: 500;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.brand-feature {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.75);
  font-size: 14px;
  line-height: 1.5;
}

.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary, #4fc3f7);
  flex-shrink: 0;
}

/* Form panel */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #fff;
}

.form-container {
  width: 100%;
  max-width: 380px;
}

.form-container h2 {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px;
}

.form-subtitle {
  color: #999;
  font-size: 14px;
  margin: 0 0 28px;
}

.auth-tabs {
  margin-bottom: 16px;
}

.form-footer {
  text-align: center;
  font-size: 12px;
  color: #bbb;
  margin-top: 20px;
}

/* Responsive */
@media (max-width: 768px) {
  .login-view {
    flex-direction: column;
  }
  .brand-panel {
    padding: 40px 20px;
    min-height: auto;
  }
  .brand-content h1 {
    font-size: 28px;
  }
  .brand-features {
    display: none;
  }
  .form-panel {
    padding: 24px 20px;
  }
}
</style>
