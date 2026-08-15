<template>
    <div class="container">
        <div class="title">
            <div class="back-home" @click="router.push('/')">
                <el-icon><ArrowLeft /></el-icon>
                <span>返回首页</span>
            </div>
            <div class="title-text">
                <h2>创建您的账户</h2>
                <p>请填写注册信息</p>
            </div>
        </div>
        <div class="form-container">
            <el-form label-position="top" :model="formData" :rules="rules" ref="submitFormRef">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="formData.username" placeholder="请输入用户名" size="large" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                    <el-input v-model="formData.email" placeholder="请输入邮箱" size="large" />
                </el-form-item>
                <el-form-item label="昵称" prop="nickname">
                    <el-input v-model="formData.nickname" placeholder="请输入昵称(可选)" size="large" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                    <el-input v-model="formData.phone" placeholder="请输入手机号(可选)" size="large" />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input v-model="formData.password" placeholder="请输入密码" size="large" type="password" show-password />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input v-model="formData.confirmPassword" placeholder="请再次输入密码" size="large" type="password" show-password />
                </el-form-item>
                <el-button class="btn" type="primary" size="large" @click="submitForm(submitFormRef)" :loading="loading">
                    {{ loading ? '注册中...' : '注册' }}
                </el-button>
            </el-form>
            <div class="footer">
                <p>已有账户？<router-link to="/login">去登录</router-link></p>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { register } from '@/api/auth'

const router = useRouter()
const submitFormRef = ref()
const loading = ref(false)

const formData = reactive({
    username: "",
    email: "",
    nickname: "",
    phone: "",
    password: "",
    confirmPassword: "",
    gender: 0,
    userType: 1
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
        { required: true, message: "请输入用户名", trigger: "blur" },
        { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
    ],
    email: [
        { required: true, message: "请输入邮箱", trigger: "blur" },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
    ],
    password: [
        { required: true, message: "请输入密码", trigger: "blur" },
        { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: "请再次输入密码", trigger: "blur" },
        { validator: validatePass2, trigger: 'blur' }
    ]
})

const submitForm = async (formEl) => {
    if (!formEl) return

    await formEl.validate(async (valid) => {
        if (valid) {
            loading.value = true
            try {
                const res = await register(formData)
                ElMessage.success('注册成功')
                // 注册成功后跳转到登录页
                router.push('/login')
            } catch (error) {
                console.error('注册失败:', error)
            } finally {
                loading.value = false
            }
        }
    })
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