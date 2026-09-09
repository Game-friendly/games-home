<script setup>
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '../api';

const list = ref([]);
const onlyPending = ref(false);

const filtered = computed(() =>
  onlyPending.value ? list.value.filter(e => e.status === 'PENDING') : list.value
);
const totalPending = computed(() =>
  list.value.filter(e => e.status === 'PENDING').reduce((s, e) => s + e.amount, 0)
);

async function load() {
  list.value = await api.get('/admin/earnings');
}

function yuan(fen) {
  return (fen / 100).toFixed(2);
}

async function settle(e) {
  await ElMessageBox.confirm(`确认结算该笔收益（订单 ${e.orderId}，¥${yuan(e.amount)}）？`, '结算', { type: 'warning' });
  await api.post(`/admin/orders/${e.orderId}/settle`);
  ElMessage.success('已结算');
  load();
}

onMounted(load);
</script>

<template>
  <div>
    <el-card shadow="never" style="margin-bottom: 16px">
      <div style="display: flex; gap: 12px; align-items: center">
        <el-checkbox v-model="onlyPending">只看待结算</el-checkbox>
        <el-button type="primary" @click="load">刷新</el-button>
        <span style="margin-left: auto; color: #999">待结算总额：¥{{ yuan(totalPending) }}</span>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="filtered" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="orderId" label="订单ID" width="90" />
        <el-table-column prop="workerId" label="接单人ID" width="90" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }">¥{{ yuan(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SETTLED' ? 'success' : 'warning'" size="small">
              {{ row.status === 'SETTLED' ? '已结算' : '待结算' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="入账时间" min-width="170" />
        <el-table-column prop="settledAt" label="结算时间" min-width="170">
          <template #default="{ row }">{{ row.settledAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="settle(row)">结算</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
