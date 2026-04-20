<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <div class="logo">律法通</div>
      <div class="nav-links">
        <router-link to="/">首页</router-link>
        <router-link to="/chat">法律咨询</router-link>
        <router-link to="/contract">合同分析</router-link>
      </div>
      <div class="user-area">
        <el-button type="primary" size="small">登录</el-button>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background-color: #f5f7fa;
}

.app-container {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  background: #1a1a2e;
  color: white;
  padding: 0 20px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #4fc3f7;
}

.nav-links {
  display: flex;
  gap: 20px;
  margin-left: 40px;
}

.nav-links a {
  color: #ccc;
  text-decoration: none;
  font-size: 14px;
}

.nav-links a:hover,
.nav-links a.router-link-active {
  color: #4fc3f7;
}

.user-area {
  margin-left: auto;
}
</style>
