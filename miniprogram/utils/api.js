const app = getApp();

function request(path, method, data) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: app.globalData.baseUrl + path,
      method: method || 'GET',
      data: data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + app.globalData.token
      },
      success(res) {
        const body = res.data;
        if (res.statusCode === 401) {
          wx.navigateTo({ url: '/pages/login/login' });
          reject(body);
        } else if (body && body.code === 0) {
          resolve(body.data);
        } else {
          wx.showToast({ title: (body && body.msg) || '请求失败', icon: 'none' });
          reject(body);
        }
      },
      fail(err) {
        wx.showToast({ title: '网络错误，请确认后端已启动', icon: 'none' });
        reject(err);
      }
    });
  });
}

function upload(filePath) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: app.globalData.baseUrl + '/api/uploads',
      filePath,
      name: 'file',
      header: {
        'Authorization': 'Bearer ' + app.globalData.token
      },
      success(res) {
        let body;
        try {
          body = JSON.parse(res.data);
        } catch (e) {
          reject(new Error('上传响应解析失败'));
          return;
        }
        if (res.statusCode === 401) {
          wx.navigateTo({ url: '/pages/login/login' });
          reject(body);
        } else if (body && body.code === 0) {
          resolve(body.data.url);
        } else {
          wx.showToast({ title: (body && body.msg) || '上传失败', icon: 'none' });
          reject(body);
        }
      },
      fail(err) {
        wx.showToast({ title: '网络错误，请确认后端已启动', icon: 'none' });
        reject(err);
      }
    });
  });
}

module.exports = { request, upload };
