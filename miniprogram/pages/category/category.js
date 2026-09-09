const api = require('../../utils/api');
const { DEFAULT_THEME, categoryMeta } = require('../../theme');
const app = getApp();

Page({
  data: {
    list: [], filtered: [], search: '', category: '全部',
    categories: DEFAULT_THEME.categories
  },

  onShow() {
    if (app.globalData.category) {
      this.setData({ category: app.globalData.category });
      app.globalData.category = '全部';
    }
    this.load();
  },

  load() {
    api.request('/api/orders/pool').then(list => {
      const decorated = list.map(o => ({
        ...o,
        budgetYuan: (o.budget / 100).toFixed(2),
        badge: o.status === 'PUBLISHED' ? '待接单' : '锁定中',
        catIcon: categoryMeta(o.category).icon,
        catColor: categoryMeta(o.category).color
      }));
      this.setData({ list: decorated, filtered: this.filter(decorated, this.data.search, this.data.category) });
    });
  },

  filter(list, kw, category) {
    return list.filter(o => {
      if (category !== '全部' && o.category !== category) return false;
      if (!kw) return true;
      return [o.title, o.category, o.description].some(v => (v || '').toLowerCase().includes(kw.toLowerCase()));
    });
  },

  onSearch(e) {
    this.setData({ search: e.detail.value, filtered: this.filter(this.data.list, e.detail.value, this.data.category) });
  },

  chooseCategory(e) {
    const category = e.currentTarget.dataset.category;
    this.setData({ category, filtered: this.filter(this.data.list, this.data.search, category) });
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  }
});
