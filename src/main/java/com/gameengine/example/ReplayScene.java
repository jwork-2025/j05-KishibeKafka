package com.gameengine.example;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.gameengine.components.RenderComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameObject;
import com.gameengine.core.SpriteLoader;
import com.gameengine.graphics.IRenderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.recording.FileRecordingStorage;
import com.gameengine.recording.RecordingJson;
import com.gameengine.recording.RecordingStorage;
import com.gameengine.scene.Scene;

public class ReplayScene extends Scene {
    private final GameEngine engine;
    private String recordingPath;
    private IRenderer renderer;
    private InputManager input;
    private SpriteLoader spriteLoader;
    private float time;
    private boolean DEBUG_REPLAY = false;
    private float debugAccumulator = 0f;

    private static class Keyframe {
        static class EntityInfo {
            int id;
            int status;
            float x, y;
            float w, h;
            String image;
            float angle;
        }
        double t;
        List<EntityInfo> players = new ArrayList<>();
        List<EntityInfo> enemies = new ArrayList<>();
        List<EntityInfo> fireballs = new ArrayList<>();
    }

    private final List<Keyframe> keyframes = new ArrayList<>();
    private final List<GameObject> playerList = new ArrayList<>();
    private final List<GameObject> enemyList = new ArrayList<>();
    private final List<GameObject> fireballList = new ArrayList<>();

    public ReplayScene(GameEngine engine, String path) {
        super("Replay");
        this.engine = engine;
        this.recordingPath = path;
        this.spriteLoader = SpriteLoader.getInstance();
    }

    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.input = engine.getInputManager();
        // 重置状态，防止从列表进入后残留
        this.time = 0f;
        this.keyframes.clear();
        this.playerList.clear();
        this.enemyList.clear();
        this.fireballList.clear();
        if (recordingPath != null) {
            loadRecording(recordingPath);
            buildObjectsFromFirstKeyframe();
        } else {
            // 仅进入文件选择模式
            this.recordingFiles = null;
            this.selectedIndex = 0;
        }
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        if (input.isKeyJustPressed(259)) { // BACK
            engine.setScene(new MenuScene(engine, "MainMenu"));
            return;
        }
        // 文件选择模式
        if (recordingPath == null) {
            handleFileSelection();
            return;
        }

        if (keyframes.size() < 1) return;
        time += deltaTime;
        // 限制在最后关键帧处停止（也可选择循环播放）
        double lastT = keyframes.get(keyframes.size() - 1).t;
        if (time > lastT) {
            time = (float)lastT;
        }

