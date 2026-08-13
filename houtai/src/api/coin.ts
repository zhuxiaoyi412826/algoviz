import request from './request'

/** 分页查询硬币购买记录（支持关键词搜索） */
export function getCoinPurchases(params?: { keyword?: string; page?: number; pageSize?: number }) {
  return request.get('/coin/admin/purchases', { params })
}

/** 获取硬币系统统计 */
export function getCoinStats() {
  return request.get('/coin/admin/stats')
}

/** 获取所有硬币商品 */
export function getCoinProducts() {
  return request.get('/coin/admin/products')
}

/** 新增硬币商品 */
export function createCoinProduct(data: any) {
  return request.post('/coin/admin/products', data)
}

/** 编辑硬币商品 */
export function updateCoinProduct(productId: string, data: any) {
  return request.put(`/coin/admin/products/${productId}`, data)
}

/** 删除硬币商品 */
export function deleteCoinProduct(productId: string) {
  return request.delete(`/coin/admin/products/${productId}`)
}
