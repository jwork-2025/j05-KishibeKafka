package com.gameengine.example;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.gameengine.components.ColliderComponent;
import com.gameengine.components.EnemyController;
import com.gameengine.components.FireballComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.PlayerController;
import com.gameengine.components.RenderComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameLogic;
import com.gameengine.core.GameObject;
import com.gameengine.core.SpriteLoader;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

public class GameScene extends Scene {
    private final GameEngine engine;
    private IRenderer renderer;
    private Random random;
    private SpriteLoader spriteLoader = SpriteLoader.getInstance();
    private float time;
    private GameLogic gameLogic;
    private boolean waitingReturn;
    private float waitInputTimer;
    private float freezeTimer;
    private final float inputCooldown = 0.25f;
    private final float freezeDelay = 0.20f;

    private int playerIdTop = 0;
    private int enemyIdTop = 0;

    public GameScene(GameEngine engine) {
        super("GameScene");
        this.engine = engine;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.random = new Random();
        this.time = 0;
        this.gameLogic = new GameLogic(this);
        
        // 创建游戏对象
        createPlayer();
        createEnemies(3);
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        time += deltaTime;
        
        boolean wasGameOver = gameLogic.isGameOver();
        gameLogic.checkCollisions();
        if (gameLogic.isGameOver() && !wasGameOver) {
            waitingReturn = true;
            waitInputTimer = 0f;
            freezeTimer = 0f;
        }

        if (waitingReturn) {
            waitInputTimer += deltaTime;
            freezeTimer += deltaTime;
        }

        if (waitingReturn && waitInputTimer >= inputCooldown && (engine.getInputManager().isAnyKeyJustPressed())) {
            MenuScene menu = new MenuScene(engine, "MainMenu");
            engine.setScene(menu);
            return;
        }

        
        // 生成新敌人
        if (time > 2.0f && !gameLogic.isGameOver()) {
            createEnemies(3);
            time = 0;
        }
    }
    
    @Override
    public void render() {
        // 绘制背景
        // renderer.drawRect(0, 0, 800, 600, 0.1f, 0.1f, 0.2f, 1.0f);
        renderer.drawImage(0, 0, renderer.getWidth(), renderer.getHeight(), spriteLoader.GetImageByName("BackgroundImage"));
        // fenshu 
        renderer.drawText(10, 20, "Score: " + gameLogic.getScore(), 1, 1, 1, 1);
        renderer.drawText(10, 50, "FPS: " + engine.getCurrentFPS(), 1, 1, 1, 1);
        
        if (gameLogic.isGameOver()) {
            float cx = renderer.getWidth() / 2.0f;
            float cy = renderer.getHeight() / 2.0f;
            renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.0f, 0.0f, 0.0f, 0.35f);
            renderer.drawRect(cx - 200, cy - 60, 400, 120, 0.0f, 0.0f, 0.0f, 0.7f);
            renderer.drawText(cx - 100, cy - 10, "GAME OVER", 1.0f, 1.0f, 1.0f, 1.0f);
            renderer.drawText(cx - 180, cy + 30, "PRESS ANY KEY TO RETURN", 0.8f, 0.8f, 0.8f, 1.0f);
        }

