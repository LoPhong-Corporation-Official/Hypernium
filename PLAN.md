# Hypernium

> **Adaptive Performance Optimization Framework for Minecraft Java Edition**
>
> Target: **Minecraft Java Edition 1.21.11**
>
> Mod Loader: **Fabric**
>
> Primary Language: **Java**
>
> Optional Native Layer: **C/C++**
>
> Status: **Development Plan**

---

# 1. Tổng quan

**Hypernium** là một performance optimization mod/framework dành cho Minecraft Java Edition 1.21.11 trên Fabric.

Khác với một mod tối ưu truyền thống, Hypernium **không cố gắng thay thế toàn bộ renderer, chunk system hoặc game simulation của Minecraft**.

Hypernium tập trung vào:

- Performance profiling.
- Benchmarking.
- Runtime performance analysis.
- Compatibility với các optimization mod khác.
- Adaptive performance control.
- Predictive chunk loading.
- Workload scheduling.
- Memory/allocation optimization.
- Performance-aware configuration.
- Experimental rendering/meshing backend.

Mục tiêu:

```text
Minecraft
    ↓
Measure
    ↓
Analyze
    ↓
Identify Bottleneck
    ↓
Select Optimization
    ↓
Apply Safely
    ↓
Measure Again
```

Nguyên tắc cốt lõi:

> **Measure first. Optimize second. Verify everything.**

---

# 2. Vấn đề Hypernium giải quyết

Minecraft có rất nhiều loại bottleneck:

```text
CPU Bottleneck
├── Entity ticking
├── AI
├── Chunk processing
├── World simulation
└── Game logic

GPU Bottleneck
├── Rendering
├── Shadows
├── Particles
├── Geometry
└── Post-processing

Memory Bottleneck
├── Allocation
├── GC
├── Chunk memory
└── Cache

Streaming Bottleneck
├── Chunk loading
├── Chunk generation
├── Mesh generation
└── Disk I/O
```

Một optimization có thể hiệu quả trong một tình huống nhưng vô ích trong tình huống khác.

Ví dụ:

```text
GPU-bound
    ↓
Tối ưu AI
    ↓
Gần như không tăng FPS
```

Vì vậy Hypernium phải xác định bottleneck trước.

---

# 3. Hypernium khác Sodium/Lithium như thế nào?

Hypernium **không đặt mục tiêu trở thành một bản Sodium/Lithium khác**.

## Sodium

Tập trung mạnh vào:

- Rendering.
- Chunk rendering.
- Mesh generation.
- GPU workload.
- Renderer architecture.

## Lithium

Tập trung mạnh vào:

- Game logic.
- Entity.
- AI.
- Block entities.
- World simulation.

## FerriteCore

Tập trung mạnh vào:

- Memory usage.
- Data structures.
- Memory optimization.

## ModernFix

Tập trung rộng vào:

- Startup.
- Memory.
- Compatibility.
- Various Minecraft optimizations.

## Hypernium

Tập trung vào:

```text
                    Hypernium
                        │
       ┌────────────────┼────────────────┐
       │                │                │
       ▼                ▼                ▼
    Profiler        Controller      Compatibility
       │                │                │
       ▼                ▼                ▼
   Benchmark        Adaptive       Mod Detection
       │                │                │
       └────────────────┼────────────────┘
                        ▼
               Optimization Policy
```

Hypernium trả lời câu hỏi:

> **"Minecraft hiện đang chậm ở đâu, những optimization nào đã có, và runtime nên làm gì tiếp theo?"**

---

# 4. Kiến trúc tổng thể

```text
                         Minecraft
                             │
                           Fabric
                             │
                           Mixin
                             │
                    ┌────────▼────────┐
                    │    Hypernium    │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
     Profiler           Controller          Compatibility
         │                   │                   │
         │                   ▼                   │
         │             Adaptive Policy           │
         │                   │                   │
         └───────────────────┼───────────────────┘
                             │
                    Optimization Layer
                             │
       ┌─────────────────────┼─────────────────────┐
       │                     │                     │
       ▼                     ▼                     ▼
    Rendering              World                Entity
       │                     │                     │
       ▼                     ▼                     ▼
    Culling              Chunk Load              AI
    LOD                  Streaming                Tick
    Mesh                 Lighting                 Collision
```

