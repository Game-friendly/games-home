const api = require('../../utils/api');
const { DEFAULT_THEME } = require('../../theme');
const app = getApp();

Page({
  data: {
    grades: ['A', 'B', 'C'],
    gradeIndex: 2,
    people: [1, 2, 3, 4, 5],
    peopleIndex: 0,
    images: [],
    displayImages: [],
    uploading: false,
    categories: DEFAULT_THEME.categories,
    assignedWorkerId: null,
    assignedWorkerName: '',
    form: { title: '', category: DEFAULT_THEME.categories[0].name, grade: 'C', requiredWorkers: 1, budget: '', contact: '', description: '' }
  },

  onLoad(q) {
    if (q.workerId) {
      this.setData({
        assignedWorkerId: Number(q.workerId),
        assignedWorkerName: decodeURIComponent(q.workerName || '')
      });
    }
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    const value = typeof e.detail === 'string' ? e.detail : e.detail.value;
    this.setData({ ['form.' + field]: value });
  },

  chooseCategory(e) {
    this.setData({ 'form.category': e.currentTarget.dataset.category });
  },

  clearWorker() {
    this.setData({ assignedWorkerId: null, assignedWorkerName: '' });
  },

  onGrade(e) {
    const i = e.detail.value;
    this.setData({ gradeIndex: i, 'form.grade': this.data.grades[i] });
  },

  onPeople(e) {
    const i = e.detail.value;
    this.setData({ peopleIndex: i, 'form.requiredWorkers': this.data.people[i] });
  },

  chooseImages() {
    const remain = 6 - this.data.images.length;
    if (remain <= 0) return wx.showToast({ title: '最多上传 6 张', icon: 'none' });
    wx.chooseImage({
      count: remain,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: res => this.uploadImages(res.tempFilePaths)
    });
  },

  async uploadImages(paths) {
    this.setData({ uploading: true });
    try {
      for (const p of paths) {
        const url = await api.upload(p);
        const images = this.data.images.concat(url);
        this.setData({ images, displayImages: images.map(u => app.globalData.baseUrl + u) });
      }
    } catch (e) {
      // 错误提示已由 api.upload 统一处理
    } finally {
      this.setData({ uploading: false });
    }
  },

  removeImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = this.data.images.slice();
    images.splice(index, 1);
    this.setData({ images, displayImages: images.map(u => app.globalData.baseUrl + u) });
  },

  submit() {
    const f = this.data.form;
    if (!f.title) return wx.showToast({ title: '请填标题', icon: 'none' });
    const payload = {
      title: f.title,
      category: f.category,
      grade: f.grade,
      description: f.description,
      budget: Math.round(parseFloat(f.budget || '0') * 100),
      requiredWorkers: f.requiredWorkers,
      contact: f.contact,
      images: this.data.images,
      assignedWorkerId: this.data.assignedWorkerId
    };
    api.request('/api/orders', 'POST', payload).then(d => {
      wx.showToast({ title: '下单成功', icon: 'success' });
      setTimeout(() => wx.redirectTo({ url: '/pages/detail/detail?id=' + d.id }), 500);
    });
  }
});
