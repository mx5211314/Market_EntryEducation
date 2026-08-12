# 入市教育智慧助手 - 后端项目总结

## 项目概述

本项目基于 **Spring Boot 3 + LangChain4j** 构建的智能投教助手，将传统灌输式投教系统升级为具备语义洞察与自主引导能力的智能伴学助手。核心实现了 **RAG 检索增强问答**、**多状态引导 Agent**、**意图重写**、**合规校验与自动重试**、**MCP 金融数据模拟插件** 等功能。

---

## 技术栈

| 技术         | 说明                          |
| ------------ | ----------------------------- |
| Spring Boot 3.2.5 | 主框架 |
| LangChain4j 0.36.2 | AI 应用编排，集成 Chat、Embedding、Vector Store |
| 阿里云百炼 DashScope | 提供大模型（qwen-turbo）和 Embedding 模型（text-embedding-v2） |
| Milvus 2.3.4 | 向量数据库 |
| Elasticsearch 8.15.0 | 搜索引擎（当前仅双写，混合检索因版本兼容问题暂用 Milvus 单路+本地重排） |
| MySQL（未实际使用，但架构预留） | 用户会话存储 |
| Redis（未实际使用，架构预留） | 缓存与状态管理 |
| 自研状态机 | 替代 LangGraph4j 实现多状态 Agent |
| 本地 Embedding 相似度 | 重排序（余弦相似度） |
| PDFBox 2.0.29 | PDF 文档解析 |
| Docker Desktop | 运行 Milvus、ES 容器 |

---

## 系统架构
