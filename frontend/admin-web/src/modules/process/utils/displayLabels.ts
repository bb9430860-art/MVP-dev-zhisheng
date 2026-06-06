const productTypeLabels: Record<string, string> = {
  GENERAL: "通用导视",
  SPIRIT_FORTRESS: "精神堡垒",
  FLOOR_SIGN: "楼层牌",
  ILLUMINATED_LETTER: "发光字",
  WAYFINDING_SIGN: "普通导视牌",
};

const assignedRoleLabels: Record<string, string> = {
  ADMIN: "管理员",
  PRODUCTION_MANAGER: "生产主管",
  DESIGNER: "设计",
  WORKER: "生产工人",
  QC: "质检",
  WAREHOUSE: "仓库",
  INSTALLER: "安装",
};

export function formatProductType(value?: string | null) {
  if (!value) {
    return "通用";
  }
  return productTypeLabels[value] ?? value;
}

export function formatAssignedRole(value?: string | null) {
  if (!value) {
    return "-";
  }
  return assignedRoleLabels[value] ?? value;
}
