---
name: "路书"
description: "以江南山水册页为灵感的自由行工作台"
colors:
  paper: "#f6f4ed"
  surface: "#fffef9"
  control-paper: "#fffefa"
  ink: "#202e27"
  muted: "#58665e"
  rule: "#d8ddd1"
  input-rule: "#aab7aa"
  forest: "#153a30"
  action: "#125c49"
  action-hover: "#0c4838"
  tint: "#eaf1e8"
  seal: "#934529"
  error: "#8d3027"
typography:
  display:
    fontFamily: '"Folio Display", "Noto Serif CJK SC", "Songti SC", serif'
    fontSize: "3.25rem"
    fontWeight: 700
    lineHeight: 1.24
    letterSpacing: "-0.035em"
  title:
    fontFamily: '"Noto Sans CJK SC", "PingFang SC", "Microsoft YaHei", sans-serif'
    fontSize: "1.22rem"
    lineHeight: 1.35
  body:
    fontFamily: '"Noto Sans CJK SC", "PingFang SC", "Microsoft YaHei", sans-serif'
    fontSize: "15px"
    lineHeight: 1.55
  label:
    fontSize: "0.84rem"
    fontWeight: 650
rounded:
  control: "7px"
  panel: "12px"
  chip: "999px"
spacing:
  linked-items: "8px"
  form-columns: "12px"
  stack: "16px"
  panel-inset: "22px"
components:
  button-primary:
    backgroundColor: "{colors.action}"
    textColor: "{colors.control-paper}"
    rounded: "{rounded.control}"
    padding: "8px 13px"
  button-primary-hover:
    backgroundColor: "{colors.action-hover}"
  button-subtle:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "8px 13px"
  button-text:
    textColor: "{colors.action}"
    rounded: "{rounded.control}"
    padding: "5px 1px"
  input:
    backgroundColor: "{colors.control-paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "8px 10px"
  preference-chip:
    backgroundColor: "{colors.control-paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.chip}"
    padding: "4px 9px"
  preference-chip-selected:
    backgroundColor: "{colors.tint}"
    textColor: "{colors.action}"
  panel:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.panel}"
  navigation-item:
    textColor: "{colors.muted}"
    padding: "0 4px"
  navigation-item-current:
    textColor: "{colors.action}"
---

# Design System: 路书

## Overview

**Creative North Star: "山水册页"**

象牙纸色构成底面，墨绿标题和细线划分阅读区。西湖实景照片给出地域感，日程与路线则借用册页对读的秩序。

信息以清晰的标题、时间、地点和操作排列。重点从文字和间距获得层次，绿色引导操作，朱色标记必去地点和当前关注的站点。

**Key Characteristics:**

- 暖纸底与墨绿色文字
- 写实地方照片与清晰的行程页
- 细边线与浅色选中状态
- 时间表与路线共用站点编号

## Colors

### Primary

- 墨绿用于品牌、首屏照片的暗面和关键文字。松绿用于主要按钮、链接、焦点描边及选中项。
- 浅青绿用于筛选项、进度反馈与已选日期。主操作悬停转为更深的松绿。

### Secondary

- 印泥朱用于必去标签和当前站点编号。错误文字与删除操作使用独立的错误红。

### Neutral

- 象牙纸色铺满页面。页面面板和表单控件使用较亮的近白色。
- 深墨色用于正文，浅墨色用于辅助说明。轻分隔线连接面板与列表，输入框使用更清楚的边线。

**The Route Accent Rule.** 松绿标出可操作和已选状态。印泥朱标出必去地点和当前关注的站点。

## Typography

封面标题和「路书」字标使用本地 Folio Display 字体，并以思源宋体和宋体回退。中文无衬线用于正文、控件和日程。

- 封面标题在桌面使用 3.25rem，在手机使用 2.23rem。
- 纸页标题使用 1.22rem，行程成果标题使用 1.55rem，日期安排标题使用 1.12rem。
- 基准正文为 15px，行高 1.55。表单标签使用 0.84rem 和 650 字重。

**The Aligned Numerals Rule.** 日期、时刻、距离和金额在需要前后比较的列表里使用等宽数字。

## Layout

顶部导航与内容共享最大 1320px 宽度。桌面以 22px 间隔并排摆放约束表单和行程页，面板标题左右各留 22px。行程页把当日时间表与路线、预算放在相邻阅读区。

不超过 1120px 时，路线与预算移到时间表下方并排。到 730px 时，工作台及侧栏改为单列，面板横向内边距缩为 15px，保留主导航的文字标签。到 370px 时，进一步收紧顶部横向间距，并把步骤图标放在文字上方。

## Elevation & Depth

面板依靠纸色变化和细边线划分。登录对话框使用低饱和深绿背景遮罩与柔和投影（0 18px 48px rgb(16 45 37 / 22%)），保持焦点集中在会话动作上。

## Shapes

常规控件采用 7px 圆角，页面面板采用 12px 圆角。偏好条使用胶囊轮廓，行程站点使用圆形编号。常规表单控件至少高 42px。

## Components

- 主按钮采用实色松绿底和浅色字。辅助按钮保留纸面和边线，文字按钮在悬停时显示下划线。键盘焦点使用独立描边。
- 输入框的标签和输入值始终可读。错误状态在表单中显示文字提示，禁用状态降低不透明度。
- 偏好条使用边线、浅绿底以及较重字重共同标记选中。当前导航项使用字重和底部短线识别。
- 日期切换保留天数和日期两行。站点在时刻旁列出名称和操作，路线图使用相同编号。悬停或键盘聚焦站点时，文字区的编号与图上点位同步变化。步行路段用虚线表示。
- 查询阶段的加载点和图形骨架柔和地改变不透明度，按系统的减少动态效果设置收敛。

## Do's and Don'ts

### Do:

- 保留导航和筛选项的文字标签，让状态同时通过颜色和轮廓或字重表达。
- 让时间、地点编号、路程和费用按阅读顺序对齐。
- 让路线图的编号与日程列表同步，并保留文本说明和数据来源。

### Don't:

- 避免给常规内容面板叠加阴影并抬高视觉层级。
- 避免把封面字体用于表单值、时刻和金额。
- 避免用印泥朱渲染整片内容面板。
