<script setup>
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '../api';

const list = ref([]);
const workers = ref([]);
const statusFilter = ref('');
const loading = ref(false);
const categories = ref([]);

const statuses = ['PENDING_PAYMENT', 'PUBLISHED', 'RESERVED', 'ACCEPTED', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'REFUNDED', 'DISPUTED'];

const filtered = computed(() =>
  statusFilter.value ? list.value.filter(o => o.status === statusFilter.value) : list.value
);

async function load() {
  loading.value = true;
  try {
    list.value = await api.get('/admin/orders');
    workers.value = await api.get('/admin/workers');
    const theme = await api.get('/theme');
    categories.value = theme.categories || [];
  } finally {
    loading.value = false;
  }
}

function yuan(fen) {
  return (fen / 100).toFixed(2);
}

function workerText(o) {
  const names = (o.workers || []).map(w => `${w.workerGrade || '?'}级#${w.workerId}`).join('、');
  return names || '-';
}

// ---- 编辑单 ----
const editVisible = ref(false);
const editForm = ref({});
function openEdit(o) {
  editForm.value = {
    id: o.id,
    title: o.title,
    category: o.category,
    grade: o.grade,
    budget: yuan(o.budget),
    requiredWorkers: o.requiredWorkers,
    description: o.description
  };
  editVisible.value = true;
}
async function saveEdit() {
  await api.patch(`/admin/orders/${editForm.value.id}`, {
    title: editForm.value.title,
    category: editForm.value.category,
    grade: editForm.value.grade,
    budget: Math.round(parseFloat(editForm.value.budget) * 100),
    requiredWorkers: Number(editForm.value.requiredWorkers),
    description: editForm.value.description
  });
  ElMessage.success('已修改');
  editVisible.value = false;
  load();
}

// ---- 派单 ----
const dispatchVisible = ref(false);
const dispatchForm = ref({ orderId: null, workerId: null });
const workerOptions = computed(() =>
  workers.value.filter(w => w.role === 'WORKER').map(w => ({ id: w.id, label: `${w.nickname}（${w.grade}级）` }))
);
function openDispatch(o) {
  dispatchForm.value = { orderId: o.id, workerId: null };
  dispatchVisible.value = true;
}
async function saveDispatch() {
  if (!dispatchForm.value.workerId) return ElMessage.warning('请选择接单人');
  await api.post(`/admin/orders/${dispatchForm.value.orderId}/dispatch`, { workerId: dispatchForm.value.workerId });
  ElMessage.success('已派单');
  dispatchVisible.value = false;
  load();
}

// ---- 结算 ----
async function settle(o) {
  await ElMessageBox.confirm(`确认结算订单「${o.title}」的所有收益？`, '结算', { type: 'warning' });
  await api.post(`/admin/orders/${o.id}/settle`);
  ElMessage.success('已结算');
  load();
}

onMounted(load);
</script>

<template>
  <div>
    <el-card shadow="never" style="margin-bottom: 16px">
      <div style="display: flex; gap: 12px; align-items: center">
        <span>状态筛选：</span>
        <el-select v-model="statusFilter" placeholder="全部" clearable style="width: 180px">
          <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" @click="load">刷新</el-button>
        <span style="margin-left: auto; color: #999">共 {{ list.length }} 单</span>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="filtered" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="grade" label="等级" width="70" />
        <el-table-column label="价格" width="90">
          <template #default="{ row }">¥{{ yuan(row.budget) }}</template>
        </el-table-column>
        <el-table-column label="人数" width="90">
          <template #default="{ row }">{{ row.filledWorkers }}/{{ row.requiredWorkers }}</template>
        </el-table-column>
        <el-table-column label="接单人" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ workerText(row) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'COMPLETED' ? 'success' : row.status === 'CANCELLED' ? 'info' : 'warning'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="buyerId" label="客户ID" width="80" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" size="small" type="primary" @click="openDispatch(row)">派单</el-button>
            <el-button v-if="row.status === 'COMPLETED'" size="small" type="success" @click="settle(row)">结算</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" title="修改订单" width="540px">
      <el-form label-width="90px">
        <el-form-item label="标题"><el-input v-model="editForm.title" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editForm.category" style="width: 100%">
            <el-option v-for="c in categories" :key="c.name" :label="c.name" :value="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="单等级">
          <el-select v-model="editForm.grade" style="width: 100%">
            <el-option v-for="g in ['A', 'B', 'C']" :key="g" :label="g + ' 级'" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格(元)"><el-input v-model="editForm.budget" type="number" /></el-form-item>
        <el-form-item label="需要人数"><el-input-number v-model="editForm.requiredWorkers" :min="1" :max="10" style="width: 100%" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dispatchVisible" title="派单（填入一个空位）" width="420px">
      <el-select v-model="dispatchForm.workerId" placeholder="选择接单人" style="width: 100%">
        <el-option v-for="w in workerOptions" :key="w.id" :label="w.label" :value="w.id" />
      </el-select>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDispatch">派单</el-button>
      </template>
    </el-dialog>
  </div>
</template>