---

# 5. Công nghệ

## 5.1 Java

Ngôn ngữ chính:

```text
Java
```

Lý do:

- Minecraft Java Edition chạy trên JVM.
- Fabric sử dụng Java.
- Fabric API sử dụng Java.
- Mixin hoạt động trực tiếp trên bytecode.
- Dễ tích hợp Minecraft internals.
- Không cần native code cho phần lớn optimization.

---

# 5.2 Fabric

Target:

```text
Minecraft: 1.21.11
Loader: Fabric
API: Fabric API
Injection: Mixin
```

Fabric là nền tảng chính của Hypernium.

---

# 5.3 Gradle + Fabric Loom

Build system:

```text
Gradle
Fabric Loom
```

Cấu trúc:

```text
Hypernium/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── gradlew
├── gradlew.bat
├── LICENSE
├── README.md
├── PLAN.md
└── src/
```

---

# 5.4 C/C++

Native code **không được sử dụng trong giai đoạn đầu**.

Chỉ sử dụng nếu profiler chứng minh rằng:

```text
Java implementation
        ↓
Hot path
        ↓
Không thể tối ưu đủ
```

Khi đó:

```text
Java
 ↓
Foreign Function & Memory API / Native Interface
 ↓
C/C++
```

Các ứng dụng tiềm năng:

- SIMD.
- Compression.
- Mesh processing.
- Spatial algorithms.
- Large-scale numerical processing.

Không sử dụng native code chỉ vì:

> "C++ nhanh hơn Java."

---

# 6. Thiết kế module

```text
com.hypernium
│
├── Hypernium
│
├── core
│   ├── config
│   ├── scheduler
│   ├── threading
│   ├── metrics
│   └── lifecycle
│
├── profiler
│   ├── frame
│   ├── tick
│   ├── chunk
│   ├── entity
│   ├── memory
│   └── gpu
│
├── benchmark
│   ├── scenarios
│   ├── runner
│   ├── result
│   └── regression
│
├── compatibility
│   ├── sodium
│   ├── lithium
│   ├── ferritecore
│   ├── modernfix
│   ├── entityculling
│   └── detector
│
├── adaptive
│   ├── controller
│   ├── policy
│   ├── hysteresis
│   └── quality
│
├── rendering
│   ├── culling
│   ├── lod
│   ├── mesh
│   └── experimental
│
├── world
│   ├── chunk
│   ├── streaming
│   ├── predictive
│   └── lighting
│
├── entity
│   ├── scheduler
│   ├── ai
│   ├── tracking
│   └── experimental
│
├── memory
│   ├── allocation
│   ├── cache
│   └── gc
│
├── spatial
│   ├── hash
│   ├── bvh
│   └── query
│
├── mixin
│   ├── client
│   ├── world
│   ├── entity
│   └── rendering
│
└── debug
    ├── hud
    ├── commands
    └── logging
```

---

# 7. Core Architecture

Hypernium gồm 5 thành phần chính:

```text
┌─────────────────────────────┐
│ 1. Performance Profiler     │
├─────────────────────────────┤
│ 2. Capability Detector      │
├─────────────────────────────┤
│ 3. Performance Controller   │
├─────────────────────────────┤
│ 4. Adaptive Optimizer       │
├─────────────────────────────┤
│ 5. Compatibility Layer      │
└─────────────────────────────┘
```

---

# 8. Performance Profiler

Profiler là thành phần quan trọng nhất.

Hypernium phải biết:

```text
FPS
Frame Time
CPU Frame Time
GPU Frame Time
Tick Time
Chunk Time
Entity Time
Memory
Allocation
GC
```

---

# 9. Frame Profiler

Đo:

```text
Frame Time
CPU Frame Time
GPU Frame Time
1% Low
0.1% Low
```

Không chỉ đo FPS.

Ví dụ:

