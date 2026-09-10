<div align="center">

# 🤖 本项目 100% 由 DeepSeek 生成

#### 一行代码都没手写 —— 包括这份 README

---

**一个文科生 × DeepSeek = 一个能用的 AI 绘图 App**

</div>

<br>

> ### 关于作者
>
> 我是个**文科生**，完全不会写代码。
>
> 这个 App 的每一行 Kotlin、每一个界面布局、每一处资源文件，甚至「怎么把代码推上 GitHub」、
> 「怎么让 GitHub Actions 自动打包 APK」这些事，**全都是 DeepSeek 手把手教我做的**。
>
> 我负责的只有三件事：**说清楚我想要什么** → **点确认** → **装到手机上试**。
>
> 所以如果你也是不会写代码的人，看到这个仓库可以放心：
> **想做一个 App，现在真的不需要先学会写代码。**

<br>

---

# AI图片生成器 Android APP

## 功能特性

1. **自定义Base URL和API地址** - 支持动态配置API服务器地址
2. **配置持久化** - API设置自动保存，下次启动自动加载
3. **从API拉取模型列表** - 支持动态加载可用的AI模型
4. **自定义生图质量** - 提供4个质量等级：
   - 低质量 (15步，快速生成)
   - 中等质量 (25步)
   - 高质量 (35步)
   - 超高质量 (50步，慢速但精细)
5. **自定义图片尺寸** - 可自定义宽度和高度
6. **提示词输入** - 支持多行文本输入
7. **图片预览** - 生成后立即显示
8. **参考图（垫图）** - 可一次添加多张参考图，随请求以 multipart 发到 /images/edits 做图生图

## 项目结构

```
ImageGenApp/
├── app/
│   ├── build.gradle.kts          # 应用级构建配置
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/example/imagegen/
│           │   ├── MainActivity.kt              # 主界面
│           │   ├── ApiPreferences.kt            # SharedPreferences封装
│           │   ├── api/
│           │   │   ├── ImageGenApi.kt          # API接口定义
│           │   │   └── RetrofitClient.kt       # 动态URL网络客户端
│           │   ├── repository/
│           │   │   └── ImageGenRepository.kt   # 数据仓库
│           │   └── viewmodel/
│           │       └── MainViewModel.kt        # ViewModel
│           └── res/
│               ├── layout/
│               │   └── activity_main.xml       # 主界面布局
│               └── values/
│                   ├── strings.xml
│                   ├── colors.xml
│                   └── themes.xml
├── build.gradle.kts              # 项目级构建配置
├── settings.gradle.kts
└── gradle.properties
```

## 使用技术栈

- **Kotlin** - 主要编程语言
- **MVVM架构** - ViewModel + LiveData
- **Retrofit** - 动态Base URL网络请求
- **OkHttp** - HTTP客户端
- **Gson** - JSON解析
- **Coroutines** - 异步处理
- **Glide** - 图片加载
- **SharedPreferences** - 配置持久化
- **ViewBinding** - 视图绑定
- **Material Design 3** - UI组件

## 使用说明

### 1. 配置API服务器

首次使用时，需要配置API服务器：

1. 在"Base URL"输入框中输入你的API服务器地址
   - 例如：`https://api.example.com/v1`
   - 或：`http://192.168.1.100:8000/api`
   
2. 在"API Key"输入框中输入你的密钥

3. 点击"保存设置"按钮保存配置（下次启动自动加载）

### 2. 加载模型

配置完成后：
1. 点击"加载模型"按钮
2. 应用会从你配置的API地址拉取可用模型列表
3. 成功后可在下拉列表中选择模型

### 3. 生成图片

1. 从下拉列表选择模型
2. 输入提示词
3. 选择生成质量（低/中/高/超高）
4. 可选：调整图片尺寸
5. 点击"生成图片"

## API接口说明

### 获取模型列表
```
GET {BASE_URL}/models
Header: Authorization: Bearer {API_KEY}

Response:
{
  "models": [
    {
      "id": "model-1",
      "name": "Stable Diffusion XL",
      "description": "高质量图片生成模型"
    }
  ]
}
```

### 生成图片
```
POST {BASE_URL}/generate
Header: Authorization: Bearer {API_KEY}

Request Body:
{
  "model": "model-1",
  "prompt": "a beautiful landscape",
  "quality": "medium",
  "width": 512,
  "height": 512,
  "steps": 25
}

Response:
{
  "success": true,
  "imageUrl": "https://...",
  "imageBase64": "base64...",  // 可选
  "message": null
}
```

## 配置持久化

应用使用SharedPreferences自动保存：
- Base URL
- API Key

下次启动时自动加载，无需重复输入。

## 权限

应用需要以下权限（已在AndroidManifest.xml中声明）：
- `INTERNET` - 网络访问
- `WRITE_EXTERNAL_STORAGE` - 保存图片（Android 9及以下）
- `READ_EXTERNAL_STORAGE` - 读取图片（Android 12及以下）

## 构建要求

- Android Studio Hedgehog | 2023.1.1 或更新版本
- JDK 17
- Android SDK 34
- Gradle 8.2
- 最低支持 Android 7.0 (API 24)

## 扩展功能建议

1. **多配置管理** - 支持保存多个API配置，快速切换
2. **图片保存** - 添加保存到相册功能
3. **历史记录** - 记录生成历史和使用过的提示词
4. **批量生成** - 支持一次生成多张图片
5. **负面提示词** - 添加negative prompt支持
6. **高级参数** - CFG Scale、采样器选择、种子值等
7. **图片编辑** - 图生图、局部重绘
8. **收藏功能** - 收藏喜欢的生成结果
9. **分享功能** - 分享到社交媒体
10. **自定义API端点** - 除了models和generate，支持更多自定义端点

## 注意事项

1. Base URL必须是完整的URL，包含协议（http://或https://）
2. Base URL会自动添加末尾斜杠（如果没有）
3. 图片生成可能需要较长时间，已设置60秒超时
4. 如果使用http://协议，需确保android:usesCleartextTraffic="true"
5. 首次使用必须先保存设置并加载模型
6. 高质量生成会消耗更多时间和API配额

## 示例配置

### OpenAI兼容API
```
Base URL: https://api.openai.com/v1
API Key: sk-xxx...
```

### 本地Stable Diffusion WebUI
```
Base URL: http://127.0.0.1:7860/sdapi/v1
API Key: (留空或输入设置的密钥)
```

### 自托管API
```
Base URL: https://your-domain.com/api/v1
API Key: your-custom-api-key
```
