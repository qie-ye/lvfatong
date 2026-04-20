<template>
  <div class="home">
    <div class="hero">
      <h1>律法通</h1>
      <p class="subtitle">AI智能法律咨询平台</p>
      <p class="description">基于智谱AI大模型，为您提供专业、准确的法律咨询服务</p>
      <div class="actions">
        <el-button type="primary" size="large" @click="$router.push('/chat')">
          开始法律咨询
        </el-button>
        <el-button size="large" @click="$router.push('/contract')">
          合同分析
        </el-button>
      </div>
    </div>
    <div class="features">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover">
            <template #header>智能法律问答</template>
            <p>基于RAG技术，结合法律知识库，提供专业准确的法律咨询</p>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <template #header>合同风险分析</template>
            <p>AI自动识别合同风险条款，提供风险评估和修改建议</p>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <template #header>法律条文检索</template>
            <p>向量语义检索，快速定位相关法律条文和案例</p>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.home {
  max-width: 1000px;
  margin: 0 auto;
  padding: 40px 20px;
}

.hero {
  text-align: center;
  padding: 60px 0;
}

.hero h1 {
  font-size: 48px;
  color: #1a1a2e;
  margin-bottom: 10px;
}

.subtitle {
  font-size: 24px;
  color: #4fc3f7;
  margin-bottom: 10px;
}

.description {
  color: #666;
  margin-bottom: 30px;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.features {
  margin-top: 40px;
}
</style>