```text
FPS = 144

Frame Time:
6.8 ms
7.0 ms
6.9 ms
7.1 ms
42.8 ms
```

FPS trung bình có thể vẫn cao nhưng frame spike tạo cảm giác giật.

Hypernium phải phát hiện spike này.

---

# 10. Tick Profiler

Đo:

```text
Server Tick
Client Tick
World Tick
Entity Tick
Block Entity Tick
AI
Physics
```

Ví dụ:

```text
Tick: 12.4 ms

Entity AI:       5.2 ms
Block Entity:    2.1 ms
World:           1.8 ms
Physics:         1.4 ms
Other:           1.9 ms
```

Từ đó xác định bottleneck.

---

# 11. Chunk Profiler

Theo dõi:

```text
Chunk Load
Chunk Generation
Chunk Meshing
Chunk Upload
Chunk Unload
```

Ví dụ:

```text
Chunk Pipeline

Load       2.1 ms
Generate   7.8 ms
Mesh       3.4 ms
Upload     0.8 ms
```

---

# 12. Memory Profiler

Theo dõi:

```text
Heap
Used Heap
Allocated Memory
Allocation Rate
GC Count
GC Pause
Cache Size
```

Mục tiêu:

```text
Less allocation
       ↓
Less GC
       ↓
Less stutter
```

---

# 13. GPU Metrics

Nếu có thể lấy dữ liệu chính xác từ renderer:

```text
GPU Frame Time
Draw Calls
Vertices
Triangles
Visible Chunks
Visible Entities
```

Nếu GPU metrics không khả dụng chính xác trên một backend, Hypernium phải ghi rõ:

```text
GPU metrics unavailable
```

Không được giả lập số liệu.

---

# 14. Performance Report

Command:

```text
/hypernium profiler
```

Output:

```text
Hypernium Performance Report

Minecraft       1.21.11
Fabric          detected

Sodium          detected
Lithium         detected
FerriteCore     detected

FPS             142
1% Low          111
0.1% Low         87

CPU Frame        5.4 ms
GPU Frame        6.1 ms
Tick             3.2 ms

Entities        143
Chunks          441

Allocation      15 MB/s
GC               0.4 ms
```

---

# 15. Capability Detection

Hypernium phải biết optimization nào đã tồn tại.

Ví dụ:

```text
Sodium
    ↓
Chunk Renderer
    ↓
PROVIDED_EXTERNALLY
```

Không tự triển khai lại.

Capability state:

```text
ACTIVE
PROVIDED_EXTERNALLY
DISABLED
UNAVAILABLE
EXPERIMENTAL
```

---

# 16. Compatibility Matrix

| Feature | Hypernium Standalone | Sodium | Lithium | EntityCulling |
|---|---|---|---|---|
| Renderer | Experimental | ✓ | - | - |
| Chunk Mesh | Experimental | ✓ | - | - |
| Entity Culling | Fallback | - | - | ✓ |
| AI Optimization | Limited | - | ✓ | - |
| Profiler | ✓ | - | - | - |
| Adaptive Controller | ✓ | - | - | - |
| Predictive Loading | ✓ | - | - | - |
| Benchmark | ✓ | - | - | - |

Mục tiêu là **complement**, không phải duplicate.

---

# 17. Compatibility Strategy

Khi Sodium tồn tại:

```text
Hypernium
   ↓
Detect Sodium
   ↓
Sodium Renderer
   ↓
Hypernium monitors performance
```

Khi Sodium không tồn tại:

```text
Hypernium
   ↓
Experimental Renderer
```

Tương tự với Lithium, EntityCulling và các mod khác.

---

# 18. Performance Controller

Đây là thành phần tạo khác biệt lớn.

Input:

```text
FPS
Frame Time
CPU
GPU
Memory
GC
Entities
Chunks
```

Output:

```text
LOD
Render Distance
Entity Distance
Particle Density
Chunk Priority
Simulation Policy
```

---

# 19. Bottleneck Classification

Hypernium phân loại:

