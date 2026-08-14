<template>
  <section class="page-panel">
    <div class="page-intro">
      <div>
        <h2>风险报告归档</h2>
        <p>这里用于集中展示风险测评和模拟引导结果，当前版本尚未接入后端持久化。</p>
      </div>
    </div>

    <div class="report-empty surface">
      <div class="empty-mark">报</div>
      <strong>暂无历史报告</strong>
      <p>完成风险测评和模拟引导后，可在这里沉淀为可追踪的学习记录。</p>
    </div>
  </section>
</template>

<style scoped>
.report-empty {
  min-height: 420px;
  display: grid;
  place-content: center;
  justify-items: center;
  text-align: center;
  padding: 32px;
}

.empty-mark {
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  margin-bottom: 16px;
  border-radius: 8px;
  background: #e9f3ff;
  color: #0b4f82;
  font-size: 24px;
  font-weight: 800;
}

.report-empty strong {
  color: #132a3a;
  font-size: 22px;
}

.report-empty p {
  max-width: 460px;
  margin: 10px 0 0;
  color: #657789;
  line-height: 1.7;
}
</style>
