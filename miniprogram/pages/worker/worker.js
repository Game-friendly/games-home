const api = require('../../utils/api');
const app = getApp();

Page({
  data: { w: null, minPriceYuan: '0.00' },

  onLoad(q) {
    this.id = q.id;
    this.load();
  },

  load() {
    api.request('/api/workers/' + this.id).then(w => {
      this.setData({ w, minPriceYuan: ((w.minPrice || 0) / 100).toFixed(2) });
    }).catch(() => {});
  },

  goBack() {
    wx.navigateBack();
  },

  order() {
    const w = this.data.w;
    if (!w) return;
    wx.navigateTo({
      url: '/pages/place/place?workerId=' + w.id + '&workerName=' + encodeURIComponent(w.nickname)
    });
  }
});