```text
CPU-bound
GPU-bound
Memory-bound
GC-bound
Chunk-bound
IO-bound
```

Ví dụ:

```text
GPU Time > CPU Time
       ↓
GPU-bound
       ↓
Apply rendering policy
```

Không thay đổi AI nếu GPU đang là bottleneck.

---

# 20. Adaptive Performance

Ví dụ:

```text
GPU overloaded
       ↓
LOD giảm
       ↓
Particle giảm
       ↓
Entity render distance giảm
       ↓
GPU workload giảm
```

CPU overloaded:

```text
CPU overloaded
       ↓
Chunk scheduling giảm
AI workload giảm
Background work giảm
```

---

# 21. Hysteresis

Không được thay đổi setting liên tục.

Sai:

```text
FPS 79
↓
80
↓
79
↓
80
```

Đúng:

```text
FPS < 75 trong 5 giây
        ↓
Decrease

FPS > 120 trong 5 giây
        ↓
Increase
```

Có cooldown giữa các thay đổi.

---

# 22. Predictive Chunk Loading

Đây là một trong những feature khác biệt chính của Hypernium.

Không chỉ load chunk dựa trên khoảng cách.

Theo dõi:

```text
Player Position
Player Velocity
Player Direction
Camera Direction
Current Chunk
```

Sau đó dự đoán:

```text
Future Position
        ↓
Future Chunks
        ↓
Priority Queue
```

Ví dụ player chạy về phía Đông:

```text
        HIGH
      HIGH HIGH
    HIGH HIGH HIGH
          P
```

Chunk phía trước được ưu tiên.

Chunk phía sau có priority thấp.

---

# 23. Chunk Priority Algorithm

Priority có thể dựa trên:

```text
Priority =
    Distance Score
  + Direction Score
  + Visibility Score
  + Velocity Prediction
  + Loading Cost
```

Không nhất thiết phải dùng công thức cố định; phải benchmark và điều chỉnh.

---

# 24. Chunk Scheduler

```text
Chunk Request
      ↓
Priority Queue
      ↓
┌─────┼─────┐
▼     ▼     ▼
HIGH NORMAL LOW
│
▼
Worker
│
▼
Result
│
▼
Main Thread
```

Không được sửa Minecraft world state tùy tiện từ worker thread.

---

# 25. Entity Optimization

Entity rendering và entity simulation phải tách biệt.

```text
Entity
 ├── Rendering
 └── Simulation
```

Entity có thể:

```text
Không render
```

nhưng vẫn:

```text
Tick bình thường
```

Ngược lại cũng có thể:

```text
Render
```

nhưng không được tùy tiện giảm simulation frequency.

---

# 26. Entity Culling

Nếu EntityCulling đã cài:

```text
Hypernium
    ↓
EntityCulling
```

Không duplicate.

Nếu không có:

```text
Hypernium fallback
```

---

# 27. Entity Tick Scheduler

Đây là **Advanced / Experimental** feature.

Không bật mặc định.

Các entity gameplay-critical:

```text
Player
Boss
Projectile
Vehicle
Redstone-related
Named entities
```

mặc định:

```text
NEVER THROTTLE
```

Entity thường có thể:

```text
Adaptive Tick
```

---

# 28. Tick Budget

Thay vì chỉ dựa vào distance:

```text
Entity Distance
```

Hypernium có thể sử dụng:

```text
CPU Budget
Entity Importance
Distance
Visibility
Current Workload
```

Ví dụ:

```text
CPU budget = 2 ms

Entity Scheduler
       ↓
Priority
       ↓
Important entities first
       ↓
Remaining entities
       ↓
Deferred
```

Điều này an toàn hơn distance-only throttling.

---

# 29. AI Optimization

Có thể nghiên cứu:

```text
Pathfinding Cache
Goal Scheduling
Navigation Cache
Nearby Entity Cache
Brain Update Scheduling
```

Nhưng nếu Lithium đã cung cấp optimization tương tự:

```text
→ Hypernium không duplicate.
```

---

# 30. Spatial Systems

Chỉ triển khai nếu profiler cho thấy cần.

