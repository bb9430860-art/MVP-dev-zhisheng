import { ElMessage } from "element-plus";
import axios from "axios";

import { unwrapApiEnvelope } from "./apiEnvelope";
import type { ApiEnvelope } from "@/types/api";

export const httpClient = axios.create({
  baseURL: "",
  timeout: 15000,
  validateStatus: () => true,
});

httpClient.interceptors.request.use((config) => {
  const token = window.localStorage.getItem("zhisheng_admin_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export async function requestData<T>(
  request: Promise<{ data: ApiEnvelope<T> }>,
): Promise<T> {
  try {
    const response = await request;
    return unwrapApiEnvelope(response.data);
  } catch (error) {
    const message = error instanceof Error ? error.message : "请求失败";
    ElMessage.error(message);
    throw error;
  }
}
