package entity;

/**
 * @Date: 2023/3/17 23:39
 * @Description:  打卡记录
 */
public class ClockRecord {
    Teacher teacher;
    // 日期
    String date;
    // 当日打卡开始时间
    String beginTime;
    // 当日打卡结束时间
    String endTime;
    // 三个时间段是否有效
    boolean[] isValid = new boolean[3];
    // 三个时间段是否有效的备注
    String[] isValidNote = new String[3];

    public boolean[] getIsValid() {
        return isValid;
    }

    public String[] getIsValidNote() {
        return isValidNote;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public String getTeacherName() {
        return teacher.getName();
    }

    public String getDate() {
        return date;
    }

    public String getBeginTime() {
        return beginTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }


    private ClockRecord(Builder builder) {
        this.teacher = builder.teacher;
        this.date = builder.date;
        this.beginTime = builder.beginTime;
        this.endTime = builder.endTime;
    }

    // 入参太多，使用建造者模式
    public static class Builder{
        private Teacher teacher;
        private String date;
        private String beginTime;
        private String endTime;

        public Builder(Teacher teacher){
            this.teacher = teacher;
        }

        public Builder date(String date){
            this.date = date;
            return this;
        }

        public Builder beginTime(String beginTime){
            this.beginTime = beginTime;
            return this;
        }

        public Builder endTime(String endTime){
            this.endTime = endTime;
            return this;
        }

        public ClockRecord build(){
            return new ClockRecord(this);
        }
    }
}
