package entity;
import java.util.*;
/**
 * @Date: 2023/3/17 23:43
 * @Description: 课表
 */
public class Curriculum {
    // 周几
    int dayOfTheWeek;
    // 某一天的上课时间, 78节上课
    int startTime;
    int endTime;
    String courseName;

    // 上几周课
    List<Integer> weeks = new ArrayList<>();

    public Curriculum(String courseName, int dayOfTheWeek, int startTime, int endTime, List<Integer> weeks) {
        this.courseName = courseName;
        this.dayOfTheWeek = dayOfTheWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.weeks = weeks;
    }

    public List<Integer> getWeeks() {
        return weeks;
    }

    public int getDayOfTheWeek() {
        return dayOfTheWeek;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public String getCourseName() {
        return courseName;
    }
}
