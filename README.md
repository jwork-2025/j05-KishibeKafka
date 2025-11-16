[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/iHSjCEgj)
# J05

本版本采用 LWJGL + OpenGL 实现纯 GPU 渲染，窗口与输入基于 GLFW，文本渲染通过 AWT 字体离屏生成纹理后在 OpenGL 中批量绘制。

## 视频链接
[bilibili](https://www.bilibili.com/video/BV1fgCQBUEFJ/)

## 核心改动
- GameLogic: GameLogic只保留了分数统计、碰撞处理逻辑，玩家输入逻辑由PlayerController组件实现，物理系统更新由PhysicsSystem实现
- PhysicsSystem：面向“过程”的批处理逻辑，跨对象统一执行。例如 `PhysicsSystem` 负责所有带 `PhysicsComponent` 的对象物理更新。并行物理计算通过 `ExecutorService` 线程池实现，按批处理提升多核利用。
- GPURenderer*：渲染后端抽象与 LWJGL 实现，负责窗口/上下文/绘制 API 封装，文本纹理缓存与绘制，实现了图片纹理缓存与绘制


## 游戏录制/回放机制

- **存储抽象**：`RecordingStorage` 定义录制的读/写/列举接口，默认实现 `FileRecordingStorage`（JSONL 文件）。
- **录制服务**：`RecordingService` 在运行时异步写 JSONL 行：
  - header：窗口大小/版本
  - EntityInfo：定义实体信息包括id、status、x、y、w、h、image、angle，记录玩家/敌人/火球物体的信息
  - keyframe：周期关键帧，保存了三种对象的位置、大小、图片、角度信息
  - 采用“暖机 + 周期写入 + 结束强制写入”的策略，避免空关键帧
- **回放场景**：`ReplayScene` 读取 JSONL，解析为 keyframe 列表，按时间在相邻关键帧间做线性插值，使用`RenderComponent`恢复外观并渲染。