Các thuật toán:

```text
Spatial Hash
BVH
Octree
Quadtree
```

Ứng dụng:

```text
Entity lookup
Collision
Raycast
Nearby queries
```

Không mặc định sử dụng tất cả.

---

# 31. Rendering

Rendering là **secondary optimization layer**.

Hypernium ưu tiên:

```text
Profiler
 ↓
Compatibility
 ↓
Adaptive control
 ↓
Renderer integration
```

không phải:

```text
Rewrite renderer immediately
```

---

# 32. Frustum Culling

Nếu backend hiện tại đã có:

```text
Sodium
```

Hypernium chỉ:

```text
monitor
analyze
control
```

Nếu không:

```text
Hypernium fallback culling
```

Thuật toán:

```text
AABB vs Frustum Planes
```

---

# 33. LOD

Hypernium có thể bổ sung adaptive LOD nếu backend cho phép.

LOD dựa trên:

```text
Distance
Screen Size
Performance State
Object Importance
```

Ví dụ:

```text
GPU healthy
    → higher LOD

GPU overloaded
    → lower LOD
```

---

# 34. Chunk Meshing

Không coi đây là feature bắt buộc.

Có ba backend:

```text
Sodium
   ↓
Preferred

Vanilla
   ↓
Hypernium experimental

Custom Renderer
   ↓
Research
```

---

# 35. Greedy Meshing

Greedy Meshing chỉ triển khai trong:

```text
Experimental Renderer
```

Không thay thế Sodium mặc định.

Pipeline:

```text
Block Data
   ↓
Visible Faces
   ↓
Greedy Merge
   ↓
Mesh
```

Benchmark phải chứng minh:

```text
CPU Mesh Time ↓
Vertices ↓
Triangles ↓
Memory ↓
```

Nếu không cải thiện tổng thể:

```text
Reject
```

---

# 36. Occlusion Culling

Các hướng:

```text
CPU Occlusion
GPU Occlusion
Hi-Z
```

Ưu tiên:

```text
CPU prototype
      ↓
Benchmark
      ↓
GPU Hi-Z research
```

Không triển khai Hi-Z ngay từ đầu.

---

# 37. Memory Optimization

Tập trung:

```text
Allocation reduction
Cache
Primitive data
Temporary object reduction
Boxing reduction
```

Không dùng object pooling nếu benchmark cho thấy pooling gây:

```text
Memory retention
GC promotion
CPU overhead
```

---

# 38. Garbage Collection

Theo dõi:

```text
Allocation Rate
GC Count
GC Pause
Heap Pressure
```

Mục tiêu:

```text
Allocation ↓
GC ↓
Frame Spike ↓
```

---

# 39. Threading

Hypernium có scheduler riêng:

```text
HyperniumScheduler
```

Có thể quản lý:

```text
Chunk prediction
Pathfinding
Mesh generation
Spatial processing
Benchmark
Analysis
```

Không đưa toàn bộ Minecraft sang worker thread.

---

# 40. Thread Safety

Nguyên tắc:

```text
Minecraft mutable state
        ↓
Main Thread
```

Worker sử dụng:

```text
Immutable snapshot
```

và trả:

```text
Result
```

về main thread.

Pipeline:

```text
Main Thread
    ↓
Snapshot
    ↓
Worker
    ↓
Result
    ↓
Main Thread Commit
```

---

# 41. Benchmark Framework

Hypernium phải có benchmark riêng.

Scenario:

```text
Flat World
Forest
Village
Cave
Mob Farm
Redstone
Dense Entities
High Render Distance
Chunk Generation
Chunk Loading
```

Mỗi benchmark phải chạy nhiều lần.

Không sử dụng một lần chạy duy nhất để kết luận.

---

# 42. Benchmark Metrics

```text
Average FPS
Median FPS
1% Low
0.1% Low
Frame Time
CPU Time
GPU Time
Tick Time
Chunk Time
Memory
Allocation
GC
```

---

# 43. Regression Testing

Mỗi optimization:

```text
Baseline
   ↓
Optimization
   ↓
Benchmark
   ↓
Comparison
```

