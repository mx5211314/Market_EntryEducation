<template>
  <div id="app">
    <LoginView v-if="!isLoggedIn" @login-success="handleLoginSuccess" />
    <router-view v-else />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import LoginView from './views/LoginView.vue'

const isLoggedIn = ref(!!localStorage.getItem('token'))

const handleLoginSuccess = () => {
  isLoggedIn.value = true
}
</script>
