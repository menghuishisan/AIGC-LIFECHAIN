<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
/**
 * 应用根组件
 * 路由视图容器，具体布局由各App的Layout组件控制
 */
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>