Ví dụ:

```text
Chunk load

Before: 18.2 ms
After:  11.7 ms

Improvement: 35.7%
```

Nếu:

```text
Performance ↑
Gameplay correctness ↓
```

→ Reject.

---

# 44. Debug HUD

Phím mặc định:

```text
F8
```

có thể bật Hypernium HUD.

Hiển thị:

```text
Hypernium

FPS       144
Frame     6.9ms
CPU       4.1ms
GPU       5.7ms

Tick      3.2ms
Chunks    441
Entities  127

GC        0.3ms
Alloc     12MB/s

Mode      Balanced
Bottleneck: GPU
```

Phím có thể configurable.

---

# 45. Configuration

File:

```text
config/hypernium.json
```

Ví dụ:

```json
{
  "profiler": {
    "enabled": true,
    "hud": false
  },

  "adaptive": {
    "enabled": true,
    "mode": "balanced"
  },

  "chunk": {
    "predictiveLoading": true
  },

  "entity": {
    "adaptiveTicking": false
  },

  "experimental": {
    "customRenderer": false,
    "greedyMeshing": false,
    "occlusionCulling": false
  }
}
```

---

# 46. Performance Modes

## Safe

```text
Profiler
Compatibility
Adaptive rendering
Predictive loading
```

## Balanced

```text
Safe features
+
Aggressive chunk scheduling
+
Additional adaptive policies
```

## Aggressive

```text
Balanced
+
Experimental entity policies
+
More aggressive quality adaptation
```

## Experimental

```text
Experimental renderer
Experimental meshing
Experimental occlusion
Experimental scheduling
```

---

# 47. Compatibility Modes

Hypernium phải có:

```text
AUTO
STANDALONE
INTEGRATION
SAFE
```

### AUTO

Tự phát hiện mod.

### STANDALONE

Không phụ thuộc optimization mod khác.

### INTEGRATION

Tối ưu khi chạy cùng Sodium/Lithium/etc.

### SAFE

Disable các feature có khả năng gây conflict.

---

# 48. Phase 0 — Foundation

Mục tiêu:

```text
Fabric 1.21.11
Gradle
Loom
Mixin
Config
Logger
Lifecycle
```

Output:

```text
Minecraft launches
Hypernium initializes
```

---

# 49. Phase 1 — Performance Infrastructure

Đây là phase quan trọng nhất.

Implement:

```text
Frame Profiler
Tick Profiler
Chunk Profiler
Entity Profiler
Memory Profiler
Metrics
HUD
Performance Report
```

Chưa cần optimization lớn.

Mục tiêu:

> **Biết Minecraft đang chậm ở đâu.**

---

# 50. Phase 2 — Compatibility Layer

Detect:

```text
Sodium
Lithium
FerriteCore
ModernFix
EntityCulling
```

Xây:

```text
Capability Registry
```

Ví dụ:

```text
Capability:
CHUNK_RENDERING

Provider:
Sodium
```

Hypernium không duplicate.

---

# 51. Phase 3 — Performance Controller

Implement:

```text
Bottleneck Detector
Performance Controller
Policy Engine
Hysteresis
Cooldown
```

Pipeline:

```text
Metrics
   ↓
Classifier
   ↓
Policy
   ↓
Controller
   ↓
Optimization
```

---

# 52. Phase 4 — Adaptive Rendering

Feature:

```text
Dynamic LOD
Dynamic Entity Distance
Dynamic Particle Density
```

Không thay thế Sodium.

Hypernium chỉ điều khiển quality nếu backend hỗ trợ.

---

# 53. Phase 5 — Predictive Chunk Loading

Feature trọng tâm của Hypernium:

```text
Player movement prediction
Chunk priority
Prefetch
Async scheduling
```

Benchmark với:

```text
Walking
Running
Elytra
Boat
High-speed movement
Teleport
```

---

# 54. Phase 6 — Entity / Simulation

Chỉ bắt đầu sau khi profiler chứng minh:

```text
Entity simulation
```

là bottleneck.

