package com.gameengine.core;

import com.gameengine.components.TransformComponent;
import com.gameengine.components.ColliderComponent;
import com.gameengine.components.FireballComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.core.GameObject;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 游戏逻辑类，处理具体的游戏规则
 */
public class GameLogic {
    private Scene scene;
    private int score;
    private boolean gameOver;
    private ExecutorService physicsExecutor;
    
    public GameLogic(Scene scene) {
        this.scene = scene;
        this.score = 0;
        this.gameOver = false;
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1); // 20 - 1
        this.physicsExecutor = Executors.newFixedThreadPool(threadCount);
    }

    public boolean isGameOver() {
        return gameOver;
    }

    private GameObject getPlayer() {
        for (GameObject obj : scene.getGameObjects()) {
            if (obj.getName().equals("Player")) {
                return obj;
            }
        }
        return null;
    }
    
    private List<GameObject> getEnemies() {
        return scene.getGameObjects().stream()
            .filter(obj -> obj.getName().equals("Enemy"))
            .filter(obj -> obj.isActive())
            .collect(Collectors.toList());
    }

    private List<GameObject> getFireballs() {
        return scene.getGameObjects().stream()
            .filter(obj -> obj.getName().equals("Fireball"))
            .filter(obj -> obj.isActive())
            .collect(Collectors.toList());
    }

    public void checkCollisions() {
        // 直接查找玩家对象
        GameObject player = getPlayer();
        if (player == null) return;
        ColliderComponent playerCollider = player.getComponent(ColliderComponent.class);
        if (playerCollider == null || !playerCollider.isEnabled()) return;
        
        // 获取敌人list
        List<GameObject> enemies = getEnemies();
        // 获取火球list
        List<GameObject> fireballs = getFireballs();
        int threadCount = Runtime.getRuntime().availableProcessors() - 1;
        threadCount = Math.max(2, threadCount);
        int batchSize = Math.max(1, enemies.size() / threadCount + 1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < enemies.size(); i += batchSize){
            final int start = i;
            final int end = Math.min(i + batchSize, enemies.size());
            
            Future<?> future = physicsExecutor.submit(() -> {
                for (int j = start; j < end; j++) {
                    GameObject obj = enemies.get(j);
                    if (playerCollider.collideWith(obj)) {
                        // 碰撞！结束游戏
                        setScore(0);
                        gameOver = true;
                        return;
                    }
                    for (GameObject fireball : fireballs){
                        ColliderComponent fireballCollider = fireball.getComponent(ColliderComponent.class);
                        if (fireballCollider.collideWith(obj)){
                            scene.removeGameObject(obj);
                            scene.removeGameObject(fireball);
                            setScore(score + 1);
                            break;
                        }
                    }
                    }
            });
            
            futures.add(future);
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void cleanup() {
        if (physicsExecutor != null && !physicsExecutor.isShutdown()) {
            physicsExecutor.shutdown();
            try {
                if (!physicsExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    physicsExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                physicsExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
