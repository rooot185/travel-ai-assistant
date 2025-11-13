<template>
  <div class="itinerary-card">
    <div class="card-header">
      <h3 class="day-title">第{{ day }}天</h3>
      <div class="date">{{ formatDate(date) }}</div>
    </div>

    <div class="card-content">
      <div class="schedule-section">
        <h4 class="section-title">
          <ClockCircleOutlined />
          行程安排
        </h4>
        <div class="schedule-items">
          <div
            v-for="(item, index) in schedule"
            :key="index"
            class="schedule-item"
          >
            <div class="time">{{ item.time }}</div>
            <div class="activity">
              <div class="activity-title">{{ item.activity }}</div>
              <div v-if="item.description" class="activity-description">
                {{ item.description }}
              </div>
              <div v-if="item.location" class="activity-location">
                <EnvironmentOutlined />
                {{ item.location }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="details-section">
        <a-row :gutter="[16, 16]">
          <a-col :span="12">
            <div class="detail-item">
              <h5><HomeOutlined /> 住宿</h5>
              <div class="detail-content">
                <div class="hotel-name">{{ accommodation.name }}</div>
                <div class="hotel-address">{{ accommodation.address }}</div>
                <div class="hotel-price">¥{{ accommodation.price }}/晚</div>
              </div>
            </div>
          </a-col>

          <a-col :span="12">
            <div class="detail-item">
              <h5><ShopOutlined /> 餐厅推荐</h5>
              <div class="detail-content">
                <div
                  v-for="(restaurant, index) in restaurants"
                  :key="index"
                  class="restaurant-item"
                >
                  <div class="restaurant-name">{{ restaurant.name }}</div>
                  <div class="restaurant-type">{{ restaurant.type }}</div>
                  <div class="restaurant-price">人均 ¥{{ restaurant.price }}</div>
                </div>
              </div>
            </div>
          </a-col>

          <a-col :span="24">
            <div class="detail-item">
              <h5><CarOutlined /> 交通建议</h5>
              <div class="detail-content">
                <div
                  v-for="(transport, index) in transportation"
                  :key="index"
                  class="transport-item"
                >
                  <div class="transport-route">
                    {{ transport.from }} → {{ transport.to }}
                  </div>
                  <div class="transport-method">
                    {{ transport.method }} (约{{ transport.duration }})
                  </div>
                  <div class="transport-cost">费用: ¥{{ transport.cost }}</div>
                </div>
              </div>
            </div>
          </a-col>

          <a-col :span="24">
            <div class="detail-item">
              <h5><ExclamationCircleOutlined /> 温馨提示</h5>
              <div class="detail-content">
                <div class="tips">
                  <div
                    v-for="(tip, index) in tips"
                    :key="index"
                    class="tip-item"
                  >
                    {{ tip }}
                  </div>
                </div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <div class="budget-section">
        <div class="budget-item">
          <span class="budget-label">当日预算:</span>
          <span class="budget-amount">¥{{ dailyBudget }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'
import {
  ClockCircleOutlined,
  EnvironmentOutlined,
  HomeOutlined,
  ShopOutlined,
  CarOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons-vue'

const props = defineProps({
  day: {
    type: Number,
    required: true
  },
  date: {
    type: String,
    required: true
  },
  schedule: {
    type: Array,
    default: () => []
  },
  accommodation: {
    type: Object,
    default: () => ({
      name: '',
      address: '',
      price: 0
    })
  },
  restaurants: {
    type: Array,
    default: () => []
  },
  transportation: {
    type: Array,
    default: () => []
  },
  tips: {
    type: Array,
    default: () => []
  },
  dailyBudget: {
    type: Number,
    default: 0
  }
})

const formatDate = (date) => {
  return dayjs(date).format('YYYY年MM月DD日')
}

// 模拟数据 - 实际项目中这些数据会从API获取
const mockSchedule = computed(() => {
  return props.schedule.length > 0 ? props.schedule : [
    {
      time: '08:00-09:00',
      activity: '早餐',
      description: '酒店自助早餐',
      location: '酒店餐厅'
    },
    {
      time: '09:00-12:00',
      activity: '参观景点',
      description: '游览著名景点',
      location: '景点名称'
    },
    {
      time: '12:00-13:30',
      activity: '午餐',
      description: '当地特色餐厅',
      location: '餐厅名称'
    }
  ]
})
</script>

<style scoped>
.itinerary-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
  overflow: hidden;
}

.card-header {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: white;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.day-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.date {
  font-size: 14px;
  opacity: 0.9;
}

.card-content {
  padding: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: #1890ff;
  font-size: 16px;
  font-weight: 600;
}

.schedule-items {
  border-left: 3px solid #1890ff;
  margin-left: 8px;
}

.schedule-item {
  display: flex;
  margin-bottom: 20px;
  position: relative;
}

.schedule-item::before {
  content: '';
  position: absolute;
  left: -11px;
  top: 6px;
  width: 8px;
  height: 8px;
  background: #1890ff;
  border-radius: 50%;
}

.time {
  min-width: 80px;
  font-weight: 600;
  color: #1890ff;
  margin-right: 16px;
}

.activity {
  flex: 1;
}

.activity-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.activity-description {
  color: #666;
  font-size: 14px;
  margin-bottom: 4px;
}

.activity-location {
  color: #999;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.details-section {
  margin-top: 24px;
}

.detail-item {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  height: 100%;
}

.detail-item h5 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #333;
  font-size: 14px;
}

.hotel-name,
.restaurant-name {
  font-weight: 600;
  margin-bottom: 4px;
}

.hotel-address,
.restaurant-type {
  color: #666;
  font-size: 12px;
  margin-bottom: 4px;
}

.hotel-price,
.restaurant-price {
  color: #ff4d4f;
  font-weight: 600;
  font-size: 12px;
}

.restaurant-item,
.transport-item {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}

.restaurant-item:last-child,
.transport-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.transport-route {
  font-weight: 600;
  margin-bottom: 4px;
}

.transport-method {
  color: #666;
  font-size: 12px;
  margin-bottom: 4px;
}

.transport-cost {
  color: #ff4d4f;
  font-size: 12px;
}

.tips {
  font-size: 14px;
  color: #666;
}

.tip-item {
  margin-bottom: 8px;
  padding-left: 12px;
  position: relative;
}

.tip-item::before {
  content: '•';
  position: absolute;
  left: 0;
  color: #1890ff;
}

.budget-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e8e8e8;
  text-align: right;
}

.budget-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #f0f8ff;
  padding: 8px 16px;
  border-radius: 20px;
}

.budget-label {
  color: #666;
  font-size: 14px;
}

.budget-amount {
  color: #ff4d4f;
  font-weight: 600;
  font-size: 16px;
}
</style>