Implement:

```text
Entity importance
Tick budget
Adaptive scheduling
AI caching
```

Entity Tick Scheduler mặc định:

```text
OFF
```

---

# 55. Phase 7 — Memory

Implement:

```text
Allocation reduction
Cache optimization
Memory metrics
GC analysis
```

Không thay đổi JVM GC configuration một cách tự động.

---

# 56. Phase 8 — Spatial Systems

Chỉ triển khai khi cần:

```text
Spatial Hash
BVH
Octree
```

Benchmark:

```text
10
100
1,000
10,000
100,000 entities
```

Chọn thuật toán dựa trên workload.

---

# 57. Phase 9 — Experimental Renderer

Chỉ dành cho:

```text
Sodium absent
OR
Developer testing
```

Implement:

```text
Custom Mesh
Greedy Meshing
Culling
Batching
```

Không trở thành dependency bắt buộc.

---

# 58. Phase 10 — Advanced GPU

Nghiên cứu:

```text
GPU Instancing
Indirect Rendering
Occlusion Culling
Hi-Z
Advanced Batching
```

Chỉ tiếp tục nếu benchmark chứng minh lợi ích.

---

# 59. Phase 11 — Benchmark & Regression

Xây:

```text
Automated Benchmark
Performance Baseline
Regression Detector
Compatibility Matrix
```

Mỗi release có:

```text
Performance Report
```

---

# 60. Phase 12 — Native Acceleration

Chỉ thực hiện nếu:

```text
Profiler
   ↓
Hot path
   ↓
Java optimized
   ↓
Still bottleneck
```

Sau đó:

```text
Java
 ↓
FFM / Native
 ↓
C/C++
```

Native module phải optional.

Minecraft vẫn chạy nếu native library không tồn tại.

---

# 61. Release Architecture

Hypernium phải có fallback:

```text
Optimized Path
      │
      ├── Success
      │      ↓
      │   Continue
      │
      └── Failure
             ↓
       Vanilla Path
```

Không để một optimization experimental làm crash Minecraft.

---

# 62. Error Handling

Nếu subsystem lỗi:

```text
Subsystem Error
      ↓
Log Error
      ↓
Disable subsystem
      ↓
Fallback
      ↓
Continue Game
```

Ví dụ:

```text
[Hypernium]
Occlusion Culling failed.
Subsystem disabled.
Falling back to renderer backend.
```

---

# 63. Coding Standards

Ngôn ngữ:

```text
Java
```

Code phải:

- rõ ràng
- type-safe
- thread-safe
- ít allocation trong hot path
- có benchmark cho optimization quan trọng
- có comment giải thích các quyết định kỹ thuật

Ví dụ:

```java
// This method runs on the entity hot path.
// Avoid allocating temporary collections here.
```

---

# 64. Hot Path Rules

Trong hot path:

```text
❌ new object
❌ boxing
❌ unnecessary stream
❌ unnecessary lambda
❌ logging
❌ synchronization không cần thiết
```

Ưu tiên:

```text
✓ primitive data
✓ cache
✓ reuse
✓ branch-aware logic
✓ predictable data access
```

Nhưng mọi thay đổi phải được benchmark.

---

# 65. Logging

Không log trong:

```text
render loop
tick loop
entity loop
chunk loop
```

Chỉ log:

```text
Initialization
Configuration
Subsystem state
Warnings
Errors
Benchmark result
```

---

# 66. Security / Stability

Hypernium không:

- sửa file game ngoài phạm vi cần thiết
- gửi telemetry bắt buộc
- thu thập dữ liệu người dùng không cần thiết
- thay đổi world save format tùy tiện
- tự động upload performance data

Benchmark local là mặc định.

---

# 67. Definition of Done

Một optimization chỉ được coi là hoàn thành khi:

```text
[✓] Implemented
[✓] Compiles
[✓] Minecraft launches
[✓] No crash
[✓] Gameplay verified
[✓] Benchmark completed
[✓] Performance measured
[✓] Regression tested
[✓] Compatibility checked
[✓] Fallback implemented
[✓] Documentation written
```

