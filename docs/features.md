# 功能介绍 (Features)

## 核心功能

Finsight 作为一个个人账单管理系统，主要包含以下核心模块：

### 1. 交易账单管理 (Transaction Bill)
- **多维度查询**：支持按日期范围、消费类型、卡种、关键字等进行筛选。
- **消费树分析**：提供消费分类的树状结构展示。
- **智能展示**：表格列自适配，支持条纹行显示，长文本自动省略。
- **页面路径**：`/account/transaction/transaction_bill.html`

### 2. 收入与资产管理
- **工资管理 (Salary)**：记录和查询每月的工资明细。
    - 路径：`/account/salary`
- **公积金管理 (Accumulation)**：追踪住房公积金缴纳记录。
    - 路径：`/account/accumulation` (需确认路径)

### 3. 社保与福利管理
- **医保 (Medical)**：医疗保险缴纳与使用记录。
    - 路径：`/account/medical`
- **养老 (Endowment)**：养老保险缴纳记录。
    - 路径：`/account/endowment`
- **失业保险 (Unemployment)**：失业保险相关记录。
    - 路径：`/account/unemployment`
- **房租 (House Rent)**：房租支出记录管理。
    - 路径：`/account/house-rent`

## 技术特性

- **路由统一**：所有功能模块统一在 `/account/*` 路径下，结构清晰。
- **模板渲染**：使用 Thymeleaf 模板引擎 (`.html`)，替代传统的 JSP，提升渲染效率与开发体验。
- **安全机制**：基于 Spring Security 的权限控制（待确认具体配置）。
- **数据可视化**：集成 ECharts 进行数据图表展示（如消费趋势、占比等）。
