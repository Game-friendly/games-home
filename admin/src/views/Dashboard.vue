<script setup>
import { ref, onMounted } from 'vue';
import api from '../api';

const data = ref({});
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    data.value = await api.get('/admin/dashboard');
  } finally {
    loading.value = false;
  }
}

function yuan(fen) {
  return ((fen || 0) / 100).toFixed(2);
}

onMounted(load);
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never" style="margin-bottom: 16px">
      <div style="display: flex; align-items: center; justify-content: space-between">
        <span style="font-weight: 700">经营概览</span>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </el-card>

    <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px">
      <el-card shadow="never"><div class="num">{{ data.totalOrders || 0 }}</div><div class="label">总订单</div></el-card>
      <el-card shadow="never"><div class="num">{{ data.publishedOrders || 0 }}</div><div class="label">待接单</div></el-card>
      <el-card shadow="never"><div class="num">{{ data.completedOrders || 0 }}</div><div class="label">已完成</div></el-card>
      <el-card shadow="never"><div class="num">{{ data.disputedOrders || 0 }}</div><div class="label">争议中</div></el-card>
      <el-card shadow="never"><div class="num">{{ data.openDisputes || 0 }}</div><div class="label">待处理争议</div></el-card>
      <el-card shadow="never"><div class="num">{{ data.workers || 0 }}</div><div class="label">接单人</div></el-card>
      <el-card shadow="never"><div class="num">{{ data.clients || 0 }}</div><div class="label">客户</div></el-card>
      <el-card shadow="never"><div class="num">¥{{ yuan(data.pendingEarnings) }}</div><div class="label">待结算收益</div></el-card>
      <el-card shadow="never"><div class="num">¥{{ yuan(data.settledEarnings) }}</div><div class="label">已结算收益</div></el-card>
    </div>
  </div>
</template>

<style scoped>
.num { font-size: 28px; font-weight: 700; color: #1a1a2e; }
.label { color: #999; margin-top: 6px; }
</style>
