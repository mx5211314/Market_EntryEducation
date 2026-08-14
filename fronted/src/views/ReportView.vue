<template>
  <div class="report-page">
    <div class="page-card">
      <h2>📋 历史风险报告</h2>
      <p class="desc">您的测评记录和模拟引导报告将在这里展示。</p>

      <div class="empty-report">
        <span class="empty-icon">📄</span>
        <p>暂无历史报告</p>
        <span class="hint">请先完成风险测评和模拟引导</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.report-page {
  max-width: 700px;
  margin: 0 auto;
}
.page-card {
  background: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-soft);
  padding: 24px;
}
h2 {
  color: var(--text-dark);
  margin-bottom: 6px;
}
.desc {
  color: var(--text-muted);
  font-size: 14px;
  margin-bottom: 18px;
}
.empty-report {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
}
.empty-icon {
  font-size: 40px;
}
.empty-report p {
  color: var(--text-dark);
  font-size: 16px;
  margin: 0;
}
.hint {
  color: var(--text-muted);
  font-size: 13px;
}
</style>
