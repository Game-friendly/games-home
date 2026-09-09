const config = require('./config');
const { DEFAULT_THEME } = require('./theme');

App({
  globalData: {
    baseUrl: config.baseUrl,
    token: '',
    user: null,
    serverOffset: 0,
    theme: DEFAULT_THEME,
    category: '全部'
  },
  onLaunch() {
    const token = wx.getStorageSync('token');
    if (token) this.globalData.token = token;
    this.loadTheme();
  },
  loadTheme() {
    wx.request({
      url: this.globalData.baseUrl + '/api/theme',
      method: 'GET',
      success: res => {
        const body = res.data;
        if (body && body.code === 0 && body.data) {
          this.globalData.theme = body.data;
        }
      }
    });
  }
});
