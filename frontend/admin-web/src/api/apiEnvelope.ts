import type { ApiEnvelope } from "@/types/api";

export function unwrapApiEnvelope<T>(envelope: ApiEnvelope<T>): T {
  if (envelope.code === 0) {
    return envelope.data;
  }

  throw new Error(envelope.message || "请求失败");
}
