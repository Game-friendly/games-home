import axios from 'axios';
import { ElMessage } from 'element-plus';

const api = axios.create({ baseURL: '/api', timeout: 10000 });

api.interceptors.request.use(config => {
  const token = localStorage.getItem('admin_token');
  if (token) config.headers.Authorization = 'Bearer ' + token;
  return config;
});

api.interceptors.response.use(
  res => {
    const body = res.data;
    if (body && body.code === 0) return body.data;
    ElMessage.error((body && body.msg) || '请求失败');
    return Promise.reject(body);
  },
  err => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('admin_token');
      location.reload();
    }
    ElMessage.error(err.response?.data?.msg || '网络错误，请确认后端已启动');
    return Promise.reject(err);
  }
);

export default api;
