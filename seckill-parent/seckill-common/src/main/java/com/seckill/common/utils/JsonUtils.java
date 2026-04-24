package com.seckill.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * JSON 工具类
 * 基于 Fastjson2 封装
 *
 * @author seckill
 */
@Slf4j
public class JsonUtils {

    /**
     * 对象转 JSON 字符串
     *
     * @param obj 对象
     * @return JSON 字符串
     */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            log.error("对象转 JSON 失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * JSON 字符串转对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类
     * @param <T>   泛型
     * @return 对象
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.error("JSON 转对象失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字符串转对象（支持泛型）
     *
     * @param json JSON 字符串
     * @param type TypeReference
     * @param <T>  泛型
     * @return 对象
     */
    public static <T> T parseObject(String json, TypeReference<T> type) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, type);
        } catch (Exception e) {
            log.error("JSON 转对象失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字符串转 List
     *
     * @param json  JSON 字符串
     * @param clazz 元素类型
     * @param <T>   泛型
     * @return List
     */
    public static <T> List<T> parseList(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseArray(json, clazz);
        } catch (Exception e) {
            log.error("JSON 转 List 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字符串转 Map
     *
     * @param json JSON 字符串
     * @return Map
     */
    public static Map<String, Object> parseMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("JSON 转 Map 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 对象转 JSONObject
     *
     * @param obj 对象
     * @return JSONObject
     */
    public static JSONObject toJsonObject(Object obj) {
        if (obj == null) {
            return new JSONObject();
        }
        try {
            return (JSONObject) JSON.toJSON(obj);
        } catch (Exception e) {
            log.error("对象转 JSONObject 失败: {}", e.getMessage());
            return new JSONObject();
        }
    }

    /**
     * 对象转 JSONArray
     *
     * @param obj 对象
     * @return JSONArray
     */
    public static JSONArray toJsonArray(Object obj) {
        if (obj == null) {
            return new JSONArray();
        }
        try {
            return (JSONArray) JSON.toJSON(obj);
        } catch (Exception e) {
            log.error("对象转 JSONArray 失败: {}", e.getMessage());
            return new JSONArray();
        }
    }

    /**
     * 格式化 JSON 字符串（美化输出）
     *
     * @param json JSON 字符串
     * @return 格式化后的 JSON 字符串
     */
    public static String formatJson(String json) {
        if (json == null || json.isEmpty()) {
            return "";
        }
        try {
            Object obj = JSON.parse(json);
            return JSON.toJSONString(obj, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);
        } catch (Exception e) {
            log.error("格式化 JSON 失败: {}", e.getMessage());
            return json;
        }
    }

    /**
     * 判断字符串是否为有效的 JSON
     *
     * @param json JSON 字符串
     * @return 是否有效
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            JSON.parse(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