---

# 68. Thành công của Hypernium

Hypernium không được đánh giá bằng:

```text
"FPS tăng bao nhiêu?"
```

mà bằng:

```text
FPS
+
1% Low
+
0.1% Low
+
Frame Time
+
Tick Time
+
Chunk Time
+
Memory
+
GC
+
Compatibility
+
Stability
```

Ví dụ một optimization:

```text
FPS:
140 → 145

1% Low:
80 → 115

Frame spike:
40ms → 18ms
```

Đây là một optimization rất thành công dù FPS trung bình chỉ tăng ~3.5%.

---

# 69. Final Architecture

```text
                         ┌──────────────────────┐
                         │ Minecraft 1.21.11    │
                         └──────────┬───────────┘
                                    │
                                  Fabric
                                    │
                                  Mixin
                                    │
                         ┌──────────▼───────────┐
                         │      Hypernium       │
                         └──────────┬───────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            │                       │                       │
            ▼                       ▼                       ▼
       Performance             Compatibility          Benchmark
        Profiler                  Layer                 Engine
            │                       │                       │
            └───────────────────────┼───────────────────────┘
                                    │
                                    ▼
                         Bottleneck Classifier
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
                    ▼               ▼               ▼
                  CPU              GPU            Memory
                    │               │               │
                    └───────────────┼───────────────┘
                                    ▼
                         Performance Controller
                                    │
                                    ▼
                           Adaptive Policy
                                    │
             ┌──────────────────────┼──────────────────────┐
             │                      │                      │
             ▼                      ▼                      ▼
       Chunk Pipeline         Rendering Layer        Entity Layer
             │                      │                      │
       Predictive Load        Adaptive LOD          Tick Policy
       Priority Queue         Culling               AI
       Async Work             Mesh                   Spatial
             │                      │                      │
             └──────────────────────┼──────────────────────┘
                                    │
                                    ▼
                           Minecraft Runtime
```

---

# 70. Core Principle

Hypernium phải tuân thủ 5 nguyên tắc:

```text
1. Measure
2. Understand
3. Optimize
4. Benchmark
5. Verify
```

Không tối ưu chỉ vì một thuật toán nghe có vẻ nhanh.

Không duplicate một optimization đã được Sodium/Lithium/FerriteCore/ModernFix làm tốt.

Không thay renderer nếu profiler chưa chứng minh renderer là bottleneck.

Không throttle entity gameplay-critical chỉ để tăng FPS.

Không thêm C/C++ nếu Java vẫn đủ nhanh.

---

# 71. Định hướng cuối cùng

Hypernium sẽ trở thành một:

> **Adaptive Performance Layer for Minecraft**

thay vì một:

> **Collection of FPS Tweaks**

Mục tiêu cuối cùng:

```text
              Hypernium
                  │
      ┌───────────┼───────────┐
      ▼           ▼           ▼
    Measure     Analyze     Control
      │           │           │
      └───────────┼───────────┘
                  ▼
              Optimize
                  │
                  ▼
              Benchmark
                  │
                  ▼
              Verify
                  │
                  ▼
        Smooth Minecraft
```

**Ưu tiên phát triển thực tế:**

```text
Phase 0
  ↓
Phase 1 — Profiler
  ↓
Phase 2 — Compatibility
  ↓
Phase 3 — Controller
  ↓
Phase 4 — Adaptive systems
  ↓
Phase 5 — Predictive chunk loading
  ↓
Phase 6 — Entity / Simulation
  ↓
Phase 7 — Memory
  ↓
Phase 8 — Spatial
  ↓
Phase 9 — Experimental Renderer
  ↓
Phase 10 — Advanced GPU
  ↓
Phase 11 — Benchmark / Regression
  ↓
Phase 12 — Optional Native
```

> **Hypernium không cố làm mọi thứ tốt hơn Minecraft ngay từ ngày đầu. Hypernium trước tiên phải biết chính xác Minecraft đang làm gì, tại sao nó chậm, và optimization nào thực sự đáng áp dụng.**