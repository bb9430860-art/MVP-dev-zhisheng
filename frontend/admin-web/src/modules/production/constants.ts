export const productionRoleOptions = [
  { label: "生产主管", value: "PRODUCTION_MANAGER" },
  { label: "设计", value: "DESIGNER" },
  { label: "生产工人", value: "WORKER" },
  { label: "质检", value: "QC" },
  { label: "仓库", value: "WAREHOUSE" },
  { label: "安装", value: "INSTALLER" },
];

export const productTypeLabels: Record<string, string> = {
  GENERAL: "通用导视",
  SPIRIT_FORTRESS: "精神堡垒",
  FLOOR_SIGN: "楼层牌",
  ILLUMINATED_LETTER: "发光字",
  WAYFINDING_SIGN: "普通导视牌",
};

export const assignedRoleLabels: Record<string, string> = {
  PRODUCTION_MANAGER: "生产主管",
  DESIGNER: "设计",
  WORKER: "生产工人",
  QC: "质检",
  WAREHOUSE: "仓库",
  INSTALLER: "安装",
};