        // 查找区间
        Keyframe a = keyframes.get(0);
        Keyframe b = keyframes.get(keyframes.size() - 1);
        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe k1 = keyframes.get(i);
            Keyframe k2 = keyframes.get(i + 1);
            if (time >= k1.t && time <= k2.t) { a = k1; b = k2; break; }
        }
        double span = Math.max(1e-6, b.t - a.t);
        double u = Math.min(1.0, Math.max(0.0, (time - a.t) / span));
        // 调试输出节流
        updateInterpolatedPositions(a, b, (float)u);
    }

    @Override
    public void render() {
        renderer.drawImage(0, 0, renderer.getWidth(), renderer.getHeight(), spriteLoader.GetImageByName("BackgroundImage"));
        if (recordingPath == null) {
            renderFileList();
            return;
        }
        // 基于 Transform 手动绘制（回放对象没有附带 RenderComponent）
        super.render();
        String hint = "REPLAY: Backspace to return";
        float w = hint.length() * 12.0f;
        renderer.drawText(renderer.getWidth()/2.0f - w/2.0f, 30, hint, 0.8f, 0.8f, 0.8f, 1.0f);
    }

    private void loadRecording(String path) {
        keyframes.clear();
        RecordingStorage storage = new FileRecordingStorage();
        try {
            for (String line : storage.readLines(path)) {
                if (line.contains("\"type\":\"keyframe\"")) { // 读取关键帧
                    Keyframe kf = new Keyframe();
                    kf.t = RecordingJson.parseDouble(RecordingJson.field(line, "t"));
                    parseEntityArrayToTarget(line, "players", kf.players);
                    parseEntityArrayToTarget(line, "enemies", kf.enemies);
                    parseEntityArrayToTarget(line, "fireballs", kf.fireballs);
                    keyframes.add(kf);
                }
            }
        } catch (Exception e) {
            System.err.println("解析录制文件失败：" + path);
            e.printStackTrace();
        }
        keyframes.sort(Comparator.comparingDouble(k -> k.t));
    }

    private void parseEntityArrayToTarget(String line, String arrayKey, List<Keyframe.EntityInfo> targetList) {
        // 1. 找到数组的起始位置（格式："arrayKey":[...）
        int arrayStartMarkerIndex = line.indexOf("\"" + arrayKey + "\":[");
        if (arrayStartMarkerIndex < 0) {
            return; // 该数组不存在，直接返回
        }
        
        // 2. 提取数组内容（从 '[' 开始到对应的 ']' 结束）
        int bracketStartIndex = line.indexOf('[', arrayStartMarkerIndex);
        if (bracketStartIndex < 0) {
            return;
        }
        String arrayContent = RecordingJson.extractArray(line, bracketStartIndex);
        if (arrayContent.trim().isEmpty()) {
            return;
        }
        
        // 3. 分割数组的顶层实体（不拆分嵌套对象的逗号）
        String[] entityJsonParts = RecordingJson.splitTopLevel(arrayContent);
        for (String entityPart : entityJsonParts) {
            if (entityPart.trim().isEmpty()) {
                continue;
            }
            
            // 4. 解析单个实体信息，存入目标列表
            Keyframe.EntityInfo ei = parseSingleEntity(entityPart);
            targetList.add(ei);
        }
    }

    private Keyframe.EntityInfo parseSingleEntity(String entityJsonPart) {
        Keyframe.EntityInfo ei = new Keyframe.EntityInfo();
        try {
            ei.id = (int)RecordingJson.parseDouble(RecordingJson.field(entityJsonPart, "id"));
            ei.status = (int)RecordingJson.parseDouble(RecordingJson.field(entityJsonPart, "status"));
            ei.x = (float)RecordingJson.parseDouble(RecordingJson.field(entityJsonPart, "x"));
            ei.y = (float)RecordingJson.parseDouble(RecordingJson.field(entityJsonPart, "y"));
            ei.w = (float)RecordingJson.parseDouble(RecordingJson.field(entityJsonPart, "w"));
            ei.h = (float)RecordingJson.parseDouble(RecordingJson.field(entityJsonPart, "h"));
            ei.image = RecordingJson.stripQuotes(RecordingJson.field(entityJsonPart, "image"));
            ei.angle = (float)RecordingJson.parseDouble(RecordingJson.field(entityJsonPart, "angle"));
            // System.err.println("id:"+ ei.id + " status:"+ei.status+" x:"+ei.x+" y:"+ei.y+" w:"+ei.w+" h:"+ei.h+" image:"+ei.image);
            return ei;
        } catch (Exception e) {
            System.err.println("解析实体失败：" + entityJsonPart);
            e.printStackTrace();
            return null;
        }
    }

    private void buildObjectsFromFirstKeyframe() {
        if (keyframes.isEmpty()) return;
        Keyframe kf0 = keyframes.get(0);
        playerList.clear();
        enemyList.clear();
        fireballList.clear();
        clear();
        for (int i = 0; i < kf0.players.size(); i++) {
            if (kf0.players.get(i).status == 1){
                GameObject obj = buildObjectFromEntity("Player", kf0.players.get(i), i);
                addGameObject(obj);
                playerList.add(obj);
            }
        }
        for (int i = 0; i < kf0.enemies.size(); i++) {
            if (kf0.enemies.get(i).status == 1){
                GameObject obj = buildObjectFromEntity("Enemy", kf0.enemies.get(i), i);
                addGameObject(obj);
                enemyList.add(obj);
            }
        }
        for (int i = 0; i < kf0.fireballs.size(); i++) {
            if (kf0.fireballs.get(i).status == 1){
                GameObject obj = buildObjectFromEntity("Fireball", kf0.fireballs.get(i), i);
                addGameObject(obj);
                fireballList.add(obj);
            }
        }
        time = 0f;
    }
    
    private void updateInterpolatedPositions(Keyframe a, Keyframe b, float u) {
        updateObjectsForEntity(a.players, b.players, u, playerList, "Player");
        updateObjectsForEntity(a.enemies, b.enemies, u, enemyList, "Enemy");
        updateObjectsForEntity(a.fireballs, b.fireballs, u, fireballList, "Fireball");
    }

    private GameObject findObjectWithId(List<GameObject> objList, int id){
        return objList.stream().filter(obj -> obj.getId() == id).findFirst().orElse(null);
    }

    private void ensureEntityNum(List<Keyframe.EntityInfo> a, List<GameObject> objList, String name)
    {
        int n = a.size();
        for (int i = 0; i < n; ++i){
            Keyframe.EntityInfo ei = a.get(i);
            GameObject obj = findObjectWithId(objList, ei.id);
            if (ei.status == 1){
                if (obj == null){
                    obj = new GameObject(name);
                    obj.setId(ei.id);
                    TransformComponent transform = obj.addComponent(new TransformComponent(new Vector2(ei.x,ei.y)));
                    transform.setScale(new Vector2(ei.w, ei.h));
                    RenderComponent render = obj.addComponent(new RenderComponent(
                        RenderComponent.RenderType.IMAGE,
                        transform.getScale(),
                        ei.image));
                    render.setRenderer(renderer);
                    render.setRotation(ei.angle);
                    addGameObject(obj);
                    objList.add(obj);
                }
            } else{
                if (obj != null){
                    removeGameObject(obj);
                    objList.remove(obj);
                }
            }
        }
    }

    private void updateObjectsForEntity(List<Keyframe.EntityInfo> a, List<Keyframe.EntityInfo> b, float u, List<GameObject> objList, String name){
        int n = a.size();
        ensureEntityNum(a, objList, name);
        for (int i = 0; i < n; ++i){
            Keyframe.EntityInfo eia = a.get(i);
            Keyframe.EntityInfo eib = b.get(i);
            if (eia.status == 1 && eib.status == 1){
                if (eia.id == eib.id){
                    float x = (float)((1.0 - u) * eia.x + u * eib.x);
                    float y = (float)((1.0 - u) * eia.y + u * eib.y);
                    GameObject obj = findObjectWithId(objList, eia.id);
                    if (obj == null) continue;
                    TransformComponent tc = obj.getComponent(TransformComponent.class);
                    RenderComponent render = obj.getComponent(RenderComponent.class);
                    if (tc != null) tc.setPosition(new Vector2(x, y));
                    if (render != null){
                        render.setImageKey(eib.image);
                        render.setImage(spriteLoader.GetImageByName(eib.image));
                    }
                }
            }
        }
    }

    private GameObject buildObjectFromEntity(String name, Keyframe.EntityInfo ei, int index) {
        GameObject obj = new GameObject(name);
        obj.setId(ei.id);
        TransformComponent transform = obj.addComponent(new TransformComponent(new Vector2(ei.x,ei.y)));
        transform.setScale(new Vector2(ei.w, ei.h));
        RenderComponent render = obj.addComponent(new RenderComponent(
            RenderComponent.RenderType.IMAGE,
            transform.getScale(),
            ei.image));
        render.setRenderer(renderer);
        render.setRotation(ei.angle);
        return obj;
    }

    // ========== 文件列表模式 ==========
    private List<File> recordingFiles;
    private int selectedIndex = 0;

    private void ensureFilesListed() {
        if (recordingFiles != null) return;
        com.gameengine.recording.RecordingStorage storage = new com.gameengine.recording.FileRecordingStorage();
        recordingFiles = storage.listRecordings();
    }

    private void handleFileSelection() {
        ensureFilesListed();
        if (input.isKeyJustPressed(38) || input.isKeyJustPressed(265)) { // up (AWT 38 / GLFW 265)
            selectedIndex = (selectedIndex - 1 + Math.max(1, recordingFiles.size())) % Math.max(1, recordingFiles.size());
        } else if (input.isKeyJustPressed(40) || input.isKeyJustPressed(264)) { // down (AWT 40 / GLFW 264)
            selectedIndex = (selectedIndex + 1) % Math.max(1, recordingFiles.size());
        } else if (input.isKeyJustPressed(10) || input.isKeyJustPressed(32) || input.isKeyJustPressed(257) || input.isKeyJustPressed(335)) { // enter/space (AWT 10/32, GLFW 257/335)
            if (recordingFiles.size() > 0) {
                String path = recordingFiles.get(selectedIndex).getAbsolutePath();
                this.recordingPath = path;
                clear();
                initialize();
            }
        } else if (input.isKeyJustPressed(259)) { //back
            engine.setScene(new MenuScene(engine, "MainMenu"));
        }
    }

    private void renderFileList() {
        ensureFilesListed();
        int w = renderer.getWidth();
        int h = renderer.getHeight();
        String title = "SELECT RECORDING";
        float tw = title.length() * 16f;
        renderer.drawText(w/2f - tw/2f, 80, title, 1f,1f,1f,1f);

        if (recordingFiles.isEmpty()) {
            String none = "NO RECORDINGS FOUND";
            float nw = none.length() * 14f;
            renderer.drawText(w/2f - nw/2f, h/2f, none, 0.9f,0.8f,0.2f,1f);
            String back = "BACKSPACE TO RETURN";
            float bw = back.length() * 12f;
            renderer.drawText(w/2f - bw/2f, h - 60, back, 0.7f,0.7f,0.7f,1f);
            return;
        }

        float startY = 140f;
        float itemH = 28f;
        for (int i = 0; i < recordingFiles.size(); i++) {
            String name = recordingFiles.get(i).getName();
            float x = 100f;
            float y = startY + i * itemH;
            if (i == selectedIndex) {
                renderer.drawRect(x - 10, y - 6, 600, 24, 0.3f,0.3f,0.4f,0.8f);
            }
            renderer.drawText(x, y, name, 0.9f,0.9f,0.9f,1f);
        }

        String hint = "UP/DOWN SELECT, ENTER PLAY, BACKSPACE RETURN";
        float hw = hint.length() * 12f;
        renderer.drawText(w/2f - hw/2f, h - 60, hint, 0.7f,0.7f,0.7f,1f);
    }
}
