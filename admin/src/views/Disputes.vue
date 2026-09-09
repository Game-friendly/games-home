<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import api from '../api';

const list = ref([]);
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    list.value = await api.get('/admin/disputes');
  } finally {
    loading.value = false;
  }
}

async function resolve(d, outcome) {
  await api.post(`/admin/disputes/${d.id}/resolve`, { outcome });
  ElMessage.success('已处理');
  load();
}

onMounted(load);
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span style="font-weight: 700">争议处理</span></template>
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="orderId" label="订单ID" width="90" />
      <el-table-column prop="createdBy" label="发起人" width="90" />
      <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'OPEN' ? 'danger' : 'success'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="outcome" label="裁决" width="110">
        <template #default="{ row }">{{ row.outcome || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'OPEN'">
            <el-button size="small" type="success" @click="resolve(row, 'RELEASE')">恢复订单</el-button>
            <el-button size="small" type="warning" @click="resolve(row, 'REFUND')">退款取消</el-button>
            <el-button size="small" type="danger" @click="resolve(row, 'CANCEL')">取消订单</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
