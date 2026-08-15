<template>
  <div class="auth-layout">
    <div class="left-section">
      <div class="content">
        <h2 class="title">智能投资助手</h2>
        <p class="text">专业的AI投资顾问，为您智能分析市场，提供专业的投资建议和风险控制</p>
        <div class="robot">
          <el-icon size="80" color="#409eff"><ChatDotRound /></el-icon>
        </div>
      </div>
    </div>
    <div class="right-section">
      <router-view></router-view>
    </div>
  </div>
</template>

<script setup>
import { ChatDotRound } from '@element-plus/icons-vue'
</script>

<style scoped lang="scss">
.auth-layout {
  display: flex;
  height: 100vh;

  .left-section {
    display: flex;
    justify-content: center;
    align-items: center;
    flex: 1;
    background: linear-gradient(90deg, #409eff 0%, #337ecc 100%);
    height: 100vh;

    .content {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      text-align: center;

      .title {
        font-size: 48px;
        font-weight: bold;
        color: #fff;
        margin-bottom: 20px;
      }

      .text {
        width: 500px;
        font-size: 20px;
        color: #fff;
        line-height: 1.6;
        margin-bottom: 40px;
      }

      .robot {
        display: flex;
        justify-content: center;
        align-items: center;
        width: 160px;
        height: 160px;
        border-radius: 50%;
        border: 2px solid rgba(255, 255, 255, 0.2);
        background: linear-gradient(135deg, rgba(255, 255, 255, 0.15) 0%, rgba(255, 255, 255, 0.05) 100%);
        box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.3);
      }
    }
  }

  .right-section {
    display: flex;
    justify-content: center;
    align-items: center;
    flex: 1;
    background-color: #fff;
  }
}
</style>