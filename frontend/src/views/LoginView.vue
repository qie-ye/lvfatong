<template>
  <div class="login-view">
    <!-- Brand panel -->
    <div class="brand-panel">
      <div class="brand-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
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
        <div class="form-header">
          <h2>{{ activeTab === 'login' ? '欢迎回来' : '创建账户' }}</h2>
          <p class="form-subtitle">{{ activeTab === 'login' ? '登录您的律法通账户' : '注册开始使用律法通' }}</p>
        </div>

        <el-tabs v-model="activeTab" class="auth-tabs">
          <el-tab-pane label="登录" name="login">
            <el-form :model="loginForm" @submit.prevent="handleLogin" label-position="top" size="large">
              <el-form-item label="用户名">
                <el-input v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="User" />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password :prefix-icon="Lock" />
              </el-form-item>
              <el-button type="primary" @click="handleLogin" :loading="loading" round class="submit-btn">登录</el-button>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="注册" name="register">
            <el-form :model="registerForm" @submit.prevent="handleRegister" label-position="top" size="large">
              <el-form-item label="用户名">
                <el-input v-model="registerForm.username" placeholder="3-50字符" :prefix-icon="User" />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="registerForm.password" type="password" placeholder="6位以上" show-password :prefix-icon="Lock" />
              </el-form-item>
              <el-form-item label="昵称">
                <el-input v-model="registerForm.nickname" placeholder="可选" :prefix-icon="UserFilled" />
              </el-form-item>
              <el-button type="primary" @click="handleRegister" :loading="loading" round class="submit-btn">注册</el-button>
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
import { User, Lock, UserFilled } from '@element-plus/icons-vue'

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
  background: linear-gradient(135deg, #0f1b3d 0%, #122150 40%, #0d2b5e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;
  position: relative;
  overflow: hidden;
}

.brand-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.06;
}

.shape-1 {
  width: 300px;
  height: 300px;
  background: var(--color-primary-400);
  top: -60px;
  right: -60px;
  animation: float 8s ease-in-out infinite;
}

.shape-2 {
  width: 200px;
  height: 200px;
  background: var(--color-accent);
  bottom: -40px;
  left: 10%;
  animation: float 10s ease-in-out infinite reverse;
}

.shape-3 {
  width: 150px;
  height: 150px;
  background: var(--color-primary-300);
  top: 50%;
  left: 20%;
  animation: float 7s ease-in-out infinite 1s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(20px, -20px) scale(1.05); }
  66% { transform: translate(-10px, 10px) scale(0.95); }
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
  color: var(--color-primary-300);
  margin: 0 0 40px;
  font-weight: 500;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.brand-feature {
  display: flex;
  align-items: center;
  gap: 14px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  line-height: 1.5;
}

.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary-400);
  flex-shrink: 0;
  box-shadow: 0 0 8px rgba(59, 111, 240, 0.5);
}

/* Form panel */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: var(--bg-card);
}

.form-container {
  width: 100%;
  max-width: 400px;
}

.form-header {
  margin-bottom: 8px;
}

.form-container h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px;
  letter-spacing: -0.02em;
}

.form-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0;
}

.auth-tabs {
  margin-bottom: 16px;
}

.auth-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  font-size: 15px;
  padding: 12px 0;
  height: auto;
  font-weight: 600;
}

.submit-btn:not(:disabled) {
  box-shadow: 0 4px 14px rgba(26, 92, 208, 0.35);
}

.form-footer {
  text-align: center;
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 24px;
}

/* Responsive */
@media (max-width: 768px) {
  .login-view {
    flex-direction: column;
  }
  .brand-panel {
    padding: 40px 24px;
    min-height: auto;
  }
  .brand-content h1 {
    font-size: 28px;
  }
  .brand-features {
    display: none;
  }
  .form-panel {
    padding: 28px 20px;
  }
  .form-container h2 {
    font-size: 24px;
  }
}
</style>
