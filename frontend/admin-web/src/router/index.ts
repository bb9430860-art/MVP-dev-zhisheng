import { createRouter, createWebHistory } from "vue-router";

import AdminLayout from "@/layout/AdminLayout.vue";
import RouteTemplateEditPage from "@/modules/process/pages/RouteTemplateEditPage.vue";
import RouteTemplateListPage from "@/modules/process/pages/RouteTemplateListPage.vue";
import ProductionOrderItemConfigPage from "@/modules/production/pages/ProductionOrderItemConfigPage.vue";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      component: AdminLayout,
      children: [
        {
          path: "",
          redirect: "/process/route-templates",
        },
        {
          path: "process/route-templates",
          name: "process-route-templates",
          component: RouteTemplateListPage,
        },
        {
          path: "process/route-templates/new",
          name: "process-route-template-new",
          component: RouteTemplateEditPage,
        },
        {
          path: "process/route-templates/:id/edit",
          name: "process-route-template-edit",
          component: RouteTemplateEditPage,
          props: true,
        },
        {
          path: "production/order-items/:orderItemId/configure",
          name: "production-order-item-configure",
          component: ProductionOrderItemConfigPage,
          props: true,
        },
      ],
    },
  ],
});
