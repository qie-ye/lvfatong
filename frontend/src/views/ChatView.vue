<template>
  <div class="chat-view">
    <el-container>
      <el-aside width="260px">
        <div class="session-header">
          <el-button type="primary" size="small" @click="newSession">新建对话</el-button>
        </div>
        <div class="session-list">
          <p class="placeholder-text">对话历史（v0.2实现）</p>
        </div>
      </el-aside>
      <el-main>
        <div class="chat-area">
          <div class="messages">
            <div class="welcome-message">
              <h3>欢迎使用律法通法律咨询</h3>
              <p>请输入您的法律问题，我将为您提供专业解答</p>
              <p class="disclaimer">⚠️ 本平台提供的信息仅供参考，不构成法律意见。如有正式法律需求，请咨询专业律师。</p>
            </div>
          </div>
          <div class="input-area">
            <el-input
              v-model="inputText"
              placeholder="请输入您的法律问题..."
              size="large"
              @keyup.enter="sendMessage"
            >
              <template #append>
                <el-button @click="sendMessage">发送</el-button>
              </template>
            </el-input>
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const inputText = ref('')

const newSession = () => {
  // TODO: v0.2
}

const sendMessage = () => {
  // TODO: v0.2 SSE流式调用
  if (!inputText.value.trim()) return
  inputText.value = ''
}
</script>

<style scoped>
.chat-view {
  height: calc(100vh - 60px);
}

.el-container {
  height: 100%;
}

.el-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  padding: 16px;
}

.session-header {
  margin-bottom: 16px;
}

.session-list {
  overflow-y: auto;
}

.placeholder-text {
  color: #999;
  font-size: 13px;
  text-align: center;
  margin-top: 20px;
}

.chat-area {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.welcome-message {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.welcome-message h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 8px;
}

.disclaimer {
  margin-top: 20px;
  padding: 12px;
  background: #fff3e0;
  border-radius: 8px;
  font-size: 13px;
  color: #e65100;
}

.input-area {
  padding: 16px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
}
</style>
