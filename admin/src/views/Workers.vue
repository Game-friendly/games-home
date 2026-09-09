<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import api from '../api';

const list = ref([]);

async function load() {
  list.value = await api.get('/admin/workers');
}

const gradeVisible = ref(false);
const gradeForm = ref({ id: null, grade: 'C' });
function openGrade(w) {
  gradeForm.value = { id: w.id, grade: w.grade };
  gradeVisible.value = true;
}
async function saveGrade() {
  await api.post(`/admin/workers/${gradeForm.value.id}/grade`, { grade: gradeForm.value.grade });
  ElMessage.success('等级已更新');
  gradeVisible.value = false;
  load();
}

onMounted(load);
</script>

<template>
  <el-card shadow="never">
    <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center">
      <span>接单人等级与信用分管理</span>
      <el-button type="primary" @click="load">刷新</el-button>
    </div>
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="role" label="角色" width="100" />
      <el-table-column label="等级" width="100">
        <template #default="{ row }">
          <el-tag :type="row.grade === 'A' ? 'danger' : row.grade === 'B' ? 'warning' : 'info'" size="small">
            {{ row.grade }} 级
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="creditScore" label="信用分" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.role !== 'ADMIN'" size="small" type="primary" @click="openGrade(row)">改等级</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="gradeVisible" title="设置接单人等级" width="360px">
    <el-select v-model="gradeForm.grade" style="width: 100%">
      <el-option v-for="g in ['A', 'B', 'C']" :key="g" :label="g + ' 级'" :value="g" />
    </el-select>
    <template #footer>
      <el-button @click="gradeVisible = false">取消</el-button>
      <el-button type="primary" @click="saveGrade">保存</el-button>
    </template>
  </el-dialog>
</template>
