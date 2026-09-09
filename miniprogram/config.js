// 小程序无法读取系统环境变量，这里按运行环境切换后端地址。
// 开发版默认连本机后端；体验版和正式版改成已备案的 HTTPS 域名。
function envVersion() {
  try {
    return wx.getAccountInfoSync().miniProgram.envVersion || 'develop';
  } catch (e) {
    return 'develop';
  }
}

const endpoints = {
  develop: 'http://localhost:8080',
  trial: 'http://localhost:8080', // TODO: 体验版 HTTPS 域名
  release: 'https://api.example.com' // TODO: 正式 HTTPS 域名
};

module.exports = {
  baseUrl: endpoints[envVersion()] || endpoints.develop
};
