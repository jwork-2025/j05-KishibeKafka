package com.gameengine.recording;

import com.gameengine.components.RenderComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameObject;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class RecordingService {
    private class EntityInfo {
        int id=0;
        int status=0;
        float x=0, y=0;
        float w=0, h=0;
        String image="";
        float angle=0;
    }


    private final RecordingConfig config;
    private final BlockingQueue<String> lineQueue; // 阻塞队列
    private volatile boolean recording; // volatile
    private Thread writerThread;
    private RecordingStorage storage = new FileRecordingStorage();
    private double elapsed;
    private double keyframeElapsed;
    private final double warmupSec = 0.1; // 等待一帧让场景对象完成初始化
    private final DecimalFormat qfmt;
    private Scene lastScene;

    private int playerBufferSize;
    private int enemyBufferSize;
    private int fireballBufferSize;
    private List<EntityInfo> playerInfoList;
    private List<EntityInfo> enemyInfoList;
    private List<EntityInfo> fireballInfoList;

    

    public RecordingService(RecordingConfig config) {
        this.config = config;
        this.lineQueue = new ArrayBlockingQueue<>(config.queueCapacity);
        this.recording = false;
        this.elapsed = 0.0;
        this.keyframeElapsed = 0.0;
        this.qfmt = new DecimalFormat();
        this.qfmt.setMaximumFractionDigits(Math.max(0, config.quantizeDecimals));
        this.qfmt.setGroupingUsed(false);
        this.playerBufferSize = 1;
        this.enemyBufferSize = 100;
        this.fireballBufferSize = 100;
        this.enemyInfoList = new ArrayList<>(enemyBufferSize);
        this.fireballInfoList = new ArrayList<>(fireballBufferSize);
        this.playerInfoList = new ArrayList<>(playerBufferSize);
        for (int i = 0; i < enemyBufferSize; ++i){
            EntityInfo ei = new EntityInfo();
            ei.id = i;
            ei.status = 0;
            enemyInfoList.add(ei);
        }
        for (int i = 0; i < fireballBufferSize; ++i){
            EntityInfo ei = new EntityInfo();
            ei.id = i;
            ei.status = 0;
            fireballInfoList.add(ei);
        }
        for (int i = 0; i < playerBufferSize; ++i){
            EntityInfo ei = new EntityInfo();
            ei.id = i;
            ei.status = 0;
            playerInfoList.add(ei);
        }
    }

    public boolean isRecording() {
        return recording;
    }

    public void start(Scene scene, int width, int height) throws IOException {
        if (recording) return;
        storage.openWriter(config.outputPath);
        writerThread = new Thread(() -> {
            try {
                while (recording || !lineQueue.isEmpty()) {
                    String s = lineQueue.poll();
                    if (s == null) {
                        try { Thread.sleep(2); } catch (InterruptedException ignored) {}
                        continue;
                    }
                    storage.writeLine(s);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { storage.closeWriter(); } catch (Exception ignored) {}
            }
        }, "record-writer");
        recording = true;
        writerThread.start();

        // header
        enqueue("{\"type\":\"header\",\"version\":1,\"w\":" + width + ",\"h\":" + height + "}");
        keyframeElapsed = 0.0;
    }

    public void stop() {
        if (!recording) return;
        try {
            if (lastScene != null) {
                writeKeyframe(lastScene);
            }
        } catch (Exception ignored) {}
        recording = false;
        try { writerThread.join(500); } catch (InterruptedException ignored) {}
    }

    public void update(double deltaTime, Scene scene, InputManager input) {
        if (!recording) return;
        elapsed += deltaTime;
        keyframeElapsed += deltaTime;
        lastScene = scene;
        // Set<Integer> just = input.getJustPressedKeysSnapshot();
        // if (!just.isEmpty()) {
        //     StringBuilder sb = new StringBuilder();
        //     sb.append("{\"type\":\"input\",\"t\":").append(qfmt.format(elapsed)).append(",\"keys\":[");
        //     boolean first = true;
        //     for (Integer k : just) {
        //         if (!first) sb.append(',');
        //         sb.append(k);
        //         first = false;
        //     }
        //     sb.append("]}");
        //     enqueue(sb.toString());
        // }

        // periodic keyframe（跳过开头暖机，避免空关键帧）
        if (elapsed >= warmupSec && keyframeElapsed >= config.keyframeIntervalSec) {
            if (writeKeyframe(scene)) {
                keyframeElapsed = 0.0;
            }
        }
    }

    private List<GameObject> getPlayers(Scene scene) {
        return scene.getGameObjects().stream()
            .filter(obj -> obj.getName().equals("Player"))
            .filter(obj -> obj.isActive())
            .collect(Collectors.toList());
    }
    
    private List<GameObject> getEnemies(Scene scene) {
        return scene.getGameObjects().stream()
            .filter(obj -> obj.getName().equals("Enemy"))
            .filter(obj -> obj.isActive())
            .collect(Collectors.toList());
    }
    
    private List<GameObject> getFireballs(Scene scene) {
        return scene.getGameObjects().stream()
        .filter(obj -> obj.getName().equals("Fireball"))
        .filter(obj -> obj.isActive())
        .collect(Collectors.toList());
    }
    
    private boolean writeKeyframe(Scene scene) {
        int count = 0;
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        // 写player信息
        List<GameObject> players = getPlayers(scene);
        for (GameObject player : players){
            if (player == null) continue;
            int id = player.getId();
            TransformComponent transform = player.getComponent(TransformComponent.class);
            RenderComponent render = player.getComponent(com.gameengine.components.RenderComponent.class);
            if (transform == null || render == null) continue;
            EntityInfo ei = playerInfoList.get(id % playerBufferSize);
            ei.id = id;
            ei.status = 1;
            ei.x = transform.getPosition().x;
            ei.y = transform.getPosition().y;
            ei.w = transform.getScale().x;
            ei.h = transform.getScale().y;
            ei.image = render.getImageKey();
            ei.angle = render.getRotation();
        }
        sb.append("{\"type\":\"keyframe\",\"t\":").append(qfmt.format(elapsed)).append(",\"players\":[");
        for (int i = 0; i < playerBufferSize; ++i){
            EntityInfo ei = playerInfoList.get(i);
            if (!first) sb.append(',');
            sb.append('{')
            .append("\"id\":").append(qfmt.format(ei.id)).append(",")
            .append("\"status\":1,")
            .append("\"x\":").append(qfmt.format(ei.x)).append(',')
            .append("\"y\":").append(qfmt.format(ei.y)).append(',')
            .append("\"w\":").append(qfmt.format(ei.w)).append(',')
            .append("\"h\":").append(qfmt.format(ei.h)).append(',')
            .append("\"image\":\"").append(ei.image).append("\",")
            .append("\"angle\":").append(qfmt.format(ei.angle))
            .append('}');
            first = true;
            count++;
        }
        // 写enemy信息
        List<GameObject> enemies = getEnemies(scene);
        for (GameObject enemy : enemies){
            if (enemy == null) continue;
            int id = enemy.getId();
            TransformComponent transform = enemy.getComponent(TransformComponent.class);
            RenderComponent render = enemy.getComponent(com.gameengine.components.RenderComponent.class);
            if (transform == null || render == null) continue;
            EntityInfo ei = enemyInfoList.get(id % enemyBufferSize);
            ei.id = id;
            ei.status = 1;
            ei.x = transform.getPosition().x;
            ei.y = transform.getPosition().y;
            ei.w = transform.getScale().x;
            ei.h = transform.getScale().y;
            ei.image = render.getImageKey();
            ei.angle = render.getRotation();
        }
        sb.append("],\"enemies\":[");
        first = true;
        for (int i = 0; i < enemyBufferSize; ++i){
            EntityInfo ei = enemyInfoList.get(i);
            if (!first) sb.append(',');
            sb.append('{')
            .append("\"id\":").append(qfmt.format(ei.id)).append(",")
            .append("\"status\":").append(qfmt.format(ei.status)).append(",")
            .append("\"x\":").append(qfmt.format(ei.x)).append(',')
            .append("\"y\":").append(qfmt.format(ei.y)).append(',')
            .append("\"w\":").append(qfmt.format(ei.w)).append(',')
            .append("\"h\":").append(qfmt.format(ei.h)).append(',')
            .append("\"image\":\"").append(ei.image).append("\",")
            .append("\"angle\":").append(qfmt.format(ei.angle))
            .append('}');
            first = false;
            if (ei.status == 1)
                count++;
        }
        // 写fireball信息
        List<GameObject> fireballs = getFireballs(scene);
        for (GameObject fireball : fireballs){
            if (fireball == null) continue;
            int id = fireball.getId();
            TransformComponent transform = fireball.getComponent(TransformComponent.class);
            RenderComponent render = fireball.getComponent(com.gameengine.components.RenderComponent.class);
            if (transform == null || render == null) continue;
            EntityInfo ei = fireballInfoList.get(id % fireballBufferSize);
            ei.id = id;
            ei.status = 1;
            ei.x = transform.getPosition().x;
            ei.y = transform.getPosition().y;
            ei.w = transform.getScale().x;
            ei.h = transform.getScale().y;
            ei.image = render.getImageKey();
            ei.angle = render.getRotation();
        }
        sb.append("],\"fireballs\":[");
        first = true;
        for (int i = 0; i < fireballBufferSize; ++i){
            EntityInfo ei = fireballInfoList.get(i);
            if (!first) sb.append(',');
            sb.append('{')
            .append("\"id\":").append(qfmt.format(ei.id)).append(",")
            .append("\"status\":").append(qfmt.format(ei.status)).append(",")
            .append("\"x\":").append(qfmt.format(ei.x)).append(',')
            .append("\"y\":").append(qfmt.format(ei.y)).append(',')
            .append("\"w\":").append(qfmt.format(ei.w)).append(',')
            .append("\"h\":").append(qfmt.format(ei.h)).append(',')
            .append("\"image\":\"").append(ei.image).append("\",")
            .append("\"angle\":").append(qfmt.format(ei.angle))
            .append('}');
            first = false;
            if (ei.status == 1)
                count++;
        }
        sb.append("]}");
        if (count == 0) return false;
        enqueue(sb.toString());
        return true;
    }

    private void enqueue(String line) {
        if (!lineQueue.offer(line)) {
            // 简单丢弃策略：队列满时丢弃低优先级数据（此处直接丢弃）
        }
    }
}


