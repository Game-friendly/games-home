const api = require('../../utils/api');
const app = getApp();

function serverNow() {
  const offset = app.globalData.serverOffset || 0;
  return Date.now() + offset;
}

Page({
  data: {
    order: null,
    myGrade: null,
    isBuyer: false,
    canAccept: false,
    canWithdraw: false,
    canComplete: false,
    canConfirm: false,
    canPay: false,
    canDispute: false,
    disputeOpen: false,
    favorited: false,
    workers: [],
    reviews: [],
    reviewedToIds: [],
    reviewTarget: null,
    rating: 5,
    displayImages: [],
    messages: [],
    messageInput: '',
    myId: null
  },

  onLoad(q) {
    this.id = q.id;
  },

  onShow() {
    this.load();
    this.reloadTimer = setInterval(() => this.load(), 8000);
  },

  onUnload() {
    clearInterval(this.reloadTimer);
    clearInterval(this.tickTimer);
  },

  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) wx.navigateBack();
    else wx.switchTab({ url: '/pages/pool/pool' });
  },

  async load() {
    try {
      const o = await api.request('/api/orders/' + this.id);
      const me = app.globalData.user || {};
      const myId = me.userId || me.id;
      const isBuyer = myId === o.buyerId;
      const isWorker = me.role === 'WORKER';
      const mySlot = (o.workers || []).find(w => w.workerId === myId);
      const workers = (o.workers || []).map(w => this.decorateSlot(w, myId, isBuyer, isWorker));
      const messages = await api.request('/api/orders/' + this.id + '/messages').catch(() => []);

      this.setData({
        order: o,
        displayImages: (o.images || []).map(u => app.globalData.baseUrl + u),
        budgetYuan: (o.budget / 100).toFixed(2),
        statusText: this.statusText(o.status),
        gradeTagType: o.grade === 'A' ? 'danger' : (o.grade === 'B' ? 'warning' : 'primary'),
        myGrade: me.grade,
        isBuyer,
        canAccept: o.status === 'PUBLISHED' && isWorker && !isBuyer && !mySlot && o.filledWorkers < o.requiredWorkers,
        canWithdraw: !!mySlot && (mySlot.status === 'RESERVED' || mySlot.status === 'ACCEPTED') && !mySlot.submittedAt,
        canComplete: !!mySlot && mySlot.status === 'ACCEPTED' && !mySlot.submittedAt,
        canConfirm: isBuyer && o.status === 'DELIVERED',
        canPay: isBuyer && o.status === 'PENDING_PAYMENT',
        canDispute: (isBuyer || !!mySlot) && ['ACCEPTED', 'DELIVERED', 'COMPLETED'].indexOf(o.status) >= 0,
        favorited: this.isFavorite(o.id),
        messages,
        myId,
        workers
      });
      this.startTick();

      if (isBuyer || mySlot) {
        api.request('/api/orders/' + this.id + '/dispute').then(d => {
          this.setData({ disputeOpen: d.open });
        }).catch(() => {});
      }

      if (isBuyer && o.status === 'COMPLETED') {
        const reviews = await api.request('/api/orders/' + this.id + '/review');
        const workersWithReview = this.data.workers.map(w => Object.assign({}, w, {
          reviewed: reviews.some(r => r.toId === w.workerId)
        }));
        this.setData({
          reviews,
          reviewedToIds: reviews.map(r => r.toId),
          workers: workersWithReview
        });
      }
    } catch (e) {
      // 错误提示已由 api.request 统一处理
    }
  },

  decorateSlot(w, myId, isBuyer, isWorker) {
    return Object.assign({}, w, {
      canSteal: w.status === 'RESERVED' && isWorker && !isBuyer && w.workerId !== myId,
      isMine: w.workerId === myId
    });
  },

  statusText(s) {
    return ({ PENDING_PAYMENT:'待支付', PUBLISHED:'待接单', RESERVED:'锁定中', ACCEPTED:'进行中', DELIVERED:'待确认', COMPLETED:'已完成', CANCELLED:'已取消', REFUNDED:'已退款', DISPUTED:'争议中' })[s] || s;
  },

  startTick() {
    clearInterval(this.tickTimer);
    this.tickTimer = setInterval(() => {
      if (!this.data.order) return;
      const workers = this.data.workers.map(w => Object.assign({}, w, { remain: this.remain(w) }));
      this.setData({ workers });
    }, 1000);
  },

  remain(w) {
    if (w.status !== 'RESERVED' || !w.reserveExpiresAt) return 0;
    const expire = new Date(String(w.reserveExpiresAt).replace(' ', 'T')).getTime();
    return Math.max(0, Math.floor((expire - serverNow()) / 1000));
  },

  accept() { this.act('/accept', '接单成功'); },
  withdraw() { this.act('/withdraw', '已退单'); },
  complete() { this.act('/complete', '已提交完成'); },
  confirm() { this.act('/confirm', '已确认完成'); },

  pay() {
    api.request('/api/payments/orders/' + this.id + '/pay', 'POST').then(() => {
      wx.showToast({ title: '支付成功', icon: 'success' });
      this.load();
    });
  },

  isFavorite(id) {
    return (wx.getStorageSync('favorites') || []).some(o => o.id === id);
  },

  toggleFavorite() {
    const order = this.data.order;
    if (!order) return;
    const list = wx.getStorageSync('favorites') || [];
    const next = this.isFavorite(order.id)
      ? list.filter(o => o.id !== order.id)
      : [order, ...list];
    wx.setStorageSync('favorites', next);
    this.setData({ favorited: this.isFavorite(order.id) });
  },

  dispute() {
    wx.showModal({
      title: '发起争议',
      editable: true,
      placeholderText: '请填写争议原因',
      success: res => {
        if (res.confirm) {
          api.request('/api/orders/' + this.id + '/dispute', 'POST', {
            reason: res.content || '未填写原因'
          }).then(() => {
            wx.showToast({ title: '已提交争议', icon: 'success' });
            this.load();
          });
        }
      }
    });
  },

  steal(e) {
    const targetWorkerId = e.currentTarget.dataset.workerId;
    api.request('/api/orders/' + this.id + '/steal', 'POST', { targetWorkerId }).then(() => {
      wx.showToast({ title: '抢单成功', icon: 'success' });
      this.load();
    });
  },

  chooseReview(e) {
    this.setData({ reviewTarget: e.currentTarget.dataset.workerId, rating: 5 });
  },

  onStar(e) {
    this.setData({ rating: Number(e.currentTarget.dataset.r) });
  },

  onRateChange(e) {
    this.setData({ rating: Number(e.detail) });
  },

  onMessageInput(e) {
    this.setData({ messageInput: e.detail.value });
  },

  sendMessage() {
    const content = (this.data.messageInput || '').trim();
    if (!content) return wx.showToast({ title: '请输入留言', icon: 'none' });
    api.request('/api/orders/' + this.id + '/messages', 'POST', { content }).then(msg => {
      this.setData({ messages: this.data.messages.concat(msg), messageInput: '' });
    });
  },

  review() {
    if (!this.data.reviewTarget) return wx.showToast({ title: '请选择要评价的接单人', icon: 'none' });
    wx.showModal({
      title: '评价接单人（' + this.data.rating + ' 星）',
      editable: true,
      placeholderText: '输入评价内容（可选）',
      success: res => {
        if (res.confirm) {
          api.request('/api/orders/' + this.id + '/review', 'POST', {
            toId: this.data.reviewTarget,
            rating: this.data.rating,
            comment: res.content || ''
          }).then(() => {
            wx.showToast({ title: '感谢评价', icon: 'success' });
            this.setData({ reviewTarget: null });
            this.load();
          });
        }
      }
    });
  },

  act(path, okMsg) {
    api.request('/api/orders/' + this.id + path, 'POST').then(() => {
      wx.showToast({ title: okMsg, icon: 'success' });
      this.load();
    });
  }
});
