<template>
  <div class="travel-plan">
    <a-layout>
      <a-layout-header class="header">
        <div class="header-content">
          <div class="header-left">
            <a-button type="link" @click="$router.push('/dashboard')">
              <ArrowLeftOutlined />
              返回首页
            </a-button>
            <h1 class="page-title">旅行计划详情</h1>
          </div>
          <div class="header-right">
            <a-button type="primary" @click="handleSavePlan">
              <SaveOutlined />
              保存计划
            </a-button>
            <a-button @click="handleExport">
              <DownloadOutlined />
              导出
            </a-button>
          </div>
        </div>
      </a-layout-header>

      <a-layout-content class="content">
        <div v-if="!travelStore.currentPlan" class="no-plan">
          <a-result
            status="404"
            title="暂无旅行计划"
            sub-title="请先创建一个旅行计划"
          >
            <template #extra>
              <a-button type="primary" @click="$router.push('/dashboard')">
                创建旅行计划
              </a-button>
            </template>
          </a-result>
        </div>

        <div v-else class="plan-details">
          <div class="plan-overview">
            <a-card class="overview-card">
              <div class="overview-content">
                <div class="destination-info">
                  <h2>{{ travelStore.currentPlan.destination }}</h2>
                  <div class="plan-meta">
                    <a-tag color="blue">
                      <CalendarOutlined />
                      {{ parseInt(travelStore.currentPlan.duration?.replace(/[^0-9]/g, '') || '0') }}天
                    </a-tag>
                    <a-tag color="green">
                      <UserOutlined />
                      {{ travelStore.currentPlan.travelers }}人
                    </a-tag>
                    <a-tag color="orange">
                      <DollarOutlined />
                      {{ travelStore.currentPlan.total_budget }}
                    </a-tag>
                    <a-tag
                      v-for="pref in travelStore.currentPlan.preferences"
                      :key="pref"
                      color="purple"
                    >
                      {{ pref }}
                    </a-tag>
                  </div>
                </div>
                <div class="plan-summary">
                  <p>{{ travelStore.currentPlan.additional_requirements }}</p>
                </div>
              </div>
            </a-card>
          </div>

          <div class="plan-layout">
            <div class="itinerary-section">
              <a-card title="每日行程" class="itinerary-card">
                <a-tabs v-model:activeKey="activeDay" @change="handleTabChange">
                  <a-tab-pane
                    v-for="(dayData, dayKey) in travelStore.currentPlan.itinerary"
                    :key="dayKey"
                    :tab="`第${parseInt(dayKey.replace(/[^0-9]/g, '') || 1)}天`"
                    :name="parseInt(dayKey.replace(/[^0-9]/g, '') || 1)"
                  >
                    <ItineraryCard
                      :day="parseInt(dayKey.replace(/[^0-9]/g, '') || 1)"
                      :date="dayData.date"
                      :schedule="getDaySchedule(dayData)"
                      :accommodation="getAccommodation()"
                      :restaurants="getRestaurants()"
                      :transportation="getTransportation()"
                      :tips="getTips()"
                      :daily-budget="getDailyBudget()"
                    />
                  </a-tab-pane>
                </a-tabs>
              </a-card>
            </div>

            <div class="map-section">
              <a-card title="旅行地图" class="map-card">
                <TravelMap :plan="travelStore.currentPlan" />
              </a-card>
            </div>
          </div>

          <div class="additional-info">
            <a-row :gutter="[16, 16]">
              <a-col :span="12">
                <a-card title="预算明细" class="budget-card">
                  <div class="budget-details">
                    <div
                      v-for="(amount, category) in travelStore.currentPlan.budget_breakdown"
                      :key="category"
                      class="budget-item"
                    >
                      <div class="budget-category">{{ getBudgetCategoryLabel(category) }}</div>
                      <div class="budget-amount">{{ amount }}</div>
                      <div class="budget-percentage">{{ getBudgetPercentage(category) }}%</div>
                    </div>
                    <div class="budget-total">
                      <div class="total-label">总计</div>
                      <div class="total-amount">{{ travelStore.currentPlan.total_budget }}</div>
                    </div>
                  </div>
                </a-card>
              </a-col>

              <a-col :span="12">
                <a-card title="实用信息" class="info-card">
                  <div class="info-content">
                    <div class="info-item">
                      <h4><InfoCircleOutlined /> 住宿建议</h4>
                      <p>{{ travelStore.currentPlan.accommodation_suggestion?.recommendation || '请根据个人需求选择合适的住宿' }}</p>
                    </div>
                    <div class="info-item">
                      <h4><SafetyCertificateOutlined /> 交通提示</h4>
                      <p>{{ getTransportationTips() }}</p>
                    </div>
                    <div class="info-item">
                      <h4><ToolOutlined /> 美食推荐</h4>
                      <div class="essentials">
                        <a-tag
                          v-for="food in travelStore.currentPlan.food_recommendations || []"
                          :key="food"
                          color="blue"
                        >
                          {{ food }}
                        </a-tag>
                      </div>
                    </div>
                  </div>
                </a-card>
              </a-col>
            </a-row>
          </div>
        </div>
      </a-layout-content>
    </a-layout>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  SaveOutlined,
  DownloadOutlined,
  CalendarOutlined,
  UserOutlined,
  DollarOutlined,
  InfoCircleOutlined,
  SafetyCertificateOutlined,
  ToolOutlined
} from '@ant-design/icons-vue'
import { useTravelStore } from '../stores/travel'
import TravelMap from '../components/TravelMap.vue'
import ItineraryCard from '../components/ItineraryCard.vue'

