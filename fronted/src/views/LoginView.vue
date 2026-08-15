<template>
    <div class="container">
        <div class="title">
            <div class="back-home" @click="router.push('/')">
                <el-icon><ArrowLeft /></el-icon>
                <span>返回首页</span>
            </div>
            <div class="title-text">
                <h2>登录您的账户</h2>
                <p>请输入您的登录信息</p>
            </div>
        </div>
        <div class="form-container">
            <el-form
                ref="ruleFormRef"
                :model="formData"
                :rules="rules"
                label-position="top"
            >
                <el-form-item label="用户名或邮箱" prop="username">
                    <el-input v-model="formData.username" size="large" placeholder="请输入用户名" />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input v-model="formData.password" size="large" placeholder="请输入密码" type="password" show-password />
                </el-form-item>
                <el-button class="btn" size="large" type="primary" @click="submitForm(ruleFormRef)" :loading="loading">
                    {{ loading ? '登录中...' : '登录' }}
                </el-button>
            </el-form>
            <div class="footer">
                <p>还没有账户？<router-link to="/register">去注册</router-link></p>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { login } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const ruleFormRef = ref()
const loading = ref(false)

const formData = reactive({
    username: '',
    password: ''
})

const rules = reactive({
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
    ]
})

const submitForm = async (formEl) => {
    if (!formEl) return

    try {
        await formEl.validate()
        loading.value = true

        try {
            const res = await login(formData)

            // 保存登录信息
            sessionStorage.setItem('token', res.token)
            sessionStorage.setItem('username', res.username)
            sessionStorage.setItem('nickname', res.nickname || res.username)
            sessionStorage.setItem('role', res.role)
            sessionStorage.setItem('userInfo', JSON.stringify({
                username: res.username,
                nickname: res.nickname,
                role: res.role
            }))

            ElMessage.success({
                message: '登录成功',
                duration: 1000
            })

            // 根据用户角色跳转
            if (res.role === 'ADMIN') {
                router.push('/admin/dashboard')
            } else {
                // 获取重定向地址或跳转到首页
                const redirect = sessionStorage.getItem('redirect') || '/'
                sessionStorage.removeItem('redirect')
                router.push(redirect)
            }
        } catch (error) {
            console.error('登录失败:', error)
        } finally {
            loading.value = false
        }
    } catch (error) {
        // 表单验证失败，不处理错误，Element Plus会显示错误信息
        console.log('表单验证失败:', error)
    }
}
</script>

<style scoped lang="scss">
.container {
    width: 384px;
    margin: 0 auto;
    padding: 20px 0;

    .title {
        .back-home {
            margin-bottom: 60px;
            display: flex;
            align-items: center;
            gap: 8px;
            color: #409eff;
            cursor: pointer;
            font-size: 14px;

            .el-icon {
                font-size: 16px;
            }

            &:hover {
                color: #66b1ff;
            }
        }

        .title-text {
            text-align: center;
            h2 {
                font-size: 36px;
                margin-bottom: 10px;
                color: #303133;
            }
            p {
                font-size: 18px;
                color: #909399;
            }
        }
    }

    .form-container {
        margin-top: 30px;

        .btn {
            margin-top: 40px;
            width: 100%;
            height: 44px;
            font-size: 16px;
        }

        .footer {
            padding: 30px 0;
            text-align: center;

            p {
                color: #606266;
                font-size: 14px;

                a {
                    color: #409eff;
                    text-decoration: none;

                    &:hover {
                        color: #66b1ff;
                    }
                }
            }
        }
    }
}
</style>