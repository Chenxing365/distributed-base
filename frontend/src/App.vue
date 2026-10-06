<script setup lang="ts">
// UniApp 根组件
</script>

<template>
  <page-meta root-font-size="16px" />
  <router-view />
</template>

<style>
/* 全局重置 */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}
page {
  background-color: #0f1117;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', sans-serif;
}
button::after {
  border: none;
}
</style>
