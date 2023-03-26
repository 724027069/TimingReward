package util;

import entity.ClockRecord;
import entity.Curriculum;
import entity.Teacher;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 工具类
 *
 * @Date: 2023/3/17 23:22
 * @Description:
 */
public class Utils {

    // 读取resources目录下的文件
    public static List<ClockRecord> readClockRecord() throws IOException {
        List<ClockRecord> clockRecords = new ArrayList<>();

        String filePath = Utils.class.getClassLoader().getResource("ALOG_001.txt").getPath();
        File file = new File(filePath);

        try (InputStreamReader read = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
            //文件流是否存在
            if (file.isFile() && file.exists()) {
                @SuppressWarnings("resource")
                BufferedReader bufferedReader = new BufferedReader(read);
                String txt = null;
                boolean isFirst = true;
                while ((txt = bufferedReader.readLine()) != null) {
                    if (!isFirst) {
                        String[] txtArr = txt.split("\\t");
                        Teacher teacher = new Teacher(txtArr[3], txtArr[4]);
                        String dateTime = txtArr[6];
                        String[] dataArr = dateTime.split(" ");

                        // 判断该老师当日是否有打卡
                        ClockRecord clockRecord = clockRecords.stream().filter(cr -> {
                            return cr.getTeacher().getName().equals(teacher.getName()) && cr.getDate().equals(dataArr[0]);
                        }).findFirst().orElse(null);

                        // 该老师当日第一次打卡，添加到队列中
                        if (clockRecord == null) {
                            clockRecord = new ClockRecord.Builder(teacher)
                                    .date(dataArr[0])
                                    .beginTime(dateTime)
                                    .endTime(dateTime)
                                    .build();
                            clockRecords.add(clockRecord);
                        } else if (dateTime.compareTo(clockRecord.getEndTime()) > 0) { // 已经打卡一次，判断打卡时间是否需要更新
                            clockRecord.setEndTime(dateTime);
                        }
                    }
                    isFirst = false;
                }
            }
        }
        return clockRecords;
    }

    /**
     * 读取课表内容
     * 课表信息如下：
     * 编号	    姓名  	周几上课	  时间	       周次
     * 205073	李克峰	1	      10:15-11:45	1-8
     * 表示编号为205073的老师，第1-8周的 周一 10:15-11:45 上课
     *
     * @throws IOException
     */
    public static List<Teacher> readSchoolTimetable() throws IOException {
        List<Teacher> teachers = new ArrayList<>();
        // 文件名不能是中文
        String filePath = Utils.class.getClassLoader().getResource("SchoolTimetable.xls").getPath();
        HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(filePath));
        //读取Sheet
        Sheet sheet = workbook.getSheetAt(0);

        //获取最大行数
        int rowNum = sheet.getPhysicalNumberOfRows();
        if (rowNum <= 1) {
            System.err.println("表格内容为空，第一行表示表头，请从第二行开始编辑教师课表内容");
            return teachers;
        }

        for (int i = 1; i < rowNum; i++) {
            //获取第i行数据
            Row row = sheet.getRow(i);

            // 第7列(从0开始)表示教师，如果存在多个教师同上一个课程，选第一个教师。 按照[ ] 进行分割，第一个表示教师姓名，第二个表示教师编号
            String[] teacherInfo = (getCellText(row.getCell(7)).split(",")[0]).split("[\\[\\]]");
            Teacher currentTeacher = teachers
                    .stream()
                    .filter(teacher -> teacherInfo[1].equals(teacher.getId()))
                    .findFirst()
                    .orElse(null);
            if (currentTeacher == null) {
                currentTeacher = new Teacher(teacherInfo[1], teacherInfo[0]);
                teachers.add(currentTeacher);
            }

            // 课程名,下标从0开始
            String courseName = getCellText(row.getCell(4));

            // 开课时间
            String openingTime = getCellText(row.getCell(9));

            // 周几上课
            int dayOfTheWeek = Integer.parseInt(openingTime.substring(0, openingTime.length() - 4));
            int startTime = Integer.parseInt(openingTime.substring(openingTime.length() - 4, openingTime.length() - 2));
            int endTime = Integer.parseInt(openingTime.substring(openingTime.length() - 2));

            // 上几周课
            String week = getCellText(row.getCell(11));
            List<Integer> weeks = new ArrayList<>();

            for (String single : week.split(",")) {
                if (single.contains("-")) {
                    String[] split = single.split("-");
                    for (int j = Integer.parseInt(split[0]); j <= Integer.parseInt(split[1]); j++) {
                        weeks.add(j);
                    }
                } else {
                    weeks.add(Integer.parseInt(single));
                }
            }
            weeks.stream().sorted();

            currentTeacher
                    .getCurriculums()
                    .add(new Curriculum(courseName, dayOfTheWeek, startTime, endTime, weeks));
        }

