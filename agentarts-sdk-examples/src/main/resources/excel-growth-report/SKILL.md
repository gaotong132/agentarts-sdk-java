---
name: 经营增长报告生成器
description: 分析业务数据（日活DAU、营收），计算同比增长率并生成带图表的专业Excel报表。当用户提到对比DAU、营收增长、同比增长分析、经营报表生成，或需要带增长率计算的Excel文件时使用。
license: MIT
metadata:
  作者: 你的组织
  版本: "1.0"
---

# 经营增长报告生成器

## 概述
本技能接收业务数据（DAU和营收），计算同比（YoY）增长率，并生成带格式化表格和图表的专业Excel文件。专为业务分析师、产品经理、运营团队设计，帮助快速获取可靠的增长报告。

## 适用场景
- 用户要求对比DAU或营收的增长率
- 用户提到"同比增长"、"年同比"、"YoY"
- 用户需要包含业务指标和图表的Excel报告
- 用户上传了业务数据并要求分析

## 核心原则

### 零公式错误
- 每个生成的Excel模型必须保证零公式错误（#REF!、#DIV/0!、#VALUE!、#N/A、#NAME?）
- 在最终输出前，必须对边界情况进行公式测试

### 公式构建规则
- 所有假设条件必须放在单独的假设单元格中
- 使用单元格引用替代公式中的硬编码数值
- 示例：使用`=B5*(1+$B$6)`而非`=B5*1.05`
- 写入前验证所有单元格引用是否正确
- 检查是否有"偏一位"错误：Excel行号从1开始

### 数据处理规则
- **绝对禁止在单元格中硬编码计算值**
- ❌ 错误写法：`sheet['B10'] = 总金额`（把5000写死了）
- ✅ 正确写法：`sheet['B10'] = '=SUM(B2:B9)'`（让Excel自己计算）
- 处理前先校验输入数据（处理空值、负值、异常值）
- 使用`inspect_data`函数，先预览前几行再全量处理

## 输入数据要求
本技能期望的数据包含以下列：
- `日期`（date）：记录日期（格式：YYYY-MM-DD 或 YYYY-MM）
- `指标`（metric）：指标类型（`dau` 或 `revenue`）
- `数值`（value）：该指标的数值

## 脚本调用方式

本技能包含一个Python脚本 `scripts/generate_growth_report.py`，提供以下函数：

### 函数1：inspect_data()
**用途**：预览数据前几行，了解数据结构
**调用方式**：
```python
from scripts.generate_growth_report import inspect_data
preview = inspect_data('data.csv', n_rows=5)
print(preview)  # 输出Markdown格式表格
```

### 函数2：generate_growth_report()
**用途**：处理数据并生成Excel报告
**调用方式**：
```python
from scripts.generate_growth_report import generate_growth_report
import pandas as pd

# 方式1：从文件读取
df = pd.read_excel('经营数据.xlsx')
result = generate_growth_report(df, output_dir='./reports')

# 方式2：从DataFrame直接传入
result = generate_growth_report(df, output_dir='./reports')

print(result)  # 输出：{"status": "success", "file": "growth_report_20260709.xlsx", "errors": 0}
```

### 函数3：validate_report()
**用途**：生成后校验Excel文件公式是否正确
**调用方式**：
```python
from scripts.generate_growth_report import validate_report
validation = validate_report('growth_report_20260709.xlsx')
print(validation)  # 输出：{"valid": True, "error_cells": []}
```

## 工作流程
LLM在执行本Skill时，需按以下步骤调用脚本：

### 第一步：校验并查看数据
```python
from scripts.generate_growth_report import inspect_data
import pandas as pd

# 读取用户文件
df = pd.read_excel(user_file)

# 预览数据结构
preview = inspect_data(df, n_rows=5)
# 检查列名是否为：日期、指标、数值
```

### 第二步：处理和分析数据
```python
from scripts.generate_growth_report import generate_growth_report

# 生成报告
result = generate_growth_report(df, output_dir='./reports')
```

### 第三步：校验并返回结果
```python
from scripts.generate_growth_report import validate_report

# 校验生成的Excel
validation = validate_report(result['file'])
if validation['valid']:
    return f"✅ 报告生成成功！文件：{result['file']}"
else:
    return f"⚠️ 报告生成完成，但发现错误：{validation['error_cells']}"
```

## 错误处理
| 错误类型   | 处理方式                                    |
|--------|-----------------------------------------|
| 数据校验失败 | 返回："错误：缺少必需列。期望列：日期、指标、数值。请检查文件格式。"     |
| 除以零    | 在增长率计算中，如分母为零，将增长率设为"N/A"并附说明           |
| 日期转换失败 | 尝试多种日期格式（YYYY-MM-DD、YYYY-MM），失败则给出明确报错  |

## 输出示例

增长报告_20260709.xlsx
├── 工作表1："数据与增长"
│   ├── 列：日期、DAU、DAU同比、营收、营收同比
│   ├── 同比增长率条件格式（红绿标注）
│   └── 自动筛选启用
├── 工作表2："汇总看板"
│   ├── 关键指标汇总表
│   ├── 柱状图：DAU同比增长率
│   ├── 柱状图：营收同比增长率
│   └── 折线图：DAU与营收趋势
└── 工作表3："处理说明"
└── 数据处理决策记录