const router = useRouter()
const travelStore = useTravelStore()
const activeDay = ref(1)

// 自动激活第一个tab
const autoActivateFirstTab = () => {
  if (travelStore.currentPlan?.itinerary) {
    const dayKeys = Object.keys(travelStore.currentPlan.itinerary)
    if (dayKeys.length > 0) {
      // 获取第一个天的key，如 "day_1"
      const firstDayKey = dayKeys[0]
      // 提取数字部分作为activeKey
      const dayNumber = parseInt(firstDayKey.replace(/[^0-9]/g, '') || 1)
      activeDay.value = dayNumber
    }
  }
}

// 数据转换辅助函数
const getDaySchedule = (dayData) => {
  const schedule = []
  const timeSlots = ['morning', 'lunch', 'afternoon', 'evening']

  timeSlots.forEach(slot => {
    if (dayData[slot] && dayData[slot].activity) {
      schedule.push({
        time: dayData[slot].time || '',
        activity: dayData[slot].activity || '',
        description: dayData[slot].description || '',
        location: dayData[slot].location || ''
      })
    }
  })

  return schedule
}

const getAccommodation = () => {
  const suggestion = travelStore.currentPlan?.accommodation_suggestion
  return {
    name: suggestion?.type || '',
    address: suggestion?.location || '',
    price: parseInt(suggestion?.estimated_cost?.replace(/[^0-9]/g, '') || '0')
  }
}

const getRestaurants = () => {
  const foods = travelStore.currentPlan?.food_recommendations || []
  const totalBudget = parseInt(travelStore.currentPlan?.total_budget?.replace(/[^0-9]/g, '') || '0')
  const duration = parseInt(travelStore.currentPlan?.duration?.replace(/[^0-9]/g, '') || '1')

  return foods.map(food => ({
    name: food,
    type: '当地美食',
    price: Math.round(totalBudget / (duration * 3))
  }))
}

const getTransportation = () => {
  const tips = travelStore.currentPlan?.transportation_tips || {}
  const transport = []

  if (tips.metro) {
    transport.push({
      from: '起点',
      to: '目的地',
      method: '地铁',
      duration: '30分钟',
      cost: 5
    })
  }
  if (tips.bus) {
    transport.push({
      from: '起点',
      to: '目的地',
      method: '公交',
      duration: '45分钟',
      cost: 2
    })
  }
  if (tips.walking) {
    transport.push({
      from: '起点',
      to: '目的地',
      method: '步行',
      duration: '15分钟',
      cost: 0
    })
  }

  return transport
}

const getTips = () => {
  return travelStore.currentPlan?.money_saving_tips || []
}

const getDailyBudget = () => {
  const totalBudget = parseInt(travelStore.currentPlan?.total_budget?.replace(/[^0-9]/g, '') || '0')
  const duration = parseInt(travelStore.currentPlan?.duration?.replace(/[^0-9]/g, '') || '1')
  return Math.round(totalBudget / duration)
}

const getBudgetCategoryLabel = (category) => {
  const labels = {
    accommodation: '住宿',
    food: '餐饮',
    transportation: '交通',
    attractions: '门票'
  }
  return labels[category] || category
}

const getBudgetPercentage = (category) => {
  const breakdown = travelStore.currentPlan?.budget_breakdown || {}
  const totalBudget = parseInt(travelStore.currentPlan?.total_budget?.replace(/[^0-9]/g, '') || '0')
  const categoryAmount = parseInt(breakdown[category]?.replace(/[^0-9]/g, '') || '0')

  if (totalBudget === 0) return 0
  return Math.round((categoryAmount / totalBudget) * 100)
}

const getTransportationTips = () => {
  const tips = travelStore.currentPlan?.transportation_tips || {}
  const tipList = []

  if (tips.metro) tipList.push('地铁：' + tips.metro)
  if (tips.bus) tipList.push('公交：' + tips.bus)
  if (tips.walking) tipList.push('步行：' + tips.walking)

  return tipList.join('；') || '请根据实际情况选择合适的交通方式'
}

