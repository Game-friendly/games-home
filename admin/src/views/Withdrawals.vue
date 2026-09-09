<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import api from '../api';

const list = ref([]);
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    list.value = await api.get('/admin/withdrawals');
  } finally {
    loading.value = false;
  }
}

function yuan(fen) {
  return (fen / 100).toFixed(2);
}

async function approve(row) {
  await api.post(`/admin/withdrawals/${row.id}/approve`);
  ElMessage.success('已打款');
  load();
}

async function reject(row) {
  await api.post(`/admin/withdrawals/${row.id}/reject`, { remark: '管理员驳回' });
  ElMessage.success('已驳回');
  load();
}

onMounted(load);
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span style="font-weight: 700">提现审核</span></template>
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="workerId" label="接单人ID" width="100" />
      <el-table-column label="金额" width="120">
        <template #default="{ row }">¥{{ yuan(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PAID' ? 'success' : row.status === 'REJECTED' ? 'info' : 'warning'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="申请时间" min-width="170" />
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button size="small" type="success" @click="approve(row)">打款</el-button>
            <el-button size="small" type="danger" @click="reject(row)">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
