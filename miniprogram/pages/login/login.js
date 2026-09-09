const api = require('../../utils/api');
const { DEFAULT_THEME } = require('../../theme');
const app = getApp();

Page({
  data: {
    theme: DEFAULT_THEME,
    accounts: [
      { code: 'demo_client_1', label: '客户', desc: '发布陪玩需求' },
      { code: 'demo_worker_a', label: '大神 · A 级', desc: '可接 A/B/C 单' },
      { code: 'demo_worker_b', label: '高手 · B 级', desc: '可接 B/C 单' },
      { code: 'demo_worker_c', label: '陪玩 · C 级', desc: '入门' },
      { code: 'demo_admin', label: '管理员', desc: '派单/改价/改等级' }
    ]
  },

  onLoad() {
    this.setData({ theme: app.globalData.theme || DEFAULT_THEME });
  },

  onLogin(e) {
    const code = e.currentTarget.dataset.code;
    api.request('/api/auth/login', 'POST', { code }).then(d => {
      app.globalData.token = d.token;
      app.globalData.user = d;
      app.globalData.serverOffset = (d.serverTime || Date.now()) - Date.now();
      wx.setStorageSync('token', d.token);
      wx.showToast({ title: '登录成功', icon: 'success' });
      setTimeout(() => wx.switchTab({ url: '/pages/pool/pool' }), 400);
    });
  }
});
