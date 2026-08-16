<template>
  <div class="profile-page">
    <div class="page-card">
      <h2>个人中心</h2>

      <div class="avatar-section">
        <el-upload
          class="avatar-uploader"
          action="/api/upload"
          :show-file-list="false"
          :headers="uploadHeaders"
          :on-success="handleAvatarSuccess"
          :before-upload="beforeAvatarUpload">
          <img v-if="profile.avatar" :src="profile.avatar" class="avatar-img" />
          <div v-else class="avatar-placeholder">上传头像</div>
        </el-upload>
        <p class="avatar-tip">点击上传头像（支持 jpg/png，不超过 5MB）</p>
      </div>

      <el-form label-width="90px" class="profile-form">
        <el-form-item label="用户名">
          <el-input :model-value="profile.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="profile.nickname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profile.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="profile.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
            <el-radio label="保密">保密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="profile.age" :min="18" :max="100" />
        </el-form-item>
        <el-form-item label="个性签名">
          <el-input v-model="profile.signature" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item>
          <button class="save-btn" @click="saveProfile">保存修改</button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card" style="margin-top: 18px">
      <h2>修改密码</h2>
      <el-form label-width="90px">
        <el-form-item label="旧密码">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password />
        </el-form-item>
        <el-form-item>
          <button class="save-btn warning" @click="changePassword">
            修改密码
          </button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const profile = ref({})
const passwordForm = ref({ oldPassword: '', newPassword: '' })

const uploadHeaders = { Authorization: 'Bearer ' + (sessionStorage.getItem('token') || '') }

const handleAvatarSuccess = (res) => {
  if (res.errno === 0) {
    profile.value.avatar = res.data.url
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}

const beforeAvatarUpload = (file) => {
  const isImg = file.type.startsWith('image/')
  if (!isImg) ElMessage.error('请选择图片文件')
  return isImg
}

const loadProfile = async () => {
  const res = await axios.get('/api/user/profile')
  profile.value = res.data
}

const saveProfile = async () => {
  await axios.put('/api/user/profile', {
    nickname: profile.value.nickname,
    gender: profile.value.gender,
    age: profile.value.age,
    signature: profile.value.signature,
    phone: profile.value.phone,
    avatar: profile.value.avatar,
  })
  sessionStorage.setItem('nickname', profile.value.nickname)
  ElMessage.success('保存成功')
}

const changePassword = async () => {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    ElMessage.warning('请输入旧密码和新密码')
    return
  }
  await axios.put('/api/user/password', {
    oldPassword: passwordForm.value.oldPassword,
    newPassword: passwordForm.value.newPassword,
  })
  ElMessage.success('密码修改成功')
  passwordForm.value = { oldPassword: '', newPassword: '' }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  max-width: 600px;
  margin: 0 auto;
}
.page-card {
  background: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-soft);
  padding: 24px;
  margin-top: 0;
}
.page-card:not(:first-child) {
  margin-top: 18px;
}
h2 {
  color: var(--text-dark);
  margin-bottom: 18px;
}
.avatar-section {
  text-align: center;
  margin-bottom: 20px;
}
.avatar-uploader {
  display: inline-block;
}
.avatar-img {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #eef0f4;
  cursor: pointer;
}
.avatar-placeholder {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  border: 2px dashed #c0c4cc;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 13px;
  cursor: pointer;
  background: #fafafa;
}
.avatar-tip {
  color: #999;
  font-size: 12px;
  margin-top: 8px;
}
.profile-form {
  margin-bottom: 0;
}
.save-btn {
  padding: 10px 28px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #ff9a8b, #ff6a88);
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}
.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 106, 136, 0.4);
}
.save-btn.warning {
  background: linear-gradient(135deg, #a4508b, #6a3d8b);
}
</style>
