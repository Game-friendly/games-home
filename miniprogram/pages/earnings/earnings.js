const api = require('../../utils/api');

Page({
  data: { list: [], totalPending: 0, withdrawable: 0, withdrawals: [], withdrawAmount: '' },

  onShow() {
    this.load();
  },

  load() {
    api.request('/api/earnings/mine').then(list => {
      const total = list.filter(e => e.status === 'PENDING').reduce((s, e) => s + e.amount, 0);
      this.setData({
        list: list.map(e => ({
          ...e,
          amountYuan: (e.amount / 100).toFixed(2),
          statusText: e.status === 'SETTLED' ? '已结算' : '待结算'
        })),
        totalPending: (total / 100).toFixed(2)
      });
    }).catch(() => {});

    api.request('/api/earnings/withdrawable').then(d => {
      this.setData({ withdrawable: (d.amount / 100).toFixed(2) });
    }).catch(() => {});

    api.request('/api/earnings/withdrawals').then(list => {
      this.setData({
        withdrawals: list.map(w => ({
          ...w,
          amountYuan: (w.amount / 100).toFixed(2),
          statusText: w.status === 'PAID' ? '已打款' : w.status === 'REJECTED' ? '已驳回' : '审核中'
        }))
      });
    }).catch(() => {});
  },

  onAmount(e) {
    this.setData({ withdrawAmount: e.detail.value });
  },

  withdraw() {
    const amount = Math.round(parseFloat(this.data.withdrawAmount || '0') * 100);
    if (amount <= 0) return wx.showToast({ title: '请输入提现金额', icon: 'none' });
    api.request('/api/earnings/withdrawals', 'POST', { amount }).then(() => {
      wx.showToast({ title: '提现申请已提交', icon: 'success' });
      this.setData({ withdrawAmount: '' });
      this.load();
    });
  },

  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) wx.navigateBack();
    else wx.switchTab({ url: '/pages/pool/pool' });
  }
});
