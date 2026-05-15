<template>
  <div class="language-toggle">
    <el-dropdown trigger="click" @command="handleSwitch">
      <span class="toggle-btn">
        <span class="lang-text">{{ currentLabel }}</span>
        <el-icon><ArrowDown /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="zh" :class="{ active: locale === 'zh' }">中文</el-dropdown-item>
          <el-dropdown-item command="en" :class="{ active: locale === 'en' }">English</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()

const currentLabel = computed(() => {
  return locale.value === 'zh' ? '中文' : 'EN'
})

const handleSwitch = (lang) => {
  locale.value = lang
  localStorage.setItem('locale', lang)
}
</script>

<style lang="scss" scoped>
.language-toggle {
  .toggle-btn {
    display: flex;
    align-items: center;
    gap: 2px;
    cursor: pointer;
    padding: 6px 8px;
    border-radius: 6px;
    transition: background-color 0.3s;
    font-size: 14px;
    color: var(--color-text-regular);

    &:hover {
      background-color: #f5f7fa;
    }

    .lang-text {
      font-size: 13px;
      font-weight: 500;
    }
  }

  :deep(.el-dropdown-menu__item.active) {
    color: var(--color-primary);
    font-weight: 600;
  }
}
</style>
