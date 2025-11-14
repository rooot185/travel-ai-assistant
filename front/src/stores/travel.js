import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useTravelStore = defineStore('travel', () => {
  const currentPlan = ref(null)
  const travelHistory = ref([])
  const loading = ref(false)

  const generateTravelPlan = async (params) => {
    loading.value = true
    try {
      const response = await fetch('/api/travel/generate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify(params)
      })

      if (response.ok) {
        const data = await response.json()
        currentPlan.value = data.travel_plan
        travelHistory.value.unshift(data.travel_plan)
        return { success: true, data }
      } else {
        return { success: false, message: '生成旅行计划失败' }
      }
    } catch (error) {
      return { success: false, message: '网络错误' }
    } finally {
      loading.value = false
    }
  }

  const savePlan = async (plan) => {
    try {
      const response = await fetch('/api/travel/save', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify(plan)
      })

      return response.ok
    } catch (error) {
      return false
    }
  }

  const loadHistory = async () => {
    try {
      const response = await fetch('/api/travel/history', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      })

      if (response.ok) {
        const data = await response.json()
        travelHistory.value = data
      }
    } catch (error) {
      console.error('加载历史记录失败:', error)
    }
  }

  const loadPlanById = async (id) => {
    try {
      const response = await fetch(`/api/travel/${id}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      if (response.ok) {
        const data = await response.json();
        currentPlan.value = data; // Correctly assign the whole plan object
        return { success: true };
      } else {
        return { success: false, message: '加载计划详情失败' };
      }
    } catch (error) {
      console.error('加载计划详情失败:', error);
      return { success: false, message: '网络错误' };
    }
  };

  const clearCurrentPlan = () => {
    currentPlan.value = null
  }

  return {
    currentPlan,
    travelHistory,
    loading,
    generateTravelPlan,
    savePlan,
    loadHistory,
    clearCurrentPlan,
    loadPlanById
  }
})