import entity.ClockRecord;
import entity.Curriculum;
import entity.Teacher;
import util.Utils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Date: 2023/3/22 23:55
 * @Description: 计算打卡奖励
 */
public class Main {
    // 开学日期
    private static final String CommencementDateStr = "2023-02-13";
    private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat df_hour = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static Date commencementDate = null;
    private static String Valid1DateBegin = "08:30:00";
    private static String Valid1DateEnd = "12:00:00";
    private static String Valid2DateBegin = "13:30:00";
    private static String Valid2DateEnd = "17:00:00";
    private static String Valid3DateBegin = "18:00:00";
    private static String Valid3DateEnd = "21:30:00";
    private static String UN_VALID_MSG = "不满足3小时";

    static {
        try {
            commencementDate = df.parse(CommencementDateStr);
        } catch (ParseException e) {
            System.err.println("格式化 开学日期[" + CommencementDateStr + "] 失败.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // 获取所有教师的信息
        List<Teacher> teachers = Utils.readSchoolTimetable();
        // 获取打卡记录
        List<ClockRecord> clockRecords = Utils.readClockRecord();

        List<ClockRecord> commencementClockRecord = clockRecords
                .stream()
                .filter(it -> it.getDate().compareTo(CommencementDateStr) > 0)  // 过滤掉开学之前的
                .filter(it -> timeCompare(it.getBeginTime(), it.getEndTime()) >= 3) // 过滤掉 打卡记录不满足3小时的
                .collect(Collectors.toList());

        // 按天分组,并且升序排序
        TreeMap<String, List<ClockRecord>> dateClockRecordMap = commencementClockRecord
                .stream()
                .collect(Collectors.groupingBy(ClockRecord::getDate))
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1, TreeMap::new));

        dateClockRecordMap.forEach((date, clockRecordList) -> {
            processClockRecord(teachers, date, clockRecordList);
        });


        List<ClockRecord> records = new ArrayList<>();
        dateClockRecordMap.values().stream().forEach(clockRecordList -> {
            records.addAll(clockRecordList);
        });

        // 导出记录，生成excel
        Utils.export(records);
    }

    // 处理打卡记录，判断是否有效
    private static void processClockRecord(List<Teacher> teachers, String date, List<ClockRecord> clockRecordList) {
        // 计算第几周
        int dayWeek = computeDayWeek(date);
        // 计算周几
        int weekOfDate = getWeekOfDate(date);
        if (dayWeek > 0) {
            clockRecordList.stream().forEach(clockRecord -> {
                // 截取时分
                String beginHourMin = clockRecord.getBeginTime().split(" ")[1];
                String endHourMin = clockRecord.getEndTime().split(" ")[1];

                String computeBegin = "", computeEnd = "";
                if (endHourMin.compareTo(Valid2DateBegin) < 0) { // 判断8:30-12:00时间段是否有效
                    // 开始时间和8:30之间的较大者
                    computeBegin = beginHourMin.compareTo(Valid1DateBegin) > 0 ? beginHourMin : Valid1DateBegin;
                    // 结束时间和12:00之间的较小者
                    computeEnd = endHourMin.compareTo(Valid1DateEnd) > 0 ? Valid1DateEnd : endHourMin;
                    if (timeCompare(clockRecord.getDate() + " " + computeBegin, clockRecord.getDate() + " " + computeEnd) >= 3) {
                        processValidDate(teachers, dayWeek, weekOfDate, clockRecord, 1, 4, 0);
                    } else {
                        clockRecord.getIsValidNote()[0] = UN_VALID_MSG;
                    }
                } else if (endHourMin.compareTo(Valid3DateBegin) < 0) { // 判断8:30-12:00和13:30-17:00 2个时间段是否有效
                    if (beginHourMin.compareTo(Valid1DateEnd) >= 0) {
                        computeBegin = beginHourMin.compareTo(Valid2DateBegin) > 0 ? beginHourMin : Valid2DateBegin;
                        computeEnd = endHourMin.compareTo(Valid2DateEnd) > 0 ? Valid2DateEnd : endHourMin;

                        if (timeCompare(clockRecord.getDate() + " " + computeBegin, clockRecord.getDate() + " " + computeEnd) >= 3) {
                            processValidDate(teachers, dayWeek, weekOfDate, clockRecord, 5, 8, 1);
                        } else {
                            clockRecord.getIsValidNote()[1] = UN_VALID_MSG;
                        }
                    } else {
                        computeEnd = endHourMin.compareTo(Valid2DateEnd) > 0 ? Valid2DateEnd : endHourMin;
                        if (timeCompare(clockRecord.getDate() + " " + Valid2DateBegin, clockRecord.getDate() + " " + computeEnd) >= 3) {
                            processValidDate(teachers, dayWeek, weekOfDate, clockRecord, 5, 8, 1);
                        }

                        computeBegin = beginHourMin.compareTo(Valid1DateBegin) > 0 ? beginHourMin : Valid1DateBegin;
                        if (timeCompare(clockRecord.getDate() + " " + computeBegin, clockRecord.getDate() + " " + Valid1DateEnd) >= 3) {
                            processValidDate(teachers, dayWeek, weekOfDate, clockRecord, 1, 4, 0);
                        } else {
                            clockRecord.getIsValidNote()[0] = UN_VALID_MSG;
                        }
                    }
                } else { // 判断3个时间段是否有效
                    if (beginHourMin.compareTo(Valid3DateBegin) >= 0) {
                        computeEnd = endHourMin.compareTo(Valid3DateEnd) > 0 ? Valid3DateEnd : endHourMin;

                        if (timeCompare(clockRecord.getDate() + " " + beginHourMin, clockRecord.getDate() + " " + computeEnd) >= 3) {
                            processValidDate(teachers, dayWeek, weekOfDate, clockRecord, 9, 10, 2);
                        } else {
                            clockRecord.getIsValidNote()[2] = UN_VALID_MSG;
                        }
                    } else if (beginHourMin.compareTo(Valid2DateBegin) >= 0) {
                        computeEnd = endHourMin.compareTo(Valid3DateEnd) > 0 ? Valid3DateEnd : endHourMin;
                        if (timeCompare(clockRecord.getDate() + " " + Valid3DateBegin, clockRecord.getDate() + " " + computeEnd) >= 3) {
                            processValidDate(teachers, dayWeek, weekOfDate, clockRecord, 9, 10, 2);
                        } else {
                            clockRecord.getIsValidNote()[2] = UN_VALID_MSG;
                        }

                        if (timeCompare(clockRecord.getDate() + " " + beginHourMin, clockRecord.getDate() + " " + Valid2DateEnd) >= 3) {
                            processValidDate(teachers, dayWeek, weekOfDate, clockRecord, 5, 8, 1);
                        } else {
                            clockRecord.getIsValidNote()[1] = UN_VALID_MSG;
                        }
                    } else {
                        computeEnd = endHourMin.compareTo(Valid3DateEnd) > 0 ? Valid3DateEnd : endHourMin;
                        if (timeCompare(clockRecord.getDate() + " " + Valid3DateBegin, clockRecord.getDate() + " " + computeEnd) >= 3) {
                            processValidDate(teachers, dayWeek, weekOfDate, clockRecord, 9, 10, 2);
                        } else {
                            clockRecord.getIsValidNote()[2] = UN_VALID_MSG;
                        }

                        if (timeCompare(clockRecord.getDate() + " " + Valid2DateBegin, clockRecord.getDate() + " " + Valid2DateEnd) >= 3) {
                            processValidDate(teachers, dayWeek, weekOfDate, clockRecord, 5, 8, 1);
                        } else {
                            clockRecord.getIsValidNote()[1] = UN_VALID_MSG;
                        }
                        computeBegin = beginHourMin.compareTo(Valid1DateBegin) > 0 ? beginHourMin : Valid1DateBegin;
                        if (timeCompare(clockRecord.getDate() + " " + computeBegin, clockRecord.getDate() + " " + Valid1DateEnd) >= 3) {
                            processValidDate(teachers, dayWeek, weekOfDate, clockRecord, 1, 4, 0);
                        } else {
                            clockRecord.getIsValidNote()[0] = UN_VALID_MSG;
                        }
                    }
                }
            });
        }
    }

