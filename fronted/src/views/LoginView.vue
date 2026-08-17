<template>
    <div class="auth-panel">
        <div class="back-home" @click="router.push('/')">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回首页</span>
        </div>

        <div class="panel-head">
            <h2>欢迎回来</h2>
            <p>登录后即可使用智能问答、风险测评与投资日记</p>
        </div>

        <el-form
            ref="ruleFormRef"
            :model="formData"
            :rules="rules"
            label-position="top"
            class="auth-form"
            @submit.prevent="submitForm(ruleFormRef)">
            <el-form-item label="用户名或邮箱" prop="username">
                <el-input
                    v-model="formData.username"
                    size="large"
                    placeholder="请输入用户名或邮箱"
                    :prefix-icon="User"
                    @keyup.enter="submitForm(ruleFormRef)" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
                <el-input
                    v-model="formData.password"
                    size="large"
                    type="password"
                    placeholder="请输入密码"
                    :prefix-icon="Lock"
                    show-password
                    @keyup.enter="submitForm(ruleFormRef)" />
            </el-form-item>

            <el-button
                class="submit-btn"
                size="large"
                type="primary"
                native-type="submit"
                :loading="loading">
                {{ loading ? '登录中...' : '登录' }}
            </el-button>
        </el-form>

        <div class="divider"><span>其他登录方式</span></div>

        <button class="oauth-btn" :disabled="githubLoading" @click="loginWithGithub">
            <svg class="gh-icon" viewBox="0 0 16 16" aria-hidden="true">
                <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.42 7.42 0 0 1 2-.27c.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A7.995 7.995 0 0 0 16 8c0-4.42-3.58-8-8-8Z" />
            </svg>
            {{ githubLoading ? '正在跳转 GitHub...' : '使用 GitHub 账号登录' }}
        </button>

        <div class="panel-foot">
            还没有账户？<router-link to="/auth/register">立即注册</router-link>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, User, Lock } from '@element-plus/icons-vue'
import { login, getGithubLoginUrl } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const ruleFormRef = ref()
const loading = ref(false)
const githubLoading = ref(false)

const formData = reactive({
    username: '',
    password: ''
})

const rules = reactive({
    username: [
        { required: true, message: '请输入用户名或邮箱', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
    ]
})

const saveAndRedirect = (info) => {
    sessionStorage.setItem('token', info.token)
    sessionStorage.setItem('username', info.username)
    sessionStorage.setItem('nickname', info.nickname || info.username)
    sessionStorage.setItem('role', info.role)
    sessionStorage.setItem('userInfo', JSON.stringify({
        username: info.username,
        nickname: info.nickname,
        role: info.role
    }))

    if (info.role === 'ADMIN') {
        router.push('/admin/dashboard')
        return
    }
    const redirect = sessionStorage.getItem('redirect') || '/'
    sessionStorage.removeItem('redirect')
    router.push(redirect)
}

const submitForm = async (formEl) => {
    if (!formEl || loading.value) return
    try {
        await formEl.validate()
    } catch {
        return
    }

    loading.value = true
    try {
        const res = await login(formData)
        ElMessage.success({ message: '登录成功', duration: 1000 })
        saveAndRedirect(res)
    } catch (error) {
        console.error('登录失败:', error)
        ElMessage.error(error?.response?.data?.message || '用户名或密码错误')
    } finally {
        loading.value = false
    }
}

const loginWithGithub = async () => {
    githubLoading.value = true
    try {
        const url = await getGithubLoginUrl()
        window.location.href = url
    } catch (error) {
        githubLoading.value = false
        ElMessage.error('获取 GitHub 授权地址失败')
    }
}

// GitHub 回调是后端 302 带 query 打回本页，这里把 token 落地再跳转
onMounted(() => {
    if (route.query.error) {
        ElMessage.error('GitHub 登录失败，请重试或使用账号密码登录')
        router.replace({ path: '/auth/login', query: {} })
        return
    }
    if (route.query.token) {
        saveAndRedirect({
            token: String(route.query.token),
            username: String(route.query.username || ''),
            nickname: String(route.query.nickname || ''),
            role: String(route.query.role || 'USER')
        })
    }
})
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

.divider {
    position: relative;
    margin: 24px 0 16px;
    text-align: center;

    &::before {
        content: '';
        position: absolute;
        top: 50%;
        left: 0;
        right: 0;
        height: 1px;
        background: rgba(64, 158, 255, 0.15);
    }

    span {
        position: relative;
        padding: 0 12px;
        background: #fff;
        font-size: 12px;
        color: #c0c4cc;
    }
}

.oauth-btn {
    width: 100%;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border: 1px solid rgba(64, 158, 255, 0.2);
    border-radius: 10px;
    background: #fff;
    font-size: 14px;
    color: #333;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

    &:hover:not(:disabled) {
        border-color: #24292f;
        background: #24292f;
        color: #fff;

        .gh-icon { fill: #fff; }
    }

    &:disabled { color: #c0c4cc; cursor: not-allowed; }

    .gh-icon {
        width: 17px;
        height: 17px;
        fill: #24292f;
        transition: fill 0.3s;
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
