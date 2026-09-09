<script setup>
import { ref, computed } from 'vue';
import Login from './views/Login.vue';
import Orders from './views/Orders.vue';
import Workers from './views/Workers.vue';
import Earnings from './views/Earnings.vue';
import Dashboard from './views/Dashboard.vue';
import Disputes from './views/Disputes.vue';
import Withdrawals from './views/Withdrawals.vue';

const logged = ref(!!localStorage.getItem('admin_token'));
const menu = ref('dashboard');

const current = computed(() => {
  if (!logged.value) return Login;
  return { dashboard: Dashboard, orders: Orders, workers: Workers, earnings: Earnings, disputes: Disputes, withdrawals: Withdrawals }[menu.value];
});
</script>

<template>
  <Login v-if="!logged" @success="logged = true" />
  <t-layout v-else style="height: 100vh">
    <t-aside width="220px" style="background: #1a1a2e">
      <div class="brand">跑腿接单后台</div>
      <t-menu :value="menu" theme="dark" @change="m => (menu = m)">
        <t-menu-item value="dashboard">统计看板</t-menu-item>
        <t-menu-item value="orders">订单管理</t-menu-item>
        <t-menu-item value="workers">接单人管理</t-menu-item>
        <t-menu-item value="earnings">收益结算</t-menu-item>
        <t-menu-item value="disputes">争议处理</t-menu-item>
        <t-menu-item value="withdrawals">提现审核</t-menu-item>
      </t-menu>
    </t-aside>
    <t-layout>
      <t-content style="background: #f5f5f7; padding: 16px; overflow: auto">
        <component :is="current" />
      </t-content>
    </t-layout>
  </t-layout>
</template>

<style scoped>
.brand {
  color: #fff;
  padding: 22px 18px;
  font-weight: 700;
  font-size: 16px;
}
</style>