    //处理8:30-12:00时间段是否有效
    private static void processValidDate(List<Teacher> teachers, int dayWeek, int weekOfDate, ClockRecord clockRecord, int startTime, int endTime, int index) {
        // 判断当前老师是否有课
        String teacherName = clockRecord.getTeacher().getName();
        Teacher teacher = teachers.stream().filter(it -> it.getName().equals(teacherName)).findFirst().orElse(null);
        if (teacher == null) { // 该老师不上课，有效
            clockRecord.getIsValid()[index] = true;
            return;
        }

        Curriculum cur = teacher.getCurriculums().stream().filter(curriculum -> {
            // 第几周上课，周几，1-4节有课
            return curriculum.getWeeks().contains(dayWeek) && curriculum.getWeeks().contains(weekOfDate)
                    && (curriculum.getStartTime() >= startTime || curriculum.getEndTime() <= endTime);
        }).findFirst().orElse(null);
        if (cur != null) {
            clockRecord.getIsValidNote()[index] = "授 [" + cur.getCourseName() + "] 课";
        } else { // 无课
            clockRecord.getIsValid()[index] = true;
        }
    }

    // 依据时间判断周几
    public static int getWeekOfDate(String date) {
        int[] weekDays = {7, 1, 2, 3, 4, 5, 6};
        Calendar cal = Calendar.getInstance();
        int w = 0;
        try {
            cal.setTimeInMillis(df.parse(date).getTime());
            w = cal.get(Calendar.DAY_OF_WEEK) - 1;
            if (w < 0) {
                w = 0;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            System.err.println("计算当前日期是第几周时出错。");
        }
        return weekDays[w];
    }

    /**
     * 计算当前日期为第几周。使用向上取整实现
     *
     * @param date
     * @return
     */
    private static int computeDayWeek(String date) {
        int result = 0;
        try {
            result = (int) Math.ceil((df.parse(date).getTime() - commencementDate.getTime()) * 1.0 / (7 * 24 * 60 * 60 * 1000));
        } catch (ParseException e) {
            System.err.println("计算当前日期是第几周时出错。");
            e.printStackTrace();
        } finally {
            return result;
        }
    }

    // 计算两个时间相差几个小时
    private static long timeCompare(String beginDate, String endDate) {
        long result = 0;
        try {
            result = new Double((df_hour.parse(endDate).getTime() - df_hour.parse(beginDate).getTime()) * 1.0 / (60 * 60 * 1000)).longValue();
        } catch (ParseException e) {
            System.err.println("计算当前日期是第几周时出错。");
            e.printStackTrace();
        } finally {
            return result;
        }
    }
}