        return teachers;
    }

    private static String getCellText(Cell cell) {
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue();
    }

    public static void export(List<ClockRecord> records) {
        // 1.创建一个工作簿。03
        Workbook workbook = new HSSFWorkbook();
        createSum(records, workbook);

        // 创建明细
        createDetails(records, workbook);

        try (FileOutputStream fileOutputStream = new FileOutputStream(System.getProperty("user.dir") + "/打卡奖励.xls")) {
            workbook.write(fileOutputStream);
        } catch (FileNotFoundException e) {
            System.err.println("生成报表失败。" + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("生成报表失败。" + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("打卡奖励表生成成功！");
    }

    private static void createSum(List<ClockRecord> records, Workbook workbook) {
        // 创建汇总
        Sheet sheet = workbook.createSheet("汇总");
        Row row = sheet.createRow(0);
        Cell cell0 = row.createCell(0);
        cell0.setCellValue("授课教师");
        Cell cell1 = row.createCell(1);
        cell1.setCellValue("奖励");

        Map<String, List<ClockRecord>> teacherListMap = records.stream().collect(Collectors.groupingBy(ClockRecord::getTeacherName));
        Map<String, Integer> unsortMap = new HashMap<>();
        for (Map.Entry<String, List<ClockRecord>> entry : teacherListMap.entrySet()) {
            int result = 0;
            for (ClockRecord clockRecord : entry.getValue()) {
                for (boolean isValid : clockRecord.getIsValid()) {
                    if (isValid) {
                        result++;
                    }
                }
            }
            if (result == 0) {
                continue;
            }
            unsortMap.put(entry.getKey(), result * 15);
        }

        Map<String, Integer> result = new LinkedHashMap<>();
        unsortMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEachOrdered(x -> result.put(x.getKey(), x.getValue()));

        AtomicInteger index = new AtomicInteger();
        result.forEach((key, value) -> {
            Row tempRow = sheet.createRow(index.incrementAndGet());
            // 4.创建列。
            Cell tempCell0 = tempRow.createCell(0);
            tempCell0.setCellValue(key);
            Cell tempCell1 = tempRow.createCell(1);
            tempCell1.setCellValue(value);
        });
    }

    private static void createDetails(List<ClockRecord> records, Workbook workbook) {
        // 2.创建一个工作表
        Sheet sheet = workbook.createSheet("明细");
        // 3.创建行。第一行
        Row row = sheet.createRow(0);
        // 4.创建列。
        Cell cell0 = row.createCell(0);
        cell0.setCellValue("日期");
        Cell cell1 = row.createCell(1);
        cell1.setCellValue("授课教师");
        Cell cell2 = row.createCell(2);
        cell2.setCellValue("开始打卡日期");
        Cell cell3 = row.createCell(3);
        cell3.setCellValue("结束打卡日期");
        Cell cell4 = row.createCell(4);
        cell4.setCellValue("08:30-12:00 是否有效");
        Cell cell5 = row.createCell(5);
        cell5.setCellValue("无效备注1");
        Cell cell6 = row.createCell(6);
        cell6.setCellValue("13:30-17:00 是否有效");
        Cell cell7 = row.createCell(7);
        cell7.setCellValue("无效备注2");
        Cell cell8 = row.createCell(8);
        cell8.setCellValue("18:00-21:30 是否有效");
        Cell cell9 = row.createCell(9);
        cell9.setCellValue("无效备注3");

        // 第二行。(1,0)
        int index = 1;
        for (ClockRecord record : records) {
            Row tempRow = sheet.createRow(index++);
            // 4.创建列。
            Cell tempCell0 = tempRow.createCell(0);
            tempCell0.setCellValue(record.getDate());
            Cell tempCell1 = tempRow.createCell(1);
            tempCell1.setCellValue(record.getTeacher().getName());
            Cell tempCell2 = tempRow.createCell(2);
            tempCell2.setCellValue(record.getBeginTime());
            Cell tempCell3 = tempRow.createCell(3);
            tempCell3.setCellValue(record.getEndTime());
            Cell tempCell4 = tempRow.createCell(4);
            tempCell4.setCellValue(record.getIsValid()[0]);
            Cell tempCell5 = tempRow.createCell(5);
            tempCell5.setCellValue(record.getIsValidNote()[0]);
            Cell tempCell6 = tempRow.createCell(6);
            tempCell6.setCellValue(record.getIsValid()[1]);
            Cell tempCell7 = tempRow.createCell(7);
            tempCell7.setCellValue(record.getIsValidNote()[1]);
            Cell tempCell8 = tempRow.createCell(8);
            tempCell8.setCellValue(record.getIsValid()[2]);
            Cell tempCell9 = tempRow.createCell(9);
            tempCell9.setCellValue(record.getIsValidNote()[2]);
        }
    }


}
