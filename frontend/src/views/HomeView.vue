<template>
  <div class="home">
    <section class="hero">
      <div class="hero-content">
        <h1 class="hero-title">律法通</h1>
        <p class="hero-subtitle">专业法律AI辅助平台</p>
        <p class="hero-desc">基于大语言模型与法律知识库，为法律从业者提供合同审查、法条检索、案例分析与文书起草工具</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/chat')">进入法律咨询</el-button>
          <el-button size="large" class="btn-outline" @click="$router.push('/contract')">合同智能分析</el-button>
        </div>
      </div>
    </section>

    <section class="stats">
      <div class="stat-item" v-for="s in statList" :key="s.label">
        <div class="stat-value">{{ s.display }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </section>

    <section class="tools">
      <div class="tool-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="['tool-tab', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >{{ tab.label }}</button>
      </div>

      <div class="tool-grid" v-if="activeTab === 'features'">
        <div v-for="f in features" :key="f.title" class="tool-card" @click="$router.push(f.link)">
          <div class="tool-icon">
            <el-icon :size="22" color="#2563eb"><component :is="f.icon" /></el-icon>
          </div>
          <div class="tool-body">
            <h3>{{ f.title }}</h3>
            <p>{{ f.desc }}</p>
          </div>
          <span class="tool-link">进入工具 →</span>
        </div>
      </div>

      <div class="tool-grid" v-else>
        <div v-for="s in services" :key="s.title" class="tool-card" @click="$router.push(s.link)">
          <div class="tool-icon">
            <el-icon :size="22" color="#2563eb"><component :is="s.icon" /></el-icon>
          </div>
          <div class="tool-body">
            <h3>{{ s.title }}</h3>
            <p>{{ s.desc }}</p>
          </div>
          <span class="tool-link">了解更多 →</span>
        </div>
      </div>
    </section>

    <section class="cta">
      <h2>开始使用律法通</h2>
      <p>注册账号即可体验AI法律辅助服务</p>
      <el-button type="primary" size="large" @click="$router.push(authStore.isLoggedIn ? '/chat' : '/login')">
        {{ authStore.isLoggedIn ? '进入工作台' : '免费注册' }}
      </el-button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useTransition } from '@vueuse/core'
import { ChatDotRound, Document, Search, ScaleToOriginal, Reading, EditPen, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const activeTab = ref('features')

const tabs = [
  { key: 'features', label: '核心工具' },
  { key: 'services', label: '增值服务' }
]

const statsData = [
  { target: 120000, suffix: '+', label: '法律法规及司法解释' },
  { target: 85000, suffix: '+', label: '裁判文书收录' },
  { target: 14, suffix: '类', label: '法律文书模板' },
  { target: 7, suffix: '', label: '核心法律领域覆盖' }
]

const animatedValues = statsData.map(() => ref(0))

const statList = computed(() => {
  return statsData.map((s, i) => {
    const val = animatedValues[i].value
    let display = ''
    if (s.target >= 10000) {
      display = (s.target === 120000 ? '12' : s.target === 85000 ? '8.5' : '') + '万'
    } else {
      display = Math.round(val).toString()
    }
    return { label: s.label, display: (s.prefix ?? '') + display + s.suffix }
  })
})

onMounted(() => {
  statsData.forEach((s, i) => {
    useTransition(animatedValues[i], { duration: 1200, transition: [0, 0.3, 0.1, 1] })
    setTimeout(() => {
      const source = ref(0)
      const { pause } = useTransition(source, {
        duration: 1200, transition: [0, 0.3, 0.1, 1],
        onFinished() { pause() }
      })
      source.value = s.target
    }, 200 + i * 100)
  })
})

const features = [
  { title: '法律咨询', desc: '基于大语言模型与法律知识库，提供多轮对话式的法律问题解答与分析', icon: ChatDotRound, link: '/chat' },
  { title: '合同审查', desc: '上传合同文件，自动识别风险条款并提供逐条修改建议', icon: Document, link: '/contract' },
  { title: '法条检索', desc: '语义搜索与关键词检索结合，快速定位法律条文及司法解释', icon: Search, link: '/laws' },
  { title: '法律意见书', desc: '根据案情描述，自动生成包含事实分析、法律依据的结构化意见书', icon: ScaleToOriginal, link: '/opinions' },
  { title: '文书起草', desc: '支持起诉状、答辩状、仲裁申请书等6类法律文书一键生成', icon: EditPen, link: '/documents' },
  { title: '案例检索', desc: '搜索相似案例，分析裁判要旨，辅助诉讼策略制定', icon: Reading, link: '/cases' }
]

const services = [
  { title: '律师匹配', desc: '按执业领域与经验匹配律师，支持在线预约咨询', icon: User, link: '/lawyers' },
  { title: '常见问题', desc: '覆盖劳动、婚姻、合同、房产等领域的法律问题解答', icon: ChatDotRound, link: '/faq' },
  { title: '数据合规', desc: '全链路加密，数据脱敏存储，符合法律行业数据安全要求', icon: Document, link: '/chat' }
]
</script>

<style scoped>
.home { background: var(--bg); }

.hero {
  background: linear-gradient(170deg, #eff6ff 0%, #f8fafc 40%, #fff 100%);
  text-align: center; padding: 100px 24px 80px;
  border-bottom: 1px solid var(--border);
}
.hero-content { max-width: 640px; margin: 0 auto; }
.hero-title { font-size: 42px; font-weight: 700; color: var(--color-primary-600); letter-spacing: 6px; margin: 0 0 12px; }
.hero-subtitle { font-size: 20px; color: var(--gray-600); margin: 0 0 14px; font-weight: 500; }
.hero-desc { font-size: 15px; color: var(--text-tertiary); line-height: 1.75; margin: 0 0 36px; }
.hero-actions { display: flex; justify-content: center; gap: 14px; flex-wrap: wrap; }
.hero-actions .el-button { padding: 12px 28px; font-size: 15px; font-weight: 500; border-radius: 8px; }
.btn-outline { border: 1px solid #d1d5db !important; color: var(--gray-600) !important; background: #fff !important; }
.btn-outline:hover { border-color: var(--color-primary-400) !important; color: var(--color-primary-600) !important; transform: translateY(-1px); box-shadow: 0 2px 8px rgba(37,99,235,0.1); }

.stats {
  display: flex; justify-content: center; gap: 72px;
  padding: 40px 20px; background: #fff; flex-wrap: wrap;
  border-bottom: 1px solid var(--border);
}
.stat-item { text-align: center; position: relative; }
.stat-item:not(:last-child)::after { content: ''; position: absolute; right: -36px; top: 10%; height: 80%; width: 1px; background: var(--border); }
.stat-value { font-size: 36px; font-weight: 700; color: var(--color-primary-600); line-height: 1.2; font-variant-numeric: tabular-nums; }
.stat-label { font-size: 13px; color: var(--text-tertiary); margin-top: 6px; }

/* Tab switching */
.tools { padding: 64px 20px; max-width: 1100px; margin: 0 auto; }
.tool-tabs { display: flex; justify-content: center; gap: 0; margin-bottom: 40px; border-bottom: 2px solid var(--border); }
.tool-tab {
  padding: 12px 32px; font-size: 15px; font-weight: 500;
  background: none; border: none; cursor: pointer;
  color: var(--text-tertiary); position: relative;
  transition: color 0.2s;
}
.tool-tab:hover { color: var(--color-primary-600); }
.tool-tab.active { color: var(--color-primary-600); font-weight: 600; }
.tool-tab.active::after {
  content: ''; position: absolute; bottom: -2px; left: 20%; right: 20%;
  height: 2px; background: var(--color-primary-500); border-radius: 1px;
}

.tool-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }

.tool-card {
  background: #fff; border: 1px solid var(--border); border-radius: 12px;
  padding: 28px 24px; cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  display: flex; flex-direction: column;
}
.tool-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 28px rgba(37,99,235,0.1);
  border-color: var(--color-primary-200);
}
.tool-icon {
  width: 44px; height: 44px; border-radius: 10px;
  background: var(--color-primary-50); display: flex;
  align-items: center; justify-content: center; margin-bottom: 16px;
}
.tool-body { flex: 1; }
.tool-card h3 { font-size: 16px; font-weight: 600; color: var(--text-primary); margin: 0 0 8px; }
.tool-card p { font-size: 14px; color: var(--text-secondary); line-height: 1.6; margin: 0; }
.tool-link { font-size: 13px; color: var(--color-primary-600); font-weight: 500; margin-top: 14px; transition: color 0.2s; }
.tool-card:hover .tool-link { color: var(--color-primary-500); }

/* CTA */
.cta {
  text-align: center; padding: 72px 20px;
  background: linear-gradient(170deg, #eff6ff 0%, #dbeafe 100%);
}
.cta h2 { font-size: 26px; font-weight: 600; color: var(--text-primary); margin: 0 0 10px; }
.cta p { color: var(--text-secondary); font-size: 15px; margin: 0 0 28px; }

@media (max-width: 768px) {
  .hero { padding: 72px 20px 56px; }
  .hero-title { font-size: 32px; letter-spacing: 3px; }
  .stats { gap: 24px; padding: 32px 16px; }
  .stat-item:not(:last-child)::after { display: none; }
  .stat-value { font-size: 28px; }
  .tool-grid { grid-template-columns: 1fr; }
  .tools, .cta { padding: 48px 16px; }
}

html.dark .hero { background: linear-gradient(170deg, rgba(37,99,235,0.06) 0%, var(--bg) 40%, var(--bg-card) 100%); }
html.dark .hero-title { color: var(--text-primary); }
html.dark .hero-subtitle { color: var(--text-secondary); }
html.dark .hero-desc { color: var(--text-tertiary); }
html.dark .stats { background: var(--bg-card); }
html.dark .tool-tabs { border-bottom-color: var(--border); }
html.dark .tool-card { background: var(--bg-card); border-color: var(--border); }
html.dark .tool-icon { background: rgba(37,99,235,0.1); }
html.dark .btn-outline { background: var(--bg-card) !important; }
html.dark .cta { background: linear-gradient(170deg, rgba(37,99,235,0.06) 0%, rgba(37,99,235,0.10) 100%); }
</style>