        // 渲染所有对象
        super.render();
    }

    private GameObject player;
    
    private void createPlayer() {
        // 创建葫芦娃
        player = new GameObject("Player") {
            private int facingDirection = 1;
            
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
                // 处理转向
                facingDirection = parseFlip(facingDirection, this);
            }
            
        };
        player.setId(playerIdTop);
        playerIdTop++;
        
        // 添加变换组件
        TransformComponent transform = player.addComponent(new TransformComponent(new Vector2(960, 540)));
        transform.setScale(new Vector2(40, 60));

        // 添加物理组件
        PhysicsComponent physics = player.addComponent(new PhysicsComponent(1.0f));
        physics.setFriction(0.95f);

        // 添加渲染组件
        RenderComponent render = player.addComponent(new RenderComponent(
            RenderComponent.RenderType.IMAGE, 
            transform.getScale(), 
            "PlayerImage"));
        render.setRenderer(renderer);

        // 添加碰撞组件
        ColliderComponent collider = player.addComponent(new ColliderComponent(
            ColliderComponent.ColliderType.BOX, 
            20, 
            40, 
            new Vector2(0, 5)));
        collider.setShowBound(false);
        collider.setRenderer(renderer);

        // 添加火球组件
        FireballComponent fireball = player.addComponent(new FireballComponent(0.3f,300));
        fireball.setScene(this);
        fireball.setRenderer(renderer);

        // 添加控制组件
        player.addComponent(new PlayerController(player, gameLogic));

        addGameObject(player);
    }
    
    private void createEnemies(int num) {
        for (int i = 0; i < num; ++i){
            createEnemy();
        }
    }
    
    // 创建敌人
    private void createEnemy() {
        GameObject enemy = new GameObject("Enemy") {
            private int facingDirection = 1;
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
                facingDirection = parseFlip(facingDirection, this);
            }
            
            @Override
            public void render() {
                renderComponents();
            }
        };
        enemy.setId(enemyIdTop);
        enemyIdTop++;
        
        // 随机位置
        Vector2 position;
        position = new Vector2(
            random.nextFloat() * renderer.getWidth(),
            random.nextFloat() * renderer.getHeight()
        );
        
        // 添加变换组件
        TransformComponent transform = enemy.addComponent(new TransformComponent(position));
        transform.setScale(new Vector2(60, 80));

        // 添加渲染组件
        RenderComponent render = enemy.addComponent(new RenderComponent(
            RenderComponent.RenderType.IMAGE, 
            transform.getScale(), 
            "EnemyImage"));
        render.setRenderer(renderer);
        
        // 添加物理组件
        PhysicsComponent physics = enemy.addComponent(new PhysicsComponent(0.5f));
        physics.setFriction(0.98f);

        // 添加碰撞组件
        ColliderComponent collider = enemy.addComponent(new ColliderComponent(
            ColliderComponent.ColliderType.BOX, 
            22,
            55,
            new Vector2(0, 15)));
        collider.setShowBound(false);
        collider.setRenderer(renderer);

        enemy.addComponent(new EnemyController(enemy, player, 50, gameLogic));

        addGameObject(enemy);
    }
    
    // 翻转方向
    public int parseFlip(int facingDirection, GameObject obj){
        if (!obj.hasComponent(PhysicsComponent.class)) return facingDirection;
        PhysicsComponent physics = obj.getComponent(PhysicsComponent.class);
        if ((physics.getVelocity().x > 0 && facingDirection < 0)
            || physics.getVelocity().x < 0 && facingDirection > 0){
            facingDirection *= -1;
            if (!obj.hasComponent(RenderComponent.class)) return facingDirection;
            RenderComponent render = obj.getComponent(RenderComponent.class);
            if (obj.getName() == "Enemy"){
                if (facingDirection > 0){
                    render.setImageKey("EnemyImage");
                    render.setImage(spriteLoader.GetImageByName(render.getImageKey()));
                }
                else{
                    render.setImageKey("FlippedEnemyImage");
                    render.setImage(spriteLoader.GetImageByName(render.getImageKey()));
                }
            } else if (obj.getName() == "Player")
            {
                if (facingDirection > 0){
                    render.setImageKey("PlayerImage");
                    render.setImage(spriteLoader.GetImageByName(render.getImageKey()));
                }
                else{
                    render.setImageKey("FlippedPlayerImage");
                    render.setImage(spriteLoader.GetImageByName(render.getImageKey()));
                }
            }
            return facingDirection;
        } else return facingDirection;
    }
    
    @Override
    public void clear() {
        if (gameLogic != null) {
            gameLogic.cleanup();
        }
        super.clear();
    }
}
