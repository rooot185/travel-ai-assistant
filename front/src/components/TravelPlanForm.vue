<template>
  <a-form
    :model="formState"
    name="travel-plan"
    @finish="onFinish"
    @finishFailed="onFinishFailed"
    layout="vertical"
    class="travel-plan-form"
  >
    <div class="form-row">
      <a-form-item
        label="目的地"
        name="destination"
        :rules="[{ required: true, message: '请输入目的地!' }]"
        class="form-item"
      >
        <a-input
          v-model:value="formState.destination"
          placeholder="例如：北京、上海、杭州"
          size="large"
        >
          <template #prefix>
            <EnvironmentOutlined />
          </template>
        </a-input>
      </a-form-item>

      <a-form-item
        label="旅行开始时间"
        name="startDate"
        :rules="[{ required: true, message: '请选择开始时间!' }]"
        class="form-item"
      >
        <a-date-picker
          v-model:value="formState.startDate"
          placeholder="选择日期"
          size="large"
          style="width: 100%"
        />
      </a-form-item>
    </div>

    <div class="form-row">
      <a-form-item
        label="旅行天数"
        name="days"
        :rules="[
          { required: true, message: '请输入天数!' },
          { type: 'number', min: 1, max: 30, message: '天数范围1-30天' }
        ]"
        class="form-item"
      >
        <a-input-number
          v-model:value="formState.days"
          placeholder="请输入天数"
          :min="1"
          :max="30"
          size="large"
          style="width: 100%"
        >
          <template #addonAfter>天</template>
        </a-input-number>
      </a-form-item>

      <a-form-item
        label="同行人数"
        name="travelers"
        :rules="[
          { required: true, message: '请输入人数!' },
          { type: 'number', min: 1, max: 10, message: '人数范围1-10人' }
        ]"
        class="form-item"
      >
        <a-input-number
          v-model:value="formState.travelers"
          placeholder="请输入人数"
          :min="1"
          :max="10"
          size="large"
          style="width: 100%"
        >
          <template #addonAfter>人</template>
        </a-input-number>
      </a-form-item>

      <a-form-item
        label="总预算 (人民币)"
        name="budget"
        :rules="[
          { required: true, message: '请输入预算!' },
          { type: 'number', min: 100, message: '预算至少100元' }
        ]"
        class="form-item"
      >
        <a-input-number
          v-model:value="formState.budget"
          placeholder="请输入预算"
          :min="100"
          :step="100"
          size="large"
          style="width: 100%"
        >
          <template #addonAfter>元</template>
        </a-input-number>
      </a-form-item>
    </div>

    <a-form-item
      label="旅行偏好"
      name="preferences"
      :rules="[{ required: true, message: '请选择至少一个偏好!' }]"
    >
      <a-checkbox-group v-model:value="formState.preferences">
        <a-row :gutter="[16, 16]">
          <a-col :span="6">
            <a-checkbox value="休闲">
              <div class="preference-item">
                <CoffeeOutlined class="preference-icon" />
                <span>休闲</span>
              </div>
            </a-checkbox>
          </a-col>
          <a-col :span="6">
            <a-checkbox value="冒险">
              <div class="preference-item">
                <RocketOutlined class="preference-icon" />
                <span>冒险</span>
              </div>
            </a-checkbox>
          </a-col>
          <a-col :span="6">
            <a-checkbox value="观景">
              <div class="preference-item">
                <EyeOutlined class="preference-icon" />
                <span>观景</span>
              </div>
            </a-checkbox>
          </a-col>
          <a-col :span="6">
            <a-checkbox value="体验">
              <div class="preference-item">
                <ExperimentOutlined class="preference-icon" />
                <span>体验</span>
              </div>
            </a-checkbox>
          </a-col>
          <a-col :span="6">
            <a-checkbox value="美食">
              <div class="preference-item">
                <ShopOutlined class="preference-icon" />
                <span>美食</span>
              </div>
            </a-checkbox>
          </a-col>
          <a-col :span="6">
            <a-checkbox value="购物">
              <div class="preference-item">
                <ShoppingOutlined class="preference-icon" />
                <span>购物</span>
              </div>
            </a-checkbox>
          </a-col>
          <a-col :span="6">
            <a-checkbox value="文化">
              <div class="preference-item">
                <ReadOutlined class="preference-icon" />
                <span>文化</span>
              </div>
            </a-checkbox>
          </a-col>
          <a-col :span="6">
            <a-checkbox value="自然">
              <div class="preference-item">
                <GlobalOutlined class="preference-icon" />
                <span>自然</span>
              </div>
            </a-checkbox>
          </a-col>
        </a-row>
      </a-checkbox-group>
    </a-form-item>

    <a-form-item
      label="额外要求 (可选)"
      name="additionalRequirements"
    >
      <a-textarea
        v-model:value="formState.additionalRequirements"
        placeholder="请输入您的特殊需求，例如：需要无障碍设施、有儿童同行、饮食禁忌等（可选）"
        :rows="3"
        show-count
        :maxlength="500"
      />
    </a-form-item>

    <a-form-item>
      <a-button
        type="primary"
        html-type="submit"
        size="large"
        :loading="travelStore.loading"
        block
        class="submit-button"
      >
        {{ travelStore.loading ? 'AI正在规划中...' : '生成旅行计划' }}
      </a-button>
    </a-form-item>
  </a-form>
</template>

<script setup>
import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import {
  EnvironmentOutlined,
  CoffeeOutlined,
  RocketOutlined,
  EyeOutlined,
  ExperimentOutlined,
  ShopOutlined,
  ShoppingOutlined,
  ReadOutlined,
  GlobalOutlined
} from '@ant-design/icons-vue'
import { useTravelStore } from '../stores/travel'

const emit = defineEmits(['plan-generated'])
const travelStore = useTravelStore()

const formState = reactive({
  destination: '',
  startDate: null,
  days: 3,
  travelers: 2,
  budget: 2000,
  preferences: [],
  additionalRequirements: undefined
})

const onFinish = async (values) => {
  // 清理空值，确保额外要求字段为空时不发送
  const cleanedValues = {
    ...values,
    additionalRequirements: values.additionalRequirements || undefined
  }

  const result = await travelStore.generateTravelPlan(cleanedValues)

  if (result.success) {
    emit('plan-generated', result.data)
  } else {
    message.error(result.message)
  }
}

const onFinishFailed = (errorInfo) => {
  console.log('Failed:', errorInfo)
  message.error('请检查表单填写是否正确')
}
</script>

<style scoped>
.travel-plan-form {
  max-width: 800px;
  margin: 0 auto;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-item {
  margin-bottom: 16px;
}

.preference-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  transition: all 0.3s;
}

.preference-icon {
  font-size: 16px;
  color: #1890ff;
}

:deep(.ant-checkbox-wrapper:hover .preference-item) {
  background-color: #f0f8ff;
}

:deep(.ant-checkbox-wrapper-checked .preference-item) {
  background-color: #e6f7ff;
  border: 1px solid #91d5ff;
}

.submit-button {
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
}

@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>