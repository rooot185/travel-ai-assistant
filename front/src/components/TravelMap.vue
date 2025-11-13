<template>
  <div class="travel-map">
    <div ref="mapContainer" class="map-container">
      <div v-if="!isMapLoaded" class="map-loading">
        <a-spin size="large" />
        <p>地图加载中...</p>
      </div>
    </div>
    <div v-if="markers.length > 0" class="map-legend">
      <div class="legend-item">
        <div class="legend-icon hotel"></div>
        <span>住宿</span>
      </div>
      <div class="legend-item">
        <div class="legend-icon attraction"></div>
        <span>景点</span>
      </div>
      <div class="legend-item">
        <div class="legend-icon restaurant"></div>
        <span>餐厅</span>
      </div>
      <div class="legend-item">
        <div class="legend-icon transport"></div>
        <span>交通</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'

const props = defineProps({
  plan: {
    type: Object,
    default: () => ({})
  }
})

const mapContainer = ref(null)
const map = ref(null)
const isMapLoaded = ref(false)
const markers = ref([])

onMounted(() => {
  initializeMap()
})

onUnmounted(() => {
  // 清理地图实例
  if (map.value) {
    map.value.destroy()
  }
})

watch(() => props.plan, async (newPlan) => {
  if (newPlan && Object.keys(newPlan).length > 0) {
    await updateMapWithPlan(newPlan)
  }
}, { deep: true })

