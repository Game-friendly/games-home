// 小程序端主题配置：和网页预览、后端保持一致。换行业时改这里即可。
const DEFAULT_THEME = {
  appName: '游伴',
  slogan: '大神陪你开黑上分',
  primaryColor: '#f7b500',
  categories: [
    { name: '王者荣耀陪玩', icon: '⚔', color: '#6d5df6' },
    { name: '英雄联盟陪玩', icon: '🏆', color: '#4f8cff' },
    { name: '和平精英陪玩', icon: '🎯', color: '#16a34a' },
    { name: '其他游戏', icon: '🎮', color: '#f7b500' },
    { name: '攻略教学', icon: '📘', color: '#0ea5a5' },
    { name: '组队开黑', icon: '👥', color: '#ec4899' }
  ]
};

function categoryMeta(name) {
  return DEFAULT_THEME.categories.find(c => c.name === name) ||
    { name: name || '其他', icon: '🎮', color: '#24283b' };
}

module.exports = { DEFAULT_THEME, categoryMeta };
