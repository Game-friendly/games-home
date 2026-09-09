<script setup>
import { ref } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import axios from 'axios';

const emit = defineEmits(['success']);
const code = ref('demo_admin');
const loading = ref(false);

async function login() {
  loading.value = true;
  try {
    const res = await axios.post('/api/auth/login', { code: code.value });
    if (res.data && res.data.code === 0) {
      localStorage.setItem('admin_token', res.data.data.token);
      MessagePlugin.success('登录成功');
      emit('success');
    } else {
      MessagePlugin.error(res.data?.msg || '登录失败');
    }
  } catch (e) {
    MessagePlugin.error('登录失败，请确认后端已启动');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login">
    <t-card class="card" :bordered="false">
      <template #header>
        <div class="title">跑腿接单 · 管理后台</div>
      </template>
      <t-input v-model="code" size="large" placeholder="登录码（演示：demo_admin）" @enter="login" />
      <t-button theme="primary" block size="large" style="margin-top: 18px" :loading="loading" @click="login">
        登录
      </t-button>
    </t-card>
  </div>
</template>

<style scoped>
.login {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #2b2f45 100%);
}
.card {
  width: 360px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}
.title {
  text-align: center;
  font-weight: 700;
  font-size: 18px;
}
</style>