const initializeMap = () => {
  // 动态加载高德地图API
  if (!window.AMap) {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${import.meta.env.VITE_AMAP_API_KEY || '8a5e31d8e8c356f706fb6b3bc250fc06'}`
    script.onload = () => {
      createMapInstance()
    }
    document.head.appendChild(script)
  } else {
    createMapInstance()
  }
}

const createMapInstance = () => {
  if (!mapContainer.value) return

  try {
    // 创建地图实例
    map.value = new AMap.Map(mapContainer.value, {
      zoom: 12,
      center: [116.397428, 39.90923], // 默认北京中心
      viewMode: '3D',
      mapStyle: 'amap://styles/normal'
    })

    isMapLoaded.value = true

    // 添加地图控件
    map.value.addControl(new AMap.Scale())
    map.value.addControl(new AMap.ToolBar())

  } catch (error) {
    console.error('地图初始化失败:', error)
  }
}

const updateMapWithPlan = async (plan) => {
  if (!map.value || !plan.travel_plan) return

  // 清除现有标记
  clearMarkers()

  const locations = []

  // 提取所有地点信息
  if (plan.travel_plan.itinerary) {
    for (const dayKey in plan.travel_plan.itinerary) {
      const day = plan.travel_plan.itinerary[dayKey]
      const timeSlots = ['morning', 'lunch', 'afternoon', 'evening']

      for (const slot of timeSlots) {
        if (day[slot] && day[slot].locationDetails) {
          const location = day[slot].locationDetails
          if (location.coordinates) {
            locations.push({
              ...location,
              type: getLocationType(day[slot].activity),
              timeSlot: slot,
              day: dayKey
            })
          }
        }
      }
    }
  }

  // 添加住宿建议
  if (plan.travel_plan.accommodation_suggestion &&
      plan.travel_plan.accommodation_suggestion.location) {
    const accLocation = await getLocationCoordinates(plan.travel_plan.accommodation_suggestion.location)
    if (accLocation) {
      locations.push({
        ...accLocation,
        name: plan.travel_plan.accommodation_suggestion.type || '住宿',
        type: 'hotel',
        description: plan.travel_plan.accommodation_suggestion.recommendation
      })
    }
  }

  // 在地图上添加标记
  addMarkersToMap(locations)

  // 调整地图视图以包含所有标记
  if (locations.length > 0) {
    adjustMapView(locations)
  }
}

const getLocationType = (activity) => {
  if (activity.includes('酒店') || activity.includes('住宿')) return 'hotel'
  if (activity.includes('餐厅') || activity.includes('美食') || activity.includes('午餐') || activity.includes('晚餐')) return 'restaurant'
  if (activity.includes('地铁') || activity.includes('公交') || activity.includes('交通')) return 'transport'
  return 'attraction'
}

const getLocationCoordinates = async (locationName) => {
  try {
    // 这里可以调用后端的坐标获取接口
    const response = await fetch(`/api/map/geocode?address=${encodeURIComponent(locationName)}`)
    if (response.ok) {
      const data = await response.json()
      return data
    }
  } catch (error) {
    console.error('获取坐标失败:', error)
  }
  return null
}

const addMarkersToMap = (locations) => {
  if (!map.value) return

  locations.forEach(location => {
    const [lng, lat] = location.coordinates.split(',').map(coord => parseFloat(coord.trim()))

    const marker = new AMap.Marker({
      position: [lng, lat],
      title: location.name,
      content: createMarkerContent(location),
      offset: new AMap.Pixel(-13, -30)
    })

    // 添加信息窗口
    const infoWindow = new AMap.InfoWindow({
      content: createInfoWindowContent(location),
      offset: new AMap.Pixel(0, -30)
    })

    marker.on('click', () => {
      infoWindow.open(map.value, marker.getPosition())
    })

    map.value.add(marker)
    markers.value.push(marker)
  })
}

const createMarkerContent = (location) => {
  const iconClass = {
    hotel: 'hotel',
    attraction: 'attraction',
    restaurant: 'restaurant',
    transport: 'transport'
  }[location.type] || 'attraction'

  return `<div class="custom-marker ${iconClass}">
    <div class="marker-icon"></div>
  </div>`
}

const createInfoWindowContent = (location) => {
  return `<div class="map-info-window">
    <h4>${location.name}</h4>
    <p>${location.address || '地址信息'}</p>
    ${location.rating && location.rating !== 'N/A' ? `<p>评分: ${location.rating}</p>` : ''}
    ${location.description ? `<p>${location.description}</p>` : ''}
  </div>`
}

const adjustMapView = (locations) => {
  if (!map.value || locations.length === 0) return

  const bounds = new AMap.Bounds()

  locations.forEach(location => {
    const [lng, lat] = location.coordinates.split(',').map(coord => parseFloat(coord.trim()))
    bounds.extend([lng, lat])
  })

  map.value.setBounds(bounds, true, [20, 20, 20, 20])
}

const clearMarkers = () => {
  if (!map.value) return

  markers.value.forEach(marker => {
    map.value.remove(marker)
  })
  markers.value = []
}
</script>
<style scoped>
.travel-map {
  position: relative;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
}

.map-container {
  width: 100%;
  height: 100%;
  background: #f5f5f5;
  position: relative;
}

.map-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #666;
}

.map-legend {
  position: absolute;
  bottom: 20px;
  left: 20px;
  background: rgba(255, 255, 255, 0.95);
  padding: 12px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  gap: 16px;
  z-index: 1000;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #333;
}

.legend-icon {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.legend-icon.hotel {
  background-color: #ff6b6b;
}

.legend-icon.attraction {
  background-color: #4ecdc4;
}

.legend-icon.restaurant {
  background-color: #45b7d1;
}

.legend-icon.transport {
  background-color: #96ceb4;
}

/* 自定义标记样式 */
.custom-marker {
  position: relative;
  width: 26px;
  height: 34px;
}

.marker-icon {
  width: 26px;
  height: 34px;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.custom-marker.hotel .marker-icon {
  background-image: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%23ff6b6b"><path d="M7 13c1.66 0 3-1.34 3-3S8.66 7 7 7s-3 1.34-3 3 1.34 3 3 3zm12-6h-8v7H3V5H1v15h2v-3h18v3h2v-9c0-2.21-1.79-4-4-4z"/></svg>');
}

.custom-marker.attraction .marker-icon {
  background-image: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%234ecdc4"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>');
}

.custom-marker.restaurant .marker-icon {
  background-image: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2345b7d1"><path d="M8.1 13.34l2.83-2.83L3.91 3.5c-1.56 1.56-1.56 4.09 0 5.66l4.19 4.18zm6.78-1.81c1.53.71 3.68.21 5.27-1.38 1.91-1.91 2.28-4.65.81-6.12-1.46-1.46-4.2-1.1-6.12.81-1.59 1.59-2.09 3.74-1.38 5.27L3.7 19.87l1.41 1.41L12 14.41l6.88 6.88 1.41-1.41L13.41 13l1.47-1.47z"/></svg>');
}

.custom-marker.transport .marker-icon {
  background-image: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2396ceb4"><path d="M12 2c-4.42 0-8 3.58-8 8s3.58 8 8 8 8-3.58 8-8-3.58-8-8-8zm0 14.5c-3.31 0-6-2.69-6-6s2.69-6 6-6 6 2.69 6 6-2.69 6-6 6z"/></svg>');
}

/* 信息窗口样式 */
.map-info-window {
  padding: 8px;
  max-width: 200px;
}

.map-info-window h4 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 14px;
}

.map-info-window p {
  margin: 4px 0;
  color: #666;
  font-size: 12px;
  line-height: 1.4;
}
</style>