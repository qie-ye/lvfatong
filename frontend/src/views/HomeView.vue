<template>
  <div class="home">
    <!-- Hero Section -->
    <section class="hero">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="title-accent">律法通</span>
        </h1>
        <p class="hero-subtitle">AI智能法律咨询平台</p>
        <p class="hero-desc">基于智谱AI大模型与RAG技术，为您提供专业、准确、高效的法律服务</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="$router.push('/chat')">
            <el-icon><ChatDotRound /></el-icon>
            开始法律咨询
          </el-button>
          <el-button size="large" round @click="$router.push('/contract')">
            <el-icon><Document /></el-icon>
            合同智能分析
          </el-button>
        </div>
      </div>
    </section>

    <!-- Stats -->
    <section class="stats">
      <div class="stat-item" v-for="s in stats" :key="s.label">
        <div class="stat-value">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </section>

    <!-- Features -->
    <section class="features">
      <h2 class="section-title">核心功能</h2>
      <div class="feature-grid">
        <div
          v-for="f in features"
          :key="f.title"
          class="feature-card"
          @click="$router.push(f.link)"
        >
          <div class="feature-icon" :style="{ background: f.color }">
            <el-icon :size="28" color="#fff"><component :is="f.icon" /></el-icon>
          </div>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
          <span class="feature-link">了解更多 →</span>
        </div>
      </div>
    </section>

    <!-- Services -->
    <section class="services">
      <h2 class="section-title">专业服务</h2>
      <div class="service-grid">
        <div v-for="s in services" :key="s.title" class="service-card" @click="$router.push(s.link)">
          <div class="service-badge">{{ s.badge }}</div>
          <h3>{{ s.title }}</h3>
          <p>{{ s.desc }}</p>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta">
      <h2>开始您的智能法律之旅</h2>
      <p>注册即可免费体验AI法律咨询服务</p>
      <el-button type="primary" size="large" round @click="$router.push(authStore.isLoggedIn ? '/chat' : '/login')">
        {{ authStore.isLoggedIn ? '立即咨询' : '免费注册' }}
      </el-button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ChatDotRound, Document, Search, ScaleToOriginal, Reading, EditPen, User, TrendCharts } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const stats = [
  { value: '10万+', label: '法律知识条目' },
  { value: '6+', label: '核心法律领域' },
  { value: '99.2%', label: '咨询满意度' },
  { value: '24/7', label: '全天候服务' }
]

const features = [
  { title: '智能法律问答', desc: '基于RAG检索增强生成，结合法律知识库，提供专业准确的实时法律咨询', icon: ChatDotRound, color: 'linear-gradient(135deg, #667eea, #764ba2)', link: '/chat' },
  { title: '合同风险分析', desc: 'AI自动识别合同风险条款，提供条款级风险评估、修改建议和合同对比', icon: Document, color: 'linear-gradient(135deg, #f093fb, #f5576c)', link: '/contract' },
  { title: '法律条文检索', desc: '向量语义检索+全文检索融合，快速精准定位相关法律条文', icon: Search, color: 'linear-gradient(135deg, #4facfe, #00f2fe)', link: '/laws' },
  { title: '法律意见生成', desc: 'GLM-4-Plus深度推理，生成结构化法律意见书，含法律分析与结论', icon: ScaleToOriginal, color: 'linear-gradient(135deg, #43e97b, #38f9d7)', link: '/opinions' },
  { title: '法律文书生成', desc: 'AI一键生成起诉状、答辩状、仲裁申请书等6类法律文书', icon: EditPen, color: 'linear-gradient(135deg, #fa709a, #fee140)', link: '/documents' },
  { title: '案例智能检索', desc: '语义+关键词双引擎检索海量案例，AI深度分析相似案例', icon: Reading, color: 'linear-gradient(135deg, #a18cd1, #fbc2eb)', link: '/cases' }
]

const services = [
  { title: '律师智能匹配', desc: '根据法律问题AI推荐专业律师，支持协同过滤精准推荐', badge: '推荐', link: '/lawyers' },
  { title: '常见问题速查', desc: '覆盖劳动法、婚姻法、合同法等高频法律问题解答', badge: '免费', link: '/faq' },
  { title: '数据安全合规', desc: '全链路加密传输，数据脱敏存储，符合法律行业合规要求', badge: '安全', link: '/chat' }
]
</script>

<style scoped>
.home {
  overflow-x: hidden;
}

/* Hero */
.hero {
  position: relative;
  min-height: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 40%, #0f3460 100%);
  z-index: 0;
}

.hero-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 50%, rgba(79, 195, 247, 0.15) 0%, transparent 50%),
              radial-gradient(circle at 70% 80%, rgba(102, 126, 234, 0.1) 0%, transparent 40%);
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 80px 20px 60px;
  max-width: 700px;
}

.hero-title {
  font-size: 56px;
  font-weight: 800;
  margin: 0 0 12px;
  letter-spacing: 4px;
}

.title-accent {
  background: linear-gradient(135deg, #4fc3f7, #81d4fa, #b3e5fc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 22px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 12px;
  font-weight: 500;
}

.hero-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.55);
  margin: 0 0 36px;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.hero-actions .el-button {
  font-size: 16px;
  padding: 12px 32px;
  height: auto;
}

/* Stats */
.stats {
  display: flex;
  justify-content: center;
  gap: 48px;
  padding: 40px 20px;
  background: #fff;
  flex-wrap: wrap;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

/* Section Title */
.section-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 36px;
  position: relative;
}

.section-title::after {
  content: '';
  display: block;
  width: 40px;
  height: 3px;
  background: linear-gradient(90deg, #4fc3f7, #667eea);
  margin: 12px auto 0;
  border-radius: 2px;
}

/* Features */
.features {
  padding: 60px 20px;
  max-width: 1100px;
  margin: 0 auto;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.feature-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
  position: relative;
  overflow: hidden;
}

.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
  border-color: transparent;
}

.feature-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.feature-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.feature-card p {
  font-size: 13px;
  color: #888;
  line-height: 1.7;
  margin: 0 0 16px;
}

.feature-link {
  font-size: 13px;
  color: #4fc3f7;
  font-weight: 500;
}

/* Services */
.services {
  padding: 60px 20px;
  background: #f8fafc;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  max-width: 1100px;
  margin: 0 auto;
}

.service-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px 24px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid #ebeef5;
  position: relative;
}

.service-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
  border-color: #4fc3f7;
}

.service-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #4fc3f7, #667eea);
  color: #fff;
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 500;
}

.service-card h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.service-card p {
  font-size: 13px;
  color: #888;
  line-height: 1.6;
  margin: 0;
}

/* CTA */
.cta {
  text-align: center;
  padding: 80px 20px;
  background: linear-gradient(135deg, #1a1a2e, #0f3460);
  color: #fff;
}

.cta h2 {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 12px;
}

.cta p {
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 28px;
  font-size: 15px;
}

.cta .el-button {
  font-size: 16px;
  padding: 12px 40px;
  height: auto;
}

/* Responsive */
@media (max-width: 768px) {
  .hero-title { font-size: 36px; }
  .hero-subtitle { font-size: 18px; }
  .feature-grid, .service-grid { grid-template-columns: 1fr; }
  .stats { gap: 24px; }
  .stat-value { font-size: 24px; }
}
</style>
