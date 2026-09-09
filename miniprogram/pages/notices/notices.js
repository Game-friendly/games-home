const api = require('../../utils/api');

Page({
  data: { list: [] },

  onShow() {
    this.load();
  },

  load() {
    api.request('/api/notices').then(list => {
      this.setData({ list: list.map(n => ({ ...n, timeText: (n.createdAt || '').replace('T', ' ') })) });
    });
  },

  onTap(e) {
    const { id, read } = e.currentTarget.dataset;
    if (!read) {
      api.request('/api/notices/' + id + '/read', 'POST').then(() => this.load());
    }
  },

  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) wx.navigateBack();
    else wx.switchTab({ url: '/pages/pool/pool' });
  }
});
