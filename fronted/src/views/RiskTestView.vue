<template>
  <div class="risk-page">
    <div class="page-card">
      <h2>📊 投资者风险测评</h2>
      <p class="desc">根据《投资者适当性管理办法》，请完成以下测评：</p>

      <el-card v-for="(q, idx) in questions" :key="idx" class="question-card">
        <p class="question-text">{{ idx + 1 }}. {{ q.text }}</p>
        <el-radio-group v-model="answers[idx]">
          <el-radio
            v-for="opt in q.options"
            :key="opt.value"
            :value="opt.value">
            {{ opt.label }}
          </el-radio>
        </el-radio-group>
      </el-card>

      <button class="submit-btn" @click="submit" :disabled="!allAnswered">
        提交测评
      </button>

      <el-dialog v-model="resultVisible" title="测评结果" width="400px">
        <p class="result-level">
          您的风险等级：<strong>{{ result }}</strong>
        </p>
        <p class="result-biz">可参与业务：{{ suitableBiz }}</p>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const questions = [
  {
    text: '您的投资经验年限？',
    options: [
      { value: 1, label: '少于1年' },
      { value: 2, label: '1-5年' },
      { value: 3, label: '5年以上' },
    ],
  },
  {
    text: '您能承受的最大投资亏损比例？',
    options: [
      { value: 1, label: '亏损5%以下' },
      { value: 2, label: '亏损5%-15%' },
      { value: 3, label: '亏损15%以上' },
    ],
  },
  {
    text: '您对融资融券的了解程度？',
    options: [
      { value: 1, label: '不了解' },
      { value: 2, label: '一般了解' },
      { value: 3, label: '非常熟悉' },
    ],
  },
]

const answers = ref(questions.map(() => null))
const resultVisible = ref(false)
const result = ref('')
const suitableBiz = ref('')

const allAnswered = computed(() => answers.value.every((a) => a !== null))

const submit = () => {
  const total = answers.value.reduce((a, b) => a + b, 0)
  let level = '保守型'
  if (total >= 7) level = '积极型'
  else if (total >= 5) level = '稳健型'

  result.value = level
  suitableBiz.value =
    level === '保守型'
      ? '仅限低风险产品（如国债、货币基金）'
      : level === '稳健型'
        ? '股票、基金、债券等'
        : '股票、融资融券、科创板、期货等（请谨慎参与）'
  resultVisible.value = true
}
</script>

<style scoped>
.risk-page {
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
.question-card {
  margin-bottom: 14px;
}
.question-text {
  font-weight: 600;
  color: var(--text-dark);
  margin-bottom: 10px;
}
.submit-btn {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff9a8b, #ff6a88);
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 8px;
}
.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(255, 106, 136, 0.4);
}
.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.result-level {
  font-size: 16px;
  margin-bottom: 8px;
}
.result-biz {
  color: var(--text-muted);
  font-size: 14px;
}
</style>
