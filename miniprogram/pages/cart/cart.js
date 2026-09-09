Page({
  data: { list: [] },

  onShow() {
    this.setData({ list: wx.getStorageSync('favorites') || [] });
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  remove(e) {
    const id = Number(e.currentTarget.dataset.id);
    const list = (wx.getStorageSync('favorites') || []).filter(o => o.id !== id);
    wx.setStorageSync('favorites', list);
    this.setData({ list });
  }
});
