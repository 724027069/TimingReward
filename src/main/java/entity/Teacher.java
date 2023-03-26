package entity;

import java.util.*;
/**
 * @Date: 2023/3/17 23:40
 * @Description: 教师类
 */
public class Teacher {
    String id;
    String name;
    // 课表，第几周、周几、当日时间
    List<Curriculum> curriculums = new ArrayList<>();

    public Teacher(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Curriculum> getCurriculums() {
        return curriculums;
    }
}
