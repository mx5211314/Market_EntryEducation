<template>
  <div>
    <h2>📊 投资者风险测评</h2>
    <p>根据《投资者适当性管理办法》，请完成以下测评：</p>
    <el-card v-for="(q, idx) in questions" :key="idx" class="question-card">
      <p>{{ idx + 1 }}. {{ q.text }}</p>
      <el-radio-group v-model="answers[idx]">
        <el-radio
          v-for="opt in q.options"
          :key="opt.value"
          :value="opt.value"
          >{{ opt.label }}</el-radio
        >
      </el-radio-group>
    </el-card>
    <el-button
      type="primary"
      @click="submit"
      :disabled="!allAnswered"
      style="margin-top: 15px"
      >提交测评</el-button
    >

    <el-dialog v-model="resultVisible" title="测评结果">
      <p><strong>您的风险等级：</strong>{{ result }}</p>
      <p><strong>可参与业务：</strong>{{ suitableBiz }}</p>
    </el-dialog>
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
.question-card {
  margin: 15px 0;
  border-radius: 8px;
}
h2 {
  color: #1e3c72;
  margin-bottom: 10px;
}
p {
  color: #555;
  margin-bottom: 20px;
}
</style>
