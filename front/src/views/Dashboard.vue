<template>
  <div class="dashboard">
    <a-layout>
      <a-layout-header class="header">
        <div class="header-content">
          <h1 class="logo">AI旅行助手</h1>
          <div class="user-info">
            <span>欢迎，{{ authStore.user.username }}</span>
            <a-button type="link" @click="handleLogout">
              退出登录
            </a-button>
          </div>
        </div>
      </a-layout-header>

      <a-layout-content class="content">
        <div class="welcome-section">
          <h2>开启您的个性化旅行体验</h2>
          <p>输入您的旅行偏好，AI将为您生成完美的旅行路线</p>
        </div>

        <div class="plan-form-section">
          <a-card title="创建旅行计划" class="plan-form-card">
            <TravelPlanForm @plan-generated="handlePlanGenerated" />
          </a-card>
        </div>

        <div v-if="travelStore.currentPlan" class="plan-result-section">
          <a-card title="您的旅行计划" class="result-card">
            <div class="result-actions">
              <a-button type="primary" @click="$router.push('/travel-plan')">
                查看详细行程
              </a-button>
              <a-button @click="travelStore.clearCurrentPlan">
                创建新计划
              </a-button>
            </div>
          </a-card>
        </div>

        <div v-if="travelStore.travelHistory.length > 0" class="history-section">
          <a-card title="历史旅行计划" class="history-card">
            <a-list
              :data-source="travelStore.travelHistory"
              :loading="loadingHistory"
            >
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta
                    :title="item.destination"
                    :description="`${item.days}天 · ${item.budget}元 · ${item.travelers}人`"
                  />
                  <template #actions>
                    <a @click="viewPlan(item)">查看</a>
                  </template>
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </div>
      </a-layout-content>
    </a-layout>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuthStore } from '../stores/auth'
import { useTravelStore } from '../stores/travel'
import TravelPlanForm from '../components/TravelPlanForm.vue'

const router = useRouter()
const authStore = useAuthStore()
const travelStore = useTravelStore()
const loadingHistory = ref(false)

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}

const handlePlanGenerated = (plan) => {
  message.success('旅行计划生成成功！')
}

const viewPlan = (plan) => {
  travelStore.currentPlan = plan
  router.push('/travel-plan')
}

onMounted(async () => {
  loadingHistory.value = true
  await travelStore.loadHistory()
  loadingHistory.value = false
})
</script>

<style scoped>
.dashboard {
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
  max-width: 1200px;
  margin: 0 auto;
}

.logo {
  color: #1890ff;
  margin: 0;
  font-size: 24px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.content {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-section {
  text-align: center;
  margin-bottom: 40px;
  padding: 40px 0;
}

.welcome-section h2 {
  font-size: 32px;
  color: #1890ff;
  margin-bottom: 16px;
}

.welcome-section p {
  font-size: 16px;
  color: #666;
}

.plan-form-section {
  margin-bottom: 24px;
}

.plan-form-card {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.plan-result-section {
  margin-bottom: 24px;
}

.result-card {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.result-actions {
  display: flex;
  gap: 12px;
}

.history-section {
  margin-bottom: 24px;
}

.history-card {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
</style>