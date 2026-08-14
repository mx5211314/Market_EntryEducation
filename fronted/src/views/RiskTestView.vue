<template>
  <section class="page-panel">
    <div class="page-intro">
      <div>
        <h2>投资者风险测评</h2>
        <p>根据投资经验、亏损承受能力和业务理解程度，给出基础风险等级和适配业务范围。</p>
      </div>
      <el-tag v-if="result" size="large" effect="plain">{{ result }}</el-tag>
    </div>

    <div class="risk-layout">
      <div class="questions surface">
        <div v-for="(q, idx) in questions" :key="idx" class="question-item">
          <div class="question-title">
            <span>{{ idx + 1 }}</span>
            <strong>{{ q.text }}</strong>
          </div>
          <el-radio-group v-model="answers[idx]" class="option-list">
            <el-radio-button v-for="opt in q.options" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>
        </div>
        <el-button type="primary" size="large" @click="submit" :disabled="!allAnswered">提交测评</el-button>
      </div>

      <div class="result-card surface">
        <template v-if="result">
          <span>您的风险等级</span>
          <strong>{{ result }}</strong>
          <p>{{ suitableBiz }}</p>
        </template>
        <template v-else>
          <span>测评进度</span>
          <strong>{{ answeredCount }} / {{ questions.length }}</strong>
          <p>完成所有题目后，将生成可参与业务范围。</p>
        </template>
      </div>
    </div>
  </section>
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
const result = ref('')
const suitableBiz = ref('')

const answeredCount = computed(() => answers.value.filter((a) => a !== null).length)
const allAnswered = computed(() => answers.value.every((a) => a !== null))

const submit = () => {
  const total = answers.value.reduce((a, b) => a + b, 0)
  let level = '保守型'
  if (total >= 7) level = '积极型'
  else if (total >= 5) level = '稳健型'

  result.value = level
  suitableBiz.value =
    level === '保守型'
      ? '仅限低风险产品，如国债、货币基金。'
      : level === '稳健型'
        ? '可关注股票、基金、债券等常见品类。'
        : '可进一步了解股票、融资融券、科创板、期货等业务，但应严格控制风险。'
}
</script>

<style scoped>
.risk-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
}

.questions,
.result-card {
  padding: 20px;
}

.question-item {
  padding: 18px 0;
  border-bottom: 1px solid #edf1f5;
}

.question-item:first-child {
  padding-top: 0;
}

.question-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.question-title span {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #e9f3ff;
  color: #0b4f82;
  font-weight: 700;
}

.option-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.result-card {
  align-self: start;
  position: sticky;
  top: 28px;
}

.result-card span {
  color: #738699;
  font-size: 13px;
}

.result-card strong {
  display: block;
  margin: 10px 0;
  color: #0b4f82;
  font-size: 34px;
}

.result-card p {
  margin: 0;
  color: #516577;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .risk-layout {
    grid-template-columns: 1fr;
  }

  .result-card {
    position: static;
  }
}
</style>
