package com.gameengine.core;

import com.gameengine.graphics.IRenderer;
import com.gameengine.graphics.RenderBackend;
import com.gameengine.graphics.RendererFactory;
import com.gameengine.input.InputManager;
import com.gameengine.recording.RecordingService;
import com.gameengine.scene.Scene;

import javax.swing.Renderer;
import javax.swing.Timer;

/**
 * 游戏引擎
 */
public class GameEngine {
    private IRenderer renderer;
    private InputManager inputManager;
    private SpriteLoader spriteLoader;
    private Scene currentScene;
    private PhysicsSystem physicsSystem;
    private boolean running;
    private float targetFPS;
    private float deltaTime;
    private long lastTime;
    private String title;
    private Timer gameTimer;

    // 添加FPS统计相关变量
    private int frameCount;      // 帧计数器
    private float fpsUpdateTimer; // FPS更新时间累加器
    private float currentFPS;    // 当前FPS值（每秒更新一次）

    // 录制
    private RecordingService recordingService;
    
    public GameEngine(int width, int height, String title, RenderBackend backend) {
        this.title = title;
        this.renderer = RendererFactory.createRenderer(backend, width, height, title);
        this.inputManager = InputManager.getInstance();
        this.spriteLoader = SpriteLoader.getInstance();
        this.running = false;
        this.targetFPS = 60.0f;
        this.deltaTime = 0.0f;
        this.lastTime = System.nanoTime();

        // 初始化FPS统计变量
        this.frameCount = 0;
        this.fpsUpdateTimer = 0.0f;
        this.currentFPS = 0.0f;
    }
    
    /**
     * 初始化游戏引擎
     */
    public boolean initialize() {
        return true;
    }
    
    /**
     * 运行游戏引擎
     */
    public void run() {
        if (!initialize()) {
            System.err.println("游戏引擎初始化失败");
            return;
        }
        
        running = true;
        
        // 初始化当前场景
        if (currentScene != null) {
            currentScene.initialize();
            if (currentScene.getName().equals("MainMenu")) {
                physicsSystem = null;
            } else {
                physicsSystem = new PhysicsSystem(currentScene, renderer.getWidth(), renderer.getHeight());
            }
        }
        
        long lastFrameTime = System.nanoTime();
        long frameTimeNanos = (long)(1_000_000_000.0 / targetFPS);
        
        while (running) {
            long currentTime = System.nanoTime();
            
            if (currentTime - lastFrameTime >= frameTimeNanos) {
                update();
                if (running) {
                    render();
                }
                lastFrameTime = currentTime;
            }
            
            renderer.pollEvents();
            
            if (renderer.shouldClose()) {
                running = false;
            }
            
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    /**
     * 更新游戏逻辑
     */
    private void update() {
        // 计算时间间隔
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastTime) / 1_000_000_000.0f; // 转换为秒
        lastTime = currentTime;
        
        // FPS统计：每1秒更新一次FPS值
        frameCount++;
        fpsUpdateTimer += deltaTime;
        if (fpsUpdateTimer >= 1.0f) { // 每秒计算一次FPS
            currentFPS = frameCount / fpsUpdateTimer;
            frameCount = 0;
            fpsUpdateTimer = 0.0f;
        }
        
        // 处理事件
        renderer.pollEvents();
        
        // 更新场景
        if (currentScene != null) {
            currentScene.update(deltaTime);
        }
        
        // 物理系统更新
        if (physicsSystem != null) {
            physicsSystem.update(deltaTime);
        }

        if (recordingService != null && recordingService.isRecording()) {
            recordingService.update(deltaTime, currentScene, inputManager);
        }
        
        // 更新输入
        inputManager.update();
        
        // 检查退出条件
        if (inputManager.isKeyPressed(256)) { // ESC键
            running = false;
            cleanup();
        }
        
        // 检查窗口是否关闭
        if (renderer.shouldClose()) {
            running = false;
            cleanup();
        }
    }
    
    /**
     * 渲染游戏
     */
    private void render() {
        renderer.beginFrame();
        
        // 渲染场景
        if (currentScene != null) {
            currentScene.render();
        }
        
        renderer.endFrame();
    }

    // 添加获取当前FPS的方法
    public float getCurrentFPS() {
        return currentFPS;
    }
    
    /**
     * 设置当前场景
     */
    public void setScene(Scene scene) {
        if (currentScene != null) {
            if (physicsSystem != null) {
                physicsSystem.cleanup();
                physicsSystem = null;
            }
            currentScene.clear();
        }
        this.currentScene = scene;
        if (scene != null) {
            if (running) {
                scene.initialize();
                if (!scene.getName().equals("MainMenu") && !scene.getName().equals("Replay")) {
                    physicsSystem = new PhysicsSystem(scene, renderer.getWidth(), renderer.getHeight());
                }
            }
        }
    }
    
    /**
     * 获取当前场景
     */
    public Scene getCurrentScene() {
        return currentScene;
    }
    
    /**
     * 停止游戏引擎
     */
    public void stop() {
        running = false;
        if (gameTimer != null) {
            gameTimer.stop();
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (recordingService != null && recordingService.isRecording()) {
            try { recordingService.stop(); } catch (Exception ignored) {}
        }
        if (physicsSystem != null) {
            physicsSystem.cleanup();
        }
        if (currentScene != null) {
            currentScene.clear();
        }
        renderer.cleanup();
    }

    public void enableRecording(RecordingService service) {
        this.recordingService = service;
        try {
            if (service != null && currentScene != null) {
                service.start(currentScene, renderer.getWidth(), renderer.getHeight());
            }
        } catch (Exception e) {
            System.err.println("录制启动失败: " + e.getMessage());
        }
    }

    public void disableRecording() {
        if (recordingService != null && recordingService.isRecording()) {
            try { recordingService.stop(); } catch (Exception ignored) {}
        }
        recordingService = null;
    }
    
    /**
     * 获取渲染器
     */
    public IRenderer getRenderer() {
        return renderer;
    }
    
    /**
     * 获取输入管理器
     */
    public InputManager getInputManager() {
        return inputManager;
    }
    
    /**
     * 获取时间间隔
     */
    public float getDeltaTime() {
        return deltaTime;
    }
    
    /**
     * 设置目标帧率
     */
    public void setTargetFPS(float fps) {
        this.targetFPS = fps;
        if (gameTimer != null) {
            gameTimer.setDelay((int) (1000 / fps));
        }
    }
    
    /**
     * 获取目标帧率
     */
    public float getTargetFPS() {
        return targetFPS;
    }
    
    /**
     * 检查引擎是否正在运行
     */
    public boolean isRunning() {
        return running;
    }
}
