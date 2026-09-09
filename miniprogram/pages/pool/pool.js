const api = require('../../utils/api');
const { DEFAULT_THEME, categoryMeta } = require('../../theme');
const app = getApp();

Page({
  data: {
    list: [], filtered: [], search: '', loading: false,
    theme: DEFAULT_THEME, topWorkers: [],
    gradeFilter: '全部', peopleFilter: '全部', sort: 'new',
    gradeIndex: 0, peopleIndex: 0, sortIndex: 0
  },

  onLoad() {
    this.setData({ theme: app.globalData.theme || DEFAULT_THEME });
  },

  onShow() {
    this.load();
    clearInterval(this.timer);
    this.timer = setInterval(() => this.load(), 10000);
  },

  onHide() {
    clearInterval(this.timer);
  },

  onUnload() {
    clearInterval(this.timer);
  },

  load() {
    this.setData({ loading: true });
    return Promise.all([
      api.request('/api/orders/pool'),
      api.request('/api/workers/top').catch(() => [])
    ]).then(([list, workers]) => {
      const decorated = list.map(o => this.decorate(o));
      const top = workers.slice(0, 6).map(w => Object.assign({}, w, {
        minPriceYuan: ((w.minPrice || 0) / 100).toFixed(2)
      }));
      this.setData({ list: decorated, topWorkers: top, filtered: this.filterList(decorated, this.data.search) });
    }).catch(() => {}).then(() => this.setData({ loading: false }));
  },

  decorate(o) {
    // 大厅两类可操作单：PUBLISHED(待接单) / RESERVED(锁定中，高等级可抢)
    let badge = '';
    if (o.status === 'PUBLISHED') badge = '待接单';
    else if (o.status === 'RESERVED') badge = '锁定中';
    return Object.assign({}, o, {
      badge,
      budgetYuan: (o.budget / 100).toFixed(2),
      catIcon: categoryMeta(o.category).icon,
      catColor: categoryMeta(o.category).color,
      remainSlots: Math.max(0, (o.requiredWorkers || 1) - (o.filledWorkers || 0))
    });
  },

  filterList(list, kw) {
    let result = list;
    if (this.data.gradeFilter !== '全部') result = result.filter(o => o.grade === this.data.gradeFilter);
    if (this.data.peopleFilter === '1人') result = result.filter(o => o.requiredWorkers === 1);
    if (this.data.peopleFilter === '2人以上') result = result.filter(o => o.requiredWorkers >= 2);
    if (kw) result = result.filter(o => [o.title, o.category, o.description].some(v => (v || '').toLowerCase().includes(kw.toLowerCase())));
    if (this.data.sort === 'price') result = result.slice().sort((a, b) => a.budget - b.budget);
    else if (this.data.sort === 'priceDesc') result = result.slice().sort((a, b) => b.budget - a.budget);
    else result = result.slice().sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')));
    return result;
  },

  onSearch(e) {
    const kw = typeof e.detail === 'string' ? e.detail : (e.detail && e.detail.value);
    this.setData({ search: kw || '', filtered: this.filterList(this.data.list, kw || '') });
  },

  onGradeFilter(e) {
    const labels = ['全部', 'A', 'B', 'C'];
    const i = Number(e.detail.value);
    this.setData({ gradeIndex: i, gradeFilter: labels[i], filtered: this.filterList(this.data.list, this.data.search) });
  },

  onPeopleFilter(e) {
    const labels = ['全部', '1人', '2人以上'];
    const i = Number(e.detail.value);
    this.setData({ peopleIndex: i, peopleFilter: labels[i], filtered: this.filterList(this.data.list, this.data.search) });
  },

  onSortFilter(e) {
    const labels = ['new', 'price', 'priceDesc'];
    const i = Number(e.detail.value);
    this.setData({ sortIndex: i, sort: labels[i], filtered: this.filterList(this.data.list, this.data.search) });
  },

  contactService() {
    wx.showModal({ title: '联系客服', content: '如有订单付款等问题，请联系右下角客服。', showCancel: false });
  },

  goCategory(e) {
    app.globalData.category = e.currentTarget.dataset.category || '全部';
    wx.switchTab({ url: '/pages/category/category' });
  },

  goPlace() {
    wx.navigateTo({ url: '/pages/place/place' });
  },

  goWorker(e) {
    wx.navigateTo({ url: '/pages/worker/worker?id=' + e.currentTarget.dataset.id });
  },

  onPullDownRefresh() {
    this.load().then(() => wx.stopPullDownRefresh());
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  }
});
