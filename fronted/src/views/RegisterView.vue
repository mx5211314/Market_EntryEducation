<template>
    <div class="auth-panel">
        <div class="back-home" @click="router.push('/')">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回首页</span>
        </div>

        <div class="panel-head">
            <h2>创建账户</h2>
            <p>只需用户名和密码，30 秒完成注册</p>
        </div>

        <el-form
            ref="submitFormRef"
            :model="formData"
            :rules="rules"
            label-position="top"
            class="auth-form"
            @submit.prevent="submitForm(submitFormRef)">
            <el-form-item label="用户名" prop="username">
                <el-input
                    v-model="formData.username"
                    size="large"
                    placeholder="3-20 个字符，登录时使用"
                    :prefix-icon="User"
                    @keyup.enter="submitForm(submitFormRef)" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
                <el-input
                    v-model="formData.password"
                    size="large"
                    type="password"
                    placeholder="6-20 个字符"
                    :prefix-icon="Lock"
                    show-password
                    @keyup.enter="submitForm(submitFormRef)" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                    v-model="formData.confirmPassword"
                    size="large"
                    type="password"
                    placeholder="请再次输入密码"
                    :prefix-icon="Lock"
                    show-password
                    @keyup.enter="submitForm(submitFormRef)" />
            </el-form-item>

            <el-button
                class="submit-btn"
                size="large"
                type="primary"
                native-type="submit"
                :loading="loading">
                {{ loading ? '注册中...' : '注册' }}
            </el-button>
        </el-form>

        <div class="panel-foot">
            已有账户？<router-link to="/auth/login">去登录</router-link>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, User, Lock } from '@element-plus/icons-vue'
import { register } from '@/api/auth'

const router = useRouter()
const submitFormRef = ref()
const loading = ref(false)

const formData = reactive({
    username: '',
    password: '',
    confirmPassword: ''
})

const validatePass2 = (rule, value, callback) => {
    if (value === '') {
        callback(new Error('请再次输入密码'))
    } else if (value !== formData.password) {
        callback(new Error('两次输入密码不一致'))
    } else {
        callback()
    }
}

const rules = reactive({
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请再次输入密码', trigger: 'blur' },
        { validator: validatePass2, trigger: 'blur' }
    ]
})

const submitForm = async (formEl) => {
    if (!formEl || loading.value) return
    try {
        await formEl.validate()
    } catch {
        return
    }

    loading.value = true
    try {
        // 昵称留空时后端会用用户名兜底，这里不再让用户多填一格
        await register({ username: formData.username, password: formData.password })
        ElMessage.success('注册成功，请登录')
        router.push('/auth/login')
    } catch (error) {
        console.error('注册失败:', error)
        ElMessage.error(error?.response?.data?.message || '注册失败，用户名可能已被占用')
    } finally {
        loading.value = false
    }
}
</script>

<style scoped lang="scss">
$brand: #409eff;

.auth-panel {
    width: 400px;
    padding: 8px 0;
}

.back-home {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 32px;
    font-size: 13px;
    color: $brand;
    cursor: pointer;
    transition: color 0.3s cubic-bezier(0.22, 1, 0.36, 1);

    &:hover { color: #66b1ff; }
}

.panel-head {
    margin-bottom: 28px;

    h2 {
        margin: 0 0 8px;
        font-size: 28px;
        font-weight: 800;
        color: #333;
    }

    p {
        margin: 0;
        font-size: 14px;
        color: #999;
    }
}

.auth-form {
    :deep(.el-form-item) { margin-bottom: 20px; }

    :deep(.el-form-item__label) {
        font-size: 13px;
        color: #666;
        padding-bottom: 6px;
    }

    :deep(.el-input__wrapper) {
        border-radius: 10px;
        box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.2) inset;

        &.is-focus { box-shadow: 0 0 0 1px $brand inset; }
    }
}

.submit-btn {
    width: 100%;
    height: 46px;
    margin-top: 8px;
    border: none;
    border-radius: 10px;
    font-size: 15px;
    font-weight: 600;
    background: linear-gradient(135deg, $brand, #66b1ff);
    box-shadow: 0 4px 16px rgba(64, 158, 255, 0.25);
    transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

    &:hover:not(.is-loading) {
        transform: translateY(-1px);
        box-shadow: 0 8px 22px rgba(64, 158, 255, 0.32);
    }
}

.panel-foot {
    margin-top: 26px;
    text-align: center;
    font-size: 13px;
    color: #666;

    a {
        color: $brand;
        font-weight: 600;
        text-decoration: none;

        &:hover { color: #66b1ff; }
    }
}
</style>
