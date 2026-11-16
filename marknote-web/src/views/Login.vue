<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <div class="logo">M</div>
        <div>
          <h1>MarkNote</h1>
          <p>仿 mdnice 的 Markdown 笔记系统</p>
        </div>
      </div>

      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-position="top" @submit.prevent>
            <el-form-item label="账号">
              <el-input v-model="loginForm.username" placeholder="admin" size="large" clearable />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="123456"
                size="large"
                show-password
                @keyup.enter="doLogin"
              />
            </el-form-item>
            <el-button type="primary" size="large" class="submit" :loading="loading" @click="doLogin">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="regForm" label-position="top" @submit.prevent>
            <el-form-item label="账号">
              <el-input v-model="regForm.username" placeholder="请输入账号" size="large" clearable />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="regForm.password" type="password" placeholder="请输入密码" size="large" show-password />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="regForm.nickname" placeholder="选填" size="large" clearable />
            </el-form-item>
            <el-button type="primary" size="large" class="submit" :loading="loading" @click="doRegister">
              注册并登录
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="tip">演示账号：admin / 123456</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '@/api/user'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const tab = ref('login')
const loading = ref(false)
const loginForm = reactive({ username: 'admin', password: '123456' })
const regForm = reactive({ username: '', password: '', nickname: '' })

async function doLogin() {
  if (!loginForm.username || !loginForm.password) {
    return ElMessage.warning('请输入账号和密码')
  }
  loading.value = true
  try {
    const res = await login({ username: loginForm.username, password: loginForm.password })
    userStore.setLogin(res.data)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  if (!regForm.username || !regForm.password) {
    return ElMessage.warning('请输入账号和密码')
  }
  loading.value = true
  try {
    await register({ ...regForm })
    ElMessage.success('注册成功，正在登录')
    const res = await login({ username: regForm.username, password: regForm.password })
    userStore.setLogin(res.data)
    router.push('/')
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef4ff 0%, #f7f8fa 45%, #eef7f1 100%);
}

.login-card {
  width: 420px;
  padding: 32px 34px 24px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 18px 50px rgba(30, 55, 90, 0.12);
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.logo {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6, #22c55e);
  color: #fff;
  font-weight: 700;
  font-size: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand h1 {
  margin: 0;
  font-size: 20px;
  letter-spacing: 0.5px;
}

.brand p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #8a8f99;
}

.submit {
  width: 100%;
  margin-top: 6px;
}

.tip {
  margin-top: 18px;
  text-align: center;
  font-size: 12px;
  color: #9aa0a8;
}
</style>