const handleTabChange = (activeKey) => {
  activeDay.value = activeKey
}

const handleSavePlan = async () => {
  if (travelStore.currentPlan) {
    const success = await travelStore.savePlan(travelStore.currentPlan)
    if (success) {
      message.success('旅行计划保存成功')
    } else {
      message.error('保存失败，请重试')
    }
  }
}

const handleExport = () => {
  message.info('导出功能开发中...')
}

// 监听行程数据变化，自动激活第一个tab
watch(() => travelStore.currentPlan?.itinerary, () => {
  autoActivateFirstTab()
}, { immediate: true })

onMounted(() => {
  // 如果没有当前计划，使用模拟数据
  if (!travelStore.currentPlan) {
    travelStore.currentPlan = {
      destination: '北京',
      days: 3,
      travelers: 2,
      budget: 2000,
      preferences: ['休闲', '文化'],
      summary: '这是一次充满文化气息的北京之旅，带您领略古都的魅力。',
      itinerary: [
        {
          day: 1,
          date: '2024-01-15',
          schedule: [
            {
              time: '08:00-09:00',
              activity: '早餐',
              description: '酒店自助早餐',
              location: '酒店餐厅'
            },
            {
              time: '09:00-12:00',
              activity: '参观故宫',
              description: '游览紫禁城，感受明清皇家文化',
              location: '故宫博物院'
            }
          ],
          accommodation: {
            name: '北京王府井希尔顿酒店',
            address: '北京市东城区王府井大街',
            price: 600
          },
          restaurants: [
            {
              name: '全聚德烤鸭店',
              type: '北京菜',
              price: 150
            }
          ],
          transportation: [
            {
              from: '酒店',
              to: '故宫',
              method: '地铁',
              duration: '30分钟',
              cost: 5
            }
          ],
          tips: [
            '故宫需要提前预约门票',
            '建议穿着舒适的鞋子'
          ],
          dailyBudget: 800
        }
      ],
      budgetBreakdown: [
        { category: '住宿', amount: 1200, percentage: 60 },
        { category: '餐饮', amount: 400, percentage: 20 },
        { category: '交通', amount: 200, percentage: 10 },
        { category: '门票', amount: 200, percentage: 10 }
      ],
      weatherTips: '北京冬季较冷，请准备保暖衣物',
      safetyTips: '注意保管好个人财物，特别是在人流密集区域',
      essentials: ['身份证', '充电宝', '相机', '保暖衣物']
    }
  }
})
</script>

<style scoped>
.travel-plan {
  min-height: 100vh;
}

.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1400px;
  margin: 0 auto;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  color: #1890ff;
}

.header-right {
  display: flex;
  gap: 12px;
}

.content {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.no-plan {
  text-align: center;
  padding: 80px 0;
}

.plan-overview {
  margin-bottom: 24px;
}

.overview-card {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.overview-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}

.destination-info h2 {
  margin: 0 0 12px 0;
  color: #1890ff;
  font-size: 24px;
}

.plan-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.plan-summary {
  flex: 1;
  max-width: 400px;
}

.plan-summary p {
  margin: 0;
  color: #666;
  line-height: 1.6;
}

.plan-layout {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 24px;
  margin-bottom: 24px;
}

.itinerary-section {
  grid-column: 1;
}

.map-section {
  grid-column: 2;
}

.itinerary-card,
.map-card {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  height: 600px;
  overflow: auto;
}

.map-card {
  height: 600px;
}

.additional-info {
  margin-bottom: 24px;
}

.budget-card,
.info-card {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  height: 300px;
}

.budget-details {
  padding: 8px 0;
}

.budget-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.budget-item:last-child {
  border-bottom: none;
}

.budget-category {
  flex: 1;
  color: #333;
}

.budget-amount {
  margin-right: 16px;
  color: #ff4d4f;
  font-weight: 600;
}

.budget-percentage {
  color: #666;
  font-size: 12px;
}

.budget-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0 0 0;
  margin-top: 16px;
  border-top: 2px solid #1890ff;
}

.total-label {
  font-weight: 600;
  color: #333;
}

.total-amount {
  font-weight: 600;
  color: #ff4d4f;
  font-size: 18px;
}

.info-content {
  padding: 8px 0;
}

.info-item {
  margin-bottom: 20px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-item h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 8px 0;
  color: #333;
  font-size: 14px;
}

.info-item p {
  margin: 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.essentials {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 1200px) {
  .plan-layout {
    grid-template-columns: 1fr;
  }

  .map-section {
    grid-column: 1;
  }
}

@media (max-width: 768px) {
  .overview-content {
    flex-direction: column;
    gap: 16px;
  }

  .plan-summary {
    max-width: none;
  }

  .header-content {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .header-right {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>