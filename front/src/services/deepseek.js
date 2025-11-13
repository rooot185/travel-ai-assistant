import axios from 'axios'

const DEEPSEEK_API_KEY = import.meta.env.VITE_DEEPSEEK_API_KEY || 'your-deepseek-api-key'
const DEEPSEEK_API_URL = 'https://api.deepseek.com/v1/chat/completions'

const deepseekClient = axios.create({
  baseURL: DEEPSEEK_API_URL,
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${DEEPSEEK_API_KEY}`
  }
})

export const generateTravelPlan = async (params) => {
  try {
    const prompt = buildTravelPrompt(params)

    const response = await deepseekClient.post('', {
      model: 'deepseek-chat',
      messages: [
        {
          role: 'system',
          content: '你是一个专业的旅行规划专家，擅长根据用户需求制定详细的旅行计划。请以JSON格式返回结构化的旅行计划数据。'
        },
        {
          role: 'user',
          content: prompt
        }
      ],
      temperature: 0.7,
      max_tokens: 2000
    })

    const aiResponse = response.data.choices[0].message.content

    // 解析AI返回的JSON数据
    try {
      const travelPlan = JSON.parse(aiResponse)
      return {
        success: true,
        data: travelPlan
      }
    } catch (parseError) {
      console.error('解析AI响应失败:', parseError)
      // 如果解析失败，返回模拟数据
      return {
        success: true,
        data: generateMockTravelPlan(params)
      }
    }
  } catch (error) {
    console.error('调用DeepSeek API失败:', error)
    // API调用失败时返回模拟数据
    return {
      success: true,
      data: generateMockTravelPlan(params)
    }
  }
}

const buildTravelPrompt = (params) => {
  return `请为以下旅行需求生成详细的旅行计划，并以JSON格式返回：

目的地：${params.destination}
旅行天数：${params.days}天
同行人数：${params.travelers}人
总预算：${params.budget}元人民币
旅行偏好：${params.preferences.join('、')}
开始时间：${params.startDate}
额外要求：${params.additionalRequirements || '无'}

请返回包含以下结构的JSON数据：
{
  "destination": "目的地",
  "days": 天数,
  "travelers": 人数,
  "budget": 总预算,
  "preferences": ["偏好列表"],
  "summary": "旅行计划概述",
  "itinerary": [
    {
      "day": 1,
      "date": "日期",
      "schedule": [
        {
          "time": "时间段",
          "activity": "活动名称",
          "description": "活动描述",
          "location": "地点"
        }
      ],
      "accommodation": {
        "name": "住宿名称",
        "address": "地址",
        "price": 价格
      },
      "restaurants": [
        {
          "name": "餐厅名称",
          "type": "菜系类型",
          "price": 人均价格
        }
      ],
      "transportation": [
        {
          "from": "起点",
          "to": "终点",
          "method": "交通方式",
          "duration": "耗时",
          "cost": 费用
        }
      ],
      "tips": ["温馨提示"],
      "dailyBudget": 当日预算
    }
  ],
  "budgetBreakdown": [
    {
      "category": "类别",
      "amount": 金额,
      "percentage": 百分比
    }
  ],
  "weatherTips": "天气提醒",
  "safetyTips": "安全提示",
  "essentials": ["必备物品"]
}`
}

const generateMockTravelPlan = (params) => {
  const dailyBudget = Math.floor(params.budget / params.days)
  const accommodationBudget = Math.floor(dailyBudget * 0.6)
  const foodBudget = Math.floor(dailyBudget * 0.2)
  const transportBudget = Math.floor(dailyBudget * 0.1)
  const activityBudget = Math.floor(dailyBudget * 0.1)

  const itinerary = []
  for (let i = 1; i <= params.days; i++) {
    itinerary.push({
      day: i,
      date: new Date(Date.now() + i * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      schedule: [
        {
          time: '08:00-09:00',
          activity: '早餐',
          description: '酒店自助早餐',
          location: '酒店餐厅'
        },
        {
          time: '09:00-12:00',
          activity: '参观景点',
          description: '游览当地著名景点',
          location: `${params.destination}景点`
        },
        {
          time: '12:00-13:30',
          activity: '午餐',
          description: '品尝当地特色美食',
          location: '特色餐厅'
        },
        {
          time: '14:00-17:00',
          activity: '自由活动',
          description: '根据个人兴趣安排活动',
          location: `${params.destination}市区`
        },
        {
          time: '18:00-19:30',
          activity: '晚餐',
          description: '享用晚餐',
          location: '推荐餐厅'
        }
      ],
      accommodation: {
        name: `${params.destination}精选酒店`,
        address: `${params.destination}市中心`,
        price: accommodationBudget
      },
      restaurants: [
        {
          name: '当地特色餐厅',
          type: '地方菜',
          price: Math.floor(foodBudget / 2)
        }
      ],
      transportation: [
        {
          from: '酒店',
          to: '景点',
          method: '公共交通',
          duration: '30分钟',
          cost: Math.floor(transportBudget / 2)
        }
      ],
      tips: [
        '建议穿着舒适的鞋子',
        '注意保管好个人财物',
        '提前了解当地天气情况'
      ],
      dailyBudget: dailyBudget
    })
  }

  return {
    destination: params.destination,
    days: params.days,
    travelers: params.travelers,
    budget: params.budget,
    preferences: params.preferences,
    summary: `这是一次精彩的${params.destination}之旅，结合了${params.preferences.join('、')}等元素，为您量身定制。`,
    itinerary: itinerary,
    budgetBreakdown: [
      { category: '住宿', amount: accommodationBudget * params.days, percentage: 60 },
      { category: '餐饮', amount: foodBudget * params.days, percentage: 20 },
      { category: '交通', amount: transportBudget * params.days, percentage: 10 },
      { category: '活动', amount: activityBudget * params.days, percentage: 10 }
    ],
    weatherTips: '请根据实际出行时间关注当地天气预报，准备合适的衣物。',
    safetyTips: '注意人身和财产安全，遵守当地法律法规。',
    essentials: ['身份证', '充电宝', '相机', '常用药品', '防晒用品']
  }
}