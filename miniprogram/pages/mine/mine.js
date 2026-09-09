const api = require('../../utils/api');
const app = getApp();

Page({
  data: { myOrders: [], accepted: [], reviews: [], me: null },

  onShow() {
    this.loadMe();
    api.request('/api/orders/my').then(list => this.setData({ myOrders: list })).catch(() => {});
    if (this.data.me && this.data.me.role !== 'CLIENT') {
      api.request('/api/orders/accepted').then(list => this.setData({ accepted: list })).catch(() => {});
      api.request('/api/reviews/mine').then(list => this.setData({ reviews: list })).catch(() => {});
    }
  },

  loadMe() {
    api.request('/api/me').then(me => {
      this.setData({ me });
      app.globalData.user = Object.assign({}, app.globalData.user || {}, me);
      if (me.serverTime) app.globalData.serverOffset = me.serverTime - Date.now();
      if (me.role !== 'CLIENT') {
        api.request('/api/orders/accepted').then(list => this.setData({ accepted: list })).catch(() => {});
        api.request('/api/reviews/mine').then(list => this.setData({ reviews: list })).catch(() => {});
      }
    }).catch(() => {});
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  },
  goEarnings() {
    wx.navigateTo({ url: '/pages/earnings/earnings' });
  },
  goNotices() {
    wx.navigateTo({ url: '/pages/notices/notices' });
  },
  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },
  switchRole() {
    app.globalData.token = '';
    app.globalData.user = null;
    wx.removeStorageSync('token');
    wx.reLaunch({ url: '/pages/login/login' });
  }
});
