<template>
  <el-container class="admin-shell">
    <el-aside width="220px" class="admin-sidebar">
      <div class="brand">至胜 MVP</div>
      <el-menu router :default-active="$route.path" class="side-menu">
        <el-menu-item index="/process/route-templates">
          <el-icon><Tickets /></el-icon>
          <span>工艺路线模板</span>
        </el-menu-item>
        <el-menu-item index="/production/order-items/1001/configure">
          <el-icon><Operation /></el-icon>
          <span>生产下发配置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <span>生产线管理后台</span>
      </el-header>
      <el-main class="admin-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { Operation, Tickets } from "@element-plus/icons-vue";
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
}

.admin-sidebar {
  border-right: 1px solid #d9e2ec;
  background: #ffffff;
}

.brand {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-weight: 700;
  border-bottom: 1px solid #edf2f7;
}

.side-menu {
  border-right: 0;
}

.admin-header {
  height: 56px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #d9e2ec;
  background: #ffffff;
  font-weight: 600;
}

.admin-main {
  padding: 20px;
}
</style>
