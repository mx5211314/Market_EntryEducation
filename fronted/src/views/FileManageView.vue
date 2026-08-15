<template>
  <div>
    <h2>文件管理</h2>
    <p>上传和管理用户头像、文章配图等文件。</p>
    <el-upload
      action="/api/file/upload"
      :headers="{ Authorization: 'Bearer ' + token }">
      <el-button type="primary">上传文件</el-button>
    </el-upload>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const token = computed(() => sessionStorage.getItem('token') || '')
</script>
